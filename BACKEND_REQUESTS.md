# Backend Requests & Deployment Checklist for `Mevlore_backend` (PHP 8.3 + MariaDB)

The Android app now strictly reports every backend error (HTTP 5xx, HTTP 200 with `success: false`, empty/missing tables via `data.diagnostics`, and manifest fallbacks) in **Profile → About & Design System → Connection Diagnostics** (visible in all build types).

---

## URGENT — Observed Live Host Issue (`https://app.melvore.co.in`)
During automated live verification, `GET https://app.melvore.co.in/api/health.php` returned **`HTTP 500` with a `0-byte` response body**. On PHP 8.3 (Hostinger / Apache), a 0-byte HTTP 500 typically indicates a fatal PHP error before output buffering flushes. Check the server's `error_log` for:
1. Missing `/services/` directory at the document root alongside `/api/` and `/config/` (many `api/*.php` scripts `require_once __DIR__ . '/../../services/...'`).
2. Missing or unreadable `.env` file (`DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASS`, `APP_URL`, `STOREFRONT_URL`) causing PDO connection failure or unhandled exception.
3. Missing `/design-system/live/manifest.json` and `/design-system/assets/svg/` directories on the API host.
4. PHP 8.3 strict type or `pdo_mysql` extension error.

**Quick Host Verification Commands (SSH / Hostinger File Manager):**
```bash
# 1. Verify required top-level directories exist in app.melvore.co.in docroot:
ls -la api/ config/ services/ design-system/ design.php index.php .env

# 2. Check PHP error log immediately after hitting health.php:
curl -i https://app.melvore.co.in/api/health.php
curl -i "https://app.melvore.co.in/api/design/manifest.php?slot=live"
curl -i https://app.melvore.co.in/design-system/live/manifest.json
```

---

## Exact MariaDB Schema Migration Run Order (8 Steps)
If Connection Diagnostics → **Home Feed Server Diagnostics** reports entries in `missing_tables` or `empty_modules`, apply the `Mevlore_backend` SQL scripts in this exact order on your MariaDB database:

1. Core storefront tables (`users`, `categories`, `brands`, `collections`, `products`, `product_images`, `product_variants`, `carts`, `orders`, `coupons`, `wishlists`, `addresses`, `site_settings`, `hero_slides`, `promo_banners`, `faqs`, `blogs`).
2. Discovery & Style Worlds tables (`style_worlds`, `style_world_products`, `style_signals`, `style_world_feedback`, `home_sections`, `home_section_items`).
3. Campaigns & Festivals tables (`campaigns`, `campaign_assets`, `occasions`, `occasion_rules`).
4. Design System & Theme Engine tables (`design_slots`, `design_manifests`, `theme_configs`, `fashion_world_themes`).
5. Smart Offers & World Map tables (`smart_offers`, `offer_impressions`, `world_map_origins`).
6. Reels & Luxe tables (`videos`, `video_events`, `elite_packs`, `elite_pack_products`).
7. Verified Reviews & Delivery tables (`product_reviews`, `review_otps`, `review_media`, `serviceable_pincodes`).
8. Help Center, Support & User Preferences tables (`help_topics`, `help_flows`, `help_sessions`, `support_tickets`, `user_preferences`).

---

## Endpoint Contract Verification Checklist

### 1. `GET /api/design/manifest.php?slot=live`
- **HTTP `ETag` header + `If-None-Match` → `304 Not Modified`**:
  - Compute `ETag` from the active manifest hash (`sha256` of `manifest.json` + DB slot timestamp).
  - Return `HTTP 304 Not Modified` when `If-None-Match` matches (note: the Android app never sends `If-None-Match` on first install and retries without it if `304` arrives with an empty cache).
- **Required fields inside `data`**:
  - `active_design` (`0` = default, `1` = live)
  - `etag` (matching the HTTP `ETag` header)
  - `manifest_hash` (SHA-256 hex digest)
  - `asset_base_url` (absolute HTTPS origin for `/design-system/assets/svg/...`, e.g., `"https://app.melvore.co.in"`)
  - `manifest` (full **v1.6.0** JSON object containing `tokens`, `navigation`, `components`, `media`, `pages`, `worlds` with `kit.v2` & `kit.v3`, and `occasion_overrides`)
- **Dark-mode tokens inside `manifest.tokens.colors.dark`**:
  - Include `dark` color tokens so switching Appearance to **Dark** uses DB-authored dark surfaces.

### 2. `GET /api/bootstrap.php?config_version=`
- Return deterministic `config_version`, `active_design`, `design_slot`, `design_version`, `design_manifest`, `fashion_worlds` (with explicit `"mode": "light" | "dark"`), `active_occasion`, `smart_offers`, and `world_map_origins`.

### 3. `GET /api/home/feed.php`
- Return `{ "success": true, "data": { ... } }` including `hero_slides`, `promo_banners`, `categories`, `brands`, `style_worlds`, `sections`, `discovery_products`, `new_arrivals`, `bestsellers`, `trending_products`, and `diagnostics: { "missing_tables": [], "empty_modules": [] }`.
- Always return `"success": false` with a clear `"message"` (and HTTP 4xx/5xx where appropriate) on DB/query errors — the Android app now surfaces HTTP 200 `success: false` directly as an error.

### 4. `GET /api/themes/config.php`, `GET /api/themes/active.php`, `GET /api/occasions/active.php`, `GET /api/occasions/list.php`, `GET /api/campaigns/active.php`
- Use consistent `{ "success": true, "message": "...", "data": { ... } }` envelope.
- Include valid `#RRGGBB` hex colors and explicit `mode` on all 12 fashion worlds.

### 5. Static Assets & Media Origins
- **Design System SVGs (`/design-system/assets/svg/...`)** on `https://app.melvore.co.in`: serve with `Content-Type: image/svg+xml`, `Cache-Control: public, max-age=86400`, and `Access-Control-Allow-Origin: *`.
- **Storefront Product/Banner Uploads (`uploads/...`)** on `https://melvore.co.in`: ensure uploaded images are publicly readable over HTTPS (Android resolves relative `uploads/...` paths against `https://melvore.co.in` and logs any Coil image load failure into Connection Diagnostics).
