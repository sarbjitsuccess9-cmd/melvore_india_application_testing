<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $slug = trim((string)($_GET['slug'] ?? ''));
    $categoryId = (int)($_GET['category_id'] ?? 0);
    $page = max(1, (int)($_GET['page'] ?? 1));
    $limit = max(1, min(60, (int)($_GET['limit'] ?? 24)));
    $offset = ($page - 1) * $limit;
    $sort = trim((string)($_GET['sort'] ?? 'featured'));

    if ($slug === '' && $categoryId <= 0) {
        MelvoreResponse::error('Category slug or id is required.', 'VALIDATION_ERROR', 422);
    }

    $cStmt = $categoryId > 0
        ? $pdo->prepare("SELECT * FROM categories WHERE category_id = ? AND status = 'active' LIMIT 1")
        : $pdo->prepare("SELECT * FROM categories WHERE slug = ? AND status = 'active' LIMIT 1");
    $cStmt->execute([$categoryId > 0 ? $categoryId : $slug]);
    $cat = $cStmt->fetch();
    if (!$cat) {
        MelvoreResponse::error('Category not found.', 'NOT_FOUND', 404);
    }
    $resolvedId = (int)$cat['category_id'];

    // Include direct child category IDs
    $childStmt = $pdo->prepare("SELECT category_id, name, slug, image FROM categories WHERE parent_id = ? AND status = 'active' ORDER BY display_order ASC");
    $childStmt->execute([$resolvedId]);
    $subcategories = [];
    $catIds = [$resolvedId];
    foreach ($childStmt->fetchAll() as $sub) {
        $catIds[] = (int)$sub['category_id'];
        $subcategories[] = [
            'category_id' => (int)$sub['category_id'],
            'name' => (string)$sub['name'],
            'slug' => (string)$sub['slug'],
            'image_url' => MelvoreResponse::mediaUrl($sub['image'] ?? null),
        ];
    }

    $ph = implode(',', array_fill(0, count($catIds), '?'));
    $orderSql = match ($sort) {
        'price_asc' => 'COALESCE(NULLIF(p.sale_price, 0), p.regular_price) ASC',
        'price_desc' => 'COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC',
        'rating' => 'p.rating_average DESC, p.rating_count DESC',
        'newest' => 'p.product_id DESC',
        default => 'p.is_featured DESC, p.purchase_count DESC, p.product_id DESC',
    };

    $cntStmt = $pdo->prepare("SELECT COUNT(*) FROM products p WHERE p.is_active = 1 AND p.category_id IN ($ph)");
    $cntStmt->execute($catIds);
    $total = (int)$cntStmt->fetchColumn();

    $pStmt = $pdo->prepare(
        "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
         FROM products p
         LEFT JOIN categories c ON c.category_id = p.category_id
         LEFT JOIN brands b ON b.brand_id = p.brand_id
         WHERE p.is_active = 1 AND p.category_id IN ($ph)
         ORDER BY $orderSql
         LIMIT ? OFFSET ?"
    );
    $idx = 1;
    foreach ($catIds as $cid) {
        $pStmt->bindValue($idx++, $cid, PDO::PARAM_INT);
    }
    $pStmt->bindValue($idx++, $limit, PDO::PARAM_INT);
    $pStmt->bindValue($idx, $offset, PDO::PARAM_INT);
    $pStmt->execute();
    $products = MelvoreProductFormatter::formatRows($pStmt->fetchAll());

    MelvoreResponse::ok([
        'category' => [
            'category_id' => $resolvedId,
            'name' => (string)$cat['name'],
            'slug' => (string)$cat['slug'],
            'description' => (string)($cat['description'] ?? ''),
            'image_url' => MelvoreResponse::mediaUrl($cat['image'] ?? null),
            'subcategories' => $subcategories,
        ],
        'products' => $products,
        'pagination' => [
            'page' => $page,
            'limit' => $limit,
            'total' => $total,
            'total_pages' => max(1, (int)ceil($total / $limit)),
        ],
    ], 'Category details loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load category details.', 'SERVER_ERROR', 500);
}
