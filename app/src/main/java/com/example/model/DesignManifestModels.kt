package com.example.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Serializable
enum class ManifestSource(val wireName: String) {
    REMOTE("remote"),
    CACHED("cached"),
    STATIC_LIVE_FILE("static-live-file"),
    BUNDLED("bundled")
}

@Serializable
data class DesignManifest(
    @SerialName("manifest_id") val manifestId: String = "melvore_default_issue_v1",
    val slot: String = "default",
    @SerialName("design_version") val designVersion: String = "1.0.0",
    @SerialName("schema_version") val schemaVersion: String = "1.0.0",
    @SerialName("min_app_schema_version") val minAppSchemaVersion: String = "1.0.0",
    @SerialName("release_notes") val releaseNotes: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("checksum_sha256") val checksumSha256: String = "",
    val tokens: DesignTokens = DesignTokens(),
    val navigation: NavigationDesignSpec = NavigationDesignSpec(),
    val components: ComponentVariantsSpec = ComponentVariantsSpec(),
    val media: DesignMediaSpec = DesignMediaSpec(),
    val pages: Map<String, PageLayoutConfig> = emptyMap(),
    val worlds: Map<String, WorldDesignOverride> = emptyMap(),
    @SerialName("occasion_overrides") val occasionOverrides: OccasionPolicySpec = OccasionPolicySpec()
) {
    fun pageConfig(pageKey: String): PageLayoutConfig? = pages[pageKey.lowercase()]

    fun worldOverride(worldSlug: String): WorldDesignOverride? =
        worlds[worldSlug.trim().lowercase()]

    fun hasWorldKitV2(): Boolean =
        worlds.values.any { it.kit?.v2?.let { v2 -> v2.components.isNotEmpty() || v2.icons.isNotEmpty() } == true }

    fun hasWorldKitV3(): Boolean =
        worlds.values.any { it.kit?.v3?.let { v3 -> v3.layouts.isNotEmpty() || v3.widgets.isNotEmpty() } == true }

    fun hasWorldKitV2OrV3(): Boolean = hasWorldKitV2() || hasWorldKitV3()

    fun worldKitCount(): Int = worlds.values.count { it.kit != null }
}

@Serializable
data class DesignTokens(
    val colors: DesignColorTokens = DesignColorTokens(),
    val typography: DesignTypographyTokens = DesignTypographyTokens(),
    val spacing: DesignSpacingTokens = DesignSpacingTokens(),
    val radii: DesignRadiiTokens = DesignRadiiTokens(),
    val motion: DesignMotionTokens = DesignMotionTokens()
)

@Serializable
data class DesignColorTokens(
    val mode: String = "light",
    val background: String = "#FFFFFF",
    val surface: String = "#F7F7FA",
    @SerialName("surface_elevated") val surfaceElevated: String = "#EFEFF5",
    @SerialName("surface_variant") val surfaceVariant: String = "#F1F1F5",
    val primary: String = "#09090B",
    @SerialName("on_primary") val onPrimary: String = "#FFFFFF",
    val accent: String = "#C9A24B",
    @SerialName("secondary_accent") val secondaryAccent: String = "#9CA3AF",
    @SerialName("text_primary") val textPrimary: String = "#09090B",
    @SerialName("text_secondary") val textSecondary: String = "#52525B",
    val border: String = "#E4E4E7",
    @SerialName("badge_bg") val badgeBg: String = "#09090B",
    @SerialName("badge_text") val badgeText: String = "#FFFFFF",
    @SerialName("sale_accent") val saleAccent: String = "#B91C1C",
    @SerialName("success_accent") val successAccent: String = "#15803D",
    val dark: DesignDarkColorTokens? = null
)

@Serializable
data class DesignDarkColorTokens(
    val background: String = "#0B0B10",
    val surface: String = "#14141C",
    @SerialName("surface_elevated") val surfaceElevated: String = "#1D1D28",
    @SerialName("surface_variant") val surfaceVariant: String = "#191923",
    val primary: String = "#FFFFFF",
    @SerialName("on_primary") val onPrimary: String = "#09090B",
    val accent: String = "#D4AF37",
    @SerialName("secondary_accent") val secondaryAccent: String = "#94A3B8",
    @SerialName("text_primary") val textPrimary: String = "#FAFAFA",
    @SerialName("text_secondary") val textSecondary: String = "#A1A1AA",
    val border: String = "#272732",
    @SerialName("badge_bg") val badgeBg: String = "#FAFAFA",
    @SerialName("badge_text") val badgeText: String = "#09090B",
    @SerialName("sale_accent") val saleAccent: String = "#F87171",
    @SerialName("success_accent") val successAccent: String = "#34D399"
)

