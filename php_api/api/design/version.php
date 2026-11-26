<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/services/DesignSystemService.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $resolved = MelvoreDesignSystemService::resolvePublishedManifest($pdo);
    $occasions = MelvoreThemeEngineService::fetchActiveOccasions($pdo);
    MelvoreResponse::ok([
        'active_design' => $resolved['active_design'],
        'slot' => $resolved['resolved_slot'],
        'design_version' => (string)($resolved['manifest']['design_version'] ?? '1.0.0'),
        'schema_version' => (string)($resolved['manifest']['schema_version'] ?? '1.0.0'),
        'checksum_sha256' => $resolved['checksum_sha256'],
        'etag' => $resolved['etag'],
        'config_version' => MelvoreThemeEngineService::computeConfigVersion($pdo),
        'active_occasion_slug' => $occasions[0]['slug'] ?? null,
    ], 'Design version fetched');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to fetch design version.', 'SERVER_ERROR', 500);
}
