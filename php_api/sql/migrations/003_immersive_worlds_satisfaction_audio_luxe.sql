-- ============================================================================
-- MELVORE — ADDITIVE MIGRATION 003:
-- Immersive Fashion Worlds, One-Time Vibe Satisfaction + Coupons,
-- World Audio URLs, Promo Videos, Y2K Runway Config, Category Editorial Configs,
-- Recommendation Cache, and Admin-Managed Luxe Collections
-- Safe, non-destructive (`CREATE TABLE IF NOT EXISTS` + `information_schema` guards).
-- ============================================================================

SET NAMES utf8mb4;

-- 1. One-Time Vibe Satisfaction Feedback & Reward Tracking
CREATE TABLE IF NOT EXISTS `app_style_world_feedback` (
  `feedback_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `style_world_id` INT UNSIGNED NOT NULL,
  `world_slug` VARCHAR(100) NOT NULL,
  `satisfaction_score` TINYINT NOT NULL,
  `feedback_label` VARCHAR(80) NOT NULL,
  `coupon_id` INT DEFAULT NULL,
  `coupon_code` VARCHAR(64) DEFAULT NULL,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`feedback_id`),
  UNIQUE KEY `uq_swf_user_world` (`user_id`, `style_world_id`),
  UNIQUE KEY `uq_swf_guest_world` (`guest_id`, `style_world_id`),
  KEY `idx_swf_world_slug` (`world_slug`, `submitted_at`),
  KEY `idx_swf_coupon` (`coupon_id`, `coupon_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Homepage Editorial Category Showcase Configuration
CREATE TABLE IF NOT EXISTS `app_category_editorial_configs` (
  `config_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id` INT NOT NULL,
  `motif_key` VARCHAR(48) NOT NULL DEFAULT 'editorial_frame',
  `editorial_badge` VARCHAR(64) DEFAULT NULL,
  `editorial_subtitle` VARCHAR(220) DEFAULT NULL,
  `accent_hex` VARCHAR(16) NOT NULL DEFAULT '#141414',
  `bg_hex` VARCHAR(16) NOT NULL DEFAULT '#F8F7F4',
  `animation_key` VARCHAR(64) NOT NULL DEFAULT 'zoom_reveal',
  `tile_span` VARCHAR(24) NOT NULL DEFAULT 'regular',
  `cover_image_url` VARCHAR(512) DEFAULT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uq_cat_edit_category` (`category_id`),
  KEY `idx_cat_edit_order` (`is_active`, `display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Backend Personalisation Recommendation Cache
CREATE TABLE IF NOT EXISTS `app_recommendation_cache` (
  `cache_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `actor_key` VARCHAR(120) NOT NULL,
  `user_id` INT DEFAULT NULL,
  `guest_id` VARCHAR(96) DEFAULT NULL,
  `payload_json` LONGTEXT NOT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `expires_at` DATETIME NOT NULL,
  PRIMARY KEY (`cache_id`),
  UNIQUE KEY `uk_rec_cache_actor` (`actor_key`),
  KEY `idx_rec_cache_exp` (`expires_at`),
  KEY `idx_rec_cache_user` (`user_id`),
  KEY `idx_rec_cache_guest` (`guest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Guarded Additive Columns on Existing Experience Tables
DROP PROCEDURE IF EXISTS `melvore_apply_additive_columns_003`;
DELIMITER $$
CREATE PROCEDURE `melvore_apply_additive_columns_003`()
BEGIN
    -- app_style_worlds: audio, promo video, runway config, button style
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'audio_url') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `audio_url` VARCHAR(512) DEFAULT NULL AFTER `audio_track_key`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'audio_loop') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `audio_loop` TINYINT(1) NOT NULL DEFAULT 1 AFTER `audio_url`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'audio_volume') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `audio_volume` DECIMAL(3,2) NOT NULL DEFAULT 0.45 AFTER `audio_loop`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'promo_video_url') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `promo_video_url` VARCHAR(512) DEFAULT NULL AFTER `video_url`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'promo_video_poster_url') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `promo_video_poster_url` VARCHAR(512) DEFAULT NULL AFTER `promo_video_url`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'promo_video_is_sponsored') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `promo_video_is_sponsored` TINYINT(1) NOT NULL DEFAULT 0 AFTER `promo_video_poster_url`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'runway_config') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `runway_config` JSON DEFAULT NULL AFTER `theme_config`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_style_worlds' AND column_name = 'button_style') = 0 THEN
        ALTER TABLE `app_style_worlds` ADD COLUMN `button_style` VARCHAR(48) DEFAULT NULL AFTER `animation_key`;
    END IF;

    -- app_occasions: audio override & festive button style
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_occasions' AND column_name = 'audio_url') = 0 THEN
        ALTER TABLE `app_occasions` ADD COLUMN `audio_url` VARCHAR(512) DEFAULT NULL AFTER `audio_track_key`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_occasions' AND column_name = 'audio_loop') = 0 THEN
        ALTER TABLE `app_occasions` ADD COLUMN `audio_loop` TINYINT(1) NOT NULL DEFAULT 1 AFTER `audio_url`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_occasions' AND column_name = 'button_style') = 0 THEN
        ALTER TABLE `app_occasions` ADD COLUMN `button_style` VARCHAR(48) DEFAULT NULL AFTER `motif_key`;
    END IF;

    -- app_user_preferences: video autoplay & data saver
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_user_preferences' AND column_name = 'video_autoplay_muted') = 0 THEN
        ALTER TABLE `app_user_preferences` ADD COLUMN `video_autoplay_muted` TINYINT(1) NOT NULL DEFAULT 1 AFTER `reduced_motion`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_user_preferences' AND column_name = 'data_saver_mode') = 0 THEN
        ALTER TABLE `app_user_preferences` ADD COLUMN `data_saver_mode` TINYINT(1) NOT NULL DEFAULT 0 AFTER `video_autoplay_muted`;
    END IF;

    -- app_elite_packs: Luxe editorial layout & scheduling controls
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_elite_packs' AND column_name = 'layout_type') = 0 THEN
        ALTER TABLE `app_elite_packs` ADD COLUMN `layout_type` VARCHAR(32) NOT NULL DEFAULT 'editorial_grid' AFTER `is_sponsored`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_elite_packs' AND column_name = 'featured_product_id') = 0 THEN
        ALTER TABLE `app_elite_packs` ADD COLUMN `featured_product_id` INT DEFAULT NULL AFTER `layout_type`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_elite_packs' AND column_name = 'status') = 0 THEN
        ALTER TABLE `app_elite_packs` ADD COLUMN `status` VARCHAR(24) NOT NULL DEFAULT 'published' AFTER `is_active`;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_elite_packs' AND column_name = 'scheduled_publish_at') = 0 THEN
        ALTER TABLE `app_elite_packs` ADD COLUMN `scheduled_publish_at` DATETIME DEFAULT NULL AFTER `start_date`;
    END IF;
END$$
DELIMITER ;

CALL `melvore_apply_additive_columns_003`();
DROP PROCEDURE IF EXISTS `melvore_apply_additive_columns_003`;
