<?php
declare(strict_types=1);

require_once __DIR__ . '/db.php';
require_once __DIR__ . '/response.php';

/**
 * Bearer HMAC-SHA256 & api_tokens authentication middleware + X-Guest-Id resolver + admin role enforcement.
 */
final class MelvoreAuth
{
    public static function getGuestId(): string
    {
        $raw = $_SERVER['HTTP_X_GUEST_ID'] ?? ($_GET['guest_id'] ?? '');
        $clean = preg_replace('/[^a-zA-Z0-9_\-]/', '', (string)$raw);
        return substr((string)$clean, 0, 64);
    }

    public static function getBearerToken(): ?string
    {
        $header = $_SERVER['HTTP_AUTHORIZATION']
            ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
            ?? '';
        if ($header === '' && function_exists('apache_request_headers')) {
            $headers = apache_request_headers();
            foreach ($headers as $k => $v) {
                if (strcasecmp((string)$k, 'Authorization') === 0) {
                    $header = (string)$v;
                    break;
                }
            }
        }
        if (preg_match('/Bearer\s+(.+)$/i', trim($header), $matches)) {
            $token = trim($matches[1]);
            return $token !== '' ? $token : null;
        }
        return null;
    }

    public static function issueToken(int $userId, string $passwordHash = '', ?int $ttlSeconds = null): string
    {
        $ttl = $ttlSeconds ?? MelvoreEnv::tokenTtlSeconds();
        $now = time();
        $exp = $now + $ttl;
        $secret = MelvoreEnv::tokenSecret();
        $pvf = $passwordHash !== '' ? substr(hash('sha256', $passwordHash), 0, 12) : '';

        if ($secret !== '') {
            $claims = [
                'user_id' => $userId,
                'iat' => $now,
                'exp' => $exp,
                'jti' => bin2hex(random_bytes(8)),
                'pvf' => $pvf,
            ];
            $payloadB64 = rtrim(strtr(base64_encode((string)json_encode($claims)), '+/', '-_'), '=');
            $sigB64 = rtrim(strtr(base64_encode(hash_hmac('sha256', $payloadB64, $secret, true)), '+/', '-_'), '=');
            $token = $payloadB64 . '.' . $sigB64;
        } else {
            $token = 'mlv_' . bin2hex(random_bytes(32));
        }

        if (MelvoreDB::tableExists('api_tokens')) {
            $pdo = MelvoreDB::pdo();
            $hash = hash('sha256', $token);
            $ua = substr((string)($_SERVER['HTTP_USER_AGENT'] ?? 'android'), 0, 250);
            $ip = substr((string)($_SERVER['REMOTE_ADDR'] ?? ''), 0, 64);
            $expiresAt = date('Y-m-d H:i:s', $exp);
            $stmt = $pdo->prepare(
                'INSERT INTO api_tokens (user_id, token_hash, device_label, ip_address, expires_at)
                 VALUES (?, ?, ?, ?, ?)'
            );
            $stmt->execute([$userId, $hash, $ua, $ip, $expiresAt]);
        }

        return $token;
    }

    public static function revokeToken(?string $token): void
    {
        if ($token === null || $token === '') {
            return;
        }
        if (MelvoreDB::tableExists('api_tokens')) {
            $hash = hash('sha256', $token);
            $stmt = MelvoreDB::pdo()->prepare('UPDATE api_tokens SET revoked_at = NOW() WHERE token_hash = ? AND revoked_at IS NULL');
            $stmt->execute([$hash]);
        }
    }

    public static function getCurrentUserId(): ?int
    {
        $token = self::getBearerToken();
        if ($token === null) {
            return null;
        }

        $secret = MelvoreEnv::tokenSecret();
        $pdo = MelvoreDB::pdo();

        if ($secret !== '' && str_contains($token, '.')) {
            $parts = explode('.', $token, 2);
            if (count($parts) === 2) {
                [$payloadB64, $sigB64] = $parts;
                $expected = rtrim(strtr(base64_encode(hash_hmac('sha256', $payloadB64, $secret, true)), '+/', '-_'), '=');
                if (hash_equals($expected, $sigB64)) {
                    $jsonStr = base64_decode(strtr($payloadB64, '-_', '+/'), true);
                    $claims = $jsonStr !== false ? json_decode($jsonStr, true) : null;
                    if (is_array($claims) && isset($claims['user_id'])) {
                        $uid = (int)$claims['user_id'];
                        $exp = (int)($claims['exp'] ?? 0);
                        if ($uid > 0 && ($exp === 0 || $exp >= time())) {
                            if (MelvoreDB::tableExists('api_tokens')) {
                                $hash = hash('sha256', $token);
                                $stmt = $pdo->prepare(
                                    'SELECT token_id, revoked_at, expires_at FROM api_tokens WHERE user_id = ? AND token_hash = ? LIMIT 1'
                                );
                                $stmt->execute([$uid, $hash]);
                                $row = $stmt->fetch();
                                if ($row && (!empty($row['revoked_at']) || (!empty($row['expires_at']) && strtotime((string)$row['expires_at']) < time()))) {
                                    return null;
                                }
                            }
                            $uStmt = $pdo->prepare('SELECT user_id, password_hash, account_status FROM users WHERE user_id = ? LIMIT 1');
                            $uStmt->execute([$uid]);
                            $uRow = $uStmt->fetch();
                            if (!$uRow || (($uRow['account_status'] ?? 'active') !== 'active')) {
                                return null;
                            }
                            if (isset($claims['pvf']) && $claims['pvf'] !== '') {
                                $pvf = substr(hash('sha256', (string)$uRow['password_hash']), 0, 12);
                                if (!hash_equals($pvf, (string)$claims['pvf'])) {
                                    return null;
                                }
                            }
                            return $uid;
                        }
                    }
                }
            }
        }

        if (MelvoreDB::tableExists('api_tokens')) {
            $hash = hash('sha256', $token);
            $stmt = $pdo->prepare(
                'SELECT t.user_id FROM api_tokens t
                 INNER JOIN users u ON u.user_id = t.user_id
                 WHERE t.token_hash = ? AND t.revoked_at IS NULL
                   AND (t.expires_at IS NULL OR t.expires_at > NOW())
                   AND u.account_status = "active"
                 LIMIT 1'
            );
            $stmt->execute([$hash]);
            $uid = $stmt->fetchColumn();
            return $uid !== false ? (int)$uid : null;
        }

        return null;
    }

    public static function requireUserId(): int
    {
        $uid = self::getCurrentUserId();
        if ($uid === null || $uid <= 0) {
            MelvoreResponse::error(
                'Authentication required. Please sign in to continue.',
                'UNAUTHORIZED',
                401
            );
        }
        return $uid;
    }

    public static function requireAdminUserId(): int
    {
        $uid = self::requireUserId();
        $pdo = MelvoreDB::pdo();
        if (!MelvoreDB::columnExists('users', 'role')) {
            MelvoreResponse::error('Admin role configuration is not enabled in database.', 'FORBIDDEN', 403);
        }
        $stmt = $pdo->prepare('SELECT role FROM users WHERE user_id = ? AND account_status = "active" LIMIT 1');
        $stmt->execute([$uid]);
        $role = strtolower(trim((string)$stmt->fetchColumn()));
        if (!in_array($role, ['admin', 'superadmin', 'merchandiser'], true)) {
            MelvoreResponse::error('Administrator access required.', 'FORBIDDEN', 403);
        }
        return $uid;
    }
}
