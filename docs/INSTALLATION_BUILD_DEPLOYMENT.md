# Melvore — Installation, Build, Deployment & Verification Guide

This guide covers:
1. **Local React + Express Preview Setup** (`npm run dev` on port `3000`)
2. **Hostinger PHP 8.3 & MySQL Production Deployment** (`https://app.melvore.co.in`)
3. **Flutter Mobile App Setup & Android Release Build** (`flutter_app`)
4. **Honest Verification & Test Report** (executed checks vs environment blockers)

---

## 1. Local React + Express Storefront Preview

### Prerequisites
- Node.js `v20+` (`v22.23.2` verified in workspace) and `npm`

### Commands
```bash
# 1. Install Node dependencies
npm install

# 2. (Optional) Copy environment template if customizing local preview vars
cp .env.example .env

# 3. Start Express + Vite dev server on http://localhost:3000
npm run dev

# 4. Verify TypeScript & production bundle
npm run lint
npm run build
```

> **Preview Payment Simulation**: Online checkouts (`UPI` / `CARD`) in local preview start as `paymentStatus: 'Pending'` and open a local simulation modal (`Simulate Payment Success` / `Simulate Failure` / `Cancel Unpaid Order`) backed by `/api/payments/demo-{initiate,verify,fail}`. No real charges occur in local preview.

---

## 2. Hostinger PHP 8.3 & MySQL Production Deployment (`app.melvore.co.in`)

### Step 1: Backup & Safe MySQL Migration (Run Manually in phpMyAdmin)
1. Open **Hostinger hPanel → Databases → phpMyAdmin → `u205121999_melvore`**.
2. **Export → Custom → Format: SQL → Go** to save a complete local backup before touching anything.
3. Run [`php_api/sql/melvore_safe_migration.sql`](../php_api/sql/melvore_safe_migration.sql) in the **SQL** tab.
   - Safe & idempotent (`information_schema`-checked). Widens `payment_sessions.payment_session_id` to `VARCHAR(255)`, adds `homepage_sections` PK, adds `payments.order_id`, adds `orders.refund_status`/`refund_reference` & indexes, updates legacy `carts.unique_cart_item`, and creates `api_tokens`, `api_order_idempotency`, and `payment_webhook_events`.

### Step 2: Subdomain, SSL & PHP Runtime
1. Create subdomain **`app.melvore.co.in`** in Hostinger hPanel.
2. Enable **Let's Encrypt SSL** + **Force HTTPS**.
3. Set PHP version to **PHP 8.2 / 8.3** with `pdo_mysql`, `curl`, `mbstring`, `json`, `fileinfo`, and `openssl` enabled.

### Step 3: Upload `/php_api` & Configure `.env`
Upload the contents of `/php_api/` into the `app.melvore.co.in` document root (leaving `/melvore.co.in` website files untouched), ensure `logs/`, `storage/`, and `uploads/` are writable (`chmod 750` / `755`), and create `.env` (`chmod 600`, protected by `.htaccess`):

```ini
APP_ENV=production
APP_URL=https://melvore.co.in
API_URL=https://app.melvore.co.in
ASSET_BASE_URL=https://melvore.co.in
REQUIRE_HTTPS=true
MAIL_FROM=connect@melvore.co.in

DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=u205121999_melvore
DB_USER=u205121999_melvore
DB_PASS=REPLACE_WITH_HOSTINGER_MYSQL_PASSWORD
DB_CHARSET=utf8mb4

# Generate with: openssl rand -hex 32
API_TOKEN_SECRET=REPLACE_WITH_64_CHAR_HEX_SECRET
API_TOKEN_TTL_SECONDS=2592000

CORS_ALLOWED_ORIGINS=https://melvore.co.in,https://www.melvore.co.in,https://app.melvore.co.in

# Razorpay (Mobile App + Server Verification & Refunds)
RAZORPAY_ENV=live
RAZORPAY_KEY_ID=rzp_live_REPLACE_ME
RAZORPAY_KEY_SECRET=REPLACE_WITH_RAZORPAY_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET=REPLACE_WITH_RAZORPAY_WEBHOOK_SECRET

# Cashfree (Website Checkout — Preserved)
CASHFREE_ENV=production
CASHFREE_APP_ID=REPLACE_WITH_CASHFREE_APP_ID
CASHFREE_SECRET_KEY=REPLACE_WITH_CASHFREE_SECRET_KEY
CASHFREE_WEBHOOK_SECRET=REPLACE_WITH_CASHFREE_WEBHOOK_SECRET

# Google OAuth Client IDs (comma-separated Web/Android/iOS IDs for server ID token aud check)
GOOGLE_CLIENT_IDS=
```

