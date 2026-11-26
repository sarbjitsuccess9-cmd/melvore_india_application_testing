<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/includes/db.php';
require_once dirname(__DIR__) . '/includes/response.php';
require_once dirname(__DIR__) . '/includes/product_formatter.php';

/**
 * Weighted Personalisation & World-Aware Recommendation Engine.
 * Combines world satisfaction, purchases, wishlists, cart adds, views, and video engagement
 * over real active catalog products, with DB-backed response caching & invalidation.
 */
final class MelvoreRecommendationService
{
    public static function invalidateForActor(PDO $pdo, ?int $userId, string $guestId = ''): void
    {
        if (!MelvoreDB::tableExists('app_recommendation_cache')) {
            return;
        }
        try {
            if ($userId !== null && $userId > 0) {
                $pdo->prepare('DELETE FROM app_recommendation_cache WHERE actor_key = ? OR user_id = ?')
                    ->execute(['u:' . $userId, $userId]);
            }
            if ($guestId !== '') {
                $pdo->prepare('DELETE FROM app_recommendation_cache WHERE actor_key = ? OR guest_id = ?')
                    ->execute(['g:' . $guestId, $guestId]);
            }
        } catch (Throwable $e) {
            error_log('[Melvore][RecommendationService::invalidate] ' . $e->getMessage());
        }
    }

