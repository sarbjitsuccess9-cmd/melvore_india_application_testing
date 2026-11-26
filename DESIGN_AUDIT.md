# Melvore DB-Driven Design & Mixed-Black Elimination Audit

This document tracks every hardcoded visual replacement migrated to the DB-driven manifest (`LocalMelvoreDesignManifest`, `LocalFashionWorldTheme`, `LocalDesignKit`, `LocalWorld`), the unified semantic surface layer (`LocalMelvoreSurfaces`), and the Liquid Glass / FrontSlider upgrades.

## 1. Architecture Summary

- **Single Semantic Color Layer (`MelvoreSurfaceColors.kt`):** All UI surfaces, text, borders, scrims, image overlays, glass tints, accents, and status tones are provided through `LocalMelvoreSurfaces` built from `FashionWorldThemeResolver.resolveWithPrecedence(...)`.
- **Unified Theme Precedence (`FashionWorldThemeEngine.kt`):** Implemented in ONE place (`FashionWorldThemeResolver.resolveWithPrecedence`):
  1. **Active Occasion** (festival / active occasion tab)
  2. **Selected Fashion World** (12 DB/manifest worlds)
  3. **Remote Manifest Default Tokens** (with `tokens.colors.dark` support)
  4. **Bundled Default** (`design_system_default.json`)
  - *World mode rule:* Dark worlds (`goth`, `grunge`, `y2k`, `party-wear`, etc.) stay dark ONLY when user is in Dark Mode OR inside `StyleWorldRoute`. On Home / Shop / Discover / Bag in Light Mode, `deriveLightWorldBackground(...)` creates a crisp `>= 94%` luminance canvas keeping the world accent and typography.
  - *Luxe isolation:* Only `EliteRoute` and `ElitePackRoute` enter `LUXE_ISOLATED_THEME`.
- **Contrast Guard:** `ensureContrast(foreground, background, minRatio = 4.5f)` automatically guarantees WCAG AA contrast on dynamic DB colors.
- **Tolerant Per-Section Manifest Parsing (`RemoteDesignManifestManager.kt`):** `tokens`, `navigation`, `components`, `media`, `pages`, `worlds`, and `occasion_overrides` are each decoded inside independent `runCatching` blocks so one malformed field keeps the default for that section only and records a visible warning (logged via `Log.w`, displayed in the Home debug chip, Profile About card, and `DesignAuditScreen`).
- **Live Updates & Caching:** Startup, `ON_RESUME`, and 5-minute periodic refresh synchronize all 9 backend endpoints (`manifest.php`, `bootstrap.php`, `themes/config.php`, `themes/active.php`, `occasions/active.php`, `occasions/list.php`, `design.php?section=all`, `home/feed.php`, `campaigns/active.php`) with ETag / 304 caching and smooth cross-fade recomposition.
- **SMIL Animated SVG Assets:** `RemoteSvgSkinComponents.kt` resolves every `/design-system/assets/svg/...` path against `asset_base_url` (fallback API base) and routes `.svg` animations through `AnimatedSvg` WebView with a static Coil fallback when reduced motion is enabled or WebView fails.

---

## 2. Hardcoded Visual Replacements Table