### Step 4: Register Webhooks & Smoke Test Live Endpoints
1. **Razorpay Dashboard → Developers → Webhooks**:
   - URL: `https://app.melvore.co.in/api/payments/webhook_razorpay.php`
   - Secret: exact match to `RAZORPAY_WEBHOOK_SECRET`
   - Events: `payment.captured`, `order.paid`, `payment.failed`
2. **Cashfree Dashboard → Webhooks**:
   - URL: `https://app.melvore.co.in/api/payments/webhook_cashfree.php` (`2023-08-01`)
3. **Run PHP Syntax Check & Live HTTP Checks on Hostinger SSH**:
   ```bash
   find /path/to/app.melvore.co.in -name "*.php" -exec php -l {} +
   curl -I https://app.melvore.co.in/.env                 # Must return 403 Forbidden
   curl -s https://app.melvore.co.in/api/health.php       # {"success":true,...}
   curl -s https://app.melvore.co.in/design.php | grep -E "key_secret|secret_key" || echo "PASS: Zero secrets exposed"
   ```

---

## 3. Flutter Mobile App & Android Release Build (`flutter_app`)

### Prerequisites
- Flutter SDK `3.22+` (Dart `>=3.4.0 <4.0.0`)
- Android Studio / Android SDK (`compileSdkVersion 34`, `minSdkVersion 21`) & JDK 17
- *(iOS)* macOS + Xcode `15.4+` + CocoaPods

### Bootstrap & Tests
```bash
cd flutter_app

# Generates Gradle wrapper binary & platform runners without overwriting customized manifests/Gradle/Dart
flutter create . --org in.co.melvore --project-name melvore_app

flutter pub get
flutter analyze
flutter test
```

### Environment Overrides
```bash
# Default production target (https://app.melvore.co.in):
flutter run

# Custom staging API override:
flutter run \
  --dart-define=MELVORE_ENV=dev \
  --dart-define=MELVORE_API_BASE=https://staging-api.melvore.co.in
```

### Android Release Signing & APK / AAB Build
`flutter_app/android/app/build.gradle` explicitly enforces real signing credentials on release tasks (throwing a `GradleException` rather than silently falling back to the debug key).

1. **Generate Upload Keystore (once, outside Git)**:
   ```bash
   keytool -genkey -v \
     -keystore ~/melvore-upload-keystore.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias melvore_upload
   ```
2. **Configure `flutter_app/android/key.properties`** (copy from `flutter_app/android/key.properties.example`; gitignored):
   ```properties
   storePassword=YOUR_STORE_PASSWORD
   keyPassword=YOUR_KEY_PASSWORD
   keyAlias=melvore_upload
   storeFile=/absolute/path/to/melvore-upload-keystore.jks
   ```
   *(CI alternative: export `MELVORE_KEYSTORE_PATH`, `MELVORE_KEYSTORE_PASSWORD`, `MELVORE_KEY_ALIAS`, `MELVORE_KEY_PASSWORD`.)*
3. **Build APK & Play Store App Bundle**:
   ```bash
   cd flutter_app
   flutter build apk --release
   flutter build appbundle --release
   ```
   - Outputs:
     - `flutter_app/build/app/outputs/flutter-apk/app-release.apk`
     - `flutter_app/build/app/outputs/bundle/release/app-release.aab`
