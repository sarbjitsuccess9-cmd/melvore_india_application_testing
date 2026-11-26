# Melvore Remote Design System — Deployment & Operations Guide

## 1. Initial Database Migration & Seeding

Execute the SQL scripts in order against your MySQL 8.0+ / MariaDB 10.6+ database:

1. `php_api/sql/melvore_remote_design_system_migration.sql`
   - Creates `app_design_settings`, `app_design_releases`, `app_design_assets`, `app_design_page_configs`, and `app_design_release_history`.
   - Safely alters `app_occasions` via an idempotent stored procedure to add `event_date`, `activation_days_before` (default `8`), `activation_start_at`, `activation_end_at`, `timezone` (`Asia/Kolkata`), `region_state`, `region_city`, and `design_override_json`.
2. `php_api/sql/melvore_remote_design_system_seed.sql`
   - Seeds the initial Default (`0`) and Live (`1`) release entries, page configs, and sample festival/occasion schedules (Diwali, Christmas, Halloween, New Year, Valentine's Day, Summer Sale).

Ensure `php_api/design-system/` is writable by the PHP-FPM/Apache user so `MelvoreDesignSystemService::publishSlot` can atomically write `default/manifest.json`, `live/manifest.json`, and snapshots under `releases/`.

---

## 2. How `active_design` Switching (`0` vs `1`) Works

The flag `app_design_settings.active_design` controls the global public storefront:

- `0` → **Default Manifest (`default/manifest.json`)**: Stable classic Ivory/Obsidian editorial layout.
- `1` → **Live Manifest (`live/manifest.json`)**: Independent edition (e.g., Atelier Warm Gallery) tested in draft before activation.

Because `Default` and `Live` are separate files and separate database slots:
- Editing or publishing drafts in `live` **never** overwrites `default/manifest.json`.
- Toggling `active_design` via Admin CMS **Design Studio** (`POST /api/admin/design.php` with `{"action":"switch_active","active_design":1}`) takes effect immediately for all customers on next cold start, app resume (`ON_RESUME`), or pull-to-refresh.

---

## 3. Keeping Android Bundled Fallback Synced

Whenever the baseline `default` storefront changes meaningfully before a Play Store release:

1. Copy the approved `php_api/design-system/default/manifest.json` to `app/src/main/assets/design_system_default.json`.
2. Keep `DesignManifestSupport.SUPPORTED_APP_SCHEMA_VERSION` aligned with `min_app_schema_version` in manifests meant for older clients.

The Android app uses `design_system_default.json` **only** when both network and the persisted last-known-good cache are unavailable, or when an incoming manifest fails validation or compatibility checks.

---

## 4. Instant Rollback Procedure

If a newly activated `live` release shows layout or visual regressions:

1. **Zero-downtime Emergency Switch**: In Admin CMS Design Studio, click **Switch to Default (0)** (or `POST /api/admin/design.php` with `{"action":"rollback"}`).
2. **Versioned Release Rollback**: Pick any prior release row in `app_design_releases` and trigger `{"action":"rollback","release_id":<id>}` to republish that immutable snapshot and restore audit history in `app_design_release_history`.
3. Every change records `previous_active_design`, `new_active_design`, `actor_user_id`, and `snapshot_json` for compliance auditing.
