<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $brandId = (int)($_GET['brand_id'] ?? ($_GET['id'] ?? 0));
    $slug = trim((string)($_GET['slug'] ?? ''));

    if ($brandId <= 0 && $slug === '') {
        MelvoreResponse::error('Brand ID or slug is required.', 'VALIDATION_ERROR', 422);
    }

    $hasProfiles = MelvoreDB::tableExists('app_brand_profiles');
    if ($hasProfiles) {
        $taglineCol = MelvoreDB::columnExists('app_brand_profiles', 'tagline')
            ? 'bp.tagline'
            : (MelvoreDB::columnExists('app_brand_profiles', 'editorial_tagline') ? 'bp.editorial_tagline AS tagline' : 'NULL AS tagline');
        $coverCol = MelvoreDB::columnExists('app_brand_profiles', 'editorial_cover_url')
            ? 'bp.editorial_cover_url'
            : (MelvoreDB::columnExists('app_brand_profiles', 'cover_image_url') ? 'bp.cover_image_url AS editorial_cover_url' : 'NULL AS editorial_cover_url');
        $videoCol = MelvoreDB::columnExists('app_brand_profiles', 'video_url')
            ? 'bp.video_url'
            : (MelvoreDB::columnExists('app_brand_profiles', 'brand_video_url') ? 'bp.brand_video_url AS video_url' : 'NULL AS video_url');

        $sql = $brandId > 0
            ? "SELECT b.*, $taglineCol, $coverCol, $videoCol, bp.origin_country,
                      bp.story, bp.theme_config, bp.is_featured, bp.is_sponsored, bp.is_new
               FROM brands b LEFT JOIN app_brand_profiles bp ON bp.brand_id = b.brand_id
               WHERE b.brand_id = ? AND b.status = 'active' LIMIT 1"
            : "SELECT b.*, $taglineCol, $coverCol, $videoCol, bp.origin_country,
                      bp.story, bp.theme_config, bp.is_featured, bp.is_sponsored, bp.is_new
               FROM brands b LEFT JOIN app_brand_profiles bp ON bp.brand_id = b.brand_id
               WHERE b.slug = ? AND b.status = 'active' LIMIT 1";
    } else {
        $sql = $brandId > 0
            ? "SELECT * FROM brands WHERE brand_id = ? AND status = 'active' LIMIT 1"
            : "SELECT * FROM brands WHERE slug = ? AND status = 'active' LIMIT 1";
    }

    $stmt = $pdo->prepare($sql);
    $stmt->execute([$brandId > 0 ? $brandId : $slug]);
    $brand = $stmt->fetch();
    if (!$brand) {
        MelvoreResponse::error('Brand not found.', 'NOT_FOUND', 404);
    }
    $resolvedId = (int)$brand['brand_id'];

    // Featured products
    $fStmt = $pdo->prepare(
        'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
         FROM products p
         LEFT JOIN categories c ON c.category_id = p.category_id
         LEFT JOIN brands b ON b.brand_id = p.brand_id
         WHERE p.brand_id = ? AND p.is_active = 1
         ORDER BY p.is_featured DESC, p.rating_average DESC LIMIT 12'
    );
    $fStmt->execute([$resolvedId]);
    $featuredProducts = MelvoreProductFormatter::formatRows($fStmt->fetchAll());

    // New arrivals
    $nStmt = $pdo->prepare(
        'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
         FROM products p
         LEFT JOIN categories c ON c.category_id = p.category_id
         LEFT JOIN brands b ON b.brand_id = p.brand_id
         WHERE p.brand_id = ? AND p.is_active = 1
         ORDER BY p.product_id DESC LIMIT 12'
    );
    $nStmt->execute([$resolvedId]);
    $newArrivals = MelvoreProductFormatter::formatRows($nStmt->fetchAll());

    // Bestsellers
    $bStmt = $pdo->prepare(
        'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
         FROM products p
         LEFT JOIN categories c ON c.category_id = p.category_id
         LEFT JOIN brands b ON b.brand_id = p.brand_id
         WHERE p.brand_id = ? AND p.is_active = 1
         ORDER BY p.purchase_count DESC, p.view_count DESC LIMIT 12'
    );
    $bStmt->execute([$resolvedId]);
    $bestsellers = MelvoreProductFormatter::formatRows($bStmt->fetchAll());

    // Sponsored products for this brand
    $sponsoredProducts = [];
    if (MelvoreDB::tableExists('app_product_placements')) {
        $sStmt = $pdo->prepare(
            'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug, 1 AS is_sponsored
             FROM app_product_placements pl
             INNER JOIN products p ON p.product_id = pl.product_id
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.brand_id = ? AND p.is_active = 1 AND pl.is_active = 1 AND pl.is_sponsored = 1
             ORDER BY pl.priority DESC LIMIT 10'
        );
        $sStmt->execute([$resolvedId]);
        $sponsoredProducts = MelvoreProductFormatter::formatRows($sStmt->fetchAll());
    }

    MelvoreResponse::ok([
        'brand' => [
            'brand_id' => $resolvedId,
            'name' => (string)$brand['name'],
            'slug' => (string)$brand['slug'],
            'description' => (string)($brand['description'] ?? ''),
            'tagline' => (string)($brand['tagline'] ?? ''),
            'story' => (string)($brand['story'] ?? ''),
            'logo_url' => MelvoreResponse::mediaUrl($brand['logo'] ?? null),
            'cover_image_url' => MelvoreResponse::mediaUrl($brand['editorial_cover_url'] ?? null),
            'video_url' => MelvoreResponse::mediaUrl($brand['video_url'] ?? null),
            'origin_country' => (string)($brand['origin_country'] ?? ''),
            'is_featured' => ((int)($brand['is_featured'] ?? 0)) === 1,
            'is_sponsored' => ((int)($brand['is_sponsored'] ?? 0)) === 1,
            'is_new' => ((int)($brand['is_new'] ?? 0)) === 1,
        ],
        'featured_products' => $featuredProducts,
        'new_arrivals' => $newArrivals,
        'bestsellers' => $bestsellers,
        'sponsored_products' => $sponsoredProducts,
    ], 'Brand details loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load brand profile.', 'SERVER_ERROR', 500);
}
