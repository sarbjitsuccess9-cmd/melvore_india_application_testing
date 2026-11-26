-- ============================================================================
-- MELVORE — FIXED NON-DESTRUCTIVE EXPERIENCE & EDITORIAL SEED
-- Prerequisite: Run `melvore_live_schema_compatibility_repair.sql`,
--               `melvore_hero_content_seed_fixed.sql`, and
--               `melvore_delivery_cache_seed_fixed.sql` first.
--
-- SAFETY GUARANTEES:
--   - Does NOT DROP or TRUNCATE any table.
--   - Compatible with MySQL 5.7+ / 8.0+ / MariaDB 10.3+ (no ROW_NUMBER() window fn).
--   - Filters `categories` by `status = 'active'` (not non-existent `is_active`).
--   - Links `app_smart_offers` strictly to live active `coupons` rows.
--   - Populates `runway_config` with restrained kinetic metadata and leaves
--     `app_elite_pack_items.image_url` NULL on product items so product images
--     never get overwritten by collection hero banners.
-- ============================================================================

SET NAMES utf8mb4;

SET @img_hero_1 = (SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1 OFFSET 0);
SET @img_hero_2 = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1 OFFSET 1), @img_hero_1);
SET @img_hero_3 = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1 OFFSET 2), @img_hero_1);
SET @img_hero_4 = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.purchase_count DESC, p.product_id DESC LIMIT 1 OFFSET 0), @img_hero_1);
SET @img_hero_5 = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.view_count DESC, p.product_id DESC LIMIT 1 OFFSET 0), @img_hero_2);
SET @img_lux_1  = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.product_id DESC LIMIT 1 OFFSET 0), @img_hero_1);
SET @img_lux_2  = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.product_id DESC LIMIT 1 OFFSET 1), @img_lux_1);
SET @img_lux_3  = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.product_id DESC LIMIT 1 OFFSET 2), @img_lux_1);
SET @img_lux_4  = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.product_id DESC LIMIT 1 OFFSET 3), @img_lux_1);

-- 1. Campaigns, Regions & Assets
INSERT INTO `app_campaigns`
  (`slug`, `title`, `subtitle`, `festival_key`, `animation_key`, `animation_duration_ms`,
   `primary_color`, `accent_color`, `background_color`, `banner_image_url`, `mobile_banner_url`,
   `cta_text`, `cta_link`, `priority`, `is_active`, `theme_config`)
VALUES
  ('winter-leather-edit', 'The Winter & Leather Edit', 'Statement jackets, structured layering, and cold-weather icons.', 'winter', 'smooth_reveal', 2400, '#09090B', '#D4AF37', '#FAF8F5', @img_hero_1, @img_hero_1, 'Explore Winter Edit', 'jackets', 100, 1, '{"mode":"light","surface_color":"#FFFFFF","accent_color":"#D4AF37","badge":"SEASONAL DROP"}'),
  ('seoul-street-dispatch', 'Seoul Street & Y2K Capsule', 'Proportion-forward outerwear and nocturnal street tailoring.', 'editorial', 'cyber_sparkle', 2200, '#FFFFFF', '#FF2EA6', '#0E0D1B', @img_hero_2, @img_hero_2, 'Enter Style Edit', 'y2k', 75, 1, '{"mode":"dark","surface_color":"#181632","accent_color":"#FF2EA6","badge":"WORLD DISPATCH"}')
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `subtitle` = VALUES(`subtitle`),
  `banner_image_url` = COALESCE(NULLIF(`banner_image_url`, ''), VALUES(`banner_image_url`)),
  `mobile_banner_url` = COALESCE(NULLIF(`mobile_banner_url`, ''), VALUES(`mobile_banner_url`)),
  `is_active` = 1;

INSERT INTO `app_campaign_regions` (`campaign_id`, `country`, `state`, `city`, `pincode`, `is_nationwide`)
SELECT c.campaign_id, 'India', NULL, NULL, NULL, 1
FROM `app_campaigns` c
WHERE c.slug = 'winter-leather-edit'
  AND NOT EXISTS (SELECT 1 FROM `app_campaign_regions` r WHERE r.campaign_id = c.campaign_id AND r.is_nationwide = 1);

