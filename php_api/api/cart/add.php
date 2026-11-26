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
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    if ($userId === null && $guestId === '') {
        MelvoreResponse::error('Missing guest session identifier.', 'GUEST_SESSION_REQUIRED', 400);
    }

    $body = MelvoreResponse::jsonBody();
    $productId = (int)($body['product_id'] ?? 0);
    $variationId = isset($body['variation_id']) && (int)$body['variation_id'] > 0 ? (int)$body['variation_id'] : null;
    $quantity = max(1, min(20, (int)($body['quantity'] ?? 1)));
    $color = isset($body['color']) && trim((string)$body['color']) !== '' ? trim((string)$body['color']) : null;
    $size = isset($body['size']) && trim((string)$body['size']) !== '' ? trim((string)$body['size']) : null;

    if ($productId <= 0) {
        MelvoreResponse::error('Valid product_id is required.', 'VALIDATION_ERROR', 400);
    }

    $pStmt = $pdo->prepare('SELECT product_id, name, stock_quantity, is_active FROM products WHERE product_id = ? LIMIT 1');
    $pStmt->execute([$productId]);
    $product = $pStmt->fetch();
    if (!$product || ((int)$product['is_active']) !== 1) {
        MelvoreResponse::error('Product is no longer available.', 'PRODUCT_UNAVAILABLE', 404);
    }

    $availStock = (int)$product['stock_quantity'];
    if ($variationId !== null) {
        $vStmt = $pdo->prepare('SELECT variation_id, stock_quantity, is_active FROM product_variations WHERE variation_id = ? AND product_id = ? LIMIT 1');
        $vStmt->execute([$variationId, $productId]);
        $var = $vStmt->fetch();
        if (!$var || ((int)($var['is_active'] ?? 1)) !== 1) {
            MelvoreResponse::error('Selected variant is unavailable.', 'VARIANT_UNAVAILABLE', 404);
        }
        $availStock = (int)$var['stock_quantity'];
    }

    if ($availStock <= 0) {
        MelvoreResponse::error('This item is currently out of stock.', 'OUT_OF_STOCK', 409);
    }

    if ($userId !== null && $userId > 0) {
        $find = $pdo->prepare(
            'SELECT user_cart_id, quantity FROM user_cart
             WHERE user_id = ? AND product_id = ? AND COALESCE(variation_id, 0) = ? AND COALESCE(color, "") = ? AND COALESCE(size, "") = ?
             LIMIT 1'
        );
        $find->execute([$userId, $productId, $variationId ?? 0, (string)($color ?? ''), (string)($size ?? '')]);
        $existing = $find->fetch();
        if ($existing) {
            $newQty = min($availStock, min(20, ((int)$existing['quantity']) + $quantity));
            $pdo->prepare('UPDATE user_cart SET quantity = ? WHERE user_cart_id = ?')->execute([$newQty, (int)$existing['user_cart_id']]);
        } else {
            $ins = $pdo->prepare(
                'INSERT INTO user_cart (user_id, product_id, variation_id, quantity, color, size) VALUES (?, ?, ?, ?, ?, ?)'
            );
            $ins->execute([$userId, $productId, $variationId, min($availStock, $quantity), $color, $size]);
        }
    } else {
        $find = $pdo->prepare(
            'SELECT cart_id, quantity FROM cart
             WHERE user_id IS NULL AND session_id = ? AND product_id = ? AND COALESCE(variation_id, 0) = ? AND COALESCE(color, "") = ? AND COALESCE(size, "") = ?
             LIMIT 1'
        );
        $find->execute([$guestId, $productId, $variationId ?? 0, (string)($color ?? ''), (string)($size ?? '')]);
        $existing = $find->fetch();
        if ($existing) {
            $newQty = min($availStock, min(20, ((int)$existing['quantity']) + $quantity));
            $pdo->prepare('UPDATE cart SET quantity = ? WHERE cart_id = ?')->execute([$newQty, (int)$existing['cart_id']]);
        } else {
            $ins = $pdo->prepare(
                'INSERT INTO cart (user_id, session_id, product_id, variation_id, quantity, color, size) VALUES (NULL, ?, ?, ?, ?, ?, ?)'
            );
            $ins->execute([$guestId, $productId, $variationId, min($availStock, $quantity), $color, $size]);
        }
    }

    MelvoreRecommendationService::invalidateForActor($pdo, $userId, $guestId);

    $items = MelvoreCartHelper::fetchCartItems($pdo, $userId, $guestId);
    $summary = MelvorePricingEngine::calculateTotals($pdo, $items, '', 'online', $userId);
    $totalQty = array_sum(array_map(static fn($it) => (int)$it['quantity'], $items));

    MelvoreResponse::okFlat([
        'items' => $items,
        'count' => $totalQty,
        'summary' => $summary,
    ], 'Added to bag');
} catch (Throwable $e) {
    error_log('[Melvore][cart/add] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to add item to bag.', 'SERVER_ERROR', 500);
}
