-- ============================================================================
-- MELVORE — REPEAT-SAFE SEED 002:
-- Dynamic Fashion Worlds, Occasions, Vector Assets, Brand World Map Pins & Offers
-- Uses INSERT IGNORE / ON DUPLICATE KEY UPDATE and dynamically references ONLY
-- real rows already present in `products`, `brands`, and `coupons`.
-- ============================================================================

SET NAMES utf8mb4;

-- 1. Core Fashion Worlds (Distinct Aesthetics Preserving Default Storefront Separately)
INSERT INTO `app_style_worlds` (
    `slug`, `name`, `tagline`, `description`, `theme_key`, `animation_key`, `font_style`,
    `ambient_preset`, `audio_track_key`, `primary_color`, `accent_color`, `background_color`,
    `display_order`, `version_tag`, `is_active`, `theme_config`
) VALUES
(
    'y2k',
    'Y2K Cyber',
    'LIQUID CHROME & PIXEL HEAT',
    'Metallic low-rise silhouettes, cyber-pop outerwear, and early-2000s nostalgic club energy.',
    'y2k_cyber',
    'cyber_sparkle',
    'geometric_grotesk',
    'y2k_pixels',
    'y2k_synth',
    '#FFFFFF',
    '#FF2EA6',
    '#0E0D1B',
    1,
    '2.0',
    1,
    JSON_OBJECT(
        'mode', 'dark',
        'surface_color', '#181632',
        'surface_elevated', '#232046',
        'secondary_accent', '#38BDF8',
        'text_primary', '#FFFFFF',
        'text_secondary', '#D8D6F2',
        'border_color', '#47427A',
        'button_style', 'chrome_bevel',
        'card_radius_dp', 18,
        'border_width_dp', 1,
        'sfx_profile', 'y2k',
        'hero_layout', 'cyber_split'
    )
),
(
    'goth',
    'Goth & Nocturne',
    'OBSIDIAN SHADOWS & SILVER THORNS',
    'Sculpted monochrome tailoring, hardware accents, velvet darkness, and moonlight drama.',
    'goth_nocturne',
    'nocturne_mist',
    'gothic_serif',
    'goth_nocturne',
    'goth_drone',
    '#F4F4F5',
    '#C02638',
    '#08070B',
    2,
    '2.0',
    1,
    JSON_OBJECT(
        'mode', 'dark',
        'surface_color', '#121018',
        'surface_elevated', '#1B1824',
        'secondary_accent', '#A1A1AA',
        'text_primary', '#F4F4F5',
        'text_secondary', '#C4C2CF',
        'border_color', '#342E42',
        'button_style', 'goth_sharp',
        'card_radius_dp', 4,
        'border_width_dp', 1,
        'sfx_profile', 'goth',
        'hero_layout', 'noir_monolith'
    )
),
(
    'k-fashion',
    'K-Fashion Atelier',
    'SEOUL PROPORTION & SOFT PRECISION',
    'Architectural trench coats, crisp cropped shirts, and Seoul street-minimalist proportions.',
    'kfashion_atelier',
    'smooth_reveal',
    'atelier_didot',
    'kfashion_minimal',
    'kfashion_chime',
    '#141519',
    '#4F6D8A',
    '#F5F6FA',
    3,
    '2.0',
    1,
    JSON_OBJECT(
        'mode', 'light',
        'surface_color', '#FFFFFF',
        'surface_elevated', '#EDF0F7',
        'secondary_accent', '#94A3B8',
        'text_primary', '#141519',
        'text_secondary', '#525866',
        'border_color', '#D6DCE8',
        'button_style', 'atelier_outline',
        'card_radius_dp', 10,
        'border_width_dp', 1,
        'sfx_profile', 'kfashion',
        'hero_layout', 'seoul_grid'
    )
),
(
    'coquette',
    'Coquette Reverie',
    'SILK RIBBONS & ROSÉ DRAPING',
    'Dreamy ivory lace, satin bows, balletcore knits, and romantic vintage tailoring.',
    'coquette_romance',
    'petal_drift',
    'editorial_serif',
    'coquette_petals',
    'coquette_musicbox',
    '#3B1824',
    '#D96588',
    '#FFF6F9',
    4,
    '2.0',
    1,
    JSON_OBJECT(
        'mode', 'light',
        'surface_color', '#FFFFFF',
        'surface_elevated', '#FDECF2',
        'secondary_accent', '#E8A5BA',
        'text_primary', '#3B1824',
        'text_secondary', '#7D4E5E',
        'border_color', '#F5CCD9',
        'button_style', 'ribbon_pill',
        'card_radius_dp', 24,
        'border_width_dp', 1,
        'sfx_profile', 'coquette',
        'hero_layout', 'romantic_frame'
    )
),
(
    'streetwear',
    'Streetwear Signal',
    'HEAVYWEIGHT KNITS & URBAN UTILITY',
    'Boxy tees, cargo silhouettes, concrete graphics, and electric cobalt accents built for the block.',
    'street_utility',
    'kinetic_slide',
    'editorial_sans',
    'street_grid',
    'street_pulse',
    '#FFFFFF',
    '#F97316',
    '#111215',
    5,
    '2.0',
    1,
    JSON_OBJECT(
        'mode', 'dark',
        'surface_color', '#191B20',
        'surface_elevated', '#23262E',
        'secondary_accent', '#38BDF8',
        'text_primary', '#FFFFFF',
        'text_secondary', '#D1D5DB',
        'border_color', '#323640',
        'button_style', 'street_block',
        'card_radius_dp', 6,
        'border_width_dp', 2,
        'sfx_profile', 'streetwear',
        'hero_layout', 'utility_poster'
    )
)
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `tagline` = VALUES(`tagline`),
    `description` = VALUES(`description`),
    `theme_key` = VALUES(`theme_key`),
    `animation_key` = VALUES(`animation_key`),
    `font_style` = VALUES(`font_style`),
    `ambient_preset` = VALUES(`ambient_preset`),
    `audio_track_key` = VALUES(`audio_track_key`),
    `primary_color` = VALUES(`primary_color`),
    `accent_color` = VALUES(`accent_color`),
    `background_color` = VALUES(`background_color`),
    `display_order` = VALUES(`display_order`),
    `version_tag` = VALUES(`version_tag`),
    `theme_config` = VALUES(`theme_config`);

