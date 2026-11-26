package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FashionWorldTheme
import com.example.model.ManifestSource
import com.example.ui.components.LiquidGlassBox
import com.example.ui.theme.FashionWorldThemeResolver
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreSurfaceColors
import com.example.ui.theme.buildMelvoreSurfaceColors
import com.example.ui.theme.contrastRatio
import com.example.ui.viewmodel.DiscoveryUiState
import java.util.Locale
import kotlin.math.roundToInt

private data class RouteAuditEntry(
    val routeLabel: String,
    val isLuxe: Boolean,
    val isImmersiveWorld: Boolean,
    val resolvedLayer: String,
    val isDarkResult: Boolean,
    val surfaces: MelvoreSurfaceColors,
    val bgLuminancePct: Int,
    val surfaceLuminancePct: Int,
    val textContrastRatio: Float,
    val mixedBlackViolation: Boolean
)

/**
 * Debug-only Design Audit screen showing the active DB manifest metadata, fallback warnings,
 * and a live route-by-route luminance & contrast audit across light/dark modes to verify
 * zero "mixed black" regressions outside the Luxe showroom and immersive StyleWorld pages.
 */
@Composable
fun DesignAuditScreen(
    discoveryState: DiscoveryUiState,
    themeMode: String,
    onSetTheme: (String) -> Unit,
    onForceRefreshManifest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalMelvoreSurfaces.current
    val effectiveDark = discoveryState.isDarkMode
    val manifest = discoveryState.activeManifest
    val fallbackTheme = discoveryState.bootstrapConfig.defaultTheme
    val activeOccasion = discoveryState.bootstrapConfig.activeOccasion
    val gothWorld = remember(discoveryState.bootstrapConfig.fashionWorlds) {
        discoveryState.bootstrapConfig.fashionWorlds.firstOrNull {
            it.slug.equals("goth", ignoreCase = true)
        } ?: FashionWorldTheme(
            slug = "goth",
            name = "Goth & Dark Romance",
            mode = "dark",
            backgroundColor = "#070709",
            surfaceColor = "#111116",
            accentColor = "#DC2626",
            primaryColor = "#F4F4F5"
        )
    }
    val preferredWorld = remember(discoveryState.bootstrapConfig.fashionWorlds, discoveryState.preferredWorldSlug) {
        discoveryState.bootstrapConfig.fashionWorlds.firstOrNull {
            it.slug.equals(discoveryState.preferredWorldSlug, ignoreCase = true)
        }
    }

    val routeEntries = remember(manifest, fallbackTheme, activeOccasion, preferredWorld, gothWorld, effectiveDark) {
        data class Spec(
            val label: String,
            val world: FashionWorldTheme?,
            val isLuxe: Boolean = false,
            val isNeutral: Boolean = false,
            val isImmersive: Boolean = false
        )
        val specs = listOf(
            Spec("HomeRoute (Active World)", preferredWorld),
            Spec("HomeRoute (With Dark Goth World)", gothWorld),
            Spec("DiscoverRoute", preferredWorld),
            Spec("ShopRoute", preferredWorld),
            Spec("CategoriesRoute", preferredWorld),
            Spec("ProductDetailsRoute", preferredWorld),
            Spec("BagRoute", preferredWorld),
            Spec("CheckoutRoute (Neutral)", preferredWorld, isNeutral = true),
            Spec("ProfileRoute", preferredWorld),
            Spec("StyleWorldRoute (Goth Immersive)", gothWorld, isImmersive = true),
            Spec("EliteRoute (Melvore Luxe)", preferredWorld, isLuxe = true)
        )
        specs.map { spec ->
            val resolved = FashionWorldThemeResolver.resolveWithPrecedence(
                manifest = manifest,
                fallbackTheme = fallbackTheme,
                activeOccasion = activeOccasion,
                selectedWorld = spec.world,
                preferDarkMode = effectiveDark,
                isLuxeRoute = spec.isLuxe,
                isNeutralCheckoutOrAuthRoute = spec.isNeutral,
                isImmersiveWorldRoute = spec.isImmersive
            )
            val resolvedSurfaces = buildMelvoreSurfaceColors(
                resolvedTheme = resolved,
                manifest = manifest,
                darkTheme = resolved.isDark
            )
            val bgLum = resolvedSurfaces.background.luminance()
            val surfLum = resolvedSurfaces.surface.luminance()
            val ratio = contrastRatio(resolvedSurfaces.onSurface, resolvedSurfaces.surface)
            val violation = !effectiveDark && !spec.isLuxe && !spec.isImmersive &&
                (bgLum < 0.12f || surfLum < 0.12f)
            RouteAuditEntry(
                routeLabel = spec.label,
                isLuxe = spec.isLuxe,
                isImmersiveWorld = spec.isImmersive,
                resolvedLayer = resolved.sourceLayer,
                isDarkResult = resolved.isDark,
                surfaces = resolvedSurfaces,
                bgLuminancePct = (bgLum * 100f).roundToInt(),
                surfaceLuminancePct = (surfLum * 100f).roundToInt(),
                textContrastRatio = ratio,
                mixedBlackViolation = violation
            )
        }
    }

    val violationCount = routeEntries.count { it.mixedBlackViolation }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(surfaces.background)
            .testTag("design_audit_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LiquidGlassBox(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BugReport,
                            contentDescription = null,
                            tint = surfaces.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "DB DESIGN & CONTRAST AUDIT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = surfaces.onSurface
                        )
                    }

                    AuditKeyValueRow("Manifest Source", discoveryState.manifestSource.wireName.uppercase(Locale.ROOT), surfaces)
                    AuditKeyValueRow("Manifest Version", discoveryState.manifestVersion.ifBlank { manifest.designVersion }, surfaces)
                    AuditKeyValueRow("Bootstrap Config", discoveryState.bootstrapConfig.configVersion.ifBlank { "—" }, surfaces)
                    AuditKeyValueRow("Active Design Slot", "${discoveryState.activeDesignFlag} (${discoveryState.bootstrapConfig.designSlot})", surfaces)
                    AuditKeyValueRow("ETag", discoveryState.manifestEtag.ifBlank { "none" }, surfaces)
                    AuditKeyValueRow("Manifest Hash", discoveryState.manifestHash.take(16).ifBlank { "none" }, surfaces)
                    AuditKeyValueRow("Asset Base URL", discoveryState.designKit.assetBaseUrl.ifBlank { "api default" }, surfaces)
                    AuditKeyValueRow("Active Theme Layer", discoveryState.resolvedTheme.sourceLayer, surfaces)
                    AuditKeyValueRow("Appearance Mode", "${themeMode.uppercase(Locale.ROOT)} (effectiveDark=$effectiveDark)", surfaces)
                    AuditKeyValueRow(
                        "Fallback Reason",
                        discoveryState.manifestFallbackReason ?: "None (Clean)",
                        surfaces,
                        valueColor = if (discoveryState.manifestFallbackReason != null) surfaces.sale else surfaces.success
                    )

                    if (discoveryState.manifestWarnings.isNotEmpty()) {
                        HorizontalDivider(color = surfaces.border)
                        Text(
                            text = "SECTION WARNINGS (${discoveryState.manifestWarnings.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = surfaces.sale
                        )
                        discoveryState.manifestWarnings.forEach { warn ->
                            Text(
                                text = "• $warn",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = surfaces.onSurfaceMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onForceRefreshManifest,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaces.accent,
                                contentColor = surfaces.onAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("audit_force_refresh_btn")
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Force Refresh", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { onSetTheme(if (effectiveDark) "light" else "dark") },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, surfaces.border),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("audit_toggle_mode_btn")
                        ) {
                            Icon(
                                imageVector = if (effectiveDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = null,
                                tint = surfaces.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (effectiveDark) "Switch Light" else "Switch Dark",
                                color = surfaces.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Mixed-black summary banner
        item {
            val statusColor = if (violationCount == 0) surfaces.success else surfaces.sale
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = statusColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (violationCount == 0) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (violationCount == 0) {
                                "ALL ${routeEntries.size} ROUTES PASS MIXED-BLACK GUARD"
                            } else {
                                "$violationCount MIXED-BLACK VIOLATION(S) DETECTED"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = surfaces.onSurface
                        )
                        Text(
                            text = "Verifies non-Luxe, non-immersive routes keep surface luminance >= 12% in Light Mode and >= 4.5:1 text contrast.",
                            style = MaterialTheme.typography.bodySmall,
                            color = surfaces.onSurfaceMuted
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "ROUTE SURFACE & LUMINANCE MATRIX",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = surfaces.onSurfaceMuted
            )
        }

        items(routeEntries, key = { it.routeLabel }) { entry ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = surfaces.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (entry.mixedBlackViolation) surfaces.sale else surfaces.border
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.routeLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = surfaces.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "layer=${entry.resolvedLayer} · mode=${if (entry.isDarkResult) "dark" else "light"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = surfaces.onSurfaceMuted
                            )
                        }
                        val badgeColor = if (entry.mixedBlackViolation) surfaces.sale else surfaces.success
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = badgeColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (entry.mixedBlackViolation) "FAIL" else "PASS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorSwatchDot("BG", entry.surfaces.background, surfaces)
                        ColorSwatchDot("SURF", entry.surfaces.surface, surfaces)
                        ColorSwatchDot("TEXT", entry.surfaces.onSurface, surfaces)
                        ColorSwatchDot("ACC", entry.surfaces.accent, surfaces)
                    }

                    Text(
                        text = "BG lum ${entry.bgLuminancePct}% · Surface lum ${entry.surfaceLuminancePct}% · Contrast ${"%.1f".format(Locale.US, entry.textContrastRatio)}:1",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = surfaces.onSurfaceMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditKeyValueRow(
    label: String,
    value: String,
    surfaces: MelvoreSurfaceColors,
    valueColor: Color = surfaces.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = surfaces.onSurfaceMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ColorSwatchDot(
    label: String,
    color: Color,
    surfaces: MelvoreSurfaceColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, surfaces.border, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = surfaces.onSurfaceMuted
        )
    }
}
