<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $activeProducts = (int)$pdo->query('SELECT COUNT(*) FROM products WHERE is_active = 1')->fetchColumn();

    MelvoreResponse::okFlat([
        'status' => 'ok',
        'service' => 'melvore-php-api',
        'active_products' => $activeProducts,
        'timestamp' => gmdate('c'),
    ], 'Healthy');
} catch (Throwable $e) {
    error_log('[Melvore][health] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Database health check failed.', 'HEALTH_ERROR', 503);
}
