# Melvore Design Manifest Schema Specification (`1.0.0`)

Formal JSON Schema: `php_api/design-system/schemas/design_manifest.schema.json`

## 1. Root Metadata

| Field | Type | Description |
| :--- | :--- | :--- |
| `manifest_id` | string | Unique identifier (e.g., `melvore_default_issue_v1`). |
| `slot` | enum (`default` \| `live`) | Slot isolation marker. Default and Live cannot overwrite each other accidentally. |
| `design_version` | semver string | Visual release version (e.g., `1.0.0`, `1.1.0`). |
| `schema_version` | semver string | Structure version (`1.0.0`). |
| `min_app_schema_version` | semver string | Minimum Android app `SUPPORTED_APP_SCHEMA_VERSION` allowed to adopt this manifest. |
| `release_notes` | string | Human-readable release summary. |
| `published_at` | ISO-8601 string | UTC publish timestamp set by `MelvoreDesignSystemService::publishSlot`. |
| `checksum_sha256` | hex string (64 chars) | Canonical SHA-256 computed over sorted JSON excluding `checksum_sha256`. |

## 2. `tokens`

### `tokens.colors`
All values must be valid 6-digit or 8-digit hex strings (`#RRGGBB` or `#AARRGGBB`):
- `mode`: `"light"` or `"dark"`
- `background`, `surface`, `surface_elevated`, `surface_variant`
- `primary`, `on_primary`, `accent`, `secondary_accent`
- `text_primary`, `text_secondary`, `border`
- `badge_bg`, `badge_text`, `sale_accent`, `success_accent`

### `tokens.typography`
- `display_font`: `"editorial_serif"`, `"clean_sans"`, `"minimal"`, `"futuristic"`, `"gothic"`, `"k_fashion"`
- `body_font`: Mapped via `fontFamilyForStyleKey(...)`
- `heading_scale` (`0.8..1.4`), `body_scale` (`0.85..1.3`), `letter_spacing_eyebrow` (`0.0..4.0` sp)

### `tokens.spacing`
- `screen_horizontal_dp` (`8..32`), `section_gap_dp` (`12..48`), `card_padding_dp` (`6..24`), `grid_gutter_dp` (`6..24`)

### `tokens.radii`
- `card_dp`, `button_dp`, `chip_dp`, `search_bar_dp`, `hero_dp`, `sheet_dp`, `border_width_dp`

### `tokens.motion`
- `intensity`: `"none"` \| `"subtle"` \| `"expressive"`
- `hero_parallax`: boolean
- `card_entrance`: `"fade_slide"` \| `"editorial_lift"` \| `"none"`
- `carousel_interval_ms`: integer (`2000..12000`)

## 3. `navigation`

- `top_bar_style`: `"editorial_masthead"` \| `"centered_monogram"`
- `top_bar_title`, `top_bar_subtitle`: Remote masthead title & edition subhead
- `show_search_icon`, `show_bag_badge`: booleans
- `bottom_bar_variant`:
  - `"flat_editorial"` — Full-width bottom bar anchored cleanly to navigation insets
  - `"floating_dock"` — Detached pill dock elevated over content (`bottom_bar_radius_dp`, `bottom_bar_elevation_dp`)
- `tabs`: Array of 3–6 tab items:
  - `route` (`home`, `discover`, `videos`, `elite`, `bag`)
  - `label`, `icon_key`, `active_icon_key` (must exist in `assets/icons/icon-registry.json`)
  - `visible`, `order`, `badge_text`

## 4. `components`

- `button`: `primary_variant` (`"editorial_sharp"` \| `"pill_luxe"`), `corner_radius_dp`, `min_height_dp`, `uppercase_labels`, `show_trailing_arrow`, `border_width_dp`
- `search_bar`: `variant` (`"pill_elevated"` \| `"underlined_editorial"`), `placeholder`, `corner_radius_dp`, `height_dp`, `leading_icon_key`, `show_visual_hint_icon`
- `product_card`: `variant` (`"editorial_portrait"` \| `"gallery_bordered"`), `image_aspect_ratio` (`0.62..1.0`), `corner_radius_dp`, `show_brand`, `show_rating`, `show_quick_add`, `badge_position`, `price_alignment`
- `category_strip`: `variant` (`"circular_story"` \| `"portrait_tiles"`), `columns` (`3` or `4`), `show_eyebrow`, `tile_radius_dp`
- `section_header`: `variant` (`"magazine_serif"` \| `"tracked_mono"`), `show_eyebrow`, `show_divider`
- `states`: `loading_variant`, `empty_variant` (`"framed_editorial"` \| `"centered_minimal"`), `error_variant`, `empty_title`, `empty_subtitle`, `error_cta_label`

## 5. `pages`

Required keys: `home`, `style_worlds`, `categories`, `search`, `product_details`, `cart`.
Each page defines `layout_template`, `hero_height_dp`, `grid_columns`, optional `show_floating_world_strip`, and an ordered `sections` array of `{ id, type, visible, order, layout_variant, eyebrow, title, subtitle, cta_label, columns }`.

## 6. `worlds`

Per-world overrides keyed by slug (`y2k`, `goth`, `coquette`, `streetwear`, `k-fashion`, `quiet-luxury`), defining `template`, `hero_layout`, `font_style`, `button_style`, `card_radius_dp`, `grid_columns`, and `section_order`.

## 7. `occasion_overrides`

- `default_activation_days_before` (default `8`)
- `default_timezone` (default `"Asia/Kolkata"`)
- `max_banner_elevation_dp` (`4`)
- `preserve_world_layout` (`true`)
