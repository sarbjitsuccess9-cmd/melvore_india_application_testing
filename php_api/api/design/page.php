<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/services/DesignSystemService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $pageKey = strtolower(trim((string)($_GET['page_key'] ?? 'home')));
    $resolved = MelvoreDesignSystemService::resolvePublishedManifest($pdo);
    $page = $resolved['manifest']['pages'][$pageKey] ?? null;
    if ($page === null) {
        MelvoreResponse::error("Page design '{$pageKey}' not found.", 'NOT_FOUND', 404);
    }
    MelvoreResponse::ok([
        'active_design' => $resolved['active_design'],
        'slot' => $resolved['resolved_slot'],
        'design_version' => (string)($resolved['manifest']['design_version'] ?? '1.0.0'),
        'page_key' => $pageKey,
        'page' => $page,
    ], 'Page layout configuration loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load page design.', 'SERVER_ERROR', 500);
}
