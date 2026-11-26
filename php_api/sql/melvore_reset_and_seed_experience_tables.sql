-- ============================================================================
-- MELVORE — COMPLETE RESET & DYNAMIC SEED FOR EXPERIENCE & FASHION WORLD TABLES
-- Target Database: u205121999_melvore (MySQL 8.0+ / MariaDB 10.3+)
--
-- WHAT THIS SCRIPT DOES:
--   1. Safely DROPS & RECREATES all 23 requested `app_*` experience tables
--      (plus companion `app_tags` and `app_product_tag_map`) with exact column
--      parity required by `/php_api/api/*` (including `normalized_query` in
--      `app_search_logs`, `synonyms` in `app_search_synonyms`, `activity_type`
--      & `last_viewed_at` in `app_user_brand_activity`, `theme_config` in
--      `app_brand_profiles`, and `ambient_preset` / `audio_track_key` /
--      `version_tag` in `app_style_worlds`).
--   2. Non-destructively ensures companion tables (`app_user_preferences`,
--      `app_product_media`, `app_delivery_serviceability_cache`) exist.
--   3. Dynamically populates every table using live rows from `products`,
--      `brands`, `categories`, and `coupons` — NEVER fabricating fake product
--      IDs, brand IDs, video URLs, or coupon codes.
--   4. Leaves ALL core commerce tables untouched:
--      `users`, `products`, `product_variations`, `categories`, `brands`,
--      `coupons`, `orders`, `order_items`, `payments`, `addresses`, `cart`,
--      `user_cart`, `wishlist`, `reviews`, `hero_content`, `banner_content`,
--      `site_settings`, `api_tokens`.
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- SECTION 1: DROP & RECREATE REQUESTED TABLES IN CLEAN STATE
-- ============================================================================

DROP TABLE IF EXISTS `app_offer_impressions`;
DROP TABLE IF EXISTS `app_smart_offers`;
DROP TABLE IF EXISTS `app_world_brands`;
DROP TABLE IF EXISTS `app_brand_origins`;
DROP TABLE IF EXISTS `app_world_assets`;
DROP TABLE IF EXISTS `app_occasions`;
DROP TABLE IF EXISTS `app_search_logs`;
DROP TABLE IF EXISTS `app_search_synonyms`;
DROP TABLE IF EXISTS `app_user_style_signals`;
DROP TABLE IF EXISTS `app_elite_pack_items`;
DROP TABLE IF EXISTS `app_elite_packs`;
DROP TABLE IF EXISTS `app_video_events`;
DROP TABLE IF EXISTS `app_product_videos`;
DROP TABLE IF EXISTS `app_style_world_products`;
DROP TABLE IF EXISTS `app_style_worlds`;
DROP TABLE IF EXISTS `app_user_brand_activity`;
DROP TABLE IF EXISTS `app_brand_profiles`;
DROP TABLE IF EXISTS `app_product_placements`;
DROP TABLE IF EXISTS `app_home_section_items`;
DROP TABLE IF EXISTS `app_home_sections`;
DROP TABLE IF EXISTS `app_campaign_assets`;
DROP TABLE IF EXISTS `app_campaign_regions`;
DROP TABLE IF EXISTS `app_campaigns`;
DROP TABLE IF EXISTS `app_product_tag_map`;
DROP TABLE IF EXISTS `app_tags`;

