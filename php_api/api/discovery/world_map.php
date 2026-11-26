<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $origins = MelvoreThemeEngineService::fetchWorldMapOrigins($pdo);
    MelvoreResponse::ok([
        'pins' => $origins,
        'count' => count($origins),
    ], 'Verified brand world map loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load world map origins.', 'SERVER_ERROR', 500);
}
