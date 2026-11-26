package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.DesignManifest
import com.example.model.NavigationTabSpec
import com.example.model.OccasionKitSpec
import com.example.model.PageSectionConfig
import com.example.model.WorldKitSpec
import com.example.ui.theme.LocalFashionWorldTheme
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.ResolvedFashionWorldTheme
import com.example.ui.theme.fontFamilyForStyleKey
import com.example.ui.theme.parseHexColor
import java.util.Locale

data class EffectiveNavTab(
    val route: String,
    val label: String,
    val iconKey: String,
    val activeIconKey: String,
    val badgeText: String = "",
    val iconSvgUrl: String? = null,
    val activeIconSvgUrl: String? = null
)

/**
 * Curated Native Compose Component & Icon Registry.
 * Strictly maps server-driven manifest keys onto compiled Jetpack Compose primitives
 * and Material vector icons — never executes remote code or unverified URLs as code.
 */
object RemoteDesignRegistry {

    private val DEFAULT_HOME_ORDER = listOf(
        "search_bar",
        "category_strip",
        "occasion_banner",
        "hero_carousel",
        "style_worlds",
        "trending_rail",
        "world_atelier_map",
        "elite_teaser",
        "curated_grid"
    )

    private val DEFAULT_STYLE_WORLD_ORDER = listOf(
        "world_hero",
        "world_promo_film",
        "vibe_satisfaction",
        "world_subcultures",
        "world_products",
        "world_brands"
    )

    fun iconForKey(key: String, filled: Boolean = false): ImageVector {
        val normalized = key.trim().lowercase(Locale.ROOT)
        val wantFilled = filled || normalized.endsWith("_filled")
        val base = normalized.removeSuffix("_filled")
        return when (base) {
            "home" -> if (wantFilled) Icons.Filled.Home else Icons.Outlined.Home
            "explore" -> if (wantFilled) Icons.Filled.Explore else Icons.Outlined.Explore
            "compass" -> if (wantFilled) Icons.Filled.TravelExplore else Icons.Outlined.TravelExplore
            "sparkle" -> if (wantFilled) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
            "play" -> if (wantFilled) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle
            "reel" -> if (wantFilled) Icons.Filled.Movie else Icons.Outlined.Movie
            "diamond" -> if (wantFilled) Icons.Filled.Diamond else Icons.Outlined.Diamond
            "crown" -> if (wantFilled) Icons.Filled.WorkspacePremium else Icons.Outlined.WorkspacePremium
            "bag" -> if (wantFilled) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag
            "tote" -> if (wantFilled) Icons.Filled.LocalMall else Icons.Outlined.LocalMall
            "search" -> Icons.Outlined.Search
            "heart" -> if (wantFilled) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
            else -> if (wantFilled) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
        }
    }

    fun resolveNavTabs(manifest: DesignManifest): List<NavigationTabSpec> {
        val visible = manifest.navigation.tabs.filter { it.visible }.sortedBy { it.order }
        return if (visible.size >= 3) visible else NavigationTabSpecFallback()
    }

    private fun NavigationTabSpecFallback(): List<NavigationTabSpec> =
        com.example.model.NavigationDesignSpec.defaultTabs()

    fun resolveNavTab(manifest: DesignManifest, route: String): NavigationTabSpec {
        return resolveNavTabs(manifest).firstOrNull { it.route.equals(route, ignoreCase = true) }
            ?: NavigationTabSpecFallback().firstOrNull { it.route.equals(route, ignoreCase = true) }
            ?: NavigationTabSpec(route = route, label = route.replaceFirstChar { it.uppercase() })
    }

    fun resolveOrderedHomeSectionIds(manifest: DesignManifest): List<String> {
        val page = manifest.pageConfig("home") ?: return DEFAULT_HOME_ORDER
        val ordered = page.orderedVisibleSections().map { it.id.ifBlank { it.type } }.filter { it.isNotBlank() }
        return if (ordered.isNotEmpty()) ordered else DEFAULT_HOME_ORDER
    }

    fun resolveHomeSectionConfig(manifest: DesignManifest, sectionId: String): PageSectionConfig? {
        return manifest.pageConfig("home")?.sections?.firstOrNull {
            it.id.equals(sectionId, ignoreCase = true) || it.type.equals(sectionId, ignoreCase = true)
        }
    }