4. *(Optional)* Replace adaptive vector foreground/legacy PNG mipmaps in `android/app/src/main/res/` with raster exports from `flutter_launcher_icons` if using an updated photographic mark.

### iOS Build (macOS)
```bash
cd flutter_app/ios && pod install --repo-update && cd ..
flutter build ipa --release
```

---

## 4. Honest Verification & Test Report

### A. Checks Actually Executed & Passed in This Workspace

| Check | Scope & Command | Observed Result |
|---|---|---|
| **TypeScript Static Check (`npm run lint`)** | `tsc --noEmit` across `/src` & `/server.ts` | **PASS** — `0` TypeScript errors after final React/Express edits. |
| **Vite Production Bundle (`npm run build`)** | Full client compilation (`compile_applet`) | **PASS** — Clean production bundle built. |
| **Express Live API Integration Test (`http://127.0.0.1:3000`)** | Automated Node integration script hitting live dev server on port `3000` (with `/data/melvore_db.json` restored cleanly afterward) | **PASS** — Verified `/api/health` (`ok: true`), `/api/design`, `/api/catalog` (`8` products), guest cart add (`2` items) + registration cart merge (`2` items), `/api/coupons/validate` (`MELVORE10` valid discount, `INVALID99` HTTP `422`), `POST /api/orders` (`UPI` starts `paymentStatus: "Pending"`, `requiresPaymentVerification: true`), `/api/payments/demo-initiate`, `/api/payments/demo-verify` (`Paid` + `processing` + cart cleared to `0`), `POST /api/orders/:id/cancel` (`cancelled` + `Refunded`), and `/api/export/hostinger-php` (`success: true`). |
| **Cross-Stack Static Integrity Audit** | Python audit across `/php_api`, `/flutter_app`, `/src`, `/server.ts` | **PASS** — `63` PHP files checked (`0` unsafe SQL interpolations, `0` `hydrateFromCompanionTables` / `used_count` / `PricingEngine::validateCoupon` / `coupon_error` bugs), `49/49` endpoints in `api_config.dart` resolved to existing PHP files, `35` Dart files with `0` `flutter_cashfree_pg_sdk` / legacy `.applyCoupon(` / `.couponError` issues, `57` SQL reference tables parsed, Android adaptive `@mipmap/ic_launcher` XML + `mdpi`–`xxxhdpi` PNGs + `GradleException` release signing guard verified. |

### B. Unexecuted Checks & Environment Blockers (Must Run Outside Container)

Probed container toolchain (`which php flutter dart java gradle mysql`): only `git`, `node` (`v22.23.2`), `npm`, and `python3` exist in this environment. Accordingly, the following were **NOT executed** here and are explicitly marked **BLOCKED / REMAINING**:

| Target | Blocker | Command / Action Needed on Workstation or Hostinger |
|---|---|---|
| **PHP 8.3 Syntax & Runtime** | `php` binary not installed (`php: null`) | Run `find php_api -name "*.php" -exec php -l {} +` on Hostinger SSH or local PHP 8.2/8.3. |
| **Live MySQL Schema & Migration** | `mysql` client not installed (`mysql: null`) and no live Hostinger DB credentials | Back up `u205121999_melvore` in phpMyAdmin, run `php_api/sql/melvore_safe_migration.sql`, and verify `design.php` & `/api/orders/*`. |
| **Flutter Static Analysis, Unit Tests & Android APK** | `flutter`, `dart`, `java`, `gradle` binaries not installed (`null`) | Inside `flutter_app/`: run `flutter create . --org in.co.melvore --project-name melvore_app`, `flutter pub get`, `flutter analyze`, `flutter test`, and `flutter build apk --release` (with `android/key.properties` configured). |
| **Live Razorpay & Google OAuth Flow** | Requires live/sandbox Razorpay merchant credentials & real Google OAuth client IDs | Configure `.env` on `app.melvore.co.in`, register `webhook_razorpay.php`, and complete an end-to-end ₹1 Razorpay order, cancellation refund, and Google sign-in on an Android device. |