@Serializable
data class DesignTypographyTokens(
    @SerialName("display_font") val displayFont: String = "editorial_serif",
    @SerialName("body_font") val bodyFont: String = "clean_sans",
    @SerialName("heading_scale") val headingScale: Float = 1.0f,
    @SerialName("body_scale") val bodyScale: Float = 1.0f,
    @SerialName("letter_spacing_eyebrow") val letterSpacingEyebrow: Float = 1.8f
)

@Serializable
data class DesignSpacingTokens(
    @SerialName("screen_horizontal_dp") val screenHorizontalDp: Int = 14,
    @SerialName("section_gap_dp") val sectionGapDp: Int = 24,
    @SerialName("card_padding_dp") val cardPaddingDp: Int = 10,
    @SerialName("grid_gutter_dp") val gridGutterDp: Int = 12
)

@Serializable
data class DesignRadiiTokens(
    @SerialName("card_dp") val cardDp: Int = 6,
    @SerialName("button_dp") val buttonDp: Int = 4,
    @SerialName("chip_dp") val chipDp: Int = 999,
    @SerialName("search_bar_dp") val searchBarDp: Int = 999,
    @SerialName("hero_dp") val heroDp: Int = 4,
    @SerialName("sheet_dp") val sheetDp: Int = 16,
    @SerialName("border_width_dp") val borderWidthDp: Int = 1
)

@Serializable
data class DesignMotionTokens(
    val intensity: String = "subtle",
    @SerialName("hero_parallax") val heroParallax: Boolean = true,
    @SerialName("card_entrance") val cardEntrance: String = "fade_slide",
    @SerialName("carousel_interval_ms") val carouselIntervalMs: Int = 4800,
    @SerialName("duration_ms") val durationMs: Int = 300,
    val easing: String = "ease_out"
)

@Serializable
data class NavigationDesignSpec(
    @SerialName("top_bar_style") val topBarStyle: String = "editorial_masthead",
    @SerialName("top_bar_title") val topBarTitle: String = "MELVORE",
    @SerialName("top_bar_subtitle") val topBarSubtitle: String = "FASHION HOUSE",
    @SerialName("show_search_icon") val showSearchIcon: Boolean = true,
    @SerialName("show_bag_badge") val showBagBadge: Boolean = true,
    @SerialName("bottom_bar_variant") val bottomBarVariant: String = "flat_editorial",
    @SerialName("bottom_bar_radius_dp") val bottomBarRadiusDp: Int = 0,
    @SerialName("bottom_bar_elevation_dp") val bottomBarElevationDp: Int = 0,
    val tabs: List<NavigationTabSpec> = defaultTabs()
) {
    companion object {
        fun defaultTabs(): List<NavigationTabSpec> = listOf(
            NavigationTabSpec("home", "Home", "home", "home_filled", true, 1),
            NavigationTabSpec("discover", "Discover", "explore", "explore_filled", true, 2),
            NavigationTabSpec("videos", "Reels", "play", "play_filled", true, 3),
            NavigationTabSpec("elite", "Luxe", "diamond", "diamond_filled", true, 4),
            NavigationTabSpec("bag", "Bag", "bag", "bag_filled", true, 5)
        )
    }
}

@Serializable
data class NavigationTabSpec(
    val route: String = "",
    val label: String = "",
    @SerialName("icon_key") val iconKey: String = "home",
    @SerialName("active_icon_key") val activeIconKey: String = "home_filled",
    val visible: Boolean = true,
    val order: Int = 1,
    @SerialName("badge_text") val badgeText: String = ""
)

