<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';
require_once dirname(__DIR__, 2) . '/includes/user_formatter.php';
require_once dirname(__DIR__, 2) . '/services/SessionMergeService.php';
require_once dirname(__DIR__, 2) . '/services/MailerService.php';

MelvoreResponse::requireMethod('POST');
MelvoreRateLimit::enforce('auth_otp', 12, 300);

try {
    $body = MelvoreResponse::jsonBody();
    $action = strtolower(trim((string)($body['action'] ?? 'send')));
    $email = strtolower(trim((string)($body['email'] ?? '')));

    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        MelvoreResponse::error('Please enter a valid email address.', 'VALIDATION_ERROR', 400);
    }

    $pdo = MelvoreDB::pdo();
    $stmt = $pdo->prepare('SELECT * FROM users WHERE LOWER(email) = ? LIMIT 1');
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    if (!$user || ($user['account_status'] ?? 'active') !== 'active') {
        MelvoreResponse::error('No active Melvore account found for this email.', 'USER_NOT_FOUND', 404);
    }
    $uid = (int)$user['user_id'];

    if ($action === 'send') {
        $otp = str_pad((string)random_int(0, 999999), 6, '0', STR_PAD_LEFT);
        $expires = date('Y-m-d H:i:s', time() + 600);
        $pdo->prepare('UPDATE users SET otp = ?, otp_expires = ? WHERE user_id = ?')->execute([$otp, $expires, $uid]);
        MelvoreMailerService::sendOtpEmail($email, $otp, 'Sign-In');

        MelvoreResponse::okFlat([
            'sent' => true,
            'expires_in_seconds' => 600,
        ], 'Verification OTP sent to your email.');
    }

    if ($action === 'verify') {
        $otpInput = trim((string)($body['otp'] ?? ''));
        if ($otpInput === '' || (string)($user['otp'] ?? '') === '') {
            MelvoreResponse::error('Please enter the 6-digit OTP code.', 'INVALID_OTP', 400);
        }
        $expTs = !empty($user['otp_expires']) ? strtotime((string)$user['otp_expires']) : 0;
        if ($expTs < time()) {
            MelvoreResponse::error('This OTP code has expired. Please request a new one.', 'OTP_EXPIRED', 400);
        }
        if (!hash_equals((string)$user['otp'], $otpInput)) {
            MelvoreResponse::error('Incorrect OTP code.', 'INVALID_OTP', 401);
        }

        $pdo->prepare('UPDATE users SET otp = NULL, otp_expires = NULL, email_verified = 1, last_login = NOW() WHERE user_id = ?')
            ->execute([$uid]);

        $guestId = MelvoreAuth::getGuestId();
        if ($guestId !== '') {
            MelvoreSessionMergeService::mergeGuestExperienceToUser($pdo, $uid, $guestId);
        }

        $token = MelvoreAuth::issueToken($uid, (string)($user['password_hash'] ?? ''));
        MelvoreResponse::okFlat([
            'token' => $token,
            'user' => MelvoreUserFormatter::formatUser($user),
        ], 'OTP verified and signed in');
    }

    MelvoreResponse::error('Unsupported OTP action.', 'INVALID_ACTION', 400);
} catch (Throwable $e) {
    error_log('[Melvore][auth/otp] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to process OTP request.', 'SERVER_ERROR', 500);
}
