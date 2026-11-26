<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/OrderService.php';

MelvoreResponse::requireMethod('POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $body = MelvoreResponse::jsonBody();

    $orderId = (int)($body['order_id'] ?? 0);
    $action = strtolower(trim((string)($body['action'] ?? 'fail')));
    $reason = trim((string)($body['reason'] ?? 'Online payment failed or dismissed by customer'));

    if ($orderId <= 0) {
        MelvoreResponse::error('Valid order_id is required.', 'VALIDATION_ERROR', 400);
    }

    $pdo->beginTransaction();
    $stmt = $pdo->prepare('SELECT * FROM orders WHERE order_id = ? AND user_id = ? FOR UPDATE');
    $stmt->execute([$orderId, $userId]);
    $order = $stmt->fetch();
    if (!$order) {
        $pdo->rollBack();
        MelvoreResponse::error('Order not found.', 'NOT_FOUND', 404);
    }
    if (strtolower((string)$order['payment_status']) === 'paid') {
        $pdo->rollBack();
        MelvoreResponse::error('Paid orders cannot be marked failed.', 'ALREADY_PAID', 409);
    }

    if ($action === 'cancel_order' && strtolower((string)$order['order_status']) !== 'cancelled') {
        MelvoreOrderService::restoreStockAndCoupon($pdo, $order);
        $pdo->prepare(
            "UPDATE orders SET order_status = 'cancelled', payment_status = 'failed', cancellation_reason = ? WHERE order_id = ?"
        )->execute([$reason, $orderId]);
        if (MelvoreDB::tableExists('payments') && MelvoreDB::columnExists('payments', 'order_id')) {
            $pdo->prepare("UPDATE payments SET status = 'failed' WHERE order_id = ? AND status = 'pending'")->execute([$orderId]);
        }
        $pdo->commit();
        MelvoreResponse::okFlat([
            'order_id' => $orderId,
            'order_number' => (string)$order['order_number'],
            'order_status' => 'cancelled',
            'payment_status' => 'failed',
            'can_retry' => false,
        ], 'Unpaid order cancelled and inventory restored');
    }

    $pdo->prepare("UPDATE orders SET payment_status = 'failed' WHERE order_id = ?")->execute([$orderId]);
    if (MelvoreDB::tableExists('payments') && MelvoreDB::columnExists('payments', 'order_id')) {
        $pdo->prepare("UPDATE payments SET status = 'failed' WHERE order_id = ? AND status = 'pending'")->execute([$orderId]);
    }
    $pdo->commit();

    MelvoreResponse::okFlat([
        'order_id' => $orderId,
        'order_number' => (string)$order['order_number'],
        'order_status' => (string)$order['order_status'],
        'payment_status' => 'failed',
        'can_retry' => true,
    ], 'Payment failure recorded; retry available');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    MelvoreResponse::error('Unable to update payment state.', 'SERVER_ERROR', 500);
}
