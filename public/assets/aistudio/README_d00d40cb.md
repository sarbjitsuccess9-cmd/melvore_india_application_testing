# Melvore Design System Assets

Central asset storage for remote design manifests:

- `fonts/` — Bundled & licensed typography font metadata (`playfair_display`, `jost`, `plus_jakarta_sans`, `space_grotesk`, `cinzel`, `didact_gothic`, `silkscreen`).
- `icons/` — Curated navigation and storefront icon registry descriptors (`icon-registry.json`) plus custom vector SVG uploads.
- `images/` — Promotional hero banners, lookbook covers, and storefront fallback plates.
- `animations/` — Declarative motion presets (`none`, `subtle`, `expressive`, `fade_slide`, `editorial_lift`).
- `illustrations/` — Editorial motifs and seasonal decorative artwork.
- `sounds/` — Ambient fashion world & festive occasion audio assets.

## v1.2.0 additions
- `svg/worlds/<slug>/` — 9 component skins per world (button, button_ghost, navbar, nav_indicator, slider, pager, divider, card_frame, loader) + `icons/` (5 tabs x inactive/active).
- `svg/sprites/<asset_key>.svg` — one sprite per `app_world_assets` row (wired via `image_url` in the SQL patch).
- `svg/occasions/<slug>/` — button, button_ghost, banner, divider, corner for 12 occasions.
- `images/pixel-arcade-hero.svg`, `images/goth-nocturne-hero.svg` — new hero plates.
- `animations/motion-presets.json` — 14 new presets (pixel_step, goth_flap, chrome_shine, ...).
