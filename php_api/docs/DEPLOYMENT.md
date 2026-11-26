# Melvore PHP 8.3 API Deployment Guide (Hostinger)

## 1. Database Backup & Additive Migration
1. Open Hostinger hPanel -> **phpMyAdmin** -> `u205121999_melvore` -> **Export** full `.sql` backup.
2. Run `php_api/sql/melvore_customer_app_experience_migration.sql` (if not previously applied).
3. Run `php_api/sql/migrations/002_dynamic_fashion_worlds_and_commerce.sql`.
4. Run repeat-safe seeds:
   - `php_api/sql/melvore_sample_editorial_seed.sql` (if needed)
   - `php_api/sql/seeds/002_dynamic_fashion_worlds_seed.sql`

## 2. Server Configuration (`/php_api/.env`)
Copy `php_api/.env.example` to `/php_api/.env` (protected by `php_api/.htaccess`) and set real secrets:
- `DB_HOST`, `DB_PORT`, `DB_NAME=u205121999_melvore`, `DB_USER`, `DB_PASS`
- `API_TOKEN_SECRET` (64+ random hex chars)
- `GOOGLE_CLIENT_IDS`
- `RAZORPAY_ENV=live`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`
- `CORS_ALLOWED_ORIGINS=https://melvore.co.in,https://app.melvore.co.in`
- `REQUIRE_HTTPS=true`

## 3. Deploy Files & Admin Grant
Upload `php_api/` to `app.melvore.co.in` document root. To enable an admin merchandiser account for `/api/admin/*.php`:
```sql
UPDATE users SET role = 'admin' WHERE email = 'connect@melvore.co.in';
```

## 4. Razorpay Webhook
Register `https://app.melvore.co.in/api/payments/webhook_razorpay.php` in the Razorpay Dashboard for `payment.captured` and `payment.failed` events using `RAZORPAY_WEBHOOK_SECRET`.
