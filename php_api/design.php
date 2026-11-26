<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/includes/db.php';
require_once dirname(__DIR__) . '/includes/response.php';
require_once dirname(__DIR__) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $section = strtolower(trim((string)($_GET['section'] ?? 'all')));

    // Safe site_settings (strip ALL payment secrets & sensitive keys)
    $siteSettings = [
        'brand_name' => 'Melvore',
        'site_url' => MelvoreEnv::get('STOREFRONT_BASE_URL', 'https://melvore.co.in'),
        'currency' => '₹',
        'shipping_charge' => 0.0,
        'free_shipping_minimum' => 0.0,
        'enable_cod' => true,
        'enable_online_payment' => true,
        'contact_email' => 'connect@melvore.co.in',
        'contact_phone' => '+91 70423 36155',
        'site_address' => 'NCR, New Delhi 110033',
        'working_hours' => '',
        'footer_copyright' => '© 2026 Melvore. All rights reserved.',
        'show_tracking_id_to_customer' => true,
        'show_courier_name_to_customer' => true,
        'tracking_page_support_text' => '',
        'order_tracking_hero_text' => 'Your order, every step of the way.',
    ];
    if (MelvoreDB::tableExists('site_settings')) {
        $stmt = $pdo->query("SELECT setting_key, setting_value FROM site_settings");
        foreach ($stmt->fetchAll() as $r) {
            $k = (string)($r['setting_key'] ?? '');
            $v = (string)($r['setting_value'] ?? '');
            if (
                str_contains($k, 'secret') ||
                str_contains($k, 'cashfree') ||
                str_contains($k, 'razorpay') ||
                str_contains($k, 'password') ||
                str_contains($k, 'webhook')
            ) {
                continue;
            }
            if (in_array($k, ['shipping_charge', 'free_shipping_minimum'], true)) {
                $siteSettings[$k] = (float)$v;
            } elseif (in_array($k, ['enable_cod', 'enable_online_payment', 'show_tracking_id_to_customer', 'show_courier_name_to_customer'], true)) {
                $siteSettings[$k] = ((int)$v === 1 || strtolower($v) === 'true');
            } elseif ($k === 'currency' && $v !== '') {
                $siteSettings['currency'] = $v === 'INR' ? '₹' : $v;
            } elseif (array_key_exists($k, $siteSettings) && $v !== '') {
                $siteSettings[$k] = $v;
            }
        }
    }

    $heroSlides = [];
    if (($section === 'all' || $section === 'hero') && MelvoreDB::tableExists('hero_content')) {
        foreach ($pdo->query('SELECT * FROM hero_content')->fetchAll() as $r) {
            $isActiveFlag = array_key_exists('is_active', $r)
                ? ((int)$r['is_active'] === 1)
                : (!array_key_exists('status', $r) || strtolower((string)$r['status']) === 'active');
            if (!$isActiveFlag) {
                continue;
            }
            $primaryImg = $r['image_url'] ?? ($r['desktop_image_url'] ?? ($r['image'] ?? ($r['banner_image'] ?? ($r['desktop_image'] ?? ($r['img_src'] ?? null)))));
            $heroSlides[] = [
                'id' => (int)($r['id'] ?? ($r['hero_id'] ?? 0)),
                'title' => (string)($r['title'] ?? ($r['heading'] ?? '')),
                'subtitle' => (string)($r['subtitle'] ?? ($r['description'] ?? '')),
                'eyebrow' => (string)($r['eyebrow'] ?? ($r['badge_text'] ?? '')),
                'image_url' => MelvoreResponse::mediaUrl($primaryImg),
                'mobile_image_url' => MelvoreResponse::mediaUrl($r['mobile_image_url'] ?? ($r['mobile_image'] ?? $primaryImg)),
                'desktop_image_url' => MelvoreResponse::mediaUrl($r['desktop_image_url'] ?? ($r['desktop_image'] ?? $primaryImg)),
                'cta_text' => (string)($r['cta_text'] ?? ($r['button_text'] ?? '')),
                'cta_link' => (string)($r['cta_link'] ?? ($r['button_link'] ?? '')),
                'display_order' => (int)($r['display_order'] ?? ($r['sort_order'] ?? 0)),
                'is_active' => true,
            ];
        }
        usort($heroSlides, static fn(array $a, array $b): int => ($a['display_order'] <=> $b['display_order']) ?: ($a['id'] <=> $b['id']));
    }

    $banners = [];
    if (($section === 'all' || $section === 'banners') && MelvoreDB::tableExists('banner_content')) {
        foreach ($pdo->query('SELECT * FROM banner_content')->fetchAll() as $b) {
            $bActive = array_key_exists('is_active', $b)
                ? ((int)$b['is_active'] === 1)
                : (!array_key_exists('status', $b) || strtolower((string)$b['status']) === 'active');
            if (!$bActive) {
                continue;
            }
            $banners[] = [
                'banner_id' => (int)($b['banner_id'] ?? ($b['id'] ?? 0)),
                'title' => (string)($b['title'] ?? ($b['heading'] ?? '')),
                'subtitle' => (string)($b['subtitle'] ?? ($b['description'] ?? '')),
                'tag' => (string)($b['tag'] ?? ($b['badge_text'] ?? '')),
                'image_url' => MelvoreResponse::mediaUrl($b['image_url'] ?? ($b['banner_image'] ?? ($b['image'] ?? null))),
                'cta_text' => (string)($b['cta_text'] ?? 'Shop Edit'),
                'cta_link' => (string)($b['cta_link'] ?? ''),
                'filter_tag' => (string)($b['filter_tag'] ?? ''),
                'position' => (string)($b['position'] ?? 'home'),
                '_order' => (int)($b['display_order'] ?? ($b['sort_order'] ?? 0)),
            ];
        }
        usort($banners, static fn(array $a, array $b): int => ($a['_order'] <=> $b['_order']) ?: ($a['banner_id'] <=> $b['banner_id']));
        $banners = array_slice(array_map(static function (array $item): array {
            unset($item['_order']);
            return $item;
        }, $banners), 0, 12);
    }

    $marquee = [];
    if (MelvoreDB::tableExists('marquee_items')) {
        foreach ($pdo->query('SELECT * FROM marquee_items WHERE is_active = 1 ORDER BY display_order ASC LIMIT 16')->fetchAll() as $m) {
            $marquee[] = [
                'id' => (int)($m['id'] ?? 0),
                'text' => (string)($m['text'] ?? ($m['title'] ?? '')),
                'icon' => (string)($m['icon'] ?? ''),
            ];
        }
    }

    $featureTiles = [];
    if (MelvoreDB::tableExists('feature_tiles')) {
        foreach ($pdo->query('SELECT * FROM feature_tiles WHERE is_active = 1 ORDER BY display_order ASC LIMIT 8')->fetchAll() as $ft) {
            $featureTiles[] = [
                'feature_id' => (int)($ft['feature_id'] ?? ($ft['id'] ?? 0)),
                'icon' => (string)($ft['icon'] ?? ''),
                'title' => (string)($ft['title'] ?? ''),
                'description' => (string)($ft['description'] ?? ''),
            ];
        }
    }

    $categories = [];
    if (MelvoreDB::tableExists('categories')) {
        $cRows = $pdo->query(
            "SELECT c.category_id, c.name, c.slug, c.description, c.image, c.is_featured, COUNT(p.product_id) AS product_count
             FROM categories c
             LEFT JOIN products p ON p.category_id = c.category_id AND p.is_active = 1
             WHERE c.status = 'active'
             GROUP BY c.category_id
             ORDER BY c.display_order ASC, c.is_featured DESC, c.name ASC"
        )->fetchAll();
        foreach ($cRows as $c) {
            $categories[] = [
                'category_id' => (int)$c['category_id'],
                'name' => (string)$c['name'],
                'slug' => (string)$c['slug'],
                'description' => $c['description'] ?? null,
                'image_url' => MelvoreResponse::mediaUrl($c['image'] ?? null),
                'is_featured' => ((int)($c['is_featured'] ?? 0)) === 1,
                'product_count' => (int)($c['product_count'] ?? 0),
            ];
        }
    }

    $brands = [];
    if (MelvoreDB::tableExists('brands')) {
        $hasProfiles = MelvoreDB::tableExists('app_brand_profiles');
        $bSql = $hasProfiles
            ? "SELECT b.brand_id, b.name, b.slug, b.description, b.logo, b.status, bp.origin_country, COUNT(p.product_id) AS product_count
               FROM brands b
               LEFT JOIN app_brand_profiles bp ON bp.brand_id = b.brand_id
               LEFT JOIN products p ON p.brand_id = b.brand_id AND p.is_active = 1
               WHERE b.status = 'active'
               GROUP BY b.brand_id
               ORDER BY COALESCE(bp.display_order, 999) ASC, b.name ASC"
            : "SELECT b.brand_id, b.name, b.slug, b.description, b.logo, b.status, NULL AS origin_country, COUNT(p.product_id) AS product_count
               FROM brands b
               LEFT JOIN products p ON p.brand_id = b.brand_id AND p.is_active = 1
               WHERE b.status = 'active'
               GROUP BY b.brand_id
               ORDER BY b.name ASC";
        foreach ($pdo->query($bSql)->fetchAll() as $b) {
            $brands[] = [
                'brand_id' => (int)$b['brand_id'],
                'id' => (int)$b['brand_id'],
                'name' => (string)$b['name'],
                'brand_name' => (string)$b['name'],
                'slug' => (string)$b['slug'],
                'description' => $b['description'] ?? null,
                'logo' => MelvoreResponse::mediaUrl($b['logo'] ?? null),
                'logo_url' => MelvoreResponse::mediaUrl($b['logo'] ?? null),
                'origin_country' => $b['origin_country'] ?? null,
                'is_imported' => !empty($b['origin_country']),
                'product_count' => (int)($b['product_count'] ?? 0),
                'is_active' => true,
                'status' => (string)($b['status'] ?? 'active'),
            ];
        }
    }

    $collections = [];
    if (MelvoreDB::tableExists('collections')) {
        foreach ($pdo->query('SELECT * FROM collections WHERE is_active = 1 ORDER BY display_order ASC')->fetchAll() as $col) {
            $collections[] = [
                'id' => (int)$col['id'],
                'name' => (string)$col['name'],
                'subtitle' => (string)($col['subtitle'] ?? ''),
                'slug' => (string)($col['slug'] ?? ''),
                'banner_url' => MelvoreResponse::mediaUrl($col['banner'] ?? null),
            ];
        }
    }

    $trendingTags = [];
    if (MelvoreDB::tableExists('trending_tags')) {
        foreach ($pdo->query('SELECT * FROM trending_tags WHERE is_active = 1 ORDER BY display_order ASC LIMIT 16')->fetchAll() as $tt) {
            $trendingTags[] = [
                'tag_id' => (int)($tt['tag_id'] ?? ($tt['id'] ?? 0)),
                'name' => (string)($tt['name'] ?? ''),
                'icon' => $tt['icon'] ?? null,
            ];
        }
    }

    $featured = [];
    $fStmt = $pdo->query(
        'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
         FROM products p
         LEFT JOIN categories c ON c.category_id = p.category_id
         LEFT JOIN brands b ON b.brand_id = p.brand_id
         WHERE p.is_active = 1 AND p.is_featured = 1
         ORDER BY p.product_id DESC
         LIMIT 12'
    );
    $featured = MelvoreProductFormatter::formatRows($fStmt->fetchAll());

    $testimonials = [];
    if (MelvoreDB::tableExists('testimonials')) {
        foreach ($pdo->query('SELECT * FROM testimonials WHERE is_approved = 1 ORDER BY testimonial_id DESC LIMIT 10')->fetchAll() as $t) {
            $testimonials[] = [
                'testimonial_id' => (int)($t['testimonial_id'] ?? 0),
                'customer_name' => (string)($t['customer_name'] ?? 'Melvore Customer'),
                'customer_image' => MelvoreResponse::mediaUrl($t['customer_image'] ?? null),
                'customer_title' => (string)($t['customer_title'] ?? 'Verified Buyer'),
                'rating' => (int)($t['rating'] ?? 5),
                'content' => (string)($t['content'] ?? ''),
            ];
        }
    }

    $siteStats = [];
    if (MelvoreDB::tableExists('site_stats')) {
        foreach ($pdo->query('SELECT * FROM site_stats ORDER BY stat_id ASC LIMIT 8')->fetchAll() as $s) {
            $siteStats[] = [
                'stat_id' => (int)($s['stat_id'] ?? 0),
                'label' => (string)($s['label'] ?? ''),
                'value' => (string)($s['value'] ?? ''),
            ];
        }
    }

    $teamMembers = [];
    if (MelvoreDB::tableExists('team_members')) {
        foreach ($pdo->query('SELECT * FROM team_members ORDER BY id ASC LIMIT 12')->fetchAll() as $tm) {
            $teamMembers[] = [
                'id' => (int)$tm['id'],
                'name' => (string)($tm['name'] ?? ''),
                'position' => (string)($tm['position'] ?? ''),
                'bio' => (string)($tm['bio'] ?? ''),
                'image_url' => MelvoreResponse::mediaUrl($tm['image_url'] ?? null),
            ];
        }
    }

    $milestones = [];
    if (MelvoreDB::tableExists('milestones')) {
        foreach ($pdo->query('SELECT * FROM milestones ORDER BY id ASC LIMIT 12')->fetchAll() as $ms) {
            $milestones[] = [
                'id' => (int)$ms['id'],
                'year' => (string)($ms['year'] ?? ''),
                'title' => (string)($ms['title'] ?? ''),
                'description' => (string)($ms['description'] ?? ''),
            ];
        }
    }

    $faqs = [];
    if (MelvoreDB::tableExists('faqs')) {
        foreach ($pdo->query('SELECT * FROM faqs WHERE is_active = 1 ORDER BY display_order ASC, faq_id ASC')->fetchAll() as $f) {
            $faqs[] = [
                'faq_id' => (int)($f['faq_id'] ?? 0),
                'question' => (string)($f['question'] ?? ''),
                'answer' => (string)($f['answer'] ?? ''),
                'category' => (string)($f['category'] ?? 'General'),
                'display_order' => (int)($f['display_order'] ?? 0),
            ];
        }
    }

    MelvoreResponse::okFlat([
        'site_settings' => $siteSettings,
        'hero_slides' => $heroSlides,
        'banners' => $banners,
        'marquee' => $marquee,
        'feature_tiles' => $featureTiles,
        'categories' => $categories,
        'brands' => $brands,
        'collections' => $collections,
        'trending_tags' => $trendingTags,
        'featured' => $featured,
        'testimonials' => $testimonials,
        'site_stats' => $siteStats,
        'team_members' => $teamMembers,
        'milestones' => $milestones,
        'faqs' => $faqs,
    ], 'Design bundle loaded');
} catch (Throwable $e) {
    error_log('[Melvore][design] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to load design bundle.', 'SERVER_ERROR', 500);
}
