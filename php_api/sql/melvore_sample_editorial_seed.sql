-- ============================================================================
-- MELVORE DYNAMIC EDITORIAL, STYLE WORLDS, LUXE & SEARCH SEED SQL
-- Uses dynamic INSERT ... SELECT against live `products`, `categories`, and `brands`
-- so zero fake product IDs or orphan foreign keys are ever inserted.
-- Run AFTER `melvore_customer_app_experience_migration.sql`.
-- ============================================================================

SET NAMES utf8mb4;

-- 1. Hero Slides (populates hero_content if table exists, using real active product photography)
INSERT INTO `hero_content` (`eyebrow`, `title`, `subtitle`, `image_url`, `mobile_image_url`, `cta_text`, `cta_link`, `display_order`, `is_active`)
SELECT 'AUTUMN / WINTER EDIT', 'The Leather Atelier', 'Sculpted biker silhouettes, heavyweight hardware, and statement outerwear.',
       p.img_src, p.img_src, 'Shop Leather Edit', 'jackets', 1, 1
FROM `products` p
WHERE p.is_active = 1 AND p.img_src IS NOT NULL AND p.img_src <> ''
  AND NOT EXISTS (SELECT 1 FROM `hero_content` WHERE `title` = 'The Leather Atelier')
ORDER BY p.is_featured DESC, p.product_id DESC
LIMIT 1;

INSERT INTO `hero_content` (`eyebrow`, `title`, `subtitle`, `image_url`, `mobile_image_url`, `cta_text`, `cta_link`, `display_order`, `is_active`)
SELECT 'SEOUL & STREET STORIES', 'New Season, New Rules', 'Relaxed proportions, contemporary layering, and everyday statement pieces.',
       p.img_src, p.img_src, 'Shop New Arrivals', 'new-arrivals', 2, 1
FROM `products` p
WHERE p.is_active = 1 AND p.img_src IS NOT NULL AND p.img_src <> ''
  AND NOT EXISTS (SELECT 1 FROM `hero_content` WHERE `title` = 'New Season, New Rules')
ORDER BY p.product_id DESC
LIMIT 1 OFFSET 1;

INSERT INTO `hero_content` (`eyebrow`, `title`, `subtitle`, `image_url`, `mobile_image_url`, `cta_text`, `cta_link`, `display_order`, `is_active`)
SELECT 'EVENING CAPSULE', 'After Dark Silhouettes', 'Sharp monochrome cuts and refined textures built for nightfall.',
       p.img_src, p.img_src, 'Explore Capsule', 'all', 3, 1
FROM `products` p
WHERE p.is_active = 1 AND p.img_src IS NOT NULL AND p.img_src <> ''
  AND NOT EXISTS (SELECT 1 FROM `hero_content` WHERE `title` = 'After Dark Silhouettes')
ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC
LIMIT 1;

-- 2. Active Nationwide Editorial Campaign
INSERT INTO `app_campaigns`
  (`slug`, `title`, `subtitle`, `festival_key`, `animation_key`, `primary_color`, `accent_color`, `background_color`, `banner_image_url`, `mobile_banner_url`, `cta_text`, `cta_link`, `priority`, `is_active`)
SELECT 'winter-leather-edit', 'The Winter & Leather Edit', 'Statement jackets, structured layering, and cold-weather icons.',
       'winter', 'smooth_reveal', '#09090B', '#D4AF37', '#FAF8F5', p.img_src, p.img_src, 'Explore Winter Edit', 'jackets', 100, 1
FROM `products` p
WHERE p.is_active = 1 AND p.img_src IS NOT NULL AND p.img_src <> ''
ORDER BY p.is_featured DESC, p.product_id DESC
LIMIT 1
ON DUPLICATE KEY UPDATE `is_active` = VALUES(`is_active`), `priority` = VALUES(`priority`);

-- 3. 10 Homepage Merchandising & Editorial Sections
INSERT INTO `app_home_sections`
  (`section_key`, `section_type`, `eyebrow`, `title`, `subtitle`, `animation_key`, `theme_key`, `cta_text`, `cta_link`, `item_limit`, `display_order`, `is_active`)
