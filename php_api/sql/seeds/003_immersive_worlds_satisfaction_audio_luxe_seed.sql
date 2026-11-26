-- ============================================================================
-- MELVORE — SEED 003:
-- World Button Styles, Y2K Pixel Runway Config, Occasion Action Styles,
-- Homepage Category Editorial Configs, and Luxe Collection Integrity Polish
-- Non-destructive & idempotent (`ON DUPLICATE KEY UPDATE`).
-- Run AFTER 003_immersive_worlds_satisfaction_audio_luxe.sql
-- ============================================================================

SET NAMES utf8mb4;

-- 1. Enrich Fashion Worlds with Button Styles, Font Keys, Audio Defaults, and Y2K Runway Config
UPDATE `app_style_worlds`
SET
    `font_style` = 'pixel',
    `button_style` = 'y2k_pixel_chrome',
    `audio_loop` = 1,
    `audio_volume` = 0.45,
    `runway_config` = JSON_OBJECT(
        'enabled', true,
        'title', 'CYBER PIXEL RUNWAY 2000',
        'subtitle', 'Chrome silhouettes in live digital motion',
        'preset', 'pixel_cyber_walk',
        'speed', 1.0,
        'position', 'after_hero',
        'collection_slug', 'y2k-chrome',
        'background_hex', '#120826',
        'accent_hex', '#22D3EE',
        'secondary_hex', '#F472B6',
        'artwork_mode', 'pixel_models_v1',
        'models', JSON_ARRAY(
            JSON_OBJECT('id', 'cyber_nova', 'name', 'NOVA-01', 'outfit', 'Liquid Chrome Corset & Cargo', 'primary_hex', '#F472B6', 'secondary_hex', '#22D3EE', 'hair_hex', '#E2E8F0'),
            JSON_OBJECT('id', 'pixel_kiki', 'name', 'KIKI-2K', 'outfit', 'Metallic Baby Tee & Pleat Mini', 'primary_hex', '#38BDF8', 'secondary_hex', '#C084FC', 'hair_hex', '#F472B6'),
            JSON_OBJECT('id', 'chrome_jin', 'name', 'JIN-X', 'outfit', 'Cyber Visor & Silver Bomber', 'primary_hex', '#E2E8F0', 'secondary_hex', '#F472B6', 'hair_hex', '#22D3EE'),
            JSON_OBJECT('id', 'holo_mira', 'name', 'MIRA-HOLO', 'outfit', 'Iridescent Mesh & Platform Boots', 'primary_hex', '#C084FC', 'secondary_hex', '#22D3EE', 'hair_hex', '#0F172A')
        )
    )
WHERE `slug` LIKE '%y2k%';

UPDATE `app_style_worlds`
SET
    `font_style` = 'gothic',
    `button_style` = 'goth_fang',
    `animation_key` = 'bat_swarm_noir',
    `audio_loop` = 1,
    `audio_volume` = 0.42
WHERE `slug` LIKE '%goth%';

UPDATE `app_style_worlds`
SET
    `font_style` = 'editorial_serif',
    `button_style` = 'coquette_ribbon',
    `primary_color` = '#C04870',
    `accent_color` = '#E986A5',
    `background_color` = '#FFF5F8',
    `audio_loop` = 1,
    `audio_volume` = 0.40
WHERE `slug` LIKE '%coquette%';

UPDATE `app_style_worlds`
SET
    `font_style` = 'futuristic',
    `button_style` = 'streetwear_utility',
    `audio_loop` = 1,
    `audio_volume` = 0.45
WHERE `slug` LIKE '%street%';

UPDATE `app_style_worlds`
SET
    `font_style` = 'minimal_grotesk',
    `button_style` = 'kfashion_minimal',
    `audio_loop` = 1,
    `audio_volume` = 0.38
WHERE `slug` LIKE '%k-fashion%' OR `slug` LIKE '%korean%';

