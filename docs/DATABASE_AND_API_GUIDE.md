# Melvore — Database Compatibility Report & REST API Guide

**Database:** `u205121999_melvore` (MariaDB / MySQL on Hostinger)  
**Shared Storefront:** `https://melvore.co.in`  
**PHP 8.3 REST API Subdomain:** `https://app.melvore.co.in` (`/php_api`)  
**Source Schema Reference:** [`php_api/sql/melvore_production_schema_reference.sql`](../php_api/sql/melvore_production_schema_reference.sql) (`57` production tables)  
**Additive Migration Script:** [`php_api/sql/melvore_safe_migration.sql`](../php_api/sql/melvore_safe_migration.sql)

---

## 1. Executive Database Audit Summary

1. **Zero Separate Mobile Database**: All Flutter mobile endpoints in `/php_api` query and mutate the existing `u205121999_melvore` schema shared with `https://melvore.co.in/`.
2. **Non-Destructive Guarantee**: Neither `melvore_safe_migration.sql` nor any PHP endpoint executes `DROP TABLE`, `TRUNCATE`, or destructive table consolidation. All changes in `melvore_safe_migration.sql` are additive and guarded by `information_schema` checks so re-running the script is safe.
3. **Never Executed Against Production**: Migrations have **not** been run against the live database; export a full phpMyAdmin `.sql` backup before running [`php_api/sql/melvore_safe_migration.sql`](../php_api/sql/melvore_safe_migration.sql).

### Critical Schema Findings Resolved Across PHP & SQL

| Area | Audit Finding | Implemented Resolution |
|---|---|---|
| **Cart Tables (`user_cart` / `cart` / `carts`)** | `user_cart` (`user_cart_id` PK, `user_id NOT NULL` FK) is the canonical authenticated customer cart table; `cart` supports nullable `user_id` + `session_id` for guests; `carts` is a legacy table whose `unique_cart_item` index omitted `(color, size)`, causing `1062 Duplicate entry` on multi-variant carts. Meanwhile, `orders/create.php` previously called a nonexistent `hydrateFromCompanionTables` method and queried `FROM carts c` with nonexistent `c.attributes`. | Standardized authenticated customers (`user_id > 0`) on **`user_cart`**, guests (`X-Guest-Id`) on **`cart` (`user_id IS NULL`)**, and merged guest rows into `user_cart` on login/register/OTP/social sign-in (`CartHelper`). Fixed `orders/create.php` to lock `user_cart` (`FOR UPDATE`) and added `Section 6` in `melvore_safe_migration.sql` to safely add `(color, size)` to legacy `carts.unique_cart_item`. |
| **Coupons Column Names (`coupons`)** | Production `coupons` table uses `usage_count` and `valid_to` (NOT `used_count` / `valid_until`). `coupons/validate.php` and `payments/fail.php` previously referenced broken column/method names (`used_count`, `PricingEngine::validateCoupon()`, `$summary['coupon_error']`). | Updated `coupons/validate.php` and `payments/fail.php` to use `usage_count`, `valid_to`, `PricingEngine::calculateTotals()`, and `coupon_id === null` / `coupon_message`. |
| **Cashfree `payment_sessions` Truncation** | `payment_sessions.payment_session_id` was `VARCHAR(100)`, truncating Cashfree PG `2023-08-01` session tokens (106–128 chars) under `STRICT_TRANS_TABLES`. | `melvore_safe_migration.sql` Section 1 widens `payment_session_id` to `VARCHAR(255) NOT NULL`; `payments/initiate.php` checks live `CHARACTER_MAXIMUM_LENGTH` before inserting and preserves full token in `orders.payment_details`. |
| **`payments` & `orders` Linking** | `payments` (`id` PK) lacked an `order_id` column and lookup indexes (`razorpay_order_id`, `(user_id, order_id)`). `orders` lacked `(user_id, created_at)` index and dedicated nullable `refund_status` / `refund_reference` audit columns. | `melvore_safe_migration.sql` Sections 3, 4, and 9 add nullable `payments.order_id`, composite indexes, and nullable `orders.refund_status` / `orders.refund_reference`. PHP code uses `MelvoreDB::columnExists()` so queries never crash even before migration runs. |
| **Missing PK & Catalog Indexes** | `homepage_sections` had `UNIQUE KEY section_id` + `AUTO_INCREMENT` without a formal `PRIMARY KEY`; `products` lacked indexes on `slug`, `sku`, `brand_id`, and `(is_active, is_featured)`. | `melvore_safe_migration.sql` Sections 2 and 5 add these safely if missing. |
| **Webhook Replay & Concurrency** | `payment_webhook_events` lacked `status` & `updated_at` on older installs, and webhook handlers risked locking out retries if an exception occurred mid-processing. | Added `status` & `updated_at` (Sections 7–8) and moved event lock/insert inside the DB transaction (`FOR UPDATE`) so rollback preserves gateway retryability. |