    public static function buildForActor(PDO $pdo, ?int $userId, string $guestId = '', int $limit = 16, bool $forceRefresh = false): array
    {
        $limit = max(4, min(30, $limit));
        $actorKey = $userId !== null && $userId > 0
            ? 'u:' . $userId
            : ($guestId !== '' ? 'g:' . $guestId : '');

        if (!$forceRefresh && $actorKey !== '' && MelvoreDB::tableExists('app_recommendation_cache')) {
            $cStmt = $pdo->prepare(
                'SELECT payload_json FROM app_recommendation_cache
                 WHERE actor_key = ? AND expires_at > NOW() LIMIT 1'
            );
            $cStmt->execute([$actorKey]);
            $cachedJson = $cStmt->fetchColumn();
            if (is_string($cachedJson) && $cachedJson !== '') {
                $decoded = json_decode($cachedJson, true);
                if (is_array($decoded) && isset($decoded['products'])) {
                    return $decoded;
                }
            }
        }

        $worldScores = [];     // style_world_id => float
        $categoryScores = [];  // category_id => float
        $brandScores = [];     // brand_id => float
        $avoidedWorldIds = []; // style_world_id => true
        $recentWorldIds = [];  // ordered unique style_world_id list
        $interactedProductWeights = []; // product_id => float
        $signalCount = 0;

        $addScore = static function (array &$map, int $id, float $weight): void {
            if ($id <= 0) {
                return;
            }
            $map[$id] = ($map[$id] ?? 0.0) + $weight;
        };

        $hasActor = ($userId !== null && $userId > 0) || $guestId !== '';
        $actorSql = ($userId !== null && $userId > 0) ? 'user_id = ?' : 'guest_id = ?';
        $actorVal = ($userId !== null && $userId > 0) ? $userId : $guestId;

        if ($hasActor) {
            // 1. One-time Vibe Satisfaction feedback (strongest explicit aesthetic preference)
            if (MelvoreDB::tableExists('app_style_world_feedback')) {
                $fStmt = $pdo->prepare(
                    "SELECT style_world_id, satisfaction_score
                     FROM app_style_world_feedback
                     WHERE $actorSql
                     ORDER BY submitted_at DESC"
                );
                $fStmt->execute([$actorVal]);
                foreach ($fStmt->fetchAll() as $fr) {
                    $signalCount += 2;
                    $wid = (int)$fr['style_world_id'];
                    $score = (int)$fr['satisfaction_score'];
                    $recentWorldIds[] = $wid;
                    $delta = match ($score) {
                        4 => 10.0,
                        3 => 5.5,
                        2 => -1.5,
                        1 => -7.5,
                        default => 0.0,
                    };
                    $addScore($worldScores, $wid, $delta);
                    if ($score <= 1) {
                        $avoidedWorldIds[$wid] = true;
                    }
                }
            }

            // 2. Style signals (explore, view, wishlist, video engage, etc.)
            if (MelvoreDB::tableExists('app_user_style_signals')) {
                $weightCol = MelvoreDB::columnExists('app_user_style_signals', 'signal_weight') ? 'signal_weight' : '1.0';
                $sStmt = $pdo->prepare(
                    "SELECT style_world_id, brand_id, category_id, product_id, signal_type, $weightCol AS w
                     FROM app_user_style_signals
                     WHERE $actorSql
                     ORDER BY created_at DESC
                     LIMIT 150"
                );
                $sStmt->execute([$actorVal]);
                foreach ($sStmt->fetchAll() as $sr) {
                    $signalCount++;
                    $w = (float)($sr['w'] ?? 1.5);
                    if (!empty($sr['style_world_id'])) {
                        $wid = (int)$sr['style_world_id'];
                        $addScore($worldScores, $wid, $w);
                        if (!in_array($wid, $recentWorldIds, true)) {
                            $recentWorldIds[] = $wid;
                        }
                    }
                    if (!empty($sr['category_id'])) {
                        $addScore($categoryScores, (int)$sr['category_id'], $w);
                    }
                    if (!empty($sr['brand_id'])) {
                        $addScore($brandScores, (int)$sr['brand_id'], $w);
                    }
                    if (!empty($sr['product_id'])) {
                        $addScore($interactedProductWeights, (int)$sr['product_id'], $w);
                    }
                }
            }

            // 3. Brand activity
            if (MelvoreDB::tableExists('app_user_brand_activity')) {
                $bStmt = $pdo->prepare(
                    "SELECT brand_id FROM app_user_brand_activity WHERE $actorSql ORDER BY created_at DESC LIMIT 20"
                );
                $bStmt->execute([$actorVal]);
                foreach ($bStmt->fetchAll() as $br) {
                    $signalCount++;
                    $addScore($brandScores, (int)$br['brand_id'], 2.5);
                }
            }

            // 4. Purchases (very strong signal) & Wishlist (strong signal) & Active Cart (strong signal)
            if ($userId !== null && $userId > 0) {
                if (MelvoreDB::tableExists('orders') && MelvoreDB::tableExists('order_items')) {
                    $oStmt = $pdo->prepare(
                        'SELECT oi.product_id, p.category_id, p.brand_id
                         FROM order_items oi
                         INNER JOIN orders o ON o.order_id = oi.order_id
                         INNER JOIN products p ON p.product_id = oi.product_id
                         WHERE o.user_id = ? AND o.order_status <> "cancelled"
                         ORDER BY o.created_at DESC
                         LIMIT 25'
                    );
                    $oStmt->execute([$userId]);
                    foreach ($oStmt->fetchAll() as $or) {
                        $signalCount += 2;
                        $addScore($interactedProductWeights, (int)$or['product_id'], 8.0);
                        $addScore($categoryScores, (int)($or['category_id'] ?? 0), 8.0);
                        $addScore($brandScores, (int)($or['brand_id'] ?? 0), 8.0);
                    }
                }

                if (MelvoreDB::tableExists('wishlist')) {
                    $wStmt = $pdo->prepare(
                        'SELECT w.product_id, p.category_id, p.brand_id
                         FROM wishlist w
                         INNER JOIN products p ON p.product_id = w.product_id
                         WHERE w.user_id = ?
                         LIMIT 25'
                    );
                    $wStmt->execute([$userId]);
                    foreach ($wStmt->fetchAll() as $wr) {
                        $signalCount++;
                        $addScore($interactedProductWeights, (int)$wr['product_id'], 5.0);
                        $addScore($categoryScores, (int)($wr['category_id'] ?? 0), 5.0);
                        $addScore($brandScores, (int)($wr['brand_id'] ?? 0), 5.0);
                    }
                }

                if (MelvoreDB::tableExists('user_cart')) {
                    $ucStmt = $pdo->prepare(
                        'SELECT uc.product_id, p.category_id, p.brand_id
                         FROM user_cart uc
                         INNER JOIN products p ON p.product_id = uc.product_id
                         WHERE uc.user_id = ?'
                    );
                    $ucStmt->execute([$userId]);
                    foreach ($ucStmt->fetchAll() as $cr) {
                        $signalCount++;
                        $addScore($interactedProductWeights, (int)$cr['product_id'], 4.5);
                        $addScore($categoryScores, (int)($cr['category_id'] ?? 0), 4.5);
                        $addScore($brandScores, (int)($cr['brand_id'] ?? 0), 4.5);
                    }
                }
            } elseif ($guestId !== '' && MelvoreDB::tableExists('cart')) {
                $gcStmt = $pdo->prepare(
                    'SELECT c.product_id, p.category_id, p.brand_id
                     FROM cart c
                     INNER JOIN products p ON p.product_id = c.product_id
                     WHERE c.user_id IS NULL AND c.session_id = ?'
                );
                $gcStmt->execute([$guestId]);
                foreach ($gcStmt->fetchAll() as $cr) {
                    $signalCount++;
                    $addScore($interactedProductWeights, (int)$cr['product_id'], 4.5);
                    $addScore($categoryScores, (int)($cr['category_id'] ?? 0), 4.5);
                    $addScore($brandScores, (int)($cr['brand_id'] ?? 0), 4.5);
                }
            }
        }

        // Propagate interacted products to mapped fashion worlds
        if (!empty($interactedProductWeights) && MelvoreDB::tableExists('app_style_world_products')) {
            $pids = array_keys($interactedProductWeights);
            $ph = implode(',', array_fill(0, count($pids), '?'));
            $pwStmt = $pdo->prepare(
                "SELECT style_world_id, product_id FROM app_style_world_products WHERE product_id IN ($ph)"
            );
            $pwStmt->execute($pids);
            foreach ($pwStmt->fetchAll() as $pwr) {
                $addScore($worldScores, (int)$pwr['style_world_id'], $interactedProductWeights[(int)$pwr['product_id']] * 0.55);
            }
        }

        foreach ($worldScores as $wid => $sc) {
            if ($sc <= -2.0) {
                $avoidedWorldIds[$wid] = true;
            }
        }

        // Map all product -> world associations once cleanly for candidate scoring
        $productWorldsMap = []; // product_id => [style_world_id, ...]
        if (MelvoreDB::tableExists('app_style_world_products')) {
            foreach ($pdo->query('SELECT style_world_id, product_id FROM app_style_world_products')->fetchAll() as $row) {
                $productWorldsMap[(int)$row['product_id']][] = (int)$row['style_world_id'];
            }
        }

        // Fetch active catalog candidates and compute weighted score per real product
        $candStmt = $pdo->query(
            'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM products p
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.is_active = 1
             ORDER BY p.is_featured DESC, p.purchase_count DESC, p.rating_average DESC, p.product_id DESC
             LIMIT 140'
        );
        $rawCandidates = $candStmt->fetchAll();

        $scoredCandidates = [];
        foreach ($rawCandidates as $row) {
            $pid = (int)$row['product_id'];
            $cid = (int)($row['category_id'] ?? 0);
            $bid = (int)($row['brand_id'] ?? 0);
            $wids = $productWorldsMap[$pid] ?? [];

            $score = ((int)($row['is_featured'] ?? 0)) * 0.8 + ((float)($row['rating_average'] ?? 0)) * 0.25;
            if ($cid > 0 && isset($categoryScores[$cid])) {
                $score += $categoryScores[$cid];
            }
            if ($bid > 0 && isset($brandScores[$bid])) {
                $score += $brandScores[$bid];
            }
            $belongsOnlyToAvoidedWorld = !empty($wids);
            foreach ($wids as $wid) {
                if (isset($worldScores[$wid])) {
                    $score += $worldScores[$wid] * 1.4;
                }
                if (!isset($avoidedWorldIds[$wid])) {
                    $belongsOnlyToAvoidedWorld = false;
                }
            }
            if ($belongsOnlyToAvoidedWorld) {
                $score -= 25.0; // Demote strongly disliked world products
            }

            $scoredCandidates[] = [
                'score' => $score,
                'row' => $row,
            ];
        }

        usort($scoredCandidates, static fn(array $a, array $b): int => $b['score'] <=> $a['score']);
        $topRows = array_map(static fn(array $it) => $it['row'], array_slice($scoredCandidates, 0, $limit));
        $recommendedProducts = MelvoreProductFormatter::formatRows($topRows);

        // Build preferred_worlds, avoided_worlds, recently_explored_worlds
        $worldsById = [];
        if (MelvoreDB::tableExists('app_style_worlds')) {
            foreach ($pdo->query('SELECT * FROM app_style_worlds WHERE is_active = 1 ORDER BY display_order ASC')->fetchAll() as $w) {
                $worldsById[(int)$w['style_world_id']] = [
                    'style_world_id' => (int)$w['style_world_id'],
                    'slug' => (string)$w['slug'],
                    'name' => (string)$w['name'],
                    'tagline' => (string)($w['tagline'] ?? ''),
                    'cover_image_url' => MelvoreResponse::mediaUrl($w['cover_image_url'] ?? ($w['mobile_image_url'] ?? null)),
                    'accent_color' => (string)($w['accent_color'] ?? '#C8CCD4'),
                    'theme_key' => (string)($w['theme_key'] ?? $w['slug']),
                ];
            }
        }

        $preferredWorlds = [];
        arsort($worldScores);
        foreach ($worldScores as $wid => $sc) {
            if ($sc > 0 && isset($worldsById[$wid]) && !isset($avoidedWorldIds[$wid])) {
                $item = $worldsById[$wid];
                $item['affinity_score'] = round($sc, 2);
                $preferredWorlds[] = $item;
            }
        }

        $avoidedWorlds = [];
        foreach (array_keys($avoidedWorldIds) as $wid) {
            if (isset($worldsById[$wid])) {
                $avoidedWorlds[] = $worldsById[$wid];
            }
        }

        $recentlyExploredWorlds = [];
        foreach (array_slice(array_values(array_unique($recentWorldIds)), 0, 8) as $wid) {
            if (isset($worldsById[$wid])) {
                $recentlyExploredWorlds[] = $worldsById[$wid];
            }
        }

        // Build preferred_categories
        $preferredCategories = [];
        arsort($categoryScores);
        if (!empty($categoryScores) && MelvoreDB::tableExists('categories')) {
            $topCids = array_slice(array_keys($categoryScores), 0, 8);
            $ph = implode(',', array_fill(0, count($topCids), '?'));
            $cs = $pdo->prepare("SELECT category_id, name, slug, image FROM categories WHERE category_id IN ($ph) AND status = 'active'");
            $cs->execute($topCids);
            $catRowsById = [];
            foreach ($cs->fetchAll() as $cr) {
                $catRowsById[(int)$cr['category_id']] = $cr;
            }
            foreach ($topCids as $cid) {
                if (isset($catRowsById[$cid])) {
                    $cr = $catRowsById[$cid];
                    $preferredCategories[] = [
                        'category_id' => $cid,
                        'name' => (string)$cr['name'],
                        'slug' => (string)$cr['slug'],
                        'image_url' => MelvoreResponse::mediaUrl($cr['image'] ?? null),
                        'affinity_score' => round($categoryScores[$cid], 2),
                    ];
                }
            }
        }

        // Recent brands
        $recentBrands = [];
        arsort($brandScores);
        if (!empty($brandScores) && MelvoreDB::tableExists('brands')) {
            $topBids = array_slice(array_keys($brandScores), 0, 8);
            $ph = implode(',', array_fill(0, count($topBids), '?'));
            $bs = $pdo->prepare("SELECT brand_id, name, slug, logo, description FROM brands WHERE brand_id IN ($ph) AND status = 'active'");
            $bs->execute($topBids);
            foreach ($bs->fetchAll() as $br) {
                $recentBrands[] = [
                    'brand_id' => (int)$br['brand_id'],
                    'name' => (string)$br['name'],
                    'slug' => (string)$br['slug'],
                    'logo_url' => MelvoreResponse::mediaUrl($br['logo'] ?? null),
                    'description' => (string)($br['description'] ?? ''),
                ];
            }
        }

        $isPersonalized = $signalCount >= 1 && (!empty($preferredWorlds) || !empty($preferredCategories) || !empty($recentBrands));
        $preferredStyles = $preferredWorlds;

        $payload = [
            'is_personalized' => $isPersonalized,
            'title' => $isPersonalized ? 'Curated For You' : 'Trending & Editorial Picks',
            'subtitle' => $isPersonalized
                ? 'Tailored from your fashion world vibes, saved pieces, and browsing history'
                : 'Explore trending drops across Melvore',
            'products' => $recommendedProducts,
            'recent_brands' => $recentBrands,
            'favourite_styles' => $preferredStyles,
            'preferred_worlds' => $preferredWorlds,
            'avoided_worlds' => $avoidedWorlds,
            'preferred_categories' => $preferredCategories,
            'preferred_styles' => array_map(static fn(array $w) => $w['slug'], $preferredWorlds),
            'recently_explored_worlds' => $recentlyExploredWorlds,
        ];

        if ($actorKey !== '' && MelvoreDB::tableExists('app_recommendation_cache')) {
            try {
                $ins = $pdo->prepare(
                    'INSERT INTO app_recommendation_cache (actor_key, user_id, guest_id, payload_json, expires_at)
                     VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE))
                     ON DUPLICATE KEY UPDATE
                        user_id = VALUES(user_id),
                        guest_id = VALUES(guest_id),
                        payload_json = VALUES(payload_json),
                        expires_at = VALUES(expires_at)'
                );
                $ins->execute([
                    $actorKey,
                    $userId,
                    $guestId !== '' ? $guestId : null,
                    json_encode($payload, JSON_UNESCAPED_UNICODE),
                ]);
            } catch (Throwable $e) {
                error_log('[Melvore][RecommendationService::cacheWrite] ' . $e->getMessage());
            }
        }

        return $payload;
    }

    public static function fetchWorldScopedRelatedProducts(PDO $pdo, string $worldSlug, int $excludeProductId, int $limit = 8): array
    {
        if ($worldSlug === '' || !MelvoreDB::tableExists('app_style_worlds') || !MelvoreDB::tableExists('app_style_world_products')) {
            return [];
        }
        $stmt = $pdo->prepare(
            'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM app_style_world_products swp
             INNER JOIN app_style_worlds sw ON sw.style_world_id = swp.style_world_id AND sw.is_active = 1
             INNER JOIN products p ON p.product_id = swp.product_id AND p.is_active = 1
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE sw.slug = ? AND p.product_id <> ?
             ORDER BY swp.display_order ASC, p.is_featured DESC, p.rating_average DESC
             LIMIT ?'
        );
        $stmt->bindValue(1, $worldSlug, PDO::PARAM_STR);
        $stmt->bindValue(2, $excludeProductId, PDO::PARAM_INT);
        $stmt->bindValue(3, max(2, min(20, $limit)), PDO::PARAM_INT);
        $stmt->execute();
        return MelvoreProductFormatter::formatRows($stmt->fetchAll());
    }
}