VALUES
  ('winter_collection', 'editorial_campaign', 'SEASONAL STORY', 'The Winter Collection', 'Heavyweight outer layers, brushed textures, and cold-season tailoring.', 'smooth_reveal', 'light_editorial', 'Shop Winter', 'jackets', 8, 10, 1),
  ('leather_edit', 'editorial_campaign', 'SIGNATURE CRAFT', 'The Leather Edit', 'Precision biker cuts, cropped silhouettes, and vintage-washed finishes.', 'smooth_reveal', 'light_editorial', 'Shop Leather', 'jackets', 8, 20, 1),
  ('streetwear_stories', 'product_shelf', 'URBAN ARCHIVE', 'Streetwear Stories', 'Oversized graphic tees, wide-leg cargos, and off-duty staples.', 'smooth_reveal', 'streetwear', 'Shop Streetwear', 't-shirts', 8, 30, 1),
  ('korean_fashion', 'editorial_campaign', 'SEOUL DISPATCH', 'Korean Fashion Edit', 'Clean lines, soft neutral layering, and effortless K-fashion proportions.', 'smooth_reveal', 'minimal_editorial', 'Explore Seoul Edit', 'shirts', 8, 40, 1),
  ('weekend_wardrobe', 'product_shelf', 'OFF-DUTY', 'The Weekend Wardrobe', 'Co-ord sets, relaxed shirting, and easy silhouettes from morning to dusk.', 'smooth_reveal', 'warm_ivory', 'Shop Co-ords', 'co-ords', 8, 50, 1),
  ('statement_jackets', 'product_grid', 'OUTERWEAR ICONS', 'Statement Jackets', 'Studded, cropped, hybrid, and Moto silhouettes engineered to stand out.', 'smooth_reveal', 'light_editorial', 'View Outerwear', 'jackets', 8, 60, 1),
  ('after_dark', 'editorial_campaign', 'NIGHT OUT', 'After Dark', 'Sleek monochrome dressing and evening-ready statement pieces.', 'metallic_shine', 'dark_accent', 'Shop Evening', 'dresses', 8, 70, 1),
  ('everyday_essentials', 'product_shelf', 'CAPSULE CORE', 'Everyday Essentials', 'Foundational tees, crisp shirting, and daily denim built to repeat.', 'smooth_reveal', 'light_editorial', 'Shop Essentials', 'all', 10, 80, 1),
  ('minimal_style', 'product_shelf', 'QUIET TAILORING', 'Minimal Style', 'Uncluttered palettes, tonal textures, and refined everyday cuts.', 'smooth_reveal', 'minimal_editorial', 'Shop Minimal', 'shirts', 8, 90, 1)
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `subtitle` = VALUES(`subtitle`),
  `display_order` = VALUES(`display_order`),
  `is_active` = 1;

-- 4. Brand Editorial Profiles (dynamically enriches existing active brands)
INSERT INTO `app_brand_profiles` (`brand_id`, `tagline`, `origin_country`, `is_featured`, `is_sponsored`, `is_new`, `trending_score`, `display_order`)
SELECT b.brand_id,
       CONCAT('Curated ', b.name, ' seasonal edit & signature silhouettes.'),
       'India',
       1,
       0,
       CASE WHEN b.brand_id % 3 = 0 THEN 1 ELSE 0 END,
       80,
       b.brand_id
FROM `brands` b
WHERE b.status = 'active'
ON DUPLICATE KEY UPDATE `is_featured` = VALUES(`is_featured`);

-- 5. 11 Distinct Style Worlds (each with its own palette, typography & aesthetic identity)
INSERT INTO `app_style_worlds`
  (`slug`, `name`, `tagline`, `description`, `theme_key`, `animation_key`, `font_style`, `primary_color`, `accent_color`, `background_color`, `cover_image_url`, `mobile_image_url`, `display_order`, `is_active`)