-- 2. Homepage Editorial & Merchandising Sections
INSERT INTO `app_home_sections`
  (`section_key`, `section_type`, `eyebrow`, `title`, `subtitle`, `animation_key`, `theme_key`,
   `theme_config`, `cta_text`, `cta_link`, `item_limit`, `display_order`, `is_active`)
VALUES
  ('winter_collection', 'editorial_campaign', 'SEASONAL STORY', 'The Winter Collection', 'Heavyweight outer layers, brushed textures, and cold-season tailoring.', 'smooth_reveal', 'light_editorial', '{"surface":"#FFFFFF","accent":"#09090B"}', 'Shop Winter', 'jackets', 8, 10, 1),
  ('leather_edit', 'editorial_campaign', 'SIGNATURE CRAFT', 'The Leather Edit', 'Precision biker cuts, cropped silhouettes, and vintage-washed finishes.', 'smooth_reveal', 'light_editorial', '{"surface":"#FAF8F5","accent":"#B48811"}', 'Shop Leather', 'jackets', 8, 20, 1),
  ('streetwear_stories', 'product_shelf', 'URBAN ARCHIVE', 'Streetwear Stories', 'Oversized graphic tees, wide-leg cargos, and off-duty staples.', 'kinetic_slide', 'streetwear', '{"surface":"#111215","accent":"#F97316"}', 'Shop Streetwear', 'streetwear', 8, 30, 1),
  ('korean_fashion', 'editorial_campaign', 'SEOUL DISPATCH', 'Korean Fashion Edit', 'Clean lines, soft neutral layering, and effortless K-fashion proportions.', 'smooth_reveal', 'minimal_editorial', '{"surface":"#F5F6FA","accent":"#4F6D8A"}', 'Explore Seoul Edit', 'k-fashion', 8, 40, 1),
  ('weekend_wardrobe', 'product_shelf', 'OFF-DUTY', 'The Weekend Wardrobe', 'Co-ord sets, relaxed shirting, and easy silhouettes from morning to dusk.', 'smooth_reveal', 'warm_ivory', '{"surface":"#F7F4EF","accent":"#1C1917"}', 'Shop Co-ords', 'co-ords', 8, 50, 1),
  ('statement_jackets', 'product_grid', 'OUTERWEAR ICONS', 'Statement Jackets', 'Studded, cropped, and Moto silhouettes engineered to stand out.', 'smooth_reveal', 'light_editorial', '{"surface":"#FFFFFF","accent":"#09090B"}', 'View Outerwear', 'jackets', 8, 60, 1),
  ('after_dark', 'editorial_campaign', 'NIGHT OUT', 'After Dark', 'Sleek monochrome dressing and evening-ready statement pieces.', 'metallic_shine', 'dark_accent', '{"surface":"#08070B","accent":"#C02638"}', 'Shop Evening', 'goth', 8, 70, 1),
  ('trending_now', 'product_shelf', 'MOST VIEWED', 'Trending Across Melvore', 'High-heat pieces leading bags and wishlists right now.', 'smooth_reveal', 'light_editorial', '{"surface":"#FFFFFF","accent":"#3B82F6"}', 'Shop Trending', 'trending', 8, 75, 1),
  ('everyday_essentials', 'product_shelf', 'CAPSULE CORE', 'Everyday Essentials', 'Foundational tees, crisp shirting, and daily denim built to repeat.', 'smooth_reveal', 'light_editorial', '{"surface":"#FFFFFF","accent":"#09090B"}', 'Shop Essentials', 'all', 10, 80, 1),
  ('minimal_style', 'product_shelf', 'QUIET TAILORING', 'Minimal Style', 'Uncluttered palettes, tonal textures, and refined everyday cuts.', 'smooth_reveal', 'minimal_editorial', '{"surface":"#FAFAFA","accent":"#52525B"}', 'Shop Minimal', 'minimal', 8, 90, 1)
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `subtitle` = VALUES(`subtitle`),
  `display_order` = VALUES(`display_order`),
  `is_active` = 1;

-- 3. Product Placements
INSERT IGNORE INTO `app_product_placements`
  (`product_id`, `section_key`, `is_sponsored`, `badge_override`, `express_delivery`, `priority`, `display_order`, `is_active`)
SELECT p.product_id, 'featured', CASE WHEN p.is_featured = 1 THEN 1 ELSE 0 END,
       CASE WHEN p.is_featured = 1 THEN 'LUXE PICK' ELSE NULL END, 1,
       CASE WHEN p.is_featured = 1 THEN 100 ELSE 50 END, p.product_id, 1
