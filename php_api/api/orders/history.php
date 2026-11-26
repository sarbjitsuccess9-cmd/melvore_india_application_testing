<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/OrderService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $page = max(1, (int)($_GET['page'] ?? 1));
    $limit = max(1, min(50, (int)($_GET['limit'] ?? 30)));
    $offset = ($page - 1) * $limit;

    $cStmt = $pdo->prepare('SELECT COUNT(*) FROM orders WHERE user_id = ?');
    $cStmt->execute([$userId]);
    $total = (int)$cStmt->fetchColumn();

    $stmt = $pdo->prepare(
        'SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC, order_id DESC LIMIT ? OFFSET ?'
    );
    $stmt->bindValue(1, $userId, PDO::PARAM_INT);
    $stmt->bindValue(2, $limit, PDO::PARAM_INT);
    $stmt->bindValue(3, $offset, PDO::PARAM_INT);
    $stmt->execute();

    $orders = [];
    foreach ($stmt->fetchAll() as $row) {
        $oid = (int)$row['order_id'];
        $orders[] = [
            'order_id' => $oid,
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
            'tracking_number' => $row['tracking_number'] ?? null,
            'courier_name' => $row['courier_name'] ?? null,
            'estimated_delivery_date' => $row['estimated_delivery_date'] ?? null,
            'created_at' => (string)($row['created_at'] ?? ''),
            'items' => MelvoreOrderService::formatOrderItems($pdo, $oid),
        ];
    }

    MelvoreResponse::okFlat([
        'orders' => $orders,
        'pagination' => [
            'page' => $page,
            'limit' => $limit,
            'total' => $total,
            'total_pages' => max(1, (int)ceil($total / $limit)),
        ],
    ], 'Order history loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load order history.', 'SERVER_ERROR', 500);
}
