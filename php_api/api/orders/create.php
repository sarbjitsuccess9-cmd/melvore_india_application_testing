<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/CartHelper.php';
require_once dirname(__DIR__, 2) . '/services/PricingEngine.php';
require_once dirname(__DIR__, 2) . '/services/RecommendationService.php';

MelvoreResponse::requireMethod('POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $body = MelvoreResponse::jsonBody();

    $shippingAddressId = (int)($body['shipping_address_id'] ?? 0);
    $paymentMethod = strtolower(trim((string)($body['payment_method'] ?? 'cod')));
    if (!in_array($paymentMethod, ['cod', 'online'], true)) {
        MelvoreResponse::error('Payment method must be cod or online.', 'INVALID_PAYMENT_METHOD', 400);
    }
    $couponCode = strtoupper(trim((string)($body['coupon_code'] ?? '')));
    $notes = trim((string)($body['notes'] ?? ''));
    $idemKey = trim((string)($_SERVER['HTTP_IDEMPOTENCY_KEY'] ?? ''));

    if ($shippingAddressId <= 0) {
        MelvoreResponse::error('Please select a shipping address.', 'ADDRESS_REQUIRED', 400);
    }

    $reqHash = hash('sha256', json_encode([$userId, $shippingAddressId, $paymentMethod, $couponCode, $notes]));
    if ($idemKey !== '' && MelvoreDB::tableExists('api_order_idempotency')) {
        $iStmt = $pdo->prepare('SELECT response_json, request_hash FROM api_order_idempotency WHERE user_id = ? AND idempotency_key = ? LIMIT 1');
        $iStmt->execute([$userId, substr($idemKey, 0, 128)]);
        $cached = $iStmt->fetch();
        if ($cached) {
            if (!hash_equals((string)$cached['request_hash'], $reqHash)) {
                MelvoreResponse::error('Idempotency key reused with different checkout payload.', 'IDEMPOTENCY_CONFLICT', 409);
            }
            $decoded = json_decode((string)$cached['response_json'], true);
            if (is_array($decoded)) {
                MelvoreResponse::okFlat($decoded, 'Order retrieved (idempotent)');
            }
        }
    }

    $addrStmt = $pdo->prepare('SELECT * FROM user_addresses WHERE address_id = ? AND user_id = ? LIMIT 1');
    $addrStmt->execute([$shippingAddressId, $userId]);
    if (!$addrStmt->fetch()) {
        MelvoreResponse::error('Selected shipping address was not found.', 'ADDRESS_NOT_FOUND', 404);
    }

    $siteCfg = MelvorePricingEngine::loadSitePricingConfig($pdo);
    if ($paymentMethod === 'cod' && !$siteCfg['enable_cod']) {
        MelvoreResponse::error('Cash on Delivery is currently unavailable.', 'PAYMENT_METHOD_DISABLED', 400);
    }
    if ($paymentMethod === 'online' && !$siteCfg['enable_online_payment']) {
        MelvoreResponse::error('Online payment is currently unavailable.', 'PAYMENT_METHOD_DISABLED', 400);
    }

    $pdo->beginTransaction();

    // Consolidate guest/legacy bag rows cleanly before locking user_cart
    $guestId = MelvoreAuth::getGuestId();
    if ($guestId !== '') {
        MelvoreCartHelper::mergeGuestCartToUser($pdo, $userId, $guestId);
    }
    MelvoreCartHelper::consolidateLegacyCartsIfNeeded($pdo, $userId);

    $lockStmt = $pdo->prepare(
        'SELECT uc.user_cart_id, uc.product_id, uc.variation_id, uc.quantity, uc.color, uc.size,
                p.name, p.sku, p.category_id, p.regular_price, p.sale_price, p.stock_quantity, p.is_active,
                pv.regular_price AS var_regular_price, pv.sale_price AS var_sale_price,
                pv.stock_quantity AS var_stock_quantity, pv.is_active AS var_is_active
         FROM user_cart uc
         INNER JOIN products p ON p.product_id = uc.product_id
         LEFT JOIN product_variations pv ON pv.variation_id = uc.variation_id
         WHERE uc.user_id = ?
         FOR UPDATE'
    );
    $lockStmt->execute([$userId]);
    $rawLines = $lockStmt->fetchAll();

    if (empty($rawLines)) {
        $pdo->rollBack();
        MelvoreResponse::error('Your shopping bag is empty.', 'EMPTY_CART', 400);
    }

    $pricedLines = [];
    foreach ($rawLines as $rl) {
        if (((int)$rl['is_active']) !== 1) {
            $pdo->rollBack();
            MelvoreResponse::error('One or more items in your bag are no longer active.', 'PRODUCT_UNAVAILABLE', 409);
        }
        $pid = (int)$rl['product_id'];
        $vid = $rl['variation_id'] !== null ? (int)$rl['variation_id'] : null;
        $qty = max(1, (int)$rl['quantity']);

        $pLock = $pdo->prepare('SELECT stock_quantity FROM products WHERE product_id = ? FOR UPDATE');
        $pLock->execute([$pid]);
        $prodStock = (int)$pLock->fetchColumn();

        if ($vid !== null) {
            $vLock = $pdo->prepare('SELECT stock_quantity, is_active FROM product_variations WHERE variation_id = ? FOR UPDATE');
            $vLock->execute([$vid]);
            $vRow = $vLock->fetch();
            if (!$vRow || ((int)($vRow['is_active'] ?? 1)) !== 1 || (int)$vRow['stock_quantity'] < $qty) {
                $pdo->rollBack();
                MelvoreResponse::error("Insufficient stock for {$rl['name']}.", 'OUT_OF_STOCK', 409);
            }
        } elseif ($prodStock < $qty) {
            $pdo->rollBack();
            MelvoreResponse::error("Insufficient stock for {$rl['name']}.", 'OUT_OF_STOCK', 409);
        }

        $regPrice = ($rl['var_regular_price'] !== null && (float)$rl['var_regular_price'] > 0)
            ? (float)$rl['var_regular_price'] : (float)$rl['regular_price'];
        $salePrice = ($rl['var_sale_price'] !== null && (float)$rl['var_sale_price'] > 0)
            ? (float)$rl['var_sale_price']
            : (($rl['sale_price'] !== null && (float)$rl['sale_price'] > 0) ? (float)$rl['sale_price'] : null);
        $unitPrice = ($salePrice !== null && $salePrice < $regPrice) ? $salePrice : $regPrice;

        $pricedLines[] = [
            'product_id' => $pid,
            'variation_id' => $vid,
            'category_id' => (int)($rl['category_id'] ?? 0),
            'product_name' => (string)$rl['name'],
            'sku' => (string)($rl['sku'] ?? ''),
            'quantity' => $qty,
            'unit_price' => round($unitPrice, 2),
            'line_total' => round($unitPrice * $qty, 2),
            'color' => $rl['color'] ?? null,
            'size' => $rl['size'] ?? null,
        ];
    }

    $totals = MelvorePricingEngine::calculateTotals($pdo, $pricedLines, $couponCode, $paymentMethod, $userId);
    if ($couponCode !== '' && $totals['coupon_id'] === null) {
        $pdo->rollBack();
        MelvoreResponse::error((string)($totals['coupon_message'] ?? 'Invalid coupon code.'), 'INVALID_COUPON', 422);
    }

    $orderNumber = 'MLV' . date('Ymd') . strtoupper(bin2hex(random_bytes(3)));
    $estDate = date('Y-m-d', strtotime('+5 days'));

    $insOrder = $pdo->prepare(
        "INSERT INTO orders (
            user_id, order_number, shipping_address_id, billing_address_id,
            subtotal, shipping_charge, tax_amount, discount_amount, cod_fee, total_amount,
            payment_method, payment_status, order_status, coupon_id, coupon_code,
            notes, estimated_delivery_date, created_at
         ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', 'pending', ?, ?, ?, ?, NOW())"
    );
    $insOrder->execute([
        $userId,
        $orderNumber,
        $shippingAddressId,
        $shippingAddressId,
        $totals['subtotal'],
        $totals['shipping_charge'],
        $totals['tax_amount'],
        $totals['discount_amount'],
        $totals['cod_fee'],
        $totals['total_amount'],
        $paymentMethod,
        $totals['coupon_id'],
        $totals['coupon_code'] !== '' ? $totals['coupon_code'] : null,
        $notes !== '' ? $notes : null,
        $estDate,
    ]);
    $orderId = (int)$pdo->lastInsertId();

    $insItem = $pdo->prepare(
        'INSERT INTO order_items (order_id, product_id, variation_id, product_name, sku, quantity, unit_price, total_price, attributes)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
    );
    $createdItems = [];
    foreach ($pricedLines as $pl) {
        $attrsJson = json_encode(['color' => $pl['color'], 'size' => $pl['size']], JSON_UNESCAPED_UNICODE);
        $insItem->execute([
            $orderId,
            $pl['product_id'],
            $pl['variation_id'],
            $pl['product_name'],
            $pl['sku'],
            $pl['quantity'],
            $pl['unit_price'],
            $pl['line_total'],
            $attrsJson,
        ]);

        // Atomic stock deduction
        $pdo->prepare('UPDATE products SET stock_quantity = GREATEST(0, stock_quantity - ?), purchase_count = purchase_count + ? WHERE product_id = ?')
            ->execute([$pl['quantity'], $pl['quantity'], $pl['product_id']]);
        if ($pl['variation_id'] !== null) {
            $pdo->prepare('UPDATE product_variations SET stock_quantity = GREATEST(0, stock_quantity - ?) WHERE variation_id = ?')
                ->execute([$pl['quantity'], $pl['variation_id']]);
        }

        $createdItems[] = [
            'product_id' => $pl['product_id'],
            'product_name' => $pl['product_name'],
            'sku' => $pl['sku'],
            'quantity' => $pl['quantity'],
            'unit_price' => $pl['unit_price'],
            'total_price' => $pl['line_total'],
            'color' => $pl['color'],
            'size' => $pl['size'],
        ];
    }

    if ($totals['coupon_id'] !== null && MelvoreDB::tableExists('coupons')) {
        $pdo->prepare('UPDATE coupons SET usage_count = usage_count + 1 WHERE coupon_id = ?')
            ->execute([(int)$totals['coupon_id']]);
    }

    // Clear user cart immediately on COD; online orders clear upon verified Razorpay capture
    if ($paymentMethod === 'cod') {
        $pdo->prepare('DELETE FROM user_cart WHERE user_id = ?')->execute([$userId]);
    }

    $orderPayload = [
        'order' => [
            'order_id' => $orderId,
            'order_number' => $orderNumber,
            'subtotal' => $totals['subtotal'],
            'shipping_charge' => $totals['shipping_charge'],
            'tax_amount' => $totals['tax_amount'],
            'discount_amount' => $totals['discount_amount'],
            'cod_fee' => $totals['cod_fee'],
            'total_amount' => $totals['total_amount'],
            'payment_method' => $paymentMethod,
            'payment_status' => 'pending',
            'order_status' => 'pending',
            'coupon_code' => $totals['coupon_code'] !== '' ? $totals['coupon_code'] : null,
            'estimated_delivery_date' => $estDate,
            'requires_online_payment' => $paymentMethod === 'online',
            'items' => $createdItems,
        ],
    ];

    if ($idemKey !== '' && MelvoreDB::tableExists('api_order_idempotency')) {
        $pdo->prepare(
            'INSERT IGNORE INTO api_order_idempotency (user_id, idempotency_key, request_hash, order_id, response_json)
             VALUES (?, ?, ?, ?, ?)'
        )->execute([$userId, substr($idemKey, 0, 128), $reqHash, $orderId, json_encode($orderPayload)]);
    }

    $pdo->commit();
    MelvoreRecommendationService::invalidateForActor($pdo, $userId, $guestId);
    MelvoreResponse::okFlat($orderPayload, 'Order created', 201);
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[Melvore][orders/create] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to place order right now.', 'SERVER_ERROR', 500);
}
