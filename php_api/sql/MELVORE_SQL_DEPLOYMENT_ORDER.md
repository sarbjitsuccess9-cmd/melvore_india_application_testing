# Melvore SQL Deployment Order & Live Compatibility Guide

**Target Database:** `u205121999_melvore`  
**Compatible Engines:** MySQL 5.7+, MySQL 8.0+, MariaDB 10.3+  
**Core Rule:** None of the scripts in this deployment sequence drop, truncate, or alter core commerce data (`users`, `products`, `product_variations`, `categories`, `brands`, `coupons`, `orders`, `order_items`, `payments`, `addresses`, `cart`, `user_cart`, `wishlist`, `reviews`).

---

## Step 0 — Mandatory Pre-Flight Backup & Schema Inspection

Before running any SQL script on production, take a safety dump and inspect existing legacy column layouts on `hero_content` and `coupons`:

```bash
mysqldump -u u205121999_user -p u205121999_melvore > melvore_pre_repair_backup_$(date +%F).sql
```

Inspect live columns in phpMyAdmin or MySQL CLI before importing:

```sql
SHOW CREATE TABLE hero_content;
SHOW CREATE TABLE coupons;
SHOW CREATE TABLE app_delivery_serviceability_cache;
```

> Note: If your live `coupons` table uses column names other than `is_active`, `valid_from`, `valid_to`, `usage_limit`, `usage_count`, `discount_type`, `discount_value`, or `min_order_amount`, verify section 8 of `melvore_remaining_seed_fixed.sql` against the output of `SHOW CREATE TABLE coupons;` before running it.

---

## Step 1 — Exact Execution Order

Run the SQL files strictly in this sequence:

1. **Schema Repair (Idempotent & Non-Destructive)**
   - File: `php_api/sql/melvore_live_schema_compatibility_repair.sql`
   - Classification: `SAFE RE-RUNNABLE`
   - Purpose:
     - Adds any missing columns to `hero_content` (`eyebrow`, `image_url`, `mobile_image_url`, `desktop_image_url`, `cta_text`, `cta_link`, `display_order`, `is_active`) and backfills `image_url` from legacy columns (`image`, `banner_image`, `desktop_image`, `img_src`, `media_url`) if present.
     - Repairs `app_delivery_serviceability_cache` (`city`, `state`, `is_serviceable`, `cod_available`, `express_available`, `estimated_days_min`, `estimated_days_max`, `updated_at`, `uk_delivery_pincode`).
     - Ensures `app_style_world_feedback`, `app_category_editorial_configs`, `app_recommendation_cache`, `app_user_preferences`, `app_product_media` exist and repairs missing columns across `app_style_worlds` (`button_style`, `runway_config`, `audio_url`, `promo_video_url`, `promo_video_poster_url`, `promo_video_is_sponsored`), `app_occasions`, `app_elite_packs`, `app_user_brand_activity`, `app_search_synonyms`, and `app_search_logs`.

2. **Hero Content Seed**
   - File: `php_api/sql/melvore_hero_content_seed_fixed.sql`
   - Classification: `IDEMPOTENT INSERT IF NOT EXISTS`
   - Purpose: Inserts default active hero slides backed by real `products.img_src` photography only when titles are not already present, and fills blank `mobile_image_url` / `desktop_image_url` fields.

3. **Delivery Cache Seed**
   - File: `php_api/sql/melvore_delivery_cache_seed_fixed.sql`
   - Classification: `IDEMPOTENT INSERT IGNORE`
   - Purpose: Populates Indian metro pincode delivery/COD cache rows without overwriting existing entries.

4. **Editorial, Style Worlds, Luxe & Merchandising Seed**
   - File: `php_api/sql/melvore_remaining_seed_fixed.sql`
   - Classification: `NON-DESTRUCTIVE UPSERT / INSERT IGNORE`
   - Purpose: Populates `app_campaigns`, `app_home_sections`, `app_product_placements`, `app_brand_profiles`, `app_brand_origins`, `app_style_worlds`, `app_style_world_products`, `app_world_brands`, `app_category_editorial_configs` (using `@cat_rn` and `categories.status = 'active'`), `app_elite_packs`, `app_elite_pack_items` (keeps `image_url = NULL` on product rows so real `products.img_src` is shown), `app_search_synonyms`, and coupon-backed `app_smart_offers`.

5. **Companion Performance Indexes (Optional / Already Existing OK)**
   - File: `php_api/sql/melvore_experience_companion_indexes.sql`
   - Classification: `INDEX ADDITION` (skip if indexes already exist).

---

## Step 2 — Post-Deployment Verification Queries

Run these read-only checks to confirm the backend has zero empty critical modules:

```sql
SELECT id, eyebrow, title, image_url, is_active FROM hero_content ORDER BY display_order ASC;
SELECT COUNT(*) AS metro_pincodes FROM app_delivery_serviceability_cache;
SELECT slug, name, button_style, audio_url, promo_video_url FROM app_style_worlds ORDER BY display_order ASC;
SELECT COUNT(*) AS category_editorial_rows FROM app_category_editorial_configs WHERE is_active = 1;
SELECT slug, title, status, layout_type FROM app_elite_packs WHERE is_active = 1 ORDER BY display_order ASC;
SELECT COUNT(*) AS broken_luxe_product_overrides FROM app_elite_pack_items WHERE item_type = 'product' AND image_url IS NOT NULL;
```

Expected:
- `hero_content` returns >= 1 active row with non-empty `image_url`.
- `broken_luxe_product_overrides` returns `0`.
- `ThemeEngineService::formatWorldTheme` returns non-empty `slug` and `background_color` via `/php_api/api/styles/list.php` and `/php_api/api/styles/details.php`.

---

## Step 3 — Rollback Strategy

If any unexpected column behavior occurs on production:
1. Restore from Step 0 snapshot:
   ```bash
   mysql -u u205121999_user -p u205121999_melvore < melvore_pre_repair_backup_YYYY-MM-DD.sql
   ```
2. Remember that PHP endpoints (`api/home/feed.php`, `design.php`) now inspect `hero_content` and `banner_content` columns dynamically (`SELECT *`), so legacy column schemas continue serving responses cleanly.