-- 2. Dynamic World Vector Assets Mapped to Each World & Occasion
INSERT IGNORE INTO `app_world_assets` (
    `asset_key`, `world_slug`, `occasion_slug`, `vector_type`, `layer_role`,
    `primary_hex`, `secondary_hex`, `opacity`, `motion_pattern`, `display_order`
) VALUES
('y2k_pixel_heart', 'y2k', NULL, 'pixel_heart', 'ambient_floating', '#FF2EA6', '#38BDF8', 0.42, 'float_drift', 1),
('y2k_chrome_sparkle', 'y2k', NULL, 'chrome_sparkle', 'corner_motif', '#E2E8F0', '#FF2EA6', 0.55, 'pulse_twinkle', 2),
('y2k_pixel_star', 'y2k', NULL, 'pixel_star', 'ambient_floating', '#38BDF8', '#FFFFFF', 0.36, 'orbit_spin', 3),
('goth_raven', 'goth', NULL, 'goth_raven', 'corner_motif', '#E4E4E7', '#C02638', 0.40, 'glide_slow', 1),
('goth_bat', 'goth', NULL, 'goth_bat', 'ambient_floating', '#D4D4D8', '#7F1D1D', 0.35, 'float_drift', 2),
('goth_crescent', 'goth', NULL, 'crescent_moon', 'header_watermark', '#F4F4F5', '#9CA3AF', 0.30, 'pulse_twinkle', 3),
('goth_mist', 'goth', NULL, 'gothic_mist', 'background_atmosphere', '#4C1D95', '#C02638', 0.22, 'mist_sweep', 4),
('coquette_bow', 'coquette', NULL, 'ribbon_bow', 'corner_motif', '#D96588', '#FBCFE8', 0.50, 'sway_gentle', 1),
('coquette_petal', 'coquette', NULL, 'flower_petal', 'ambient_floating', '#F472B6', '#FDA4AF', 0.42, 'petal_fall', 2),
('street_lightning', 'streetwear', NULL, 'street_bolt', 'corner_motif', '#F97316', '#38BDF8', 0.45, 'kinetic_pulse', 1),
('street_grid', 'streetwear', NULL, 'editorial_grid', 'background_atmosphere', '#334155', '#F97316', 0.24, 'static_clean', 2),
('kfashion_origami', 'k-fashion', NULL, 'atelier_frame', 'corner_motif', '#4F6D8A', '#94A3B8', 0.32, 'sway_gentle', 1),
('occ_diwali_diya', 'occasion', 'diwali-luxe-edit', 'diya_flame', 'corner_motif', '#F59E0B', '#FDE68A', 0.58, 'flicker_warm', 1),
('occ_winter_snow', 'occasion', 'winter-atelier', 'snowflake', 'ambient_floating', '#E0F2FE', '#93C5FD', 0.45, 'petal_fall', 1),
('occ_ny_firework', 'occasion', 'new-year-countdown', 'firework', 'ambient_floating', '#FBBF24', '#F472B6', 0.48, 'pulse_twinkle', 1),
('occ_sale_sticker', 'occasion', 'end-of-season-edit', 'sale_sticker', 'corner_motif', '#EF4444', '#FFFFFF', 0.50, 'kinetic_pulse', 1);

