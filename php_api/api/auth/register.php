<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';
require_once dirname(__DIR__, 2) . '/includes/user_formatter.php';
require_once dirname(__DIR__, 2) . '/services/SessionMergeService.php';

MelvoreResponse::requireMethod('POST');
MelvoreRateLimit::enforce('auth_register', 10, 600);

try {
    $body = MelvoreResponse::jsonBody();
    $name = trim((string)($body['name'] ?? ''));
    $email = strtolower(trim((string)($body['email'] ?? '')));
    $phone = preg_replace('/[^0-9+\- ]/', '', trim((string)($body['phone'] ?? ($body['mobile_primary'] ?? ''))));
    $password = (string)($body['password'] ?? '');

    if (strlen($name) < 2) {
        MelvoreResponse::error('Please enter your full name.', 'VALIDATION_ERROR', 400);
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        MelvoreResponse::error('Please enter a valid email address.', 'VALIDATION_ERROR', 400);
    }
    if (strlen((string)$phone) < 10) {
        MelvoreResponse::error('Please enter a valid phone number.', 'VALIDATION_ERROR', 400);
    }
    if (strlen($password) < 6) {
        MelvoreResponse::error('Password must be at least 6 characters long.', 'VALIDATION_ERROR', 400);
    }

    $pdo = MelvoreDB::pdo();
    $chk = $pdo->prepare('SELECT user_id FROM users WHERE LOWER(email) = ? LIMIT 1');
    $chk->execute([$email]);
    if ($chk->fetch()) {
        MelvoreResponse::error('An account with this email already exists.', 'EMAIL_EXISTS', 409);
    }

    $hash = password_hash($password, PASSWORD_BCRYPT);
    $ins = $pdo->prepare(
        "INSERT INTO users (name, email, password_hash, mobile_primary, remember_token, reason, account_status, social_provider, last_login, created_at)
         VALUES (?, ?, ?, ?, 0, '', 'active', 'none', NOW(), NOW())"
    );
    $ins->execute([$name, $email, $hash, $phone]);
    $uid = (int)$pdo->lastInsertId();

    $uStmt = $pdo->prepare('SELECT * FROM users WHERE user_id = ? LIMIT 1');
    $uStmt->execute([$uid]);
    $user = $uStmt->fetch() ?: ['user_id' => $uid, 'name' => $name, 'email' => $email, 'mobile_primary' => $phone];

    $guestId = MelvoreAuth::getGuestId();
    if ($guestId !== '') {
        MelvoreSessionMergeService::mergeGuestExperienceToUser($pdo, $uid, $guestId);
    }

    $token = MelvoreAuth::issueToken($uid, $hash);

    MelvoreResponse::okFlat([
        'token' => $token,
        'user' => MelvoreUserFormatter::formatUser($user),
    ], 'Account created successfully', 201);
} catch (Throwable $e) {
    error_log('[Melvore][auth/register] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to create account right now.', 'SERVER_ERROR', 500);
}
