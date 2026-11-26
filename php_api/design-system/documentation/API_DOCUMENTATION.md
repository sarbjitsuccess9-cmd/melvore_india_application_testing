# Melvore Central Remote Design System — API Documentation

## 1. Layer Priority & Resolution Order

Every storefront render resolves styles cleanly in this exact precedence order:

1. `default/manifest.json` (Base stable design & Android bundled fallback)
2. `active_design` switch (`0 = default`, `1 = live/manifest.json` from `app_design_settings`)
3. `style_world` override (When inside Y2K, Goth, Coquette, Streetwear, K-Fashion, or Quiet Luxury; preserves world layout & atmosphere)
4. `active_occasion` overlay (Automatically activates `activation_days_before` days before `event_date` in `Asia/Kolkata`, applying seasonal banner & accent tokens without erasing world layout)
5. `component_override` / Route safety guards (`Luxe` stays isolated obsidian/gold; `Checkout` and `Auth` stay crisp light editorial)

---

## 2. Public Endpoints (Customer Android App)

Public endpoints **always** serve published `active_design` state only and evaluate occasion windows against the authoritative server clock in `Asia/Kolkata`. Simulated dates are never accepted on public endpoints.

### `GET /api/design/manifest.php`

Returns the active published design manifest, checksum, ETag, and resolved active occasion.

**Query Parameters:**
- `schema_version` / `app_schema_version` (optional, default `1.0.0`): Client schema version checked against `min_app_schema_version`.
- `world_slug` (optional): Includes `world_override` section for the requested Fashion World.
- `country` (default `India`), `state`, `city`: Regional targeting parameters for occasion matching.

**Headers:**
- Supports `If-None-Match` matching `ETag`. When matched, returns `unchanged: true` and `manifest: null`.

**Sample Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Active design manifest resolved",
  "data": {
    "active_design": 1,
    "slot": "live",
    "design_version": "1.1.0",
    "schema_version": "1.0.0",
    "min_app_schema_version": "1.0.0",
    "checksum_sha256": "e3b0c44298fc1c149afbf4c8996fb924...",
    "etag": "\"e3b0c44298fc1c14\"",
    "unchanged": false,
    "fallback_used": false,
    "fallback_reason": null,
    "server_time_ist": "2026-10-14T11:30:00+05:30",
    "priority_order": [
      "default_manifest",
      "active_design_switch",
      "style_world",
      "active_occasion",
      "component_override"
    ],
    "active_occasion": {
      "occasion_id": 1,
      "slug": "diwali-luminous-edit",
      "name": "Diwali Luminous Edit",
      "activation_days_before": 8
    },
    "manifest": { "...": "full DesignManifest object" }
  }
}
```

### `GET /api/design/version.php`

Lightweight polling check for manifest changes. Returns `active_design`, `slot`, `design_version`, `schema_version`, `checksum_sha256`, `etag`, `config_version`, and `active_occasion_slug`.

### `GET /api/design/page.php?page_key=home`

Returns the section layout configuration for a single page key (`home`, `style_worlds`, `categories`, `search`, `product_details`, `cart`).

### `GET /api/bootstrap.php`

Single cold-start payload combining `design_manifest`, `active_design`, `design_slot`, `design_version`, `design_checksum`, `fashion_worlds`, `active_occasion`, `occasions`, `world_assets`, `world_map_origins`, `smart_offers`, and `user_preferences`.

### `GET /api/occasions/list.php`

Lists configured occasions along with computed IST activation windows (`event_date`, `activation_days_before`, `activation_start_at`, `activation_end_at`, `timezone`, and regional fields).

### `GET /api/themes/config.php` & `GET /api/themes/active.php`

Legacy/combined theme endpoints updated to reflect `active_design` and the active `DesignManifest` colors, typography, and motion tokens alongside active occasions.

---

## 3. Admin Endpoints (Requires Admin Auth)

All `/api/admin/*` endpoints enforce `MelvoreAuth::requireAdminUserId()`.

### `GET /api/admin/design.php`

- `?action=workspace`: Loads `active_design`, `default_manifest`, `live_manifest`, drafts, `releases`, `history`, and registered `assets`.
- `?action=compare&left_id=1&right_id=2`: Computes section-by-section JSON diffs across `tokens`, `navigation`, `components`, `pages`, and `worlds`.

### `POST /api/admin/design.php`

JSON body actions:
- `{ "action": "save_draft", "slot": "live", "manifest": { ... }, "notes": "..." }`
- `{ "action": "publish", "slot": "live", "manifest": { ... }, "version_tag": "1.2.0", "notes": "..." }` — Validates against schema, writes atomic file to `design-system/{slot}/manifest.json`, writes snapshot to `releases/`, updates `app_design_releases` and `app_design_page_configs`.
- `{ "action": "switch_active", "active_design": 1, "notes": "..." }` — Switches `app_design_settings.active_design` between `0` (Default) and `1` (Live).
- `{ "action": "rollback", "release_id": 12, "notes": "..." }` — Restores a prior published release, or omit `release_id` for immediate emergency switch to Default (`0`).
- `{ "action": "duplicate", "release_id": 12, "target_slot": "live" }` — Clones a release into a draft in the target slot.

### `GET / POST / DELETE /api/admin/design_assets.php`

- `GET`: Lists active design assets from `app_design_assets`.
- `POST`: Supports multipart file uploads (`svg`, `png`, `jpg`, `jpeg`, `webp`, `json`, `ttf`, `otf`, `mp3`, `wav`, `ogg`) or JSON asset metadata registration into `/design-system/assets/{type}/`.
- `DELETE ?id={asset_id}`: Soft-deactivates an asset.

### `GET / POST /api/admin/occasion_preview.php`

Admin-only 8-day scheduler simulator (does **not** mutate DB or affect customers):
```json
{
  "simulated_datetime": "2026-10-12 00:05:00",
  "country": "India",
  "state": "Maharashtra",
  "city": "",
  "world_slug": "y2k"
}
```
Returns `active_occasion`, `active_occasions`, detailed per-row `evaluated` reasons, and `blended_preview`.

### `GET / POST / PUT / DELETE /api/admin/occasions.php`

CRUD management for occasions including `event_date`, `activation_days_before` (default `8`), `timezone` (default `Asia/Kolkata`), `region_country`, `region_state`, `region_city`, `priority`, and `is_active`.