---

## 2. Complete Table Compatibility Matrix (All 57 Reference Tables + 3 Additive API Tables)

### A. Core Commerce, Auth, Cart, Orders & Payments

| Table | Primary Key (`AUTO_INCREMENT`) | Foreign Keys & Unique Constraints | Key Required (`NOT NULL`) Columns | Key Nullable / Default Columns & Relationship Notes |
|---|---|---|---|---|
| `users` | `user_id` (`int(11)`) | Unique: `email`, `google_id`, `instagram_id` | `name`, `email`, `password_hash` (bcrypt), `mobile_primary`, `remember_token` (`int(11)`, default `0` supplied on insert), `reason` (`varchar(255)`, default `''` supplied on insert) | `mobile_alternate`, `profile_image`, `avatar`, `email_verified (0)`, `mobile_verified (0)`, `is_subscribed (0)`, `account_status ('active')`, `otp`, `otp_expires`, `social_provider ('none')`, `last_login`, `created_at`, `updated_at`. |
| `user_addresses` | `address_id` (`int(11)`) | FK: `user_id -> users.user_id`; Index: `user_id` | `user_id`, `full_name`, `address_line1`, `city`, `state`, `pincode`, `mobile` | `address_line2 (NULL)`, `country ('India')`, `is_default (0)`, `address_type ('home'|'work'|'other')`, `created_at`. |
| `categories` | `category_id` (`int(11)`) | Unique: `slug`; Self-FK/parent index: `parent_id` | `name`, `slug` | `description`, `parent_id`, `image`, `display_order (0)`, `is_featured (0)`, `status ('active'|'hidden'|'deleted')`, SEO meta columns. |
| `brands` | `brand_id` (`int(11)`) | Unique: `slug` | `name`, `slug` | `description`, `logo`, `status ('active'|'inactive')`. |
| `collections` | `id` (`int(11)`) | Index: `display_order` | `name` | `subtitle`, `slug`, `banner`, `is_active (1)`, `display_order (0)`. |
| `products` | `product_id` (`int(11)`) | Index: `category_id` (+ additive `idx_products_slug`, `idx_products_sku`, `idx_products_brand_id`, `idx_products_active_featured`) | `sku`, `img_src` (`\|`-delimited), `name`, `special_icon`, `slug`, `description`, `category_id`, `regular_price` | `brand_id`, `sale_price`, `cost_price`, `stock_quantity (0)`, `low_stock_threshold (5)`, `is_featured (0)`, `is_active (1)`, `has_variations (0)`, `view_count (0)`, `purchase_count (0)`, `rating_average (0.00)`, `rating_count (0)`, JSON `available_colors`, JSON `available_sizes`, `default_color`, `default_size`. |
| `product_variations` | `variation_id` (`int(11)`) | FK/Index: `product_id -> products.product_id` | `product_id`, `attribute_name`, `attribute_value` | `sku_suffix`, `additional_price (0.00)`, `stock_quantity (0)`, `image_url`, `is_active (1)`, `is_default (0)`, `color`, `size`, `sale_price`, `regular_price`, `sku`. |
| `user_cart` *(Canonical Auth)* | `user_cart_id` (`int(11)`) | FK: `user_id -> users.user_id`; Unique: `(user_id, product_id, variation_id, color, size)` | `user_id`, `product_id`, `quantity (1)` | `variation_id`, `color`, `size`, `added_at`. Canonical source of truth for logged-in website & mobile customers. |
| `cart` *(Canonical Guest)* | `cart_id` (`int(11)`) | Index: `session_id`, `user_id`, `product_id` | `product_id` | `user_id (NULL)`, `session_id`, `variation_id`, `quantity (1)`, `color`, `size`, `added_at`. Used exclusively for guest (`X-Guest-Id`) carts before login merge. |
| `carts` *(Legacy Web)* | `cart_id` (`int(11)`) | Unique: `unique_cart_item` (migrated to include `color, size`) | `product_id`, `quantity (1)`, `price` | `user_id`, `session_id`, `variation_id`, `color`, `size`, `attributes`, `image`, `added_at`. Never dropped; auto-consolidated once if `user_cart` is empty. |
| `wishlist` | `wishlist_id` (`int(11)`) | Unique/Index: `(user_id, product_id)` | `user_id`, `product_id` | `added_at`. |
| `coupons` | `coupon_id` (`int(11)`) | Unique: `code` | `code`, `discount_value`, `shw (0)` | `description`, `discount_type ('percentage'|'fixed'|'free_shipping')`, `min_order_amount (0.00)`, `max_discount_amount`, `usage_limit`, `usage_count (0)`, `per_user_limit (1)`, `valid_from`, `valid_to`, `is_active (1)`, JSON `conditions`, JSON `payment_methods`, `item_count_min (0)`, `item_count_max`, JSON `categories`. |
| `orders` | `order_id` (`int(11)`) | Unique: `order_number`; Index: `user_id`, `shipping_address_id` (+ additive `idx_orders_user_created`) | `order_number`, `subtotal`, `total_amount` | `user_id`, `guest_email`, `guest_name`, `shipping_address_id`, `billing_address_id`, `shipping_charge (0.00)`, `tax_amount (0.00)`, `discount_amount (0.00)`, `cod_fee (0.00)`, `payment_method ('cod'|'online'|'wallet')`, `payment_status ('pending'|'paid'|'failed'|'refunded')`, `payment_details` (`longtext`/JSON), `payment_transaction_id`, `refund_status` *(additive)*, `refund_reference` *(additive)*, `order_status ('pending'|'processing'|'shipped'|'delivered'|'cancelled'|'refunded')`, `coupon_id`, `coupon_code`, `is_subscription_applied`, `notes`, `cancellation_reason`, `estimated_delivery_date`, `tracking_number`, `courier_name`, `parent_order_id`, `is_combined_order`, `tracking_url`, `estimated_delivery`. |
| `order_items` | `order_item_id` (`int(11)`) | FK/Index: `order_id -> orders.order_id`, `product_id` | `order_id`, `product_id`, `product_name`, `sku`, `quantity`, `unit_price`, `total_price` | `variation_id`, JSON `attributes` (`{"color":...,"size":...}`). |
| `payments` *(Razorpay)* | `id` (`int(11)`) | Index: `idx_payments_rzp_order_id`, `idx_payments_user_order` *(additive)* | `user_id`, `razorpay_order_id`, `razorpay_payment_id`, `amount` | `order_id` *(additive nullable FK-style link)*, `status ('pending'|'completed'|'failed')`, `created_at`. |
| `payment_sessions` *(Cashfree Web)* | `id` (`int(11)`) | Index: `idx_payment_sessions_cf_order`, `idx_payment_sessions_order_id` *(additive)* | `order_id`, `cf_order_id`, `payment_session_id` *(widened to `VARCHAR(255)`)*, `amount` | `status ('pending'|'completed'|'failed')`, `created_at`. |
| `reviews` | `review_id` (`int(11)`) | Index: `product_id`, `user_id`, `order_item_id` | `product_id`, `user_id`, `order_item_id`, `rating (1..5)`, `comment` | `title`, `is_approved (0)`, `is_verified_purchase (0)`, `helpful_count (0)`, `created_at`, `updated_at`. |

