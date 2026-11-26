<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';
require_once dirname(__DIR__, 2) . '/services/MailerService.php';

MelvoreResponse::requireMethod('POST');
MelvoreRateLimit::enforce('auth_forgot', 8, 600);

try {
    $body = MelvoreResponse::jsonBody();
    $action = strtolower(trim((string)($body['action'] ?? 'request')));
    $email = strtolower(trim((string)($body['email'] ?? '')));

    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        MelvoreResponse::error('Please enter a valid email address.', 'VALIDATION_ERROR', 400);
    }

    $pdo = MelvoreDB::pdo();
    $stmt = $pdo->prepare('SELECT user_id, otp, otp_expires, account_status FROM users WHERE LOWER(email) = ? LIMIT 1');
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    if ($action === 'request') {
        if ($user && ($user['account_status'] ?? 'active') === 'active') {
            $otp = str_pad((string)random_int(0, 999999), 6, '0', STR_PAD_LEFT);
            $expires = date('Y-m-d H:i:s', time() + 600);
            $pdo->prepare('UPDATE users SET otp = ?, otp_expires = ? WHERE user_id = ?')
                ->execute([$otp, $expires, (int)$user['user_id']]);
            MelvoreMailerService::sendOtpEmail($email, $otp, 'Password Reset');
        }
        MelvoreResponse::okFlat([], 'If an account matches this email, a verification code has been sent.');
    }

    if ($action === 'verify') {
        $otpInput = trim((string)($body['otp'] ?? ''));
        $newPass = (string)($body['new_password'] ?? ($body['password'] ?? ''));
        if (strlen($newPass) < 6) {
            MelvoreResponse::error('New password must be at least 6 characters long.', 'VALIDATION_ERROR', 400);
        }
        if (!$user || ($user['account_status'] ?? 'active') !== 'active') {
            MelvoreResponse::error('Invalid reset request.', 'INVALID_RESET', 400);
        }
        $expTs = !empty($user['otp_expires']) ? strtotime((string)$user['otp_expires']) : 0;
        if ($otpInput === '' || (string)($user['otp'] ?? '') === '' || $expTs < time() || !hash_equals((string)$user['otp'], $otpInput)) {
            MelvoreResponse::error('Invalid or expired verification code.', 'INVALID_OTP', 400);
        }

        $newHash = password_hash($newPass, PASSWORD_BCRYPT);
        $uid = (int)$user['user_id'];
        $pdo->prepare('UPDATE users SET password_hash = ?, otp = NULL, otp_expires = NULL WHERE user_id = ?')
            ->execute([$newHash, $uid]);
        if (MelvoreDB::tableExists('api_tokens')) {
            $pdo->prepare('UPDATE api_tokens SET revoked_at = NOW() WHERE user_id = ? AND revoked_at IS NULL')->execute([$uid]);
        }

        MelvoreResponse::okFlat([], 'Password updated successfully. Please sign in with your new password.');
    }

    MelvoreResponse::error('Unsupported password reset action.', 'INVALID_ACTION', 400);
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to reset password right now.', 'SERVER_ERROR', 500);
}
