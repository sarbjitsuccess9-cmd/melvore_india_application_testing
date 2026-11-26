<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/RazorpayService.php';

MelvoreResponse::requireMethod('POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $body = MelvoreResponse::jsonBody();

    $orderId = (int)($body['order_id'] ?? 0);
    $rzpOrderId = trim((string)($body['razorpay_order_id'] ?? ''));
    $rzpPaymentId = trim((string)($body['razorpay_payment_id'] ?? ''));
    $signature = trim((string)($body['razorpay_signature'] ?? ''));

    if ($orderId <= 0 || $rzpOrderId === '' || $rzpPaymentId === '' || $signature === '') {
        MelvoreResponse::error('Complete Razorpay verification fields are required.', 'VALIDATION_ERROR', 400);
    }

    $pdo->beginTransaction();
    $stmt = $pdo->prepare('SELECT * FROM orders WHERE order_id = ? AND user_id = ? FOR UPDATE');
    $stmt->execute([$orderId, $userId]);
    $order = $stmt->fetch();
    if (!$order) {
        $pdo->rollBack();
        MelvoreResponse::error('Order not found.', 'NOT_FOUND', 404);
    }
    if (strtolower((string)$order['order_status']) === 'cancelled') {
        $pdo->rollBack();
        MelvoreResponse::error('Order has already been cancelled.', 'ORDER_CANCELLED', 409);
    }
    if (strtolower((string)$order['payment_status']) === 'paid') {
        $pdo->commit();
        MelvoreResponse::okFlat([
            'order_id' => $orderId,
            'order_number' => (string)$order['order_number'],
            'payment_status' => 'paid',
            'order_status' => (string)$order['order_status'],
        ], 'Payment already verified');
    }

    // Bind check against saved Razorpay order ID
    $storedDetails = !empty($order['payment_details']) ? json_decode((string)$order['payment_details'], true) : [];
    if (is_array($storedDetails) && !empty($storedDetails['razorpay_order_id']) && !hash_equals((string)$storedDetails['razorpay_order_id'], $rzpOrderId)) {
        $pdo->rollBack();
        MelvoreResponse::error('Razorpay order binding mismatch.', 'SIGNATURE_MISMATCH', 403);
    }

    $keyId = MelvoreRazorpayService::keyId($pdo);
    $keySecret = MelvoreRazorpayService::keySecret($pdo);
    if (!MelvoreRazorpayService::verifyCheckoutSignature($rzpOrderId, $rzpPaymentId, $signature, $keySecret)) {
        $pdo->rollBack();
        MelvoreResponse::error('Invalid payment signature.', 'SIGNATURE_MISMATCH', 403);
    }

    // Server-side verification via Razorpay Payments REST API
    $rzpPayment = MelvoreRazorpayService::fetchPayment($keyId, $keySecret, $rzpPaymentId);
    $expectedPaise = (int)round(((float)$order['total_amount']) * 100);
    $paidStatus = strtolower((string)($rzpPayment['status'] ?? ''));

    if (($rzpPayment['order_id'] ?? '') !== $rzpOrderId || strtoupper((string)($rzpPayment['currency'] ?? '')) !== 'INR' || (int)($rzpPayment['amount'] ?? 0) !== $expectedPaise) {
        $pdo->rollBack();
        MelvoreResponse::error('Razorpay payment amount/order verification failed.', 'PAYMENT_MISMATCH', 409);
    }
    if ($paidStatus === 'authorized') {
        $rzpPayment = MelvoreRazorpayService::capturePayment($keyId, $keySecret, $rzpPaymentId, $expectedPaise);
        $paidStatus = strtolower((string)($rzpPayment['status'] ?? ''));
    }
    if ($paidStatus !== 'captured') {
        $pdo->rollBack();
        MelvoreResponse::error('Payment has not been captured yet.', 'PAYMENT_NOT_CAPTURED', 402);
    }

    $pdo->prepare(
        "UPDATE orders
         SET payment_status = 'paid',
             order_status = CASE WHEN order_status = 'pending' THEN 'processing' ELSE order_status END,
             payment_transaction_id = ?
         WHERE order_id = ?"
    )->execute([$rzpPaymentId, $orderId]);

    if (MelvoreDB::tableExists('payments')) {
        $pdo->prepare(
            "UPDATE payments SET razorpay_payment_id = ?, status = 'completed' WHERE razorpay_order_id = ? AND user_id = ?"
        )->execute([$rzpPaymentId, $rzpOrderId, $userId]);
    }
    if (MelvoreDB::tableExists('user_cart')) {
        $pdo->prepare('DELETE FROM user_cart WHERE user_id = ?')->execute([$userId]);
    }

    $pdo->commit();
    MelvoreResponse::okFlat([
        'order_id' => $orderId,
        'order_number' => (string)$order['order_number'],
        'payment_status' => 'paid',
        'order_status' => 'processing',
    ], 'Payment verified and confirmed');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[Melvore][payments/verify] Exception: ' . $e->getMessage());
    MelvoreResponse::error($e->getMessage() ?: 'Unable to verify payment.', 'PAYMENT_VERIFY_ERROR', 500);
}
