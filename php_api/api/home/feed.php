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

    $country = trim((string)($_GET['country'] ?? 'India'));
    $state = trim((string)($_GET['state'] ?? ''));
    $city = trim((string)($_GET['city'] ?? ''));
    $pincode = trim((string)($_GET['pincode'] ?? ''));

    // 1. Hero Slider (hero_content — resilient against legacy or modern column names)
    $heroSlides = [];
    if (MelvoreDB::tableExists('hero_content')) {
        $stmt = $pdo->query('SELECT * FROM hero_content');
        foreach ($stmt->fetchAll() as $row) {
            $isActiveFlag = array_key_exists('is_active', $row)
                ? ((int)$row['is_active'] === 1)
                : (!array_key_exists('status', $row) || strtolower((string)$row['status']) === 'active');
            if (!$isActiveFlag) {
                continue;
            }
            $primaryImg = $row['image_url'] ?? ($row['desktop_image_url'] ?? ($row['image'] ?? ($row['banner_image'] ?? ($row['desktop_image'] ?? ($row['img_src'] ?? null)))));
            $mobileImg = $row['mobile_image_url'] ?? ($row['mobile_image'] ?? $primaryImg);
            $desktopImg = $row['desktop_image_url'] ?? ($row['desktop_image'] ?? $primaryImg);
            $heroSlides[] = [
                'id' => (int)($row['id'] ?? ($row['hero_id'] ?? 0)),
                'eyebrow' => (string)($row['eyebrow'] ?? ($row['badge_text'] ?? '')),
                'title' => (string)($row['title'] ?? ($row['heading'] ?? '')),
                'subtitle' => (string)($row['subtitle'] ?? ($row['description'] ?? '')),
                'image_url' => MelvoreResponse::mediaUrl($primaryImg),
                'mobile_image_url' => MelvoreResponse::mediaUrl($mobileImg),
                'desktop_image_url' => MelvoreResponse::mediaUrl($desktopImg),
                'cta_text' => (string)($row['cta_text'] ?? ($row['button_text'] ?? '')),
                'cta_link' => (string)($row['cta_link'] ?? ($row['button_link'] ?? '')),
                'display_order' => (int)($row['display_order'] ?? ($row['sort_order'] ?? 0)),
                'is_active' => true,
            ];
        }
        usort($heroSlides, static fn(array $a, array $b): int => ($a['display_order'] <=> $b['display_order']) ?: ($a['id'] <=> $b['id']));
    }

    // 2. Active Festival & Regional Campaigns (app_campaigns, app_campaign_regions, app_campaign_assets)
    $activeCampaigns = [];
    if (MelvoreDB::tableExists('app_campaigns')) {
        $cStmt = $pdo->query(
            'SELECT * FROM app_campaigns
             WHERE is_active = 1
               AND (start_date IS NULL OR start_date <= NOW())
               AND (end_date IS NULL OR end_date >= NOW())
             ORDER BY priority DESC, campaign_id DESC'
        );
        $campaignsRaw = $cStmt->fetchAll();

        $regionsByCampaign = [];
        if (MelvoreDB::tableExists('app_campaign_regions') && !empty($campaignsRaw)) {
            $cIds = array_map(static fn($r) => (int)($r['campaign_id'] ?? $r['id']), $campaignsRaw);
            $ph = implode(',', array_fill(0, count($cIds), '?'));
            $rStmt = $pdo->prepare("SELECT * FROM app_campaign_regions WHERE campaign_id IN ($ph)");
            $rStmt->execute($cIds);
            foreach ($rStmt->fetchAll() as $reg) {
                $regionsByCampaign[(int)$reg['campaign_id']][] = $reg;
            }
        }

        $assetsByCampaign = [];
        if (MelvoreDB::tableExists('app_campaign_assets') && !empty($campaignsRaw)) {
            $cIds = array_map(static fn($r) => (int)($r['campaign_id'] ?? $r['id']), $campaignsRaw);
            $ph = implode(',', array_fill(0, count($cIds), '?'));
            $aStmt = $pdo->prepare("SELECT * FROM app_campaign_assets WHERE campaign_id IN ($ph) ORDER BY display_order ASC");
            $aStmt->execute($cIds);
            foreach ($aStmt->fetchAll() as $ast) {
                $assetsByCampaign[(int)$ast['campaign_id']][] = [
                    'asset_id' => (int)($ast['asset_id'] ?? ($ast['id'] ?? 0)),
                    'asset_type' => (string)($ast['asset_type'] ?? 'image'),
                    'media_url' => MelvoreResponse::mediaUrl($ast['media_url'] ?? ($ast['asset_url'] ?? null)),
                    'mobile_media_url' => MelvoreResponse::mediaUrl($ast['mobile_media_url'] ?? null),
                    'animation_key' => (string)($ast['animation_key'] ?? 'none'),
                    'title' => (string)($ast['title'] ?? ''),
                    'subtitle' => (string)($ast['subtitle'] ?? ''),
                ];
            }
        }

        foreach ($campaignsRaw as $c) {
            $cid = (int)($c['campaign_id'] ?? ($c['id'] ?? 0));
            $regions = $regionsByCampaign[$cid] ?? [];
            $eligible = empty($regions);
            $specificityScore = 0;

            foreach ($regions as $reg) {
                $rCountry = strtolower(trim((string)($reg['country'] ?? '')));
                $rState = strtolower(trim((string)($reg['state'] ?? '')));
                $rCity = strtolower(trim((string)($reg['city'] ?? '')));
                $rPin = trim((string)($reg['pincode'] ?? ''));
                $isNationwide = ((int)($reg['is_nationwide'] ?? 0)) === 1;

                if ($pincode !== '' && $rPin !== '' && $pincode === $rPin) {
                    $eligible = true;
                    $specificityScore = max($specificityScore, 4);
                } elseif ($city !== '' && $rCity !== '' && strcasecmp($city, $rCity) === 0) {
                    $eligible = true;
                    $specificityScore = max($specificityScore, 3);
                } elseif ($state !== '' && $rState !== '' && strcasecmp($state, $rState) === 0) {
                    $eligible = true;
                    $specificityScore = max($specificityScore, 2);
                } elseif ($isNationwide || ($rPin === '' && $rCity === '' && $rState === '' && ($rCountry === '' || strcasecmp($country, $rCountry) === 0))) {
                    $eligible = true;
                    $specificityScore = max($specificityScore, 1);
                }
            }

            if ($eligible) {
                $themeCfg = [];
                if (!empty($c['theme_config'])) {
                    $decoded = is_string($c['theme_config']) ? json_decode($c['theme_config'], true) : $c['theme_config'];
                    if (is_array($decoded)) {
                        $themeCfg = $decoded;
                    }
                }
                $activeCampaigns[] = [
                    'campaign_id' => $cid,
                    'slug' => (string)($c['slug'] ?? ''),
                    'title' => (string)($c['title'] ?? ($c['name'] ?? '')),
                    'subtitle' => (string)($c['subtitle'] ?? ''),
                    'festival_key' => (string)($c['festival_key'] ?? ($c['campaign_type'] ?? 'seasonal')),
                    'animation_key' => (string)($c['animation_key'] ?? 'none'),
                    'animation_duration_ms' => (int)($c['animation_duration_ms'] ?? 2400),
                    'primary_color' => (string)($c['primary_color'] ?? '#D4AF37'),
                    'accent_color' => (string)($c['accent_color'] ?? '#FFFFFF'),
                    'background_color' => (string)($c['background_color'] ?? '#0B0B0E'),
                    'banner_image_url' => MelvoreResponse::mediaUrl($c['banner_image_url'] ?? ($c['desktop_banner_url'] ?? null)),
                    'mobile_banner_url' => MelvoreResponse::mediaUrl($c['mobile_banner_url'] ?? ($c['banner_image_url'] ?? null)),
                    'video_url' => MelvoreResponse::mediaUrl($c['video_url'] ?? null),
                    'cta_text' => (string)($c['cta_text'] ?? 'Explore Edit'),
                    'cta_link' => (string)($c['cta_link'] ?? ''),
                    'priority' => (int)($c['priority'] ?? 0),
                    'specificity_score' => $specificityScore,
                    'theme_config' => $themeCfg,
                    'assets' => $assetsByCampaign[$cid] ?? [],
                ];
            }
        }
        usort($activeCampaigns, static function (array $a, array $b): int {
            return ($b['specificity_score'] <=> $a['specificity_score']) ?: ($b['priority'] <=> $a['priority']);
        });
    }

    // 3. Quick Category Navigation & Editorial Category Discovery Index (categories table + app_category_editorial_configs)
    $categories = [];
    if (MelvoreDB::tableExists('categories')) {
        $hasEdCat = MelvoreDB::tableExists('app_category_editorial_configs');
        $catSql = $hasEdCat
            ? "SELECT c.category_id, c.parent_id, c.name, c.slug, c.description, c.image, c.is_featured, c.display_order,
                      COUNT(p.product_id) AS product_count,
                      ec.motif_key, ec.editorial_badge, ec.editorial_subtitle, ec.accent_hex,
                      ec.bg_hex, ec.animation_key, ec.tile_span, ec.cover_image_url AS editorial_cover_url,
                      COALESCE(ec.display_order, c.display_order) AS effective_order
               FROM categories c
               LEFT JOIN products p ON p.category_id = c.category_id AND p.is_active = 1
               LEFT JOIN app_category_editorial_configs ec ON ec.category_id = c.category_id AND ec.is_active = 1
               WHERE c.status = 'active'
               GROUP BY c.category_id
               ORDER BY effective_order ASC, c.is_featured DESC, c.name ASC"
            : "SELECT c.category_id, c.parent_id, c.name, c.slug, c.description, c.image, c.is_featured, c.display_order,
                      COUNT(p.product_id) AS product_count
               FROM categories c
               LEFT JOIN products p ON p.category_id = c.category_id AND p.is_active = 1
               WHERE c.status = 'active'
               GROUP BY c.category_id
               ORDER BY c.display_order ASC, c.is_featured DESC, c.name ASC";
        $catStmt = $pdo->query($catSql);
        foreach ($catStmt->fetchAll() as $cRow) {
            $cover = !empty($cRow['editorial_cover_url']) ? $cRow['editorial_cover_url'] : ($cRow['image'] ?? null);
            $categories[] = [
                'category_id' => (int)$cRow['category_id'],
                'parent_id' => $cRow['parent_id'] !== null ? (int)$cRow['parent_id'] : null,
                'name' => (string)$cRow['name'],
                'slug' => (string)$cRow['slug'],
                'description' => (string)($cRow['description'] ?? ''),
                'image_url' => MelvoreResponse::mediaUrl($cRow['image'] ?? null),
                'cover_image_url' => MelvoreResponse::mediaUrl($cover),
                'motif_key' => (string)($cRow['motif_key'] ?? 'editorial_frame'),
                'editorial_badge' => !empty($cRow['editorial_badge']) ? (string)$cRow['editorial_badge'] : null,
                'editorial_subtitle' => !empty($cRow['editorial_subtitle']) ? (string)$cRow['editorial_subtitle'] : null,
                'accent_hex' => (string)($cRow['accent_hex'] ?? '#141414'),
                'bg_hex' => (string)($cRow['bg_hex'] ?? '#F8F7F4'),
                'animation_key' => (string)($cRow['animation_key'] ?? 'zoom_reveal'),
                'tile_span' => (string)($cRow['tile_span'] ?? 'regular'),
                'is_featured' => ((int)($cRow['is_featured'] ?? 0)) === 1,
                'display_order' => (int)($cRow['effective_order'] ?? ($cRow['display_order'] ?? 0)),
                'product_count' => (int)($cRow['product_count'] ?? 0),
            ];
        }
    }

    // 4. Dynamic Homepage Sections (app_home_sections, app_home_section_items, app_product_placements)
    $sections = [];
    $seenProductIdsAcrossShelves = [];

    if (MelvoreDB::tableExists('app_home_sections')) {
        $sStmt = $pdo->query(
            'SELECT * FROM app_home_sections
             WHERE is_active = 1
               AND (start_date IS NULL OR start_date <= NOW())
               AND (end_date IS NULL OR end_date >= NOW())
             ORDER BY display_order ASC, section_id ASC'
        );
        $rawSections = $sStmt->fetchAll();

        $itemsBySection = [];
        if (MelvoreDB::tableExists('app_home_section_items') && !empty($rawSections)) {
            $sIds = array_map(static fn($r) => (int)($r['section_id'] ?? $r['id']), $rawSections);
            $ph = implode(',', array_fill(0, count($sIds), '?'));
            $iStmt = $pdo->prepare(
                "SELECT * FROM app_home_section_items
                 WHERE section_id IN ($ph) AND is_active = 1
                   AND (start_date IS NULL OR start_date <= NOW())
                   AND (end_date IS NULL OR end_date >= NOW())
                 ORDER BY display_order ASC, item_id ASC"
            );
            $iStmt->execute($sIds);
            foreach ($iStmt->fetchAll() as $item) {
                $itemsBySection[(int)$item['section_id']][] = [
                    'item_id' => (int)($item['item_id'] ?? ($item['id'] ?? 0)),
                    'title' => (string)($item['title'] ?? ''),
                    'subtitle' => (string)($item['subtitle'] ?? ''),
                    'badge_text' => (string)($item['badge_text'] ?? ''),
                    'image_url' => MelvoreResponse::mediaUrl($item['image_url'] ?? null),
                    'mobile_image_url' => MelvoreResponse::mediaUrl($item['mobile_image_url'] ?? ($item['image_url'] ?? null)),
                    'theme_key' => (string)($item['theme_key'] ?? 'dark_editorial'),
                    'animation_key' => (string)($item['animation_key'] ?? 'smooth_reveal'),
                    'target_type' => (string)($item['target_type'] ?? 'category'),
                    'target_value' => (string)($item['target_value'] ?? ''),
                    'target_id' => isset($item['target_id']) ? (int)$item['target_id'] : null,
                ];
            }
        }

        foreach ($rawSections as $sec) {
            $sid = (int)($sec['section_id'] ?? ($sec['id'] ?? 0));
            $sectionKey = (string)($sec['section_key'] ?? ($sec['section_type'] ?? 'products'));
            $sectionType = (string)($sec['section_type'] ?? 'product_shelf');
            $limit = max(2, min(20, (int)($sec['item_limit'] ?? 8)));
            $sectionProducts = [];

            // Load section products via app_product_placements first, then curated query rules
            if (MelvoreDB::tableExists('app_product_placements')) {
                $pStmt = $pdo->prepare(
                    'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug,
                            pl.is_sponsored, pl.badge_override, pl.express_delivery
                     FROM app_product_placements pl
                     INNER JOIN products p ON p.product_id = pl.product_id
                     LEFT JOIN categories c ON c.category_id = p.category_id
                     LEFT JOIN brands b ON b.brand_id = p.brand_id
                     WHERE pl.section_key = ? AND pl.is_active = 1 AND p.is_active = 1
                       AND (pl.start_date IS NULL OR pl.start_date <= NOW())
                       AND (pl.end_date IS NULL OR pl.end_date >= NOW())
                     ORDER BY pl.priority DESC, pl.display_order ASC
                     LIMIT ?'
                );
                $pStmt->bindValue(1, $sectionKey, PDO::PARAM_STR);
                $pStmt->bindValue(2, $limit, PDO::PARAM_INT);
                $pStmt->execute();
                $sectionProducts = MelvoreProductFormatter::formatRows($pStmt->fetchAll());
            }

            if (empty($sectionProducts) && ($sectionType === 'product_shelf' || $sectionType === 'product_grid')) {
                $orderBy = match ($sectionKey) {
                    'trending', 'trending_now' => 'p.view_count DESC, p.rating_average DESC',
                    'bestsellers', 'hot_sales' => 'p.purchase_count DESC, p.rating_count DESC',
                    'new_arrivals', 'whats_new' => 'p.product_id DESC',
                    'budget_finds', 'maxx_saver' => 'COALESCE(NULLIF(p.sale_price, 0), p.regular_price) ASC',
                    default => 'p.is_featured DESC, p.product_id DESC',
                };
                $extraWhere = '';
                if ($sectionKey === 'up_to_90_off' || $sectionKey === 'hot_sales') {
                    $extraWhere = ' AND p.sale_price IS NOT NULL AND p.sale_price > 0 AND p.sale_price < p.regular_price';
                }
                $excludeSql = '';
                $params = [];
                if (!empty($seenProductIdsAcrossShelves)) {
                    $ph = implode(',', array_fill(0, count($seenProductIdsAcrossShelves), '?'));
                    $excludeSql = " AND p.product_id NOT IN ($ph)";
                    $params = $seenProductIdsAcrossShelves;
                }
                $qSql = "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
                         FROM products p
                         LEFT JOIN categories c ON c.category_id = p.category_id
                         LEFT JOIN brands b ON b.brand_id = p.brand_id
                         WHERE p.is_active = 1 $extraWhere $excludeSql
                         ORDER BY $orderBy
                         LIMIT ?";
                $qStmt = $pdo->prepare($qSql);
                $idx = 1;
                foreach ($params as $pid) {
                    $qStmt->bindValue($idx++, $pid, PDO::PARAM_INT);
                }
                $qStmt->bindValue($idx, $limit, PDO::PARAM_INT);
                $qStmt->execute();
                $sectionProducts = MelvoreProductFormatter::formatRows($qStmt->fetchAll());
            }

            foreach ($sectionProducts as $sp) {
                $seenProductIdsAcrossShelves[] = (int)$sp['product_id'];
            }

            $themeCfg = [];
            if (!empty($sec['theme_config'])) {
                $decoded = is_string($sec['theme_config']) ? json_decode($sec['theme_config'], true) : $sec['theme_config'];
                if (is_array($decoded)) {
                    $themeCfg = $decoded;
                }
            }

            $sections[] = [
                'section_id' => $sid,
                'section_key' => $sectionKey,
                'section_type' => $sectionType,
                'eyebrow' => (string)($sec['eyebrow'] ?? ''),
                'title' => (string)($sec['title'] ?? ''),
                'subtitle' => (string)($sec['subtitle'] ?? ''),
                'animation_key' => (string)($sec['animation_key'] ?? 'smooth_reveal'),
                'theme_key' => (string)($sec['theme_key'] ?? 'dark_editorial'),
                'theme_config' => $themeCfg,
                'cta_text' => (string)($sec['cta_text'] ?? 'View All'),
                'cta_link' => (string)($sec['cta_link'] ?? ''),
                'display_order' => (int)($sec['display_order'] ?? 0),
                'items' => $itemsBySection[$sid] ?? [],
                'products' => $sectionProducts,
            ];
        }
    }

    // 5. Fallback / Continuous Feed First Page Products (if app_home_sections has no rows yet, or for the continuous grid)
    $feedLimit = 16;
    $fStmt = $pdo->prepare(
        'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
         FROM products p
         LEFT JOIN categories c ON c.category_id = p.category_id
         LEFT JOIN brands b ON b.brand_id = p.brand_id
         WHERE p.is_active = 1
         ORDER BY p.is_featured DESC, p.purchase_count DESC, p.product_id DESC
         LIMIT ?'
    );
    $fStmt->bindValue(1, $feedLimit, PDO::PARAM_INT);
    $fStmt->execute();
    $feedProducts = MelvoreProductFormatter::formatRows($fStmt->fetchAll());

    // 6. Configured Site Settings (safe public merchandising settings only)
    $siteSettings = [
        'free_shipping_minimum' => 0.0,
        'shipping_charge' => 0.0,
        'enable_cod' => true,
        'enable_online_payment' => true,
        'currency' => 'INR',
    ];
    if (MelvoreDB::tableExists('site_settings')) {
        $setStmt = $pdo->query("SELECT setting_key, setting_value FROM site_settings WHERE setting_key IN ('free_shipping_minimum','shipping_charge','enable_cod','enable_online_payment','currency')");
        foreach ($setStmt->fetchAll() as $sr) {
            $k = (string)($sr['setting_key'] ?? '');
            $v = $sr['setting_value'] ?? '';
            if ($k === 'free_shipping_minimum' || $k === 'shipping_charge') {
                $siteSettings[$k] = (float)$v;
            } elseif ($k === 'enable_cod' || $k === 'enable_online_payment') {
                $siteSettings[$k] = ((int)$v === 1 || strtolower((string)$v) === 'true');
            } elseif ($k === 'currency' && $v !== '') {
                $siteSettings['currency'] = (string)$v;
            }
        }
    }

    // 7. Promotional Banners (banner_content — column-resilient)
    $promoBanners = [];
    if (MelvoreDB::tableExists('banner_content')) {
        $bnStmt = $pdo->query("SELECT * FROM banner_content");
        foreach ($bnStmt->fetchAll() as $br) {
            $bActive = array_key_exists('is_active', $br)
                ? ((int)$br['is_active'] === 1)
                : (!array_key_exists('status', $br) || strtolower((string)$br['status']) === 'active');
            if (!$bActive) {
                continue;
            }
            $promoBanners[] = [
                'id' => (int)($br['banner_id'] ?? ($br['id'] ?? 0)),
                'title' => (string)($br['title'] ?? ($br['heading'] ?? '')),
                'subtitle' => (string)($br['subtitle'] ?? ($br['description'] ?? '')),
                'badge_text' => (string)($br['badge_text'] ?? ($br['tag'] ?? '')),
                'image_url' => MelvoreResponse::mediaUrl($br['image_url'] ?? ($br['banner_image'] ?? ($br['image'] ?? null))),
                'cta_text' => (string)($br['cta_text'] ?? 'Shop Edit'),
                'cta_link' => (string)($br['cta_link'] ?? ''),
                '_order' => (int)($br['display_order'] ?? ($br['sort_order'] ?? 0)),
            ];
        }
        usort($promoBanners, static fn(array $a, array $b): int => ($a['_order'] <=> $b['_order']) ?: ($a['id'] <=> $b['id']));
        $promoBanners = array_slice(array_map(static function (array $item): array {
            unset($item['_order']);
            return $item;
        }, $promoBanners), 0, 8);
    }

    // 8. Brand Discovery Strip
    $brands = [];
    if (MelvoreDB::tableExists('brands')) {
        $hasProfiles = MelvoreDB::tableExists('app_brand_profiles');
        if ($hasProfiles) {
            $taglineCol = MelvoreDB::columnExists('app_brand_profiles', 'tagline')
                ? 'bp.tagline'
                : (MelvoreDB::columnExists('app_brand_profiles', 'editorial_tagline') ? 'bp.editorial_tagline AS tagline' : 'NULL AS tagline');
            $coverCol = MelvoreDB::columnExists('app_brand_profiles', 'editorial_cover_url')
                ? 'bp.editorial_cover_url'
                : (MelvoreDB::columnExists('app_brand_profiles', 'cover_image_url') ? 'bp.cover_image_url AS editorial_cover_url' : 'NULL AS editorial_cover_url');
            $bSql = "SELECT b.brand_id, b.name, b.slug, b.description, b.logo,
                            $taglineCol, $coverCol, bp.origin_country,
                            bp.is_featured, bp.is_sponsored, bp.is_new, bp.trending_score, bp.display_order,
                            COUNT(p.product_id) AS product_count
                     FROM brands b
                     LEFT JOIN app_brand_profiles bp ON bp.brand_id = b.brand_id
                     LEFT JOIN products p ON p.brand_id = b.brand_id AND p.is_active = 1
                     WHERE b.status = 'active'
                     GROUP BY b.brand_id
                     ORDER BY COALESCE(bp.is_featured, 0) DESC, COALESCE(bp.display_order, 999) ASC, b.name ASC
                     LIMIT 16";
        } else {
            $bSql = "SELECT b.brand_id, b.name, b.slug, b.description, b.logo,
                            NULL AS tagline, NULL AS editorial_cover_url, NULL AS origin_country,
                            0 AS is_featured, 0 AS is_sponsored, 0 AS is_new, 0 AS trending_score, 0 AS display_order,
                            COUNT(p.product_id) AS product_count
                     FROM brands b
                     LEFT JOIN products p ON p.brand_id = b.brand_id AND p.is_active = 1
                     WHERE b.status = 'active'
                     GROUP BY b.brand_id
                     ORDER BY product_count DESC, b.name ASC
                     LIMIT 16";
        }
        foreach ($pdo->query($bSql)->fetchAll() as $row) {
            $origin = (string)($row['origin_country'] ?? '');
            $brands[] = [
                'brand_id' => (int)$row['brand_id'],
                'name' => (string)$row['name'],
                'slug' => (string)$row['slug'],
                'description' => (string)($row['description'] ?? ''),
                'tagline' => (string)($row['tagline'] ?? ''),
                'logo_url' => MelvoreResponse::mediaUrl($row['logo'] ?? null),
                'cover_image_url' => MelvoreResponse::mediaUrl($row['editorial_cover_url'] ?? null),
                'origin_country' => $origin,
                'is_imported' => $origin !== '',
                'is_featured' => ((int)($row['is_featured'] ?? 0)) === 1,
                'is_sponsored' => ((int)($row['is_sponsored'] ?? 0)) === 1,
                'is_new' => ((int)($row['is_new'] ?? 0)) === 1,
                'trending_score' => (int)($row['trending_score'] ?? 0),
                'display_order' => (int)($row['display_order'] ?? 0),
                'product_count' => (int)($row['product_count'] ?? 0),
                'is_active' => true,
            ];
        }
    }

    // 9. Style Worlds Preview
    $styleWorlds = [];
    if (MelvoreDB::tableExists('app_style_worlds')) {
        $swStmt = $pdo->query(
            'SELECT * FROM app_style_worlds
             WHERE is_active = 1
               AND (start_date IS NULL OR start_date <= NOW())
               AND (end_date IS NULL OR end_date >= NOW())
             ORDER BY display_order ASC, style_world_id ASC
             LIMIT 12'
        );
        foreach ($swStmt->fetchAll() as $sw) {
            $styleWorlds[] = [
                'style_world_id' => (int)($sw['style_world_id'] ?? ($sw['id'] ?? 0)),
                'slug' => (string)($sw['slug'] ?? ''),
                'name' => (string)($sw['name'] ?? ''),
                'tagline' => (string)($sw['tagline'] ?? ''),
                'description' => (string)($sw['description'] ?? ''),
                'theme_key' => (string)($sw['theme_key'] ?? 'editorial'),
                'animation_key' => (string)($sw['animation_key'] ?? 'smooth_reveal'),
                'font_style' => (string)($sw['font_style'] ?? 'editorial_serif'),
                'primary_color' => (string)($sw['primary_color'] ?? '#09090B'),
                'accent_color' => (string)($sw['accent_color'] ?? '#B48811'),
                'background_color' => (string)($sw['background_color'] ?? '#141418'),
                'cover_image_url' => MelvoreResponse::mediaUrl($sw['cover_image_url'] ?? ($sw['hero_image_url'] ?? null)),
                'mobile_image_url' => MelvoreResponse::mediaUrl($sw['mobile_image_url'] ?? ($sw['cover_image_url'] ?? null)),
                'video_url' => MelvoreResponse::mediaUrl($sw['video_url'] ?? null),
            ];
        }
    }

    // 10. Distinct Product Merchandising Shelves
    $fetchShelf = static function (string $extraWhere, string $orderBy, int $lim = 10) use ($pdo): array {
        $q = $pdo->prepare(
            "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM products p
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.is_active = 1 $extraWhere
             ORDER BY $orderBy
             LIMIT ?"
        );
        $q->bindValue(1, $lim, PDO::PARAM_INT);
        $q->execute();
        return MelvoreProductFormatter::formatRows($q->fetchAll());
    };

    $newArrivals = $fetchShelf('', 'p.product_id DESC', 10);
    $bestsellers = $fetchShelf('', 'p.purchase_count DESC, p.rating_count DESC, p.product_id DESC', 10);
    $trendingProducts = $fetchShelf('', 'p.view_count DESC, p.rating_average DESC, p.product_id DESC', 10);
    $mostWishlisted = $fetchShelf('', 'p.rating_average DESC, p.rating_count DESC, p.purchase_count DESC', 10);
    $budget999 = $fetchShelf('AND COALESCE(NULLIF(p.sale_price, 0), p.regular_price) <= 999', 'p.is_featured DESC, p.purchase_count DESC', 10);
    $budget1999 = $fetchShelf('AND COALESCE(NULLIF(p.sale_price, 0), p.regular_price) <= 1999', 'p.is_featured DESC, p.rating_average DESC', 10);

    $sponsoredProducts = [];
    if (MelvoreDB::tableExists('app_product_placements')) {
        $spStmt = $pdo->query(
            'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug,
                    pl.is_sponsored, pl.badge_override, pl.express_delivery
             FROM app_product_placements pl
             INNER JOIN products p ON p.product_id = pl.product_id
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE pl.is_active = 1 AND pl.is_sponsored = 1 AND p.is_active = 1
             ORDER BY pl.priority DESC, pl.display_order ASC
             LIMIT 10'
        );
        $sponsoredProducts = MelvoreProductFormatter::formatRows($spStmt->fetchAll());
    }

    // 11. Recently Viewed Products for Current Visitor (Never Fabricated)
    $recentlyViewed = [];
    if (($userId !== null || $guestId !== '') && MelvoreDB::tableExists('app_user_style_signals')) {
        $actorSql = $userId !== null ? 's.user_id = ?' : 's.guest_id = ?';
        $actorVal = $userId ?? $guestId;
        $rvStmt = $pdo->prepare(
            "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug,
                    MAX(s.created_at) AS seen_at
             FROM app_user_style_signals s
             INNER JOIN products p ON p.product_id = s.product_id
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE $actorSql AND s.product_id IS NOT NULL AND p.is_active = 1
             GROUP BY p.product_id
             ORDER BY seen_at DESC
             LIMIT 10"
        );
        $rvStmt->execute([$actorVal]);
        $recentlyViewed = MelvoreProductFormatter::formatRows($rvStmt->fetchAll());
    }

    // 12. Editorial Journal Stories & Video Reels Preview
    $editorialStories = [];
    if (MelvoreDB::tableExists('blog_posts')) {
        $stStmt = $pdo->query("SELECT id, title, slug, excerpt, featured_image, published_at, read_time FROM blog_posts WHERE status = 'published' ORDER BY published_at DESC, id DESC LIMIT 6");
        foreach ($stStmt->fetchAll() as $bp) {
            $editorialStories[] = [
                'id' => (int)$bp['id'],
                'title' => (string)($bp['title'] ?? ''),
                'slug' => (string)($bp['slug'] ?? ''),
                'excerpt' => (string)($bp['excerpt'] ?? ''),
                'featured_image' => MelvoreResponse::mediaUrl($bp['featured_image'] ?? null),
                'published_at' => (string)($bp['published_at'] ?? ''),
                'read_time' => (string)($bp['read_time'] ?? '4 min read'),
            ];
        }
    }

    $previewVideos = [];
    if (MelvoreDB::tableExists('app_product_videos')) {
        $pvStmt = $pdo->query(
            "SELECT v.video_id, v.title, v.caption, v.video_url, v.poster_url, v.thumbnail_url,
                    v.creator_name, v.is_sponsored, v.product_id, v.brand_id, b.name AS brand_name
             FROM app_product_videos v
             LEFT JOIN brands b ON b.brand_id = v.brand_id
             WHERE v.is_active = 1 AND TRIM(COALESCE(v.video_url, '')) <> ''
             ORDER BY v.is_featured DESC, v.display_order ASC
             LIMIT 8"
        );
        foreach ($pvStmt->fetchAll() as $vr) {
            $vUrl = MelvoreResponse::mediaUrl($vr['video_url'] ?? null);
            if ($vUrl === null || !filter_var($vUrl, FILTER_VALIDATE_URL)) {
                continue;
            }
            $previewVideos[] = [
                'video_id' => (int)$vr['video_id'],
                'title' => (string)($vr['title'] ?? ''),
                'caption' => (string)($vr['caption'] ?? ''),
                'video_url' => $vUrl,
                'poster_url' => MelvoreResponse::mediaUrl($vr['poster_url'] ?? ($vr['thumbnail_url'] ?? null)),
                'creator_name' => (string)($vr['creator_name'] ?? ''),
                'brand_id' => !empty($vr['brand_id']) ? (int)$vr['brand_id'] : null,
                'brand_name' => $vr['brand_name'] ?? null,
                'product_id' => !empty($vr['product_id']) ? (int)$vr['product_id'] : null,
                'is_sponsored' => ((int)($vr['is_sponsored'] ?? 0)) === 1,
            ];
        }
    }

    $expectedTables = [
        'hero_content',
        'app_campaigns',
        'app_home_sections',
        'app_brand_profiles',
        'app_style_worlds',
        'app_product_videos',
        'app_elite_packs',
        'app_product_placements',
    ];
    $missingTables = [];
    foreach ($expectedTables as $tbl) {
        if (!MelvoreDB::tableExists($tbl)) {
            $missingTables[] = $tbl;
        }
    }
    $emptyModules = [];
    if (empty($heroSlides)) $emptyModules[] = 'hero_slides';
    if (empty($sections)) $emptyModules[] = 'sections';
    if (empty($styleWorlds)) $emptyModules[] = 'style_worlds';
    if (empty($previewVideos)) $emptyModules[] = 'preview_videos';
    if (empty($editorialStories)) $emptyModules[] = 'editorial_stories';

    MelvoreResponse::ok([
        'hero_slides' => $heroSlides,
        'active_campaign' => $activeCampaigns[0] ?? null,
        'campaigns' => $activeCampaigns,
        'promo_banners' => $promoBanners,
        'site_settings' => $siteSettings,
        'categories' => $categories,
        'brands' => $brands,
        'style_worlds' => $styleWorlds,
        'sections' => $sections,
        'new_arrivals' => $newArrivals,
        'bestsellers' => $bestsellers,
        'trending_products' => $trendingProducts,
        'most_wishlisted' => $mostWishlisted,
        'budget_999' => $budget999,
        'budget_1999' => $budget1999,
        'sponsored_products' => $sponsoredProducts,
        'recently_viewed' => $recentlyViewed,
        'editorial_stories' => $editorialStories,
        'preview_videos' => $previewVideos,
        'discovery_products' => $feedProducts,
        'diagnostics' => [
            'missing_tables' => $missingTables,
            'empty_modules' => $emptyModules,
        ],
        'viewer' => [
            'authenticated' => $userId !== null,
            'has_guest_session' => $guestId !== '',
            'country' => $country,
            'state' => $state,
            'city' => $city,
            'pincode' => $pincode,
        ],
    ], 'Homepage feed loaded');
} catch (Throwable $e) {
    error_log('[Melvore][home/feed] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to load storefront homepage.', 'SERVER_ERROR', 500);
}
