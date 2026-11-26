-- ============================================================================
-- MELVORE — FIXED IDEMPOTENT HERO CONTENT SEED
-- Prerequisite: Run `melvore_live_schema_compatibility_repair.sql` first.
--
-- Dynamically selects real active product photography from `products`
-- without overwriting or duplicating admin-customized `hero_content` rows.
-- ============================================================================

SET NAMES utf8mb4;

SET @h_img_1 = (SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1 OFFSET 0);
SET @h_img_2 = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.purchase_count DESC, p.product_id DESC LIMIT 1 OFFSET 0), @h_img_1);
SET @h_img_3 = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.product_id DESC LIMIT 1 OFFSET 0), @h_img_1);

INSERT INTO `hero_content`
  (`eyebrow`, `title`, `subtitle`, `image_url`, `mobile_image_url`, `desktop_image_url`, `cta_text`, `cta_link`, `display_order`, `is_active`)
SELECT
  'AUTUMN / WINTER EDIT',
  'The Leather Atelier',
  'Sculpted biker silhouettes, heavyweight hardware, and statement outerwear.',
  @h_img_1,
  @h_img_1,
  @h_img_1,
  'Shop Leather Edit',
  'jackets',
  1,
  1
FROM DUAL
WHERE @h_img_1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `hero_content` WHERE `title` = 'The Leather Atelier');

INSERT INTO `hero_content`
  (`eyebrow`, `title`, `subtitle`, `image_url`, `mobile_image_url`, `desktop_image_url`, `cta_text`, `cta_link`, `display_order`, `is_active`)
SELECT
  'SEOUL & STREET STORIES',
  'New Season, New Rules',
  'Relaxed proportions, contemporary layering, and everyday statement pieces.',
  @h_img_2,
  @h_img_2,
  @h_img_2,
  'Shop New Arrivals',
  'new-arrivals',
  2,
  1
FROM DUAL
WHERE @h_img_2 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `hero_content` WHERE `title` = 'New Season, New Rules');

INSERT INTO `hero_content`
  (`eyebrow`, `title`, `subtitle`, `image_url`, `mobile_image_url`, `desktop_image_url`, `cta_text`, `cta_link`, `display_order`, `is_active`)
SELECT
  'EVENING CAPSULE',
  'After Dark Silhouettes',
  'Sharp monochrome cuts and refined textures built for nightfall.',
  @h_img_3,
  @h_img_3,
  @h_img_3,
  'Explore Capsule',
  'all',
  3,
  1
FROM DUAL
WHERE @h_img_3 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `hero_content` WHERE `title` = 'After Dark Silhouettes');

-- Ensure responsive fields are populated on any existing active slide where NULL/empty
UPDATE `hero_content`
SET
  `mobile_image_url` = COALESCE(NULLIF(`mobile_image_url`, ''), `image_url`),
  `desktop_image_url` = COALESCE(NULLIF(`desktop_image_url`, ''), `image_url`)
WHERE `is_active` = 1 AND `image_url` IS NOT NULL AND `image_url` <> '';

SELECT `id`, `eyebrow`, `title`, `image_url`, `is_active`, `display_order`
FROM `hero_content`
ORDER BY `display_order` ASC, `id` ASC;