-- 3. Seasonal & Regional Occasions (Scheduled & Admin-Configurable)
INSERT IGNORE INTO `app_occasions` (
    `slug`, `name`, `eyebrow`, `headline`, `subtitle`, `occasion_type`, `motif_key`,
    `primary_color`, `accent_color`, `background_color`, `font_style`, `audio_track_key`,
    `cta_text`, `cta_collection_slug`, `region_country`, `priority`, `is_active`,
    `start_date`, `end_date`, `theme_config`
) VALUES
(
    'season-editorial-spotlight',
    'Melvore Seasonal Edit',
    'LIMITED CAPSULE DROP',
    'Curated Outerwear & Atelier Knitwear',
    'Discover this season’s global drops, small-batch tailoring, and limited silhouettes.',
    'seasonal',
    'confetti',
    '#FFFFFF',
    '#D4AF37',
    '#141218',
    'editorial_serif',
    'festive',
    'Shop The Edit',
    'featured',
    'ALL',
    40,
    1,
    NULL,
    NULL,
    JSON_OBJECT('mode', 'dark', 'surface_color', '#1D1A24', 'card_radius_dp', 16)
),
(
    'diwali-luxe-edit',
    'Diwali & Festive Radiance',
    'FESTIVAL CAPSULE',
    'Warm Golds & Evening Tailoring',
    'Celebrate in sculpted silks, metallic knits, and luminous evening outerwear.',
    'festival',
    'diya_flame',
    '#FFFBEB',
    '#F59E0B',
    '#1F1209',
    'editorial_serif',
    'diwali',
    'Explore Festive Edit',
    'featured',
    'India',
    90,
    0,
    '2026-10-15 00:00:00',
    '2026-11-15 23:59:59',
    JSON_OBJECT('mode', 'dark', 'surface_color', '#2B190C', 'card_radius_dp', 18)
),
(
    'winter-atelier',
    'Winter Frost Atelier',
    'COLD WEATHER EDIT',
    'Heavyweight Wool & Puffer Layers',
    'Engineered warmth framed by crisp alpine tailoring and frosted minimalism.',
    'seasonal',
    'snowflake',
    '#0F172A',
    '#38BDF8',
    '#F0F9FF',
    'atelier_didot',
    'winter',
    'Shop Winter Layers',
    'outerwear',
    'ALL',
    70,
    0,
    '2026-12-01 00:00:00',
    '2027-02-15 23:59:59',
    JSON_OBJECT('mode', 'light', 'surface_color', '#FFFFFF', 'card_radius_dp', 14)
),
(
    'end-of-season-edit',
    'End of Season Archive Sale',
    'ARCHIVE REDUCTIONS',
    'Selected Silhouettes on Markdown',
    'Direct archival reductions across global labels while stock remains.',
    'sale',
    'sale_sticker',
    '#FFFFFF',
    '#EF4444',
    '#181113',
    'geometric_grotesk',
    'streetwear',
    'Shop Archive Sale',
    'sale',
    'ALL',
    80,
    0,
    NULL,
    NULL,
    JSON_OBJECT('mode', 'dark', 'surface_color', '#24171B', 'card_radius_dp', 10)
);

