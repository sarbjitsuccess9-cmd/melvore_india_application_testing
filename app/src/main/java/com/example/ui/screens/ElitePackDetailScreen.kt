package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Product
import com.example.ui.components.GoldShimmerEyebrow
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductEditorialSpotlightCard
import com.example.ui.components.SlowEditorialZoomBox
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreGothicFontFamily
import com.example.ui.theme.MelvoreSerifFontFamily
import com.example.ui.viewmodel.DiscoveryUiState
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun ElitePackDetailScreen(
    packId: Int,
    slug: String,
    uiState: MelvoreUiState,
    discoveryState: DiscoveryUiState,
    onLoadPack: (Int?, String?) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onOpenBrand: (Int, String) -> Unit,
    onOpenRelatedPack: (Int, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current

    LaunchedEffect(packId, slug) {
        onLoadPack(packId.takeIf { it > 0 }, slug.takeIf { it.isNotBlank() })
    }

    if (discoveryState.isElitePackLoading && discoveryState.elitePackDetail == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(surfaces.background),
            contentAlignment = Alignment.Center
        ) {
            MelvoreLoadingState("Opening Luxe collection…")
        }
        return
    }

    val bundle = discoveryState.elitePackDetail
    if (bundle == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(surfaces.background)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            MelvoreErrorState(
                message = "Unable to load this Luxe collection.",
                onRetry = { onLoadPack(packId.takeIf { it > 0 }, slug.takeIf { it.isNotBlank() }) }
            )
        }
        return
    }

    val pack = bundle.pack
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(surfaces.background)
            .testTag("elite_pack_detail_screen"),
        contentPadding = PaddingValues(bottom = 42.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Full-Bleed Collection Hero
        item(key = "pack_hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(352.dp)
            ) {
                SlowEditorialZoomBox(
                    triggerKey = "${pack.packId}_${pack.slug}",
                    modifier = Modifier.fillMaxSize()
                ) {
                    MelvoreNetworkImage(
                        url = pack.mobileImageUrl ?: pack.heroImageUrl,
                        contentDescription = pack.title,
                        fallbackDrawable = R.drawable.img_hero_atelier,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to surfaces.imageOverlay.copy(alpha = 0.20f),
                                    0.45f to Color.Transparent,
                                    1f to surfaces.background
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoldShimmerEyebrow(
                        text = pack.brandName.ifBlank { pack.eyebrow }.ifBlank { "MELVORE LUXE EDITION" }.uppercase(),
                        baseColor = ext.goldAccent,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = pack.title,
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = MelvoreGothicFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = surfaces.onImageOverlay
                    )
                    if (pack.subtitle.isNotBlank()) {
                        Text(
                            text = pack.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = surfaces.onImageOverlay.copy(alpha = 0.86f)
                        )
                    }
                }
            }
        }

        // 2. Editorial Story / Curatorial Note
        if (pack.story.isNotBlank()) {
            item(key = "pack_story") {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = surfaces.surface,
                    border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "CURATORIAL NOTE",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.8.sp,
                            color = ext.goldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = pack.story,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = MelvoreSerifFontFamily,
                            color = surfaces.onSurface.copy(alpha = 0.90f)
                        )
                    }
                }
            }
        }

        // 3. Featured Luxury Houses
        if (bundle.brands.isNotEmpty()) {
            item(key = "pack_brands") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "FEATURED HOUSES",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.8.sp,
                        color = surfaces.onSurfaceMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bundle.brands, key = { "epb_${it.brandId}" }) { br ->
                            Surface(
                                onClick = { onOpenBrand(br.brandId, br.slug) },
                                shape = RoundedCornerShape(3.dp),
                                color = surfaces.surface,
                                border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                                ) {
                                    if (!br.logoUrl.isNullOrBlank()) {
                                        MelvoreNetworkImage(
                                            url = br.logoUrl,
                                            contentDescription = br.name,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Text(br.name, style = MaterialTheme.typography.labelLarge, color = surfaces.onSurface, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Collector Pieces Grid
        if (bundle.products.isEmpty()) {
            item(key = "pack_empty_products") {
                Text(
                    text = "This collection currently highlights featured partner houses above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.muted,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            item(key = "pack_products_header") {
                Text(
                    text = "COLLECTION PIECES (${bundle.products.size})",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.8.sp,
                    color = ext.goldAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }

            val featuredProd = pack.featuredProductId?.let { fId ->
                bundle.products.find { it.id == fId }
            } ?: if (pack.layoutType == "designer_spotlight") bundle.products.firstOrNull() else null
            val remainingProducts = if (featuredProd != null && bundle.products.size > 2) {
                bundle.products.filter { it.id != featuredProd.id }
            } else {
                bundle.products
            }

            if (featuredProd != null) {
                item(key = "pack_featured_spotlight_${featuredProd.id}") {
                    ProductEditorialSpotlightCard(
                        eyebrow = pack.brandName.ifBlank { pack.eyebrow }.ifBlank { "COLLECTOR SPOTLIGHT" },
                        product = featuredProd,
                        currency = uiState.currency,
                        isWishlisted = uiState.wishlistIds.contains(featuredProd.id),
                        onOpenProduct = { onOpenProduct(featuredProd) },
                        onWishlistClick = { onToggleWishlist(featuredProd.id) },
                        onQuickAdd = { onQuickAdd(featuredProd) },
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }

            val rows = remainingProducts.chunked(2)
            items(rows.size, key = { "pack_prod_row_$it" }) { idx ->
                val pair = rows[idx]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { prod ->
                        ProductCard(
                            product = prod,
                            currency = uiState.currency,
                            isWishlisted = uiState.wishlistIds.contains(prod.id),
                            onProductClick = { onOpenProduct(prod) },
                            onWishlistClick = { onToggleWishlist(prod.id) },
                            onAddToCartClick = { onQuickAdd(prod) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // 5. Related Collections Rail
        if (bundle.relatedPacks.isNotEmpty()) {
            item(key = "pack_related_collections") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "MORE LUXE COLLECTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.8.sp,
                        color = ext.goldAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bundle.relatedPacks, key = { "rel_pack_${it.packId}_${it.slug}" }) { rel ->
                            Surface(
                                onClick = { onOpenRelatedPack(rel.packId, rel.slug) },
                                shape = RoundedCornerShape(4.dp),
                                color = surfaces.surface,
                                border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .width(228.dp)
                                    .testTag("related_pack_${rel.slug}")
                            ) {
                                Box(modifier = Modifier.height(148.dp)) {
                                    MelvoreNetworkImage(
                                        url = rel.mobileImageUrl ?: rel.heroImageUrl,
                                        contentDescription = rel.title,
                                        fallbackDrawable = R.drawable.img_villain_jacket,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.88f))))
                                    )
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (rel.eyebrow.isNotBlank()) {
                                            Text(
                                                text = rel.eyebrow.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = ext.goldAccent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = rel.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = MelvoreSerifFontFamily,
                                            color = surfaces.onImageOverlay,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
