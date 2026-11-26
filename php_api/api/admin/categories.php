<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('GET', 'POST', 'PUT');

try {
    $pdo = MelvoreDB::pdo();
    MelvoreAuth::requireAdminUserId();
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if ($method === 'GET') {
        $hasEd = MelvoreDB::tableExists('app_category_editorial_configs');
        $sql = $hasEd
            ? "SELECT c.category_id, c.name, c.slug, c.description, c.image, c.display_order AS base_order,
                      ec.config_id, ec.motif_key, ec.editorial_badge, ec.editorial_subtitle, ec.accent_hex,
                      ec.bg_hex, ec.animation_key, ec.tile_span, ec.cover_image_url,
                      COALESCE(ec.display_order, c.display_order) AS display_order,
                      COALESCE(ec.is_active, 1) AS is_active
               FROM categories c
               LEFT JOIN app_category_editorial_configs ec ON ec.category_id = c.category_id
               WHERE c.status = 'active'
               ORDER BY display_order ASC, c.name ASC"
            : "SELECT c.category_id, c.name, c.slug, c.description, c.image, c.display_order
               FROM categories c WHERE c.status = 'active' ORDER BY c.display_order ASC, c.name ASC";

        $rows = [];
        foreach ($pdo->query($sql)->fetchAll() as $r) {
            $rows[] = [
                'category_id' => (int)$r['category_id'],
                'name' => (string)$r['name'],
                'slug' => (string)$r['slug'],
                'image_url' => MelvoreResponse::mediaUrl($r['image'] ?? null),
                'cover_image_url' => MelvoreResponse::mediaUrl($r['cover_image_url'] ?? ($r['image'] ?? null)),
                'motif_key' => (string)($r['motif_key'] ?? 'editorial_frame'),
                'editorial_badge' => (string)($r['editorial_badge'] ?? ''),
                'editorial_subtitle' => (string)($r['editorial_subtitle'] ?? ($r['description'] ?? '')),
                'accent_hex' => (string)($r['accent_hex'] ?? '#141414'),
                'bg_hex' => (string)($r['bg_hex'] ?? '#F8F7F4'),
                'animation_key' => (string)($r['animation_key'] ?? 'zoom_reveal'),
                'tile_span' => (string)($r['tile_span'] ?? 'regular'),
                'display_order' => (int)($r['display_order'] ?? 0),
                'is_active' => ((int)($r['is_active'] ?? 1)) === 1,
            ];
        }
        MelvoreResponse::ok(['categories' => $rows], 'Admin category editorial configs loaded');
    }

    if (!MelvoreDB::tableExists('app_category_editorial_configs')) {
        MelvoreResponse::error('Run migration 003 before editing category configs.', 'SCHEMA_ERROR', 500);
    }

    $body = MelvoreResponse::jsonBody();
    $catId = (int)($body['category_id'] ?? 0);
    if ($catId <= 0) {
        MelvoreResponse::error('Valid category_id is required.', 'VALIDATION_ERROR', 422);
    }

    $pdo->prepare(
        'INSERT INTO app_category_editorial_configs
            (category_id, motif_key, editorial_badge, editorial_subtitle, accent_hex, bg_hex, animation_key, tile_span, cover_image_url, display_order, is_active)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE
            motif_key = VALUES(motif_key),
            editorial_badge = VALUES(editorial_badge),
            editorial_subtitle = VALUES(editorial_subtitle),
            accent_hex = VALUES(accent_hex),
            bg_hex = VALUES(bg_hex),
            animation_key = VALUES(animation_key),
            tile_span = VALUES(tile_span),
            cover_image_url = VALUES(cover_image_url),
            display_order = VALUES(display_order),
            is_active = VALUES(is_active)'
    )->execute([
        $catId,
        (string)($body['motif_key'] ?? 'editorial_frame'),
        !empty($body['editorial_badge']) ? trim((string)$body['editorial_badge']) : null,
        !empty($body['editorial_subtitle']) ? trim((string)$body['editorial_subtitle']) : null,
        (string)($body['accent_hex'] ?? '#141414'),
        (string)($body['bg_hex'] ?? '#F8F7F4'),
        (string)($body['animation_key'] ?? 'zoom_reveal'),
        (string)($body['tile_span'] ?? 'regular'),
        !empty($body['cover_image_url']) ? trim((string)$body['cover_image_url']) : null,
        (int)($body['display_order'] ?? 0),
        isset($body['is_active']) ? (!empty($body['is_active']) ? 1 : 0) : 1,
    ]);

    MelvoreResponse::ok(['category_id' => $catId], 'Category showcase saved');
} catch (Throwable $e) {
    error_log('[Melvore][admin/categories] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to manage category editorial configuration.', 'SERVER_ERROR', 500);
}
