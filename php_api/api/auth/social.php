<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';
require_once dirname(__DIR__, 2) . '/includes/user_formatter.php';
require_once dirname(__DIR__, 2) . '/services/SessionMergeService.php';

MelvoreResponse::requireMethod('POST');
MelvoreRateLimit::enforce('auth_social', 15, 300);

try {
    $body = MelvoreResponse::jsonBody();
    $provider = strtolower(trim((string)($body['provider'] ?? 'google')));
    $idToken = trim((string)($body['id_token'] ?? ''));

    if ($provider !== 'google' || $idToken === '') {
        MelvoreResponse::error('Valid Google id_token is required.', 'INVALID_SOCIAL_TOKEN', 400);
    }

    $ch = curl_init('https://oauth2.googleapis.com/tokeninfo?id_token=' . rawurlencode($idToken));
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 12,
    ]);
    $raw = curl_exec($ch);
    $http = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    $claims = is_string($raw) ? json_decode($raw, true) : null;
    if ($http !== 200 || !is_array($claims) || empty($claims['email'])) {
        MelvoreResponse::error('Google token verification failed.', 'INVALID_SOCIAL_TOKEN', 401);
    }

    $emailVerified = $claims['email_verified'] ?? false;
    if ($emailVerified !== true && $emailVerified !== 'true' && $emailVerified !== 1 && $emailVerified !== '1') {
        MelvoreResponse::error('Google email address is not verified.', 'UNVERIFIED_EMAIL', 403);
    }

    $configuredClients = array_values(array_filter(array_map('trim', explode(',', MelvoreEnv::get('GOOGLE_CLIENT_IDS', MelvoreEnv::get('GOOGLE_WEB_CLIENT_ID', ''))))));
    if (!empty($configuredClients)) {
        $aud = (string)($claims['aud'] ?? '');
        $azp = (string)($claims['azp'] ?? '');
        if (!in_array($aud, $configuredClients, true) && !in_array($azp, $configuredClients, true)) {
            MelvoreResponse::error('Google client audience mismatch.', 'AUDIENCE_MISMATCH', 403);
        }
    }

    $email = strtolower(trim((string)$claims['email']));
    $googleSub = trim((string)($claims['sub'] ?? ''));
    $name = trim((string)($claims['name'] ?? strstr($email, '@', true) ?: 'Melvore Customer'));
    $avatar = isset($claims['picture']) ? (string)$claims['picture'] : null;

    $pdo = MelvoreDB::pdo();
    $stmt = $pdo->prepare('SELECT * FROM users WHERE LOWER(email) = ? OR (google_id IS NOT NULL AND google_id = ?) LIMIT 1');
    $stmt->execute([$email, $googleSub]);
    $user = $stmt->fetch();

    if (!$user) {
        $randomPass = password_hash(bin2hex(random_bytes(24)), PASSWORD_BCRYPT);
        $ins = $pdo->prepare(
            "INSERT INTO users (name, email, password_hash, mobile_primary, google_id, avatar, email_verified, remember_token, reason, account_status, social_provider, last_login, created_at)
             VALUES (?, ?, ?, '', ?, ?, 1, 0, '', 'active', 'google', NOW(), NOW())"
        );
        $ins->execute([$name, $email, $randomPass, $googleSub !== '' ? $googleSub : null, $avatar]);
        $uid = (int)$pdo->lastInsertId();
    } else {
        if (($user['account_status'] ?? 'active') !== 'active') {
            MelvoreResponse::error('Account is disabled.', 'ACCOUNT_DISABLED', 403);
        }
        $uid = (int)$user['user_id'];
        $pdo->prepare(
            'UPDATE users SET google_id = COALESCE(google_id, ?), avatar = COALESCE(avatar, ?), email_verified = 1, last_login = NOW() WHERE user_id = ?'
        )->execute([$googleSub !== '' ? $googleSub : null, $avatar, $uid]);
    }

    $uStmt = $pdo->prepare('SELECT * FROM users WHERE user_id = ? LIMIT 1');
    $uStmt->execute([$uid]);
    $finalUser = $uStmt->fetch();

    $guestId = MelvoreAuth::getGuestId();
    if ($guestId !== '') {
        MelvoreSessionMergeService::mergeGuestExperienceToUser($pdo, $uid, $guestId);
    }

    $token = MelvoreAuth::issueToken($uid, (string)($finalUser['password_hash'] ?? ''));

    MelvoreResponse::okFlat([
        'token' => $token,
        'user' => MelvoreUserFormatter::formatUser($finalUser),
    ], 'Signed in with Google');
} catch (Throwable $e) {
    error_log('[Melvore][auth/social] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to complete social sign-in.', 'SERVER_ERROR', 500);
}
