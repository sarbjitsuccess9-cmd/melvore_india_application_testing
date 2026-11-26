package com.example

import com.example.model.DesignKit
import com.example.model.DesignManifest
import com.example.model.FashionWorldTheme
import com.example.model.OccasionTheme
import com.example.model.WorldKit
import com.example.network.ApiEnvelope
import com.example.network.DesignManifestResponseDto
import com.example.ui.theme.FashionWorldThemeResolver
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickCommerceDesignEngineTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun manifestParsing_handlesV2AndV3KitsDarkColorsAndUnknownFields() {
        val raw = """
            {
              "success": true,
              "message": "Manifest ready",
              "data": {
                "active_design": 1,
                "slot": "live",
                "etag": "\"v3-live-001\"",
                "manifest_hash": "sha256_abc123",
                "asset_base_url": "https://cdn.melvore.co.in",
                "unknown_top_level": {"nested": [1, 2, 3]},
                "manifest": {
                  "schema_version": "3.0.0",
                  "design_version": "3.2.1",
                  "future_manifest_flag": true,
                  "tokens": {
                    "colors": {
                      "background": "#FAF8F4",
                      "surface": "#FFFFFF",
                      "primary": "#1A1816",
                      "accent": "#B08D57",
                      "dark": {
                        "background": "#0E0D0C",
                        "surface": "#181615",
                        "primary": "#F5F2EC",
                        "accent": "#D4AF37",
                        "text_primary": "#FAF8F4"
                      }
                    },
                    "motion": {
                      "duration_ms": 300,
                      "easing": "ease_out"
                    }
                  },
                  "worlds": {
                    "y2k": {
                      "palette": {
                        "background": "#0A0614",
                        "surface": "#171128",
                        "primary": "#FF4FD8",
                        "accent": "#00F5D4",
                        "text_primary": "#FFFFFF"
                      },
                      "kit": {
                        "v2": {
                          "components": {
                            "button": "/design-system/live/y2k/v2/button.svg",
                            "navbar": "/design-system/live/y2k/v2/navbar.svg",
                            "nav_indicator": "/design-system/live/y2k/v2/nav_indicator.svg"
                          },
                          "sizes": {
                            "button": {"w": 160, "h": 48}
                          },
                          "icons": {
                            "home": {
                              "icon": "/design-system/live/y2k/v2/icons/home.svg",
                              "active_icon": "/design-system/live/y2k/v2/icons/home_active.svg"
                            }
                          }
                        },
                        "v3": {
                          "layouts": {
                            "home": "/design-system/live/y2k/v3/layouts/home.svg",
                            "bag": "/design-system/live/y2k/v3/layouts/bag.svg"
                          },
                          "widgets": {
                            "banner": "/design-system/live/y2k/v3/widgets/banner.svg",
                            "order_steps": "/design-system/live/y2k/v3/widgets/order_steps.svg"
                          },
                          "widget_sizes": {
                            "banner": {"w": 360, "h": 140}
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val env = json.decodeFromString<ApiEnvelope<DesignManifestResponseDto>>(raw)
        assertTrue(env.success)
        val payload = env.data
        assertNotNull(payload)
        assertEquals("sha256_abc123", payload!!.manifestHash)
        assertEquals("https://cdn.melvore.co.in", payload.assetBaseUrl)

        val manifest = payload.manifest!!
        assertEquals("3.2.1", manifest.designVersion)
        assertEquals(300, manifest.tokens.motion.durationMs)
        assertEquals("#0E0D0C", manifest.tokens.colors.dark?.background)
        assertEquals("#D4AF37", manifest.tokens.colors.dark?.accent)

        val kit = DesignKit(
            manifest = manifest,
            assetBaseUrl = payload.assetBaseUrl,
            manifestHash = payload.manifestHash,
            etag = payload.etag
        )
        val y2kWorld: WorldKit? = kit.world("y2k")
        assertNotNull(y2kWorld)
        assertEquals("/design-system/live/y2k/v3/layouts/home.svg", y2kWorld!!.layoutPath("home"))
        assertEquals("/design-system/live/y2k/v3/widgets/order_steps.svg", y2kWorld.widgetPath("order_steps"))
        assertEquals("/design-system/live/y2k/v2/navbar.svg", y2kWorld.componentPath("navbar"))
        assertEquals("/design-system/live/y2k/v2/icons/home_active.svg", y2kWorld.iconPath("home", active = true))
        assertEquals(
            "https://cdn.melvore.co.in/design-system/live/y2k/v3/layouts/home.svg",
            kit.url(y2kWorld.layoutPath("home"))
        )

        // Unknown world slug must return null cleanly without throwing
        assertNull(kit.world("non_existent_world_xyz"))
    }

    @Test
    fun themePrecedence_occasionOverridesWorldWhichOverridesDefault() {
        val manifest = json.decodeFromString<DesignManifest>(
            """
            {
              "tokens": {
                "colors": {
                  "background": "#FAF8F4",
                  "surface": "#FFFFFF",
                  "primary": "#1A1816",
                  "accent": "#B08D57",
                  "dark": {
                    "background": "#0E0D0C",
                    "surface": "#181615",
                    "primary": "#F5F2EC",
                    "accent": "#D4AF37",
                    "text_primary": "#FAF8F4"
                  }
                }
              },
              "worlds": {
                "goth": {
                  "palette": {
                    "background": "#100C14",
                    "surface": "#1B1522",
                    "primary": "#E50914",
                    "accent": "#9B5DE5",
                    "text_primary": "#F7F3FF"
                  }
                }
              },
              "occasion_overrides": {
                "occasions": {
                  "diwali": {
                    "motif": "diya",
                    "colors": {
                      "primary": "#B45309",
                      "accent": "#FFB703",
                      "background": "#FFF6E0"
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val defaultTheme = com.example.model.ThemeTokenSpec(
            themeKey = "melvore_light",
            name = "Melvore Classic",
            backgroundColor = "#FAF8F4",
            surfaceColor = "#FFFFFF",
            primaryColor = "#1A1816",
            accentColor = "#B08D57"
        )
        val gothWorld = FashionWorldTheme(
            slug = "goth",
            name = "Goth Atelier",
            mode = "dark",
            backgroundColor = "#100C14",
            surfaceColor = "#1B1522",
            primaryColor = "#E50914",
            accentColor = "#9B5DE5"
        )
        val diwaliOccasion = OccasionTheme(
            slug = "diwali",
            name = "Diwali",
            headline = "Diwali Luxe Edit",
            primaryColor = "#B45309",
            accentColor = "#FFB703",
            backgroundColor = "#FFF6E0"
        )

        // 1) Default only (light)
        val resolvedDefault = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            fallbackTheme = defaultTheme,
            activeOccasion = null,
            selectedWorld = null,
            preferDarkMode = false
        )
        assertEquals("default", resolvedDefault.sourceLayer)
        assertEquals("#FAF8F4", resolvedDefault.backgroundHex)
        assertFalse(resolvedDefault.isDark)

        // 2) Default in dark mode uses tokens.colors.dark
        val resolvedDefaultDark = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            fallbackTheme = defaultTheme,
            activeOccasion = null,
            selectedWorld = null,
            preferDarkMode = true
        )
        assertEquals("#0E0D0C", resolvedDefaultDark.backgroundHex)
        assertEquals("#D4AF37", resolvedDefaultDark.accentHex)
        assertTrue(resolvedDefaultDark.isDark)

        // 3) Selected world overrides default
        val resolvedWorld = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            fallbackTheme = defaultTheme,
            activeOccasion = null,
            selectedWorld = gothWorld,
            preferDarkMode = false
        )
        assertEquals("fashion_world", resolvedWorld.sourceLayer)
        assertEquals("goth", resolvedWorld.worldSlug)
        assertEquals("#9B5DE5", resolvedWorld.accentHex)

        // 4) Active occasion strictly overrides selected world accent & background tint
        val resolvedOccasionOverWorld = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            fallbackTheme = defaultTheme,
            activeOccasion = diwaliOccasion,
            selectedWorld = gothWorld,
            preferDarkMode = false
        )
        assertEquals("occasion", resolvedOccasionOverWorld.sourceLayer)
        assertEquals("diwali", resolvedOccasionOverWorld.occasionSlug)
        assertEquals("#FFB703", resolvedOccasionOverWorld.accentHex)
        assertEquals("#FFF6E0", resolvedOccasionOverWorld.backgroundHex)

        // 5) Neutral checkout/auth route ignores both occasion and world
        val resolvedCheckout = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            fallbackTheme = defaultTheme,
            activeOccasion = diwaliOccasion,
            selectedWorld = gothWorld,
            isNeutralCheckoutOrAuthRoute = true
        )
        assertEquals("default", resolvedCheckout.sourceLayer)
        assertFalse(resolvedCheckout.isWorldActive)
    }
}
