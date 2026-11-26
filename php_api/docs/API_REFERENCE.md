# Melvore PHP 8.3 REST API Reference

**Base URL:** `https://app.melvore.co.in`  
**Storefront & Media Origin:** `https://melvore.co.in`  
**Auth Header:** `Authorization: Bearer <hmac_token>`  
**Guest Header:** `X-Guest-Id: mlv_<hex>`

All endpoints return JSON with `success`, `message`, optional `error_code`, and payload in `data` (and top-level keys where Android legacy Retrofit DTOs read top-level fields).

## 1. Health, Bootstrap & Dynamic Theme Engine
- `GET /index.php` — API service manifest & endpoint map.
- `GET /api/health.php` — DB connectivity & active product count probe.
- `GET /design.php?section=all` — Public storefront bundle (strips all payment secrets).
- `GET /api/bootstrap.php?config_version=&country=&state=&city=` — Versioned lightweight manifest returning `default_theme`, `active_occasion`, `occasions`, `fashion_worlds`, `world_assets`, `world_map_origins`, `smart_offers`, and synced `user_preferences`.
- `GET /api/themes/config.php` — Theme definitions + vector assets.
- `GET /api/themes/active.php?world_slug=&country=&state=&city=` — Resolved layered theme hierarchy.
- `GET /api/occasions/list.php` & `GET /api/occasions/active.php` — Occasion & festival schedules.
- `GET /api/campaigns/active.php` & `GET /api/campaigns/details.php?id=&slug=` — Regional & seasonal campaigns.

## 2. Storefront Discovery, Style Worlds, World Map & Offers
- `GET /api/home/feed.php` — Full personalized editorial home feed.
- `GET /api/discovery/feed.php?world_slug=` — Fashion Worlds curated feed with attached products & active occasions.
- `GET /api/discovery/world_map.php` — Verified brand origin map pins (`app_brand_origins`).
- `GET /api/styles/list.php`, `GET /api/styles/details.php?id=&slug=`, `POST /api/styles/signal.php`.
- `GET /api/brands/list.php?tab=`, `GET /api/brands/details.php?id=&slug=`, `POST /api/brands/view.php`.
- `GET /api/categories/list.php`, `GET /api/categories/details.php`.
- `GET /api/offers/eligible.php?world_slug=&cart_subtotal=` — Evaluates active Smart Offers & real coupon rules obeying cooldowns/caps.
- `POST /api/offers/impression.php` — Logs `shown`, `dismissed`, or `applied`.

## 3. Catalog, Search, Delivery & Reviews
- `GET /api/products/list.php`, `GET /api/products/details.php?id=`, `GET /api/products/categories.php`, `GET /api/products/recommendations.php?product_id=`, `GET /api/products/recently_viewed.php`.
- `GET /api/search/query.php?q=`, `GET /api/search/suggestions.php?q=`.
- `GET /api/delivery/check.php?pincode=&product_id=`.
- `GET /api/reviews/list.php`, `POST /api/reviews/request_otp.php`, `POST /api/reviews/verify_otp.php`, `POST /api/reviews/create.php`, `POST /api/reviews/helpful.php`, `POST /api/reviews/upload_media.php`.

## 4. Auth, Preferences, Bag, Coupons, Wishlist & Addresses
- `POST /api/auth/login.php`, `register.php`, `social.php` (Google ID token), `otp.php`, `forgot_password.php`, `logout.php`.
- `GET|PUT /api/auth/profile.php`.
- `GET|POST|PUT /api/users/preferences.php` — Audio/motion/world preferences.
- `GET /api/cart/list.php`, `POST /api/cart/add.php`, `update.php`, `POST|DELETE /api/cart/remove.php`.
- `GET|POST /api/coupons/validate.php`.
- `GET /api/wishlist/list.php`, `POST /api/wishlist/add.php`, `POST|DELETE /api/wishlist/remove.php`.
- `GET /api/addresses/list.php`, `POST /api/addresses/save.php`, `POST|DELETE /api/addresses/delete.php`.

## 5. Orders & Razorpay/COD Payments
- `POST /api/orders/create.php` (Auth, `Idempotency-Key`, atomic `FOR UPDATE` stock reservation, `cod` or `online`).
- `GET /api/orders/history.php`, `details.php`, `track.php`, `POST /api/orders/cancel.php` (auto Razorpay refund on paid orders).
- `POST /api/payments/initiate.php`, `verify.php` (HMAC + Razorpay REST API capture verify), `fail.php`, `webhook_razorpay.php`.

## 6. Blog, CMS, Support, Contact, Uploads & Analytics
- `GET /api/blog/list.php`, `details.php`; `GET /api/cms/faqs.php`, `page.php`.
- `GET /api/support/tickets.php`, `POST /api/support/create.php`, `reply.php`, `upload_media.php`.
- `GET /api/help/topics.php`, `flow.php`, `POST /api/help/session.php`.
- `POST /api/forms/contact.php`, `POST /api/analytics/track.php`, `POST /api/uploads/upload.php`.

## 7. Admin Management (Requires `users.role IN ('admin','superadmin','merchandiser')`)
- `GET|POST|PUT|DELETE /api/admin/worlds.php`
- `GET|POST|PUT|DELETE /api/admin/occasions.php`
- `GET|POST|DELETE /api/admin/assets.php`
- `GET|POST|DELETE /api/admin/offers.php`