FROM `products` p
WHERE p.is_active = 1
ORDER BY p.is_featured DESC, p.purchase_count DESC, p.product_id DESC
LIMIT 12;

-- 4. Brand Profiles & Origins
INSERT IGNORE INTO `app_brand_profiles`
  (`brand_id`, `tagline`, `story`, `editorial_cover_url`, `mobile_cover_url`, `origin_country`, `theme_config`,
   `is_featured`, `is_sponsored`, `is_new`, `trending_score`, `display_order`)
SELECT b.brand_id,
       CONCAT('Curated ', b.name, ' seasonal edit & signature silhouettes.'),
       COALESCE(NULLIF(b.description, ''), CONCAT(b.name, ' crafts precision-cut contemporary garments.')),
       COALESCE((SELECT p.img_src FROM `products` p WHERE p.brand_id = b.brand_id AND p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1), @img_hero_1),
       COALESCE((SELECT p.img_src FROM `products` p WHERE p.brand_id = b.brand_id AND p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1), @img_hero_1),
       CASE
         WHEN LOWER(CONCAT(b.name, ' ', COALESCE(b.description, ''))) LIKE '%seoul%' OR LOWER(CONCAT(b.name, ' ', COALESCE(b.description, ''))) LIKE '%korea%' THEN 'South Korea'
         WHEN LOWER(CONCAT(b.name, ' ', COALESCE(b.description, ''))) LIKE '%tokyo%' OR LOWER(CONCAT(b.name, ' ', COALESCE(b.description, ''))) LIKE '%japan%' THEN 'Japan'
         WHEN LOWER(CONCAT(b.name, ' ', COALESCE(b.description, ''))) LIKE '%paris%' OR LOWER(CONCAT(b.name, ' ', COALESCE(b.description, ''))) LIKE '%france%' THEN 'France'
         WHEN LOWER(CONCAT(b.name, ' ', COALESCE(b.description, ''))) LIKE '%milan%' OR LOWER(CONCAT(b.name, ' ', COALESCE(b.description, ''))) LIKE '%italy%' THEN 'Italy'
         WHEN LOWER(CONCAT(b.name, ' ', COALESCE(b.description, ''))) LIKE '%london%' THEN 'United Kingdom'
         ELSE 'India'
       END,
       '{"mode":"light","accent_color":"#09090B"}',
       1, 0, CASE WHEN b.brand_id % 3 = 0 THEN 1 ELSE 0 END, 85, b.brand_id
FROM `brands` b
WHERE b.status = 'active';

INSERT IGNORE INTO `app_brand_origins`
  (`brand_id`, `city`, `country`, `country_code`, `latitude`, `longitude`, `world_slug`, `origin_story`, `is_verified`, `display_order`, `is_active`)
SELECT b.brand_id,
       CASE
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%korea%' THEN 'Seoul'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%japan%' THEN 'Tokyo'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%france%' THEN 'Paris'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%italy%' THEN 'Milan'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%uk%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united kingdom%' THEN 'London'
         ELSE 'New Delhi'
       END,
       COALESCE(NULLIF(TRIM(bp.origin_country), ''), 'India'),
       CASE
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%korea%' THEN 'KR'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%japan%' THEN 'JP'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%france%' THEN 'FR'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%italy%' THEN 'IT'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%uk%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united kingdom%' THEN 'GB'
         ELSE 'IN'
       END,
       CASE
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%korea%' THEN 37.566535
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%japan%' THEN 35.676192
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%france%' THEN 48.856614
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%italy%' THEN 45.464204
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%uk%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united kingdom%' THEN 51.507351
         ELSE 28.613939
       END,
       CASE
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%korea%' THEN 126.977969
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%japan%' THEN 139.650311
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%france%' THEN 2.352222
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%italy%' THEN 9.189982
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%uk%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united kingdom%' THEN -0.127758
         ELSE 77.209021
       END,
       CASE
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%korea%' THEN 'k-fashion'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%japan%' THEN 'streetwear'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%france%' THEN 'coquette'
         WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%uk%' THEN 'goth'
         ELSE 'y2k'
       END,
       COALESCE(NULLIF(bp.story, ''), NULLIF(b.description, ''), CONCAT('Verified studio label originating in ', COALESCE(NULLIF(bp.origin_country, ''), 'India'), '.')),
       1, COALESCE(bp.display_order, b.brand_id), 1
