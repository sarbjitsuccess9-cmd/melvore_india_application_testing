<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();

    $page = max(1, (int)($_GET['page'] ?? 1));
    $limit = max(1, min(30, (int)($_GET['limit'] ?? 12)));
    $offset = ($page - 1) * $limit;
    $brandId = (int)($_GET['brand_id'] ?? 0);
    $styleId = (int)($_GET['style_world_id'] ?? 0);
    $productId = (int)($_GET['product_id'] ?? 0);

    if (!MelvoreDB::tableExists('app_product_videos')) {
        error_log('[Melvore][videos/feed] Missing table app_product_videos. Run melvore_customer_app_experience_migration.sql.');
        MelvoreResponse::ok([
            'videos' => [],
            'pagination' => [
                'page' => $page,
                'limit' => $limit,
                'total' => 0,
                'total_pages' => 1,
                'has_more' => false,
            ],
            'diagnostics' => [
                'table_exists' => false,
                'active_count' => 0,
                'invalid_url_count' => 0,
                'reason' => 'Table app_product_videos is not installed yet. Apply melvore_customer_app_experience_migration.sql.',
            ],
        ], 'Video feed unavailable (migration pending)');
    }

    $where = [
        'v.is_active = 1',
        "TRIM(COALESCE(v.video_url, '')) <> ''",
        '(v.publish_at IS NULL OR v.publish_at <= NOW())',
        '(v.expire_at IS NULL OR v.expire_at >= NOW())',
    ];
    $params = [];
    if ($brandId > 0) {
        $where[] = 'v.brand_id = ?';
        $params[] = $brandId;
    }
    if ($styleId > 0) {
        $where[] = 'v.style_world_id = ?';
        $params[] = $styleId;
    }
    if ($productId > 0) {
        $where[] = 'v.product_id = ?';
        $params[] = $productId;
    }
    $whereSql = implode(' AND ', $where);

    $cntStmt = $pdo->prepare("SELECT COUNT(*) FROM app_product_videos v WHERE $whereSql");
    $cntStmt->execute($params);
    $total = (int)$cntStmt->fetchColumn();
    $totalPages = max(1, (int)ceil($total / $limit));

    $sql = "SELECT v.*,
                   p.name AS product_name, p.slug AS product_slug, p.regular_price, p.sale_price, p.img_src,
                   p.rating_average, p.rating_count, p.stock_quantity, p.available_sizes, p.available_colors,
                   p.category_id, c.name AS category_name, c.slug AS category_slug,
                   COALESCE(v.brand_id, p.brand_id) AS resolved_brand_id,
                   b.name AS brand_name, b.slug AS brand_slug, b.logo AS brand_logo
            FROM app_product_videos v
            LEFT JOIN products p ON p.product_id = v.product_id
            LEFT JOIN categories c ON c.category_id = p.category_id
            LEFT JOIN brands b ON b.brand_id = COALESCE(v.brand_id, p.brand_id)
            WHERE $whereSql
            ORDER BY v.is_featured DESC, v.display_order ASC, v.video_id DESC
            LIMIT ? OFFSET ?";

    $stmt = $pdo->prepare($sql);
    $idx = 1;
    foreach ($params as $p) {
        $stmt->bindValue($idx++, $p, PDO::PARAM_INT);
    }
    $stmt->bindValue($idx++, $limit, PDO::PARAM_INT);
    $stmt->bindValue($idx, $offset, PDO::PARAM_INT);
    $stmt->execute();
    $rows = $stmt->fetchAll();

    // Check user/guest liked & saved states
    $likedIds = [];
    $savedIds = [];
    if (!empty($rows) && ($userId !== null || $guestId !== '') && MelvoreDB::tableExists('app_video_events')) {
        $vIds = array_map(static fn($r) => (int)$r['video_id'], $rows);
        $ph = implode(',', array_fill(0, count($vIds), '?'));
        $actorSql = $userId !== null ? 'user_id = ?' : 'guest_id = ?';
        $actorVal = $userId ?? $guestId;
        $evStmt = $pdo->prepare(
            "SELECT video_id, event_type
             FROM app_video_events
             WHERE video_id IN ($ph) AND $actorSql AND event_type IN ('like', 'unlike', 'save', 'unsave')
             ORDER BY event_id ASC"
        );
        $evStmt->execute(array_merge($vIds, [$actorVal]));
        foreach ($evStmt->fetchAll() as $ev) {
            $vid = (int)$ev['video_id'];
            $et = (string)$ev['event_type'];
            if ($et === 'like') {
                $likedIds[$vid] = true;
            } elseif ($et === 'unlike') {
                unset($likedIds[$vid]);
            } elseif ($et === 'save') {
                $savedIds[$vid] = true;
            } elseif ($et === 'unsave') {
                unset($savedIds[$vid]);
            }
        }
    }

    $videos = [];
    $invalidUrlCount = 0;

    foreach ($rows as $row) {
        $vid = (int)$row['video_id'];
        $resolvedVideoUrl = MelvoreResponse::mediaUrl($row['video_url'] ?? null);

        if (
            $resolvedVideoUrl === null ||
            !filter_var($resolvedVideoUrl, FILTER_VALIDATE_URL) ||
            (!str_starts_with($resolvedVideoUrl, 'https://') && !str_starts_with($resolvedVideoUrl, 'http://'))
        ) {
            $invalidUrlCount++;
            error_log(sprintf('[Melvore][videos/feed] Skipping invalid video_url for video_id=%d: %s', $vid, (string)($row['video_url'] ?? 'null')));
            continue;
        }

        $productObj = null;
        if (!empty($row['product_id']) && !empty($row['product_name'])) {
            $formatted = MelvoreProductFormatter::formatRows([[
                'product_id' => $row['product_id'],
                'sku' => '',
                'name' => $row['product_name'],
                'slug' => $row['product_slug'],
                'img_src' => $row['img_src'],
                'regular_price' => $row['regular_price'],
                'sale_price' => $row['sale_price'],
                'rating_average' => $row['rating_average'],
                'rating_count' => $row['rating_count'],
                'stock_quantity' => $row['stock_quantity'],
                'available_sizes' => $row['available_sizes'],
                'available_colors' => $row['available_colors'],
                'category_id' => $row['category_id'],
                'category_name' => $row['category_name'],
                'category_slug' => $row['category_slug'],
                'brand_id' => $row['resolved_brand_id'],
                'brand_name' => $row['brand_name'],
                'brand_slug' => $row['brand_slug'],
                'is_sponsored' => $row['is_sponsored'] ?? 0,
            ]]);
            $productObj = $formatted[0] ?? null;
        }

        $posterUrl = MelvoreResponse::mediaUrl($row['poster_url'] ?? ($row['thumbnail_url'] ?? null));
        if ($posterUrl === null && $productObj !== null) {
            $posterUrl = $productObj['primary_image'] ?? null;
        }

        $videos[] = [
            'video_id' => $vid,
            'title' => (string)($row['title'] ?? ''),
            'caption' => (string)($row['caption'] ?? ''),
            'creator_name' => (string)($row['creator_name'] ?? ''),
            'creator_handle' => (string)($row['creator_handle'] ?? ''),
            'video_url' => $resolvedVideoUrl,
            'poster_url' => $posterUrl,
            'duration_seconds' => (int)($row['duration_seconds'] ?? 0),
            'aspect_ratio' => (string)($row['aspect_ratio'] ?? '9:16'),
            'is_sponsored' => ((int)($row['is_sponsored'] ?? 0)) === 1,
            'is_featured' => ((int)($row['is_featured'] ?? 0)) === 1,
            'brand_id' => !empty($row['resolved_brand_id']) ? (int)$row['resolved_brand_id'] : null,
            'brand_name' => $row['brand_name'] ?? null,
            'brand_slug' => $row['brand_slug'] ?? null,
            'brand_logo' => MelvoreResponse::mediaUrl($row['brand_logo'] ?? null),
            'product_id' => !empty($row['product_id']) ? (int)$row['product_id'] : null,
            'product' => $productObj,
            'views_count' => (int)($row['views_count'] ?? 0),
            'likes_count' => (int)($row['likes_count'] ?? 0),
            'saves_count' => (int)($row['saves_count'] ?? 0),
            'shares_count' => (int)($row['shares_count'] ?? 0),
            'is_liked' => isset($likedIds[$vid]),
            'is_saved' => isset($savedIds[$vid]),
        ];
    }

    MelvoreResponse::ok([
        'videos' => $videos,
        'pagination' => [
            'page' => $page,
            'limit' => $limit,
            'total' => $total,
            'total_pages' => $totalPages,
            'has_more' => $page < $totalPages,
        ],
        'diagnostics' => [
            'table_exists' => true,
            'active_count' => $total,
            'invalid_url_count' => $invalidUrlCount,
            'reason' => $total === 0 ? 'No active rows in app_product_videos yet.' : 'ok',
        ],
    ], 'Video feed loaded');
} catch (Throwable $e) {
    error_log('[Melvore][videos/feed] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to load video feed.', 'SERVER_ERROR', 500);
}