@Serializable
data class ComponentVariantsSpec(
    val button: ButtonVariantSpec = ButtonVariantSpec(),
    @SerialName("search_bar") val searchBar: SearchBarVariantSpec = SearchBarVariantSpec(),
    @SerialName("product_card") val productCard: ProductCardVariantSpec = ProductCardVariantSpec(),
    @SerialName("category_strip") val categoryStrip: CategoryStripVariantSpec = CategoryStripVariantSpec(),
    @SerialName("section_header") val sectionHeader: SectionHeaderVariantSpec = SectionHeaderVariantSpec(),
    val states: StateComponentsVariantSpec = StateComponentsVariantSpec()
)

@Serializable
data class ButtonVariantSpec(
    @SerialName("primary_variant") val primaryVariant: String = "editorial_sharp",
    @SerialName("corner_radius_dp") val cornerRadiusDp: Int = 4,
    @SerialName("min_height_dp") val minHeightDp: Int = 48,
    @SerialName("uppercase_labels") val uppercaseLabels: Boolean = true,
    @SerialName("show_trailing_arrow") val showTrailingArrow: Boolean = false,
    @SerialName("border_width_dp") val borderWidthDp: Int = 1
)

@Serializable
data class SearchBarVariantSpec(
    val variant: String = "pill_elevated",
    val placeholder: String = "Search silhouettes, worlds, or houses…",
    @SerialName("corner_radius_dp") val cornerRadiusDp: Int = 999,
    @SerialName("height_dp") val heightDp: Int = 50,
    @SerialName("leading_icon_key") val leadingIconKey: String = "search",
    @SerialName("show_visual_hint_icon") val showVisualHintIcon: Boolean = true
)

@Serializable
data class ProductCardVariantSpec(
    val variant: String = "editorial_portrait",
    @SerialName("image_aspect_ratio") val imageAspectRatio: Float = 0.78f,
    @SerialName("corner_radius_dp") val cornerRadiusDp: Int = 6,
    @SerialName("show_brand") val showBrand: Boolean = true,
    @SerialName("show_rating") val showRating: Boolean = true,
    @SerialName("show_quick_add") val showQuickAdd: Boolean = true,
    @SerialName("badge_position") val badgePosition: String = "top_left",
    @SerialName("price_alignment") val priceAlignment: String = "start"
)

@Serializable
data class CategoryStripVariantSpec(
    val variant: String = "circular_story",
    val columns: Int = 4,
    @SerialName("show_eyebrow") val showEyebrow: Boolean = true,
    @SerialName("tile_radius_dp") val tileRadiusDp: Int = 999
)

@Serializable
data class SectionHeaderVariantSpec(
    val variant: String = "magazine_serif",
    @SerialName("show_eyebrow") val showEyebrow: Boolean = true,
    @SerialName("show_divider") val showDivider: Boolean = false
)

@Serializable
data class StateComponentsVariantSpec(
    @SerialName("loading_variant") val loadingVariant: String = "shimmer_editorial",
    @SerialName("empty_variant") val emptyVariant: String = "framed_editorial",
    @SerialName("error_variant") val errorVariant: String = "outlined_retry_card",
    @SerialName("empty_title") val emptyTitle: String = "Nothing Curated Here Yet",
    @SerialName("empty_subtitle") val emptySubtitle: String = "Explore seasonal drops or clear active filters to continue browsing.",
    @SerialName("error_cta_label") val errorCtaLabel: String = "Retry"
)

@Serializable
data class DesignMediaSpec(
    @SerialName("hero_fallback_image") val heroFallbackImage: String = "",
    @SerialName("occasion_overlay_opacity") val occasionOverlayOpacity: Float = 0.14f,
    @SerialName("video_autoplay_default") val videoAutoplayDefault: Boolean = true,
    @SerialName("brand_wordmark_url") val brandWordmarkUrl: String = ""
)

@Serializable
data class PageLayoutConfig(
    @SerialName("page_key") val pageKey: String = "home",
    @SerialName("layout_template") val layoutTemplate: String = "editorial_magazine",
    @SerialName("hero_height_dp") val heroHeightDp: Int = 420,
    @SerialName("grid_columns") val gridColumns: Int = 2,
    @SerialName("show_floating_world_strip") val showFloatingWorldStrip: Boolean = true,
    val sections: List<PageSectionConfig> = emptyList()
) {
    fun orderedVisibleSections(): List<PageSectionConfig> =
        sections.filter { it.visible }.sortedBy { it.order }
}