SELECT sw.slug, sw.name, sw.tagline, sw.description, sw.theme_key, sw.animation_key, sw.font_style,
       sw.primary_color, sw.accent_color, sw.background_color,
       (SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND p.img_src IS NOT NULL AND p.img_src <> '' ORDER BY p.product_id DESC LIMIT 1),
       (SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND p.img_src IS NOT NULL AND p.img_src <> '' ORDER BY p.product_id DESC LIMIT 1),
       sw.display_order, 1
FROM (
  SELECT 'y2k' AS slug, 'Y2K' AS name, 'Chrome hardware, baby tees & cyber-pop nostalgia' AS tagline,
         'High-shine metallics, cropped moto jackets, low-slung silhouettes, and early-2000s attitude.' AS description,
         'y2k' AS theme_key, 'pulse_glow' AS animation_key, 'futuristic' AS font_style,
         '#FFFFFF' AS primary_color, '#EC4899' AS accent_color, '#161224' AS background_color, 1 AS display_order
  UNION ALL
  SELECT 'goth', 'Goth', 'Nocturnal leather, silver studs & dark romanticism',
         'Obsidian layering, architectural hardware, dramatic collars, and velvet-dark contrast.',
         'goth', 'metallic_shine', 'gothic', '#F4F4F5', '#DC2626', '#09090B', 2
  UNION ALL
  SELECT 'coquette', 'Coquette', 'Soft blush, delicate drape & romantic tailoring',
         'Poetic silhouettes, pearl-toned neutrals, ribbon accents, and feminine poise.',
         'coquette', 'smooth_reveal', 'coquette', '#1F1619', '#DB2777', '#FFF5F7', 3
  UNION ALL
  SELECT 'streetwear', 'Streetwear', 'Boxy graphic cuts, utility cargos & concrete energy',
         'Heavyweight jersey, dropped shoulders, moto layers, and city-ready proportions.',
         'streetwear', 'kinetic_slide', 'futuristic', '#FFFFFF', '#3B82F6', '#111318', 4
  UNION ALL
  SELECT 'k-fashion', 'K-Fashion', 'Seoul layering, crisp proportions & tonal harmony',
         'Clean cropped outerwear, wide-leg trousers, and effortless Korean street tailoring.',
         'k_fashion', 'smooth_reveal', 'clean_sans', '#18181B', '#2563EB', '#F4F5F8', 5
  UNION ALL
  SELECT 'quiet-luxury', 'Quiet Luxury', 'Unbranded excellence, tactile weaves & pure form',
         'Restrained camel, ivory, and charcoal pieces where fabric and cut speak first.',
         'quiet_luxury', 'smooth_reveal', 'editorial_serif', '#1C1917', '#B48811', '#F7F4EF', 6
  UNION ALL
  SELECT 'minimal', 'Minimal', 'Monochrome clarity & architectural essentials',
         'Pared-back wardrobe anchors engineered for daily versatility.',
         'minimal', 'smooth_reveal', 'clean_sans', '#09090B', '#52525B', '#FAFAFA', 7
  UNION ALL
  SELECT 'grunge', 'Grunge', 'Distressed washes, plaid layers & raw moto edge',
         'Washed black denim, oversized flannels, and worn-in leather attitude.',
         'grunge', 'smooth_reveal', 'futuristic', '#F4F4F5', '#D97706', '#181512', 8
  UNION ALL
  SELECT 'old-money', 'Old Money', 'Heritage knits, structured blazers & club classics',
         'Timeless tailoring, polo collars, and heirloom-inspired neutral palettes.',
         'old_money', 'smooth_reveal', 'editorial_serif', '#14261C', '#B48811', '#F4F1EA', 9
  UNION ALL
  SELECT 'casual', 'Casual', 'Relaxed everyday staples & effortless comfort',
         'Breathable cottons, clean tees, and weekend-ready fits.',
         'casual', 'smooth_reveal', 'clean_sans', '#0F172A', '#059669', '#F8FAFC', 10
  UNION ALL
  SELECT 'party-wear', 'Party Wear', 'High-impact shimmer, sharp lapels & midnight glamour',
         'Statement dresses, metallic accents, and after-hours tailoring.',
         'party_wear', 'metallic_shine', 'editorial_serif', '#FFFFFF', '#D4AF37', '#0D0A14', 11
) sw
ON DUPLICATE KEY UPDATE
  `tagline` = VALUES(`tagline`),
  `description` = VALUES(`description`),
  `theme_key` = VALUES(`theme_key`),
  `font_style` = VALUES(`font_style`),
  `primary_color` = VALUES(`primary_color`),
  `accent_color` = VALUES(`accent_color`),
  `background_color` = VALUES(`background_color`),
  `is_active` = 1;

