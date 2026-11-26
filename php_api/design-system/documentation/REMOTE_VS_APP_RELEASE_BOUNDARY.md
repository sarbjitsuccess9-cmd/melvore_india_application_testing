# Remote Manifest Configuration vs Native App Release Boundary

Melvore separates **declarative presentation** (controlled remotely at zero binary cost) from **compiled security, payments, and new Compose primitives** (shipped via APK/AAB updates).

---

## Changes That Work Immediately via Remote Manifest (`default` / `live`)

1. **Brand Palette & Dark/Light Tokens** — Background, surfaces, primary, accent, borders, sale & success accents (`tokens.colors.*`).
2. **Typography Pairing & Scale** — Swapping `display_font` / `body_font` among registered font families (`editorial_serif`, `clean_sans`, `minimal`, `futuristic`, `gothic`, `k_fashion`), heading scale, and tracked eyebrow spacing.
3. **Spacing, Shapes & Motion Intensity** — Screen horizontal padding, section vertical gaps, card corner radius, button sharpness/pill shape, parallax toggle, carousel speed.
4. **Top Bar & Bottom Navigation** — Top bar title/subtitle, `flat_editorial` vs `floating_dock` bottom bar, tab ordering, visibility (minimum 3 visible tabs), custom tab labels, badge pills (`LUXE`), and swapping among compiled icons in `icon-registry.json`.
5. **Component Variants**:
   - Search bar: `pill_elevated` vs `underlined_editorial` + placeholder copy
   - Category strip: `circular_story` (4-col) vs `portrait_tiles` (3-col)
   - Product card: aspect ratio (`0.62..1.0`), corner radius, toggle brand/rating/quick-add visibility
   - Section headers: `magazine_serif` vs `tracked_mono` + section divider lines
   - Empty/Error states: titles, subtitles, and CTA labels
6. **Page & Section Composition** — Reordering, showing, hiding, or relabeling existing Home sections (`search_bar`, `category_strip`, `occasion_banner`, `hero_carousel`, `style_worlds`, `trending_rail`, `world_atelier_map`, `elite_teaser`, `curated_grid`) and Fashion World section ordering (`worlds.{slug}.section_order`).
7. **Automated 8-Day Occasion Scheduling** — Scheduling festivals/events in `app_occasions` with `Asia/Kolkata` timing, regional city/state/country filters, priority resolution, banners, and subtle overlays.
8. **Remote Assets** — New promotional imagery, campaign banners, ambient audio URLs, and video reels stored under `design-system/assets/`.

---

## Changes Requiring a Native Android Release (APK / AAB)

1. **New Interactive Compose Section Types** — Adding a brand-new section `id` or `type` not yet mapped in `HomeScreen.kt` / `RemoteDesignRegistry.kt`.
2. **New Material Icon Keys or Custom Bundled `.ttf` Fonts** — Adding keys beyond the compiled switches in `RemoteDesignRegistry.iconForKey(...)` or `fontFamilyForStyleKey(...)`.
3. **Payment & Auth Flows** — Modifying Razorpay SDK integration, COD validation, Google Sign-In (`CredentialManager`), or session storage security.
4. **Schema Structure Changes** — Adding mandatory new top-level manifest keys or breaking deserialization fields (requires bumping `SUPPORTED_APP_SCHEMA_VERSION` and setting `min_app_schema_version` on manifests using the new fields).
5. **Executable Behavior** — Melvore never downloads or executes dynamic `.dex`, `.jar`, or script logic at runtime (Play Store Policy compliant).
