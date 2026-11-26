-- ============================================================================
-- MELVORE CUSTOMER APP EXPERIENCE — ADDITIVE SCHEMA MIGRATION
-- Safe, non-destructive migration (`CREATE TABLE IF NOT EXISTS`).
-- Does NOT drop tables, truncate data, or alter orders/payment records.
-- ============================================================================

SET NAMES utf8mb4;

-- 1. Festival & Regional Campaigns
CREATE TABLE IF NOT EXISTS `app_campaigns` (
  `campaign_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slug` VARCHAR(120) NOT NULL,
  `title` VARCHAR(180) NOT NULL,
  `subtitle` VARCHAR(320) DEFAULT NULL,
  `festival_key` VARCHAR(64) NOT NULL DEFAULT 'seasonal',
  `animation_key` VARCHAR(64) NOT NULL DEFAULT 'smooth_reveal',
  `animation_duration_ms` INT NOT NULL DEFAULT 2400,
  `primary_color` VARCHAR(16) NOT NULL DEFAULT '#09090B',
  `accent_color` VARCHAR(16) NOT NULL DEFAULT '#D4AF37',
  `background_color` VARCHAR(16) NOT NULL DEFAULT '#FFFFFF',
  `banner_image_url` VARCHAR(512) DEFAULT NULL,
  `mobile_banner_url` VARCHAR(512) DEFAULT NULL,
  `video_url` VARCHAR(512) DEFAULT NULL,
  `cta_text` VARCHAR(80) NOT NULL DEFAULT 'Shop Now',
  `cta_link` VARCHAR(255) DEFAULT NULL,
  `priority` INT NOT NULL DEFAULT 10,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `start_date` DATETIME DEFAULT NULL,
  `end_date` DATETIME DEFAULT NULL,
  `theme_config` JSON DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`campaign_id`),
  UNIQUE KEY `uk_app_campaigns_slug` (`slug`),
  KEY `idx_campaigns_active_dates` (`is_active`, `start_date`, `end_date`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_campaign_regions` (
  `region_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `campaign_id` INT UNSIGNED NOT NULL,
  `country` VARCHAR(80) DEFAULT 'India',
  `state` VARCHAR(80) DEFAULT NULL,
  `city` VARCHAR(80) DEFAULT NULL,
  `pincode` VARCHAR(20) DEFAULT NULL,
  `is_nationwide` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`region_id`),
  KEY `idx_campaign_regions_lookup` (`campaign_id`, `country`, `state`, `city`, `pincode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_campaign_assets` (
  `asset_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `campaign_id` INT UNSIGNED NOT NULL,
  `asset_type` VARCHAR(32) NOT NULL DEFAULT 'image',
  `media_url` VARCHAR(512) NOT NULL,
  `mobile_media_url` VARCHAR(512) DEFAULT NULL,
  `animation_key` VARCHAR(64) NOT NULL DEFAULT 'none',
  `title` VARCHAR(180) DEFAULT NULL,
  `subtitle` VARCHAR(280) DEFAULT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`asset_id`),
  KEY `idx_campaign_assets_order` (`campaign_id`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Dynamic Homepage Sections & Product Placements
CREATE TABLE IF NOT EXISTS `app_home_sections` (
  `section_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `section_key` VARCHAR(80) NOT NULL,
  `section_type` VARCHAR(48) NOT NULL DEFAULT 'editorial_campaign',
  `eyebrow` VARCHAR(80) DEFAULT NULL,
  `title` VARCHAR(180) NOT NULL,
  `subtitle` VARCHAR(320) DEFAULT NULL,
  `animation_key` VARCHAR(64) NOT NULL DEFAULT 'smooth_reveal',
  `theme_key` VARCHAR(64) NOT NULL DEFAULT 'light_editorial',
  `theme_config` JSON DEFAULT NULL,
  `cta_text` VARCHAR(80) NOT NULL DEFAULT 'Explore Edit',
  `cta_link` VARCHAR(255) DEFAULT NULL,
  `item_limit` INT NOT NULL DEFAULT 8,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `start_date` DATETIME DEFAULT NULL,
  `end_date` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`section_id`),
  UNIQUE KEY `uk_home_sections_key` (`section_key`),
  KEY `idx_home_sections_order` (`is_active`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_home_section_items` (
  `item_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `section_id` INT UNSIGNED NOT NULL,
  `title` VARCHAR(180) NOT NULL,
  `subtitle` VARCHAR(280) DEFAULT NULL,
  `badge_text` VARCHAR(64) DEFAULT NULL,
  `image_url` VARCHAR(512) DEFAULT NULL,
  `mobile_image_url` VARCHAR(512) DEFAULT NULL,
  `theme_key` VARCHAR(64) NOT NULL DEFAULT 'light_editorial',
  `animation_key` VARCHAR(64) NOT NULL DEFAULT 'smooth_reveal',
  `target_type` VARCHAR(32) NOT NULL DEFAULT 'category',
  `target_value` VARCHAR(180) DEFAULT NULL,
  `target_id` INT DEFAULT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `start_date` DATETIME DEFAULT NULL,
  `end_date` DATETIME DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `idx_home_section_items_order` (`section_id`, `is_active`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_product_placements` (
  `placement_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id` INT NOT NULL,
  `section_key` VARCHAR(80) NOT NULL DEFAULT 'featured',
  `is_sponsored` TINYINT(1) NOT NULL DEFAULT 0,
  `badge_override` VARCHAR(64) DEFAULT NULL,
  `express_delivery` TINYINT(1) NOT NULL DEFAULT 0,
  `priority` INT NOT NULL DEFAULT 10,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `start_date` DATETIME DEFAULT NULL,
  `end_date` DATETIME DEFAULT NULL,
  PRIMARY KEY (`placement_id`),
  KEY `idx_placements_section_active` (`section_key`, `is_active`, `priority`, `display_order`),
  KEY `idx_placements_sponsored` (`is_sponsored`, `is_active`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Brand Editorial Profiles & Activity
CREATE TABLE IF NOT EXISTS `app_brand_profiles` (
  `profile_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `brand_id` INT NOT NULL,
  `tagline` VARCHAR(220) DEFAULT NULL,
  `story` TEXT DEFAULT NULL,
  `editorial_cover_url` VARCHAR(512) DEFAULT NULL,
  `mobile_cover_url` VARCHAR(512) DEFAULT NULL,
  `video_url` VARCHAR(512) DEFAULT NULL,
  `origin_country` VARCHAR(80) DEFAULT NULL,
  `is_featured` TINYINT(1) NOT NULL DEFAULT 0,
  `is_sponsored` TINYINT(1) NOT NULL DEFAULT 0,
  `is_new` TINYINT(1) NOT NULL DEFAULT 0,
  `trending_score` INT NOT NULL DEFAULT 0,
  `display_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uk_brand_profiles_brand` (`brand_id`),
  KEY `idx_brand_profiles_flags` (`is_featured`, `is_sponsored`, `is_new`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_user_brand_activity` (
  `activity_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `brand_id` INT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`activity_id`),
  KEY `idx_brand_activity_user` (`user_id`, `created_at`),
  KEY `idx_brand_activity_guest` (`guest_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Style Worlds & Aesthetic Curations
CREATE TABLE IF NOT EXISTS `app_style_worlds` (
  `style_world_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slug` VARCHAR(100) NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `tagline` VARCHAR(240) DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `theme_key` VARCHAR(64) NOT NULL DEFAULT 'editorial',
  `animation_key` VARCHAR(64) NOT NULL DEFAULT 'smooth_reveal',
  `font_style` VARCHAR(48) NOT NULL DEFAULT 'editorial_serif',
  `primary_color` VARCHAR(16) NOT NULL DEFAULT '#FFFFFF',
  `accent_color` VARCHAR(16) NOT NULL DEFAULT '#D4AF37',
  `background_color` VARCHAR(16) NOT NULL DEFAULT '#141418',
  `cover_image_url` VARCHAR(512) DEFAULT NULL,
  `mobile_image_url` VARCHAR(512) DEFAULT NULL,
  `video_url` VARCHAR(512) DEFAULT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `start_date` DATETIME DEFAULT NULL,
  `end_date` DATETIME DEFAULT NULL,
  `theme_config` JSON DEFAULT NULL,
  PRIMARY KEY (`style_world_id`),
  UNIQUE KEY `uk_style_worlds_slug` (`slug`),
  KEY `idx_style_worlds_active` (`is_active`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_style_world_products` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `style_world_id` INT UNSIGNED NOT NULL,
  `product_id` INT NOT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_style_world_product` (`style_world_id`, `product_id`),
  KEY `idx_sw_products_order` (`style_world_id`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Vertical Product Videos & Engagement Events
CREATE TABLE IF NOT EXISTS `app_product_videos` (
  `video_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id` INT DEFAULT NULL,
  `brand_id` INT DEFAULT NULL,
  `style_world_id` INT UNSIGNED DEFAULT NULL,
  `title` VARCHAR(200) NOT NULL,
  `caption` VARCHAR(500) DEFAULT NULL,
  `creator_name` VARCHAR(120) DEFAULT 'Melvore Studio',
  `creator_handle` VARCHAR(80) DEFAULT '@melvore',
  `video_url` VARCHAR(512) NOT NULL,
  `poster_url` VARCHAR(512) DEFAULT NULL,
  `thumbnail_url` VARCHAR(512) DEFAULT NULL,
  `duration_seconds` INT NOT NULL DEFAULT 15,
  `aspect_ratio` VARCHAR(16) NOT NULL DEFAULT '9:16',
  `is_sponsored` TINYINT(1) NOT NULL DEFAULT 0,
  `is_featured` TINYINT(1) NOT NULL DEFAULT 0,
  `display_order` INT NOT NULL DEFAULT 0,
  `views_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `likes_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `saves_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `shares_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `publish_at` DATETIME DEFAULT NULL,
  `expire_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`video_id`),
  KEY `idx_videos_feed` (`is_active`, `is_featured`, `display_order`, `video_id`),
  KEY `idx_videos_brand_style` (`brand_id`, `style_world_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_video_events` (
  `event_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `video_id` INT UNSIGNED NOT NULL,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `event_type` VARCHAR(32) NOT NULL,
  `watch_seconds` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  KEY `idx_video_events_user` (`video_id`, `user_id`, `event_type`),
  KEY `idx_video_events_guest` (`video_id`, `guest_id`, `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Melvore Luxe / Elite Packs & Items
CREATE TABLE IF NOT EXISTS `app_elite_packs` (
  `pack_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slug` VARCHAR(120) NOT NULL,
  `section_key` VARCHAR(80) NOT NULL DEFAULT 'featured_luxury',
  `brand_name` VARCHAR(120) DEFAULT 'MELVORE LUXE',
  `eyebrow` VARCHAR(100) DEFAULT NULL,
  `title` VARCHAR(180) NOT NULL,
  `subtitle` VARCHAR(360) DEFAULT NULL,
  `story` TEXT DEFAULT NULL,
  `cta_text` VARCHAR(80) NOT NULL DEFAULT 'Enter Collection',
  `cta_link` VARCHAR(255) DEFAULT NULL,
  `hero_image_url` VARCHAR(512) DEFAULT NULL,
  `mobile_image_url` VARCHAR(512) DEFAULT NULL,
  `video_url` VARCHAR(512) DEFAULT NULL,
  `animation_key` VARCHAR(64) NOT NULL DEFAULT 'metallic_shine',
  `accent_color` VARCHAR(16) NOT NULL DEFAULT '#D4AF37',
  `background_color` VARCHAR(16) NOT NULL DEFAULT '#000000',
  `is_hero` TINYINT(1) NOT NULL DEFAULT 0,
  `is_sponsored` TINYINT(1) NOT NULL DEFAULT 0,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `start_date` DATETIME DEFAULT NULL,
  `end_date` DATETIME DEFAULT NULL,
  `theme_config` JSON DEFAULT NULL,
  PRIMARY KEY (`pack_id`),
  UNIQUE KEY `uk_elite_packs_slug` (`slug`),
  KEY `idx_elite_packs_order` (`is_active`, `is_hero`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_elite_pack_items` (
  `item_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pack_id` INT UNSIGNED NOT NULL,
  `item_type` VARCHAR(32) NOT NULL DEFAULT 'product',
  `product_id` INT DEFAULT NULL,
  `brand_id` INT DEFAULT NULL,
  `title` VARCHAR(180) DEFAULT NULL,
  `subtitle` VARCHAR(280) DEFAULT NULL,
  `badge_text` VARCHAR(64) DEFAULT NULL,
  `image_url` VARCHAR(512) DEFAULT NULL,
  `video_url` VARCHAR(512) DEFAULT NULL,
  `is_sponsored` TINYINT(1) NOT NULL DEFAULT 0,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`item_id`),
  KEY `idx_elite_pack_items_order` (`pack_id`, `is_active`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Personalization Signals & Search Synonyms
CREATE TABLE IF NOT EXISTS `app_user_style_signals` (
  `signal_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `style_world_id` INT UNSIGNED DEFAULT NULL,
  `brand_id` INT DEFAULT NULL,
  `category_id` INT DEFAULT NULL,
  `product_id` INT DEFAULT NULL,
  `signal_type` VARCHAR(32) NOT NULL DEFAULT 'view',
  `signal_weight` DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`signal_id`),
  KEY `idx_style_signals_user` (`user_id`, `created_at`),
  KEY `idx_style_signals_guest` (`guest_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_search_synonyms` (
  `synonym_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `term` VARCHAR(100) NOT NULL,
  `expanded_terms` VARCHAR(320) NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`synonym_id`),
  UNIQUE KEY `uk_search_synonyms_term` (`term`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_search_logs` (
  `log_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `query_text` VARCHAR(180) NOT NULL,
  `results_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_search_logs_query` (`query_text`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
