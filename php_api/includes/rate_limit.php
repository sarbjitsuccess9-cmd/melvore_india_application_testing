<?php
declare(strict_types=1);

require_once __DIR__ . '/response.php';

/**
 * Lightweight atomic sliding-window rate limiter using system temp directory.
 */
final class MelvoreRateLimit
{
    public static function enforce(string $bucket, int $maxHits, int $windowSeconds): void
    {
        $safeBucket = preg_replace('/[^a-zA-Z0-9_.\-]/', '_', $bucket);
        $ip = preg_replace('/[^a-zA-Z0-9_.:]/', '_', (string)($_SERVER['REMOTE_ADDR'] ?? 'unknown'));
        $file = sys_get_temp_dir() . "/melvore_rl_{$safeBucket}_{$ip}.json";
        $now = time();

        $fh = @fopen($file, 'c+');
        if ($fh === false) {
            return;
        }
        if (flock($fh, LOCK_EX)) {
            $raw = stream_get_contents($fh);
            $hits = $raw ? json_decode($raw, true) : [];
            if (!is_array($hits)) {
                $hits = [];
            }
            $hits = array_values(array_filter($hits, static fn($ts) => is_int($ts) && ($now - $ts) < $windowSeconds));
            if (count($hits) >= $maxHits) {
                flock($fh, LOCK_UN);
                fclose($fh);
                MelvoreResponse::error(
                    'Too many requests. Please wait a moment before trying again.',
                    'RATE_LIMITED',
                    429
                );
            }
            $hits[] = $now;
            ftruncate($fh, 0);
            rewind($fh);
            fwrite($fh, json_encode($hits));
            fflush($fh);
            flock($fh, LOCK_UN);
        }
        fclose($fh);
    }
}