@Serializable
data class PageSectionConfig(
    val id: String = "",
    val type: String = "",
    val visible: Boolean = true,
    val order: Int = 0,
    @SerialName("layout_variant") val layoutVariant: String = "",
    val eyebrow: String = "",
    val title: String = "",
    val subtitle: String = "",
    @SerialName("cta_label") val ctaLabel: String = "",
    val columns: Int = 2
)

@Serializable
data class WorldDesignOverride(
    val template: String = "editorial_magazine",
    @SerialName("hero_layout") val heroLayout: String = "asymmetrical_split",
    @SerialName("font_style") val fontStyle: String = "editorial_serif",
    @SerialName("button_style") val buttonStyle: String = "editorial_sharp",
    @SerialName("card_radius_dp") val cardRadiusDp: Int = 8,
    @SerialName("grid_columns") val gridColumns: Int = 2,
    @SerialName("section_order") val sectionOrder: List<String> = emptyList(),
    val kit: WorldKitSpec? = null
)

@Serializable
data class WorldKitSpec(
    val version: String = "1.2.0",
    @SerialName("svg_base") val svgBase: String = "",
    @SerialName("button_skin") val buttonSkin: String = "",
    val svg: WorldKitSvgSpec = WorldKitSvgSpec(),
    val slices: WorldKitSlicesSpec = WorldKitSlicesSpec(),
    @SerialName("label_slots") val labelSlots: WorldKitLabelSlotsSpec = WorldKitLabelSlotsSpec(),
    val icons: Map<String, WorldKitIconPair> = emptyMap(),
    @SerialName("tab_labels") val tabLabels: List<String> = emptyList(),
    @SerialName("motion_preset") val motionPreset: String = "",
    val palette: WorldKitPaletteSpec? = null,
    val typography: WorldKitTypographySpec? = null,
    val components: WorldKitComponentsSpec? = null,
    val navigation: WorldKitNavigationSpec? = null,
    val ambient: List<WorldKitAmbientItem> = emptyList(),
    val budget: WorldKitBudgetSpec = WorldKitBudgetSpec(),
    val fonts: String = "",
    @SerialName("hero_image") val heroImage: String = "",
    val v2: WorldKitV2Spec? = null,
    val v3: WorldKitV3Spec? = null
) {
    fun componentPath(key: String): String? {
        val clean = key.trim().lowercase()
        v2?.components?.get(clean)?.takeIf { it.isNotBlank() }?.let { return it }
        return when (clean) {
            "button" -> svg.button.takeIf { it.isNotBlank() }
            "button_ghost" -> svg.buttonGhost.takeIf { it.isNotBlank() }
            "navbar" -> svg.navbar.takeIf { it.isNotBlank() }
            "nav_indicator" -> svg.navIndicator.takeIf { it.isNotBlank() }
            "slider" -> svg.slider.takeIf { it.isNotBlank() }
            "pager" -> svg.pager.takeIf { it.isNotBlank() }
            "divider" -> svg.divider.takeIf { it.isNotBlank() }
            "card_frame" -> svg.cardFrame.takeIf { it.isNotBlank() }
            "loader" -> svg.loader.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    fun iconPath(key: String, active: Boolean = false): String? {
        val clean = key.trim().lowercase()
        val pair = v2?.icons?.get(clean) ?: icons[clean] ?: return null
        return if (active) {
            pair.activeIcon.takeIf { it.isNotBlank() } ?: pair.icon.takeIf { it.isNotBlank() }
        } else {
            pair.icon.takeIf { it.isNotBlank() }
        }
    }

    fun layoutPath(screenKey: String): String? =
        v3?.layouts?.get(screenKey.trim().lowercase())?.takeIf { it.isNotBlank() }

    fun widgetPath(widgetKey: String): String? =
        v3?.widgets?.get(widgetKey.trim().lowercase())?.takeIf { it.isNotBlank() }
}

object KitDimensionSpecSerializer : KSerializer<KitDimensionSpec> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("KitDimensionSpec") {
        element<Int>("width", isOptional = true)
        element<Int>("height", isOptional = true)
        element<Int>("w", isOptional = true)
        element<Int>("h", isOptional = true)
    }

    override fun deserialize(decoder: Decoder): KitDimensionSpec {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return KitDimensionSpec()
        return when (val el = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> {
                val wVal = el.getOrNull(0)?.jsonPrimitive?.intOrNull ?: 0
                val hVal = el.getOrNull(1)?.jsonPrimitive?.intOrNull ?: 0
                KitDimensionSpec(width = wVal, height = hVal, w = wVal, h = hVal)
            }
            is JsonObject -> {
                val width = el["width"]?.jsonPrimitive?.intOrNull ?: 0
                val height = el["height"]?.jsonPrimitive?.intOrNull ?: 0
                val w = el["w"]?.jsonPrimitive?.intOrNull ?: 0
                val h = el["h"]?.jsonPrimitive?.intOrNull ?: 0
                KitDimensionSpec(width = width, height = height, w = w, h = h)
            }
            is JsonPrimitive -> {
                val single = el.intOrNull ?: 0
                KitDimensionSpec(width = single, height = single, w = single, h = single)
            }
            else -> KitDimensionSpec()
        }
    }

    override fun serialize(encoder: Encoder, value: KitDimensionSpec) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            jsonEncoder.encodeJsonElement(
                buildJsonObject {
                    put("width", value.resolvedWidth)
                    put("height", value.resolvedHeight)
                    put("w", value.resolvedWidth)
                    put("h", value.resolvedHeight)
                }
            )
        } else {
            encoder.encodeString("${value.resolvedWidth}x${value.resolvedHeight}")
        }
    }
}