FROM `brands` b
LEFT JOIN `app_brand_profiles` bp ON bp.brand_id = b.brand_id
WHERE b.status = 'active';

-- 5. Style Worlds (`app_style_worlds`) — Upsert by slug without wiping admin custom media/video URLs
INSERT INTO `app_style_worlds` (
  `slug`, `name`, `tagline`, `description`, `theme_key`, `animation_key`, `button_style`, `font_style`,
  `ambient_preset`, `audio_track_key`, `audio_loop`, `audio_volume`, `primary_color`, `accent_color`, `background_color`,
  `cover_image_url`, `mobile_image_url`, `promo_video_poster_url`, `display_order`, `version_tag`, `is_active`, `theme_config`, `runway_config`
) VALUES
  ('y2k', 'Y2K Cyber', 'LIQUID CHROME & LIGHT', 'Metallic low-rise silhouettes and nostalgic cyber tailoring.', 'y2k_cyber', 'cyber_sparkle', 'y2k_pixel_chrome', 'pixel', 'y2k_pixels', 'y2k_synth', 1, 0.45, '#FFFFFF', '#FF2EA6', '#0E0D1B', @img_hero_1, @img_hero_1, @img_hero_1, 1, '3.0', 1, '{"mode":"dark","surface_color":"#181632","accent_color":"#FF2EA6","secondary_accent":"#38BDF8","text_primary":"#FFFFFF","text_secondary":"#D8D6F2","border_color":"#47427A","button_style":"y2k_pixel_chrome"}', '{"enabled":true,"title":"Y2K Runway Frame","preset":"kinetic_frame","speed":1.0,"accent_hex":"#22D3EE","secondary_hex":"#F472B6"}'),
  ('goth', 'Goth & Nocturne', 'OBSIDIAN TAILORING & SHADOW', 'Sculpted monochrome layers, silver hardware, and midnight poise.', 'goth_nocturne', 'bat_swarm_noir', 'goth_fang', 'gothic', 'goth_nocturne', 'goth_drone', 1, 0.42, '#F4F4F5', '#C02638', '#08070B', @img_hero_2, @img_hero_2, @img_hero_2, 2, '3.0', 1, '{"mode":"dark","surface_color":"#121018","accent_color":"#C02638","text_primary":"#F4F4F5","text_secondary":"#C4C2CF","border_color":"#342E42","button_style":"goth_fang"}', NULL),
  ('coquette', 'Coquette Reverie', 'SILK RIBBONS & DRAPING', 'Ivory lace, soft rose drapery, and romantic vintage tailoring.', 'coquette_romance', 'petal_drift', 'coquette_ribbon', 'editorial_serif', 'coquette_petals', 'coquette_musicbox', 1, 0.40, '#C04870', '#E986A5', '#FFF5F8', @img_hero_4, @img_hero_4, @img_hero_4, 3, '3.0', 1, '{"mode":"light","surface_color":"#FFFFFF","text_primary":"#3B1824","text_secondary":"#7D4E5E","border_color":"#F5CCD9","button_style":"coquette_ribbon"}', NULL),
  ('streetwear', 'Streetwear Signal', 'HEAVYWEIGHT UTILITY & VOLUME', 'Oversized proportions, boxy silhouettes, and city-ready hardware.', 'street_utility', 'kinetic_slide', 'streetwear_utility', 'futuristic', 'street_grid', 'street_pulse', 1, 0.45, '#FFFFFF', '#F97316', '#111215', @img_hero_5, @img_hero_5, @img_hero_5, 4, '3.0', 1, '{"mode":"dark","surface_color":"#191B20","text_primary":"#FFFFFF","text_secondary":"#D1D5DB","border_color":"#323640","button_style":"streetwear_utility"}', NULL),
  ('k-fashion', 'K-Fashion Atelier', 'SEOUL PROPORTION & CLARITY', 'Architectural trench coats, crisp shirting, and Seoul minimalism.', 'kfashion_atelier', 'smooth_reveal', 'kfashion_minimal', 'minimal_grotesk', 'kfashion_minimal', 'kfashion_chime', 1, 0.38, '#141519', '#4F6D8A', '#F5F6FA', @img_hero_3, @img_hero_3, @img_hero_3, 5, '3.0', 1, '{"mode":"light","surface_color":"#FFFFFF","text_primary":"#141519","text_secondary":"#525866","border_color":"#D6DCE8","button_style":"kfashion_minimal"}', NULL),
  ('quiet-luxury', 'Quiet Luxury', 'UNBRANDED FORM & TACTILE WEAVES', 'Restrained camel, ivory, and charcoal tailoring.', 'quiet_luxury', 'smooth_reveal', 'editorial_pill', 'editorial_serif', 'none', 'luxe_ambient', 1, 0.35, '#1C1917', '#B48811', '#F7F4EF', @img_lux_1, @img_lux_1, @img_lux_1, 6, '3.0', 1, '{"mode":"light","surface_color":"#FFFFFF","text_primary":"#1C1917","text_secondary":"#57534E","border_color":"#E7E5E4","button_style":"editorial_pill"}', NULL)
