-- ============================================================================
-- MELVORE — LIVE SCHEMA COMPATIBILITY REPAIR (NON-DESTRUCTIVE & IDEMPOTENT)
-- Target Database: u205121999_melvore (MySQL 5.7+ / 8.0+ / MariaDB 10.3+)
--
-- PURPOSE:
--   Repairs column drift on existing live tables WITHOUT dropping or truncating
--   any data, and safely ensures all additive experience tables exist.
--   Uses dynamic PREPARE/EXECUTE guarded by information_schema so it runs cleanly
--   in phpMyAdmin & MySQL CLI without requiring CREATE ROUTINE or SUPER rights.
--
-- NEVER TOUCHES / DROPS CORE COMMERCE TABLES:
--   users, products, product_variations, categories, brands, orders,
--   order_items, payments, addresses, cart, user_cart, wishlist, reviews,
--   coupons, inventory, site_settings, api_tokens.
-- ============================================================================

SET NAMES utf8mb4;

-- ============================================================================
-- 1. HERO CONTENT (`hero_content`) — Resolves #1054 Unknown column 'image_url'
-- ============================================================================
CREATE TABLE IF NOT EXISTS `hero_content` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `eyebrow` VARCHAR(120) DEFAULT NULL,
  `title` VARCHAR(200) DEFAULT NULL,
  `subtitle` VARCHAR(360) DEFAULT NULL,
  `image_url` VARCHAR(512) DEFAULT NULL,
  `mobile_image_url` VARCHAR(512) DEFAULT NULL,
  `desktop_image_url` VARCHAR(512) DEFAULT NULL,
  `cta_text` VARCHAR(80) DEFAULT NULL,
  `cta_link` VARCHAR(255) DEFAULT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_hero_content_active_order` (`is_active`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add any missing columns on pre-existing `hero_content`
SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `hero_content` ADD COLUMN `eyebrow` VARCHAR(120) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_content' AND COLUMN_NAME = 'eyebrow');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `hero_content` ADD COLUMN `title` VARCHAR(200) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_content' AND COLUMN_NAME = 'title');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `hero_content` ADD COLUMN `subtitle` VARCHAR(360) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_content' AND COLUMN_NAME = 'subtitle');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `hero_content` ADD COLUMN `image_url` VARCHAR(512) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_content' AND COLUMN_NAME = 'image_url');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `hero_content` ADD COLUMN `mobile_image_url` VARCHAR(512) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_content' AND COLUMN_NAME = 'mobile_image_url');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `hero_content` ADD COLUMN `desktop_image_url` VARCHAR(512) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_content' AND COLUMN_NAME = 'desktop_image_url');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `hero_content` ADD COLUMN `cta_text` VARCHAR(80) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_content' AND COLUMN_NAME = 'cta_text');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `hero_content` ADD COLUMN `cta_link` VARCHAR(255) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_content' AND COLUMN_NAME = 'cta_link');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `hero_content` ADD COLUMN `display_order` INT NOT NULL DEFAULT 0', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_content' AND COLUMN_NAME = 'display_order');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `hero_content` ADD COLUMN `is_active` TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_content' AND COLUMN_NAME = 'is_active');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- Backfill `image_url` from legacy column names if a pre-existing column is populated
SET @legacy_hero_col = (
  SELECT COLUMN_NAME
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'hero_content'
    AND COLUMN_NAME IN ('image', 'banner_image', 'desktop_image', 'img_src', 'media_url')
  ORDER BY FIELD(COLUMN_NAME, 'image', 'banner_image', 'desktop_image', 'img_src', 'media_url')
  LIMIT 1
);
SET @s = IF(
  @legacy_hero_col IS NOT NULL,
  CONCAT('UPDATE `hero_content` SET `image_url` = `', @legacy_hero_col, '` WHERE (`image_url` IS NULL OR `image_url` = '''') AND `', @legacy_hero_col, '` IS NOT NULL AND `', @legacy_hero_col, '` <> '''''),
  'SELECT 1'
);
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

UPDATE `hero_content`
SET `mobile_image_url` = COALESCE(NULLIF(`mobile_image_url`, ''), `image_url`),
    `desktop_image_url` = COALESCE(NULLIF(`desktop_image_url`, ''), `image_url`)
WHERE (`mobile_image_url` IS NULL OR `mobile_image_url` = '' OR `desktop_image_url` IS NULL OR `desktop_image_url` = '')
  AND `image_url` IS NOT NULL AND `image_url` <> '';

-- ============================================================================
-- 2. DELIVERY CACHE (`app_delivery_serviceability_cache`)
--    Resolves #1054 Unknown column 'city' / 'cod_available'
-- ============================================================================
CREATE TABLE IF NOT EXISTS `app_delivery_serviceability_cache` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pincode` VARCHAR(12) NOT NULL,
  `city` VARCHAR(100) DEFAULT NULL,
  `state` VARCHAR(100) DEFAULT NULL,
  `is_serviceable` TINYINT(1) NOT NULL DEFAULT 1,
  `cod_available` TINYINT(1) NOT NULL DEFAULT 1,
  `express_available` TINYINT(1) NOT NULL DEFAULT 0,
  `estimated_days_min` INT NOT NULL DEFAULT 3,
  `estimated_days_max` INT NOT NULL DEFAULT 6,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_delivery_pincode` (`pincode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_delivery_serviceability_cache` ADD COLUMN `city` VARCHAR(100) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_delivery_serviceability_cache' AND COLUMN_NAME = 'city');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_delivery_serviceability_cache` ADD COLUMN `state` VARCHAR(100) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_delivery_serviceability_cache' AND COLUMN_NAME = 'state');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_delivery_serviceability_cache` ADD COLUMN `is_serviceable` TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_delivery_serviceability_cache' AND COLUMN_NAME = 'is_serviceable');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_delivery_serviceability_cache` ADD COLUMN `cod_available` TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_delivery_serviceability_cache' AND COLUMN_NAME = 'cod_available');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_delivery_serviceability_cache` ADD COLUMN `express_available` TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_delivery_serviceability_cache' AND COLUMN_NAME = 'express_available');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_delivery_serviceability_cache` ADD COLUMN `estimated_days_min` INT NOT NULL DEFAULT 3', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_delivery_serviceability_cache' AND COLUMN_NAME = 'estimated_days_min');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_delivery_serviceability_cache` ADD COLUMN `estimated_days_max` INT NOT NULL DEFAULT 6', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_delivery_serviceability_cache' AND COLUMN_NAME = 'estimated_days_max');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_delivery_serviceability_cache` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_delivery_serviceability_cache' AND COLUMN_NAME = 'updated_at');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_delivery_serviceability_cache` ADD UNIQUE KEY `uk_delivery_pincode` (`pincode`)', 'SELECT 1')
          FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_delivery_serviceability_cache' AND NON_UNIQUE = 0 AND COLUMN_NAME = 'pincode');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================================
-- 3. IMMERSIVE WORLDS, SATISFACTION FEEDBACK, AUDIO, VIDEO, LUXE & EDITORIAL CONFIGS
-- ============================================================================
CREATE TABLE IF NOT EXISTS `app_style_world_feedback` (
  `feedback_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `style_world_id` INT UNSIGNED NOT NULL,
  `world_slug` VARCHAR(100) NOT NULL,
  `satisfaction_score` TINYINT UNSIGNED NOT NULL,
  `feedback_label` VARCHAR(64) NOT NULL,
  `coupon_id` INT DEFAULT NULL,
  `coupon_code` VARCHAR(64) DEFAULT NULL,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`feedback_id`),
  UNIQUE KEY `uk_world_feedback_user` (`style_world_id`, `user_id`),
  UNIQUE KEY `uk_world_feedback_guest` (`style_world_id`, `guest_id`),
  KEY `idx_world_feedback_user_lookup` (`user_id`, `style_world_id`),
  KEY `idx_world_feedback_guest_lookup` (`guest_id`, `style_world_id`),
  KEY `idx_world_feedback_slug` (`world_slug`, `submitted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_category_editorial_configs` (
  `config_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id` INT NOT NULL,
  `motif_key` VARCHAR(64) NOT NULL DEFAULT 'editorial_frame',
  `editorial_badge` VARCHAR(64) DEFAULT NULL,
  `editorial_subtitle` VARCHAR(180) DEFAULT NULL,
  `accent_hex` VARCHAR(16) NOT NULL DEFAULT '#141414',
  `bg_hex` VARCHAR(16) NOT NULL DEFAULT '#F8F7F4',
  `animation_key` VARCHAR(64) NOT NULL DEFAULT 'zoom_reveal',
  `tile_span` VARCHAR(24) NOT NULL DEFAULT 'regular',
  `cover_image_url` VARCHAR(512) DEFAULT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_cat_editorial_cat_id` (`category_id`),
  KEY `idx_cat_editorial_order` (`is_active`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_recommendation_cache` (
  `cache_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `actor_key` VARCHAR(128) NOT NULL,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `world_slug` VARCHAR(100) NOT NULL DEFAULT '',
  `payload_json` LONGTEXT NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`cache_id`),
  UNIQUE KEY `uk_rec_actor_world` (`actor_key`, `world_slug`),
  KEY `idx_rec_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_user_preferences` (
  `pref_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `ambient_audio_enabled` TINYINT(1) NOT NULL DEFAULT 0,
  `ui_sfx_enabled` TINYINT(1) NOT NULL DEFAULT 0,
  `reduced_motion` TINYINT(1) NOT NULL DEFAULT 0,
  `audio_consent_answered` TINYINT(1) NOT NULL DEFAULT 0,
  `video_autoplay_muted` TINYINT(1) NOT NULL DEFAULT 1,
  `data_saver_mode` TINYINT(1) NOT NULL DEFAULT 0,
  `preferred_world_slug` VARCHAR(100) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`pref_id`),
  UNIQUE KEY `uk_user_prefs_user` (`user_id`),
  KEY `idx_user_prefs_guest` (`guest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_product_media` (
  `media_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id` INT NOT NULL,
  `media_type` VARCHAR(24) NOT NULL DEFAULT 'image',
  `media_url` VARCHAR(512) NOT NULL,
  `thumbnail_url` VARCHAR(512) DEFAULT NULL,
  `alt_text` VARCHAR(240) DEFAULT NULL,
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`media_id`),
  KEY `idx_product_media_lookup` (`product_id`, `is_active`, `is_primary`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 4. COLUMN DRIFT REPAIR ACROSS ALL REMAINING `app_*` TABLES
-- ============================================================================

-- `app_style_worlds`
SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `ambient_preset` VARCHAR(64) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'ambient_preset');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `audio_track_key` VARCHAR(64) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'audio_track_key');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `audio_url` VARCHAR(512) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'audio_url');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `audio_loop` TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'audio_loop');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `audio_volume` DECIMAL(3,2) NOT NULL DEFAULT 0.45', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'audio_volume');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `promo_video_url` VARCHAR(512) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'promo_video_url');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `promo_video_poster_url` VARCHAR(512) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'promo_video_poster_url');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `promo_video_is_sponsored` TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'promo_video_is_sponsored');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `button_style` VARCHAR(64) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'button_style');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `runway_config` TEXT DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'runway_config');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `version_tag` VARCHAR(32) NOT NULL DEFAULT ''3.0''', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'version_tag');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_style_worlds` ADD COLUMN `theme_config` TEXT DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_style_worlds' AND COLUMN_NAME = 'theme_config');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- `app_occasions`
SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_occasions` ADD COLUMN `audio_url` VARCHAR(512) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'audio_url');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_occasions` ADD COLUMN `audio_loop` TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'audio_loop');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_occasions` ADD COLUMN `button_style` VARCHAR(64) DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'button_style');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- `app_user_preferences`
SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_user_preferences` ADD COLUMN `video_autoplay_muted` TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user_preferences' AND COLUMN_NAME = 'video_autoplay_muted');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_user_preferences` ADD COLUMN `data_saver_mode` TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user_preferences' AND COLUMN_NAME = 'data_saver_mode');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- `app_elite_packs`
SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_elite_packs` ADD COLUMN `layout_type` VARCHAR(48) NOT NULL DEFAULT ''editorial_grid''', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_elite_packs' AND COLUMN_NAME = 'layout_type');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_elite_packs` ADD COLUMN `featured_product_id` INT DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_elite_packs' AND COLUMN_NAME = 'featured_product_id');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_elite_packs` ADD COLUMN `status` VARCHAR(24) NOT NULL DEFAULT ''published''', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_elite_packs' AND COLUMN_NAME = 'status');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_elite_packs` ADD COLUMN `scheduled_publish_at` DATETIME DEFAULT NULL', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_elite_packs' AND COLUMN_NAME = 'scheduled_publish_at');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- `app_user_brand_activity`
SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_user_brand_activity` ADD COLUMN `activity_type` VARCHAR(32) NOT NULL DEFAULT ''view''', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user_brand_activity' AND COLUMN_NAME = 'activity_type');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_user_brand_activity` ADD COLUMN `last_viewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user_brand_activity' AND COLUMN_NAME = 'last_viewed_at');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- `app_search_synonyms` & `app_search_logs`
SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_search_synonyms` ADD COLUMN `synonyms` VARCHAR(320) NOT NULL DEFAULT ''''', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_search_synonyms' AND COLUMN_NAME = 'synonyms');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_search_synonyms` ADD COLUMN `expanded_terms` VARCHAR(320) NOT NULL DEFAULT ''''', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_search_synonyms' AND COLUMN_NAME = 'expanded_terms');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `app_search_logs` ADD COLUMN `normalized_query` VARCHAR(180) NOT NULL DEFAULT ''''', 'SELECT 1')
          FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_search_logs' AND COLUMN_NAME = 'normalized_query');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- Sync synonyms/expanded_terms if one was populated earlier
UPDATE `app_search_synonyms`
SET `synonyms` = CASE WHEN (`synonyms` IS NULL OR `synonyms` = '') THEN `expanded_terms` ELSE `synonyms` END,
    `expanded_terms` = CASE WHEN (`expanded_terms` IS NULL OR `expanded_terms` = '') THEN `synonyms` ELSE `expanded_terms` END
WHERE (`synonyms` = '' OR `synonyms` IS NULL OR `expanded_terms` = '' OR `expanded_terms` IS NULL);