@Serializable(with = KitDimensionSpecSerializer::class)
data class KitDimensionSpec(
    val width: Int = 0,
    val height: Int = 0,
    val w: Int = 0,
    val h: Int = 0
) {
    val resolvedWidth: Int get() = if (width > 0) width else w
    val resolvedHeight: Int get() = if (height > 0) height else h
}

@Serializable
data class WorldKitV2Spec(
    val components: Map<String, String> = emptyMap(),
    val sizes: Map<String, KitDimensionSpec> = emptyMap(),
    val icons: Map<String, WorldKitIconPair> = emptyMap()
)

@Serializable
data class WorldKitV3Spec(
    val layouts: Map<String, String> = emptyMap(),
    val widgets: Map<String, String> = emptyMap(),
    @SerialName("widget_sizes") val widgetSizes: Map<String, KitDimensionSpec> = emptyMap()
)

/**
 * High-level runtime facade over [DesignManifest] exposing resolved asset URLs
 * (`asset_base_url`), `manifest_hash`, ETag, and typed [WorldKit] helpers for kit.v2 / kit.v3.
 */
data class DesignKit(
    val manifest: DesignManifest = DesignManifest(),
    val assetBaseUrl: String = "",
    val manifestHash: String = "",
    val etag: String = "",
    val activeDesign: Int = 0
) {
    fun world(slug: String?): WorldKit? {
        val clean = slug?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        val override = manifest.worldOverride(clean) ?: return null
        return WorldKit(
            slug = clean,
            override = override,
            assetBaseUrl = assetBaseUrl
        )
    }

    fun url(relativeOrAbsolutePath: String?): String? {
        val clean = relativeOrAbsolutePath?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
            return clean
        }
        val base = assetBaseUrl.trim().trimEnd('/')
        if (base.isBlank()) return clean
        return "$base/${clean.trimStart('/')}"
    }
}

data class WorldKit(
    val slug: String,
    val override: WorldDesignOverride,
    val assetBaseUrl: String = ""
) {
    val spec: WorldKitSpec? get() = override.kit

    fun layoutPath(key: String): String? = spec?.layoutPath(key)
    fun widgetPath(key: String): String? = spec?.widgetPath(key)
    fun componentPath(key: String): String? = spec?.componentPath(key)
    fun iconPath(key: String, active: Boolean = false): String? = spec?.iconPath(key, active)

    fun url(path: String?): String? {
        val clean = path?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
            return clean
        }
        val base = assetBaseUrl.trim().trimEnd('/')
        if (base.isBlank()) return clean
        return "$base/${clean.trimStart('/')}"
    }
}

@Serializable
data class WorldKitSvgSpec(
    val button: String = "",
    @SerialName("button_ghost") val buttonGhost: String = "",
    val navbar: String = "",
    @SerialName("nav_indicator") val navIndicator: String = "",
    val slider: String = "",
    val pager: String = "",
    val divider: String = "",
    @SerialName("card_frame") val cardFrame: String = "",
    val loader: String = ""
)

