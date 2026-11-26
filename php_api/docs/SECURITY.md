# Melvore PHP API Security Architecture

1. **Secrets Isolation**: All DB credentials, Razorpay keys, Webhook secrets, and HMAC token signing keys load from process env / protected `.env`. `php_api/.htaccess` denies direct HTTP access to `.env*`, `sql/`, `config/`, `includes/`, and `services/`.
2. **PDO Prepared Statements**: Every SQL query binds inputs via PDO prepared statements with `PDO::ATTR_EMULATE_PREPARES => false`.
3. **Authentication & Revocation**: Bearer tokens use HMAC-SHA256 claims verified via `hash_equals` + SHA-256 digest lookup in `api_tokens` + password-version fingerprint (`pvf`) so password resets immediately invalidate older tokens.
4. **Role Enforcement**: `/api/admin/*.php` enforces `MelvoreAuth::requireAdminUserId()` checking `users.role` in `['admin','superadmin','merchandiser']`.
5. **Payment Integrity (Razorpay + COD Only)**:
   - Checkout pricing calculated strictly server-side (`PricingEngine`).
   - Orders locked with `SELECT ... FOR UPDATE` and optional `Idempotency-Key` deduplication.
   - `payments/verify.php` verifies checkout HMAC signature **and** queries Razorpay Payments REST API server-side for `captured` status and exact paise match before clearing the bag.
   - Webhook signatures verified with `hash_hmac('sha256', ...)` and deduplicated transactionally.
6. **Rate Limiting & Upload Validation**: Auth, OTP, password reset, and contact routes enforce sliding-window rate limits. Image uploads inspect binary MIME headers via `finfo`.
