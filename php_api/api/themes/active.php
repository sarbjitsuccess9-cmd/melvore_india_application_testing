<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $worldSlug = trim((string)($_GET['world_slug'] ?? ''));
    $default = MelvoreThemeEngineService::loadDefaultTheme($pdo);
    $resolvedDesign = MelvoreDesignSystemService::resolvePublishedManifest($pdo);
    $occasions = MelvoreThemeEngineService::fetchActiveOccasions(
        $pdo,
        trim((string)($_GET['country'] ?? 'India')),
        trim((string)($_GET['state'] ?? '')),
        trim((string)($_GET['city'] ?? ''))
    );

    $selectedWorld = null;
    if ($worldSlug !== '' && MelvoreDB::tableExists('app_style_worlds')) {
        $stmt = $pdo->prepare('SELECT * FROM app_style_worlds WHERE slug = ? AND is_active = 1 LIMIT 1');
        $stmt->execute([$worldSlug]);
        $row = $stmt->fetch();
        if ($row) {
            $selectedWorld = MelvoreThemeEngineService::formatWorldTheme($row);
        }
    }

    MelvoreResponse::ok([
        'config_version' => MelvoreThemeEngineService::computeConfigVersion($pdo),
        'active_design' => $resolvedDesign['active_design'],
        'design_slot' => $resolvedDesign['resolved_slot'],
        'design_version' => (string)($resolvedDesign['manifest']['design_version'] ?? '1.0.0'),
        'default_theme' => $default,
        'active_occasion' => $occasions[0] ?? null,
        'selected_world' => $selectedWorld,
        'world_override' => $worldSlug !== '' ? ($resolvedDesign['manifest']['worlds'][strtolower($worldSlug)] ?? null) : null,
        'layer_priority' => ['default_manifest', 'active_design_switch', 'style_world', 'active_occasion', 'component_override'],
    ], 'Active theme resolved');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to resolve active theme.', 'SERVER_ERROR', 500);
}