ON DUPLICATE KEY UPDATE
  `tagline` = COALESCE(NULLIF(`tagline`, ''), VALUES(`tagline`)),
  `button_style` = COALESCE(NULLIF(`button_style`, ''), VALUES(`button_style`)),
  `cover_image_url` = COALESCE(NULLIF(`cover_image_url`, ''), VALUES(`cover_image_url`)),
  `mobile_image_url` = COALESCE(NULLIF(`mobile_image_url`, ''), VALUES(`mobile_image_url`)),
  `promo_video_poster_url` = COALESCE(NULLIF(`promo_video_poster_url`, ''), VALUES(`promo_video_poster_url`)),
  `runway_config` = COALESCE(`runway_config`, VALUES(`runway_config`)),
  `is_active` = 1;

INSERT IGNORE INTO `app_style_world_products` (`style_world_id`, `product_id`, `display_order`)
SELECT sw.style_world_id, p.product_id, p.product_id
FROM `app_style_worlds` sw
JOIN `products` p ON p.is_active = 1
WHERE (
  (sw.slug = 'y2k' AND (LOWER(p.name) LIKE '%crop%' OR LOWER(p.name) LIKE '%denim%' OR LOWER(p.name) LIKE '%zip%' OR p.is_featured = 1))
  OR (sw.slug = 'goth' AND (LOWER(p.name) LIKE '%black%' OR LOWER(p.name) LIKE '%leather%' OR LOWER(p.name) LIKE '%jacket%' OR LOWER(p.name) LIKE '%biker%'))
  OR (sw.slug = 'k-fashion' AND (LOWER(p.name) LIKE '%blazer%' OR LOWER(p.name) LIKE '%shirt%' OR LOWER(p.name) LIKE '%coat%' OR p.is_featured = 1))
  OR (sw.slug = 'coquette' AND (LOWER(p.name) LIKE '%dress%' OR LOWER(p.name) LIKE '%bow%' OR LOWER(p.name) LIKE '%knit%' OR LOWER(p.name) LIKE '%top%'))
  OR (sw.slug = 'streetwear' AND (LOWER(p.name) LIKE '%hoodie%' OR LOWER(p.name) LIKE '%cargo%' OR LOWER(p.name) LIKE '%oversized%' OR LOWER(p.name) LIKE '%tee%' OR LOWER(p.name) LIKE '%bomber%'))
  OR (sw.slug = 'quiet-luxury')
);

INSERT IGNORE INTO `app_world_brands` (`style_world_id`, `brand_id`, `display_order`)
SELECT DISTINCT swp.style_world_id, p.brand_id, p.brand_id
FROM `app_style_world_products` swp
INNER JOIN `products` p ON p.product_id = swp.product_id
INNER JOIN `brands` b ON b.brand_id = p.brand_id AND b.status = 'active'
WHERE p.brand_id IS NOT NULL;

-- 6. Category Editorial Showcase Config (MySQL 5.7/MariaDB compatible — no ROW_NUMBER(), uses categories.status = 'active')
SET @cat_rn := 0;
INSERT INTO `app_category_editorial_configs`
  (`category_id`, `motif_key`, `editorial_badge`, `editorial_subtitle`, `accent_hex`, `bg_hex`, `animation_key`, `tile_span`, `display_order`, `is_active`)
