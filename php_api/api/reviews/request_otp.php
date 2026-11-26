<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';

MelvoreResponse::requireMethod('POST');
$userId = MelvoreAuth::requireUserId();
MelvoreRateLimit::enforce('review_otp_' . $userId, 5, 300);

try {
    $pdo = MelvoreDB::pdo();
    $body = MelvoreResponse::jsonBody();
    $orderItemId = (int)($body['order_item_id'] ?? 0);

    if ($orderItemId <= 0) {
        MelvoreResponse::error('Valid order_item_id is required.', 'VALIDATION_ERROR', 422);
    }

    // Verify ownership strictly from authenticated user_id + order_items + orders
    $oStmt = $pdo->prepare(
        "SELECT oi.order_item_id, oi.product_id, o.order_id, o.order_status, u.email, u.name
         FROM order_items oi
         INNER JOIN orders o ON o.order_id = oi.order_id
         INNER JOIN users u ON u.user_id = o.user_id
         LEFT JOIN reviews r ON r.order_item_id = oi.order_item_id AND r.user_id = o.user_id
         WHERE oi.order_item_id = ? AND o.user_id = ? AND o.order_status <> 'cancelled'
         LIMIT 1"
    );
    $oStmt->execute([$orderItemId, $userId]);
    $item = $oStmt->fetch();
    if (!$item) {
        MelvoreResponse::error('Purchased order item not found for your account.', 'FORBIDDEN', 403);
    }

    $dupStmt = $pdo->prepare('SELECT review_id FROM reviews WHERE order_item_id = ? AND user_id = ? LIMIT 1');
    $dupStmt->execute([$orderItemId, $userId]);
    if ($dupStmt->fetch()) {
        MelvoreResponse::error('You have already reviewed this purchased item.', 'ALREADY_REVIEWED', 409);
    }

    if (!MelvoreDB::tableExists('app_review_otp_requests')) {
        MelvoreResponse::error('Review OTP verification is not configured on the server.', 'SERVER_UNAVAILABLE', 503);
    }

    // Enforce 60-second resend cooldown
    $cdStmt = $pdo->prepare(
        'SELECT otp_request_id, created_at
         FROM app_review_otp_requests
         WHERE user_id = ? AND order_item_id = ? AND created_at > DATE_SUB(NOW(), INTERVAL 60 SECOND)
         ORDER BY otp_request_id DESC
         LIMIT 1'
    );
    $cdStmt->execute([$userId, $orderItemId]);
    if ($cdStmt->fetch()) {
        MelvoreResponse::error('Please wait 60 seconds before requesting another verification code.', 'COOLDOWN_ACTIVE', 429);
    }

    // Generate cryptographically random 6-digit OTP and store ONLY bcrypt hash
    $otpPlain = str_pad((string)random_int(0, 999999), 6, '0', STR_PAD_LEFT);
    $otpHash = password_hash($otpPlain, PASSWORD_BCRYPT);

    $ins = $pdo->prepare(
        'INSERT INTO app_review_otp_requests
         (user_id, product_id, order_item_id, otp_hash, attempt_count, is_verified, expires_at, created_at)
         VALUES (?, ?, ?, ?, 0, 0, DATE_ADD(NOW(), INTERVAL 10 MINUTE), NOW())'
    );
    $ins->execute([$userId, (int)$item['product_id'], $orderItemId, $otpHash]);
    $requestId = (int)$pdo->lastInsertId();

    // Dispatch email via configured mail transport (never return plaintext OTP in JSON)
    $email = (string)($item['email'] ?? '');
    if ($email !== '') {
        $subject = 'Melvore Verified Purchase Review Code';
        $message = "Your Melvore review verification code is: $otpPlain\nThis code expires in 10 minutes.";
        $from = MelvoreEnv::get('MAIL_FROM_ADDRESS', 'noreply@melvore.co.in');
        @mail($email, $subject, $message, "From: Melvore <$from>\r\nContent-Type: text/plain; charset=UTF-8");
    }

    $maskedEmail = $email !== '' ? preg_replace('/(^.).*(@.*$)/', '$1***$2', $email) : 'your registered email';

    MelvoreResponse::ok([
        'otp_request_id' => $requestId,
        'order_item_id' => $orderItemId,
        'product_id' => (int)$item['product_id'],
        'expires_in_seconds' => 600,
        'resend_cooldown_seconds' => 60,
        'sent_to' => $maskedEmail,
    ], "Verification code sent to $maskedEmail");
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to issue review verification code.', 'SERVER_ERROR', 500);
}