-- Map active catalog products into Style Worlds dynamically
INSERT IGNORE INTO `app_style_world_products` (`style_world_id`, `product_id`, `display_order`)
SELECT sw.style_world_id, p.product_id, p.product_id
FROM `app_style_worlds` sw
JOIN `products` p ON p.is_active = 1
WHERE (
  (sw.slug IN ('goth', 'grunge', 'y2k', 'streetwear') AND (LOWER(p.name) LIKE '%jacket%' OR LOWER(p.name) LIKE '%leather%' OR LOWER(p.name) LIKE '%biker%' OR LOWER(p.name) LIKE '%studded%' OR p.is_featured = 1))
  OR (sw.slug IN ('k-fashion', 'minimal', 'quiet-luxury', 'old-money', 'casual', 'coquette', 'party-wear'))
);

-- 6. Melvore Luxe / Elite Showroom Packs & Real Product Items
INSERT INTO `app_elite_packs`
  (`slug`, `section_key`, `brand_name`, `eyebrow`, `title`, `subtitle`, `story`, `cta_text`, `cta_link`, `hero_image_url`, `mobile_image_url`, `animation_key`, `accent_color`, `background_color`, `is_hero`, `display_order`, `is_active`)
SELECT p_meta.slug, p_meta.section_key, p_meta.brand_name, p_meta.eyebrow, p_meta.title, p_meta.subtitle, p_meta.story,
       p_meta.cta_text, CONCAT('elite/pack/', p_meta.slug),
       (SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND p.img_src IS NOT NULL AND p.img_src <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC LIMIT 1),
       (SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND p.img_src IS NOT NULL AND p.img_src <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC LIMIT 1),
       'metallic_shine', '#D4AF37', '#000000', p_meta.is_hero, p_meta.display_order, 1
FROM (
  SELECT 'the-luxe-edit' AS slug, 'the_luxe_edit' AS section_key, 'MELVORE LUXE' AS brand_name,
         'PRIVATE SHOWROOM · AUTUMN / WINTER' AS eyebrow, 'THE LUXE EDIT' AS title,
         'Architectural tailoring, collector leatherwork, and statement silhouettes in obsidian & gold.' AS subtitle,
         'Drawn from the highest tier of the Melvore vault, The Luxe Edit unites structured outerwear, custom hardware, and limited-run evening pieces.' AS story,
         'Enter Collection' AS cta_text, 1 AS is_hero, 1 AS display_order
  UNION ALL
  SELECT 'leather-atelier', 'leather_atelier', 'ATELIER NOIR',
         'CRAFT & HARDWARE', 'Leather Atelier',
         'Supple hides, asymmetric zips, and hand-finished outerwear icons.',
         'Inspired by bespoke moto ateliers, each jacket pairs structured shoulders with enduring hardware.',
         'Explore Atelier', 0, 10
  UNION ALL
  SELECT 'statement-outerwear', 'statement_outerwear', 'MAISON MELVORE',
         'SCULPTURAL LAYERS', 'Statement Outerwear',
         'Commanding coats, cropped moto silhouettes, and cold-weather armor.',
         'Designed to anchor the silhouette from the first glance.',
         'View Outerwear', 0, 20
  UNION ALL
  SELECT 'after-dark-archive', 'after_dark_archive', 'MELVORE NOIR',
         'EVENING EDIT', 'The After Dark Archive',
         'Nocturnal drapery, sharp evening cuts, and collector pieces dressed for midnight.',
         'Monochrome palettes accented by subtle champagne gold and brushed silver.',
         'Enter Archive', 0, 30
  UNION ALL
  SELECT 'signature-silhouettes', 'signature_silhouettes', 'CONNOISSEUR VAULT',
         'CURATED FOR THE CONNOISSEUR', 'Signature Silhouettes & Premium Essentials',
         'Enduring proportions and elevated staples from leading contemporary houses.',
         'Quiet confidence in clean lines, tactile weaves, and effortless drape.',
         'Explore Vault', 0, 40
) p_meta
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `subtitle` = VALUES(`subtitle`),
  `story` = VALUES(`story`),
  `is_hero` = VALUES(`is_hero`),
  `display_order` = VALUES(`display_order`),
  `is_active` = 1;