SELECT
  ranked.`category_id`,
  CASE
    WHEN ranked.rn % 5 = 1 THEN 'editorial_arch'
    WHEN ranked.rn % 5 = 2 THEN 'chrome_spark'
    WHEN ranked.rn % 5 = 3 THEN 'ribbon_crest'
    WHEN ranked.rn % 5 = 4 THEN 'noir_stencil'
    ELSE 'minimal_grid'
  END,
  CASE WHEN ranked.rn = 1 THEN 'CURATOR INDEX' ELSE 'ARCHIVE' END,
  ranked.`editorial_subtitle`,
  CASE
    WHEN ranked.rn % 4 = 1 THEN '#C2410C'
    WHEN ranked.rn % 4 = 2 THEN '#1D4ED8'
    WHEN ranked.rn % 4 = 3 THEN '#BE185D'
    ELSE '#141414'
  END,
  CASE
    WHEN ranked.rn % 4 = 1 THEN '#FAF5F0'
    WHEN ranked.rn % 4 = 2 THEN '#F1F5FA'
    WHEN ranked.rn % 4 = 3 THEN '#FDF2F6'
    ELSE '#F5F4F0'
  END,
  'zoom_reveal',
  CASE WHEN ranked.rn = 1 THEN 'feature' WHEN ranked.rn % 4 = 0 THEN 'wide' ELSE 'regular' END,
  ranked.rn,
  1
FROM (
  SELECT c.`category_id`,
         COALESCE(NULLIF(TRIM(c.`description`), ''), CONCAT('Sculpted essentials in ', c.`name`)) AS `editorial_subtitle`,
         (@cat_rn := @cat_rn + 1) AS rn
  FROM `categories` c
  WHERE c.`status` = 'active'
  ORDER BY COALESCE(c.`display_order`, 0) ASC, c.`category_id` ASC
) ranked
ON DUPLICATE KEY UPDATE
  `editorial_subtitle` = VALUES(`editorial_subtitle`),
  `is_active` = 1;

-- 7. Luxe Collections (`app_elite_packs` & `app_elite_pack_items`)
INSERT INTO `app_elite_packs`
  (`slug`, `section_key`, `brand_name`, `eyebrow`, `title`, `subtitle`, `story`, `cta_text`, `cta_link`,
   `hero_image_url`, `mobile_image_url`, `animation_key`, `accent_color`, `background_color`,
   `is_hero`, `is_sponsored`, `display_order`, `is_active`, `status`, `layout_type`, `theme_config`)
VALUES
  ('the-luxe-edit', 'the_luxe_edit', 'MELVORE LUXE', 'PRIVATE SHOWROOM · AUTUMN / WINTER', 'THE LUXE EDIT', 'Architectural tailoring, collector leatherwork, and statement silhouettes in obsidian & gold.', 'Drawn from the highest tier of the Melvore vault.', 'Enter Collection', 'elite/pack/the-luxe-edit', @img_lux_1, @img_lux_1, 'metallic_shine', '#D4AF37', '#000000', 1, 0, 1, 1, 'published', 'hero_spotlight', '{"layout":"hero_cinematic","mode":"dark"}'),
  ('leather-atelier', 'leather_atelier', 'ATELIER NOIR', 'CRAFT & HARDWARE', 'Leather Atelier', 'Supple hides, asymmetric zips, and hand-finished outerwear icons.', 'Precision outerwear with enduring hardware.', 'Explore Atelier', 'elite/pack/leather-atelier', @img_lux_2, @img_lux_2, 'metallic_shine', '#D4AF37', '#050507', 0, 0, 10, 1, 'published', 'editorial_grid', '{"layout":"editorial_split","mode":"dark"}'),
  ('after-dark-archive', 'after_dark_archive', 'MELVORE NOIR', 'EVENING EDIT', 'The After Dark Archive', 'Nocturnal drapery, sharp evening cuts, and collector pieces dressed for midnight.', 'Monochrome silhouettes with brushed champagne gold accents.', 'Enter Archive', 'elite/pack/after-dark-archive', @img_lux_4, @img_lux_4, 'smooth_reveal', '#D4AF37', '#000000', 0, 0, 20, 1, 'published', 'editorial_grid', '{"layout":"portrait_diptych","mode":"dark"}')
