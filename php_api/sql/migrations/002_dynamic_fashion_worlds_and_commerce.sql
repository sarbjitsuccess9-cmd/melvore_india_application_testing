-- ============================================================================
-- MELVORE — ADDITIVE MIGRATION 002:
-- Dynamic Fashion Worlds, Occasions, World Map Origins, Smart Offers,
-- User Audio/Motion Preferences, Auth Tokens, Order Idempotency & Razorpay Link
-- Safe, non-destructive (`CREATE TABLE IF NOT EXISTS` + `information_schema` guards).
-- ============================================================================

SET NAMES utf8mb4;

-- 1. Core API Security & Checkout Reliability Tables
CREATE TABLE IF NOT EXISTS `api_tokens` (
  `token_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `token_hash` CHAR(64) NOT NULL,
  `device_label` VARCHAR(255) DEFAULT NULL,
  `ip_address` VARCHAR(64) DEFAULT NULL,
  `expires_at` DATETIME DEFAULT NULL,
  `revoked_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`token_id`),
  UNIQUE KEY `uk_api_tokens_hash` (`token_hash`),
  KEY `idx_api_tokens_user_active` (`user_id`, `revoked_at`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `api_order_idempotency` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `idempotency_key` VARCHAR(128) NOT NULL,
  `request_hash` CHAR(64) NOT NULL,
  `order_id` INT NOT NULL,
  `response_json` LONGTEXT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_idem_user_key` (`user_id`, `idempotency_key`),
  KEY `idx_order_idem_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `payment_webhook_events` (
  `webhook_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `gateway` VARCHAR(32) NOT NULL DEFAULT 'razorpay',
  `event_id` VARCHAR(128) NOT NULL,
  `event_type` VARCHAR(80) NOT NULL,
  `order_id` INT DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'processing',
  `payload_json` LONGTEXT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`webhook_id`),
  UNIQUE KEY `uk_webhook_gateway_event` (`gateway`, `event_id`),
  KEY `idx_webhook_order` (`order_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Dynamic Fashion Worlds: Occasions, Vector Assets, Brand Origins, Smart Offers, User Prefs
CREATE TABLE IF NOT EXISTS `app_occasions` (
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

CREATE TABLE IF NOT EXISTS `app_world_assets` (
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

CREATE TABLE IF NOT EXISTS `app_brand_origins` (
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

CREATE TABLE IF NOT EXISTS `app_world_brands` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `style_world_id` INT UNSIGNED NOT NULL,
  `brand_id` INT NOT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_world_brand` (`style_world_id`, `brand_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_smart_offers` (
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

CREATE TABLE IF NOT EXISTS `app_offer_impressions` (
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

-- 3. Idempotent `information_schema`-guarded Column & Index Additions on Existing Tables
DROP PROCEDURE IF EXISTS `melvore_apply_additive_columns_002`;
DELIMITER $$
CREATE PROCEDURE `melvore_apply_additive_columns_002`()
BEGIN
    -- users.role
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'role') = 0 THEN
        ALTER TABLE `users` ADD COLUMN `role` VARCHAR(32) NOT NULL DEFAULT 'customer' AFTER `social_provider`;
    END IF;

    -- orders.refund_status & refund_reference
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'refund_status') = 0 THEN
        ALTER TABLE `orders` ADD COLUMN `refund_status` VARCHAR(40) DEFAULT NULL AFTER `payment_transaction_id`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'refund_reference') = 0 THEN
        ALTER TABLE `orders` ADD COLUMN `refund_reference` VARCHAR(120) DEFAULT NULL AFTER `refund_status`;
    END IF;

    -- payments.order_id & lookup indexes
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payments' AND column_name = 'order_id') = 0 THEN
        ALTER TABLE `payments` ADD COLUMN `order_id` INT DEFAULT NULL AFTER `user_id`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'payments' AND index_name = 'idx_payments_rzp_order_id') = 0 THEN
        ALTER TABLE `payments` ADD INDEX `idx_payments_rzp_order_id` (`razorpay_order_id`);
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'payments' AND index_name = 'idx_payments_user_order') = 0 THEN
        ALTER TABLE `payments` ADD INDEX `idx_payments_user_order` (`user_id`, `order_id`);
    END IF;

    -- app_style_worlds additive visual/audio/version columns
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'ambient_preset') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `ambient_preset` VARCHAR(64) DEFAULT NULL AFTER `font_style`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'audio_track_key') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `audio_track_key` VARCHAR(64) DEFAULT NULL AFTER `ambient_preset`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'version_tag') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `version_tag` VARCHAR(32) NOT NULL DEFAULT '1.0' AFTER `display_order`;
    END IF;
END$$
DELIMITER ;

CALL `melvore_apply_additive_columns_002`();
DROP PROCEDURE IF EXISTS `melvore_apply_additive_columns_002`;
