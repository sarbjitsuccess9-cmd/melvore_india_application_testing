<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';

MelvoreResponse::requireMethod('POST');
$userId = MelvoreAuth::requireUserId();
MelvoreRateLimit::enforce('review_verify_' . $userId, 15, 300);

try {
    $pdo = MelvoreDB::pdo();
    $body = MelvoreResponse::jsonBody();
    $requestId = (int)($body['otp_request_id'] ?? 0);
    $otp = preg_replace('/[^0-9]/', '', (string)($body['otp'] ?? ''));

    if ($requestId <= 0 || strlen($otp) !== 6) {
        MelvoreResponse::error('Valid otp_request_id and 6-digit OTP code are required.', 'VALIDATION_ERROR', 422);
    }

    $pdo->beginTransaction();

    $stmt = $pdo->prepare(
        'SELECT * FROM app_review_otp_requests
         WHERE otp_request_id = ? AND user_id = ?
         FOR UPDATE'
    );
    $stmt->execute([$requestId, $userId]);
    $req = $stmt->fetch();

    if (!$req) {
        $pdo->rollBack();
        MelvoreResponse::error('OTP request not found.', 'NOT_FOUND', 404);
    }
    if ((int)$req['attempt_count'] >= 5) {
        $pdo->rollBack();
        MelvoreResponse::error('Maximum verification attempts exceeded. Please request a new code.', 'OTP_LOCKED', 429);
    }
    if (strtotime((string)$req['expires_at']) < time()) {
        $pdo->rollBack();
        MelvoreResponse::error('Verification code has expired. Please request a new code.', 'OTP_EXPIRED', 410);
    }

    if (!password_verify($otp, (string)$req['otp_hash'])) {
        $pdo->prepare('UPDATE app_review_otp_requests SET attempt_count = attempt_count + 1 WHERE otp_request_id = ?')
            ->execute([$requestId]);
        $pdo->commit();
        MelvoreResponse::error('Invalid verification code.', 'INVALID_OTP', 422);
    }

    $pdo->prepare('UPDATE app_review_otp_requests SET is_verified = 1, verified_at = NOW() WHERE otp_request_id = ?')
        ->execute([$requestId]);
    $pdo->commit();

    MelvoreResponse::ok([
        'otp_request_id' => $requestId,
        'verified' => true,
        'order_item_id' => (int)$req['order_item_id'],
        'product_id' => (int)$req['product_id'],
    ], 'Purchase verification code confirmed');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    MelvoreResponse::error('Unable to verify code.', 'SERVER_ERROR', 500);
}
