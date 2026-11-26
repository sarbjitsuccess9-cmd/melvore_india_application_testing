-- ============================================================================
-- MELVORE CENTRAL REMOTE DESIGN SYSTEM & 8-DAY OCCASION ENGINE MIGRATION
-- Target Engine: MySQL 8.0+ / MariaDB 10.6+ (Idempotent / Live-DB Safe)
-- ============================================================================

CREATE TABLE IF NOT EXISTS `app_design_settings` (
  `setting_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `setting_key` VARCHAR(64) NOT NULL DEFAULT 'global',
  `active_design` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0 = Default Manifest, 1 = Live Manifest',
  `active_release_id` INT UNSIGNED NULL,
  `default_release_id` INT UNSIGNED NULL,
  `live_release_id` INT UNSIGNED NULL,
  `min_app_schema_version` VARCHAR(24) NOT NULL DEFAULT '1.0.0',
  `updated_by` INT UNSIGNED NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting_id`),
  UNIQUE KEY `uq_design_setting_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_design_releases` (
  `release_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slot` ENUM('default', 'live') NOT NULL DEFAULT 'live',
  `status` ENUM('draft', 'published', 'archived') NOT NULL DEFAULT 'draft',
  `version_tag` VARCHAR(40) NOT NULL,
  `schema_version` VARCHAR(24) NOT NULL DEFAULT '1.0.0',
  `min_app_schema_version` VARCHAR(24) NOT NULL DEFAULT '1.0.0',
  `release_notes` VARCHAR(500) NOT NULL DEFAULT '',
  `manifest_json` LONGTEXT NOT NULL,
  `checksum_sha256` CHAR(64) NOT NULL DEFAULT '',
  `created_by` INT UNSIGNED NULL,
  `published_by` INT UNSIGNED NULL,
  `published_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`release_id`),
  KEY `idx_design_release_slot_status` (`slot`, `status`, `updated_at`),
  KEY `idx_design_release_version` (`version_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_design_assets` (
  `asset_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `asset_key` VARCHAR(100) NOT NULL,
  `asset_type` ENUM('font', 'icon', 'image', 'animation', 'illustration', 'sound') NOT NULL DEFAULT 'image',
  `title` VARCHAR(160) NOT NULL DEFAULT '',
  `relative_path` VARCHAR(255) NOT NULL,
  `mime_type` VARCHAR(80) NOT NULL DEFAULT 'image/svg+xml',
  `file_size_bytes` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `metadata_json` JSON NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_by` INT UNSIGNED NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`asset_id`),
  UNIQUE KEY `uq_design_asset_key` (`asset_key`),
  KEY `idx_design_asset_type` (`asset_type`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_design_page_configs` (
  `page_config_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `slot` ENUM('default', 'live') NOT NULL DEFAULT 'default',
  `page_key` VARCHAR(48) NOT NULL,
  `layout_template` VARCHAR(64) NOT NULL DEFAULT 'editorial_magazine',
  `hero_height_dp` INT NOT NULL DEFAULT 420,
  `grid_columns` TINYINT NOT NULL DEFAULT 2,
  `sections_json` LONGTEXT NOT NULL,
  `is_published` TINYINT(1) NOT NULL DEFAULT 1,
  `updated_by` INT UNSIGNED NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`page_config_id`),
  UNIQUE KEY `uq_design_slot_page` (`slot`, `page_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `app_design_release_history` (
  `history_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `release_id` INT UNSIGNED NULL,
  `action_type` VARCHAR(40) NOT NULL COMMENT 'save_draft | publish | switch_active | rollback | duplicate',
  `slot` ENUM('default', 'live') NOT NULL DEFAULT 'live',
  `previous_active_design` TINYINT(1) NULL,
  `new_active_design` TINYINT(1) NULL,
  `actor_user_id` INT UNSIGNED NULL,
  `notes` VARCHAR(500) NOT NULL DEFAULT '',
  `snapshot_json` LONGTEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`history_id`),
  KEY `idx_design_history_created` (`created_at`),
  KEY `idx_design_history_release` (`release_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Safely extend existing app_occasions table for automatic 8-day-before IST scheduler & regional targeting
DELIMITER $$
DROP PROCEDURE IF EXISTS `melvore_migrate_occasions_scheduler` $$
CREATE PROCEDURE `melvore_migrate_occasions_scheduler`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'event_date'
    ) THEN
        ALTER TABLE `app_occasions` ADD COLUMN `event_date` DATE NULL AFTER `is_active`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'activation_days_before'
    ) THEN
        ALTER TABLE `app_occasions` ADD COLUMN `activation_days_before` INT NOT NULL DEFAULT 8 AFTER `event_date`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'activation_start_at'
    ) THEN
        ALTER TABLE `app_occasions` ADD COLUMN `activation_start_at` DATETIME NULL AFTER `activation_days_before`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'activation_end_at'
    ) THEN
        ALTER TABLE `app_occasions` ADD COLUMN `activation_end_at` DATETIME NULL AFTER `activation_start_at`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'timezone'
    ) THEN
        ALTER TABLE `app_occasions` ADD COLUMN `timezone` VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata' AFTER `activation_end_at`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'region_state'
    ) THEN
        ALTER TABLE `app_occasions` ADD COLUMN `region_state` VARCHAR(80) NOT NULL DEFAULT '' AFTER `region_country`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'region_city'
    ) THEN
        ALTER TABLE `app_occasions` ADD COLUMN `region_city` VARCHAR(80) NOT NULL DEFAULT '' AFTER `region_state`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_occasions' AND COLUMN_NAME = 'design_override_json'
    ) THEN
        ALTER TABLE `app_occasions` ADD COLUMN `design_override_json` JSON NULL AFTER `theme_config`;
    END IF;
END $$
CALL `melvore_migrate_occasions_scheduler`() $$
DROP PROCEDURE IF EXISTS `melvore_migrate_occasions_scheduler` $$
DELIMITER ;
