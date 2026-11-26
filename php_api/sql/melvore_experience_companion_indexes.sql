-- ============================================================================
-- Melvore Customer App Experience — Companion Non-Destructive Index Check SQL
-- Target DB: u205121999_melvore (MariaDB / MySQL)
-- Run ONLY AFTER backing up your database and executing:
--   melvore_customer_app_experience_migration.sql
--
-- Purpose: Ensures hot lookup paths used by /php_api/api/* remain indexed
-- without duplicating or altering any existing table data or definitions.
-- ============================================================================

SET @db_name = DATABASE();

-- 1. Index reviews by (product_id, is_approved, is_verified_purchase) for fast review list queries
SET @idx_rev_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'reviews' AND INDEX_NAME = 'idx_reviews_prod_approved'
);
SET @sql_rev = IF(@idx_rev_exists = 0,
    'ALTER TABLE `reviews` ADD INDEX `idx_reviews_prod_approved` (`product_id`, `is_approved`, `is_verified_purchase`)',
    'SELECT "idx_reviews_prod_approved already exists"'
);
PREPARE stmt_rev FROM @sql_rev;
EXECUTE stmt_rev;
DEALLOCATE PREPARE stmt_rev;

-- 2. Index app_search_logs by (created_at, normalized_query) if table exists
SET @tbl_search_logs = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'app_search_logs'
);
SET @idx_search_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'app_search_logs' AND INDEX_NAME = 'idx_search_logs_recent_norm'
);
SET @sql_search = IF(@tbl_search_logs > 0 AND @idx_search_exists = 0,
    'ALTER TABLE `app_search_logs` ADD INDEX `idx_search_logs_recent_norm` (`created_at`, `normalized_query`)',
    'SELECT "idx_search_logs_recent_norm skipped"'
);
PREPARE stmt_search FROM @sql_search;
EXECUTE stmt_search;
DEALLOCATE PREPARE stmt_search;

-- 3. Index app_user_style_signals for fast For You aggregation
SET @tbl_signals = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'app_user_style_signals'
);
SET @idx_sig_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'app_user_style_signals' AND INDEX_NAME = 'idx_style_signals_actor'
);
SET @sql_sig = IF(@tbl_signals > 0 AND @idx_sig_exists = 0,
    'ALTER TABLE `app_user_style_signals` ADD INDEX `idx_style_signals_actor` (`user_id`, `guest_id`, `style_world_id`)',
    'SELECT "idx_style_signals_actor skipped"'
);
PREPARE stmt_sig FROM @sql_sig;
EXECUTE stmt_sig;
DEALLOCATE PREPARE stmt_sig;

-- 4. Index products by (is_active, regular_price, sale_price) for fast budget edits & sale queries
SET @idx_price_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'products' AND INDEX_NAME = 'idx_products_active_pricing'
);
SET @sql_price = IF(@idx_price_exists = 0,
    'ALTER TABLE `products` ADD INDEX `idx_products_active_pricing` (`is_active`, `regular_price`, `sale_price`)',
    'SELECT "idx_products_active_pricing already exists"'
);
PREPARE stmt_price FROM @sql_price;
EXECUTE stmt_price;
DEALLOCATE PREPARE stmt_price;

