<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();

    $page = max(1, (int)($_GET['page'] ?? 1));
    $limit = max(1, min(100, (int)($_GET['limit'] ?? 24)));
    $cursor = (int)($_GET['cursor'] ?? 0);
    $offset = ($page - 1) * $limit;

    $category = trim((string)($_GET['category'] ?? ''));
    $collection = strtolower(trim((string)($_GET['collection'] ?? '')));
    $brandId = (int)($_GET['brand_id'] ?? 0);
    $search = trim((string)($_GET['search'] ?? ''));
    $sort = trim((string)($_GET['sort'] ?? 'featured'));
    $featured = isset($_GET['featured']) ? (int)$_GET['featured'] : null;
    $onSale = isset($_GET['on_sale']) ? (int)$_GET['on_sale'] : null;
    $sponsored = isset($_GET['sponsored']) ? (int)$_GET['sponsored'] : null;
    $season = trim((string)($_GET['season'] ?? ''));
    $minPrice = isset($_GET['min_price']) && $_GET['min_price'] !== '' ? (float)$_GET['min_price'] : null;
    $maxPrice = isset($_GET['max_price']) && $_GET['max_price'] !== '' ? (float)$_GET['max_price'] : null;
    $size = trim((string)($_GET['size'] ?? ''));
    $color = trim((string)($_GET['color'] ?? ''));

    $where = ['p.is_active = 1'];
    $joinParams = [];
    $params = [];

    if ($collection !== '' && $collection !== 'all') {
        if ($collection === 'new-arrivals' || $collection === 'new_arrivals' || $collection === 'new') {
            if ($sort === 'featured') {
                $sort = 'newest';
            }
        } elseif ($collection === 'bestsellers' || $collection === 'best-sellers') {
            if ($sort === 'featured') {
                $sort = 'popular';
            }
        } elseif ($collection === 'trending' || $collection === 'trending-now') {
            if ($sort === 'featured') {
                $sort = 'trending';
            }
        } elseif ($collection === 'sale' || $collection === 'up-to-90-off') {
            $onSale = 1;
        } elseif ($collection === 'under-999') {
            $maxPrice = $maxPrice !== null ? min($maxPrice, 999.0) : 999.0;
        } elseif ($collection === 'under-1999') {
            $maxPrice = $maxPrice !== null ? min($maxPrice, 1999.0) : 1999.0;
        } elseif ($collection === 'top-rated' || $collection === 'most-wishlisted') {
            if ($sort === 'featured') {
                $sort = 'rating';
            }
        } elseif ($collection === 'sponsored') {
            $sponsored = 1;
        } else {
            // Check style world products first, otherwise smart keyword editorial matching
            $matchedStyleWorld = false;
            if (MelvoreDB::tableExists('app_style_worlds') && MelvoreDB::tableExists('app_style_world_products')) {
                $swLookup = $pdo->prepare('SELECT style_world_id FROM app_style_worlds WHERE slug = ? AND is_active = 1 LIMIT 1');
                $swLookup->execute([$collection]);
                $swId = $swLookup->fetchColumn();
                if ($swId) {
                    $matchedStyleWorld = true;
                    $where[] = 'p.product_id IN (SELECT product_id FROM app_style_world_products WHERE style_world_id = ?)';
                    $params[] = (int)$swId;
                }
            }
            if (!$matchedStyleWorld) {
                $tokens = array_values(array_filter(explode('-', str_replace(['_edit', '_collection', '_stories', '_wear', '_style', '_looks'], '', $collection)), static fn($t) => strlen($t) >= 3 && !in_array($t, ['the', 'edit', 'collection', 'stories', 'looks', 'new', 'season', 'signature'], true)));
                if (!empty($tokens)) {
                    $tokenConds = [];
                    foreach (array_slice($tokens, 0, 3) as $tok) {
                        $like = '%' . $tok . '%';
                        $tokenConds[] = '(p.name LIKE ? OR p.description LIKE ? OR c.name LIKE ? OR b.name LIKE ?)';
                        array_push($params, $like, $like, $like, $like);
                    }
                    $where[] = '(' . implode(' OR ', $tokenConds) . ')';
                }
            }
        }
    }

    if ($cursor > 0) {
        $where[] = 'p.product_id < ?';
        $params[] = $cursor;
    }
    if ($category !== '' && strtolower($category) !== 'all') {
        $where[] = '(c.slug = ? OR c.name = ? OR c.slug LIKE ? OR c.name LIKE ?)';
        $params[] = $category;
        $params[] = $category;
        $params[] = '%' . $category . '%';
        $params[] = '%' . $category . '%';
    }
    if ($brandId > 0) {
        $where[] = 'p.brand_id = ?';
        $params[] = $brandId;
    }
    if ($featured === 1) {
        $where[] = 'p.is_featured = 1';
    }
    if ($onSale === 1) {
        $where[] = 'p.sale_price IS NOT NULL AND p.sale_price > 0 AND p.sale_price < p.regular_price';
    }
    if ($minPrice !== null) {
        $where[] = 'COALESCE(NULLIF(p.sale_price, 0), p.regular_price) >= ?';
        $params[] = $minPrice;
    }
    if ($maxPrice !== null) {
        $where[] = 'COALESCE(NULLIF(p.sale_price, 0), p.regular_price) <= ?';
        $params[] = $maxPrice;
    }
    if ($size !== '') {
        $where[] = 'p.available_sizes LIKE ?';
        $params[] = '%"' . $size . '"%';
    }
    if ($color !== '') {
        $where[] = 'p.available_colors LIKE ?';
        $params[] = '%' . $color . '%';
    }
    if ($search !== '') {
        $where[] = '(p.name LIKE ? OR p.description LIKE ? OR p.sku LIKE ? OR b.name LIKE ? OR c.name LIKE ?)';
        $like = '%' . $search . '%';
        array_push($params, $like, $like, $like, $like, $like);
    }

    $joins = 'LEFT JOIN categories c ON c.category_id = p.category_id
              LEFT JOIN brands b ON b.brand_id = p.brand_id';
    $selectExtra = '';

    if ($sponsored === 1 && MelvoreDB::tableExists('app_product_placements')) {
        $joins .= ' INNER JOIN app_product_placements pl ON pl.product_id = p.product_id AND pl.is_active = 1 AND pl.is_sponsored = 1';
        $selectExtra = ', pl.is_sponsored, pl.express_delivery';
    }
    if ($season !== '' && MelvoreDB::tableExists('app_product_tag_map') && MelvoreDB::tableExists('app_tags')) {
        $joins .= ' INNER JOIN app_product_tag_map ptm ON ptm.product_id = p.product_id
                    INNER JOIN app_tags t ON t.tag_id = ptm.tag_id AND (t.slug = ? OR t.name LIKE ?)';
        $joinParams[] = $season;
        $joinParams[] = '%' . $season . '%';
    }

    $allParams = array_merge($joinParams, $params);
    $whereSql = implode(' AND ', $where);
    $orderSql = match ($sort) {
        'price_asc' => 'COALESCE(NULLIF(p.sale_price, 0), p.regular_price) ASC',
        'price_desc' => 'COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC',
        'rating' => 'p.rating_average DESC, p.rating_count DESC',
        'popular' => 'p.purchase_count DESC, p.view_count DESC',
        'trending' => 'p.view_count DESC, p.rating_average DESC, p.product_id DESC',
        'newest' => 'p.product_id DESC',
        'discount' => '((p.regular_price - COALESCE(NULLIF(p.sale_price, 0), p.regular_price)) / NULLIF(p.regular_price, 0)) DESC',
        default => 'p.is_featured DESC, p.product_id DESC',
    };

    $countStmt = $pdo->prepare("SELECT COUNT(DISTINCT p.product_id) FROM products p $joins WHERE $whereSql");
    $countStmt->execute($allParams);
    $total = (int)$countStmt->fetchColumn();
    $params = $allParams;

    $sql = "SELECT DISTINCT p.*, c.name AS category_name, c.slug AS category_slug,
                   b.name AS brand_name, b.slug AS brand_slug $selectExtra
            FROM products p
            $joins
            WHERE $whereSql
            ORDER BY $orderSql
            LIMIT ? OFFSET ?";

    $stmt = $pdo->prepare($sql);
    $i = 1;
    foreach ($params as $val) {
        $stmt->bindValue($i++, $val);
    }
    $stmt->bindValue($i++, $limit, PDO::PARAM_INT);
    $stmt->bindValue($i, $cursor > 0 ? 0 : $offset, PDO::PARAM_INT);
    $stmt->execute();
    $products = MelvoreProductFormatter::formatRows($stmt->fetchAll());
    $nextCursor = !empty($products) ? (int)end($products)['product_id'] : null;

    // Compatible with both legacy top-level and new envelope consumers
    MelvoreResponse::initCorsAndHeaders();
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => 'Products loaded',
        'products' => $products,
        'pagination' => [
            'page' => $page,
            'limit' => $limit,
            'total' => $total,
            'total_pages' => max(1, (int)ceil($total / $limit)),
            'next_cursor' => $nextCursor,
        ],
        'data' => [
            'products' => $products,
            'pagination' => [
                'page' => $page,
                'limit' => $limit,
                'total' => $total,
                'total_pages' => max(1, (int)ceil($total / $limit)),
                'next_cursor' => $nextCursor,
            ],
        ],
    ], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load products.', 'SERVER_ERROR', 500);
}
