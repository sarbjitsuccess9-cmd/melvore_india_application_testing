<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/user_formatter.php';
require_once dirname(__DIR__, 2) . '/services/OrderService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $orderId = (int)($_GET['order_id'] ?? ($_GET['id'] ?? 0));
    if ($orderId <= 0) {
        MelvoreResponse::error('Valid order_id is required.', 'VALIDATION_ERROR', 400);
    }

    $stmt = $pdo->prepare('SELECT * FROM orders WHERE order_id = ? AND user_id = ? LIMIT 1');
    $stmt->execute([$orderId, $userId]);
    $row = $stmt->fetch();
    if (!$row) {
        MelvoreResponse::error('Order not found.', 'NOT_FOUND', 404);
    }

    $shippingAddr = null;
    if (!empty($row['shipping_address_id']) && MelvoreDB::tableExists('user_addresses')) {
        $aStmt = $pdo->prepare('SELECT * FROM user_addresses WHERE address_id = ? LIMIT 1');
        $aStmt->execute([(int)$row['shipping_address_id']]);
        $aRow = $aStmt->fetch();
        if ($aRow) {
            $shippingAddr = MelvoreUserFormatter::formatAddress($aRow);
        }
    }

    MelvoreResponse::okFlat([
        'order' => [
            'order_id' => (int)$row['order_id'],
            'order_number' => (string)$row['order_number'],
            'subtotal' => (float)$row['subtotal'],
            'shipping_charge' => (float)($row['shipping_charge'] ?? 0),
            'tax_amount' => (float)($row['tax_amount'] ?? 0),
            'discount_amount' => (float)($row['discount_amount'] ?? 0),
            'cod_fee' => (float)($row['cod_fee'] ?? 0),
            'total_amount' => (float)$row['total_amount'],
            'payment_method' => (string)($row['payment_method'] ?? 'cod'),
            'payment_status' => (string)($row['payment_status'] ?? 'pending'),
            'order_status' => (string)($row['order_status'] ?? 'pending'),
            'coupon_code' => $row['coupon_code'] ?? null,
            'cancellation_reason' => $row['cancellation_reason'] ?? null,
            'estimated_delivery_date' => $row['estimated_delivery_date'] ?? null,
            'tracking_number' => $row['tracking_number'] ?? null,
            'courier_name' => $row['courier_name'] ?? null,
            'tracking_url' => $row['tracking_url'] ?? null,
            'created_at' => (string)($row['created_at'] ?? ''),
            'shipping_address' => $shippingAddr,
            'items' => MelvoreOrderService::formatOrderItems($pdo, (int)$row['order_id']),
        ],
    ], 'Order details loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load order details.', 'SERVER_ERROR', 500);
}
