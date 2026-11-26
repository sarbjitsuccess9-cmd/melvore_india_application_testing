<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();

    $packsTableExists = MelvoreDB::tableExists('app_elite_packs');
    $hasStatusCol = $packsTableExists && MelvoreDB::columnExists('app_elite_packs', 'status');
    $hasSchedCol = $packsTableExists && MelvoreDB::columnExists('app_elite_packs', 'scheduled_publish_at');
    $rawPacks = [];
    if ($packsTableExists) {
        $statusWhere = $hasStatusCol ? " AND (status IS NULL OR status = 'published')" : '';
        $schedWhere = $hasSchedCol ? ' AND (scheduled_publish_at IS NULL OR scheduled_publish_at <= NOW())' : '';
        $pStmt = $pdo->query(
            "SELECT * FROM app_elite_packs
             WHERE is_active = 1
               $statusWhere
               $schedWhere
               AND (start_date IS NULL OR start_date <= NOW())
               AND (end_date IS NULL OR end_date >= NOW())
             ORDER BY is_hero DESC, display_order ASC, pack_id DESC"
        );
        $rawPacks = $pStmt->fetchAll();
    }

    $itemsByPack = [];
    if (MelvoreDB::tableExists('app_elite_pack_items') && !empty($rawPacks)) {
        $packIds = array_map(static fn($r) => (int)($r['pack_id'] ?? $r['id']), $rawPacks);
        $ph = implode(',', array_fill(0, count($packIds), '?'));
        $iStmt = $pdo->prepare(
            "SELECT * FROM app_elite_pack_items
             WHERE pack_id IN ($ph) AND is_active = 1
             ORDER BY display_order ASC, item_id ASC"
        );
        $iStmt->execute($packIds);
        $rawItems = $iStmt->fetchAll();

        $prodIds = [];
        $brandIds = [];
        foreach ($rawItems as $it) {
            if (!empty($it['product_id'])) {
                $prodIds[] = (int)$it['product_id'];
            }
            if (!empty($it['brand_id'])) {
                $brandIds[] = (int)$it['brand_id'];
            }
        }
        $productsMap = [];
        if (!empty($prodIds)) {
            $prodIds = array_values(array_unique($prodIds));
            $pPh = implode(',', array_fill(0, count($prodIds), '?'));
            $ps = $pdo->prepare(
                "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
                 FROM products p
                 LEFT JOIN categories c ON c.category_id = p.category_id
                 LEFT JOIN brands b ON b.brand_id = p.brand_id
                 WHERE p.product_id IN ($pPh) AND p.is_active = 1"
            );
            $ps->execute($prodIds);
            foreach (MelvoreProductFormatter::formatRows($ps->fetchAll()) as $fp) {
                $productsMap[(int)$fp['product_id']] = $fp;
            }
        }

        $brandsMap = [];
        if (!empty($brandIds)) {
            $brandIds = array_values(array_unique($brandIds));
            $bPh = implode(',', array_fill(0, count($brandIds), '?'));
            $bs = $pdo->prepare("SELECT * FROM brands WHERE brand_id IN ($bPh) AND status = 'active'");
            $bs->execute($brandIds);
            foreach ($bs->fetchAll() as $br) {
                $brandsMap[(int)$br['brand_id']] = [
                    'brand_id' => (int)$br['brand_id'],
                    'name' => (string)$br['name'],
                    'slug' => (string)$br['slug'],
                    'logo_url' => MelvoreResponse::mediaUrl($br['logo'] ?? null),
                    'description' => (string)($br['description'] ?? ''),
                ];
            }
        }

        foreach ($rawItems as $it) {
            $pid = (int)($it['pack_id'] ?? 0);
            $prodObj = !empty($it['product_id']) ? ($productsMap[(int)$it['product_id']] ?? null) : null;
            $brandObj = !empty($it['brand_id']) ? ($brandsMap[(int)$it['brand_id']] ?? null) : null;
            $itemType = (string)($it['item_type'] ?? ($prodObj ? 'product' : ($brandObj ? 'brand' : 'editorial')));
            // Integrity guarantee: product items MUST render that exact product's primary catalog image
            if ($itemType === 'product' && $prodObj === null) {
                continue; // skip inactive/missing products cleanly
            }
            $resolvedImg = $itemType === 'product'
                ? ($prodObj['primary_image'] ?? null)
                : MelvoreResponse::mediaUrl($it['image_url'] ?? ($brandObj['logo_url'] ?? null));

            $itemsByPack[$pid][] = [
                'item_id' => (int)($it['item_id'] ?? ($it['id'] ?? 0)),
                'item_type' => $itemType,
                'title' => (string)(!empty($it['title']) ? $it['title'] : ($prodObj['name'] ?? ($brandObj['name'] ?? ''))),
                'subtitle' => (string)($it['subtitle'] ?? ($prodObj['brand_name'] ?? '')),
                'badge_text' => (string)($it['badge_text'] ?? ''),
                'image_url' => $resolvedImg,
                'video_url' => MelvoreResponse::mediaUrl($it['video_url'] ?? null),
                'is_sponsored' => ((int)($it['is_sponsored'] ?? 0)) === 1,
                'product' => $prodObj,
                'brand' => $brandObj,
            ];
        }
    }

    $heroPacks = [];
    $sections = [];
    foreach ($rawPacks as $rp) {
        $packId = (int)($rp['pack_id'] ?? ($rp['id'] ?? 0));
        $themeCfg = [];
        if (!empty($rp['theme_config'])) {
            $dec = is_string($rp['theme_config']) ? json_decode($rp['theme_config'], true) : $rp['theme_config'];
            if (is_array($dec)) {
                $themeCfg = $dec;
            }
        }
        $storyCopy = (string)($rp['story'] ?? ($rp['description'] ?? ($themeCfg['story'] ?? '')));
        $packDto = [
            'pack_id' => $packId,
            'slug' => (string)($rp['slug'] ?? ''),
            'section_key' => (string)($rp['section_key'] ?? 'featured_luxury'),
            'brand_name' => (string)($rp['brand_name'] ?? ''),
            'eyebrow' => (string)($rp['eyebrow'] ?? ''),
            'title' => (string)($rp['title'] ?? ''),
            'subtitle' => (string)($rp['subtitle'] ?? ''),
            'story' => $storyCopy,
            'cta_text' => (string)($rp['cta_text'] ?? 'Enter Collection'),
            'cta_link' => (string)($rp['cta_link'] ?? ''),
            'hero_image_url' => MelvoreResponse::mediaUrl($rp['hero_image_url'] ?? ($rp['banner_image_url'] ?? null)),
            'mobile_image_url' => MelvoreResponse::mediaUrl($rp['mobile_image_url'] ?? ($rp['hero_image_url'] ?? null)),
            'video_url' => MelvoreResponse::mediaUrl($rp['video_url'] ?? null),
            'animation_key' => (string)($rp['animation_key'] ?? 'metallic_shine'),
            'accent_color' => (string)($rp['accent_color'] ?? '#D4AF37'),
            'background_color' => (string)($rp['background_color'] ?? '#000000'),
            'layout_type' => (string)($rp['layout_type'] ?? ($themeCfg['layout'] ?? 'editorial_grid')),
            'featured_product_id' => !empty($rp['featured_product_id']) ? (int)$rp['featured_product_id'] : null,
            'is_hero' => ((int)($rp['is_hero'] ?? 0)) === 1,
            'is_sponsored' => ((int)($rp['is_sponsored'] ?? 0)) === 1,
            'theme_config' => $themeCfg,
            'items' => $itemsByPack[$packId] ?? [],
        ];
        if ($packDto['is_hero']) {
            $heroPacks[] = $packDto;
        } else {
            $sections[] = $packDto;
        }
    }

    // DB-backed Luxe synthesis from real active catalog products when app_elite_packs has not been seeded yet
    $synthesized = false;
    if (empty($heroPacks) && empty($sections)) {
        $synthesized = true;
        $luxStmt = $pdo->query(
            'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM products p
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.is_active = 1
             ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.is_featured DESC, p.product_id DESC
             LIMIT 32'
        );
        $luxProducts = MelvoreProductFormatter::formatRows($luxStmt->fetchAll());

        $wrapItems = static fn(array $slice, string $badge): array => array_map(static fn($lp) => [
            'item_id' => (int)$lp['product_id'],
            'item_type' => 'product',
            'title' => (string)$lp['name'],
            'subtitle' => (string)($lp['brand_name'] ?? 'Melvore Luxe'),
            'badge_text' => $badge,
            'image_url' => $lp['primary_image'],
            'video_url' => null,
            'is_sponsored' => false,
            'product' => $lp,
            'brand' => null,
        ], $slice);

        if (!empty($luxProducts)) {
            $heroSlice = array_slice($luxProducts, 0, 8);
            $heroImg = $heroSlice[0]['primary_image'] ?? null;

            $heroPacks[] = [
                'pack_id' => 9001,
                'slug' => 'the-luxe-edit',
                'section_key' => 'the_luxe_edit',
                'brand_name' => 'MELVORE LUXE',
                'eyebrow' => 'PRIVATE SHOWROOM · AUTUMN / WINTER',
                'title' => 'THE LUXE EDIT',
                'subtitle' => 'Architectural tailoring, collector leatherwork, and statement silhouettes curated in obsidian & gold.',
                'story' => 'Drawn from the highest tier of the Melvore vault, The Luxe Edit brings together structured outerwear, precision hardware, and limited-edition evening silhouettes.',
                'cta_text' => 'Enter Collection',
                'cta_link' => 'elite/pack/the-luxe-edit',
                'hero_image_url' => $heroImg,
                'mobile_image_url' => $heroImg,
                'video_url' => null,
                'animation_key' => 'metallic_shine',
                'accent_color' => '#D4AF37',
                'background_color' => '#000000',
                'is_hero' => true,
                'is_sponsored' => false,
                'theme_config' => ['layout' => 'hero_cinematic'],
                'items' => $wrapItems($heroSlice, 'LUXE EDIT'),
            ];

            // Filter leather / outerwear pieces from real DB rows if present
            $outerwearSlice = array_values(array_filter($luxProducts, static function (array $p): bool {
                $hay = strtolower(($p['name'] ?? '') . ' ' . ($p['category_name'] ?? '') . ' ' . ($p['category_slug'] ?? ''));
                return str_contains($hay, 'jacket') || str_contains($hay, 'leather') || str_contains($hay, 'coat') || str_contains($hay, 'biker');
            }));
            if (empty($outerwearSlice)) {
                $outerwearSlice = array_slice($luxProducts, 2, 6);
            } else {
                $outerwearSlice = array_slice($outerwearSlice, 0, 8);
            }

            if (!empty($outerwearSlice)) {
                $sections[] = [
                    'pack_id' => 9003,
                    'slug' => 'leather-atelier',
                    'section_key' => 'leather_atelier',
                    'brand_name' => 'ATELIER NOIR',
                    'eyebrow' => 'CRAFT & HARDWARE',
                    'title' => 'Leather Atelier & Statement Outerwear',
                    'subtitle' => 'Supple finishes, sculptural lapels, and heavyweight outerwear built to command the room.',
                    'story' => 'Every silhouette in the Leather Atelier balances structured shoulders with clean metallic detailing.',
                    'cta_text' => 'Explore Atelier',
                    'cta_link' => 'elite/pack/leather-atelier',
                    'hero_image_url' => $outerwearSlice[0]['primary_image'] ?? $heroImg,
                    'mobile_image_url' => $outerwearSlice[0]['primary_image'] ?? $heroImg,
                    'video_url' => null,
                    'animation_key' => 'metallic_shine',
                    'accent_color' => '#D4AF37',
                    'background_color' => '#050507',
                    'is_hero' => false,
                    'is_sponsored' => false,
                    'theme_config' => ['layout' => 'editorial_split'],
                    'items' => $wrapItems($outerwearSlice, 'ATELIER'),
                ];
            }

            $secondChunk = array_slice($luxProducts, min(6, max(0, count($luxProducts) - 6)), 8);
            if (!empty($secondChunk)) {
                $sections[] = [
                    'pack_id' => 9002,
                    'slug' => 'after-dark-archive',
                    'section_key' => 'after_dark_archive',
                    'brand_name' => 'MELVORE NOIR',
                    'eyebrow' => 'EVENING & STATEMENT',
                    'title' => 'The After Dark Archive',
                    'subtitle' => 'Nocturnal drapery, sharp evening cuts, and collector pieces dressed for midnight.',
                    'story' => 'Curated for evening entrances and gallery nights—monochrome palettes accented by subtle sheen.',
                    'cta_text' => 'Enter Archive',
                    'cta_link' => 'elite/pack/after-dark-archive',
                    'hero_image_url' => $secondChunk[0]['primary_image'] ?? $heroImg,
                    'mobile_image_url' => $secondChunk[0]['primary_image'] ?? $heroImg,
                    'video_url' => null,
                    'animation_key' => 'smooth_reveal',
                    'accent_color' => '#D4AF37',
                    'background_color' => '#000000',
                    'is_hero' => false,
                    'is_sponsored' => false,
                    'theme_config' => ['layout' => 'portrait_diptych'],
                    'items' => $wrapItems($secondChunk, 'AFTER DARK'),
                ];
            }

            $signatureChunk = array_slice(array_reverse($luxProducts), 0, 8);
            if (!empty($signatureChunk)) {
                $sections[] = [
                    'pack_id' => 9004,
                    'slug' => 'signature-silhouettes',
                    'section_key' => 'signature_silhouettes',
                    'brand_name' => 'CONNOISSEUR VAULT',
                    'eyebrow' => 'CURATED FOR THE CONNOISSEUR',
                    'title' => 'Signature Silhouettes & Premium Essentials',
                    'subtitle' => 'Enduring proportions and elevated staples from leading contemporary houses.',
                    'story' => 'Quiet confidence in clean lines, tactile weaves, and effortless drape.',
                    'cta_text' => 'View Vault',
                    'cta_link' => 'elite/pack/signature-silhouettes',
                    'hero_image_url' => $signatureChunk[0]['primary_image'] ?? $heroImg,
                    'mobile_image_url' => $signatureChunk[0]['primary_image'] ?? $heroImg,
                    'video_url' => null,
                    'animation_key' => 'smooth_reveal',
                    'accent_color' => '#C8CCD4',
                    'background_color' => '#08080C',
                    'is_hero' => false,
                    'is_sponsored' => false,
                    'theme_config' => ['layout' => 'showcase_grid'],
                    'items' => $wrapItems($signatureChunk, 'SIGNATURE'),
                ];
            }
        }
    }

    // Sponsored & Elite Brands Showcase (schema-safe column detection)
    $eliteBrands = [];
    if (MelvoreDB::tableExists('app_brand_profiles')) {
        $coverCol = MelvoreDB::columnExists('app_brand_profiles', 'cover_image_url')
            ? 'bp.cover_image_url'
            : (MelvoreDB::columnExists('app_brand_profiles', 'editorial_cover_url') ? 'bp.editorial_cover_url AS cover_image_url' : 'NULL AS cover_image_url');
        $videoCol = MelvoreDB::columnExists('app_brand_profiles', 'brand_video_url')
            ? 'bp.brand_video_url'
            : (MelvoreDB::columnExists('app_brand_profiles', 'video_url') ? 'bp.video_url AS brand_video_url' : 'NULL AS brand_video_url');
        $taglineCol = MelvoreDB::columnExists('app_brand_profiles', 'editorial_tagline')
            ? 'bp.editorial_tagline'
            : (MelvoreDB::columnExists('app_brand_profiles', 'tagline') ? 'bp.tagline AS editorial_tagline' : 'NULL AS editorial_tagline');

        $ebStmt = $pdo->query(
            "SELECT b.brand_id, b.name, b.slug, b.description, b.logo,
                    $coverCol, $videoCol, $taglineCol, bp.is_featured, bp.is_sponsored
             FROM brands b
             INNER JOIN app_brand_profiles bp ON bp.brand_id = b.brand_id
             WHERE b.status = 'active' AND (bp.is_featured = 1 OR bp.is_sponsored = 1)
             ORDER BY bp.is_sponsored DESC, bp.is_featured DESC, bp.display_order ASC
             LIMIT 16"
        );
        foreach ($ebStmt->fetchAll() as $eb) {
            $eliteBrands[] = [
                'brand_id' => (int)$eb['brand_id'],
                'name' => (string)$eb['name'],
                'slug' => (string)$eb['slug'],
                'description' => (string)($eb['description'] ?? ''),
                'tagline' => (string)($eb['editorial_tagline'] ?? ''),
                'logo_url' => MelvoreResponse::mediaUrl($eb['logo'] ?? null),
                'cover_image_url' => MelvoreResponse::mediaUrl($eb['cover_image_url'] ?? null),
                'brand_video_url' => MelvoreResponse::mediaUrl($eb['brand_video_url'] ?? null),
                'is_featured' => ((int)($eb['is_featured'] ?? 0)) === 1,
                'is_sponsored' => ((int)($eb['is_sponsored'] ?? 0)) === 1,
            ];
        }
    }
    if (empty($eliteBrands) && MelvoreDB::tableExists('brands')) {
        $ebStmt = $pdo->query("SELECT brand_id, name, slug, description, logo FROM brands WHERE status = 'active' ORDER BY brand_id ASC LIMIT 12");
        foreach ($ebStmt->fetchAll() as $eb) {
            $eliteBrands[] = [
                'brand_id' => (int)$eb['brand_id'],
                'name' => (string)$eb['name'],
                'slug' => (string)$eb['slug'],
                'description' => (string)($eb['description'] ?? ''),
                'tagline' => '',
                'logo_url' => MelvoreResponse::mediaUrl($eb['logo'] ?? null),
                'cover_image_url' => null,
                'brand_video_url' => null,
                'is_featured' => true,
                'is_sponsored' => false,
            ];
        }
    }

    // Sponsored / Connoisseur Products
    $sponsoredProducts = [];
    if (MelvoreDB::tableExists('app_product_placements')) {
        $spStmt = $pdo->query(
            "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug,
                    pl.is_sponsored, pl.badge_override, pl.express_delivery
             FROM app_product_placements pl
             INNER JOIN products p ON p.product_id = pl.product_id
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE pl.is_active = 1 AND pl.is_sponsored = 1 AND p.is_active = 1
               AND (pl.start_date IS NULL OR pl.start_date <= NOW())
               AND (pl.end_date IS NULL OR pl.end_date >= NOW())
             ORDER BY pl.priority DESC
             LIMIT 16"
        );
        $sponsoredProducts = MelvoreProductFormatter::formatRows($spStmt->fetchAll());
    }

    // Premium Product Videos (valid URLs only)
    $eliteVideos = [];
    if (MelvoreDB::tableExists('app_product_videos')) {
        $evStmt = $pdo->query(
            "SELECT v.video_id, v.title, v.caption, v.video_url, v.poster_url, v.thumbnail_url,
                    v.creator_name, v.is_sponsored, v.product_id, v.brand_id, b.name AS brand_name
             FROM app_product_videos v
             LEFT JOIN brands b ON b.brand_id = v.brand_id
             WHERE v.is_active = 1 AND TRIM(COALESCE(v.video_url, '')) <> ''
             ORDER BY v.is_featured DESC, v.display_order ASC
             LIMIT 8"
        );
        foreach ($evStmt->fetchAll() as $vr) {
            $vUrl = MelvoreResponse::mediaUrl($vr['video_url'] ?? null);
            if ($vUrl === null || !filter_var($vUrl, FILTER_VALIDATE_URL)) {
                continue;
            }
            $eliteVideos[] = [
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

    MelvoreResponse::ok([
        'hero_packs' => $heroPacks,
        'packs' => $sections,
        'elite_brands' => $eliteBrands,
        'sponsored_products' => $sponsoredProducts,
        'elite_videos' => $eliteVideos,
        'diagnostics' => [
            'table_exists' => $packsTableExists,
            'db_pack_count' => count($rawPacks),
            'synthesized_from_catalog' => $synthesized,
        ],
    ], 'Elite showroom loaded');
} catch (Throwable $e) {
    error_log('[Melvore][elite/home] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to load Elite showroom.', 'SERVER_ERROR', 500);
}