| File | Old Hardcoded Value / Pattern | New DB / Semantic Source |
|---|---|---|
| `MainActivity.kt` | `useDarkSystemBars = resolvedTheme.isDark \|\| isLuxeRoute` | Luminance computed from actual `LocalMelvoreSurfaces.current` top/bottom surface (`>= 0.5f`) |
| `MainActivity.kt` | Unclamped system font scale; unblurred overlay backdrop | Clamped `fontScale.coerceIn(0.9f, 1.15f)` + `Modifier.glassBackdropSource(...)` |
| `Theme.kt` | Standalone `MelvoreExtendedColors` with `darkEditorialBg`, `obsidianCard` | Derived from `LocalMelvoreSurfaces` (`buildMelvoreSurfaceColors`) |
| `FashionWorldThemeEngine.kt` | Split `resolveWithManifest` vs `resolveWithPrecedence`; dark worlds leaking into light Home | Unified `resolveWithPrecedence(..., isImmersiveWorldRoute)` + `deriveLightWorldBackground` + cross-fade in `MelvoreDynamicTheme` |
| `HomeScreen.kt` | Hardcoded hero/banner layout, raw dark surface accents, missing manifest debug chip | Wired `FrontSlider`, `BuildConfig.DEBUG` manifest chip (`home_debug_manifest_chip`), `LocalMelvoreSurfaces` |
| `HomeEditorialComponents.kt` | `ext.darkEditorialBg`, `Color(0xFF050507)`, `Color(0xD9050507)`, `Color.Black`, `Color.White` | `LocalMelvoreSurfaces` (`surface`, `surfaceElevated`, `imageOverlay`, `onImageOverlay`, `accent`, `onAccent`) + `ensureContrast` |
| `HomeMerchandisingSections.kt` | Hardcoded obsidian dark cards, `Color.Black`, `Color(0xFF141416)`, `Color.White` | `LocalMelvoreSurfaces` + Luxe-only spotlight styling |
| `MelvoreComponents.kt` | Hardcoded glass alpha/black borders, `Color.Black`, `Color.White` shimmer & badges | `LocalMelvoreSurfaces` (`glassTint`, `border`, `imageOverlay`, `onImageOverlay`, `sale`, `success`) + `LiquidGlassBar` |
| `MelvoreWorldActionButton.kt` | Raw black/white button fills & contrast | `LocalMelvoreSurfaces` + `ensureContrast` |
| `QuickCommerceComponents.kt` | Hardcoded search pill surface/colors, raw white/black badges | `LiquidGlassBar` + `LocalMelvoreSurfaces` |
| `Overlays.kt` | Hardcoded dark modal scrims & sheet backgrounds | `LiquidGlassBox` + `LocalMelvoreSurfaces` |
| `SearchExperienceModal.kt` | Hardcoded `Surface` on `PincodeSelectorDialog` | `LiquidGlassBox` + `LocalMelvoreSurfaces` |
| `DynamicExperienceOverlays.kt` | Plain opaque `ModalBottomSheet` for `SensorySettingsBottomSheet` | `LiquidGlassSheetContainer` + `LocalMelvoreSurfaces` |
| `DiscoverScreen.kt` | `Color.Black`, `Color.White`, `Color(0xFF09090B)` search/hero overlays | `LiquidGlassBar` + `LocalMelvoreSurfaces` |
| `ShopAndCategoriesScreens.kt` | `Color.Black`, `Color.White` on collection banner overlays | `LocalMelvoreSurfaces.imageOverlay` / `onImageOverlay` |
| `ProductDetailsScreen.kt` | `Color.Black`, `Color.White`, non-glass bottom CTA bar | `LocalMelvoreSurfaces` + sticky `LiquidGlassBar` (`product_details_glass_bottom_bar`) |
| `VideosScreen.kt` | `Color(0xFF050507)`, `Color.Black`, `Color.White`, `0xFFD4AF37`, `0xFFF43F5E` across loading/empty/sheet/scrims | `LocalMelvoreSurfaces` (`background`, `surface`, `imageOverlay`, `onImageOverlay`, `accent`, `sale`) + `LiquidGlassSheetContainer` for Shop This Look sheet |
| `StyleWorldScreen.kt` | Raw dark hex hero scrim & floating bar colors | `LocalMelvoreSurfaces` + `LiquidGlassBox` for `ImmersiveWorldFloatingBar` |
| `BrandDetailScreen.kt` | `ext.obsidianCard`, `Color.Black`, `Color.White` | `LocalMelvoreSurfaces` |
| `EliteScreen.kt` | Raw dark hex literals and hardcoded text colors | `LocalMelvoreSurfaces` + dedicated `"MELVORE LUXE"` pill |
| `ElitePackDetailScreen.kt` | Raw dark hex literals | `LocalMelvoreSurfaces` |
| `PixelFashionRunwaySection.kt` | `0xFF160D2D`, `0xFF0A0614`, `0xFF0B0718`, `0xFF0F172A`, `Color.White` (black box in light mode) | Light/dark adaptive `LocalMelvoreSurfaces` + `ensureContrast` |
| `WorldPromoVideoSection.kt` | `Color(0xFF0A0A0E)`, `Color.Black`, `Color.White` | `LocalMelvoreSurfaces` |
| `WorldMapBrandDiscovery.kt` | Hardcoded black map container & dark pin callouts in light mode | Light/dark adaptive `LocalMelvoreSurfaces` + `LiquidGlassBox` controls |
| `GothBatSwarmOverlay.kt` | `Color(0xFF0A0A0D)`, `Color(0xFFDC2626)` | `surfaces.imageOverlay`, `surfaces.sale` |
| `ReviewSheets.kt` | `Color.Black`, `Color.White`, `0xFFFBBF24`, opaque sheet container | `LiquidGlassSheetContainer` + `LocalMelvoreSurfaces` / `ext.goldAccent` |
| `InfoScreens.kt` | `Color(0x33000000)`, `Color(0xD80A0A0D)`, `Color.White` in About hero | `LocalMelvoreSurfaces.imageOverlay` / `onImageOverlay` |
| `OrderTrackingScreen.kt` | `Color.White` timeline check tint | `ensureContrast(surfaces.onAccent, ext.emerald)` |
| `VibeSatisfactionSection.kt` | `Color.White` thumb border | `LocalMelvoreSurfaces.surface` |
| `MelvoreAnimations.kt` | `Color.White`, `Color(0xFFF9A8D4)` in shimmer, quick-add, petals | `LocalMelvoreSurfaces.onImageOverlay`, `ensureContrast`, theme `accentColor` |
| `RemoteSvgSkinComponents.kt` | Direct Coil image load for animated SVGs, `Color.White` fallback | `rememberResolvedDesignAssetUrl` + `AnimatedSvg` WebView for `.svg` with Coil fallback |
| `ProfileScreen.kt` & `ProfileTabSections.kt` | Missing Light/Dark/System appearance selector & design diagnostics | `ProfileAppearanceCard` (`Light`/`Dark`/`System`), `ProfileAboutDesignSystemCard`, debug-only `DesignAuditScreen` launcher |

