# Melvore PHP API & Dynamic Worlds Testing Guide

> **Container Note:** The AI Studio Linux build environment does not have `php` or `mysql` CLI binaries installed (`php -v` -> not found). PHP syntax linting (`php -l`) and live endpoint smoke tests must be executed on your Hostinger / local PHP 8.3 server before cutover.

## 1. PHP 8.3 Syntax Verification (`php -l`)
Run across all PHP files on deployment host:
```bash
find php_api -name "*.php" -print0 | xargs -0 -n1 php -l
```

## 2. Live API Smoke Checks
```bash
curl -sS https://app.melvore.co.in/api/health.php
curl -sS https://app.melvore.co.in/api/bootstrap.php
curl -sS https://app.melvore.co.in/api/discovery/world_map.php
curl -sS "https://app.melvore.co.in/api/offers/eligible.php?cart_subtotal=1499"
```

## 3. Authenticated & Checkout Regression Checklist
1. `POST /api/auth/register.php` & `POST /api/auth/login.php` with `X-Guest-Id` merges `cart` -> `user_cart`.
2. `POST /api/coupons/validate.php` validates against live `coupons` (`usage_count`, `valid_to`).
3. `POST /api/orders/create.php` with `Idempotency-Key` returns same `order_id` on retry.
4. `POST /api/payments/initiate.php` -> Razorpay checkout -> `POST /api/payments/verify.php`.
5. `POST /api/orders/cancel.php` restores `products.stock_quantity` and triggers Razorpay refund for paid orders.