    fun isHomeSectionVisible(manifest: DesignManifest, sectionId: String): Boolean {
        val cfg = resolveHomeSectionConfig(manifest, sectionId) ?: return true
        return cfg.visible
    }

    fun resolveStyleWorldSectionOrder(manifest: DesignManifest, worldSlug: String): List<String> {
        val worldSpecific = manifest.worldOverride(worldSlug)?.sectionOrder?.filter { it.isNotBlank() }.orEmpty()
        if (worldSpecific.isNotEmpty()) return worldSpecific
        val pageOrdered = manifest.pageConfig("style_worlds")
            ?.orderedVisibleSections()
            ?.map { it.id.ifBlank { it.type } }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        return if (pageOrdered.isNotEmpty()) pageOrdered else DEFAULT_STYLE_WORLD_ORDER
    }

    fun resolveAssetUrl(
        pathOrUrl: String?,
        apiBaseUrl: String = BuildConfig.MELVORE_API_BASE
    ): String? {
        val raw = pathOrUrl?.trim().orEmpty()
        if (raw.isEmpty()) return null
        if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
            return raw
        }
        if (raw.startsWith("//")) {
            return "https:$raw"
        }
        val cleanBase = apiBaseUrl.trim().trimEnd('/').ifBlank {
            BuildConfig.MELVORE_API_BASE.trimEnd('/')
        }
        return if (raw.startsWith("/")) "$cleanBase$raw" else "$cleanBase/$raw"
    }

    fun resolveWorldSpriteUrl(
        assetKey: String,
        apiBaseUrl: String = BuildConfig.MELVORE_API_BASE
    ): String? {
        val clean = assetKey.trim()
        if (clean.isEmpty()) return null
        return resolveAssetUrl("/design-system/assets/svg/sprites/$clean.svg", apiBaseUrl)
    }

    fun activeWorldKit(
        manifest: DesignManifest,
        worldTheme: ResolvedFashionWorldTheme
    ): WorldKitSpec? {
        if (!worldTheme.isWorldActive || worldTheme.isLuxeIsolated) return null
        val slug = worldTheme.worldSlug?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (slug.isBlank()) return null
        return manifest.worldOverride(slug)?.kit
    }

    fun activeOccasionKit(
        manifest: DesignManifest,
        occasionSlug: String?
    ): OccasionKitSpec? = manifest.occasionOverrides.occasionKit(occasionSlug)

    fun resolveEffectiveNavTabs(
        manifest: DesignManifest,
        worldTheme: ResolvedFashionWorldTheme,
        apiBaseUrl: String = BuildConfig.MELVORE_API_BASE
    ): List<EffectiveNavTab> {
        val baseTabs = resolveNavTabs(manifest)
        val kit = activeWorldKit(manifest, worldTheme)
        return baseTabs.mapIndexed { idx, tab ->
            val routeKey = tab.route.lowercase(Locale.ROOT)
            val customLabel = kit?.tabLabels?.getOrNull(idx)?.takeIf { it.isNotBlank() }
            val v2IconPair = kit?.v2?.icons?.get(routeKey)
            val legacyIconPair = kit?.icons?.get(routeKey)
            val iconPath = v2IconPair?.icon?.takeIf { it.isNotBlank() } ?: legacyIconPair?.icon
            val activeIconPath = v2IconPair?.activeIcon?.takeIf { it.isNotBlank() }
                ?: legacyIconPair?.activeIcon
                ?: iconPath
            EffectiveNavTab(
                route = routeKey,
                label = customLabel ?: tab.label,
                iconKey = tab.iconKey,
                activeIconKey = tab.activeIconKey.ifBlank { tab.iconKey },
                badgeText = tab.badgeText,
                iconSvgUrl = resolveAssetUrl(iconPath, apiBaseUrl),
                activeIconSvgUrl = resolveAssetUrl(activeIconPath, apiBaseUrl)
            )
        }
    }

    fun resolveProductCardAspectRatio(manifest: DesignManifest): Float =
        manifest.components.productCard.imageAspectRatio.coerceIn(0.62f, 1.0f)

    fun resolveProductCardRadiusDp(manifest: DesignManifest, worldTheme: ResolvedFashionWorldTheme): Int =
        if (worldTheme.isWorldActive) {
            val override = manifest.worldOverride(worldTheme.worldSlug.orEmpty())
            (override?.kit?.components?.productCard?.cornerRadiusDp
                ?: override?.cardRadiusDp
                ?: worldTheme.cardRadiusDp).coerceIn(0, 28)
        } else {
            manifest.components.productCard.cornerRadiusDp.coerceIn(0, 28)
        }

    fun resolveButtonShape(
        manifest: DesignManifest,
        worldTheme: ResolvedFashionWorldTheme? = null
    ): Shape {
        val worldBtn = if (worldTheme != null) activeWorldKit(manifest, worldTheme)?.components?.button else null
        val btn = worldBtn ?: manifest.components.button
        val radius = btn.cornerRadiusDp.coerceIn(0, 999)
        return when (btn.primaryVariant.lowercase(Locale.ROOT)) {
            "pill_luxe" -> RoundedCornerShape(999.dp)
            "editorial_sharp" -> RoundedCornerShape(radius.coerceAtMost(8).dp)
            else -> RoundedCornerShape(radius.dp)
        }
    }
}

