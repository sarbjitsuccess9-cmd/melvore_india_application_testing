<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/services/DesignSystemService.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $appSchema = trim((string)($_GET['schema_version'] ?? ($_GET['app_schema_version'] ?? MelvoreDesignSystemService::SUPPORTED_SCHEMA_VERSION)));
    $worldSlug = trim((string)($_GET['world_slug'] ?? ''));
    $country = trim((string)($_GET['country'] ?? 'India'));
    $state = trim((string)($_GET['state'] ?? ''));
    $city = trim((string)($_GET['city'] ?? ''));

    // Public endpoints ALWAYS serve published active_design only (never drafts or simulated dates)
    $resolved = MelvoreDesignSystemService::resolvePublishedManifest($pdo, null, $appSchema);
    $etag = $resolved['etag'];
    header('ETag: ' . $etag);
    header('Cache-Control: public, max-age=30, must-revalidate');

    $ifNoneMatch = trim((string)($_SERVER['HTTP_IF_NONE_MATCH'] ?? ''));
    $unchanged = ($ifNoneMatch !== '' && hash_equals($etag, $ifNoneMatch));

    $occasions = MelvoreThemeEngineService::fetchActiveOccasions($pdo, $country, $state, $city);
    $activeOccasion = $occasions[0] ?? null;

    $worldOverride = null;
    if ($worldSlug !== '') {
        $worldOverride = $resolved['manifest']['worlds'][strtolower($worldSlug)] ?? null;
    }

    MelvoreResponse::ok([
        'active_design' => $resolved['active_design'],
        'slot' => $resolved['resolved_slot'],
        'design_version' => (string)($resolved['manifest']['design_version'] ?? '1.0.0'),
        'schema_version' => (string)($resolved['manifest']['schema_version'] ?? '1.0.0'),
        'min_app_schema_version' => (string)($resolved['manifest']['min_app_schema_version'] ?? '1.0.0'),
        'checksum_sha256' => $resolved['checksum_sha256'],
        'etag' => $etag,
        'unchanged' => $unchanged,
        'fallback_used' => $resolved['fallback_used'],
        'fallback_reason' => $resolved['fallback_reason'],
        'server_time_ist' => (new DateTimeImmutable('now', new DateTimeZone('Asia/Kolkata')))->format('c'),
        'priority_order' => ['default_manifest', 'active_design_switch', 'style_world', 'active_occasion', 'component_override'],
        'active_occasion' => $activeOccasion,
        'world_override' => $worldOverride,
        'manifest' => $unchanged ? null : $resolved['manifest'],
    ], $unchanged ? 'Design manifest unchanged' : 'Active design manifest resolved');
} catch (Throwable $e) {
    error_log('[Melvore][design/manifest] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to resolve design manifest.', 'SERVER_ERROR', 500);
}
