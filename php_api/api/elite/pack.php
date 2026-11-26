<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $packId = (int)($_GET['id'] ?? ($_GET['pack_id'] ?? 0));
    $slug = trim((string)($_GET['slug'] ?? ''));

    if ($packId <= 0 && $slug === '') {
        MelvoreResponse::error('Elite pack id or slug is required.', 'VALIDATION_ERROR', 422);
    }

    $pack = null;
    $relatedPacks = [];
    if (MelvoreDB::tableExists('app_elite_packs')) {
        if ($packId > 0) {
            $stmt = $pdo->prepare('SELECT * FROM app_elite_packs WHERE pack_id = ? AND is_active = 1 LIMIT 1');
            $stmt->execute([$packId]);
        } else {
            $stmt = $pdo->prepare('SELECT * FROM app_elite_packs WHERE slug = ? AND is_active = 1 LIMIT 1');
            $stmt->execute([$slug]);
        }
        $pack = $stmt->fetch() ?: null;

        if ($pack) {
            $curId = (int)$pack['pack_id'];
            $relStmt = $pdo->prepare(
                'SELECT * FROM app_elite_packs
                 WHERE is_active = 1 AND pack_id <> ?
                 ORDER BY is_hero DESC, display_order ASC, pack_id DESC
                 LIMIT 6'
            );
            $relStmt->execute([$curId]);
            foreach ($relStmt->fetchAll() as $rp) {
                $relatedPacks[] = [
                    'pack_id' => (int)$rp['pack_id'],
                    'slug' => (string)($rp['slug'] ?? ''),
                    'section_key' => (string)($rp['section_key'] ?? ''),
                    'brand_name' => (string)($rp['brand_name'] ?? ''),
                    'eyebrow' => (string)($rp['eyebrow'] ?? ''),
                    'title' => (string)($rp['title'] ?? ''),
                    'subtitle' => (string)($rp['subtitle'] ?? ''),
                    'cta_text' => (string)($rp['cta_text'] ?? 'Explore Collection'),
                    'hero_image_url' => MelvoreResponse::mediaUrl($rp['hero_image_url'] ?? null),
                    'mobile_image_url' => MelvoreResponse::mediaUrl($rp['mobile_image_url'] ?? ($rp['hero_image_url'] ?? null)),
                    'accent_color' => (string)($rp['accent_color'] ?? '#D4AF37'),
                ];
            }
        }
    }

    $synthDefinitions = [
        'the-luxe-edit' => [
            'pack_id' => 9001,
            'section_key' => 'the_luxe_edit',
            'brand_name' => 'MELVORE LUXE',
            'eyebrow' => 'PRIVATE SHOWROOM · AUTUMN / WINTER',
            'title' => 'THE LUXE EDIT',
            'subtitle' => 'Curated signature pieces, heavyweight tailoring, and collector silhouettes.',
            'story' => 'Drawn from the highest tier of the Melvore vault, The Luxe Edit brings together structured outerwear, precision hardware, and limited-edition evening silhouettes.',
            'offset' => 0,
        ],
        'after-dark-archive' => [
            'pack_id' => 9002,
            'section_key' => 'after_dark_archive',
            'brand_name' => 'MELVORE NOIR',
            'eyebrow' => 'EVENING & STATEMENT',
            'title' => 'The After Dark Archive',
            'subtitle' => 'Monochrome silhouettes selected from the active luxury vault.',
            'story' => 'Curated for evening entrances and gallery nights—monochrome palettes accented by subtle champagne gold and brushed chrome.',
            'offset' => 4,
        ],
        'leather-atelier' => [
            'pack_id' => 9003,
            'section_key' => 'leather_atelier',
            'brand_name' => 'ATELIER NOIR',
            'eyebrow' => 'CRAFT & HARDWARE',
            'title' => 'Leather Atelier & Statement Outerwear',
            'subtitle' => 'Supple finishes, sculptural lapels, and heavyweight outerwear built to command the room.',
            'story' => 'Every silhouette in the Leather Atelier balances structured shoulders with clean metallic detailing.',
            'offset' => 0,
        ],
        'signature-silhouettes' => [
            'pack_id' => 9004,
            'section_key' => 'signature_silhouettes',
            'brand_name' => 'CONNOISSEUR VAULT',
            'eyebrow' => 'CURATED FOR THE CONNOISSEUR',
            'title' => 'Signature Silhouettes & Premium Essentials',
            'subtitle' => 'Enduring proportions and elevated staples from leading contemporary houses.',
            'story' => 'Quiet confidence in clean lines, tactile weaves, and effortless drape.',
            'offset' => 2,
        ],
    ];

    $matchedSynthSlug = null;
    if (!$pack) {
        if (isset($synthDefinitions[$slug])) {
            $matchedSynthSlug = $slug;
        } else {
            foreach ($synthDefinitions as $sKey => $def) {
                if ($def['pack_id'] === $packId) {
                    $matchedSynthSlug = $sKey;
                    break;
                }
            }
        }
    }

    if (!$pack && $matchedSynthSlug !== null) {
        $def = $synthDefinitions[$matchedSynthSlug];
        $luxStmt = $pdo->prepare(
            'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM products p
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.is_active = 1
             ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.is_featured DESC, p.product_id DESC
             LIMIT 14 OFFSET ?'
        );
        $luxStmt->bindValue(1, (int)$def['offset'], PDO::PARAM_INT);
        $luxStmt->execute();
        $luxProducts = MelvoreProductFormatter::formatRows($luxStmt->fetchAll());
        if (empty($luxProducts)) {
            $fallbackStmt = $pdo->query(
                'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
                 FROM products p
                 LEFT JOIN categories c ON c.category_id = p.category_id
                 LEFT JOIN brands b ON b.brand_id = p.brand_id
                 WHERE p.is_active = 1
                 ORDER BY p.product_id DESC LIMIT 12'
            );
            $luxProducts = MelvoreProductFormatter::formatRows($fallbackStmt->fetchAll());
        }

        $synthRelated = [];
        foreach ($synthDefinitions as $sKey => $rDef) {
            if ($sKey === $matchedSynthSlug) {
                continue;
            }
            $synthRelated[] = [
                'pack_id' => $rDef['pack_id'],
                'slug' => $sKey,
                'section_key' => $rDef['section_key'],
                'brand_name' => $rDef['brand_name'],
                'eyebrow' => $rDef['eyebrow'],
                'title' => $rDef['title'],
                'subtitle' => $rDef['subtitle'],
                'cta_text' => 'Enter Collection',
                'hero_image_url' => $luxProducts[0]['primary_image'] ?? null,
                'mobile_image_url' => $luxProducts[0]['primary_image'] ?? null,
                'accent_color' => '#D4AF37',
            ];
        }

        $brands = [];
        if (MelvoreDB::tableExists('brands')) {
            $bStmt = $pdo->query("SELECT brand_id, name, slug, description, logo FROM brands WHERE status = 'active' ORDER BY brand_id ASC LIMIT 6");
            foreach ($bStmt->fetchAll() as $br) {
                $brands[] = [
                    'brand_id' => (int)$br['brand_id'],
                    'name' => (string)$br['name'],
                    'slug' => (string)$br['slug'],
                    'description' => (string)($br['description'] ?? ''),
                    'logo_url' => MelvoreResponse::mediaUrl($br['logo'] ?? null),
                ];
            }
        }

        MelvoreResponse::ok([
            'pack' => [
                'pack_id' => $def['pack_id'],
                'slug' => $matchedSynthSlug,
                'section_key' => $def['section_key'],
                'brand_name' => $def['brand_name'],
                'eyebrow' => $def['eyebrow'],
                'title' => $def['title'],
                'subtitle' => $def['subtitle'],
                'story' => $def['story'],
                'hero_image_url' => $luxProducts[0]['primary_image'] ?? null,
                'mobile_image_url' => $luxProducts[0]['primary_image'] ?? null,
                'video_url' => null,
                'animation_key' => 'metallic_shine',
                'accent_color' => '#D4AF37',
                'background_color' => '#000000',
                'is_sponsored' => false,
                'items' => [],
            ],
            'products' => $luxProducts,
            'brands' => $brands,
            'related_packs' => $synthRelated,
        ], 'Elite pack details loaded');
    }

    if (!$pack) {
        MelvoreResponse::error('Elite pack not found.', 'NOT_FOUND', 404);
    }
    $pid = (int)$pack['pack_id'];

    $products = [];
    $brands = [];
    $items = [];
    if (MelvoreDB::tableExists('app_elite_pack_items')) {
        $iStmt = $pdo->prepare('SELECT * FROM app_elite_pack_items WHERE pack_id = ? AND is_active = 1 ORDER BY display_order ASC, item_id ASC');
        $iStmt->execute([$pid]);
        $rawItems = $iStmt->fetchAll();

        $prodIds = array_values(array_unique(array_filter(array_map(static fn($r) => (int)($r['product_id'] ?? 0), $rawItems))));
        $productsById = [];
        if (!empty($prodIds)) {
            $ph = implode(',', array_fill(0, count($prodIds), '?'));
            $ps = $pdo->prepare(
                "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
                 FROM products p
                 LEFT JOIN categories c ON c.category_id = p.category_id
                 LEFT JOIN brands b ON b.brand_id = p.brand_id
                 WHERE p.product_id IN ($ph) AND p.is_active = 1"
            );
            $ps->execute($prodIds);
            foreach (MelvoreProductFormatter::formatRows($ps->fetchAll()) as $fp) {
                $productsById[(int)$fp['product_id']] = $fp;
            }
            // Preserve admin display_order cleanly
            foreach ($rawItems as $it) {
                $epid = !empty($it['product_id']) ? (int)$it['product_id'] : 0;
                if ($epid > 0 && isset($productsById[$epid])) {
                    $products[] = $productsById[$epid];
                    unset($productsById[$epid]); // deduplicate
                }
            }
        }

        $brandIds = array_values(array_unique(array_filter(array_map(static fn($r) => (int)($r['brand_id'] ?? 0), $rawItems))));
        if (!empty($brandIds)) {
            $bPh = implode(',', array_fill(0, count($brandIds), '?'));
            $bs = $pdo->prepare("SELECT * FROM brands WHERE brand_id IN ($bPh) AND status = 'active'");
            $bs->execute($brandIds);
            foreach ($bs->fetchAll() as $br) {
                $brands[] = [
                    'brand_id' => (int)$br['brand_id'],
                    'name' => (string)$br['name'],
                    'slug' => (string)$br['slug'],
                    'description' => (string)($br['description'] ?? ''),
                    'logo_url' => MelvoreResponse::mediaUrl($br['logo'] ?? null),
                ];
            }
        }

        foreach ($rawItems as $it) {
            $epid = !empty($it['product_id']) ? (int)$it['product_id'] : 0;
            $itemType = (string)($it['item_type'] ?? 'product');
            $matchedProd = null;
            if ($epid > 0) {
                foreach ($products as $pr) {
                    if ((int)$pr['product_id'] === $epid) {
                        $matchedProd = $pr;
                        break;
                    }
                }
            }
            if ($itemType === 'product' && $matchedProd === null) {
                continue;
            }
            $img = $itemType === 'product'
                ? ($matchedProd['primary_image'] ?? null)
                : MelvoreResponse::mediaUrl($it['image_url'] ?? null);
            $items[] = [
                'item_id' => (int)$it['item_id'],
                'item_type' => $itemType,
                'title' => (string)(!empty($it['title']) ? $it['title'] : ($matchedProd['name'] ?? '')),
                'subtitle' => (string)($it['subtitle'] ?? ($matchedProd['brand_name'] ?? '')),
                'badge_text' => (string)($it['badge_text'] ?? ''),
                'image_url' => $img,
                'video_url' => MelvoreResponse::mediaUrl($it['video_url'] ?? null),
                'is_sponsored' => ((int)($it['is_sponsored'] ?? 0)) === 1,
                'product_id' => $epid > 0 ? $epid : null,
                'brand_id' => !empty($it['brand_id']) ? (int)$it['brand_id'] : null,
                'product' => $matchedProd,
            ];
        }
    }

    $storyCopy = (string)($pack['story'] ?? ($pack['description'] ?? ''));

    MelvoreResponse::ok([
        'pack' => [
            'pack_id' => $pid,
            'slug' => (string)($pack['slug'] ?? ''),
            'section_key' => (string)($pack['section_key'] ?? ''),
            'brand_name' => (string)($pack['brand_name'] ?? ''),
            'eyebrow' => (string)($pack['eyebrow'] ?? ''),
            'title' => (string)($pack['title'] ?? ''),
            'subtitle' => (string)($pack['subtitle'] ?? ''),
            'story' => $storyCopy,
            'hero_image_url' => MelvoreResponse::mediaUrl($pack['hero_image_url'] ?? null),
            'mobile_image_url' => MelvoreResponse::mediaUrl($pack['mobile_image_url'] ?? ($pack['hero_image_url'] ?? null)),
            'video_url' => MelvoreResponse::mediaUrl($pack['video_url'] ?? null),
            'animation_key' => (string)($pack['animation_key'] ?? 'metallic_shine'),
            'accent_color' => (string)($pack['accent_color'] ?? '#D4AF37'),
            'background_color' => (string)($pack['background_color'] ?? '#000000'),
            'layout_type' => (string)($pack['layout_type'] ?? 'editorial_grid'),
            'featured_product_id' => !empty($pack['featured_product_id']) ? (int)$pack['featured_product_id'] : null,
            'is_sponsored' => ((int)($pack['is_sponsored'] ?? 0)) === 1,
            'items' => $items,
        ],
        'products' => $products,
        'brands' => $brands,
        'related_packs' => $relatedPacks,
    ], 'Elite pack details loaded');
} catch (Throwable $e) {
    error_log('[Melvore][elite/pack] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to load Elite pack details.', 'SERVER_ERROR', 500);
}
