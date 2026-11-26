# Melvore Storefront & Luxe Campaign Artwork Specification

Use these specifications when uploading custom studio photography, campaign banners, Style World covers, and vertical MP4 reels to the Melvore admin / database (`hero_content`, `app_campaigns`, `app_home_sections`, `app_style_worlds`, `app_elite_packs`, and `app_product_videos`).

---

## 1. Homepage Full-Bleed Hero Slides (`hero_content`)
| Asset Name | Dimensions | Aspect Ratio | Subject & Visual Direction | Safe Text Placement |
| :--- | :--- | :--- | :--- | :--- |
| `hero_winter_leather_mobile.jpg` | `1440 × 1800 px` | `4:5` | Full-body or 3/4 editorial model in structured black/espresso leather outerwear against warm architectural stone. Crisp daylight or soft key light. | Keep subject face & torso in upper 62%; reserve bottom 32% for headline, subtitle & `Shop Now` CTA scrim. |
| `hero_seoul_street_mobile.jpg` | `1440 × 1800 px` | `4:5` | Contemporary Seoul street-style duo in cropped jackets, wide trousers, and tonal layering on clean concrete backdrop. | Upper 65% subject focus; bottom-left 30% clear for typography. |
| `hero_after_dark_mobile.jpg` | `1440 × 1800 px` | `4:5` | Evening editorial look with sharp lapels and metallic hardware under warm spotlighting. | Bottom 30% dark tonal falloff for white editorial headline. |

---

## 2. Editorial Campaign Cards & Split Diptychs (`app_campaigns` / `app_home_section_items`)
| Asset Name | Dimensions | Aspect Ratio | Subject & Visual Direction | Safe Text Placement |
| :--- | :--- | :--- | :--- | :--- |
| `campaign_winter_collection.jpg` | `1200 × 1500 px` | `4:5` | Heavyweight wool & leather layering in warm ivory and charcoal tones. | Bottom-left 28% gradient zone. |
| `campaign_statement_jackets.jpg` | `1200 × 1600 px` | `3:4` | Close-up 3/4 framing highlighting collar silhouette, zips, and grain texture. | Top-left badge safe zone (`12%`); bottom 26% title zone. |
| `campaign_weekend_coords.jpg` | `1200 × 900 px` | `4:3` | Relaxed matching co-ord set shot in natural morning sun on warm plaster backdrop. | Left or bottom third clean negative space. |
| `campaign_promo_strip.jpg` | `1440 × 480 px` | `3:1` | Minimalist fabric macro or accessories flat-lay with warm champagne tones. | Center-left 65% reserved for offer headline. |

---

## 3. Style Worlds Covers (`app_style_worlds`)
| Asset Name | Dimensions | Aspect Ratio | Visual Personality | Safe Text Placement |
| :--- | :--- | :--- | :--- | :--- |
| `style_y2k_cover.jpg` | `1080 × 1440 px` | `3:4` | High-shine silver chrome, baby-pink/cobalt accent lighting, cropped moto silhouette. | Bottom 30% for `Space Grotesk` display title. |
| `style_goth_cover.jpg` | `1080 × 1440 px` | `3:4` | Deep obsidian leather, silver studs, dramatic chiaroscuro lighting with oxblood undertone. | Bottom 30% for `Cinzel` display title. |
| `style_coquette_cover.jpg` | `1080 × 1440 px` | `3:4` | Soft blush/ivory satin, delicate drapery, warm diffused studio glow. | Bottom 30% for `Playfair Display` serif title. |
| `style_streetwear_cover.jpg` | `1080 × 1440 px` | `3:4` | Wide-angle low-perspective urban look in oversized jersey and cargos. | Bottom-left 30% bold sans zone. |
| `style_k_fashion_cover.jpg` | `1080 × 1440 px` | `3:4` | Clean architectural Seoul studio portrait in slate, cream, and crisp shirting. | Bottom 28% clean negative space. |
| `style_quiet_luxury_cover.jpg` | `1080 × 1440 px` | `3:4` | Tactile camel/oatmeal cashmere & tailored trousers, zero logos, warm sunlight. | Bottom 28% serif title zone. |

---

## 4. Melvore Luxe / Elite Dark Showroom (`app_elite_packs`)
| Asset Name | Dimensions | Aspect Ratio | Subject & Visual Direction | Safe Text Placement |
| :--- | :--- | :--- | :--- | :--- |
| `luxe_hero_main.jpg` | `1440 × 1920 px` | `3:4` | High-fashion couture/atelier portrait on pure obsidian (`#050507`) background with rim light catching leather & gold hardware. | Top 14% clear for Luxe top bar; bottom 36% obsidian fade for `MELVORE LUXE` eyebrow, serif headline, and `Enter Collection` CTA. |
| `luxe_leather_atelier.jpg` | `1200 × 1500 px` | `4:5` | Macro-to-mid shot of supple black leather grain, brushed gold zips, and structured shoulder line. | Bottom 30% dark gradient zone. |
| `luxe_after_dark_archive.jpg` | `1200 × 1500 px` | `4:5` | Dramatic evening silhouette with sharp tailoring in low-key editorial lighting. | Bottom 30% dark gradient zone. |

---

## 5. Vertical Shoppable Reels (`app_product_videos`)
| Asset Name | Dimensions / Codec | Aspect Ratio | Technical & Editorial Requirements | Safe UI Zones |
| :--- | :--- | :--- | :--- | :--- |
| `reel_<product_slug>.mp4` | `1080 × 1920 px`, H.264 MP4 (`yuv420p`), `faststart` atom enabled, 4–8 Mbps, AAC audio | `9:16` | 10–25s vertical runway walk, 360° garment turn, and fabric/fit close-up. Must be hosted over `https://` with byte-range (`Accept-Ranges: bytes`) enabled. | Avoid baked-in text in right 18% (action rail) and bottom 26% (creator caption + `Shop This Look` product card). |
| `reel_<product_slug>_poster.jpg` | `1080 × 1920 px` | `9:16` | Crisp keyframe from 0.5s into the video used for instant poster display prior to first frame render. | Match video framing exactly to eliminate jump on playback start. |
