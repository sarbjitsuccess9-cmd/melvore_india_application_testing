# Melvore Remote Design System — Performance, Integrity & Compatibility

## 1. Compatibility Gating (`min_app_schema_version`)

Every manifest declares `schema_version` and `min_app_schema_version`.
- `MelvoreDesignSystemService::resolvePublishedManifest($pdo, null, $appSchema)` checks `version_compare($appSchema, $minApp, '<')` server-side and automatically falls back to `default` if the requesting client is too old.
- Client-side, `RemoteDesignManifestManager.validateAndAdopt(...)` runs `DesignManifestSupport.isSchemaCompatible(manifest.minAppSchemaVersion, SUPPORTED_APP_SCHEMA_VERSION)` so even tampered or CDN-cached payloads cannot crash older binaries.

---

## 2. Canonical SHA-256 Verification & Atomic Caching

- Both PHP (`MelvoreDesignSystemService::computeChecksum`) and Android (`DesignManifestSupport.sha256Canonical`) remove `checksum_sha256`, recursively sort object keys (`ksort` / Kotlin `sortedBy`), and hash UTF-8 JSON with SHA-256.
- Conditional HTTP caching via `ETag` (`If-None-Match`) avoids re-transferring identical manifests on `version.php` / `manifest.php` polls.
- Validated manifests are serialized and stored in `SecureSessionStore` as the **Last-Known-Good (LKG)** snapshot.

---

## 3. Safe Rollback & Offline Resilience

`RemoteDesignManifestManager` guarantees zero blank screens under any failure mode:

| Failure Condition | Resolution Path |
| :--- | :--- |
| Offline cold start | Loads cached LKG manifest from `SecureSessionStore`; falls back to bundled `assets/design_system_default.json`. |
| Corrupt / truncated JSON | Rejected by Kotlin serialization; keeps active LKG or bundled default (`usedFallback = true`). |
| Invalid color hex or `< 3` nav tabs | Rejected by `validateManifestObject`; automatic rollback with logged `fallbackReason`. |
| SHA-256 mismatch | Rejected before persistence; rolls back cleanly. |
| Incompatible `min_app_schema_version` | Rejected before adoption; rolls back cleanly. |

---

## 4. Accessibility, Reduced Motion & Battery Safeguards

- Respects user `reducedMotion` preferences (`MelvoreDynamicTheme` sets `durationMillis = 0` when `reducedMotion == true`).
- Carousel auto-advance honors `tokens.motion.carousel_interval_ms` and pauses when `tokens.motion.intensity == "none"` or `reducedMotion` is active.
- Video autoplay respects `dataSaverMode` and defaults muted (`videoAutoplayMuted = true`).
- Minimum 48dp touch targets are enforced natively inside `RemoteDesignRegistry`, `MelvoreBottomBar`, and button modifiers regardless of `min_height_dp` values in JSON.

---

## 5. Deterministic `Asia/Kolkata` 8-Day Occasion Scheduling

Both `MelvoreThemeEngineService::computeOccasionWindow` (PHP) and `OccasionScheduleEvaluator` (Kotlin) evaluate:
- **Start**: `(event_date - activation_days_before days)` at `00:00:00` in the occasion timezone (`Asia/Kolkata` default).
- **End**: `event_date` at `23:59:59` in the occasion timezone.
- **Ranking**: Highest `priority` wins, breaking ties by regional specificity (`city (+4) > state (+2) > country (+1) > ALL (0)`).
- Public APIs always evaluate against server time; Admin preview runs strictly behind `MelvoreAuth::requireAdminUserId()` on `/api/admin/occasion_preview.php`.