ON DUPLICATE KEY UPDATE
  `status` = COALESCE(NULLIF(`status`, ''), VALUES(`status`)),
  `layout_type` = COALESCE(NULLIF(`layout_type`, ''), VALUES(`layout_type`)),
  `hero_image_url` = COALESCE(NULLIF(`hero_image_url`, ''), VALUES(`hero_image_url`)),
  `mobile_image_url` = COALESCE(NULLIF(`mobile_image_url`, ''), VALUES(`mobile_image_url`));

-- Ensure product items inside Luxe packs do NOT override product photography with pack banners
UPDATE `app_elite_pack_items`
SET `image_url` = NULL
WHERE `item_type` = 'product' AND `product_id` IS NOT NULL;

INSERT IGNORE INTO `app_elite_pack_items`
  (`pack_id`, `item_type`, `product_id`, `brand_id`, `title`, `subtitle`, `badge_text`, `image_url`, `is_sponsored`, `display_order`, `is_active`)
SELECT ep.pack_id, 'product', p.product_id, p.brand_id, p.name, COALESCE(b.name, 'Melvore Luxe'), 'LUXE VAULT', NULL, CASE WHEN p.is_featured = 1 THEN 1 ELSE 0 END, p.product_id, 1
FROM `app_elite_packs` ep
JOIN (
  SELECT product_id, brand_id, name, is_featured
  FROM `products`
  WHERE is_active = 1
  ORDER BY COALESCE(NULLIF(sale_price, 0), regular_price) DESC, is_featured DESC, product_id DESC
  LIMIT 10
) p
LEFT JOIN `brands` b ON b.brand_id = p.brand_id
WHERE NOT EXISTS (
  SELECT 1 FROM `app_elite_pack_items` existing
  WHERE existing.pack_id = ep.pack_id AND existing.product_id = p.product_id
);

-- 8. Search Synonyms & Smart Offers (strictly from live active coupons)
INSERT IGNORE INTO `app_search_synonyms` (`term`, `synonyms`, `expanded_terms`, `is_active`) VALUES
  ('coords', 'co-ord,co-ords,coord set,matching set,two piece', 'co-ord,co-ords,coord set,matching set,two piece', 1),
  ('biker', 'leather jacket,moto jacket,studded jacket,outerwear', 'leather jacket,moto jacket,studded jacket,outerwear', 1),
  ('korean', 'k-fashion,seoul,oversized,cropped jacket,minimal', 'k-fashion,seoul,oversized,cropped jacket,minimal', 1),
  ('y2k', 'crop top,baby tee,cyber,chrome,denim,low rise', 'crop top,baby tee,cyber,chrome,denim,low rise', 1),
  ('goth', 'black,leather,studded,nocturnal,corset,boots', 'black,leather,studded,nocturnal,corset,boots', 1);

INSERT IGNORE INTO `app_smart_offers` (
  `offer_key`, `title`, `subtitle`, `badge_text`, `coupon_code`, `world_slug`, `occasion_slug`,
  `min_cart_value`, `max_impressions_per_session`, `cooldown_hours`, `priority`, `accent_hex`, `cta_text`, `is_active`
)
SELECT
  CONCAT('coupon_', LOWER(c.code)),
  COALESCE(NULLIF(c.description, ''), CONCAT('Save with code ', c.code)),
  CASE WHEN c.min_order_amount > 0 THEN CONCAT('Valid on orders above ₹', ROUND(c.min_order_amount, 0)) ELSE 'Valid across eligible studio edits' END,
  CASE
    WHEN c.discount_type = 'percentage' THEN CONCAT(ROUND(c.discount_value, 0), '% OFF')
    WHEN c.discount_type = 'free_shipping' THEN 'FREE SHIP'
    ELSE CONCAT('₹', ROUND(c.discount_value, 0), ' OFF')
  END,
  c.code, NULL, NULL, COALESCE(c.min_order_amount, 0.00), 1, 24, 25, '#3B82F6', 'Use Code', 1
FROM `coupons` c
WHERE c.is_active = 1
  AND (c.valid_from IS NULL OR c.valid_from <= NOW())
  AND (c.valid_to IS NULL OR c.valid_to >= NOW())
  AND (c.usage_limit IS NULL OR c.usage_limit = 0 OR c.usage_count < c.usage_limit)
ORDER BY c.coupon_id ASC
LIMIT 4;