### B. CMS, Blog, Style Quiz, Support, Chat, Analytics & Additive API Tables

| Group | Tables (`PRIMARY KEY`) | Notes & Required Column Safeguards |
|---|---|---|
| **Blog (`3`)** | `blog_categories` (`id`), `blog_posts` (`id`), `blog_comments` (`id`) | Served by `/api/blog/{list,details,comment}.php`. `blog_posts.status = 'published'` filtered publicly. |
| **Storefront CMS & Design (`14`)** | `about_content` (`id`), `banner_content` (`banner_id`), `hero_content` (`id`), `hero_marquee` (`marquee_id`), `marquee_items` (`id`), `feature_tiles` (`feature_id`), `testimonials` (`testimonial_id`), `homepage_sections` (`section_id` PK added in migration), `cms_pages` (`id`), `faqs` (`faq_id`), `contact_settings` (`id`), `site_stats` (`stat_id`), `site_settings` (`setting_id`), `team_members` (`id`), `milestones` (`id`), `trending_tags` (`tag_id`) | Served by `GET /design.php` and `/api/cms/{page,faqs}.php`. Server strips all 7 payment gateway secret/config keys (`razorpay_*`, `cashfree_*`) from `site_settings` public output. `testimonials` filters `is_approved = 1`. |
| **Style Quiz (`4`)** | `body_types` (`id`), `style_categories` (`id`), `style_options` (`id`), `user_style_preferences` (`pref_id`) | Served by `/api/style/{quiz,save}.php` & `design.php`. `user_style_preferences.selected_options` stores JSON option IDs. |
| **Forms & Support (`3`)** | `contact_messages` (`id`), `support_tickets` (`ticket_id`), `ticket_replies` (`reply_id`) | `support_tickets` requires `ticket_number`, `subject`, `category`, `message`. Managed by `/api/forms/contact.php` and `/api/support/{tickets,create,reply}.php`. |
| **Live Chat (`5`)** | `chat_conversations` (`conversation_id`), `chat_messages` (`message_id`), `chat_ratings` (`rating_id`), `chat_sessions` (`session_id`), `guest_sessions` (`session_id`) | `chat_conversations.updated_at` (`varchar(255) NOT NULL`) and `chat_sessions.ip_address` / `user_agent` (`NOT NULL`) are explicitly populated by `/api/chat/{session,send,rate}.php`. |
| **Analytics & Tracking (`9`)** | `analytics_active_sessions` (`session_id`), `analytics_events` (`event_id`), `analytics_page_views` (`view_id`), `analytics_visitors` (`visitor_id`), `tracking_events` (`event_id`), `tracking_location_cache` (`ip_address`), `tracking_page_views` (`view_id`), `tracking_product_views` (`view_id`), `tracking_sessions` (`session_id`) | Managed by `MelvoreTracker` (`/php_api/includes/tracker.php`) via `POST /api/analytics/track.php`. `int(11)` visitor IDs widen losslessly into `bigint(20)`. |
| **Additive API Tables (`3`)** | `api_tokens` (`token_id`), `api_order_idempotency` (`id`), `payment_webhook_events` (`webhook_id`) | Created via `CREATE TABLE IF NOT EXISTS` in `melvore_safe_migration.sql`. Handle token revocation, idempotent checkout (`Idempotency-Key`), and transactional payment webhook dedupe. |