-- Set valid promo_video_poster_url fallback from first active product image in each world when NULL
UPDATE `app_style_worlds` w
LEFT JOIN (
    SELECT swp.style_world_id,
           SUBSTRING_INDEX(MIN(p.img_src), '|', 1) AS first_img
    FROM `app_style_world_products` swp
    INNER JOIN `products` p ON p.product_id = swp.product_id AND p.is_active = 1 AND p.img_src IS NOT NULL AND p.img_src <> ''
    GROUP BY swp.style_world_id
) pi ON pi.style_world_id = w.style_world_id
SET w.promo_video_poster_url = COALESCE(NULLIF(w.promo_video_poster_url, ''), w.mobile_image_url, w.cover_image_url, pi.first_img)
WHERE w.promo_video_poster_url IS NULL OR w.promo_video_poster_url = '';

-- 2. Enrich Occasions with Interactive Button Style Tokens
UPDATE `app_occasions`
SET `button_style` = 'halloween_pumpkin'
WHERE `slug` LIKE '%halloween%' OR `motif_key` LIKE '%pumpkin%' OR `motif_key` LIKE '%bat%';

UPDATE `app_occasions`
SET `button_style` = 'diwali_gold'
WHERE `slug` LIKE '%diwali%' OR `motif_key` LIKE '%diya%';

-- 3. Populate Homepage Category Editorial Showcase Configs from Active Catalog Categories
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
    END AS `motif_key`,
    CASE
        WHEN ranked.rn = 1 THEN 'CURATOR INDEX'
        WHEN ranked.rn = 2 THEN 'RUNWAY SILHOUETTES'
        ELSE 'SEASONAL ARCHIVE'
    END AS `editorial_badge`,
    ranked.`editorial_subtitle`,
    CASE
        WHEN ranked.rn % 4 = 1 THEN '#C2410C'
        WHEN ranked.rn % 4 = 2 THEN '#1D4ED8'
        WHEN ranked.rn % 4 = 3 THEN '#BE185D'
        ELSE '#141414'
    END AS `accent_hex`,
    CASE
        WHEN ranked.rn % 4 = 1 THEN '#FAF5F0'
        WHEN ranked.rn % 4 = 2 THEN '#F1F5FA'
        WHEN ranked.rn % 4 = 3 THEN '#FDF2F6'
        ELSE '#F5F4F0'
    END AS `bg_hex`,
    'zoom_reveal' AS `animation_key`,
    CASE
        WHEN ranked.rn = 1 THEN 'feature'
        WHEN ranked.rn % 4 = 0 THEN 'wide'
        ELSE 'regular'
    END AS `tile_span`,
    ranked.rn AS `display_order`,
    1 AS `is_active`
FROM (
    SELECT
        c.`category_id`,
        COALESCE(NULLIF(TRIM(c.`description`), ''), CONCAT('Sculpted essentials in ', c.`name`)) AS `editorial_subtitle`,
        (@cat_rn := @cat_rn + 1) AS rn
    FROM `categories` c
    WHERE c.`status` = 'active'
    ORDER BY COALESCE(c.`display_order`, 0) ASC, c.`category_id` ASC
) ranked
ON DUPLICATE KEY UPDATE
    `editorial_subtitle` = VALUES(`editorial_subtitle`),
    `is_active` = VALUES(`is_active`);

-- 4. Ensure Existing Active Luxe Collections Have Published Status and Default Layout
UPDATE `app_elite_packs`
SET
    `status` = CASE WHEN `is_active` = 1 THEN 'published' ELSE 'draft' END,
    `layout_type` = CASE WHEN `is_hero` = 1 THEN 'hero_spotlight' ELSE 'editorial_grid' END
WHERE `status` IS NULL OR `status` = '';

-- Clear item-level image_url on product items in app_elite_pack_items so every product card renders its own verified catalog `products.img_src`
UPDATE `app_elite_pack_items`
SET `image_url` = NULL
WHERE `item_type` = 'product' AND `product_id` IS NOT NULL;
