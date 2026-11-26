<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/OrderService.php';
require_once dirname(__DIR__, 2) . '/services/RazorpayService.php';

MelvoreResponse::requireMethod('POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $body = MelvoreResponse::jsonBody();
    $orderId = (int)($body['order_id'] ?? 0);
    $reason = trim((string)($body['reason'] ?? 'Customer cancelled'));

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
    $curStatus = strtolower((string)$order['order_status']);
    if ($curStatus === 'cancelled' || $curStatus === 'refunded') {
        $pdo->commit();
        MelvoreResponse::okFlat([
            'order_id' => $orderId,
            'order_number' => (string)$order['order_number'],
            'order_status' => $curStatus,
            'payment_status' => (string)$order['payment_status'],
            'refund_status' => $order['refund_status'] ?? null,
            'refund_reference' => $order['refund_reference'] ?? null,
        ], 'Order already cancelled');
    }
    if (in_array($curStatus, ['shipped', 'delivered'], true)) {
        $pdo->rollBack();
        MelvoreResponse::error('Orders already dispatched cannot be cancelled online. Please open a support request.', 'CANNOT_CANCEL', 409);
    }

    $refundStatus = null;
    $refundRef = null;
    $paymentStatus = strtolower((string)($order['payment_status'] ?? 'pending'));
    $newOrderStatus = 'cancelled';

    if ($paymentStatus === 'paid' && !empty($order['payment_transaction_id'])) {
        $keyId = MelvoreRazorpayService::keyId($pdo);
        $keySecret = MelvoreRazorpayService::keySecret($pdo);
        if ($keyId !== '' && $keySecret !== '') {
            $paise = (int)round(((float)$order['total_amount']) * 100);
            $refund = MelvoreRazorpayService::refundPayment(
                $keyId,
                $keySecret,
                (string)$order['payment_transaction_id'],
                $paise,
                ['order_number' => (string)$order['order_number'], 'reason' => $reason]
            );
            $refundStatus = 'initiated';
            $refundRef = (string)($refund['id'] ?? '');
            $paymentStatus = 'refunded';
            $newOrderStatus = 'refunded';
        } else {
            $refundStatus = 'manual_pending';
        }
    }

    MelvoreOrderService::restoreStockAndCoupon($pdo, $order);

    $hasRefundCols = MelvoreDB::columnExists('orders', 'refund_status') && MelvoreDB::columnExists('orders', 'refund_reference');
    if ($hasRefundCols) {
        $pdo->prepare(
            'UPDATE orders SET order_status = ?, payment_status = ?, cancellation_reason = ?, refund_status = ?, refund_reference = ? WHERE order_id = ?'
        )->execute([$newOrderStatus, $paymentStatus, $reason, $refundStatus, $refundRef, $orderId]);
    } else {
        $pdo->prepare(
            'UPDATE orders SET order_status = ?, payment_status = ?, cancellation_reason = ? WHERE order_id = ?'
        )->execute([$newOrderStatus, $paymentStatus, $reason, $orderId]);
    }

    $pdo->commit();
    MelvoreResponse::okFlat([
        'order_id' => $orderId,
        'order_number' => (string)$order['order_number'],
        'order_status' => $newOrderStatus,
        'payment_status' => $paymentStatus,
        'refund_status' => $refundStatus,
        'refund_reference' => $refundRef,
    ], 'Order cancelled and inventory restored');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[Melvore][orders/cancel] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to cancel order.', 'SERVER_ERROR', 500);
}
