<?php
declare(strict_types=1);

/**
 * Melvore PHP 8.3 API Environment Loader
 * Reads configuration from process env or /php_api/.env (protected by .htaccess).
 */
final class MelvoreEnv
{
    private static ?array $cache = null;

    public static function all(): array
    {
        if (self::$cache !== null) {
            return self::$cache;
        }
        $vars = [];
        $candidates = [
            dirname(__DIR__) . '/.env',
            dirname(__DIR__, 2) . '/.env',
        ];
        foreach ($candidates as $path) {
            if (is_file($path) && is_readable($path)) {
                $lines = file($path, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
                if ($lines !== false) {
                    foreach ($lines as $line) {
                        $trimmed = trim($line);
                        if ($trimmed === '' || str_starts_with($trimmed, '#') || !str_contains($trimmed, '=')) {
                            continue;
                        }
                        [$k, $v] = explode('=', $trimmed, 2);
                        $key = trim($k);
                        $val = trim($v);
                        if (
                            (str_starts_with($val, '"') && str_ends_with($val, '"')) ||
                            (str_starts_with($val, "'") && str_ends_with($val, "'"))
                        ) {
                            $val = substr($val, 1, -1);
                        }
                        if ($key !== '' && !array_key_exists($key, $vars)) {
                            $vars[$key] = $val;
                        }
                    }
                }
            }
        }
        self::$cache = $vars;
        return self::$cache;
    }

    public static function get(string $key, string $default = ''): string
    {
        $env = getenv($key);
        if ($env !== false && $env !== '') {
            return (string)$env;
        }
        if (isset($_ENV[$key]) && $_ENV[$key] !== '') {
            return (string)$_ENV[$key];
        }
        $all = self::all();
        return isset($all[$key]) && $all[$key] !== '' ? (string)$all[$key] : $default;
    }

    public static function getInt(string $key, int $default): int
    {
        $raw = self::get($key, '');
        return ($raw !== '' && is_numeric($raw)) ? (int)$raw : $default;
    }

    public static function getBool(string $key, bool $default = false): bool
    {
        $raw = strtolower(trim(self::get($key, '')));
        if ($raw === '') {
            return $default;
        }
        return in_array($raw, ['1', 'true', 'yes', 'on'], true);
    }

    public static function tokenSecret(): string
    {
        return self::get(
            'API_TOKEN_SECRET',
            self::get('AUTH_TOKEN_SECRET', self::get('APP_SECRET', ''))
        );
    }

    public static function tokenTtlSeconds(): int
    {
        return max(300, self::getInt('API_TOKEN_TTL_SECONDS', 2592000));
    }

    public static function assetBaseUrl(): string
    {
        return rtrim(
            self::get('ASSET_BASE_URL', self::get('STOREFRONT_BASE_URL', 'https://melvore.co.in')),
            '/'
        );
    }

    public static function mailFrom(): string
    {
        return self::get('MAIL_FROM', self::get('MAIL_FROM_ADDRESS', 'connect@melvore.co.in'));
    }

    public static function allowedCorsOrigins(): array
    {
        $defaults = ['https://melvore.co.in', 'https://app.melvore.co.in'];
        $raw = self::get('CORS_ALLOWED_ORIGINS', self::get('CORS_ALLOWED_ORIGIN', ''));
        if ($raw !== '') {
            foreach (explode(',', $raw) as $part) {
                $clean = rtrim(trim($part), '/');
                if ($clean !== '' && !in_array($clean, $defaults, true)) {
                    $defaults[] = $clean;
                }
            }
        }
        return $defaults;
    }
}
