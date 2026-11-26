package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.ComponentVariantsSpec
import com.example.model.DesignColorTokens
import com.example.model.DesignManifest
import com.example.model.DesignManifestSupport
import com.example.model.DesignMediaSpec
import com.example.model.DesignMotionTokens
import com.example.model.DesignRadiiTokens
import com.example.model.DesignSpacingTokens
import com.example.model.DesignTokens
import com.example.model.DesignTypographyTokens
import com.example.model.ManifestSource
import com.example.model.NavigationDesignSpec
import com.example.model.OccasionKitSpec
import com.example.model.OccasionPolicySpec
import com.example.model.PageLayoutConfig
import com.example.model.PageSectionConfig
import com.example.model.WorldDesignOverride
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ManifestResolutionOutcome(
    val manifest: DesignManifest,
    val activeDesign: Int,
    val slot: String,
    val usedFallback: Boolean,
    val fallbackReason: String? = null,
    val assetBaseUrl: String = "",
    val manifestHash: String = "",
    val etag: String = "",
    val source: ManifestSource = ManifestSource.BUNDLED,
    val warnings: List<String> = emptyList()
) {
    val manifestVersion: String get() = manifest.designVersion

    fun toDesignKit(defaultAssetBaseUrl: String = ""): com.example.model.DesignKit =
        com.example.model.DesignKit(
            manifest = manifest,
            assetBaseUrl = assetBaseUrl.ifBlank { defaultAssetBaseUrl },
            manifestHash = manifestHash.ifBlank { manifest.checksumSha256 },
            etag = etag,
            activeDesign = activeDesign
        )
}

/**
 * Result of section-by-section tolerant manifest decoding.
 * One malformed field or section never discards the rest of the manifest.
 */
data class TolerantManifestParseResult(
    val manifest: DesignManifest,
    val warnings: List<String> = emptyList()
)

/**
 * Manages loading, tolerant per-section parsing, validation, SHA-256 verification,
 * schema compatibility checks, local caching, visible diagnostics, and safe rollback
 * of Melvore remote design manifests.
 * Never executes downloaded code — strictly deserializes declarative JSON tokens.
 */
