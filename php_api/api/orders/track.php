<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/OrderService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $orderNumber = trim((string)($_GET['order_number'] ?? ''));
    $pincode = trim((string)($_GET['pincode'] ?? ''));
    $mobile = preg_replace('/[^0-9]/', '', (string)($_GET['mobile'] ?? ''));

    if ($orderNumber === '') {
        MelvoreResponse::error('order_number is required.', 'VALIDATION_ERROR', 400);
    }

    $stmt = $pdo->prepare(
        'SELECT o.*, ua.pincode AS ship_pincode, ua.mobile AS ship_mobile
         FROM orders o
         LEFT JOIN user_addresses ua ON ua.address_id = o.shipping_address_id
         WHERE o.order_number = ? LIMIT 1'
    );
    $stmt->execute([$orderNumber]);
    $row = $stmt->fetch();
    if (!$row) {
        MelvoreResponse::error('Order not found.', 'NOT_FOUND', 404);
    }

    $authorized = ($userId !== null && (int)$row['user_id'] === $userId);
    if (!$authorized) {
        $pinMatch = ($pincode !== '' && $pincode === trim((string)($row['ship_pincode'] ?? '')));
        $shipMobDigits = preg_replace('/[^0-9]/', '', (string)($row['ship_mobile'] ?? ''));
        $mobMatch = ($mobile !== '' && $shipMobDigits !== '' && str_ends_with($shipMobDigits, substr($mobile, -10)));
        if (!$pinMatch && !$mobMatch) {
            MelvoreResponse::error('Please sign in or verify delivery pincode/mobile to track this order.', 'FORBIDDEN', 403);
        }
    }

    MelvoreResponse::okFlat([
        'tracking' => [
            'order_id' => (int)$row['order_id'],
            'order_number' => (string)$row['order_number'],
            'order_status' => (string)($row['order_status'] ?? 'pending'),
            'payment_method' => (string)($row['payment_method'] ?? 'cod'),
            'payment_status' => (string)($row['payment_status'] ?? 'pending'),
            'total_amount' => (float)$row['total_amount'],
            'tracking_number' => $row['tracking_number'] ?? null,
            'courier_name' => $row['courier_name'] ?? null,
            'tracking_url' => $row['tracking_url'] ?? null,
            'estimated_delivery_date' => $row['estimated_delivery_date'] ?? null,
            'created_at' => (string)($row['created_at'] ?? ''),
            'timeline' => MelvoreOrderService::buildTimeline($row),
            'order_tracking_hero_text' => 'Your order, every step of the way.',
            'tracking_page_support_text' => 'Need help with delivery? Contact Melvore Support.',
        ],
    ], 'Tracking loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load order tracking.', 'SERVER_ERROR', 500);
}