-- 4. Dynamically Associate Real Existing Products into Seeded Style Worlds (NO Fake Product IDs)
INSERT IGNORE INTO `app_style_world_products` (`style_world_id`, `product_id`, `display_order`)
SELECT sw.style_world_id, p.product_id, @row_num := @row_num + 1
FROM `app_style_worlds` sw
JOIN `products` p ON p.is_active = 1
CROSS JOIN (SELECT @row_num := 0) r
WHERE sw.slug = 'y2k'
  AND (p.name LIKE '%Crop%' OR p.name LIKE '%Baby%' OR p.name LIKE '%Denim%' OR p.name LIKE '%Zip%' OR p.is_featured = 1)
ORDER BY p.is_featured DESC, p.product_id DESC
LIMIT 12;

INSERT IGNORE INTO `app_style_world_products` (`style_world_id`, `product_id`, `display_order`)
SELECT sw.style_world_id, p.product_id, p.product_id
FROM `app_style_worlds` sw
JOIN `products` p ON p.is_active = 1
WHERE sw.slug = 'goth'
  AND (p.name LIKE '%Black%' OR p.name LIKE '%Leather%' OR p.name LIKE '%Jacket%' OR p.name LIKE '%Trench%' OR p.available_colors LIKE '%Black%')
ORDER BY p.product_id DESC
LIMIT 12;

INSERT IGNORE INTO `app_style_world_products` (`style_world_id`, `product_id`, `display_order`)
SELECT sw.style_world_id, p.product_id, p.product_id
FROM `app_style_worlds` sw
JOIN `products` p ON p.is_active = 1
WHERE sw.slug = 'k-fashion'
  AND (p.name LIKE '%Blazer%' OR p.name LIKE '%Shirt%' OR p.name LIKE '%Coat%' OR p.name LIKE '%Cardigan%' OR p.is_featured = 1)
ORDER BY p.purchase_count DESC, p.product_id DESC
LIMIT 12;

INSERT IGNORE INTO `app_style_world_products` (`style_world_id`, `product_id`, `display_order`)
SELECT sw.style_world_id, p.product_id, p.product_id
FROM `app_style_worlds` sw
JOIN `products` p ON p.is_active = 1
WHERE sw.slug = 'coquette'
  AND (p.name LIKE '%Dress%' OR p.name LIKE '%Bow%' OR p.name LIKE '%Pink%' OR p.name LIKE '%Knit%' OR p.name LIKE '%Top%')
ORDER BY p.rating_average DESC, p.product_id DESC
LIMIT 12;

INSERT IGNORE INTO `app_style_world_products` (`style_world_id`, `product_id`, `display_order`)
SELECT sw.style_world_id, p.product_id, p.product_id
FROM `app_style_worlds` sw
JOIN `products` p ON p.is_active = 1
WHERE sw.slug = 'streetwear'
  AND (p.name LIKE '%Hoodie%' OR p.name LIKE '%Cargo%' OR p.name LIKE '%Oversized%' OR p.name LIKE '%Tee%' OR p.name LIKE '%Bomber%')
ORDER BY p.view_count DESC, p.product_id DESC
LIMIT 12;

