<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $hasEd = MelvoreDB::tableExists('app_category_editorial_configs');

    $sql = $hasEd
        ? "SELECT c.category_id, c.parent_id, c.name, c.slug, c.description, c.image,
                  c.display_order, c.is_featured, COUNT(p.product_id) AS product_count,
                  ec.motif_key, ec.editorial_badge, ec.editorial_subtitle, ec.accent_hex,
                  ec.bg_hex, ec.animation_key, ec.tile_span, ec.cover_image_url AS editorial_cover_url,
                  COALESCE(ec.display_order, c.display_order) AS effective_order
           FROM categories c
           LEFT JOIN products p ON p.category_id = c.category_id AND p.is_active = 1
           LEFT JOIN app_category_editorial_configs ec ON ec.category_id = c.category_id AND ec.is_active = 1
           WHERE c.status = 'active'
           GROUP BY c.category_id
           ORDER BY effective_order ASC, c.is_featured DESC, c.name ASC"
        : "SELECT c.category_id, c.parent_id, c.name, c.slug, c.description, c.image,
                  c.display_order, c.is_featured, COUNT(p.product_id) AS product_count
           FROM categories c
           LEFT JOIN products p ON p.category_id = c.category_id AND p.is_active = 1
           WHERE c.status = 'active'
           GROUP BY c.category_id
           ORDER BY c.display_order ASC, c.is_featured DESC, c.name ASC";

    $stmt = $pdo->query($sql);

    $categories = [];
    foreach ($stmt->fetchAll() as $row) {
        $cover = !empty($row['editorial_cover_url']) ? $row['editorial_cover_url'] : ($row['image'] ?? null);
        $categories[] = [
            'category_id' => (int)$row['category_id'],
            'parent_id' => $row['parent_id'] !== null ? (int)$row['parent_id'] : null,
            'name' => (string)$row['name'],
            'slug' => (string)$row['slug'],
            'description' => (string)($row['description'] ?? ''),
            'image_url' => MelvoreResponse::mediaUrl($row['image'] ?? null),
            'cover_image_url' => MelvoreResponse::mediaUrl($cover),
            'motif_key' => (string)($row['motif_key'] ?? 'editorial_frame'),
            'editorial_badge' => !empty($row['editorial_badge']) ? (string)$row['editorial_badge'] : null,
            'editorial_subtitle' => !empty($row['editorial_subtitle']) ? (string)$row['editorial_subtitle'] : null,
            'accent_hex' => (string)($row['accent_hex'] ?? '#141414'),
            'bg_hex' => (string)($row['bg_hex'] ?? '#F8F7F4'),
            'animation_key' => (string)($row['animation_key'] ?? 'zoom_reveal'),
            'tile_span' => (string)($row['tile_span'] ?? 'regular'),
            'display_order' => (int)($row['effective_order'] ?? ($row['display_order'] ?? 0)),
            'is_featured' => ((int)($row['is_featured'] ?? 0)) === 1,
            'product_count' => (int)($row['product_count'] ?? 0),
        ];
    }

    MelvoreResponse::ok(['categories' => $categories], 'Categories loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load categories.', 'SERVER_ERROR', 500);
}