---

## 3. Safe SQL Migration Prerequisites & Rollback

### Prerequisites (Mandatory Before Running SQL)
1. Open **Hostinger hPanel → Databases → phpMyAdmin → `u205121999_melvore`**.
2. Click **Export → Custom → Format: SQL → Go** and save a verified backup locally.
3. Review [`php_api/sql/melvore_safe_migration.sql`](../php_api/sql/melvore_safe_migration.sql) and execute it in the **SQL** tab only after confirming the backup succeeded.

### Rollback SQL (If Reverting Only Additive Migration Objects)
> Note: Only revert additive objects if no application code referencing them is still deployed. Widening `payment_sessions.payment_session_id` to `VARCHAR(255)` should **not** be shrunk back to `100` because existing/live tokens >100 chars would truncate.

```sql
-- Rollback ONLY additive columns, indexes, and newly added API support tables
ALTER TABLE `orders`
  DROP COLUMN IF EXISTS `refund_reference`,
  DROP COLUMN IF EXISTS `refund_status`,
  DROP INDEX IF EXISTS `idx_orders_user_created`;

ALTER TABLE `payments`
  DROP INDEX IF EXISTS `idx_payments_user_order`,
  DROP INDEX IF EXISTS `idx_payments_rzp_order_id`,
  DROP COLUMN IF EXISTS `order_id`;

ALTER TABLE `payment_sessions`
  DROP INDEX IF EXISTS `idx_payment_sessions_order_id`,
  DROP INDEX IF EXISTS `idx_payment_sessions_cf_order`;

ALTER TABLE `products`
  DROP INDEX IF EXISTS `idx_products_active_featured`,
  DROP INDEX IF EXISTS `idx_products_brand_id`,
  DROP INDEX IF EXISTS `idx_products_sku`,
  DROP INDEX IF EXISTS `idx_products_slug`;

DROP TABLE IF EXISTS `payment_webhook_events`;
DROP TABLE IF EXISTS `api_order_idempotency`;
DROP TABLE IF EXISTS `api_tokens`;
```

