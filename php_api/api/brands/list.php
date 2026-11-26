<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $filter = trim((string)($_GET['filter'] ?? 'all'));
    $limit = max(1, min(100, (int)($_GET['limit'] ?? 50)));
    $hasProfiles = MelvoreDB::tableExists('app_brand_profiles');

    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();

    // Recently viewed brand IDs
    $recentBrandIds = [];
    if (MelvoreDB::tableExists('app_user_brand_activity') && ($userId !== null || $guestId !== '')) {
        if ($userId !== null) {
            $rStmt = $pdo->prepare(
                'SELECT brand_id, MAX(created_at) AS last_seen FROM app_user_brand_activity
                 WHERE user_id = ? GROUP BY brand_id ORDER BY last_seen DESC LIMIT 20'
            );
            $rStmt->execute([$userId]);
        } else {
            $rStmt = $pdo->prepare(
                'SELECT brand_id, MAX(created_at) AS last_seen FROM app_user_brand_activity
                 WHERE guest_id = ? GROUP BY brand_id ORDER BY last_seen DESC LIMIT 20'
            );
            $rStmt->execute([$guestId]);
        }
        $recentBrandIds = array_map(static fn($r) => (int)$r['brand_id'], $rStmt->fetchAll());
    }

    if ($hasProfiles) {
        $where = "b.status = 'active'";
        $order = 'COALESCE(bp.display_order, 999) ASC, b.name ASC';
        if ($filter === 'featured') {
            $where .= ' AND bp.is_featured = 1';
        } elseif ($filter === 'sponsored') {
            $where .= ' AND bp.is_sponsored = 1';
        } elseif ($filter === 'new') {
            $where .= ' AND bp.is_new = 1';
            $order = 'b.brand_id DESC';
        } elseif ($filter === 'trending') {
            $order = 'COALESCE(bp.trending_score, 0) DESC, product_count DESC';
        }

        $taglineCol = MelvoreDB::columnExists('app_brand_profiles', 'tagline')
            ? 'bp.tagline'
            : (MelvoreDB::columnExists('app_brand_profiles', 'editorial_tagline') ? 'bp.editorial_tagline AS tagline' : 'NULL AS tagline');
        $coverCol = MelvoreDB::columnExists('app_brand_profiles', 'editorial_cover_url')
            ? 'bp.editorial_cover_url'
            : (MelvoreDB::columnExists('app_brand_profiles', 'cover_image_url') ? 'bp.cover_image_url AS editorial_cover_url' : 'NULL AS editorial_cover_url');
        $videoCol = MelvoreDB::columnExists('app_brand_profiles', 'video_url')
            ? 'bp.video_url'
            : (MelvoreDB::columnExists('app_brand_profiles', 'brand_video_url') ? 'bp.brand_video_url AS video_url' : 'NULL AS video_url');

        $sql = "SELECT b.brand_id, b.name, b.slug, b.description, b.logo,
                       $taglineCol, $coverCol, $videoCol, bp.origin_country,
                       bp.is_featured, bp.is_sponsored, bp.is_new, bp.trending_score, bp.display_order,
                       COUNT(p.product_id) AS product_count
                FROM brands b
                LEFT JOIN app_brand_profiles bp ON bp.brand_id = b.brand_id
                LEFT JOIN products p ON p.brand_id = b.brand_id AND p.is_active = 1
                WHERE $where
                GROUP BY b.brand_id
                ORDER BY $order
                LIMIT ?";
    } else {
        $sql = "SELECT b.brand_id, b.name, b.slug, b.description, b.logo,
                       NULL AS tagline, NULL AS editorial_cover_url, NULL AS video_url, NULL AS origin_country,
                       0 AS is_featured, 0 AS is_sponsored, 0 AS is_new, 0 AS trending_score, 0 AS display_order,
                       COUNT(p.product_id) AS product_count
                FROM brands b
                LEFT JOIN products p ON p.brand_id = b.brand_id AND p.is_active = 1
                WHERE b.status = 'active'
                GROUP BY b.brand_id
                ORDER BY b.name ASC
                LIMIT ?";
    }

    $stmt = $pdo->prepare($sql);
    $stmt->bindValue(1, $limit, PDO::PARAM_INT);
    $stmt->execute();

    $brands = [];
    foreach ($stmt->fetchAll() as $row) {
        $bid = (int)$row['brand_id'];
        $origin = (string)($row['origin_country'] ?? '');
        $brands[] = [
            'brand_id' => $bid,
            'name' => (string)$row['name'],
            'slug' => (string)$row['slug'],
            'description' => (string)($row['description'] ?? ''),
            'tagline' => (string)($row['tagline'] ?? ''),
            'logo_url' => MelvoreResponse::mediaUrl($row['logo'] ?? null),
            'cover_image_url' => MelvoreResponse::mediaUrl($row['editorial_cover_url'] ?? null),
            'video_url' => MelvoreResponse::mediaUrl($row['video_url'] ?? null),
            'origin_country' => $origin,
            'is_imported' => $origin !== '',
            'is_featured' => ((int)($row['is_featured'] ?? 0)) === 1,
            'is_sponsored' => ((int)($row['is_sponsored'] ?? 0)) === 1,
            'is_new' => ((int)($row['is_new'] ?? 0)) === 1,
            'is_recently_viewed' => in_array($bid, $recentBrandIds, true),
            'trending_score' => (int)($row['trending_score'] ?? 0),
            'display_order' => (int)($row['display_order'] ?? 0),
            'product_count' => (int)($row['product_count'] ?? 0),
            'is_active' => true,
        ];
    }

    if ($filter === 'recent') {
        $orderMap = array_flip($recentBrandIds);
        $brands = array_values(array_filter($brands, static fn($b) => isset($orderMap[$b['brand_id']])));
        usort($brands, static fn($a, $b) => $orderMap[$a['brand_id']] <=> $orderMap[$b['brand_id']]);
    }

    MelvoreResponse::ok([
        'brands' => $brands,
        'recent_brand_ids' => $recentBrandIds,
    ], 'Brands loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load brands.', 'SERVER_ERROR', 500);
}
