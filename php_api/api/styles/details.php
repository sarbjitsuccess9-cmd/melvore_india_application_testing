<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';
require_once dirname(__DIR__, 2) . '/services/DesignSystemService.php';
require_once dirname(__DIR__, 2) . '/services/RecommendationService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();

    $styleId = (int)($_GET['id'] ?? ($_GET['style_world_id'] ?? 0));
    $slug = trim((string)($_GET['slug'] ?? ''));
    $page = max(1, (int)($_GET['page'] ?? 1));
    $limit = max(1, min(60, (int)($_GET['limit'] ?? 24)));
    $offset = ($page - 1) * $limit;

    if ($styleId <= 0 && $slug === '') {
        MelvoreResponse::error('Style world id or slug is required.', 'VALIDATION_ERROR', 422);
    }
    if (!MelvoreDB::tableExists('app_style_worlds')) {
        MelvoreResponse::error('Style world catalog is not configured.', 'NOT_FOUND', 404);
    }

    if ($styleId > 0) {
        $stmt = $pdo->prepare('SELECT * FROM app_style_worlds WHERE style_world_id = ? AND is_active = 1 LIMIT 1');
        $stmt->execute([$styleId]);
    } else {
        $stmt = $pdo->prepare('SELECT * FROM app_style_worlds WHERE slug = ? AND is_active = 1 LIMIT 1');
        $stmt->execute([$slug]);
    }
    $row = $stmt->fetch();
    $isManifestSyntheticWorld = false;
    if (!$row && $slug !== '') {
        $resolvedDesign = MelvoreDesignSystemService::resolvePublishedManifest($pdo);
        $cleanSlug = strtolower($slug);
        $wSpec = $resolvedDesign['manifest']['worlds'][$cleanSlug] ?? null;
        if (is_array($wSpec)) {
            $kit = is_array($wSpec['kit'] ?? null) ? $wSpec['kit'] : [];
            $pal = is_array($kit['palette'] ?? null) ? $kit['palette'] : [];
            $typo = is_array($kit['typography'] ?? null) ? $kit['typography'] : [];
            $heroRel = (string)($kit['hero_image'] ?? '');
            $row = [
                'style_world_id' => 0,
                'slug' => $cleanSlug,
                'name' => ucwords(str_replace('-', ' ', $cleanSlug)),
                'tagline' => ucfirst(str_replace('_', ' ', (string)($wSpec['template'] ?? 'Editorial World'))),
                'description' => '',
                'theme_key' => (string)($wSpec['template'] ?? $cleanSlug),
                'animation_key' => (string)($kit['motion_preset'] ?? 'smooth_reveal'),
                'font_style' => (string)($typo['display_font'] ?? ($wSpec['font_style'] ?? 'editorial_serif')),
                'button_style' => (string)($kit['button_skin'] ?? ($wSpec['button_style'] ?? 'editorial_sharp')),
                'primary_color' => (string)($pal['text_primary'] ?? '#FFFFFF'),
                'accent_color' => (string)($pal['accent'] ?? '#D4AF37'),
                'background_color' => (string)($pal['background'] ?? '#0E0D1B'),
                'cover_image_url' => $heroRel !== '' ? $heroRel : null,
                'mobile_image_url' => $heroRel !== '' ? $heroRel : null,
                'display_order' => 99,
                'theme_config' => json_encode([
                    'mode' => (string)($pal['mode'] ?? 'dark'),
                    'surface_color' => (string)($pal['surface'] ?? '#181632'),
                    'text_primary' => (string)($pal['text_primary'] ?? '#FFFFFF'),
                    'text_secondary' => (string)($pal['text_secondary'] ?? '#A1A1AA'),
                    'secondary_accent' => (string)($pal['secondary_accent'] ?? '#94A3B8'),
                    'border_color' => (string)($pal['border'] ?? '#27272A'),
                    'card_radius_dp' => (int)($wSpec['card_radius_dp'] ?? 8),
                ], JSON_UNESCAPED_SLASHES),
            ];
            $isManifestSyntheticWorld = true;
        }
    }
    if (!$row) {
        MelvoreResponse::error('Style world not found.', 'NOT_FOUND', 404);
    }
    $wid = (int)$row['style_world_id'];

    // Record exploration signal + invalidate cached recs (only for DB-backed world IDs)
    if ($wid > 0 && ($userId !== null || $guestId !== '') && MelvoreDB::tableExists('app_user_style_signals')) {
        $sigStmt = $pdo->prepare(
            'INSERT INTO app_user_style_signals (user_id, guest_id, style_world_id, signal_type, signal_weight, created_at)
             VALUES (?, ?, ?, "style_explore", 2.0, NOW())'
        );
        $sigStmt->execute([$userId, $guestId !== '' ? $guestId : null, $wid]);
        MelvoreRecommendationService::invalidateForActor($pdo, $userId, $guestId);
    }

    // Check one-time satisfaction feedback status for this actor + world
    $hasSubmittedFeedback = false;
    $existingFeedback = null;
    if ($wid > 0 && ($userId !== null || $guestId !== '') && MelvoreDB::tableExists('app_style_world_feedback')) {
        $fSql = $userId !== null
            ? 'SELECT style_world_id, world_slug, satisfaction_score, feedback_label, coupon_code, submitted_at
               FROM app_style_world_feedback WHERE style_world_id = ? AND user_id = ? LIMIT 1'
            : 'SELECT style_world_id, world_slug, satisfaction_score, feedback_label, coupon_code, submitted_at
               FROM app_style_world_feedback WHERE style_world_id = ? AND guest_id = ? LIMIT 1';
        $fStmt = $pdo->prepare($fSql);
        $fStmt->execute([$wid, $userId ?? $guestId]);
        $fb = $fStmt->fetch();
        if ($fb) {
            $hasSubmittedFeedback = true;
            $existingFeedback = [
                'style_world_id' => (int)$fb['style_world_id'],
                'world_slug' => (string)$fb['world_slug'],
                'satisfaction_score' => (int)$fb['satisfaction_score'],
                'feedback_label' => (string)$fb['feedback_label'],
                'coupon_code' => $fb['coupon_code'] ?? null,
                'submitted_at' => (string)$fb['submitted_at'],
            ];
        }
    }

    $products = [];
    $total = 0;
    if ($wid > 0 && MelvoreDB::tableExists('app_style_world_products')) {
        $cntStmt = $pdo->prepare(
            'SELECT COUNT(DISTINCT p.product_id)
             FROM app_style_world_products swp
             INNER JOIN products p ON p.product_id = swp.product_id
             WHERE swp.style_world_id = ? AND p.is_active = 1'
        );
        $cntStmt->execute([$wid]);
        $total = (int)$cntStmt->fetchColumn();

        $pStmt = $pdo->prepare(
            'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM app_style_world_products swp
             INNER JOIN products p ON p.product_id = swp.product_id
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE swp.style_world_id = ? AND p.is_active = 1
             ORDER BY swp.display_order ASC, p.is_featured DESC, p.product_id DESC
             LIMIT ? OFFSET ?'
        );
        $pStmt->bindValue(1, $wid, PDO::PARAM_INT);
        $pStmt->bindValue(2, $limit, PDO::PARAM_INT);
        $pStmt->bindValue(3, $offset, PDO::PARAM_INT);
        $pStmt->execute();
        $products = MelvoreProductFormatter::formatRows($pStmt->fetchAll());
    }

    if (empty($products) && $total === 0 && MelvoreDB::tableExists('app_tags') && MelvoreDB::tableExists('app_product_tag_map')) {
        $tagKey = strtolower((string)$row['slug']);
        $pStmt = $pdo->prepare(
            'SELECT DISTINCT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM products p
             INNER JOIN app_product_tag_map ptm ON ptm.product_id = p.product_id
             INNER JOIN app_tags t ON t.tag_id = ptm.tag_id
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.is_active = 1 AND (LOWER(t.slug) = ? OR LOWER(t.name) LIKE ?)
             ORDER BY p.is_featured DESC, p.product_id DESC
             LIMIT ? OFFSET ?'
        );
        $pStmt->bindValue(1, $tagKey, PDO::PARAM_STR);
        $pStmt->bindValue(2, '%' . $tagKey . '%', PDO::PARAM_STR);
        $pStmt->bindValue(3, $limit, PDO::PARAM_INT);
        $pStmt->bindValue(4, $offset, PDO::PARAM_INT);
        $pStmt->execute();
        $products = MelvoreProductFormatter::formatRows($pStmt->fetchAll());
        $total = count($products);
    }

    // Final catalog fallback for newly added manifest worlds so customers always see real DB products
    if (empty($products) && $total === 0 && MelvoreDB::tableExists('products')) {
        $pStmt = $pdo->prepare(
            'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM products p
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.is_active = 1
             ORDER BY p.is_featured DESC, p.product_id DESC
             LIMIT ? OFFSET ?'
        );
        $pStmt->bindValue(1, $limit, PDO::PARAM_INT);
        $pStmt->bindValue(2, $offset, PDO::PARAM_INT);
        $pStmt->execute();
        $products = MelvoreProductFormatter::formatRows($pStmt->fetchAll());
        $total = count($products);
    }

    $catMap = [];
    foreach ($products as $pr) {
        if (!empty($pr['category_id']) && !empty($pr['category_name'])) {
            $catMap[(int)$pr['category_id']] = [
                'category_id' => (int)$pr['category_id'],
                'name' => (string)$pr['category_name'],
                'slug' => (string)($pr['category_slug'] ?? ''),
            ];
        }
    }

    $formattedWorld = MelvoreThemeEngineService::formatWorldTheme($row);
    $formattedWorld['has_submitted_feedback'] = $hasSubmittedFeedback;
    $formattedWorld['product_count'] = $total;

    MelvoreResponse::ok([
        'style_world' => $formattedWorld,
        'satisfaction_status' => [
            'has_submitted' => $hasSubmittedFeedback,
            'feedback' => $existingFeedback,
        ],
        'categories' => array_values($catMap),
        'products' => $products,
        'pagination' => [
            'page' => $page,
            'limit' => $limit,
            'total' => $total,
            'total_pages' => max(1, (int)ceil($total / $limit)),
        ],
    ], 'Style world loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load style world.', 'SERVER_ERROR', 500);
}
