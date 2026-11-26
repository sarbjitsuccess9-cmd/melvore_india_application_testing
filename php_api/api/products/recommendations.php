<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';
require_once dirname(__DIR__, 2) . '/services/RecommendationService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $productId = (int)($_GET['product_id'] ?? 0);
    $worldSlug = trim((string)($_GET['world_slug'] ?? ''));
    $limit = max(2, min(20, (int)($_GET['limit'] ?? 8)));
    $products = [];

    if ($worldSlug !== '') {
        $products = MelvoreRecommendationService::fetchWorldScopedRelatedProducts($pdo, $worldSlug, $productId, $limit);
    }

    if (count($products) < $limit && $productId > 0) {
        $pStmt = $pdo->prepare('SELECT category_id, brand_id FROM products WHERE product_id = ? LIMIT 1');
        $pStmt->execute([$productId]);
        $base = $pStmt->fetch();
        if ($base) {
            $seenIds = array_map(static fn($p) => (int)$p['product_id'], $products);
            $seenIds[] = $productId;
            $seenIds = array_values(array_unique($seenIds));
            $notInPh = implode(',', array_fill(0, count($seenIds), '?'));
            $needed = $limit - count($products);

            $rStmt = $pdo->prepare(
                "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
                 FROM products p
                 LEFT JOIN categories c ON c.category_id = p.category_id
                 LEFT JOIN brands b ON b.brand_id = p.brand_id
                 WHERE p.is_active = 1 AND p.product_id NOT IN ($notInPh) AND (p.category_id = ? OR p.brand_id = ?)
                 ORDER BY (p.brand_id = ?) DESC, p.rating_average DESC, p.purchase_count DESC
                 LIMIT ?"
            );
            $idx = 1;
            foreach ($seenIds as $sid) {
                $rStmt->bindValue($idx++, $sid, PDO::PARAM_INT);
            }
            $rStmt->bindValue($idx++, (int)$base['category_id'], PDO::PARAM_INT);
            $rStmt->bindValue($idx++, (int)($base['brand_id'] ?? 0), PDO::PARAM_INT);
            $rStmt->bindValue($idx++, (int)($base['brand_id'] ?? 0), PDO::PARAM_INT);
            $rStmt->bindValue($idx, $needed, PDO::PARAM_INT);
            $rStmt->execute();
            $products = array_merge($products, MelvoreProductFormatter::formatRows($rStmt->fetchAll()));
        }
    }

    if (empty($products)) {
        $fStmt = $pdo->prepare(
            'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM products p
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.is_active = 1
             ORDER BY p.rating_average DESC, p.purchase_count DESC
             LIMIT ?'
        );
        $fStmt->bindValue(1, $limit, PDO::PARAM_INT);
        $fStmt->execute();
        $products = MelvoreProductFormatter::formatRows($fStmt->fetchAll());
    }

    MelvoreResponse::ok([
        'world_slug' => $worldSlug !== '' ? $worldSlug : null,
        'products' => $products,
    ], 'Recommendations loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load recommendations.', 'SERVER_ERROR', 500);
}