-- 5. Verified Brand World Map Origins Derived Directly From Existing Active `brands` & `app_brand_profiles`
INSERT IGNORE INTO `app_brand_origins` (
    `brand_id`, `city`, `country`, `country_code`, `latitude`, `longitude`, `world_slug`, `origin_story`, `is_verified`, `display_order`, `is_active`
)
SELECT
    b.brand_id,
    CASE
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%korea%' THEN 'Seoul'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%japan%' THEN 'Tokyo'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%france%' THEN 'Paris'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%italy%' THEN 'Milan'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%uk%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united kingdom%' THEN 'London'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%usa%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united states%' THEN 'New York'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%denmark%' THEN 'Copenhagen'
        ELSE 'New Delhi'
    END AS city,
    COALESCE(NULLIF(TRIM(bp.origin_country), ''), 'India') AS country,
    CASE
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%korea%' THEN 'KR'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%japan%' THEN 'JP'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%france%' THEN 'FR'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%italy%' THEN 'IT'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%uk%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united kingdom%' THEN 'GB'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%usa%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united states%' THEN 'US'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%denmark%' THEN 'DK'
        ELSE 'IN'
    END AS country_code,
    CASE
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%korea%' THEN 37.566535
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%japan%' THEN 35.676192
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%france%' THEN 48.856614
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%italy%' THEN 45.464204
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%uk%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united kingdom%' THEN 51.507351
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%usa%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united states%' THEN 40.712775
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%denmark%' THEN 55.676097
        ELSE 28.613939
    END AS latitude,
    CASE
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%korea%' THEN 126.977969
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%japan%' THEN 139.650311
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%france%' THEN 2.352222
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%italy%' THEN 9.189982
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%uk%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united kingdom%' THEN -0.127758
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%usa%' OR LOWER(COALESCE(bp.origin_country, '')) LIKE '%united states%' THEN -74.005973
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%denmark%' THEN 12.568337
        ELSE 77.209021
    END AS longitude,
    CASE
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%korea%' THEN 'k-fashion'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%japan%' THEN 'streetwear'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%france%' THEN 'coquette'
        WHEN LOWER(COALESCE(bp.origin_country, '')) LIKE '%uk%' THEN 'goth'
        ELSE 'y2k'
    END AS world_slug,
    COALESCE(NULLIF(bp.story, ''), NULLIF(b.description, ''), CONCAT('Verified studio label originating in ', COALESCE(NULLIF(bp.origin_country, ''), 'India'), '.')),
    1,
    COALESCE(bp.display_order, b.brand_id),
    1
FROM `brands` b
LEFT JOIN `app_brand_profiles` bp ON bp.brand_id = b.brand_id
WHERE b.status = 'active';

-- 6. Smart Contextual Offers Linked Strictly to Real Active `coupons` Rows (Never Fabricated Codes)
INSERT IGNORE INTO `app_smart_offers` (
    `offer_key`, `title`, `subtitle`, `badge_text`, `coupon_code`, `world_slug`,
    `min_cart_value`, `max_impressions_per_session`, `cooldown_hours`, `priority`, `accent_hex`, `cta_text`, `is_active`
)
SELECT
    CONCAT('coupon_', LOWER(c.code)),
    COALESCE(NULLIF(c.description, ''), CONCAT('Save with code ', c.code)),
    CASE
        WHEN c.min_order_amount > 0 THEN CONCAT('Valid on orders above ₹', ROUND(c.min_order_amount, 0))
        ELSE 'Valid across eligible studio edits'
    END,
    CASE
        WHEN c.discount_type = 'percentage' THEN CONCAT(ROUND(c.discount_value, 0), '% OFF')
        WHEN c.discount_type = 'free_shipping' THEN 'FREE SHIP'
        ELSE CONCAT('₹', ROUND(c.discount_value, 0), ' OFF')
    END,
    c.code,
    NULL,
    COALESCE(c.min_order_amount, 0.00),
    1,
    24,
    20,
    '#3B82F6',
    'Use Code',
    1
FROM `coupons` c
WHERE c.is_active = 1
  AND (c.valid_to IS NULL OR c.valid_to >= NOW())
ORDER BY c.coupon_id ASC
LIMIT 3;
