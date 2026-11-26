<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/config/env.php';

/**
 * Shared PDO database singleton & schema introspection helper for u205121999_melvore.
 */
final class MelvoreDB
{
    private static ?PDO $pdo = null;
    private static array $tableCache = [];
    private static array $columnCache = [];

    public static function pdo(): PDO
    {
        if (self::$pdo !== null) {
            return self::$pdo;
        }

        $host = MelvoreEnv::get('DB_HOST', '127.0.0.1');
        $port = MelvoreEnv::get('DB_PORT', '3306');
        $name = MelvoreEnv::get('DB_NAME', 'u205121999_melvore');
        $user = MelvoreEnv::get('DB_USER', '');
        $pass = MelvoreEnv::get('DB_PASS', '');
        $charset = MelvoreEnv::get('DB_CHARSET', 'utf8mb4');

        if ($user === '') {
            throw new RuntimeException('Database credentials are not configured.');
        }

        $dsn = sprintf('mysql:host=%s;port=%s;dbname=%s;charset=%s', $host, $port, $name, $charset);
        self::$pdo = new PDO($dsn, $user, $pass, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]);
        self::$pdo->exec("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
        return self::$pdo;
    }

    public static function tableExists(string $table): bool
    {
        $clean = preg_replace('/[^a-zA-Z0-9_]/', '', $table);
        if ($clean === '') {
            return false;
        }
        if (isset(self::$tableCache[$clean])) {
            return self::$tableCache[$clean];
        }
        $stmt = self::pdo()->prepare(
            'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?'
        );
        $stmt->execute([$clean]);
        self::$tableCache[$clean] = ((int)$stmt->fetchColumn()) > 0;
        return self::$tableCache[$clean];
    }

    public static function columnExists(string $table, string $column): bool
    {
        $t = preg_replace('/[^a-zA-Z0-9_]/', '', $table);
        $c = preg_replace('/[^a-zA-Z0-9_]/', '', $column);
        if ($t === '' || $c === '') {
            return false;
        }
        $key = "$t.$c";
        if (isset(self::$columnCache[$key])) {
            return self::$columnCache[$key];
        }
        $stmt = self::pdo()->prepare(
            'SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?'
        );
        $stmt->execute([$t, $c]);
        self::$columnCache[$key] = ((int)$stmt->fetchColumn()) > 0;
        return self::$columnCache[$key];
    }

    public static function columnMaxLength(string $table, string $column): ?int
    {
        $t = preg_replace('/[^a-zA-Z0-9_]/', '', $table);
        $c = preg_replace('/[^a-zA-Z0-9_]/', '', $column);
        if ($t === '' || $c === '') {
            return null;
        }
        $stmt = self::pdo()->prepare(
            'SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?'
        );
        $stmt->execute([$t, $c]);
        $val = $stmt->fetchColumn();
        return ($val !== false && $val !== null) ? (int)$val : null;
    }
}
