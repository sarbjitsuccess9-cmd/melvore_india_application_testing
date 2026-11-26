# Melvore Database Schema & Additive Migrations

**Database:** `u205121999_melvore` (MariaDB / MySQL, `utf8mb4_unicode_ci`)

## Existing Core Table Contracts Preserved
- **`users`**: `user_id` PK. Inserts supply `remember_token = 0`, `reason = ''`, `mobile_primary`, `password_hash` (`PASSWORD_BCRYPT`).
- **`user_cart` vs `cart`**: Authenticated users mutate `user_cart` (`user_cart_id` PK); guests mutate `cart` (`user_id IS NULL`, `session_id = X-Guest-Id`). `MelvoreCartHelper::mergeGuestCartToUser()` merges guest lines into `user_cart` on login/register/social/OTP.
- **`coupons`**: Production column names are `usage_count` and `valid_to`.
- **`orders` & `payments`**: Razorpay + Cash on Delivery (`cod` | `online`). Cashfree is disabled in the API layer while legacy columns remain untouched non-destructively.

## Additive Migration `002_dynamic_fashion_worlds_and_commerce.sql`
Executed cleanly via `information_schema` checks (zero `DROP` or `TRUNCATE`):
- Security & Orders: `api_tokens`, `api_order_idempotency`, `payment_webhook_events`, `users.role`, `orders.refund_status`, `orders.refund_reference`, `payments.order_id`.
- Dynamic Fashion Worlds & UX:
  - `app_style_worlds` (+ `ambient_preset`, `audio_track_key`, `version_tag`)
  - `app_occasions` (scheduled festival/seasonal/regional themes)
  - `app_world_assets` (vector motif metadata mapped to worlds/occasions)
  - `app_brand_origins` (verified city/country coordinates linked to active `brands`)
  - `app_world_brands`
  - `app_smart_offers` & `app_offer_impressions` (contextual offers linked to real `coupons` + per-session/cooldown tracking)
  - `app_user_preferences` (synced audio/SFX/reduced-motion flags)
