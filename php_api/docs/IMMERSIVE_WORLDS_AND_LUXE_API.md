# Melvore Immersive Worlds, Personalisation, Audio, Promo Media & Luxe CMS API Guide

This document describes the database schema changes, API endpoints, Android integration behavior, and Admin CMS studio workflows introduced for the Melvore Immersive Fashion Worlds upgrade.

---

## 1. SQL Schema & Seed Migrations

Run the additive SQL migrations in order after existing schema `001` / `002`:

1. **`php_api/sql/migrations/003_immersive_worlds_satisfaction_audio_luxe.sql`**
   - Extends `app_style_worlds`:
     - `audio_url VARCHAR(500) NULL`, `audio_loop TINYINT(1) DEFAULT 1`, `audio_volume DECIMAL(3,2) DEFAULT 0.45`
     - `promo_video_url VARCHAR(500) NULL`, `promo_video_poster_url VARCHAR(500) NULL`, `promo_video_is_sponsored TINYINT(1) DEFAULT 0`
     - `runway_config_json LONGTEXT NULL` (animation preset, speed, background hex, collection slug, artwork URL, display position, visibility, and model specs)
   - Extends `app_occasions`:
     - `button_style VARCHAR(40) NULL`, `audio_url VARCHAR(500) NULL`, `audio_loop TINYINT(1) DEFAULT 1`
   - Extends `categories`:
     - `cover_image_url`, `motif_key`, `editorial_badge`, `editorial_subtitle`, `accent_hex`, `bg_hex`, `animation_key`, `tile_span`, `display_order`
   - Extends `app_elite_packs` & `app_elite_pack_items`:
     - `layout_type`, `featured_product_id` on packs
     - `badge_text`, `is_featured` on pack items
   - Extends `app_user_preferences`:
     - `video_autoplay_muted TINYINT(1) DEFAULT 1`, `data_saver_mode TINYINT(1) DEFAULT 0`
   - Adds new tables:
     - `app_style_world_feedback` (`UNIQUE KEY uniq_world_user (style_world_id, user_id)`, `UNIQUE KEY uniq_world_guest (style_world_id, guest_id)`) — enforces strictly one satisfaction submission & coupon unlock per actor per Fashion World.
     - `app_user_recommendation_cache` (`actor_type`, `actor_id`, `payload_json`, `updated_at`) — caches weighted personalisation payloads and invalidates on new signals, purchases, wishlists, video events, or world satisfaction submissions.

2. **`php_api/sql/seeds/003_immersive_worlds_satisfaction_audio_luxe_seed.sql`**
   - Seeds Y2K Pixel Runway (`runway_config_json`), Goth Bat Swarm styling tokens, promo video poster URLs, audio settings, category editorial magazine cards, Luxe `designer_spotlight` + `squared_archive` pack layouts, and world reward coupons (`MELVOREVIBE10`, `MELVOREGOTH15`, `MELVOREY2K15`, `MELVORECOQ12`).

---

## 2. API Endpoints (Added & Updated)

### A. One-Time World Vibe Satisfaction & Coupon Unlock
- **`GET /api/styles/feedback.php?style_world_id={id}&slug={slug}`**
  - Returns whether the authenticated user (`user_id`) or guest (`X-Guest-Id`) has submitted satisfaction feedback for the world, plus `submitted_world_ids` and `submitted_world_slugs`.
- **`POST /api/styles/feedback.php`**
  - Body: `{ "style_world_id": 1, "world_slug": "y2k", "satisfaction_score": 1..4 }`
  - Score stages: `1 = Not really`, `2 = It's okay`, `3 = Love it`, `4 = Absolutely obsessed!`
  - Atomically inserts one feedback row inside a DB transaction, generates or assigns a unique reward coupon from `coupons` (never duplicating rewards on repeated calls), records a weighted `style_feedback` signal, and invalidates `app_user_recommendation_cache`.