---

## 3. Verification Guardrails

- **Automated Unit Tests (`DbDrivenDesignAndContrastTest.kt`):**
  - Verifies tolerant per-section parsing recovers gracefully when `components` or a single `worlds` entry is malformed and records warnings.
  - Verifies `[w, h]` and `{width, height}` decoding via `KitDimensionSpecSerializer`.
  - Verifies `occasion > world > manifest > bundled` precedence.
  - Verifies dark worlds (`goth`) derive a `>= 90%` luminance canvas on Home in Light Mode while remaining dark on `StyleWorldRoute` and Luxe remaining isolated on `EliteRoute`.
  - Verifies `ensureContrast` guarantees `>= 4.5:1` WCAG AA contrast.
  - Runs a static regex scan across `app/src/main/java/com/example/ui/**/*.kt` asserting **zero** forbidden dark hex literals (`Color(0xFF05...)`, `Color(0xD905...)`, `Color(0xCC0D...)`, `Color.Black`, `darkEditorialBg`, `obsidianCard`, `isLuxeDark`) outside the 3 allow-listed theme files (`Theme.kt`, `MelvoreSurfaceColors.kt`, `FashionWorldThemeEngine.kt`).

---

## 4. Live-Backend & Zero-Silent-Fallback Hardening (v3)

- **Removed hardcoded release seed data:** Deleted `DiscoveryRepository.defaultFallbackWorlds()` from release code paths. Offline world baselines are synthesized strictly from the active `DesignManifest` (live/cached, or `design_system_default.json` as last resort).
- **Strict HTTP 200 `success: false` enforcement:** `MelvoreApiClient.executeSafely` converts HTTP 200 responses with `success == false` on `ApiEnvelope<*>` and read `*Response` DTOs into `ApiResult.Error(httpStatus = 200)` instead of silently rendering empty lists.
- **ETag / 304 / `static-live-file` provenance:** `DiscoveryRepository.fetchDesignManifest` never sends `If-None-Match` on first install, retries without it if `304` occurs with an empty cache, tags static live file fallback as `ManifestSource.STATIC_LIVE_FILE` (`"static-live-file"`), and surfaces HTTP 5xx errors instead of swallowing them.
- **In-app Connection Diagnostics (`ConnectionDiagnosticsScreen.kt`):** Available in **all build types** (Profile → About & Design System → Connection Diagnostics, About screen, and Home fallback chip), backed by an always-on zero-secret OkHttp 50-request ring buffer (`NetworkDiagnosticsMonitor`), Coil image-failure log, saved API base URL override detector + "Reset to default" button, sequential 24-endpoint probe, server `missing_tables`/`empty_modules` viewer, and clipboard report export.
- **Home Pull-to-Refresh:** Wrapped `HomeScreen` in Material 3 `PullToRefreshBox` wired to `MelvoreViewModel.forcePullToRefresh()` (ignores ETag to force fresh live manifest + bootstrap + home feed + themes).