---

## 4. Flutter ↔ PHP REST API Endpoint Contract (`49` Routes)

Base URL: `https://app.melvore.co.in` | Auth Header: `Authorization: Bearer <hmac_token>` | Guest Cart Header: `X-Guest-Id: mlv_<hex>`

### Health & Design Hub (`2`)
- `GET /api/health.php` — API liveness & DB connectivity probe.
- `GET /design.php?section=all` — Storefront CMS bundle (`hero_slides`, `promo_banners`, `marquee_items`, `feature_tiles`, `categories`, `brands`, `collections`, `featured_products`, `testimonials`, `faqs`, `about`, `site_settings`, `style_quiz`). All payment secrets stripped.

### Authentication & Profile (`7`)
- `POST /api/auth/login.php` — Email/password (`password_verify`), merges guest `cart` into `user_cart`.
- `POST /api/auth/register.php` — Creates `users` row (`PASSWORD_BCRYPT`), merges guest `cart`, issues Bearer token.
- `POST /api/auth/logout.php` — Revokes token in `api_tokens` + file store.
- `GET / PUT /api/auth/profile.php` — Profile details, live `user_cart` + `wishlist` + pending order counts, and `addresses`.
- `POST /api/auth/otp.php` — 6-digit OTP request & verification (`users.otp`, `otp_expires`), merges guest cart.
- `POST /api/auth/forgot_password.php` — OTP password reset (`users.password_hash` update invalidates older Bearer tokens via `pvf` digest).
- `POST /api/auth/social.php` — Google sign-in requiring server-verified `id_token` (`tokeninfo` + `GOOGLE_CLIENT_IDS` audience check) or `access_token` (`oauth2/v3/userinfo` + `email_verified = true`).

### Products, Categories & Reviews (`6`)
- `GET /api/products/list.php` — Paginated catalog (`?page`, `limit`, `category`, `brand_id`, `search`, `min_price`, `max_price`, `in_stock`, `featured`, `on_sale`, `sort=newest|price_asc|price_desc|popular|rating`).
- `GET /api/products/details.php` — Product detail + `variations`, approved `reviews`, and up to 6 active same-category `related_products`.
- `GET /api/products/categories.php` — Active `categories`, `brands`, and `collections`.
- `GET /api/reviews/list.php`, `POST /api/reviews/create.php` *(Auth)*, `POST /api/reviews/helpful.php`.

### Cart, Coupons & Wishlist (`8`)
- `GET /api/cart/list.php`, `POST /api/cart/add.php`, `POST /api/cart/update.php`, `POST|DELETE /api/cart/remove.php` — Uses `user_cart` for authenticated customers, `cart` (`user_id IS NULL`) for guests, priced server-side via `PricingEngine`.
- `GET|POST /api/coupons/validate.php` — Lists active coupons (`GET`) or validates `coupon_code` against live cart/subtotal using `PricingEngine::calculateTotals()` (`valid_to`, `usage_count`, category/payment/item-count/first-order rules).
- `GET /api/wishlist/list.php`, `POST /api/wishlist/add.php`, `POST|DELETE /api/wishlist/remove.php` *(Auth)*.