### B. Weighted Recommendation & Personalisation Engine
- **`GET /api/personalization/for_you.php`**
  - Powered by `RecommendationService::buildForYouBundle()`.
  - Signal weights:
    - Purchases (`orders` + `order_items`): very strong positive (`+10.0`)
    - World satisfaction (`app_style_world_feedback`): score 4 -> `+12.0`, score 3 -> `+7.0`, score 2 -> `+1.5`, score 1 -> `-10.0` (demotes avoided world)
    - Wishlist (`wishlist`): strong positive (`+6.0`)
    - Cart adds (`cart`): strong positive (`+5.0`)
    - Video engagement (`app_video_events`: like/save/share/watch): moderate positive (`+1.5` to `+4.5`)
    - Product view (`product_view`): moderate positive (`+2.0`)
  - Returns active catalog products only plus `preferred_worlds`, `avoided_worlds`, `preferred_categories`, `preferred_styles`, and `recently_explored_worlds`.
- **`GET /api/products/recommendations.php?product_id={id}&world_slug={slug}`**
  - Returns world-aware `similar_products`, `more_from_brand`, and `world_slug`.

### C. Immersive World Metadata, Audio, Categories & Luxe
- **`GET /api/bootstrap.php`**: Includes `submitted_world_ids`, `submitted_world_slugs`, `audio_url`, `audio_loop`, `audio_volume`, `promo_video_url`, `promo_video_poster_url`, `promo_video_is_sponsored`, `runway_config`, `has_submitted_feedback`, and `occasion.button_style` / `audio_url`.
- **`GET /api/styles/list.php`** & **`GET /api/styles/details.php`**: Include full audio, promo video, runway config, and `satisfaction_status`.
- **`GET /api/users/preferences.php`** & **`POST /api/users/preferences.php`**: Persist `ambient_audio_enabled`, `ui_sfx_enabled`, `reduced_motion`, `video_autoplay_muted`, `data_saver_mode`, `audio_consent_answered`, and `preferred_world_slug`.
- **`GET /api/categories/list.php`** & **`GET /api/home/feed.php`**: Return editorial category card fields (`cover_image_url`, `motif_key`, `editorial_badge`, `editorial_subtitle`, `accent_hex`, `bg_hex`, `animation_key`, `tile_span`, `display_order`).
- **`GET /api/elite/home.php`** & **`GET /api/elite/pack.php`**: Enforce strict product image integrity (product tiles always return the product's own image URL rather than collection hero banners when linked to a product).
- **Guest -> Account Merge (`SessionMergeService`)**: Automatically merges `app_style_world_feedback`, `app_user_style_signals`, `app_user_preferences`, and recommendation caches on login/register/Google Sign-In/Email OTP without duplicating feedback or issuing double coupons.

---

## 3. Admin CMS Usage Guide (`php_api/admin/index.php`)

1. **Fashion Worlds**:
   - Edit `Audio URL` (`.mp3` / `.m4a` HTTPS stream), toggle `Audio Loop`, adjust `Audio Volume (0.05–1.0)`.
   - Configure `Promo Video URL`, `Poster URL`, and `Sponsored Flag`.
   - Manage **Pixel Runway JSON** (`enabled`, `title`, `subtitle`, `animation_preset`, `speed`, `background_hex`, `display_position`, `collection_slug`, `artwork_url`, `models`).
2. **Occasions**:
   - Pick `Button Style` (`halloween_pumpkin`, `goth_fang`, `y2k_pixel_chrome`, `coquette_ribbon`, `diwali_gold`, `streetwear_utility`, `kfashion_minimal`, `default_minimal`) and `Occasion Audio URL` override.
3. **Editorial Categories**:
   - Configure homepage magazine category cards (`cover_image_url`, `editorial_badge`, `editorial_subtitle`, `accent_hex`, `bg_hex`, `motif_key`, `tile_span`: `regular` vs `wide`, `display_order`).
4. **Luxe Collections**:
   - Manage collections with explicit `layout_type` (`editorial_grid`, `paired_spotlight`, `designer_spotlight`, `squared_archive`) and `featured_product_id`, plus add/reorder/remove products safely via `api/admin/elite.php`.
5. **World Satisfaction Analytics**:
   - Inspect per-world satisfaction average scores, response counts, and coupon redemptions via `api/admin/worlds.php`.