-- ----------------------------------------------------------------------------
-- 1. Campaigns, Regions & Assets
-- ----------------------------------------------------------------------------
CREATE TABLE `app_campaigns` (
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

CREATE TABLE `app_campaign_regions` (
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

CREATE TABLE `app_campaign_assets` (
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

-- ----------------------------------------------------------------------------
-- 2. Homepage Sections, Section Items & Merchandising Placements
-- ----------------------------------------------------------------------------
CREATE TABLE `app_home_sections` (
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

CREATE TABLE `app_home_section_items` (
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

CREATE TABLE `app_product_placements` (
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
  UNIQUE KEY `uk_placement_section_prod` (`section_key`, `product_id`),
  KEY `idx_placements_section_active` (`section_key`, `is_active`, `priority`, `display_order`),
  KEY `idx_placements_sponsored` (`is_sponsored`, `is_active`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. Brand Editorial Profiles, Origins & Brand Activity
-- ----------------------------------------------------------------------------
CREATE TABLE `app_brand_profiles` (
  `profile_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `brand_id` INT NOT NULL,
  `tagline` VARCHAR(220) DEFAULT NULL,
  `story` TEXT DEFAULT NULL,
  `editorial_cover_url` VARCHAR(512) DEFAULT NULL,
  `mobile_cover_url` VARCHAR(512) DEFAULT NULL,
  `video_url` VARCHAR(512) DEFAULT NULL,
  `origin_country` VARCHAR(80) DEFAULT NULL,
  `theme_config` JSON DEFAULT NULL,
  `is_featured` TINYINT(1) NOT NULL DEFAULT 0,
  `is_sponsored` TINYINT(1) NOT NULL DEFAULT 0,
  `is_new` TINYINT(1) NOT NULL DEFAULT 0,
  `trending_score` INT NOT NULL DEFAULT 0,
  `display_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uk_brand_profiles_brand` (`brand_id`),
  KEY `idx_brand_profiles_flags` (`is_featured`, `is_sponsored`, `is_new`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_user_brand_activity` (
  `activity_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `brand_id` INT NOT NULL,
  `activity_type` VARCHAR(32) NOT NULL DEFAULT 'view',
  `last_viewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`activity_id`),
  KEY `idx_brand_activity_user` (`user_id`, `created_at`),
  KEY `idx_brand_activity_guest` (`guest_id`, `created_at`),
  KEY `idx_brand_activity_recent` (`brand_id`, `last_viewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_brand_origins` (
  `origin_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `brand_id` INT NOT NULL,
  `city` VARCHAR(100) NOT NULL,
  `country` VARCHAR(100) NOT NULL,
  `country_code` CHAR(4) NOT NULL DEFAULT '',
  `latitude` DECIMAL(9,6) NOT NULL,
  `longitude` DECIMAL(9,6) NOT NULL,
  `world_slug` VARCHAR(100) DEFAULT NULL,
  `origin_story` TEXT DEFAULT NULL,
  `is_verified` TINYINT(1) NOT NULL DEFAULT 1,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`origin_id`),
  UNIQUE KEY `uk_brand_origin_brand` (`brand_id`),
  KEY `idx_brand_origins_active` (`is_active`, `is_verified`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. Dynamic Style Worlds, Products, Brands, Assets & Occasions
-- ----------------------------------------------------------------------------
CREATE TABLE `app_style_worlds` (
  `style_world_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slug` VARCHAR(100) NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `tagline` VARCHAR(240) DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `theme_key` VARCHAR(64) NOT NULL DEFAULT 'editorial',
  `animation_key` VARCHAR(64) NOT NULL DEFAULT 'smooth_reveal',
  `font_style` VARCHAR(48) NOT NULL DEFAULT 'editorial_serif',
  `ambient_preset` VARCHAR(64) DEFAULT NULL,
  `audio_track_key` VARCHAR(64) DEFAULT NULL,
  `primary_color` VARCHAR(16) NOT NULL DEFAULT '#FFFFFF',
  `accent_color` VARCHAR(16) NOT NULL DEFAULT '#D4AF37',
  `background_color` VARCHAR(16) NOT NULL DEFAULT '#141418',
  `cover_image_url` VARCHAR(512) DEFAULT NULL,
  `mobile_image_url` VARCHAR(512) DEFAULT NULL,
  `video_url` VARCHAR(512) DEFAULT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  `version_tag` VARCHAR(32) NOT NULL DEFAULT '2.0',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `start_date` DATETIME DEFAULT NULL,
  `end_date` DATETIME DEFAULT NULL,
  `theme_config` JSON DEFAULT NULL,
  PRIMARY KEY (`style_world_id`),
  UNIQUE KEY `uk_style_worlds_slug` (`slug`),
  KEY `idx_style_worlds_active` (`is_active`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_style_world_products` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `style_world_id` INT UNSIGNED NOT NULL,
  `product_id` INT NOT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_style_world_product` (`style_world_id`, `product_id`),
  KEY `idx_sw_products_order` (`style_world_id`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_world_brands` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `style_world_id` INT UNSIGNED NOT NULL,
  `brand_id` INT NOT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_world_brand` (`style_world_id`, `brand_id`),
  KEY `idx_world_brands_order` (`style_world_id`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_world_assets` (
  `asset_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `asset_key` VARCHAR(80) NOT NULL,
  `world_slug` VARCHAR(100) NOT NULL,
  `occasion_slug` VARCHAR(100) DEFAULT NULL,
  `vector_type` VARCHAR(64) NOT NULL,
  `layer_role` VARCHAR(48) NOT NULL DEFAULT 'ambient_floating',
  `primary_hex` VARCHAR(16) NOT NULL DEFAULT '#FFFFFF',
  `secondary_hex` VARCHAR(16) NOT NULL DEFAULT '#D4AF37',
  `opacity` DECIMAL(4,2) NOT NULL DEFAULT 0.35,
  `motion_pattern` VARCHAR(48) NOT NULL DEFAULT 'float_drift',
  `image_url` VARCHAR(512) DEFAULT NULL,
  `metadata_json` JSON DEFAULT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`asset_id`),
  UNIQUE KEY `uk_world_asset_key` (`asset_key`),
  KEY `idx_world_assets_world` (`world_slug`, `is_active`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_occasions` (
  `occasion_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slug` VARCHAR(100) NOT NULL,
  `name` VARCHAR(140) NOT NULL,
  `eyebrow` VARCHAR(80) DEFAULT NULL,
  `headline` VARCHAR(200) NOT NULL,
  `subtitle` VARCHAR(320) DEFAULT NULL,
  `occasion_type` VARCHAR(40) NOT NULL DEFAULT 'festival',
  `motif_key` VARCHAR(48) NOT NULL DEFAULT 'confetti',
  `primary_color` VARCHAR(16) NOT NULL DEFAULT '#D4AF37',
  `accent_color` VARCHAR(16) NOT NULL DEFAULT '#FFFFFF',
  `background_color` VARCHAR(16) NOT NULL DEFAULT '#140F1C',
  `font_style` VARCHAR(48) NOT NULL DEFAULT 'editorial_serif',
  `audio_track_key` VARCHAR(64) NOT NULL DEFAULT 'festive',
  `cta_text` VARCHAR(80) NOT NULL DEFAULT 'Explore Occasion',
  `cta_collection_slug` VARCHAR(120) DEFAULT NULL,
  `banner_image_url` VARCHAR(512) DEFAULT NULL,
  `region_country` VARCHAR(64) DEFAULT 'ALL',
  `region_state` VARCHAR(80) DEFAULT NULL,
  `region_city` VARCHAR(80) DEFAULT NULL,
  `priority` INT NOT NULL DEFAULT 50,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `start_date` DATETIME DEFAULT NULL,
  `end_date` DATETIME DEFAULT NULL,
  `theme_config` JSON DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`occasion_id`),
  UNIQUE KEY `uk_app_occasions_slug` (`slug`),
  KEY `idx_occasions_schedule` (`is_active`, `priority`, `start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. Product Videos & Engagement Events
-- ----------------------------------------------------------------------------
CREATE TABLE `app_product_videos` (
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

CREATE TABLE `app_video_events` (
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

-- ----------------------------------------------------------------------------
-- 6. Melvore Luxe / Elite Showroom Packs & Items
-- ----------------------------------------------------------------------------
CREATE TABLE `app_elite_packs` (
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

CREATE TABLE `app_elite_pack_items` (
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

-- ----------------------------------------------------------------------------
-- 7. Personalization, Search Synonyms, Search Logs & Tags
-- ----------------------------------------------------------------------------
CREATE TABLE `app_user_style_signals` (
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
  KEY `idx_style_signals_guest` (`guest_id`, `created_at`),
  KEY `idx_style_signals_actor` (`user_id`, `guest_id`, `style_world_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_search_synonyms` (
  `synonym_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `term` VARCHAR(100) NOT NULL,
  `synonyms` VARCHAR(320) NOT NULL,
  `expanded_terms` VARCHAR(320) NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`synonym_id`),
  UNIQUE KEY `uk_search_synonyms_term` (`term`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_search_logs` (
  `log_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `query_text` VARCHAR(180) NOT NULL,
  `normalized_query` VARCHAR(180) NOT NULL DEFAULT '',
  `results_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_search_logs_query` (`query_text`, `created_at`),
  KEY `idx_search_logs_recent_norm` (`created_at`, `normalized_query`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_tags` (
  `tag_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slug` VARCHAR(80) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `tag_type` VARCHAR(32) NOT NULL DEFAULT 'style',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_app_tags_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_product_tag_map` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id` INT NOT NULL,
  `tag_id` INT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_tag` (`product_id`, `tag_id`),
  KEY `idx_tag_lookup` (`tag_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. Smart Contextual Offers & Offer Impressions
-- ----------------------------------------------------------------------------
CREATE TABLE `app_smart_offers` (
  `offer_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `offer_key` VARCHAR(80) NOT NULL,
  `title` VARCHAR(160) NOT NULL,
  `subtitle` VARCHAR(280) DEFAULT NULL,
  `badge_text` VARCHAR(48) DEFAULT 'OFFER',
  `coupon_code` VARCHAR(64) DEFAULT NULL,
  `world_slug` VARCHAR(100) DEFAULT NULL,
  `occasion_slug` VARCHAR(100) DEFAULT NULL,
  `min_cart_value` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `max_impressions_per_session` INT NOT NULL DEFAULT 1,
  `cooldown_hours` INT NOT NULL DEFAULT 24,
  `priority` INT NOT NULL DEFAULT 10,
  `accent_hex` VARCHAR(16) NOT NULL DEFAULT '#3B82F6',
  `cta_text` VARCHAR(64) NOT NULL DEFAULT 'Apply Offer',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `start_date` DATETIME DEFAULT NULL,
  `end_date` DATETIME DEFAULT NULL,
  PRIMARY KEY (`offer_id`),
  UNIQUE KEY `uk_smart_offer_key` (`offer_key`),
  KEY `idx_smart_offers_active` (`is_active`, `priority`, `start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_offer_impressions` (
  `impression_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `offer_id` INT UNSIGNED NOT NULL,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `action_type` VARCHAR(32) NOT NULL DEFAULT 'shown',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`impression_id`),
  KEY `idx_offer_imp_user` (`offer_id`, `user_id`, `action_type`, `created_at`),
  KEY `idx_offer_imp_guest` (`offer_id`, `guest_id`, `action_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 9. Companion Non-Destructive Tables (ensure existence without wiping user state)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `app_user_preferences` (
  `pref_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `ambient_audio_enabled` TINYINT(1) NOT NULL DEFAULT 0,
  `ui_sfx_enabled` TINYINT(1) NOT NULL DEFAULT 0,
  `reduced_motion` TINYINT(1) NOT NULL DEFAULT 0,
  `audio_consent_answered` TINYINT(1) NOT NULL DEFAULT 0,
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

SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================================
-- SECTION 2: DYNAMIC SEEDING FROM LIVE `products`, `brands`, `categories`, `coupons`
-- ============================================================================

-- Helper variables holding real active product images at distinct offsets so
-- banners/worlds receive varied, authentic product photography.
SET @img_hero_1 = (SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1 OFFSET 0);
SET @img_hero_2 = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1 OFFSET 1), @img_hero_1);
SET @img_hero_3 = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1 OFFSET 2), @img_hero_1);
SET @img_hero_4 = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.purchase_count DESC, p.product_id DESC LIMIT 1 OFFSET 0), @img_hero_1);
SET @img_hero_5 = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.view_count DESC, p.product_id DESC LIMIT 1 OFFSET 0), @img_hero_2);
SET @img_lux_1  = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.product_id DESC LIMIT 1 OFFSET 0), @img_hero_1);
SET @img_lux_2  = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.product_id DESC LIMIT 1 OFFSET 1), @img_lux_1);
SET @img_lux_3  = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.product_id DESC LIMIT 1 OFFSET 2), @img_lux_1);
SET @img_lux_4  = COALESCE((SELECT p.img_src FROM `products` p WHERE p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY COALESCE(NULLIF(p.sale_price, 0), p.regular_price) DESC, p.product_id DESC LIMIT 1 OFFSET 3), @img_lux_1);

-- ----------------------------------------------------------------------------
-- 1. Seed `app_campaigns`, `app_campaign_regions`, `app_campaign_assets`
-- ----------------------------------------------------------------------------
INSERT INTO `app_campaigns`
  (`slug`, `title`, `subtitle`, `festival_key`, `animation_key`, `animation_duration_ms`,
   `primary_color`, `accent_color`, `background_color`, `banner_image_url`, `mobile_banner_url`,
   `cta_text`, `cta_link`, `priority`, `is_active`, `theme_config`)
VALUES
  (
    'winter-leather-edit',
    'The Winter & Leather Edit',
    'Statement jackets, structured layering, and cold-weather icons crafted for crisp nights.',
    'winter',
    'smooth_reveal',
    2400,
    '#09090B',
    '#D4AF37',
    '#FAF8F5',
    @img_hero_1,
    @img_hero_1,
    'Explore Winter Edit',
    'jackets',
    100,
    1,
    '{"mode":"light","surface_color":"#FFFFFF","accent_color":"#D4AF37","badge":"SEASONAL DROP"}'
  ),
  (
    'seoul-street-dispatch',
    'Seoul Street & Y2K Capsule',
    'Proportion-forward outerwear, chrome details, and nocturnal street tailoring.',
    'editorial',
    'cyber_sparkle',
    2200,
    '#FFFFFF',
    '#FF2EA6',
    '#0E0D1B',
    @img_hero_2,
    @img_hero_2,
    'Enter Style Edit',
    'y2k',
    75,
    1,
    '{"mode":"dark","surface_color":"#181632","accent_color":"#FF2EA6","badge":"WORLD DISPATCH"}'
  );

-- Campaign Region Rules (Nationwide India + Metro Highlights)
INSERT INTO `app_campaign_regions` (`campaign_id`, `country`, `state`, `city`, `pincode`, `is_nationwide`)
SELECT c.campaign_id, 'India', NULL, NULL, NULL, 1
FROM `app_campaigns` c
WHERE c.slug = 'winter-leather-edit';

INSERT INTO `app_campaign_regions` (`campaign_id`, `country`, `state`, `city`, `pincode`, `is_nationwide`)
SELECT c.campaign_id, 'India', 'Delhi', 'New Delhi', NULL, 0
FROM `app_campaigns` c
WHERE c.slug = 'seoul-street-dispatch';

INSERT INTO `app_campaign_regions` (`campaign_id`, `country`, `state`, `city`, `pincode`, `is_nationwide`)
SELECT c.campaign_id, 'India', 'Maharashtra', 'Mumbai', NULL, 0
FROM `app_campaigns` c
WHERE c.slug = 'seoul-street-dispatch';

-- Campaign Assets (top 4 featured active products per campaign)
INSERT INTO `app_campaign_assets`
  (`campaign_id`, `asset_type`, `media_url`, `mobile_media_url`, `animation_key`, `title`, `subtitle`, `display_order`)
SELECT c.campaign_id,
       'image',
       p.img_src,
       p.img_src,
       CASE WHEN c.slug = 'seoul-street-dispatch' THEN 'cyber_sparkle' ELSE 'smooth_reveal' END,
       p.name,
       COALESCE(b.name, 'Melvore Atelier'),
       p.product_id
FROM `app_campaigns` c
JOIN (
  SELECT product_id, brand_id, name, img_src
  FROM `products`
  WHERE is_active = 1 AND TRIM(COALESCE(img_src, '')) <> ''
  ORDER BY is_featured DESC, purchase_count DESC, product_id DESC
  LIMIT 4
) p
LEFT JOIN `brands` b ON b.brand_id = p.brand_id;

-- ----------------------------------------------------------------------------
-- 2. Seed `app_home_sections`, `app_home_section_items`, `app_product_placements`
-- ----------------------------------------------------------------------------
INSERT INTO `app_home_sections`
  (`section_key`, `section_type`, `eyebrow`, `title`, `subtitle`, `animation_key`, `theme_key`,
   `theme_config`, `cta_text`, `cta_link`, `item_limit`, `display_order`, `is_active`)
VALUES
  ('winter_collection', 'editorial_campaign', 'SEASONAL STORY', 'The Winter Collection', 'Heavyweight outer layers, brushed textures, and cold-season tailoring.', 'smooth_reveal', 'light_editorial', '{"surface":"#FFFFFF","accent":"#09090B"}', 'Shop Winter', 'jackets', 8, 10, 1),
  ('leather_edit', 'editorial_campaign', 'SIGNATURE CRAFT', 'The Leather Edit', 'Precision biker cuts, cropped silhouettes, and vintage-washed finishes.', 'smooth_reveal', 'light_editorial', '{"surface":"#FAF8F5","accent":"#B48811"}', 'Shop Leather', 'jackets', 8, 20, 1),
  ('streetwear_stories', 'product_shelf', 'URBAN ARCHIVE', 'Streetwear Stories', 'Oversized graphic tees, wide-leg cargos, and off-duty staples.', 'kinetic_slide', 'streetwear', '{"surface":"#111215","accent":"#F97316"}', 'Shop Streetwear', 'streetwear', 8, 30, 1),
  ('korean_fashion', 'editorial_campaign', 'SEOUL DISPATCH', 'Korean Fashion Edit', 'Clean lines, soft neutral layering, and effortless K-fashion proportions.', 'smooth_reveal', 'minimal_editorial', '{"surface":"#F5F6FA","accent":"#4F6D8A"}', 'Explore Seoul Edit', 'k-fashion', 8, 40, 1),
  ('weekend_wardrobe', 'product_shelf', 'OFF-DUTY', 'The Weekend Wardrobe', 'Co-ord sets, relaxed shirting, and easy silhouettes from morning to dusk.', 'smooth_reveal', 'warm_ivory', '{"surface":"#F7F4EF","accent":"#1C1917"}', 'Shop Co-ords', 'co-ords', 8, 50, 1),
  ('statement_jackets', 'product_grid', 'OUTERWEAR ICONS', 'Statement Jackets', 'Studded, cropped, hybrid, and Moto silhouettes engineered to stand out.', 'smooth_reveal', 'light_editorial', '{"surface":"#FFFFFF","accent":"#09090B"}', 'View Outerwear', 'jackets', 8, 60, 1),
  ('after_dark', 'editorial_campaign', 'NIGHT OUT', 'After Dark', 'Sleek monochrome dressing and evening-ready statement pieces.', 'metallic_shine', 'dark_accent', '{"surface":"#08070B","accent":"#C02638"}', 'Shop Evening', 'goth', 8, 70, 1),
  ('trending_now', 'product_shelf', 'MOST VIEWED', 'Trending Across Melvore', 'High-heat pieces leading bags and wishlists right now.', 'smooth_reveal', 'light_editorial', '{"surface":"#FFFFFF","accent":"#3B82F6"}', 'Shop Trending', 'trending', 8, 75, 1),
  ('everyday_essentials', 'product_shelf', 'CAPSULE CORE', 'Everyday Essentials', 'Foundational tees, crisp shirting, and daily denim built to repeat.', 'smooth_reveal', 'light_editorial', '{"surface":"#FFFFFF","accent":"#09090B"}', 'Shop Essentials', 'all', 10, 80, 1),
  ('minimal_style', 'product_shelf', 'QUIET TAILORING', 'Minimal Style', 'Uncluttered palettes, tonal textures, and refined everyday cuts.', 'smooth_reveal', 'minimal_editorial', '{"surface":"#FAFAFA","accent":"#52525B"}', 'Shop Minimal', 'minimal', 8, 90, 1);

-- Populate editorial highlight tiles inside `app_home_section_items` from real active categories & product photography
INSERT INTO `app_home_section_items`
  (`section_id`, `title`, `subtitle`, `badge_text`, `image_url`, `mobile_image_url`, `theme_key`, `animation_key`, `target_type`, `target_value`, `target_id`, `display_order`, `is_active`)
SELECT s.section_id,
       c.name,
       COALESCE(NULLIF(c.description, ''), CONCAT('Curated ', c.name, ' edit from active studio labels')),
       'CURATED',
       COALESCE(NULLIF(c.image, ''), (SELECT p.img_src FROM `products` p WHERE p.category_id = c.category_id AND p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1), @img_hero_1),
       COALESCE(NULLIF(c.image, ''), (SELECT p.img_src FROM `products` p WHERE p.category_id = c.category_id AND p.is_active = 1 AND TRIM(COALESCE(p.img_src, '')) <> '' ORDER BY p.is_featured DESC, p.product_id DESC LIMIT 1), @img_hero_1),
       s.theme_key,
       s.animation_key,
       'category',
       c.slug,
       c.category_id,
       COALESCE(c.display_order, c.category_id),
       1
FROM `app_home_sections` s
JOIN (
  SELECT category_id, name, slug, description, image, display_order
  FROM `categories`
  WHERE status = 'active'
  ORDER BY is_featured DESC, display_order ASC, category_id ASC
  LIMIT 4
) c
WHERE s.section_key IN ('winter_collection', 'leather_edit', 'korean_fashion', 'after_dark');

-- Populate `app_product_placements` for sponsored showcase + curated shelves
INSERT IGNORE INTO `app_product_placements`
  (`product_id`, `section_key`, `is_sponsored`, `badge_override`, `express_delivery`, `priority`, `display_order`, `is_active`)
SELECT p.product_id,
       'featured',
       CASE WHEN p.is_featured = 1 THEN 1 ELSE 0 END,
       CASE WHEN p.is_featured = 1 THEN 'LUXE PICK' ELSE NULL END,
       1,
       CASE WHEN p.is_featured = 1 THEN 100 ELSE 50 END,
       p.product_id,
       1
FROM `products` p
WHERE p.is_active = 1
ORDER BY p.is_featured DESC, p.purchase_count DESC, p.product_id DESC
LIMIT 12;

INSERT IGNORE INTO `app_product_placements`
  (`product_id`, `section_key`, `is_sponsored`, `badge_override`, `express_delivery`, `priority`, `display_order`, `is_active`)
SELECT p.product_id,
       'winter_collection',
       0,
       'WINTER EDIT',
       1,
       80,
       p.product_id,
       1
FROM `products` p
WHERE p.is_active = 1
  AND (LOWER(p.name) LIKE '%jacket%' OR LOWER(p.name) LIKE '%coat%' OR LOWER(p.name) LIKE '%hoodie%' OR LOWER(p.name) LIKE '%leather%' OR p.is_featured = 1)
ORDER BY p.is_featured DESC, p.product_id DESC
LIMIT 8;

INSERT IGNORE INTO `app_product_placements`
  (`product_id`, `section_key`, `is_sponsored`, `badge_override`, `express_delivery`, `priority`, `display_order`, `is_active`)
SELECT p.product_id,
       'statement_jackets',
       0,
       'OUTERWEAR',
       0,
       75,
       p.product_id,
       1
FROM `products` p
WHERE p.is_active = 1
  AND (LOWER(p.name) LIKE '%jacket%' OR LOWER(p.name) LIKE '%biker%' OR LOWER(p.name) LIKE '%blazer%' OR p.is_featured = 1)
ORDER BY p.rating_average DESC, p.product_id DESC
LIMIT 8;

-- ----------------------------------------------------------------------------
-- 3. Seed `app_brand_profiles`, `app_brand_origins`, `app_user_brand_activity`
-- ----------------------------------------------------------------------------
INSERT INTO `app_brand_profiles`
  (`brand_id`, `tagline`, `story`, `editorial_cover_url`, `mobile_cover_url`, `origin_country`, `theme_config`,
   `is_featured`, `is_sponsored`, `is_new`, `trending_score`, `display_order`)
SELECT b.brand_id,
       CONCAT('Curated ', b.name, ' seasonal edit & signature silhouettes.'),
       COALESCE(NULLIF(b.description, ''), CONCAT(b.name, ' crafts precision-cut contemporary garments designed for modern layering and enduring wear.')),
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
       1,
       CASE WHEN b.brand_id % 4 = 1 THEN 1 ELSE 0 END,
       CASE WHEN b.brand_id % 3 = 0 THEN 1 ELSE 0 END,
       GREATEST(70, 100 - (b.brand_id * 2)),
       b.brand_id
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

-- Seed editorial baseline brand activity under a dedicated seed guest ID
INSERT INTO `app_user_brand_activity` (`user_id`, `guest_id`, `brand_id`, `activity_type`, `last_viewed_at`, `created_at`)
SELECT NULL, 'melvore-editorial-seed', b.brand_id, 'view', NOW(), NOW()
FROM `brands` b
WHERE b.status = 'active'
ORDER BY b.brand_id ASC
LIMIT 5;

-- ----------------------------------------------------------------------------
-- 4. Seed All 11 Fashion Worlds (`app_style_worlds`) + Products, Brands, Assets & Occasions
-- ----------------------------------------------------------------------------
INSERT INTO `app_style_worlds` (
  `slug`, `name`, `tagline`, `description`, `theme_key`, `animation_key`, `font_style`,
  `ambient_preset`, `audio_track_key`, `primary_color`, `accent_color`, `background_color`,
  `cover_image_url`, `mobile_image_url`, `display_order`, `version_tag`, `is_active`, `theme_config`
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
  @img_hero_1,
  @img_hero_1,
  1,
  '2.0',
  1,
  '{"mode":"dark","surface_color":"#181632","surface_elevated":"#232046","secondary_accent":"#38BDF8","text_primary":"#FFFFFF","text_secondary":"#D8D6F2","border_color":"#47427A","button_style":"chrome_bevel","card_radius_dp":18,"border_width_dp":1,"sfx_profile":"y2k","hero_layout":"cyber_split"}'
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
  @img_hero_2,
  @img_hero_2,
  2,
  '2.0',
  1,
  '{"mode":"dark","surface_color":"#121018","surface_elevated":"#1B1824","secondary_accent":"#A1A1AA","text_primary":"#F4F4F5","text_secondary":"#C4C2CF","border_color":"#342E42","button_style":"goth_sharp","card_radius_dp":4,"border_width_dp":1,"sfx_profile":"goth","hero_layout":"noir_monolith"}'
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
  @img_hero_3,
  @img_hero_3,
  3,
  '2.0',
  1,
  '{"mode":"light","surface_color":"#FFFFFF","surface_elevated":"#EDF0F7","secondary_accent":"#94A3B8","text_primary":"#141519","text_secondary":"#525866","border_color":"#D6DCE8","button_style":"atelier_outline","card_radius_dp":10,"border_width_dp":1,"sfx_profile":"kfashion","hero_layout":"seoul_grid"}'
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
  @img_hero_4,
  @img_hero_4,
  4,
  '2.0',
  1,
  '{"mode":"light","surface_color":"#FFFFFF","surface_elevated":"#FDECF2","secondary_accent":"#E8A5BA","text_primary":"#3B1824","text_secondary":"#7D4E5E","border_color":"#F5CCD9","button_style":"ribbon_pill","card_radius_dp":24,"border_width_dp":1,"sfx_profile":"coquette","hero_layout":"romantic_frame"}'
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
  @img_hero_5,
  @img_hero_5,
  5,
  '2.0',
  1,
  '{"mode":"dark","surface_color":"#191B20","surface_elevated":"#23262E","secondary_accent":"#38BDF8","text_primary":"#FFFFFF","text_secondary":"#D1D5DB","border_color":"#323640","button_style":"street_block","card_radius_dp":6,"border_width_dp":2,"sfx_profile":"streetwear","hero_layout":"utility_poster"}'
),
(
  'quiet-luxury',
  'Quiet Luxury',
  'UNBRANDED FORM & TACTILE WEAVES',
  'Restrained camel, ivory, and charcoal pieces where fabric and tailoring speak first.',
  'quiet_luxury',
  'smooth_reveal',
  'editorial_serif',
  'none',
  'luxe_ambient',
  '#1C1917',
  '#B48811',
  '#F7F4EF',
  @img_lux_1,
  @img_lux_1,
  6,
  '2.0',
  1,
  '{"mode":"light","surface_color":"#FFFFFF","surface_elevated":"#EFECE6","secondary_accent":"#78716C","text_primary":"#1C1917","text_secondary":"#57534E","border_color":"#E7E5E4","button_style":"editorial_pill","card_radius_dp":12,"border_width_dp":1,"sfx_profile":"default","hero_layout":"asymmetrical_split"}'
),
(
  'minimal',
  'Minimal Studio',
  'MONOCHROME CLARITY & PURE STRUCTURE',
  'Pared-back wardrobe anchors engineered for daily versatility and clean silhouettes.',
  'minimal',
  'smooth_reveal',
  'clean_sans',
  'none',
  'minimal_pulse',
  '#09090B',
  '#52525B',
  '#FAFAFA',
  @img_lux_2,
  @img_lux_2,
  7,
  '2.0',
  1,
  '{"mode":"light","surface_color":"#FFFFFF","surface_elevated":"#F4F4F5","secondary_accent":"#A1A1AA","text_primary":"#09090B","text_secondary":"#52525B","border_color":"#E4E4E7","button_style":"sharp_minimal","card_radius_dp":10,"border_width_dp":1,"sfx_profile":"default","hero_layout":"seoul_grid"}'
),
(
  'grunge',
  'Grunge Archive',
  'DISTRESSED WASHES & RAW MOTO EDGE',
  'Washed black denim, oversized flannels, and worn-in leather attitude.',
  'grunge',
  'nocturne_mist',
  'futuristic',
  'street_grid',
  'goth_drone',
  '#F4F4F5',
  '#D97706',
  '#181512',
  @img_lux_3,
  @img_lux_3,
  8,
  '2.0',
  1,
  '{"mode":"dark","surface_color":"#221E1A","surface_elevated":"#2C2621","secondary_accent":"#A8A29E","text_primary":"#F5F5F4","text_secondary":"#D6D3D1","border_color":"#44403C","button_style":"street_block","card_radius_dp":6,"border_width_dp":1,"sfx_profile":"streetwear","hero_layout":"utility_poster"}'
),
(
  'old-money',
  'Old Money',
  'HERITAGE KNITS & CLUB CLASSICS',
  'Timeless tailoring, structured blazers, polo collars, and heirloom-inspired neutral palettes.',
  'old_money',
  'smooth_reveal',
  'editorial_serif',
  'none',
  'luxe_ambient',
  '#14261C',
  '#B48811',
  '#F4F1EA',
  @img_lux_4,
  @img_lux_4,
  9,
  '2.0',
  1,
  '{"mode":"light","surface_color":"#FFFFFF","surface_elevated":"#EBE6DC","secondary_accent":"#3F624D","text_primary":"#14261C","text_secondary":"#4A5D52","border_color":"#DDD6C8","button_style":"atelier_outline","card_radius_dp":12,"border_width_dp":1,"sfx_profile":"default","hero_layout":"editorial_frame"}'
),
(
  'casual',
  'Everyday Casual',
  'RELAXED STAPLES & EFFORTLESS COMFORT',
  'Breathable cottons, clean tees, and weekend-ready fits built for everyday rotation.',
  'casual',
  'smooth_reveal',
  'clean_sans',
  'none',
  'none',
  '#0F172A',
  '#059669',
  '#F8FAFC',
  @img_hero_4,
  @img_hero_4,
  10,
  '2.0',
  1,
  '{"mode":"light","surface_color":"#FFFFFF","surface_elevated":"#F1F5F9","secondary_accent":"#64748B","text_primary":"#0F172A","text_secondary":"#475569","border_color":"#E2E8F0","button_style":"editorial_pill","card_radius_dp":14,"border_width_dp":1,"sfx_profile":"default","hero_layout":"asymmetrical_split"}'
),
(
  'party-wear',
  'Party Wear & Luxe Night',
  'MIDNIGHT SHIMMER & SHARP TAILORING',
  'Statement dresses, metallic hardware accents, and after-hours silhouettes built for entrances.',
  'party_wear',
  'metallic_shine',
  'editorial_serif',
  'goth_nocturne',
  'luxe_ambient',
  '#FFFFFF',
  '#D4AF37',
  '#0D0A14',
  @img_lux_1,
  @img_lux_1,
  11,
  '2.0',
  1,
  '{"mode":"dark","surface_color":"#161122","surface_elevated":"#211A33","secondary_accent":"#E5E7EB","text_primary":"#FFFFFF","text_secondary":"#D4D4D8","border_color":"#352B4D","button_style":"chrome_bevel","card_radius_dp":16,"border_width_dp":1,"sfx_profile":"y2k","hero_layout":"noir_monolith"}'
);

-- Associate Real Active Products to Each Fashion World (Keyword Match + Guarantee Fallback so no world is empty)
INSERT IGNORE INTO `app_style_world_products` (`style_world_id`, `product_id`, `display_order`)
SELECT sw.style_world_id, p.product_id, p.product_id
FROM `app_style_worlds` sw
JOIN `products` p ON p.is_active = 1
WHERE (
  (sw.slug = 'y2k' AND (LOWER(p.name) LIKE '%crop%' OR LOWER(p.name) LIKE '%baby%' OR LOWER(p.name) LIKE '%denim%' OR LOWER(p.name) LIKE '%zip%' OR LOWER(p.name) LIKE '%studded%' OR p.is_featured = 1))
  OR (sw.slug = 'goth' AND (LOWER(p.name) LIKE '%black%' OR LOWER(p.name) LIKE '%leather%' OR LOWER(p.name) LIKE '%jacket%' OR LOWER(p.name) LIKE '%biker%' OR LOWER(p.available_colors) LIKE '%black%'))
  OR (sw.slug = 'k-fashion' AND (LOWER(p.name) LIKE '%blazer%' OR LOWER(p.name) LIKE '%shirt%' OR LOWER(p.name) LIKE '%coat%' OR LOWER(p.name) LIKE '%cardigan%' OR p.is_featured = 1))
  OR (sw.slug = 'coquette' AND (LOWER(p.name) LIKE '%dress%' OR LOWER(p.name) LIKE '%bow%' OR LOWER(p.name) LIKE '%pink%' OR LOWER(p.name) LIKE '%knit%' OR LOWER(p.name) LIKE '%top%'))
  OR (sw.slug = 'streetwear' AND (LOWER(p.name) LIKE '%hoodie%' OR LOWER(p.name) LIKE '%cargo%' OR LOWER(p.name) LIKE '%oversized%' OR LOWER(p.name) LIKE '%tee%' OR LOWER(p.name) LIKE '%bomber%'))
  OR (sw.slug = 'grunge' AND (LOWER(p.name) LIKE '%jacket%' OR LOWER(p.name) LIKE '%leather%' OR LOWER(p.name) LIKE '%washed%' OR LOWER(p.name) LIKE '%denim%'))
  OR (sw.slug IN ('quiet-luxury', 'minimal', 'old-money', 'casual', 'party-wear'))
);

-- Fallback backfill: ensure every active style world has at least up to 10 active products even if keyword filters matched fewer
INSERT IGNORE INTO `app_style_world_products` (`style_world_id`, `product_id`, `display_order`)
SELECT sw.style_world_id, top_p.product_id, top_p.product_id + 10000
FROM `app_style_worlds` sw
LEFT JOIN (
  SELECT style_world_id, COUNT(*) AS cnt
  FROM `app_style_world_products`
  GROUP BY style_world_id
) cnt_map ON cnt_map.style_world_id = sw.style_world_id
JOIN (
  SELECT product_id
  FROM `products`
  WHERE is_active = 1
  ORDER BY is_featured DESC, purchase_count DESC, product_id DESC
  LIMIT 10
) top_p
WHERE COALESCE(cnt_map.cnt, 0) < 4;

-- Associate Active Brands with Style Worlds (`app_world_brands`)
INSERT IGNORE INTO `app_world_brands` (`style_world_id`, `brand_id`, `display_order`)
SELECT DISTINCT sw.style_world_id, p.brand_id, p.brand_id
FROM `app_style_world_products` swp
INNER JOIN `app_style_worlds` sw ON sw.style_world_id = swp.style_world_id
INNER JOIN `products` p ON p.product_id = swp.product_id
INNER JOIN `brands` b ON b.brand_id = p.brand_id AND b.status = 'active'
WHERE p.brand_id IS NOT NULL;

-- Vector Decorative Assets (`app_world_assets`)
INSERT INTO `app_world_assets`
  (`asset_key`, `world_slug`, `occasion_slug`, `vector_type`, `layer_role`, `primary_hex`, `secondary_hex`, `opacity`, `motion_pattern`, `metadata_json`, `display_order`, `is_active`)
VALUES
  ('y2k_pixel_heart', 'y2k', NULL, 'pixel_heart', 'ambient_floating', '#FF2EA6', '#38BDF8', 0.42, 'float_drift', '{"size_dp":22,"count":6}', 1, 1),
  ('y2k_chrome_sparkle', 'y2k', NULL, 'chrome_sparkle', 'corner_motif', '#E2E8F0', '#FF2EA6', 0.55, 'pulse_twinkle', '{"size_dp":26,"count":4}', 2, 1),
  ('y2k_pixel_star', 'y2k', NULL, 'pixel_star', 'ambient_floating', '#38BDF8', '#FFFFFF', 0.36, 'orbit_spin', '{"size_dp":18,"count":8}', 3, 1),
  ('goth_raven', 'goth', NULL, 'goth_raven', 'corner_motif', '#E4E4E7', '#C02638', 0.40, 'glide_slow', '{"size_dp":34,"count":2}', 1, 1),
  ('goth_bat', 'goth', NULL, 'goth_bat', 'ambient_floating', '#D4D4D8', '#7F1D1D', 0.35, 'float_drift', '{"size_dp":24,"count":5}', 2, 1),
  ('goth_crescent', 'goth', NULL, 'crescent_moon', 'header_watermark', '#F4F4F5', '#9CA3AF', 0.30, 'pulse_twinkle', '{"size_dp":42,"count":1}', 3, 1),
  ('goth_mist', 'goth', NULL, 'gothic_mist', 'background_atmosphere', '#4C1D95', '#C02638', 0.22, 'mist_sweep', '{"size_dp":120,"count":2}', 4, 1),
  ('coquette_bow', 'coquette', NULL, 'ribbon_bow', 'corner_motif', '#D96588', '#FBCFE8', 0.50, 'sway_gentle', '{"size_dp":28,"count":3}', 1, 1),
  ('coquette_petal', 'coquette', NULL, 'flower_petal', 'ambient_floating', '#F472B6', '#FDA4AF', 0.42, 'petal_fall', '{"size_dp":16,"count":8}', 2, 1),
  ('street_lightning', 'streetwear', NULL, 'street_bolt', 'corner_motif', '#F97316', '#38BDF8', 0.45, 'kinetic_pulse', '{"size_dp":24,"count":4}', 1, 1),
  ('street_grid', 'streetwear', NULL, 'editorial_grid', 'background_atmosphere', '#334155', '#F97316', 0.24, 'static_clean', '{"size_dp":96,"count":1}', 2, 1),
  ('kfashion_origami', 'k-fashion', NULL, 'atelier_frame', 'corner_motif', '#4F6D8A', '#94A3B8', 0.32, 'sway_gentle', '{"size_dp":30,"count":2}', 1, 1),
  ('occ_diwali_diya', 'occasion', 'diwali-luxe-edit', 'diya_flame', 'corner_motif', '#F59E0B', '#FDE68A', 0.58, 'flicker_warm', '{"size_dp":26,"count":4}', 1, 1),
  ('occ_winter_snow', 'occasion', 'winter-atelier', 'snowflake', 'ambient_floating', '#E0F2FE', '#93C5FD', 0.45, 'petal_fall', '{"size_dp":18,"count":10}', 1, 1),
  ('occ_ny_firework', 'occasion', 'new-year-countdown', 'firework', 'ambient_floating', '#FBBF24', '#F472B6', 0.48, 'pulse_twinkle', '{"size_dp":28,"count":5}', 1, 1),
  ('occ_sale_sticker', 'occasion', 'end-of-season-edit', 'sale_sticker', 'corner_motif', '#EF4444', '#FFFFFF', 0.50, 'kinetic_pulse', '{"size_dp":24,"count":3}', 1, 1);

-- Occasions (`app_occasions`)
INSERT INTO `app_occasions` (
  `slug`, `name`, `eyebrow`, `headline`, `subtitle`, `occasion_type`, `motif_key`,
  `primary_color`, `accent_color`, `background_color`, `font_style`, `audio_track_key`,
  `cta_text`, `cta_collection_slug`, `banner_image_url`, `region_country`, `priority`, `is_active`,
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
  @img_hero_1,
  'ALL',
  40,
  1,
  NULL,
  NULL,
  '{"mode":"dark","surface_color":"#1D1A24","card_radius_dp":16}'
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
  @img_lux_1,
  'India',
  90,
  0,
  '2026-10-15 00:00:00',
  '2026-11-15 23:59:59',
  '{"mode":"dark","surface_color":"#2B190C","card_radius_dp":18}'
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
  'jackets',
  @img_hero_2,
  'ALL',
  70,
  0,
  '2026-12-01 00:00:00',
  '2027-02-28 23:59:59',
  '{"mode":"light","surface_color":"#FFFFFF","card_radius_dp":14}'
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
  @img_hero_5,
  'ALL',
  80,
  0,
  NULL,
  NULL,
  '{"mode":"dark","surface_color":"#24171B","card_radius_dp":10}'
);

-- ----------------------------------------------------------------------------
-- 5. Seed `app_product_videos` & `app_video_events` ONLY from real active videos
--    (Zero broken/fake .mp4 URLs. Syncs automatically from `app_product_media`
--     if any rows have `media_type = 'video'`.)
-- ----------------------------------------------------------------------------
INSERT INTO `app_product_videos`
  (`product_id`, `brand_id`, `style_world_id`, `title`, `caption`, `creator_name`, `creator_handle`,
   `video_url`, `poster_url`, `thumbnail_url`, `duration_seconds`, `aspect_ratio`,
   `is_sponsored`, `is_featured`, `display_order`, `is_active`)
SELECT pm.product_id,
       p.brand_id,
       (SELECT swp.style_world_id FROM `app_style_world_products` swp WHERE swp.product_id = p.product_id LIMIT 1),
       CONCAT(p.name, ' — Studio Motion Look'),
       COALESCE(NULLIF(pm.alt_text, ''), 'Shot in 9:16 vertical motion. Tap Shop This Look for sizes and instant bag add.'),
       COALESCE(b.name, 'Melvore Studio'),
       '@melvore.official',
       pm.media_url,
       COALESCE(NULLIF(pm.thumbnail_url, ''), p.img_src),
       COALESCE(NULLIF(pm.thumbnail_url, ''), p.img_src),
       18,
       '9:16',
       CASE WHEN p.is_featured = 1 THEN 1 ELSE 0 END,
       1,
       pm.display_order,
       1
FROM `app_product_media` pm
INNER JOIN `products` p ON p.product_id = pm.product_id AND p.is_active = 1
LEFT JOIN `brands` b ON b.brand_id = p.brand_id
WHERE pm.media_type = 'video'
  AND pm.is_active = 1
  AND TRIM(COALESCE(pm.media_url, '')) <> '';

INSERT INTO `app_video_events` (`video_id`, `user_id`, `guest_id`, `event_type`, `watch_seconds`, `created_at`)
SELECT v.video_id, NULL, 'melvore-editorial-seed', 'view', 14, NOW()
FROM `app_product_videos` v
WHERE v.is_active = 1;

-- ----------------------------------------------------------------------------
-- 6. Seed `app_elite_packs` & `app_elite_pack_items` (Melvore Luxe Showroom)
-- ----------------------------------------------------------------------------
INSERT INTO `app_elite_packs`
  (`slug`, `section_key`, `brand_name`, `eyebrow`, `title`, `subtitle`, `story`, `cta_text`, `cta_link`,
   `hero_image_url`, `mobile_image_url`, `animation_key`, `accent_color`, `background_color`,
   `is_hero`, `is_sponsored`, `display_order`, `is_active`, `theme_config`)
VALUES
  (
    'the-luxe-edit',
    'the_luxe_edit',
    'MELVORE LUXE',
    'PRIVATE SHOWROOM · AUTUMN / WINTER',
    'THE LUXE EDIT',
    'Architectural tailoring, collector leatherwork, and statement silhouettes in obsidian & gold.',
    'Drawn from the highest tier of the Melvore vault, The Luxe Edit unites structured outerwear, custom hardware, and limited-run evening pieces.',
    'Enter Collection',
    'elite/pack/the-luxe-edit',
    @img_lux_1,
    @img_lux_1,
    'metallic_shine',
    '#D4AF37',
    '#000000',
    1,
    0,
    1,
    1,
    '{"layout":"hero_cinematic","mode":"dark"}'
  ),
  (
    'leather-atelier',
    'leather_atelier',
    'ATELIER NOIR',
    'CRAFT & HARDWARE',
    'Leather Atelier',
    'Supple hides, asymmetric zips, and hand-finished outerwear icons.',
    'Inspired by bespoke moto ateliers, each jacket pairs structured shoulders with enduring hardware.',
    'Explore Atelier',
    'elite/pack/leather-atelier',
    @img_lux_2,
    @img_lux_2,
    'metallic_shine',
    '#D4AF37',
    '#050507',
    0,
    0,
    10,
    1,
    '{"layout":"editorial_split","mode":"dark"}'
  ),
  (
    'statement-outerwear',
    'statement_outerwear',
    'MAISON MELVORE',
    'SCULPTURAL LAYERS',
    'Statement Outerwear',
    'Commanding coats, cropped moto silhouettes, and cold-weather armor.',
    'Designed to anchor the silhouette from the first glance.',
    'View Outerwear',
    'elite/pack/statement-outerwear',
    @img_lux_3,
    @img_lux_3,
    'metallic_shine',
    '#D4AF37',
    '#000000',
    0,
    0,
    20,
    1,
    '{"layout":"portrait_diptych","mode":"dark"}'
  ),
  (
    'after-dark-archive',
    'after_dark_archive',
    'MELVORE NOIR',
    'EVENING EDIT',
    'The After Dark Archive',
    'Nocturnal drapery, sharp evening cuts, and collector pieces dressed for midnight.',
    'Monochrome palettes accented by subtle champagne gold and brushed silver.',
    'Enter Archive',
    'elite/pack/after-dark-archive',
    @img_lux_4,
    @img_lux_4,
    'smooth_reveal',
    '#D4AF37',
    '#000000',
    0,
    0,
    30,
    1,
    '{"layout":"portrait_diptych","mode":"dark"}'
  ),
  (
    'signature-silhouettes',
    'signature_silhouettes',
    'CONNOISSEUR VAULT',
    'CURATED FOR THE CONNOISSEUR',
    'Signature Silhouettes & Premium Essentials',
    'Enduring proportions and elevated staples from leading contemporary houses.',
    'Quiet confidence in clean lines, tactile weaves, and effortless drape.',
    'Explore Vault',
    'elite/pack/signature-silhouettes',
    @img_lux_1,
    @img_lux_1,
    'smooth_reveal',
    '#C8CCD4',
    '#08080C',
    0,
    0,
    40,
    1,
    '{"layout":"showcase_grid","mode":"dark"}'
  );

INSERT INTO `app_elite_pack_items`
  (`pack_id`, `item_type`, `product_id`, `brand_id`, `title`, `subtitle`, `badge_text`, `image_url`, `is_sponsored`, `display_order`, `is_active`)
SELECT ep.pack_id,
       'product',
       p.product_id,
       p.brand_id,
       p.name,
       COALESCE(b.name, 'Melvore Luxe'),
       CASE
         WHEN ep.slug = 'leather-atelier' THEN 'ATELIER'
         WHEN ep.slug = 'after-dark-archive' THEN 'AFTER DARK'
         WHEN ep.slug = 'statement-outerwear' THEN 'OUTERWEAR'
         WHEN ep.slug = 'signature-silhouettes' THEN 'SIGNATURE'
         ELSE 'LUXE VAULT'
       END,
       p.img_src,
       CASE WHEN p.is_featured = 1 THEN 1 ELSE 0 END,
       p.product_id,
       1
FROM `app_elite_packs` ep
JOIN (
  SELECT product_id, brand_id, name, img_src, is_featured
  FROM `products`
  WHERE is_active = 1
  ORDER BY COALESCE(NULLIF(sale_price, 0), regular_price) DESC, is_featured DESC, product_id DESC
  LIMIT 12
) p
LEFT JOIN `brands` b ON b.brand_id = p.brand_id;

-- ----------------------------------------------------------------------------
-- 7. Seed `app_search_synonyms`, `app_search_logs`, `app_tags`, `app_product_tag_map`, `app_user_style_signals`
-- ----------------------------------------------------------------------------
INSERT INTO `app_search_synonyms` (`term`, `synonyms`, `expanded_terms`, `is_active`) VALUES
  ('coords', 'co-ord,co-ords,coord set,matching set,two piece', 'co-ord,co-ords,coord set,matching set,two piece', 1),
  ('co-ords', 'coord,coords,co-ord set,matching set', 'coord,coords,co-ord set,matching set', 1),
  ('tee', 't-shirt,tshirt,oversized tee,graphic tee', 't-shirt,tshirt,oversized tee,graphic tee', 1),
  ('tshirt', 't-shirt,tee,oversized t-shirt', 't-shirt,tee,oversized t-shirt', 1),
  ('biker', 'leather jacket,moto jacket,studded jacket,outerwear', 'leather jacket,moto jacket,studded jacket,outerwear', 1),
  ('korean', 'k-fashion,seoul,oversized,cropped jacket,minimal', 'k-fashion,seoul,oversized,cropped jacket,minimal', 1),
  ('winter', 'jacket,coat,hoodie,sweater,layering,leather', 'jacket,coat,hoodie,sweater,layering,leather', 1),
  ('party', 'evening,dress,after dark,luxe,statement', 'evening,dress,after dark,luxe,statement', 1),
  ('y2k', 'crop top,baby tee,cyber,chrome,denim,low rise', 'crop top,baby tee,cyber,chrome,denim,low rise', 1),
  ('goth', 'black,leather,studded,nocturnal,corset,boots', 'black,leather,studded,nocturnal,corset,boots', 1);

INSERT INTO `app_search_logs` (`user_id`, `guest_id`, `query_text`, `normalized_query`, `results_count`, `created_at`)
VALUES
  (NULL, 'melvore-editorial-seed', 'Leather Jacket', 'leather jacket', 6, NOW()),
  (NULL, 'melvore-editorial-seed', 'Oversized Tee', 'oversized tee', 8, NOW()),
  (NULL, 'melvore-editorial-seed', 'Biker Outerwear', 'biker outerwear', 5, NOW()),
  (NULL, 'melvore-editorial-seed', 'Co-ord Set', 'co ord set', 4, NOW()),
  (NULL, 'melvore-editorial-seed', 'Seoul Minimal Shirt', 'seoul minimal shirt', 5, NOW()),
  (NULL, 'melvore-editorial-seed', 'Y2K Crop', 'y2k crop', 6, NOW());

INSERT INTO `app_tags` (`slug`, `name`, `tag_type`, `is_active`) VALUES
  ('y2k', 'Y2K Cyber', 'style', 1),
  ('goth', 'Goth Nocturne', 'style', 1),
  ('k-fashion', 'K-Fashion Seoul', 'style', 1),
  ('coquette', 'Coquette', 'style', 1),
  ('streetwear', 'Streetwear', 'style', 1),
  ('quiet-luxury', 'Quiet Luxury', 'style', 1),
  ('minimal', 'Minimal', 'style', 1),
  ('winter', 'Winter Edit', 'season', 1),
  ('leather', 'Leather Atelier', 'material', 1);

INSERT IGNORE INTO `app_product_tag_map` (`product_id`, `tag_id`)
SELECT swp.product_id, t.tag_id
FROM `app_style_world_products` swp
INNER JOIN `app_style_worlds` sw ON sw.style_world_id = swp.style_world_id
INNER JOIN `app_tags` t ON t.slug = sw.slug;

INSERT IGNORE INTO `app_product_tag_map` (`product_id`, `tag_id`)
SELECT p.product_id, t.tag_id
FROM `products` p
JOIN `app_tags` t ON t.slug IN ('winter', 'leather')
WHERE p.is_active = 1
  AND (LOWER(p.name) LIKE '%leather%' OR LOWER(p.name) LIKE '%jacket%' OR LOWER(p.name) LIKE '%coat%' OR LOWER(p.name) LIKE '%biker%');

INSERT INTO `app_user_style_signals`
  (`user_id`, `guest_id`, `style_world_id`, `brand_id`, `category_id`, `product_id`, `signal_type`, `signal_weight`, `created_at`)
SELECT NULL,
       'melvore-editorial-seed',
       (SELECT swp.style_world_id FROM `app_style_world_products` swp WHERE swp.product_id = p.product_id LIMIT 1),
       p.brand_id,
       p.category_id,
       p.product_id,
       'style_explore',
       2.00,
       NOW()
FROM `products` p
WHERE p.is_active = 1
ORDER BY p.is_featured DESC, p.product_id DESC
LIMIT 6;

-- ----------------------------------------------------------------------------
-- 8. Seed `app_smart_offers` & `app_offer_impressions`
--    Linked strictly to real active `coupons` rows (never invented coupon codes)
-- ----------------------------------------------------------------------------
INSERT INTO `app_smart_offers` (
  `offer_key`, `title`, `subtitle`, `badge_text`, `coupon_code`, `world_slug`, `occasion_slug`,
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
  NULL,
  COALESCE(c.min_order_amount, 0.00),
  1,
  24,
  25,
  '#3B82F6',
  'Use Code',
  1
FROM `coupons` c
WHERE c.is_active = 1
  AND (c.valid_from IS NULL OR c.valid_from <= NOW())
  AND (c.valid_to IS NULL OR c.valid_to >= NOW())
  AND (c.usage_limit IS NULL OR c.usage_limit = 0 OR c.usage_count < c.usage_limit)
ORDER BY c.coupon_id ASC
LIMIT 4;

INSERT INTO `app_offer_impressions` (`offer_id`, `user_id`, `guest_id`, `action_type`, `created_at`)
SELECT o.offer_id, NULL, 'melvore-editorial-seed', 'shown', NOW()
FROM `app_smart_offers` o
WHERE o.is_active = 1;

-- ----------------------------------------------------------------------------
-- 9. Seed Major Metro Delivery Cache (`INSERT IGNORE` so existing cache rows stay intact)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `app_delivery_serviceability_cache`
  (`pincode`, `city`, `state`, `is_serviceable`, `cod_available`, `express_available`, `estimated_days_min`, `estimated_days_max`)
VALUES
  ('110001', 'New Delhi', 'Delhi', 1, 1, 1, 2, 4),
  ('110033', 'New Delhi', 'Delhi', 1, 1, 1, 1, 3),
  ('400001', 'Mumbai', 'Maharashtra', 1, 1, 1, 2, 4),
  ('560001', 'Bengaluru', 'Karnataka', 1, 1, 1, 2, 4),
  ('500001', 'Hyderabad', 'Telangana', 1, 1, 1, 2, 5),
  ('600001', 'Chennai', 'Tamil Nadu', 1, 1, 0, 3, 5),
  ('700001', 'Kolkata', 'West Bengal', 1, 1, 0, 3, 6),
  ('411001', 'Pune', 'Maharashtra', 1, 1, 1, 2, 4);

-- ============================================================================
-- VERIFICATION SUMMARY QUERY (Safe read-only check after import)
-- ============================================================================
SELECT 'app_campaigns' AS table_name, COUNT(*) AS row_count FROM `app_campaigns`
UNION ALL SELECT 'app_campaign_regions', COUNT(*) FROM `app_campaign_regions`
UNION ALL SELECT 'app_campaign_assets', COUNT(*) FROM `app_campaign_assets`
UNION ALL SELECT 'app_home_sections', COUNT(*) FROM `app_home_sections`
UNION ALL SELECT 'app_home_section_items', COUNT(*) FROM `app_home_section_items`
UNION ALL SELECT 'app_product_placements', COUNT(*) FROM `app_product_placements`
UNION ALL SELECT 'app_brand_profiles', COUNT(*) FROM `app_brand_profiles`
UNION ALL SELECT 'app_user_brand_activity', COUNT(*) FROM `app_user_brand_activity`
UNION ALL SELECT 'app_brand_origins', COUNT(*) FROM `app_brand_origins`
UNION ALL SELECT 'app_style_worlds', COUNT(*) FROM `app_style_worlds`
UNION ALL SELECT 'app_style_world_products', COUNT(*) FROM `app_style_world_products`
UNION ALL SELECT 'app_world_brands', COUNT(*) FROM `app_world_brands`
UNION ALL SELECT 'app_world_assets', COUNT(*) FROM `app_world_assets`
UNION ALL SELECT 'app_occasions', COUNT(*) FROM `app_occasions`
UNION ALL SELECT 'app_product_videos', COUNT(*) FROM `app_product_videos`
UNION ALL SELECT 'app_video_events', COUNT(*) FROM `app_video_events`
UNION ALL SELECT 'app_elite_packs', COUNT(*) FROM `app_elite_packs`
UNION ALL SELECT 'app_elite_pack_items', COUNT(*) FROM `app_elite_pack_items`
UNION ALL SELECT 'app_user_style_signals', COUNT(*) FROM `app_user_style_signals`
UNION ALL SELECT 'app_search_synonyms', COUNT(*) FROM `app_search_synonyms`
UNION ALL SELECT 'app_search_logs', COUNT(*) FROM `app_search_logs`
UNION ALL SELECT 'app_tags', COUNT(*) FROM `app_tags`
UNION ALL SELECT 'app_product_tag_map', COUNT(*) FROM `app_product_tag_map`
UNION ALL SELECT 'app_smart_offers', COUNT(*) FROM `app_smart_offers`
UNION ALL SELECT 'app_offer_impressions', COUNT(*) FROM `app_offer_impressions`;