-- Populate app_elite_pack_items dynamically from top-priced active products
INSERT INTO `app_elite_pack_items` (`pack_id`, `item_type`, `product_id`, `brand_id`, `title`, `subtitle`, `badge_text`, `image_url`, `display_order`, `is_active`)
SELECT ep.pack_id, 'product', p.product_id, p.brand_id, p.name, COALESCE(b.name, 'Melvore Luxe'), 'LUXE VAULT', p.img_src, p.product_id, 1
FROM `app_elite_packs` ep
JOIN (
  SELECT product_id, brand_id, name, img_src
  FROM `products`
  WHERE is_active = 1
  ORDER BY COALESCE(NULLIF(sale_price, 0), regular_price) DESC, is_featured DESC
  LIMIT 12
) p
LEFT JOIN `brands` b ON b.brand_id = p.brand_id
WHERE NOT EXISTS (
  SELECT 1 FROM `app_elite_pack_items` epi WHERE epi.pack_id = ep.pack_id AND epi.product_id = p.product_id
);

-- 7. Search Synonyms for High-Recall Fashion Discovery
INSERT INTO `app_search_synonyms` (`term`, `expanded_terms`, `is_active`) VALUES
  ('coords', 'co-ord,co-ords,coord set,matching set,two piece', 1),
  ('co-ords', 'coord,coords,co-ord set,matching set', 1),
  ('tee', 't-shirt,tshirt,oversized tee,graphic tee', 1),
  ('tshirt', 't-shirt,tee,oversized t-shirt', 1),
  ('biker', 'leather jacket,moto jacket,studded jacket,outerwear', 1),
  ('korean', 'k-fashion,seoul,oversized,cropped jacket,minimal', 1),
  ('winter', 'jacket,coat,hoodie,sweater,layering,leather', 1),
  ('party', 'evening,dress,after dark,luxe,statement', 1)
ON DUPLICATE KEY UPDATE `expanded_terms` = VALUES(`expanded_terms`), `is_active` = 1;

-- 8. Product Videos Template (IMPORTANT: Only insert real, publicly reachable HTTPS MP4/HLS URLs hosted on Melvore CDN)
-- Example template once studio MP4 reels are uploaded to https://melvore.co.in/uploads/videos/:
/*
INSERT INTO `app_product_videos`
  (`product_id`, `brand_id`, `title`, `caption`, `creator_name`, `creator_handle`, `video_url`, `poster_url`, `duration_seconds`, `aspect_ratio`, `is_featured`, `display_order`, `is_active`)
SELECT p.product_id, p.brand_id,
       CONCAT(p.name, ' — Studio Motion Look'),
       'Shot in 9:16 vertical motion. Tap Shop This Look for sizes and instant bag add.',
       COALESCE(b.name, 'Melvore Studio'),
       '@melvore.official',
       'https://melvore.co.in/uploads/videos/reel_leather_01.mp4',
       p.img_src,
       18, '9:16', 1, 1, 1
FROM `products` p
LEFT JOIN `brands` b ON b.brand_id = p.brand_id
WHERE p.is_active = 1
ORDER BY p.is_featured DESC, p.product_id DESC
LIMIT 1;
*/
