<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';
require_once dirname(__DIR__, 2) . '/includes/user_formatter.php';
require_once dirname(__DIR__, 2) . '/services/SessionMergeService.php';

MelvoreResponse::requireMethod('POST');
MelvoreRateLimit::enforce('auth_login', 15, 300);

try {
    $body = MelvoreResponse::jsonBody();
    $email = strtolower(trim((string)($body['email'] ?? '')));
    $password = (string)($body['password'] ?? '');

    if ($email === '' || !filter_var($email, FILTER_VALIDATE_EMAIL) || $password === '') {
        MelvoreResponse::error('Please provide a valid email and password.', 'INVALID_CREDENTIALS', 400);
    }

    $pdo = MelvoreDB::pdo();
    $stmt = $pdo->prepare('SELECT * FROM users WHERE LOWER(email) = ? LIMIT 1');
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    if (!$user || !password_verify($password, (string)($user['password_hash'] ?? ''))) {
        MelvoreResponse::error('Invalid email or password.', 'INVALID_CREDENTIALS', 401);
    }
    if (($user['account_status'] ?? 'active') !== 'active') {
        MelvoreResponse::error('Your account is currently disabled. Please contact support.', 'ACCOUNT_DISABLED', 403);
    }

    $uid = (int)$user['user_id'];
    $pdo->prepare('UPDATE users SET last_login = NOW() WHERE user_id = ?')->execute([$uid]);

    $guestId = MelvoreAuth::getGuestId();
    if ($guestId !== '') {
        MelvoreSessionMergeService::mergeGuestExperienceToUser($pdo, $uid, $guestId);
    }

    $token = MelvoreAuth::issueToken($uid, (string)($user['password_hash'] ?? ''));

    MelvoreResponse::okFlat([
        'token' => $token,
        'user' => MelvoreUserFormatter::formatUser($user),
    ], 'Signed in successfully');
} catch (Throwable $e) {
    error_log('[Melvore][auth/login] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to sign in right now.', 'SERVER_ERROR', 500);
}