@Serializable
data class WorldKitSliceInset(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
    val all: Int = 0
)

@Serializable
data class WorldKitSlicesSpec(
    val button: WorldKitSliceInset = WorldKitSliceInset(),
    @SerialName("button_ghost") val buttonGhost: WorldKitSliceInset = WorldKitSliceInset(),
    val navbar: WorldKitSliceInset = WorldKitSliceInset(),
    @SerialName("card_frame") val cardFrame: WorldKitSliceInset = WorldKitSliceInset()
)

@Serializable
data class WorldKitSlotRect(
    val x: Int = 0,
    val w: Int = 0
)

@Serializable
data class WorldKitLabelSlotsSpec(
    val button: WorldKitSlotRect = WorldKitSlotRect(),
    @SerialName("navbar_tabs") val navbarTabs: Int = 5
)

@Serializable
data class WorldKitIconPair(
    val icon: String = "",
    @SerialName("active_icon") val activeIcon: String = ""
)

@Serializable
data class WorldKitPaletteSpec(
    val background: String = "",
    val surface: String = "",
    @SerialName("text_primary") val textPrimary: String = "",
    @SerialName("text_secondary") val textSecondary: String = "",
    val accent: String = "",
    @SerialName("secondary_accent") val secondaryAccent: String = "",
    val border: String = "",
    val mode: String = ""
)

@Serializable
data class WorldKitFontRoles(
    val heading: String = "",
    val button: String = "",
    val navigation: String = "",
    val eyebrow: String = "",
    val price: String = "",
    val body: String = ""
)

@Serializable
data class WorldKitTypographySpec(
    @SerialName("display_font") val displayFont: String = "",
    @SerialName("body_font") val bodyFont: String = "",
    @SerialName("heading_scale") val headingScale: Float = 1.0f,
    @SerialName("letter_spacing_eyebrow") val letterSpacingEyebrow: Float = 2.0f,
    @SerialName("pixel_text") val pixelText: Boolean = false,
    @SerialName("font_roles") val fontRoles: WorldKitFontRoles? = null
)

@Serializable
data class WorldKitComponentsSpec(
    val button: ButtonVariantSpec? = null,
    @SerialName("search_bar") val searchBar: SearchBarVariantSpec? = null,
    @SerialName("product_card") val productCard: ProductCardVariantSpec? = null,
    @SerialName("section_header") val sectionHeader: SectionHeaderVariantSpec? = null,
    val states: StateComponentsVariantSpec? = null
)

@Serializable
data class WorldKitNavigationSpec(
    @SerialName("bottom_bar_variant") val bottomBarVariant: String = "",
    @SerialName("bottom_bar_radius_dp") val bottomBarRadiusDp: Int = 0
)

@Serializable
data class WorldKitAmbientItem(
    @SerialName("asset_key") val assetKey: String = "",
    @SerialName("max_count") val maxCount: Int = 0
)

@Serializable
data class WorldKitBudgetSpec(
    @SerialName("max_svg_kb") val maxSvgKb: Int = 12,
    @SerialName("max_ambient_particles") val maxAmbientParticles: Int = 8,
    @SerialName("fps_cap") val fpsCap: Int = 30,
    @SerialName("animate_properties") val animateProperties: List<String> = emptyList(),
    val filters: String = "none",
    @SerialName("pause_offscreen") val pauseOffscreen: Boolean = true
)

