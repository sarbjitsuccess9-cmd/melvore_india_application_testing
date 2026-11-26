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
    $gateway = strtolower(trim((string)($body['gateway'] ?? 'razorpay')));

    if ($orderId <= 0) {
        MelvoreResponse::error('Valid order_id is required.', 'VALIDATION_ERROR', 400);
    }
    if ($gateway !== 'razorpay') {
        MelvoreResponse::error('Only Razorpay online payment is supported.', 'UNSUPPORTED_GATEWAY', 400);
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
        MelvoreResponse::error('Cannot pay for a cancelled order.', 'ORDER_CANCELLED', 409);
    }
    if (strtolower((string)$order['payment_status']) === 'paid') {
        $pdo->rollBack();
        MelvoreResponse::error('This order is already paid.', 'ALREADY_PAID', 409);
    }

    $amountInr = (float)$order['total_amount'];
    if ($amountInr < 1.0) {
        $pdo->rollBack();
        MelvoreResponse::error('Minimum online payment amount is ₹1.00.', 'AMOUNT_TOO_SMALL', 400);
    }
    $amountPaise = (int)round($amountInr * 100);

    $keyId = MelvoreRazorpayService::keyId($pdo);
    $keySecret = MelvoreRazorpayService::keySecret($pdo);
    $rzpOrder = MelvoreRazorpayService::createOrder(
        $keyId,
        $keySecret,
        $amountPaise,
        (string)$order['order_number'],
        ['order_id' => (string)$orderId, 'order_number' => (string)$order['order_number'], 'user_id' => (string)$userId]
    );
    $rzpOrderId = (string)($rzpOrder['id'] ?? '');
    if ($rzpOrderId === '') {
        $pdo->rollBack();
        MelvoreResponse::error('Failed to initialize Razorpay order.', 'GATEWAY_ERROR', 502);
    }

    if (MelvoreDB::tableExists('payments')) {
        $hasOrderIdCol = MelvoreDB::columnExists('payments', 'order_id');
        if ($hasOrderIdCol) {
            $pdo->prepare(
                "INSERT INTO payments (user_id, order_id, razorpay_order_id, razorpay_payment_id, amount, status, created_at)
                 VALUES (?, ?, ?, '', ?, 'pending', NOW())"
            )->execute([$userId, $orderId, $rzpOrderId, $amountInr]);
        } else {
            $pdo->prepare(
                "INSERT INTO payments (user_id, razorpay_order_id, razorpay_payment_id, amount, status, created_at)
                 VALUES (?, ?, '', ?, 'pending', NOW())"
            )->execute([$userId, $rzpOrderId, $amountInr]);
        }
    }

    $detailsJson = json_encode([
        'gateway' => 'razorpay',
        'razorpay_order_id' => $rzpOrderId,
        'amount_paise' => $amountPaise,
    ]);
    $pdo->prepare('UPDATE orders SET payment_details = ? WHERE order_id = ?')->execute([$detailsJson, $orderId]);
    $pdo->commit();

    $uStmt = $pdo->prepare('SELECT name, email, mobile_primary FROM users WHERE user_id = ? LIMIT 1');
    $uStmt->execute([$userId]);
    $u = $uStmt->fetch() ?: [];

    MelvoreResponse::okFlat([
        'gateway' => 'razorpay',
        'environment' => strtoupper(MelvoreEnv::get('RAZORPAY_ENV', 'LIVE')),
        'key_id' => $keyId,
        'razorpay_order_id' => $rzpOrderId,
        'amount' => $amountInr,
        'amount_paise' => $amountPaise,
        'currency' => 'INR',
        'order_id' => $orderId,
        'order_number' => (string)$order['order_number'],
        'prefill' => [
            'name' => (string)($u['name'] ?? ''),
            'email' => (string)($u['email'] ?? ''),
            'contact' => (string)($u['mobile_primary'] ?? ''),
        ],
    ], 'Payment initiated');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[Melvore][payments/initiate] Exception: ' . $e->getMessage());
    MelvoreResponse::error($e->getMessage() ?: 'Unable to initialize payment.', 'PAYMENT_INIT_ERROR', 500);
}
