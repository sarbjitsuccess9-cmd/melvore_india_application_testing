<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $videoId = (int)($_GET['id'] ?? ($_GET['video_id'] ?? 0));
    if ($videoId <= 0) {
        MelvoreResponse::error('Valid video_id is required.', 'VALIDATION_ERROR', 422);
    }
    if (!MelvoreDB::tableExists('app_product_videos')) {
        MelvoreResponse::error('Video not found.', 'NOT_FOUND', 404);
    }

    $stmt = $pdo->prepare(
        'SELECT v.*, b.name AS brand_name, b.slug AS brand_slug, b.logo AS brand_logo
         FROM app_product_videos v
         LEFT JOIN brands b ON b.brand_id = v.brand_id
         WHERE v.video_id = ? AND v.is_active = 1
         LIMIT 1'
    );
    $stmt->execute([$videoId]);
    $row = $stmt->fetch();
    if (!$row) {
        MelvoreResponse::error('Video not found.', 'NOT_FOUND', 404);
    }

    $relatedProducts = [];
    $relStmt = $pdo->prepare(
        'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
         FROM products p
         LEFT JOIN categories c ON c.category_id = p.category_id
         LEFT JOIN brands b ON b.brand_id = p.brand_id
         WHERE p.is_active = 1 AND (p.product_id = ? OR (? > 0 AND p.brand_id = ?))
         ORDER BY (p.product_id = ?) DESC, p.is_featured DESC
         LIMIT 8'
    );
    $pid = (int)($row['product_id'] ?? 0);
    $bid = (int)($row['brand_id'] ?? 0);
    $relStmt->execute([$pid, $bid, $bid, $pid]);
    $relatedProducts = MelvoreProductFormatter::formatRows($relStmt->fetchAll());

    MelvoreResponse::ok([
        'video' => [
            'video_id' => (int)$row['video_id'],
            'title' => (string)($row['title'] ?? ''),
            'caption' => (string)($row['caption'] ?? ''),
            'creator_name' => (string)($row['creator_name'] ?? ''),
            'creator_handle' => (string)($row['creator_handle'] ?? ''),
            'video_url' => MelvoreResponse::mediaUrl($row['video_url'] ?? null),
            'poster_url' => MelvoreResponse::mediaUrl($row['poster_url'] ?? ($row['thumbnail_url'] ?? null)),
            'duration_seconds' => (int)($row['duration_seconds'] ?? 0),
            'is_sponsored' => ((int)($row['is_sponsored'] ?? 0)) === 1,
            'brand_id' => $bid > 0 ? $bid : null,
            'brand_name' => $row['brand_name'] ?? null,
            'brand_slug' => $row['brand_slug'] ?? null,
            'brand_logo' => MelvoreResponse::mediaUrl($row['brand_logo'] ?? null),
            'product_id' => $pid > 0 ? $pid : null,
            'views_count' => (int)($row['views_count'] ?? 0),
            'likes_count' => (int)($row['likes_count'] ?? 0),
            'saves_count' => (int)($row['saves_count'] ?? 0),
            'shares_count' => (int)($row['shares_count'] ?? 0),
        ],
        'related_products' => $relatedProducts,
    ], 'Video details loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load video details.', 'SERVER_ERROR', 500);
}
