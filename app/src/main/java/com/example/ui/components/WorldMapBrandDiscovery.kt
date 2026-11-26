package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.FashionWorldTheme
import com.example.model.WorldMapOriginPin
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import kotlin.math.hypot

/**
 * Interactive World-Map Brand Discovery section backed strictly by verified database brand origins.
 * Supports pan, pinch-zoom, + / - zoom controls, world filtering, pin tap selection, and brand navigation.
 */
@Composable
fun WorldMapDiscoverySection(
    origins: List<WorldMapOriginPin>,
    worlds: List<FashionWorldTheme>,
    onExploreBrand: (brandId: Int, slug: String) -> Unit,
    onShopBrand: (brandId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val verifiedOrigins = remember(origins) { origins.filter { it.isVerified } }
    var worldFilterSlug by remember { mutableStateOf<String?>(null) }
    val filteredPins = remember(verifiedOrigins, worldFilterSlug) {
        if (worldFilterSlug.isNullOrBlank()) verifiedOrigins
        else verifiedOrigins.filter { it.worldSlug.equals(worldFilterSlug, ignoreCase = true) }
    }

    var selectedOriginId by remember(filteredPins) {
        mutableStateOf(filteredPins.firstOrNull()?.originId ?: 0)
    }
    val selectedPin = remember(filteredPins, selectedOriginId) {
        filteredPins.find { it.originId == selectedOriginId } ?: filteredPins.firstOrNull()
    }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("world_map_discovery_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = stringResource(R.string.cd_world_map_icon),
                        tint = cs.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.world_map_eyebrow),
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.secondary,
                        letterSpacing = 1.4.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.world_map_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = cs.onBackground
                )
                Text(
                    text = stringResource(R.string.world_map_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // World filter pills
        if (worlds.isNotEmpty() && verifiedOrigins.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = worldFilterSlug == null,
                        onClick = { worldFilterSlug = null },
                        label = { Text(stringResource(R.string.world_map_all_origins)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.primary,
                            selectedLabelColor = cs.onPrimary
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("world_map_filter_all")
                    )
                }
                items(worlds, key = { it.slug }) { world ->
                    val count = verifiedOrigins.count { it.worldSlug.equals(world.slug, ignoreCase = true) }
                    if (count > 0) {
                        val isSelected = worldFilterSlug.equals(world.slug, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { worldFilterSlug = if (isSelected) null else world.slug },
                            label = { Text("${world.name} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cs.primary,
                                selectedLabelColor = cs.onPrimary
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("world_map_filter_${world.slug}")
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Interactive Pan/Zoom Map Canvas (mode-adaptive cartography — never a black patch in light mode)
        val mapOceanTop = surfaces.surfaceElevated
        val mapOceanMid = surfaces.surface
        val mapOceanBottom = surfaces.background
        val mapLandFill = if (surfaces.isDark) surfaces.border.copy(alpha = 0.65f) else surfaces.border.copy(alpha = 0.55f)
        val mapLandStroke = surfaces.onSurfaceMuted.copy(alpha = 0.45f)
        val mapGridColor = surfaces.onSurfaceMuted.copy(alpha = 0.14f)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(238.dp)
                .clip(RoundedCornerShape(16.dp))
                .testTag("world_map_canvas_card"),
            shape = RoundedCornerShape(16.dp),
            color = mapOceanTop,
            border = BorderStroke(1.dp, surfaces.border)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(filteredPins) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (zoomScale * zoom).coerceIn(1f, 3.2f)
                                val maxOffsetX = (size.width * (nextScale - 1f)) / 2f + 40f
                                val maxOffsetY = (size.height * (nextScale - 1f)) / 2f + 24f
                                val candidate = panOffset + pan
                                zoomScale = nextScale
                                panOffset = Offset(
                                    candidate.x.coerceIn(-maxOffsetX, maxOffsetX),
                                    candidate.y.coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            }
                        }
                        .pointerInput(filteredPins, zoomScale, panOffset) {
                            detectTapGestures(
                                onDoubleTap = {
                                    zoomScale = if (zoomScale > 1.3f) 1f else 2f
                                    if (zoomScale == 1f) panOffset = Offset.Zero
                                },
                                onTap = { tap ->
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    var closestPin: WorldMapOriginPin? = null
                                    var bestDist = 72f
                                    filteredPins.forEach { pin ->
                                        val base = projectLatLon(pin.latitude, pin.longitude, w, h)
                                        val transformed = Offset(
                                            (base.x - w / 2f) * zoomScale + w / 2f + panOffset.x,
                                            (base.y - h / 2f) * zoomScale + h / 2f + panOffset.y
                                        )
                                        val d = hypot(tap.x - transformed.x, tap.y - transformed.y)
                                        if (d < bestDist) {
                                            bestDist = d
                                            closestPin = pin
                                        }
                                    }
                                    closestPin?.let { selectedOriginId = it.originId }
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(mapOceanTop, mapOceanMid, mapOceanBottom)
                        )
                    )
                    withTransform({
                        translate(left = panOffset.x, top = panOffset.y)
                        scale(scaleX = zoomScale, scaleY = zoomScale, pivot = Offset(w / 2f, h / 2f))
                    }) {
                        drawWorldGridAndContinents(w, h, mapGridColor, mapLandFill, mapLandStroke)
                        filteredPins.forEach { pin ->
                            val pos = projectLatLon(pin.latitude, pin.longitude, w, h)
                            val isSelected = selectedPin?.originId == pin.originId
                            val pinColor = if (isSelected) ext.goldAccent else ext.electricBlue
                            drawCircle(
                                color = pinColor.copy(alpha = if (isSelected) 0.34f else 0.22f),
                                radius = (if (isSelected) 18f else 11f) / zoomScale.coerceAtLeast(1f),
                                center = pos
                            )
                            drawCircle(
                                color = pinColor,
                                radius = (if (isSelected) 8.5f else 5.5f) / zoomScale.coerceAtLeast(1f),
                                center = pos
                            )
                            drawCircle(
                                color = surfaces.surface,
                                radius = (if (isSelected) 3.5f else 2f) / zoomScale.coerceAtLeast(1f),
                                center = pos
                            )
                        }
                    }
                }

                // Map zoom & reset controls
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LiquidGlassBox(
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row {
                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.45f).coerceAtMost(3.2f) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("world_map_zoom_in")
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.cd_world_map_zoom_in),
                                    tint = surfaces.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    zoomScale = (zoomScale - 0.45f).coerceAtLeast(1f)
                                    if (zoomScale <= 1.05f) panOffset = Offset.Zero
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("world_map_zoom_out")
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = stringResource(R.string.cd_world_map_zoom_out),
                                    tint = surfaces.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("world_map_reset")
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.cd_world_map_reset),
                                    tint = surfaces.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Subtle hint pill at bottom-left
                LiquidGlassBox(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = stringResource(R.string.world_map_drag_hint, filteredPins.size),
                        color = surfaces.onSurface,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredPins.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                color = cs.surfaceVariant,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Text(
                    text = stringResource(R.string.world_map_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return
        }

        // Accessible origin city pills row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredPins, key = { it.originId }) { pin ->
                val isSelected = selectedPin?.originId == pin.originId
                Surface(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedOriginId = pin.originId }
                        .testTag("world_map_pin_${pin.brandSlug}"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) cs.primary else cs.surface,
                    border = BorderStroke(1.dp, if (isSelected) cs.primary else ext.borderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isSelected) cs.onPrimary else cs.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${pin.city}, ${pin.countryCode.ifBlank { pin.country }}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) cs.onPrimary else cs.onSurface
                        )
                    }
                }
            }
        }

        // Selected Brand Preview Card
        selectedPin?.let { pin ->
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("world_map_brand_preview_card"),
                shape = RoundedCornerShape(16.dp),
                color = cs.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(cs.surfaceVariant)
                                .border(1.dp, ext.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!pin.brandLogo.isNullOrBlank()) {
                                AsyncImage(
                                    model = pin.brandLogo,
                                    contentDescription = pin.brandName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = pin.brandName.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = cs.onSurface
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = pin.brandName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = cs.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = stringResource(R.string.cd_verified_brand_origin),
                                    tint = cs.secondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = "${pin.city}, ${pin.country} · ${pin.productCount} styles",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.muted
                            )
                        }
                        if (pin.worldSlug.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = cs.surfaceVariant
                            ) {
                                Text(
                                    text = pin.worldSlug.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.secondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (pin.tagline.isNotBlank() || pin.story.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = pin.tagline.ifBlank { pin.story },
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurface.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onShopBrand(pin.brandId) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("world_map_shop_brand_button")
                        ) {
                            Text(stringResource(R.string.world_map_shop_catalog))
                        }
                        Button(
                            onClick = { onExploreBrand(pin.brandId, pin.brandSlug) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cs.primary,
                                contentColor = cs.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("world_map_explore_atelier_button")
                        ) {
                            Text(stringResource(R.string.world_map_explore_atelier))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun projectLatLon(lat: Double, lon: Double, width: Float, height: Float): Offset {
    val x = (((lon + 180.0) / 360.0).coerceIn(0.03, 0.97) * width).toFloat()
    val y = (((90.0 - lat) / 180.0).coerceIn(0.08, 0.92) * height).toFloat()
    return Offset(x, y)
}

private fun DrawScope.drawWorldGridAndContinents(
    w: Float,
    h: Float,
    gridColor: Color,
    landFill: Color,
    landStroke: Color
) {
    for (i in 1..5) {
        val y = (h / 6f) * i
        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
    }
    for (i in 1..7) {
        val x = (w / 8f) * i
        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
    }

    val continents = listOf(
        // North America
        listOf(-165f to 66f, -135f to 58f, -125f to 48f, -118f to 33f, -104f to 20f, -84f to 10f, -81f to 26f, -74f to 41f, -55f to 49f, -66f to 60f, -95f to 68f, -140f to 70f),
        // South America
        listOf(-80f to 10f, -72f to 11f, -50f to 0f, -35f to -7f, -40f to -22f, -54f to -34f, -67f to -54f, -74f to -45f, -71f to -18f, -81f to -5f),
        // Europe
        listOf(-9f to 37f, -9f to 43f, 2f to 51f, 8f to 55f, 18f to 68f, 38f to 66f, 45f to 50f, 30f to 41f, 15f to 38f, 3f to 42f),
        // Africa
        listOf(-17f to 15f, -5f to 35f, 12f to 33f, 32f to 31f, 43f to 12f, 51f to 11f, 40f to -5f, 35f to -24f, 20f to -35f, 15f to -22f, 9f to 4f, -14f to 7f),
        // Asia
        listOf(34f to 42f, 48f to 40f, 60f to 25f, 68f to 23f, 77f to 8f, 86f to 21f, 98f to 16f, 104f to 2f, 114f to 22f, 122f to 31f, 128f to 38f, 141f to 43f, 142f to 54f, 165f to 64f, 100f to 72f, 60f to 68f, 45f to 52f),
        // Australia
        listOf(114f to -22f, 130f to -12f, 144f to -14f, 153f to -26f, 148f to -38f, 134f to -34f, 116f to -34f)
    )

    continents.forEach { pts ->
        val path = Path()
        pts.forEachIndexed { idx, (lon, lat) ->
            val p = projectLatLon(lat.toDouble(), lon.toDouble(), w, h)
            if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, color = landFill)
        drawPath(path, color = landStroke, style = Stroke(width = 1.2f))
    }
}