### Addresses & Orders (`8`)
- `GET /api/addresses/list.php`, `POST /api/addresses/save.php` (create/edit & `is_default`), `POST|DELETE /api/addresses/delete.php` *(Auth)*.
- `POST /api/orders/create.php` *(Auth, supports `Idempotency-Key`)* — Consolidates residual cart, locks `user_cart` + `products` + `product_variations` (`FOR UPDATE`), validates optional `coupon_code` (rejecting invalid non-empty coupons with `422`), deducts inventory, sets `payment_status = 'pending'`, clears cart immediately only on COD.
- `GET /api/orders/history.php`, `GET /api/orders/details.php` *(Auth)*, `GET /api/orders/track.php` *(Auth or order_number + pincode/mobile check)*, `POST /api/orders/cancel.php` *(Auth — restores stock/coupon count + triggers Razorpay refund API if already paid)*.

### Payments & Server Webhooks (`5`)
- `POST /api/payments/initiate.php` *(Auth)* — Locks `orders ... FOR UPDATE`, enforces minimum `₹1.00`, creates Razorpay order (mobile) or Cashfree session (website).
- `POST /api/payments/verify.php` *(Auth)* — Locks `orders ... FOR UPDATE`, enforces `order_status !== 'cancelled'`, checks order ↔ gateway order ID binding + HMAC signature + server-side Razorpay API `GET /v1/payments/{id}` (`status === 'captured'`, auto-capturing `authorized` if needed, matching `order_id`, `amount`, `INR`).
- `POST /api/payments/fail.php` *(Auth)* — Leaves unpaid order retryable (`action: "fail"`) or cancels unpaid order atomically restoring both `products` & `product_variations` stock and `coupons.usage_count` (`action: "cancel_order"`).
- `POST /api/payments/webhook_razorpay.php` & `POST /api/payments/webhook_cashfree.php` — Signature-verified, transactionally deduplicated (`payment_webhook_events FOR UPDATE`), protects cancelled orders from late capture.

### Content, Style Quiz, Support, Chat, Uploads & Analytics (`13`)
- `GET /api/cms/page.php`, `GET /api/cms/faqs.php`
- `GET /api/blog/list.php`, `GET /api/blog/details.php`, `POST /api/blog/comment.php`
- `GET /api/style/quiz.php`, `POST /api/style/save.php` *(Auth)*
- `POST /api/forms/contact.php`
- `GET /api/support/tickets.php`, `POST /api/support/create.php`, `POST /api/support/reply.php` *(Auth)*
- `GET|POST /api/chat/session.php`, `POST /api/chat/send.php`, `POST /api/chat/rate.php`
- `POST /api/uploads/upload.php` *(Auth, MIME-checked multipart)*
- `POST /api/analytics/track.php`

---

## 5. Local Express Preview API (`/server.ts`)

Running `npm run dev` starts the development storefront & JSON preview backend on `http://localhost:3000`:
- `GET /api/health`, `/api/design`, `/api/discovery`, `/api/catalog`
- `POST /api/auth/{login,register,google,logout}`, `GET /api/auth/me`, `PUT /api/user/{profile,password,settings}`
- `GET|POST /api/cart`, `PATCH|DELETE /api/cart/:cartId`, `GET /api/wishlist`, `POST /api/wishlist/toggle`
- `GET|POST /api/addresses`, `PUT|DELETE /api/addresses/:id`
- `POST /api/coupons/validate` — validates preview coupons (`MELVORE10`, `FIRSTORDER`, `WELCOME15`)
- `POST /api/orders` — creates `COD` or `UPI`/`CARD` orders with **`paymentStatus: 'Pending'`** (`requiresPaymentVerification: true` for online orders)
- `POST /api/payments/demo-initiate`, `/api/payments/demo-verify`, `/api/payments/demo-fail` — explicit local preview payment simulation
- `GET /api/orders`, `GET /api/orders/track`, `POST /api/orders/:id/cancel`
- `GET|POST /api/tickets`, `POST /api/tickets/:id/reply`, `POST /api/contact`, `GET|POST /api/style-quiz`, `POST /api/analytics/track`
- `GET /api/export/hostinger-php` — returns structured `/php_api` deployment metadata
