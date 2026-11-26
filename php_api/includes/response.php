<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/config/env.php';

/**
 * Consistent JSON response, security headers, CORS, HTTPS check, and URL resolution helpers.
 */
final class MelvoreResponse
{
    public static function initCorsAndHeaders(): void
    {
        header('Content-Type: application/json; charset=utf-8');
        header('X-Content-Type-Options: nosniff');
        header('X-Frame-Options: DENY');
        header('Referrer-Policy: strict-origin-when-cross-origin');

        $allowedOrigins = MelvoreEnv::allowedCorsOrigins();
        if (isset($_SERVER['HTTP_ORIGIN']) && $_SERVER['HTTP_ORIGIN'] !== '') {
            $origin = rtrim((string)$_SERVER['HTTP_ORIGIN'], '/');
            if (in_array($origin, $allowedOrigins, true)) {
                header('Access-Control-Allow-Origin: ' . $origin);
                header('Vary: Origin');
            }
        }
        header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Guest-Id, Idempotency-Key, X-Razorpay-Signature');
        header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
        if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
            http_response_code(204);
            exit;
        }
    }

    public static function enforceHttpsIfConfigured(): void
    {
        if (!MelvoreEnv::getBool('REQUIRE_HTTPS', false)) {
            return;
        }
        if (strtolower(MelvoreEnv::get('APP_ENV', 'production')) !== 'production') {
            return;
        }
        $https = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
            || (strtolower((string)($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '')) === 'https')
            || ((int)($_SERVER['SERVER_PORT'] ?? 0) === 443);
        if (!$https && php_sapi_name() !== 'cli') {
            self::error('HTTPS connection is required.', 'HTTPS_REQUIRED', 426);
        }
    }

    public static function ok(mixed $data = [], string $message = 'Success', int $status = 200): never
    {
        self::initCorsAndHeaders();
        http_response_code($status);
        echo json_encode([
            'success' => true,
            'message' => $message,
            'data' => $data,
        ], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
        exit;
    }

    /**
     * Emits BOTH top-level keys (for MelvoreApiService DTOs) AND `'data'` envelope
     * (for ApiEnvelope consumers).
     */
    public static function okFlat(array $payload, string $message = 'Success', int $status = 200): never
    {
        self::initCorsAndHeaders();
        http_response_code($status);
        $out = array_merge([
            'success' => true,
            'message' => $message,
        ], $payload);
        $out['data'] = $payload;
        echo json_encode($out, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
        exit;
    }

    public static function error(string $message, string $errorCode = 'BAD_REQUEST', int $status = 400, mixed $data = null): never
    {
        self::initCorsAndHeaders();
        http_response_code($status);
        $payload = [
            'success' => false,
            'error_code' => $errorCode,
            'message' => $message,
        ];
        if ($data !== null) {
            $payload['data'] = $data;
        }
        echo json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
        exit;
    }

    public static function requireMethod(string ...$methods): void
    {
        self::initCorsAndHeaders();
        self::enforceHttpsIfConfigured();
        $current = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
        $allowed = array_map('strtoupper', $methods);
        if (!in_array($current, $allowed, true)) {
            self::error('Method not allowed.', 'METHOD_NOT_ALLOWED', 405);
        }
    }

    public static function jsonBody(): array
    {
        $raw = file_get_contents('php://input');
        if ($raw === false || trim($raw) === '') {
            return [];
        }
        $decoded = json_decode($raw, true);
        return is_array($decoded) ? $decoded : [];
    }

    public static function rawBody(): string
    {
        $raw = file_get_contents('php://input');
        return $raw === false ? '' : $raw;
    }

    public static function mediaUrl(?string $raw): ?string
    {
        if ($raw === null) {
            return null;
        }
        $trimmed = trim($raw);
        if ($trimmed === '') {
            return null;
        }
        if (str_starts_with($trimmed, 'http://') || str_starts_with($trimmed, 'https://')) {
            return $trimmed;
        }
        if (str_starts_with($trimmed, '//')) {
            return 'https:' . $trimmed;
        }
        $base = MelvoreEnv::assetBaseUrl();
        return str_starts_with($trimmed, '/') ? ($base . $trimmed) : ($base . '/' . $trimmed);
    }
}