@Composable
fun RemoteSectionHeader(
    sectionId: String,
    fallbackEyebrow: String,
    fallbackTitle: String,
    fallbackActionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val manifest = LocalMelvoreDesignManifest.current
    val worldTheme = LocalFashionWorldTheme.current
    val ext = LocalMelvoreColors.current

    val sectionCfg = RemoteDesignRegistry.resolveHomeSectionConfig(manifest, sectionId)
    val headerSpec = manifest.components.sectionHeader
    val variant = headerSpec.variant.lowercase(Locale.ROOT)
    val eyebrow = sectionCfg?.eyebrow?.takeIf { it.isNotBlank() } ?: fallbackEyebrow
    val title = sectionCfg?.title?.takeIf { it.isNotBlank() } ?: fallbackTitle
    val actionLabel = sectionCfg?.ctaLabel?.takeIf { it.isNotBlank() } ?: fallbackActionLabel

    val displayFont = fontFamilyForStyleKey(
        if (worldTheme.isWorldActive) worldTheme.fontStyle else manifest.tokens.typography.displayFont
    )
    val eyebrowSpacing = manifest.tokens.typography.letterSpacingEyebrow.coerceIn(0.6f, 4f).sp
    val accent = parseHexColor(worldTheme.accentHex, ext.goldAccent)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (headerSpec.showDivider) {
            HorizontalDivider(
                thickness = 0.75.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (headerSpec.showEyebrow && eyebrow.isNotBlank()) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = if (variant == "tracked_mono") FontFamily.Monospace else MaterialTheme.typography.labelSmall.fontFamily,
                        letterSpacing = eyebrowSpacing,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = if (variant == "tracked_mono") MaterialTheme.typography.headlineSmall.fontFamily else displayFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (manifest.components.button.showTrailingArrow) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteEmptyState(
    title: String? = null,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val manifest = LocalMelvoreDesignManifest.current
    val worldTheme = LocalFashionWorldTheme.current
    val worldKit = RemoteDesignRegistry.activeWorldKit(manifest, worldTheme)
    val statesSpec = worldKit?.components?.states ?: manifest.components.states
    val ext = LocalMelvoreColors.current
    val cardRadius = RemoteDesignRegistry.resolveProductCardRadiusDp(manifest, worldTheme).dp
    val resolvedTitle = title?.takeIf { it.isNotBlank() } ?: statesSpec.emptyTitle
    val resolvedSubtitle = subtitle?.takeIf { it.isNotBlank() } ?: statesSpec.emptySubtitle

    Surface(
        shape = RoundedCornerShape(cardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = if (statesSpec.emptyVariant == "centered_minimal") null else BorderStroke(1.dp, ext.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("remote_empty_state")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = resolvedTitle,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = fontFamilyForStyleKey(
                    if (worldTheme.isWorldActive) worldTheme.fontStyle else manifest.tokens.typography.displayFont
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = resolvedSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted,
                textAlign = TextAlign.Center
            )
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = RemoteDesignRegistry.resolveButtonShape(manifest),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.heightIn(min = manifest.components.button.minHeightDp.coerceAtLeast(48).dp)
                ) {
                    Text(
                        text = if (manifest.components.button.uppercaseLabels) actionLabel.uppercase() else actionLabel,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
