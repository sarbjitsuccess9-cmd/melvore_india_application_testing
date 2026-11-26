<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';

MelvoreResponse::requireMethod('GET', 'POST', 'DELETE');

try {
    $pdo = MelvoreDB::pdo();
    MelvoreAuth::requireAdminUserId();
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if ($method === 'GET') {
        MelvoreResponse::ok(['assets' => MelvoreThemeEngineService::fetchWorldAssets($pdo)], 'World vector assets loaded');
    }
    if ($method === 'DELETE') {
        $id = (int)($_GET['id'] ?? (MelvoreResponse::jsonBody()['asset_id'] ?? 0));
        if ($id <= 0) {
            MelvoreResponse::error('Valid asset_id required.', 'VALIDATION_ERROR', 400);
        }
        $pdo->prepare('UPDATE app_world_assets SET is_active = 0 WHERE asset_id = ?')->execute([$id]);
        MelvoreResponse::ok(['deactivated' => true], 'Asset deactivated');
    }

    $body = MelvoreResponse::jsonBody();
    $key = strtolower(trim((string)($body['asset_key'] ?? '')));
    $worldSlug = strtolower(trim((string)($body['world_slug'] ?? '')));
    $vectorType = strtolower(trim((string)($body['vector_type'] ?? '')));
    if ($key === '' || $worldSlug === '' || $vectorType === '') {
        MelvoreResponse::error('asset_key, world_slug, and vector_type are required.', 'VALIDATION_ERROR', 400);
    }

    $meta = isset($body['metadata']) ? json_encode($body['metadata'], JSON_UNESCAPED_UNICODE) : null;
    $stmt = $pdo->prepare(
        'INSERT INTO app_world_assets
            (asset_key, world_slug, occasion_slug, vector_type, layer_role, primary_hex, secondary_hex, opacity, motion_pattern, image_url, metadata_json, display_order, is_active)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE
            world_slug = VALUES(world_slug), occasion_slug = VALUES(occasion_slug), vector_type = VALUES(vector_type),
            layer_role = VALUES(layer_role), primary_hex = VALUES(primary_hex), secondary_hex = VALUES(secondary_hex),
            opacity = VALUES(opacity), motion_pattern = VALUES(motion_pattern), image_url = VALUES(image_url),
            metadata_json = VALUES(metadata_json), display_order = VALUES(display_order), is_active = VALUES(is_active)'
    );
    $stmt->execute([
        $key,
        $worldSlug,
        $body['occasion_slug'] ?? null,
        $vectorType,
        (string)($body['layer_role'] ?? 'ambient_floating'),
        (string)($body['primary_hex'] ?? '#FFFFFF'),
        (string)($body['secondary_hex'] ?? '#D4AF37'),
        max(0.05, min(1.0, (float)($body['opacity'] ?? 0.35))),
        (string)($body['motion_pattern'] ?? 'float_drift'),
        $body['image_url'] ?? null,
        $meta,
        (int)($body['display_order'] ?? 0),
        isset($body['is_active']) ? (!empty($body['is_active']) ? 1 : 0) : 1,
    ]);

    MelvoreResponse::ok(['asset_key' => $key], 'World asset saved');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to manage world assets.', 'SERVER_ERROR', 500);
}