class RemoteDesignManifestManager(
    private val context: Context?,
    private val sessionStore: SecureSessionStore? = null,
    private val appSchemaVersion: String = DesignManifestSupport.SUPPORTED_APP_SCHEMA_VERSION
) {
    private val json = DesignManifestSupport.json

    fun loadInitialManifest(): ManifestResolutionOutcome {
        val cachedJson = sessionStore?.getLastKnownGoodManifestJson()
        if (!cachedJson.isNullOrBlank()) {
            val cachedOutcome = validateAndAdopt(
                rawManifestJson = cachedJson,
                activeDesign = sessionStore?.getLastKnownGoodActiveDesign() ?: 0,
                persistIfValid = false,
                sourceHint = ManifestSource.CACHED
            )
            if (!cachedOutcome.usedFallback || cachedOutcome.source == ManifestSource.CACHED) {
                return cachedOutcome.copy(
                    source = ManifestSource.CACHED,
                    assetBaseUrl = sessionStore?.getLastKnownGoodAssetBaseUrl().orEmpty(),
                    manifestHash = sessionStore?.getLastKnownGoodManifestHash().orEmpty()
                        .ifBlank { cachedOutcome.manifestHash },
                    etag = sessionStore?.getLastKnownGoodEtag().orEmpty()
                )
            }
        }
        val bundled = loadBundledDefaultManifest()
        return ManifestResolutionOutcome(
            manifest = bundled,
            activeDesign = 0,
            slot = "default",
            usedFallback = false,
            manifestHash = bundled.checksumSha256,
            source = ManifestSource.BUNDLED
        )
    }

    fun loadBundledDefaultManifest(): DesignManifest {
        val ctx = context
        if (ctx != null) {
            val raw = runCatching {
                ctx.assets.open(BUNDLED_DEFAULT_ASSET).bufferedReader().use { it.readText() }
            }.getOrNull()
            if (!raw.isNullOrBlank()) {
                val rawObj = runCatching { json.decodeFromString<JsonObject>(raw) }.getOrNull()
                if (rawObj != null) {
                    val tolerant = parseTolerantly(rawObj, emergencyDefaultManifest())
                    if (tolerant.warnings.isNotEmpty()) {
                        logWarning("Bundled default manifest had warnings: ${tolerant.warnings.joinToString("; ")}")
                    }
                    return tolerant.manifest
                }
            }
        }
        return emergencyDefaultManifest()
    }

    /**
     * Parses each top-level section (`tokens`, `navigation`, `components`, `media`, `pages`,
     * `worlds`, `occasion_overrides`) independently with `runCatching`.
     * On failure for a section (or sub-item), keeps the baseline default for that section only
     * and records a human-readable diagnostic warning.
     */
    fun parseTolerantly(
        rawObj: JsonObject,
        baseline: DesignManifest = loadBundledDefaultManifest()
    ): TolerantManifestParseResult {
        val warnings = mutableListOf<String>()

        val manifestId = rawObj["manifest_id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: baseline.manifestId
        val rawSlot = rawObj["slot"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase().orEmpty()
        val slot = if (rawSlot in listOf("default", "live")) {
            rawSlot
        } else {
            if (rawSlot.isNotBlank()) warnings += "Invalid slot '$rawSlot'; kept '${baseline.slot}'."
            baseline.slot
        }
        val designVersion = rawObj["design_version"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: baseline.designVersion
        val schemaVersion = rawObj["schema_version"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: baseline.schemaVersion
        val minAppSchemaVersion = rawObj["min_app_schema_version"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: baseline.minAppSchemaVersion
        val releaseNotes = rawObj["release_notes"]?.jsonPrimitive?.contentOrNull ?: baseline.releaseNotes
        val publishedAt = rawObj["published_at"]?.jsonPrimitive?.contentOrNull ?: baseline.publishedAt
        val checksumSha256 = rawObj["checksum_sha256"]?.jsonPrimitive?.contentOrNull ?: baseline.checksumSha256

        // 1. tokens (sub-section tolerant: colors, typography, spacing, radii, motion)
        val tokens = parseTokensSection(rawObj["tokens"], baseline.tokens, warnings)

        // 2. navigation
        val navigation = rawObj["navigation"]?.let { el ->
            runCatching {
                val decoded = json.decodeFromJsonElement<NavigationDesignSpec>(el)
                if (decoded.tabs.count { it.visible } < 3) {
                    warnings += "navigation: must contain at least 3 visible tabs; kept default navigation."
                    baseline.navigation
                } else {
                    decoded
                }
            }.getOrElse { ex ->
                warnings += "navigation: parse failed (${ex.message}); kept default navigation."
                baseline.navigation
            }
        } ?: baseline.navigation

        // 3. components
        val components = rawObj["components"]?.let { el ->
            runCatching {
                json.decodeFromJsonElement<ComponentVariantsSpec>(el)
            }.getOrElse { ex ->
                warnings += "components: parse failed (${ex.message}); kept default components."
                baseline.components
            }
        } ?: baseline.components

        // 4. media
        val media = rawObj["media"]?.let { el ->
            runCatching {
                json.decodeFromJsonElement<DesignMediaSpec>(el)
            }.getOrElse { ex ->
                warnings += "media: parse failed (${ex.message}); kept default media."
                baseline.media
            }
        } ?: baseline.media

        // 5. pages (entry-by-entry tolerant; always ensures 'home' exists)
        val pages = parsePagesSection(rawObj["pages"], baseline.pages, warnings)

        // 6. worlds (entry-by-entry tolerant so one broken world never drops the other 11)
        val worlds = parseWorldsSection(rawObj["worlds"], baseline.worlds, warnings)

        // 7. occasion_overrides (entry-by-entry tolerant for occasions map)
        val occasionOverrides = parseOccasionOverridesSection(
            rawObj["occasion_overrides"],
            baseline.occasionOverrides,
            warnings
        )

        val merged = DesignManifest(
            manifestId = manifestId,
            slot = slot,
            designVersion = designVersion,
            schemaVersion = schemaVersion,
            minAppSchemaVersion = minAppSchemaVersion,
            releaseNotes = releaseNotes,
            publishedAt = publishedAt,
            checksumSha256 = checksumSha256,
            tokens = tokens,
            navigation = navigation,
            components = components,
            media = media,
            pages = pages,
            worlds = worlds,
            occasionOverrides = occasionOverrides
        )
        return TolerantManifestParseResult(manifest = merged, warnings = warnings)
    }

    private fun parseTokensSection(
        element: kotlinx.serialization.json.JsonElement?,
        baseline: DesignTokens,
        warnings: MutableList<String>
    ): DesignTokens {
        if (element == null) return baseline
        val obj = runCatching { element.jsonObject }.getOrElse { ex ->
            warnings += "tokens: expected object (${ex.message}); kept default tokens."
            return baseline
        }
        val colors = obj["colors"]?.let { colorsEl ->
            runCatching {
                val decoded = json.decodeFromJsonElement<DesignColorTokens>(colorsEl)
                sanitizeColorTokens(decoded, baseline.colors, warnings)
            }.getOrElse { ex ->
                warnings += "tokens.colors: parse failed (${ex.message}); kept default colors."
                baseline.colors
            }
        } ?: baseline.colors

        val typography = obj["typography"]?.let { el ->
            runCatching { json.decodeFromJsonElement<DesignTypographyTokens>(el) }.getOrElse { ex ->
                warnings += "tokens.typography: parse failed (${ex.message}); kept default typography."
                baseline.typography
            }
        } ?: baseline.typography

        val spacing = obj["spacing"]?.let { el ->
            runCatching { json.decodeFromJsonElement<DesignSpacingTokens>(el) }.getOrElse { ex ->
                warnings += "tokens.spacing: parse failed (${ex.message}); kept default spacing."
                baseline.spacing
            }
        } ?: baseline.spacing

        val radii = obj["radii"]?.let { el ->
            runCatching { json.decodeFromJsonElement<DesignRadiiTokens>(el) }.getOrElse { ex ->
                warnings += "tokens.radii: parse failed (${ex.message}); kept default radii."
                baseline.radii
            }
        } ?: baseline.radii

        val motion = obj["motion"]?.let { el ->
            runCatching { json.decodeFromJsonElement<DesignMotionTokens>(el) }.getOrElse { ex ->
                warnings += "tokens.motion: parse failed (${ex.message}); kept default motion."
                baseline.motion
            }
        } ?: baseline.motion

        return DesignTokens(
            colors = colors,
            typography = typography,
            spacing = spacing,
            radii = radii,
            motion = motion
        )
    }

    private fun sanitizeColorTokens(
        candidate: DesignColorTokens,
        fallback: DesignColorTokens,
        warnings: MutableList<String>
    ): DesignColorTokens {
        fun checkHex(field: String, value: String, defaultValue: String): String {
            return if (DesignManifestSupport.isHexColorValid(value)) {
                value
            } else {
                warnings += "Invalid hex for $field: $value."
                defaultValue
            }
        }
        return candidate.copy(
            background = checkHex("background", candidate.background, fallback.background),
            surface = checkHex("surface", candidate.surface, fallback.surface),
            surfaceElevated = checkHex("surface_elevated", candidate.surfaceElevated, fallback.surfaceElevated),
            surfaceVariant = checkHex("surface_variant", candidate.surfaceVariant, fallback.surfaceVariant),
            primary = checkHex("primary", candidate.primary, fallback.primary),
            onPrimary = checkHex("on_primary", candidate.onPrimary, fallback.onPrimary),
            accent = checkHex("accent", candidate.accent, fallback.accent),
            secondaryAccent = checkHex("secondary_accent", candidate.secondaryAccent, fallback.secondaryAccent),
            textPrimary = checkHex("text_primary", candidate.textPrimary, fallback.textPrimary),
            textSecondary = checkHex("text_secondary", candidate.textSecondary, fallback.textSecondary),
            border = checkHex("border", candidate.border, fallback.border)
        )
    }

    private fun parsePagesSection(
        element: kotlinx.serialization.json.JsonElement?,
        baseline: Map<String, PageLayoutConfig>,
        warnings: MutableList<String>
    ): Map<String, PageLayoutConfig> {
        if (element == null) {
            if (!baseline.containsKey("home")) {
                warnings += "pages.home configuration is required; kept default pages."
            }
            return baseline
        }
        val obj = runCatching { element.jsonObject }.getOrElse { ex ->
            warnings += "pages: expected object (${ex.message}); kept default pages."
            return baseline
        }
        val result = LinkedHashMap<String, PageLayoutConfig>(baseline)
        for ((key, value) in obj) {
            val cleanKey = key.trim().lowercase()
            runCatching {
                result[cleanKey] = json.decodeFromJsonElement<PageLayoutConfig>(value)
            }.onFailure { ex ->
                warnings += "pages.$cleanKey: parse failed (${ex.message}); kept default."
            }
        }
        if (!result.containsKey("home")) {
            warnings += "pages.home configuration is required; restored default home page."
            emergencyDefaultManifest().pages["home"]?.let { result["home"] = it }
        }
        return result
    }

    private fun parseWorldsSection(
        element: kotlinx.serialization.json.JsonElement?,
        baseline: Map<String, WorldDesignOverride>,
        warnings: MutableList<String>
    ): Map<String, WorldDesignOverride> {
        if (element == null) return baseline
        val obj = runCatching { element.jsonObject }.getOrElse { ex ->
            warnings += "worlds: expected object (${ex.message}); kept default worlds."
            return baseline
        }
        val result = LinkedHashMap<String, WorldDesignOverride>()
        for ((slug, value) in obj) {
            val cleanSlug = slug.trim().lowercase()
            runCatching {
                result[cleanSlug] = json.decodeFromJsonElement<WorldDesignOverride>(value)
            }.onFailure { ex ->
                warnings += "worlds.$cleanSlug: parse failed (${ex.message}); skipped malformed world."
                baseline[cleanSlug]?.let { result[cleanSlug] = it }
            }
        }
        return if (result.isEmpty()) baseline else result
    }

    private fun parseOccasionOverridesSection(
        element: kotlinx.serialization.json.JsonElement?,
        baseline: OccasionPolicySpec,
        warnings: MutableList<String>
    ): OccasionPolicySpec {
        if (element == null) return baseline
        val direct = runCatching { json.decodeFromJsonElement<OccasionPolicySpec>(element) }.getOrNull()
        if (direct != null) return direct

        val obj = runCatching { element.jsonObject }.getOrElse { ex ->
            warnings += "occasion_overrides: parse failed (${ex.message}); kept default occasions."
            return baseline
        }
        val parsedOccasions = LinkedHashMap<String, OccasionKitSpec>()
        val occObj = runCatching { obj["occasions"]?.jsonObject }.getOrNull()
        if (occObj != null) {
            for ((slug, value) in occObj) {
                runCatching {
                    parsedOccasions[slug.trim().lowercase()] = json.decodeFromJsonElement<OccasionKitSpec>(value)
                }.onFailure { ex ->
                    warnings += "occasion_overrides.$slug: parse failed (${ex.message}); skipped."
                }
            }
        } else {
            warnings += "occasion_overrides: malformed section; kept defaults for invalid fields."
        }
        return baseline.copy(
            defaultActivationDaysBefore = obj["default_activation_days_before"]?.jsonPrimitive?.intOrNull
                ?: baseline.defaultActivationDaysBefore,
            defaultTimezone = obj["default_timezone"]?.jsonPrimitive?.contentOrNull
                ?: baseline.defaultTimezone,
            preserveWorldLayout = obj["preserve_world_layout"]?.jsonPrimitive?.booleanOrNull
                ?: baseline.preserveWorldLayout,
            occasions = parsedOccasions.ifEmpty { baseline.occasions }
        )
    }

    /**
     * Validates an incoming remote manifest (object or raw JSON) against schema rules,
     * app compatibility version, and optional SHA-256 checksum.
     * On critical failure (null payload, incompatible schema, checksum mismatch), rolls back
     * to last-known-good or bundled default and logs a visible warning.
     */
    fun validateAndAdopt(
        remoteManifest: DesignManifest?,
        activeDesign: Int,
        expectedChecksum: String? = null,
        rawJsonObject: JsonObject? = null,
        persistIfValid: Boolean = true,
        etag: String = "",
        assetBaseUrl: String = "",
        manifestHash: String = "",
        sourceHint: ManifestSource = ManifestSource.REMOTE,
        parseWarnings: List<String> = emptyList()
    ): ManifestResolutionOutcome {
        if (remoteManifest == null) {
            return fallbackOutcome("Remote manifest payload is null.")
        }

        if (!DesignManifestSupport.isSchemaCompatible(remoteManifest.minAppSchemaVersion, appSchemaVersion)) {
            return fallbackOutcome(
                "Manifest min_app_schema_version ${remoteManifest.minAppSchemaVersion} exceeds app schema $appSchemaVersion."
            )
        }

        if (!expectedChecksum.isNullOrBlank() && rawJsonObject != null) {
            val computed = DesignManifestSupport.sha256Canonical(rawJsonObject)
            if (!computed.equals(expectedChecksum.trim(), ignoreCase = true)) {
                return fallbackOutcome("Manifest SHA-256 checksum mismatch (expected=$expectedChecksum actual=$computed).")
            }
        }

        val errors = validateManifestObject(remoteManifest)
        if (errors.isNotEmpty()) {
            return fallbackOutcome(errors.joinToString(" "))
        }

        val resolvedHash = manifestHash.ifBlank { expectedChecksum ?: remoteManifest.checksumSha256 }
        if (persistIfValid && sessionStore != null) {
            val serialized = json.encodeToString(DesignManifest.serializer(), remoteManifest)
            sessionStore.saveLastKnownGoodManifest(
                manifestJson = serialized,
                activeDesign = activeDesign,
                versionTag = remoteManifest.designVersion,
                checksum = expectedChecksum ?: remoteManifest.checksumSha256,
                etag = etag,
                assetBaseUrl = assetBaseUrl,
                manifestHash = resolvedHash
            )
        }

        val hasSectionWarnings = parseWarnings.isNotEmpty()
        val warningSummary = parseWarnings.takeIf { it.isNotEmpty() }?.joinToString(" ")
        if (hasSectionWarnings) {
            logWarning(
                "Manifest ${remoteManifest.designVersion} adopted from ${sourceHint.wireName} with section fallbacks: $warningSummary"
            )
        }

        return ManifestResolutionOutcome(
            manifest = remoteManifest,
            activeDesign = if (activeDesign == 1) 1 else 0,
            slot = if (activeDesign == 1) "live" else "default",
            usedFallback = hasSectionWarnings,
            fallbackReason = warningSummary,
            assetBaseUrl = assetBaseUrl,
            manifestHash = resolvedHash,
            etag = etag,
            source = sourceHint,
            warnings = parseWarnings
        )
    }

    fun validateAndAdopt(
        rawManifestJson: String,
        activeDesign: Int,
        expectedChecksum: String? = null,
        persistIfValid: Boolean = true,
        etag: String = "",
        assetBaseUrl: String = "",
        manifestHash: String = "",
        sourceHint: ManifestSource = ManifestSource.REMOTE
    ): ManifestResolutionOutcome {
        val rawObj = runCatching {
            json.decodeFromString<JsonObject>(rawManifestJson)
        }.getOrElse {
            return fallbackOutcome("Corrupt manifest JSON: ${it.message}")
        }
        return validateAndAdoptObject(
            rawJsonObject = rawObj,
            activeDesign = activeDesign,
            expectedChecksum = expectedChecksum,
            persistIfValid = persistIfValid,
            etag = etag,
            assetBaseUrl = assetBaseUrl,
            manifestHash = manifestHash,
            sourceHint = sourceHint
        )
    }

    /**
     * Tolerant entry point for raw [JsonObject] manifest payloads.
     * Checks schema version and SHA-256 first, then parses every section independently so
     * one bad field keeps defaults for that section only while adopting every valid section.
     */
    fun validateAndAdoptObject(
        rawJsonObject: JsonObject,
        activeDesign: Int,
        expectedChecksum: String? = null,
        persistIfValid: Boolean = true,
        etag: String = "",
        assetBaseUrl: String = "",
        manifestHash: String = "",
        sourceHint: ManifestSource = ManifestSource.REMOTE
    ): ManifestResolutionOutcome {
        val minSchema = rawJsonObject["min_app_schema_version"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() } ?: "1.0.0"
        if (!DesignManifestSupport.isSchemaCompatible(minSchema, appSchemaVersion)) {
            return fallbackOutcome(
                "Manifest min_app_schema_version $minSchema exceeds app schema $appSchemaVersion."
            )
        }

        if (!expectedChecksum.isNullOrBlank()) {
            val computed = DesignManifestSupport.sha256Canonical(rawJsonObject)
            if (!computed.equals(expectedChecksum.trim(), ignoreCase = true)) {
                return fallbackOutcome("Manifest SHA-256 checksum mismatch (expected=$expectedChecksum actual=$computed).")
            }
        }

        val tolerant = parseTolerantly(rawJsonObject, loadBundledDefaultManifest())
        return validateAndAdopt(
            remoteManifest = tolerant.manifest,
            activeDesign = activeDesign,
            expectedChecksum = null, // already verified on rawJsonObject above
            rawJsonObject = rawJsonObject,
            persistIfValid = persistIfValid,
            etag = etag,
            assetBaseUrl = assetBaseUrl,
            manifestHash = manifestHash,
            sourceHint = sourceHint,
            parseWarnings = tolerant.warnings
        )
    }

    fun validateManifestObject(manifest: DesignManifest): List<String> {
        val errors = mutableListOf<String>()
        if (manifest.manifestId.isBlank()) errors += "manifest_id is required."
        if (manifest.slot !in listOf("default", "live")) errors += "Invalid slot '${manifest.slot}'."

        val c = manifest.tokens.colors
        val hexChecks = listOf(
            "background" to c.background,
            "surface" to c.surface,
            "primary" to c.primary,
            "accent" to c.accent,
            "text_primary" to c.textPrimary,
            "border" to c.border
        )
        hexChecks.forEach { (field, hex) ->
            if (!DesignManifestSupport.isHexColorValid(hex)) {
                errors += "Invalid hex for $field: $hex."
            }
        }
        if (manifest.navigation.tabs.count { it.visible } < 3) {
            errors += "Navigation must contain at least 3 visible tabs."
        }
        if (!manifest.pages.containsKey("home")) {
            errors += "pages.home configuration is required."
        }
        return errors
    }

    private fun fallbackOutcome(reason: String): ManifestResolutionOutcome {
        logWarning("Design manifest fallback triggered: $reason")
        val lastGoodRaw = sessionStore?.getLastKnownGoodManifestJson()
        if (!lastGoodRaw.isNullOrBlank()) {
            val lastGoodObj = runCatching { json.decodeFromString<JsonObject>(lastGoodRaw) }.getOrNull()
            val lastGood = lastGoodObj?.let { parseTolerantly(it, emergencyDefaultManifest()).manifest }
            if (lastGood != null && validateManifestObject(lastGood).isEmpty() &&
                DesignManifestSupport.isSchemaCompatible(lastGood.minAppSchemaVersion, appSchemaVersion)
            ) {
                val flag = sessionStore.getLastKnownGoodActiveDesign()
                return ManifestResolutionOutcome(
                    manifest = lastGood,
                    activeDesign = flag,
                    slot = if (flag == 1) "live" else "default",
                    usedFallback = true,
                    fallbackReason = reason,
                    assetBaseUrl = sessionStore.getLastKnownGoodAssetBaseUrl(),
                    manifestHash = sessionStore.getLastKnownGoodManifestHash(),
                    etag = sessionStore.getLastKnownGoodEtag(),
                    source = ManifestSource.CACHED,
                    warnings = listOf(reason)
                )
            }
        }
        val bundled = loadBundledDefaultManifest()
        return ManifestResolutionOutcome(
            manifest = bundled,
            activeDesign = 0,
            slot = "default",
            usedFallback = true,
            fallbackReason = reason,
            manifestHash = bundled.checksumSha256,
            source = ManifestSource.BUNDLED,
            warnings = listOf(reason)
        )
    }

    private fun logWarning(message: String) {
        runCatching { Log.w(TAG, message) }
    }

    companion object {
        private const val TAG = "MelvoreDesignManifest"
        private const val BUNDLED_DEFAULT_ASSET = "design_system_default.json"

        fun emergencyDefaultManifest(): DesignManifest = DesignManifest(
            manifestId = "melvore_emergency_default",
            slot = "default",
            designVersion = "1.0.0",
            schemaVersion = "1.0.0",
            minAppSchemaVersion = "1.0.0",
            navigation = NavigationDesignSpec(),
            pages = mapOf(
                "home" to PageLayoutConfig(
                    pageKey = "home",
                    layoutTemplate = "editorial_magazine",
                    heroHeightDp = 420,
                    gridColumns = 2,
                    sections = listOf(
                        PageSectionConfig("search_bar", "search_bar", true, 1, "pill_elevated"),
                        PageSectionConfig("category_strip", "category_strip", true, 2, "circular_story", "DEPARTMENTS", "Shop by Category", "", "View All", 4),
                        PageSectionConfig("occasion_banner", "occasion_banner", true, 3, "festive_masthead", "SEASONAL CELEBRATION", "", "", "Shop Edit", 1),
                        PageSectionConfig("hero_carousel", "hero_carousel", true, 4, "full_bleed_editorial", "THE COVER STORY", "Featured Campaigns", "", "Explore", 1),
                        PageSectionConfig("style_worlds", "style_worlds", true, 5, "editorial_cards", "AESTHETIC MOVEMENTS", "Enter a Fashion World", "", "Explore Worlds", 2),
                        PageSectionConfig("trending_rail", "trending_rail", true, 6, "horizontal_cards", "MOST WANTED", "Trending Now", "", "See All", 2),
                        PageSectionConfig("world_atelier_map", "world_atelier_map", true, 7, "interactive_pins", "GLOBAL ATELIERS", "Origin Houses", "", "All Houses", 1),
                        PageSectionConfig("elite_teaser", "elite_teaser", true, 8, "obsidian_callout", "MELVORE LUXE", "Private Archive Packs", "", "Enter Luxe", 1),
                        PageSectionConfig("curated_grid", "curated_grid", true, 9, "editorial_2col", "CURATED DROPS", "Selected For You", "", "Shop All", 2)
                    )
                ),
                "style_worlds" to PageLayoutConfig(
                    pageKey = "style_worlds",
                    layoutTemplate = "world_editorial_story",
                    heroHeightDp = 440,
                    gridColumns = 2,
                    sections = listOf(
                        PageSectionConfig("world_hero", "world_hero", true, 1, "full_bleed_poster"),
                        PageSectionConfig("world_promo_film", "world_promo_film", true, 2, "inline_lookbook_reel", "RUNWAY FILM", "In Motion"),
                        PageSectionConfig("vibe_satisfaction", "vibe_satisfaction", true, 3, "emoji_slider", "ONE-TIME REWARD", "LIKING THE VIBE?", "Tell us how much you're feeling this world.", "Unlock Reward"),
                        PageSectionConfig("world_subcultures", "world_subcultures", true, 4, "pills_filter", "SILHOUETTES", "World Edits"),
                        PageSectionConfig("world_products", "world_products", true, 5, "editorial_2col", "CURATED PIECES", "Shop the World"),
                        PageSectionConfig("world_brands", "world_brands", true, 6, "house_cards", "PARTICIPATING HOUSES", "Houses in This World")
                    )
                )
            )
        )
    }
}
