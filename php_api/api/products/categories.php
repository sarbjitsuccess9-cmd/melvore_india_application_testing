<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();

    $categories = [];
    if (MelvoreDB::tableExists('categories')) {
        $stmt = $pdo->query(
            "SELECT c.category_id, c.name, c.slug, c.description, c.image, c.is_featured,
                    COUNT(p.product_id) AS product_count
             FROM categories c
             LEFT JOIN products p ON p.category_id = c.category_id AND p.is_active = 1
             WHERE c.status = 'active'
             GROUP BY c.category_id
             ORDER BY c.display_order ASC, c.is_featured DESC, c.name ASC"
        );
        foreach ($stmt->fetchAll() as $row) {
            $categories[] = [
                'category_id' => (int)$row['category_id'],
                'name' => (string)$row['name'],
                'slug' => (string)$row['slug'],
                'description' => $row['description'] ?? null,
                'image_url' => MelvoreResponse::mediaUrl($row['image'] ?? null),
                'is_featured' => ((int)($row['is_featured'] ?? 0)) === 1,
                'product_count' => (int)($row['product_count'] ?? 0),
            ];
        }
    }

    $brands = [];
    if (MelvoreDB::tableExists('brands')) {
        $hasProfiles = MelvoreDB::tableExists('app_brand_profiles');
        $bSql = $hasProfiles
            ? "SELECT b.brand_id, b.name, b.slug, b.description, b.logo, b.status, bp.origin_country, bp.display_order,
                      COUNT(p.product_id) AS product_count
               FROM brands b
               LEFT JOIN app_brand_profiles bp ON bp.brand_id = b.brand_id
               LEFT JOIN products p ON p.brand_id = b.brand_id AND p.is_active = 1
               WHERE b.status = 'active'
               GROUP BY b.brand_id
               ORDER BY COALESCE(bp.display_order, 999) ASC, b.name ASC"
            : "SELECT b.brand_id, b.name, b.slug, b.description, b.logo, b.status, NULL AS origin_country, 0 AS display_order,
                      COUNT(p.product_id) AS product_count
               FROM brands b
               LEFT JOIN products p ON p.brand_id = b.brand_id AND p.is_active = 1
               WHERE b.status = 'active'
               GROUP BY b.brand_id
               ORDER BY b.name ASC";
        foreach ($pdo->query($bSql)->fetchAll() as $b) {
            $brands[] = [
                'brand_id' => (int)$b['brand_id'],
                'id' => (int)$b['brand_id'],
                'name' => (string)$b['name'],
                'brand_name' => (string)$b['name'],
                'slug' => (string)$b['slug'],
                'description' => $b['description'] ?? null,
                'logo' => MelvoreResponse::mediaUrl($b['logo'] ?? null),
                'logo_url' => MelvoreResponse::mediaUrl($b['logo'] ?? null),
                'origin_country' => $b['origin_country'] ?? null,
                'is_imported' => !empty($b['origin_country']),
                'product_count' => (int)($b['product_count'] ?? 0),
                'display_order' => (int)($b['display_order'] ?? 0),
                'is_active' => true,
                'status' => (string)($b['status'] ?? 'active'),
            ];
        }
    }

    $collections = [];
    if (MelvoreDB::tableExists('collections')) {
        foreach ($pdo->query('SELECT * FROM collections WHERE is_active = 1 ORDER BY display_order ASC')->fetchAll() as $c) {
            $collections[] = [
                'id' => (int)$c['id'],
                'name' => (string)$c['name'],
                'subtitle' => (string)($c['subtitle'] ?? ''),
                'slug' => (string)($c['slug'] ?? ''),
                'banner_url' => MelvoreResponse::mediaUrl($c['banner'] ?? null),
            ];
        }
    }

    MelvoreResponse::okFlat([
        'categories' => $categories,
        'brands' => $brands,
        'collections' => $collections,
    ], 'Categories loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load categories.', 'SERVER_ERROR', 500);
}