@Serializable
data class OccasionPolicySpec(
    @SerialName("default_activation_days_before") val defaultActivationDaysBefore: Int = 8,
    @SerialName("default_timezone") val defaultTimezone: String = "Asia/Kolkata",
    @SerialName("max_banner_elevation_dp") val maxBannerElevationDp: Int = 4,
    @SerialName("preserve_world_layout") val preserveWorldLayout: Boolean = true,
    @SerialName("max_ambient_particles") val maxAmbientParticles: Int = 10,
    @SerialName("fps_cap") val fpsCap: Int = 30,
    @SerialName("respect_reduced_motion") val respectReducedMotion: Boolean = true,
    val occasions: Map<String, OccasionKitSpec> = emptyMap()
) {
    fun occasionKit(slug: String?): OccasionKitSpec? {
        val raw = slug?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        occasions[raw]?.let { return it }
        val normalized = raw.replace('_', '-').replace(' ', '-')
        occasions[normalized]?.let { return it }

        // Prefix or keyword alias fallback so DB slugs like "diwali-2026" or "holiday-gala-2026"
        // automatically map onto the 12 server SVG occasion kits.
        val aliasTarget = when {
            normalized.contains("diwali") || normalized.contains("deepavali") -> "diwali-luxe-edit"
            normalized.contains("christmas") || normalized.contains("xmas") || normalized.contains("holiday-gala") -> "christmas-edit"
            normalized.contains("new-year") || normalized.contains("newyear") || normalized.contains("nye") -> "new-year-countdown"
            normalized.contains("valentine") -> "valentines-edit"
            normalized.contains("holi") -> "holi-colour-edit"
            normalized.contains("eid") || normalized.contains("ramadan") -> "eid-festive-edit"
            normalized.contains("halloween") || normalized.contains("spooky") -> "halloween-noir"
            normalized.contains("wedding") || normalized.contains("bridal") || normalized.contains("shaadi") -> "wedding-season"
            normalized.contains("black-friday") || normalized.contains("blackfriday") -> "black-friday-drop"
            normalized.contains("end-of-season") || normalized.contains("eoss") || normalized.contains("sale") -> "end-of-season-edit"
            normalized.contains("winter") || normalized.contains("solstice") -> "winter-atelier"
            normalized.contains("season") || normalized.contains("spotlight") -> "season-editorial-spotlight"
            else -> occasions.keys.firstOrNull { key ->
                normalized.startsWith(key.substringBefore('-')) || key.startsWith(normalized.substringBefore('-'))
            }
        }
        return aliasTarget?.let { occasions[it] }
    }
}

@Serializable
data class OccasionKitSpec(
    val motif: String = "",
    @SerialName("motif_secondary") val motifSecondary: String = "",
    val colors: OccasionKitColors = OccasionKitColors(),
    val svg: OccasionKitSvgSpec = OccasionKitSvgSpec(),
    @SerialName("motion_preset") val motionPreset: String = "",
    @SerialName("overlay_opacity") val overlayOpacity: Float = 0.16f,
    @SerialName("preserve_world_layout") val preserveWorldLayout: Boolean = true
)

@Serializable
data class OccasionKitColors(
    val primary: String = "",
    val accent: String = "",
    val background: String = "",
    @SerialName("cta_face") val ctaFace: String = ""
)

@Serializable
data class OccasionKitSvgSpec(
    val button: String = "",
    @SerialName("button_ghost") val buttonGhost: String = "",
    val banner: String = "",
    val divider: String = "",
    val corner: String = ""
)

@Serializable
data class ScheduledOccasionSpec(
    val occasionId: Int = 0,
    val slug: String = "",
    val name: String = "",
    val eventDate: String? = null,
    val activationDaysBefore: Int = 8,
    val activationStartAt: String? = null,
    val activationEndAt: String? = null,
    val timezone: String = "Asia/Kolkata",
    val regionCountry: String = "ALL",
    val regionState: String = "",
    val regionCity: String = "",
    val priority: Int = 0,
    val isActive: Boolean = true
)

data class OccasionResolutionResult(
    val winner: ScheduledOccasionSpec?,
    val activeMatches: List<ScheduledOccasionSpec>,
    val computedWindowStartIst: ZonedDateTime?,
    val computedWindowEndIst: ZonedDateTime?
)

/**
 * Pure Kotlin deterministic evaluator mirroring Melvore PHP ThemeEngineService 8-day-before
 * IST occasion scheduling, regional specificity ranking, and priority tie-breaking.
 */
object OccasionScheduleEvaluator {
    private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE
    private val DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun computeWindow(spec: ScheduledOccasionSpec): Pair<ZonedDateTime?, ZonedDateTime?> {
        val zone = runCatching { ZoneId.of(spec.timezone.ifBlank { "Asia/Kolkata" }) }
            .getOrElse { ZoneId.of("Asia/Kolkata") }
        val days = spec.activationDaysBefore.coerceAtLeast(0).toLong()

        val start = spec.activationStartAt?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { LocalDateTime.parse(raw, DT_FMT).atZone(zone) }.getOrNull()
        } ?: spec.eventDate?.takeIf { it.isNotBlank() }?.let { rawDate ->
            runCatching {
                LocalDate.parse(rawDate, DATE_FMT).minusDays(days).atStartOfDay(zone)
            }.getOrNull()
        }

