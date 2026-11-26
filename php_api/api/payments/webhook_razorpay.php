<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/services/RazorpayService.php';

MelvoreResponse::requireMethod('POST');

try {
    $pdo = MelvoreDB::pdo();
    $rawBody = MelvoreResponse::rawBody();
    $signature = (string)($_SERVER['HTTP_X_RAZORPAY_SIGNATURE'] ?? '');
    $webhookSecret = MelvoreRazorpayService::webhookSecret($pdo);

    if (!MelvoreRazorpayService::verifyWebhookSignature($rawBody, $signature, $webhookSecret)) {
        MelvoreResponse::error('Invalid Razorpay webhook signature.', 'INVALID_SIGNATURE', 401);
    }

    $payload = json_decode($rawBody, true);
    if (!is_array($payload)) {
        MelvoreResponse::error('Malformed webhook payload.', 'BAD_REQUEST', 400);
    }

    $eventType = (string)($payload['event'] ?? '');
    $paymentEntity = $payload['payload']['payment']['entity'] ?? [];
    $rzpPaymentId = (string)($paymentEntity['id'] ?? '');
    $rzpOrderId = (string)($paymentEntity['order_id'] ?? '');
    $eventId = (string)($payload['id'] ?? hash('sha256', $eventType . '|' . $rzpPaymentId . '|' . $rzpOrderId));

    $pdo->beginTransaction();

    if (MelvoreDB::tableExists('payment_webhook_events')) {
        $chk = $pdo->prepare("SELECT webhook_id, status FROM payment_webhook_events WHERE gateway = 'razorpay' AND event_id = ? FOR UPDATE");
        $chk->execute([$eventId]);
        $existing = $chk->fetch();
        if ($existing && ($existing['status'] ?? '') === 'processed') {
            $pdo->commit();
            MelvoreResponse::okFlat(['deduped' => true], 'Webhook already processed');
        }
        if (!$existing) {
            $pdo->prepare(
                "INSERT INTO payment_webhook_events (gateway, event_id, event_type, status, payload_json)
                 VALUES ('razorpay', ?, ?, 'processing', ?)"
            )->execute([$eventId, $eventType, $rawBody]);
        }
    }

    $orderId = null;
    if ($rzpOrderId !== '') {
        $oStmt = $pdo->prepare('SELECT * FROM orders WHERE payment_details LIKE ? FOR UPDATE');
        $oStmt->execute(['%' . $rzpOrderId . '%']);
        $order = $oStmt->fetch();
        if ($order) {
            $orderId = (int)$order['order_id'];
            $curStatus = strtolower((string)$order['order_status']);
            if ($eventType === 'payment.captured' && $curStatus !== 'cancelled' && strtolower((string)$order['payment_status']) !== 'paid') {
                $pdo->prepare(
                    "UPDATE orders
                     SET payment_status = 'paid',
                         order_status = CASE WHEN order_status = 'pending' THEN 'processing' ELSE order_status END,
                         payment_transaction_id = ?
                     WHERE order_id = ?"
                )->execute([$rzpPaymentId, $orderId]);
                if (!empty($order['user_id']) && MelvoreDB::tableExists('user_cart')) {
                    $pdo->prepare('DELETE FROM user_cart WHERE user_id = ?')->execute([(int)$order['user_id']]);
                }
            } elseif ($eventType === 'payment.failed' && strtolower((string)$order['payment_status']) !== 'paid') {
                $pdo->prepare("UPDATE orders SET payment_status = 'failed' WHERE order_id = ?")->execute([$orderId]);
            }
        }
    }

    if (MelvoreDB::tableExists('payment_webhook_events')) {
        $pdo->prepare(
            "UPDATE payment_webhook_events SET status = 'processed', order_id = ? WHERE gateway = 'razorpay' AND event_id = ?"
        )->execute([$orderId, $eventId]);
    }

    $pdo->commit();
    MelvoreResponse::okFlat(['processed' => true, 'order_id' => $orderId], 'Webhook processed');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[Melvore][webhook_razorpay] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Webhook processing error.', 'WEBHOOK_ERROR', 500);
}
