<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $productId = (int)($_GET['id'] ?? ($_GET['product_id'] ?? 0));
    $pincode = trim((string)($_GET['pincode'] ?? ''));

    if ($productId <= 0) {
        MelvoreResponse::error('Valid product id is required.', 'VALIDATION_ERROR', 422);
    }

    $stmt = $pdo->prepare(
        'SELECT p.*, c.name AS category_name, c.slug AS category_slug,
                b.name AS brand_name, b.slug AS brand_slug
         FROM products p
         LEFT JOIN categories c ON c.category_id = p.category_id
         LEFT JOIN brands b ON b.brand_id = p.brand_id
         WHERE p.product_id = ? AND p.is_active = 1
         LIMIT 1'
    );
    $stmt->execute([$productId]);
    $row = $stmt->fetch();
    if (!$row) {
        MelvoreResponse::error('Product not found.', 'NOT_FOUND', 404);
    }

    $formatted = MelvoreProductFormatter::formatRows([$row], true);
    $product = $formatted[0];

    // Record view counter & style/brand activity cleanly
    $pdo->prepare('UPDATE products SET view_count = view_count + 1 WHERE product_id = ?')->execute([$productId]);
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();

    if (($userId !== null || $guestId !== '') && MelvoreDB::tableExists('app_user_style_signals')) {
        $cols = ['user_id', 'guest_id', 'signal_type', 'product_id', 'category_id', 'created_at'];
        $vals = ['?', '?', '?', '?', '?', 'NOW()'];
        $bind = [
            $userId,
            $guestId !== '' ? $guestId : null,
            'product_view',
            $productId,
            (int)$row['category_id'],
        ];
        if (MelvoreDB::columnExists('app_user_style_signals', 'brand_id')) {
            $cols[] = 'brand_id';
            $vals[] = '?';
            $bind[] = isset($row['brand_id']) ? (int)$row['brand_id'] : null;
        }
        if (MelvoreDB::columnExists('app_user_style_signals', 'signal_weight')) {
            $cols[] = 'signal_weight';
            $vals[] = '?';
            $bind[] = 1.0;
        } elseif (MelvoreDB::columnExists('app_user_style_signals', 'weight')) {
            $cols[] = 'weight';
            $vals[] = '?';
            $bind[] = 1.0;
        }
        $sigStmt = $pdo->prepare(
            'INSERT INTO app_user_style_signals (' . implode(', ', $cols) . ') VALUES (' . implode(', ', $vals) . ')'
        );
        $sigStmt->execute($bind);
    }

    // Variations
    $vStmt = $pdo->prepare(
        'SELECT variation_id, attribute_name, attribute_value, color, size, regular_price, sale_price,
                stock_quantity, is_active, image_url
         FROM product_variations
         WHERE product_id = ? AND is_active = 1
         ORDER BY is_default DESC, variation_id ASC'
    );
    $vStmt->execute([$productId]);
    $variations = [];
    foreach ($vStmt->fetchAll() as $v) {
        $variations[] = [
            'variation_id' => (int)$v['variation_id'],
            'attribute_name' => (string)($v['attribute_name'] ?? ''),
            'attribute_value' => (string)($v['attribute_value'] ?? ''),
            'color' => $v['color'] ?? null,
            'size' => $v['size'] ?? null,
            'regular_price' => $v['regular_price'] !== null ? (float)$v['regular_price'] : null,
            'sale_price' => $v['sale_price'] !== null ? (float)$v['sale_price'] : null,
            'stock_quantity' => (int)($v['stock_quantity'] ?? 0),
            'is_active' => ((int)($v['is_active'] ?? 1)) === 1,
            'image_url' => MelvoreResponse::mediaUrl($v['image_url'] ?? null),
        ];
    }

    // Approved reviews + app_review_media + user helpful votes
    $rStmt = $pdo->prepare(
        "SELECT r.review_id, r.rating, r.title, r.comment, r.is_verified_purchase, r.helpful_count, r.created_at,
                COALESCE(u.name, 'Melvore Customer') AS user_name
         FROM reviews r
         LEFT JOIN users u ON u.user_id = r.user_id
         WHERE r.product_id = ? AND r.is_approved = 1
         ORDER BY r.is_verified_purchase DESC, r.helpful_count DESC, r.review_id DESC
         LIMIT 30"
    );
    $rStmt->execute([$productId]);
    $reviewsRaw = $rStmt->fetchAll();

    $reviewMediaMap = [];
    $votedReviewIds = [];
    if (!empty($reviewsRaw)) {
        $rIds = array_map(static fn($r) => (int)$r['review_id'], $reviewsRaw);
        $ph = implode(',', array_fill(0, count($rIds), '?'));
        if (MelvoreDB::tableExists('app_review_media')) {
            $rmStmt = $pdo->prepare("SELECT review_id, media_url, media_type FROM app_review_media WHERE review_id IN ($ph)");
            $rmStmt->execute($rIds);
            foreach ($rmStmt->fetchAll() as $rm) {
                $reviewMediaMap[(int)$rm['review_id']][] = MelvoreResponse::mediaUrl((string)$rm['media_url']);
            }
        }
        if ($userId !== null && MelvoreDB::tableExists('app_review_votes')) {
            $rvStmt = $pdo->prepare("SELECT review_id FROM app_review_votes WHERE user_id = ? AND review_id IN ($ph)");
            $rvStmt->execute(array_merge([$userId], $rIds));
            $votedReviewIds = array_map(static fn($v) => (int)$v['review_id'], $rvStmt->fetchAll());
        }
    }

    $reviews = [];
    foreach ($reviewsRaw as $rv) {
        $rid = (int)$rv['review_id'];
        $reviews[] = [
            'review_id' => $rid,
            'user_name' => (string)$rv['user_name'],
            'rating' => (int)$rv['rating'],
            'title' => $rv['title'] ?? '',
            'comment' => (string)$rv['comment'],
            'is_verified_purchase' => ((int)($rv['is_verified_purchase'] ?? 0)) === 1,
            'helpful_count' => (int)($rv['helpful_count'] ?? 0),
            'has_voted_helpful' => in_array($rid, $votedReviewIds, true),
            'media_urls' => array_values(array_filter($reviewMediaMap[$rid] ?? [])),
            'created_at' => (string)($rv['created_at'] ?? ''),
        ];
    }

    // Similar products (same category or shared tags)
    $simStmt = $pdo->prepare(
        'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
         FROM products p
         LEFT JOIN categories c ON c.category_id = p.category_id
         LEFT JOIN brands b ON b.brand_id = p.brand_id
         WHERE p.is_active = 1 AND p.product_id <> ? AND p.category_id = ?
         ORDER BY p.is_featured DESC, p.rating_average DESC
         LIMIT 6'
    );
    $simStmt->execute([$productId, (int)$row['category_id']]);
    $similarProducts = MelvoreProductFormatter::formatRows($simStmt->fetchAll());

    // More from this brand
    $moreFromBrand = [];
    if (!empty($row['brand_id'])) {
        $brStmt = $pdo->prepare(
            'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM products p
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.is_active = 1 AND p.product_id <> ? AND p.brand_id = ?
             ORDER BY p.purchase_count DESC, p.product_id DESC
             LIMIT 6'
        );
        $brStmt->execute([$productId, (int)$row['brand_id']]);
        $moreFromBrand = MelvoreProductFormatter::formatRows($brStmt->fetchAll());
    }

    // Optional pincode serviceability lookup
    $serviceability = null;
    if ($pincode !== '' && MelvoreDB::tableExists('app_delivery_serviceability_cache')) {
        $dStmt = $pdo->prepare('SELECT * FROM app_delivery_serviceability_cache WHERE pincode = ? LIMIT 1');
        $dStmt->execute([$pincode]);
        $dRow = $dStmt->fetch();
        if ($dRow) {
            $serviceability = [
                'pincode' => (string)$dRow['pincode'],
                'serviceable' => ((int)($dRow['is_serviceable'] ?? 1)) === 1,
                'cod_available' => ((int)($dRow['cod_available'] ?? 1)) === 1,
                'express_available' => ((int)($dRow['express_available'] ?? 0)) === 1,
                'estimated_days_min' => (int)($dRow['estimated_days_min'] ?? 3),
                'estimated_days_max' => (int)($dRow['estimated_days_max'] ?? 6),
                'city' => (string)($dRow['city'] ?? ''),
                'state' => (string)($dRow['state'] ?? ''),
            ];
        }
    }

    $product['variations'] = $variations;
    $product['reviews'] = $reviews;
    $product['related_products'] = $similarProducts;
    $product['more_from_brand'] = $moreFromBrand;
    $product['serviceability'] = $serviceability;

    MelvoreResponse::initCorsAndHeaders();
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => 'Product details loaded',
        'product' => $product,
        'data' => ['product' => $product],
    ], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load product details.', 'SERVER_ERROR', 500);
}