        val end = spec.activationEndAt?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { LocalDateTime.parse(raw, DT_FMT).atZone(zone) }.getOrNull()
        } ?: spec.eventDate?.takeIf { it.isNotBlank() }?.let { rawDate ->
            runCatching {
                LocalDate.parse(rawDate, DATE_FMT).atTime(23, 59, 59).atZone(zone)
            }.getOrNull()
        }

        return start to end
    }

    fun evaluateAt(
        occasions: List<ScheduledOccasionSpec>,
        evaluationTime: ZonedDateTime,
        country: String = "India",
        state: String = "",
        city: String = ""
    ): OccasionResolutionResult {
        data class Candidate(
            val occasion: ScheduledOccasionSpec,
            val specificity: Int,
            val start: ZonedDateTime?,
            val end: ZonedDateTime?
        )

        val matched = mutableListOf<Candidate>()
        for (occ in occasions) {
            if (!occ.isActive) continue
            val (start, end) = computeWindow(occ)
            val zone = start?.zone ?: end?.zone ?: ZoneId.of("Asia/Kolkata")
            val localNow = evaluationTime.withZoneSameInstant(zone)
            if (start != null && localNow.isBefore(start)) continue
            if (end != null && localNow.isAfter(end)) continue

            var specificity = 0
            val rCountry = occ.regionCountry.trim()
            val rState = occ.regionState.trim()
            val rCity = occ.regionCity.trim()

            if (rCountry.isNotEmpty() && !rCountry.equals("ALL", ignoreCase = true)) {
                if (!rCountry.equals(country.trim(), ignoreCase = true)) continue
                specificity += 1
            }
            if (rState.isNotEmpty()) {
                if (!rState.equals(state.trim(), ignoreCase = true)) continue
                specificity += 2
            }
            if (rCity.isNotEmpty()) {
                if (!rCity.equals(city.trim(), ignoreCase = true)) continue
                specificity += 4
            }

            matched += Candidate(occ, specificity, start, end)
        }

        val sorted = matched.sortedWith(
            compareByDescending<Candidate> { it.occasion.priority }
                .thenByDescending { it.specificity }
                .thenBy { it.occasion.occasionId }
        )
        val top = sorted.firstOrNull()
        return OccasionResolutionResult(
            winner = top?.occasion,
            activeMatches = sorted.map { it.occasion },
            computedWindowStartIst = top?.start,
            computedWindowEndIst = top?.end
        )
    }
}

object DesignManifestSupport {
    const val SUPPORTED_APP_SCHEMA_VERSION = "1.0.0"

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    fun isHexColorValid(value: String): Boolean {
        val clean = value.trim()
        return Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$").matches(clean)
    }

    fun isSchemaCompatible(minRequiredVersion: String, currentAppSchema: String = SUPPORTED_APP_SCHEMA_VERSION): Boolean {
        return compareSemver(currentAppSchema, minRequiredVersion) >= 0
    }

    fun isVersionNewer(candidate: String, baseline: String): Boolean {
        return compareSemver(candidate, baseline) > 0
    }

    fun compareSemver(a: String, b: String): Int {
        val aParts = a.trim().removePrefix("v").split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val bParts = b.trim().removePrefix("v").split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val maxLen = maxOf(aParts.size, bParts.size, 3)
        for (i in 0 until maxLen) {
            val av = aParts.getOrElse(i) { 0 }
            val bv = bParts.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }

    fun sha256Canonical(rawJsonObject: JsonObject): String {
        val cleaned = sortJsonElement(
            JsonObject(rawJsonObject.filterKeys { it != "checksum_sha256" })
        )
        val canonicalString = json.encodeToString(JsonElement.serializer(), cleaned)
        val digest = MessageDigest.getInstance("SHA-256").digest(canonicalString.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun sortJsonElement(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.entries.sortedBy { it.key }.associate { it.key to sortJsonElement(it.value) }
        )
        is JsonArray -> JsonArray(element.map { sortJsonElement(it) })
        else -> element
    }
}
