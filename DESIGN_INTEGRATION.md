# Melvore Android — Backend-Driven Design & Quick-Commerce Integration

This document maps every Melvore Android screen to its live backend endpoints (`https://app.melvore.co.in`, configurable via `API_URL` → `BuildConfig.MELVORE_API_BASE`) and remote design-manifest keys, and lists recommended PHP backend additions for full quick-commerce parity.

---

## 1. Design System Architecture & Theme Precedence

- **Single source of truth**: `GET /api/design/manifest.php?slot=live` (conditional `If-None-Match` ETag support, handling HTTP `304` and `unchanged: true`) + `GET /api/bootstrap.php?config_version=&country=&state=&city=`.
- **Offline fallback**: Cached last-known-good manifest + ETag + `asset_base_url` + `manifest_hash` in `SecureSessionStore`; falls back to bundled `app/src/main/assets/design_system_default.json` when offline.
- **Background refresh**: Periodic foreground refresh (every 5 minutes) plus automatic manifest re-fetch whenever `bootstrap.config_version` changes.
- **CompositionLocals**:
  - `LocalMelvoreDesignManifest` (`DesignManifest`)
  - `LocalDesignKit` (`DesignKit` with `manifest`, `assetBaseUrl`, `manifestHash`, `etag`, `world(slug)`, `url(path)`)
  - `LocalWorld` (`WorldKit?` for the active world's `kit.v2` & `kit.v3` assets)
  - `LocalFashionWorldTheme` (`ResolvedFashionWorldTheme`)
- **Strict theme precedence** (`FashionWorldThemeResolver.resolveWithPrecedence`):
  1. **Route isolation**: `EliteRoute` / `ElitePackRoute` → Luxe Dark; `CheckoutRoute` / `AuthRoute` → Neutral editorial.
  2. **Active Occasion override** (highest priority on quick-commerce surfaces — e.g. Diwali, Christmas, Eid, Black Friday).
  3. **Selected / Preferred Fashion World** (`y2k`, `goth`, `coquette`, `streetwear`, `k-fashion`, `quiet-luxury`, `pixel`, `minimal`, `grunge`, `old-money`, `casual`, `party-wear`) persisted in `SecureSessionStore` & synced via `POST /api/user/preferences.php`.
  4. **Default Theme** (`tokens.colors` / `tokens.colors.dark` when dark mode is enabled).
- **SMIL Animated SVGs**: Rendered via sandboxed `AnimatedSvg` (`ui/design/AnimatedSvg.kt`) using an offline-safe `WebView` with `javaScriptEnabled = false`, `allowFileAccess = false`, `allowContentAccess = false`, lifecycle pause/resume, and automatic static Coil `SvgDecoder` fallback when reduced motion is enabled.
- **Remote Fonts**: `RemoteFontCache` (`ui/theme/RemoteFontCache.kt`) downloads and caches `kit.fonts` `.ttf`/`.otf` files under `cacheDir/melvore_fonts/` and falls back to bundled `res/font/`.

---

## 2. Screen → API Endpoints → Manifest Keys Map

| Screen | Commerce & Discovery Endpoints | Design & Theme Endpoints | Manifest Keys Consumed |
|---|---|---|---|
| **Home (`HomeScreen`)** | `GET /api/home/feed.php`, `GET /design.php?section=all`, `GET /products/list.php`, `GET /products/categories.php`, `GET /api/delivery/check.php`, `GET /cart/list.php`, `POST /cart/add.php`, `POST /cart/update.php`, `POST /cart/remove.php` | `GET /api/design/manifest.php?slot=live`, `GET /api/bootstrap.php`, `GET /api/occasions/active.php`, `GET /api/occasions/list.php`, `GET /api/campaigns/active.php` | `pages.home.sections` (`section_order`), `tokens.colors` & `tokens.colors.dark`, `tokens.motion.duration_ms`, `tokens.spacing`, `tokens.radii`, `components.search_bar.placeholder`, `occasion_overrides.occasions[slug]` (`svg_banner`, `accent_color`, `surface_tint`, `banner_title`, `banner_subtitle`, `banner_cta_text`, `max_ambient_particles`, `fps_cap`, `respect_reduced_motion`), `world_overrides[slug].kit.v3.layouts.home`, `kit.v3.widgets.banner`, `kit.v3.widgets.hero_art` |
| **World Picker (`WorldPickerBottomSheet`)** | `POST /api/user/preferences.php`, `POST /api/style/signal.php` | `GET /api/themes/config.php`, `GET /api/themes/active.php?world_slug=`, `GET /api/bootstrap.php` | `world_overrides[slug]` (`palette`, `typography`, `card_radius_dp`, `kit.fonts`, `kit.v2`, `kit.v3`) |
| **Categories Left-Rail (`CategoriesScreen`)** | `GET /products/categories.php`, `GET /products/list.php`, `GET /design.php?section=all`, `POST /cart/add.php`, `POST /cart/update.php`, `POST /cart/remove.php` | `GET /api/design/manifest.php?slot=live` | `tokens.colors`, `tokens.radii.card_dp`, `components.product_card.corner_radius_dp`, `world_overrides[slug].kit.components.product_card` |
| **Shop (`ShopScreen`)** | `GET /products/list.php`, `GET /products/categories.php` | `GET /api/design/manifest.php?slot=live` | `components.product_card`, `components.button`, `world_overrides[slug].kit.v3.layouts.shop` |
| **Discover & Style Worlds (`DiscoverScreen`, `StyleWorldScreen`)** | `GET /api/discover/for-you.php`, `GET /api/brands/list.php`, `GET /api/styles/list.php`, `GET /api/styles/details.php`, `GET/POST /api/styles/feedback.php` | `GET /api/bootstrap.php`, `GET /api/themes/active.php?world_slug=` | `world_overrides[slug]` (`section_order`, `hero_layout`, `button_skin`, `kit.svg`, `kit.v2`, `kit.v3`, `kit.fonts`) |
| **Product Details (`ProductDetailsScreen`)** | `GET /products/details.php`, `GET /api/products/recommendations.php`, `GET /api/delivery/check.php`, `GET /api/reviews/list.php` | `GET /api/design/manifest.php?slot=live` | `components.button`, `world_overrides[slug].kit.v3.layouts.product`, `kit.v3.widgets.cta_add_to_bag` |
| **Bag & Floating Cart Bar (`BagScreen`, `FloatingCartBar`)** | `GET /cart/list.php`, `POST /cart/update.php`, `POST /cart/remove.php`, `GET /coupons/list.php`, `POST /coupons/validate.php` | `GET /api/design/manifest.php?slot=live` | `tokens.colors`, `components.button`, `world_overrides[slug].kit.v3.layouts.bag`, `kit.v3.widgets.coupon`, `kit.v3.widgets.tile_bag` |
| **Checkout (`CheckoutScreen`)** | `GET /addresses/list.php`, `POST /addresses/save.php`, `POST /coupons/validate.php`, `POST /orders/create.php`, `POST /payments/razorpay/initiate.php`, `POST /payments/razorpay/verify.php`, `POST /payments/razorpay/fail.php` | Neutral checkout isolation (`isNeutralCheckoutOrAuthRoute = true`) | `tokens.colors` (neutral baseline), `components.button` |
| **Order Tracking (`OrderTrackingScreen`)** | `GET /orders/track.php`, `GET /orders/history.php`, `GET /orders/details.php`, `POST /orders/cancel.php` | `GET /api/design/manifest.php?slot=live` | `world_overrides[slug].kit.v3.widgets.order_steps` (rendered via `AnimatedSvg` above Shipment Journey timeline when present) |
| **Bottom Navigation (`MelvoreBottomBar`)** | Cart badge from `GET /cart/list.php` | `GET /api/design/manifest.php?slot=live` | `navigation.tabs`, `navigation.bottom_bar_variant`, `navigation.bottom_bar_radius_dp`, `kit.v2.icons[route]`, `kit.icons[route]`, `kit.v2.components.navbar`, `kit.v2.components.nav_indicator`, `kit.v3.widgets.nav_live` |

---

## 3. Missing PHP Backend Endpoints & Fields (Recommended Additions)

Inspection of `php_api/` revealed the following gaps where the Android app already parses and consumes the keys gracefully (with clean fallbacks) and will automatically activate richer behavior once populated by the PHP backend:

1. **`GET /api/design/manifest.php`** (`php_api/api/design/manifest.php`):
   - **Missing `data.manifest_hash`** (SHA-256 hash of manifest JSON) and **`data.asset_base_url`** (canonical CDN/origin URL for resolving relative SVG/font paths).
   - **Missing `?slot=live|draft|default` query parameter handling** (currently reads slot only from `site_settings.active_design`).
   - **HTTP 304 Not Modified**: Currently returns HTTP `200` with `{"unchanged": true}` when `If-None-Match` matches; returning standard `304 Not Modified` (with empty body and `ETag` header) reduces payload overhead. (The Android client handles both `304` and `200 + unchanged: true`.)
2. **`design-system/live/manifest.json`** (`php_api/design-system/live/manifest.json`):
   - Currently at schema `1.2.0` with `kit.svg` (9 SVGs) and `kit.icons` (5 icons).
   - **Missing `tokens.colors.dark`** object (`background`, `surface`, `surface_variant`, `primary`, `secondary`, `accent`, `text_primary`, `text_secondary`, `border`) for backend-driven dark mode.
   - **Missing `world_overrides[slug].kit.v2`** (`components`, `sizes`, `icons` for all 10 keys: `home`, `discover`, `videos`, `elite`, `bag`, `heart`, `search`, `profile`, `filter`, `bell`) and **`kit.v3`** (`layouts`: `home`, `shop`, `product`, `bag`; `widgets`: `bg_ambient`, `topbar`, `search`, `banner`, `section_header`, `product_card`, `coupon`, `order_steps`, `cta_add_to_bag`, `nav_live`, `hero_art`, etc.; `widget_sizes`).
3. **`GET /design.php?section=all` & `GET /api/home/feed.php`** (`site_settings` object):
   - **Missing `delivery_eta_text` / `delivery_eta_mins`** in `site_settings` for city/store-level quick-commerce ETA badges before a user enters a 6-digit pincode (currently only `GET /api/delivery/check.php?pincode=` returns `estimated_days_min`/`estimated_days_max`/`delivery_message`).
4. **`GET /api/home/feed.php`**:
   - **Missing dedicated `frequently_bought` product rail** (currently the app maps `bestsellers` to the "Frequently bought" rail).
   - **Missing dedicated `home_tabs` array** (currently the app derives the top scrollable occasion/category strip from `GET /api/occasions/list.php`, `bootstrap.active_occasion`, `manifest.occasion_overrides`, and `collections`).
