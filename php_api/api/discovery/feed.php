<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $worldSlug = trim((string)($_GET['world_slug'] ?? ''));

    $worlds = [];
    if (MelvoreDB::tableExists('app_style_worlds')) {
        $stmt = $pdo->query('SELECT * FROM app_style_worlds WHERE is_active = 1 ORDER BY display_order ASC');
        foreach ($stmt->fetchAll() as $w) {
            $formatted = MelvoreThemeEngineService::formatWorldTheme($w);
            $wid = $formatted['style_world_id'];

            // Fetch up to 6 real products inside this world
            $pStmt = $pdo->prepare(
                'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
                 FROM app_style_world_products swp
                 INNER JOIN products p ON p.product_id = swp.product_id AND p.is_active = 1
                 LEFT JOIN categories c ON c.category_id = p.category_id
                 LEFT JOIN brands b ON b.brand_id = p.brand_id
                 WHERE swp.style_world_id = ?
                 ORDER BY swp.display_order ASC, p.product_id DESC
                 LIMIT 6'
            );
            $pStmt->execute([$wid]);
            $formatted['products'] = MelvoreProductFormatter::formatRows($pStmt->fetchAll());
            $worlds[] = $formatted;
        }
    }

    if ($worldSlug !== '') {
        $worlds = array_values(array_filter($worlds, static fn($w) => strcasecmp((string)$w['slug'], $worldSlug) === 0));
    }

    MelvoreResponse::ok([
        'worlds' => $worlds,
        'active_occasions' => MelvoreThemeEngineService::fetchActiveOccasions($pdo),
        'world_map_pins' => MelvoreThemeEngineService::fetchWorldMapOrigins($pdo),
    ], 'Discovery worlds feed loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load discovery worlds feed.', 'SERVER_ERROR', 500);
}
