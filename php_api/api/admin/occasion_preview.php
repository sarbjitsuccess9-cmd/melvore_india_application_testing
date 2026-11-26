<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';
require_once dirname(__DIR__, 2) . '/services/DesignSystemService.php';

MelvoreResponse::requireMethod('GET', 'POST');

try {
    $pdo = MelvoreDB::pdo();
    // Strictly Admin-Only: never allow public traffic to simulate future dates or unpublished schedules
    MelvoreAuth::requireAdminUserId();

    $body = $_SERVER['REQUEST_METHOD'] === 'POST' ? MelvoreResponse::jsonBody() : $_GET;
    $simulatedDatetime = trim((string)($body['simulated_datetime'] ?? ($body['date'] ?? date('Y-m-d 12:00:00'))));
    $country = trim((string)($body['country'] ?? 'India'));
    $state = trim((string)($body['state'] ?? ''));
    $city = trim((string)($body['city'] ?? ''));
    $worldSlug = strtolower(trim((string)($body['world_slug'] ?? '')));

    $simulation = MelvoreThemeEngineService::simulateOccasionsForAdmin($pdo, $simulatedDatetime, $country, $state, $city);
    $resolvedManifest = MelvoreDesignSystemService::resolvePublishedManifest($pdo);

    $blendedPreview = [
        'layer_order' => ['default_manifest', 'active_design_switch', 'style_world', 'active_occasion'],
        'active_design' => $resolvedManifest['active_design'],
        'design_slot' => $resolvedManifest['resolved_slot'],
        'base_colors' => $resolvedManifest['manifest']['tokens']['colors'] ?? [],
        'world_layout_preserved' => true,
        'world_slug' => $worldSlug !== '' ? $worldSlug : null,
        'world_template' => $worldSlug !== '' ? ($resolvedManifest['manifest']['worlds'][$worldSlug]['template'] ?? 'editorial_magazine') : null,
        'occasion_overlay' => $simulation['active_occasion'] ? [
            'slug' => $simulation['active_occasion']['slug'],
            'name' => $simulation['active_occasion']['name'],
            'accent_color' => $simulation['active_occasion']['accent_color'],
            'motif_key' => $simulation['active_occasion']['motif_key'],
        ] : null,
    ];

    MelvoreResponse::ok([
        'simulated_input' => [
            'datetime' => $simulatedDatetime,
            'country' => $country,
            'state' => $state,
            'city' => $city,
            'world_slug' => $worldSlug,
        ],
        'active_occasion' => $simulation['active_occasion'],
        'active_occasions' => $simulation['active_occasions'],
        'evaluated' => $simulation['evaluated'],
        'blended_preview' => $blendedPreview,
    ], 'Admin occasion simulation evaluated (live storefront untouched)');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to run occasion preview: ' . $e->getMessage(), 'SERVER_ERROR', 500);
}
