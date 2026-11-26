package com.example.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.Product
import com.example.model.formatPrice
import com.example.model.parseHexColor
import com.example.ui.components.LiquidGlassBar
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.MelvoreWorldActionButton
import com.example.ui.components.MelvoreWorldButtonState
import com.example.ui.components.OfflineCacheBanner
import com.example.ui.components.ProductCard
import com.example.ui.theme.LocalFashionWorldTheme
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreSerifFontFamily
import com.example.ui.theme.ensureContrast
import com.example.ui.theme.fontFamilyForStyleKey
import com.example.ui.theme.parseHexColor as parseThemeHex
import com.example.ui.viewmodel.DiscoveryUiState
import com.example.ui.viewmodel.MelvoreUiState

private data class MediaSlide(val isVideo: Boolean, val url: String)

@OptIn(UnstableApi::class)
@Composable
fun ProductDetailsScreen(
    productId: Int,
    worldSlug: String = "",
    uiState: MelvoreUiState,
    discoveryState: DiscoveryUiState,
    onLoadDetails: (Int) -> Unit,
    onLoadRecommendations: (Int, String?) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onOpenBrand: (brandId: Int, slug: String) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onAddToCart: (Product, String, String, Int) -> Unit,
    onBuyNow: (Product, String, String, Int) -> Unit,
    onCheckDelivery: (pincode: String, productId: Int) -> Unit,
    onRequestReviewOtp: (orderItemId: Int) -> Unit,
    onVerifyReviewOtp: (otp: String) -> Unit,
    onUploadReviewPhoto: (Uri) -> Unit,
    onRemoveReviewPhoto: (String) -> Unit,
    onSubmitVerifiedReview: (productId: Int, orderItemId: Int, rating: Int, title: String, comment: String, onDone: () -> Unit) -> Unit,
    onToggleReviewHelpful: (reviewId: Int) -> Unit,
    onNavigateAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val worldTheme = LocalFashionWorldTheme.current
    val effectiveWorldSlug = worldSlug.ifBlank { worldTheme.worldSlug.orEmpty() }
    val worldAccent = parseThemeHex(worldTheme.accentHex, ext.goldAccent)
    val context = LocalContext.current

    LaunchedEffect(productId, effectiveWorldSlug) {
        onLoadDetails(productId)
        onLoadRecommendations(productId, effectiveWorldSlug.takeIf { it.isNotBlank() })
    }

    val product = uiState.selectedProduct?.takeIf { it.id == productId }
        ?: uiState.products.find { it.id == productId }
        ?: uiState.catalogProducts.find { it.id == productId }

    if (product == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isProductDetailsLoading -> MelvoreLoadingState("Loading product details…")
                uiState.productDetailsError != null -> MelvoreErrorState(
                    message = uiState.productDetailsError,
                    onRetry = { onLoadDetails(productId) }
                )
                else -> MelvoreErrorState(
                    message = "Product not found.",
                    onRetry = onBack
                )
            }
        }
        return
    }

    var selectedColor by remember(product.id, product.colors) {
        mutableStateOf(product.colors.firstOrNull()?.name ?: "")
    }
    var selectedSize by remember(product.id, product.sizes) {
        mutableStateOf(product.sizes.firstOrNull() ?: "")
    }
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    var pincodeInput by remember(discoveryState.savedPincode) { mutableStateOf(discoveryState.savedPincode) }
    var showWriteReviewSheet by remember { mutableStateOf(false) }
    var showFitAndCare by remember { mutableStateOf(true) }

    val selectedVariation = remember(product.variations, selectedColor, selectedSize) {
        product.variations.find { v ->
            v.isActive &&
                (selectedColor.isBlank() || v.color.equals(selectedColor, ignoreCase = true) || v.color.isBlank()) &&
                (selectedSize.isBlank() || v.size.equals(selectedSize, ignoreCase = true) || v.size.isBlank())
        }
    }

    val effectivePrice = selectedVariation?.salePrice ?: selectedVariation?.regularPrice ?: product.effectivePrice
    val effectiveRegular = selectedVariation?.regularPrice ?: product.regularPrice
    val effectiveInStock = selectedVariation?.let { it.stockQuantity > 0 } ?: product.inStock

    val mediaSlides = remember(product.id, product.imageUrl, product.galleryUrls, product.videoUrls, selectedVariation?.imageUrl) {
        val images = (listOfNotNull(selectedVariation?.imageUrl, product.imageUrl) + product.galleryUrls)
            .filter { it.isNotBlank() }
            .distinct()
            .map { MediaSlide(isVideo = false, url = it) }
        val videos = product.videoUrls
            .filter { it.isNotBlank() }
            .distinct()
            .map { MediaSlide(isVideo = true, url = it) }
        (images + videos).ifEmpty { listOf(MediaSlide(false, "")) }
    }
    val pagerState = rememberPagerState(pageCount = { mediaSlides.size })

    val activeSlide = mediaSlides.getOrNull(pagerState.settledPage)
    val exoPlayer = remember(product.id) {
        if (product.videoUrls.isEmpty()) null else ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }
    if (exoPlayer != null) {
        DisposableEffect(exoPlayer) {
            onDispose { exoPlayer.release() }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
            exoPlayer.pause()
        }
        LaunchedEffect(activeSlide) {
            if (activeSlide?.isVideo == true) {
                exoPlayer.setMediaItem(MediaItem.fromUri(activeSlide.url))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } else {
                exoPlayer.pause()
            }
        }
    }

    val isWishlisted = uiState.wishlistIds.contains(product.id)
    val reviews = remember(discoveryState.reviewList.reviews, product.reviews) {
        discoveryState.reviewList.reviews.ifEmpty { product.reviews }
    }
    val related = remember(product.id, product.relatedProducts, uiState.products) {
        product.relatedProducts.ifEmpty {
            uiState.products.filter { it.id != product.id }.take(6)
        }
    }
    val plainDescription = remember(product.description) {
        HtmlCompat.fromHtml(product.description, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toString()
            .trim()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("product_details_screen")
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .clickable(onClick = onBack)
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                    Text("Back", style = MaterialTheme.typography.labelLarge, color = ext.muted)
                }
                if (uiState.isProductDetailsLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (uiState.isShowingCachedData) {
                    OfflineCacheBanner(
                        cachedItemCount = uiState.cachedProductCount,
                        onRetry = { onLoadDetails(productId) }
                    )
                }
            }
        }

        // Editorial Media Pager (Photos + Video Reels)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 3.8f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val slide = mediaSlides[page]
                    if (slide.isVideo && exoPlayer != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = true
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    player = exoPlayer
                                }
                            },
                            update = { view ->
                                if (view.player != exoPlayer) view.player = exoPlayer
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MelvoreNetworkImage(
                            url = slide.url.takeIf { it.isNotBlank() },
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (product.isSponsored || product.expressDelivery) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        if (product.isSponsored) {
                            Surface(shape = RoundedCornerShape(1.dp), color = surfaces.imageOverlay.copy(alpha = 0.78f)) {
                                Text(
                                    "SPONSORED",
                                    color = surfaces.onImageOverlay,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (product.expressDelivery) {
                            Surface(shape = RoundedCornerShape(1.dp), color = ext.electricBlue) {
                                Text(
                                    "EXPRESS",
                                    color = ensureContrast(surfaces.onAccent, ext.electricBlue),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                if (mediaSlides.size > 1) {
                    val pdManifest = com.example.ui.theme.LocalMelvoreDesignManifest.current
                    val pdWorldTheme = com.example.ui.theme.LocalFashionWorldTheme.current
                    val pdPagerSvg = com.example.ui.components.RemoteDesignRegistry
                        .activeWorldKit(pdManifest, pdWorldTheme)?.svg?.pager
                    com.example.ui.components.RemoteSvgPagerIndicator(
                        pageCount = mediaSlides.size,
                        currentPage = pagerState.currentPage,
                        pagerSvgPath = pdPagerSvg,
                        activeColor = surfaces.onImageOverlay,
                        inactiveColor = surfaces.onImageOverlay.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    )
                }
            }
        }

        // Purchase Controls Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (product.brandName.isNotBlank()) {
                            Text(
                                text = product.brandName.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.goldAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable(enabled = (product.brandId ?: 0) > 0 || product.brandSlug.isNotBlank()) {
                                        onOpenBrand(product.brandId ?: 0, product.brandSlug)
                                    }
                            )
                            Text("·", color = ext.muted)
                        }
                        Text(product.category, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                        Text("·", color = ext.muted)
                        Text(
                            if (effectiveInStock) "In Stock" else "Out of Stock",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (effectiveInStock) ext.emerald else ext.crimson
                        )
                    }

                    if (worldTheme.isWorldActive || effectiveWorldSlug.isNotBlank()) {
                        val badgeWorld = worldTheme.worldName.ifBlank { effectiveWorldSlug }.uppercase()
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = worldAccent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, worldAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("product_world_context_badge")
                        ) {
                            Text(
                                text = "CURATED IN $badgeWorld",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold,
                                color = worldAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = if (worldTheme.isWorldActive) fontFamilyForStyleKey(worldTheme.fontStyle) else MaterialTheme.typography.headlineMedium.fontFamily,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatPrice(effectivePrice, uiState.currency),
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (effectiveRegular > effectivePrice) {
                            Text(
                                text = formatPrice(effectiveRegular, uiState.currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = ext.muted,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Text(
                                text = "Save ${product.discountPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.emerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (plainDescription.isNotBlank()) {
                        Text(plainDescription, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
                    }

                    if (product.colors.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Color: $selectedColor", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                product.colors.forEach { variant ->
                                    val active = selectedColor == variant.name
                                    Surface(
                                        onClick = { selectedColor = variant.name },
                                        shape = RoundedCornerShape(2.dp),
                                        color = if (active) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            if (active) 1.5.dp else 1.dp,
                                            if (active) MaterialTheme.colorScheme.primary else ext.borderSubtle
                                        ),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(parseHexColor(variant.hex)))
                                                    .border(1.dp, ext.borderSubtle, CircleShape)
                                            )
                                            Text(variant.name, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (product.sizes.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select Size", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                product.sizes.forEach { size ->
                                    val active = selectedSize == size
                                    Surface(
                                        onClick = { selectedSize = size },
                                        shape = RoundedCornerShape(2.dp),
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (active) null else BorderStroke(1.dp, ext.borderSubtle),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 18.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                size,
                                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, ext.borderSubtle)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier.minimumInteractiveComponentSize().size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    quantity.toString(),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { quantity++ },
                                    modifier = Modifier.minimumInteractiveComponentSize().size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onToggleWishlist(product.id) },
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, if (isWishlisted) ext.crimson else ext.borderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isWishlisted) ext.crimson else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isWishlisted) "Saved" else "Wishlist",
                                color = if (isWishlisted) ext.crimson else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MelvoreWorldActionButton(
                            text = if (effectiveInStock) "Add to Bag" else "Out of Stock",
                            onClick = { onAddToCart(product, selectedColor, selectedSize, quantity) },
                            state = if (!effectiveInStock) MelvoreWorldButtonState.Disabled else MelvoreWorldButtonState.Default,
                            leadingIcon = Icons.Filled.ShoppingBag,
                            fillWidth = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("details_add_to_cart")
                        )

                        OutlinedButton(
                            onClick = { onBuyNow(product, selectedColor, selectedSize, quantity) },
                            enabled = effectiveInStock,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, if (worldTheme.isWorldActive) worldAccent.copy(alpha = 0.65f) else ext.borderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("details_buy_now")
                        ) {
                            Text("Buy Now")
                        }
                    }

                    HorizontalDivider(color = ext.borderSubtle)

                    val specLine = listOfNotNull(
                        product.sku.takeIf { it.isNotBlank() }?.let { "SKU: $it" },
                        product.dimensions.takeIf { it.isNotBlank() }?.let { "Fit: $it" },
                        product.weight?.let { "Weight: ${it}g" }
                    ).joinToString(" · ")
                    if (specLine.isNotBlank()) {
                        Text(specLine, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PerkTag(Icons.Outlined.LocalShipping, "Free > ₹999")
                        PerkTag(Icons.Outlined.VerifiedUser, "COD Ready")
                        PerkTag(Icons.Outlined.Refresh, "7d Returns")
                    }
                }
            }
        }

        // Material, Fit & Garment Care Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFitAndCare = !showFitAndCare }
                            .heightIn(min = 48.dp)
                            .testTag("product_fit_care_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Straighten,
                                contentDescription = null,
                                tint = ext.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "DETAILS, FIT & GARMENT CARE",
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.2.sp,
                                    color = ext.goldAccent
                                )
                                Text(
                                    text = "Specifications & Atelier Care Notes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showFitAndCare) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (showFitAndCare) "Collapse details" else "Expand details",
                            tint = ext.muted
                        )
                    }

                    AnimatedVisibility(visible = showFitAndCare) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = ext.borderSubtle)
                            val specPairs = buildList {
                                if (product.brandName.isNotBlank()) add("Label" to product.brandName)
                                if (product.category.isNotBlank()) add("Department" to product.category)
                                if (product.collection.isNotBlank()) add("Edit" to product.collection)
                                if (product.dimensions.isNotBlank()) add("Silhouette / Fit" to product.dimensions)
                                if (product.originCountry.isNotBlank()) add("Origin" to product.originCountry)
                                product.weight?.takeIf { it > 0 }?.let { add("Garment Weight" to "${it.toInt()} g") }
                                if (product.sku.isNotBlank()) add("Article SKU" to product.sku)
                            }
                            specPairs.forEach { (k, v) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(k, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                    Text(v, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Text(
                                text = "Care Guide: Follow wash-care label inside the garment. Cold gentle wash or specialist dry clean recommended for structured outerwear and embellished pieces.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.muted
                            )
                        }
                    }
                }
            }
        }

        // Delivery Pincode Checker Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = ext.electricBlue, modifier = Modifier.size(17.dp))
                        Text("DELIVERY & COD AVAILABILITY", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = ext.chromeSilver)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = pincodeInput,
                            onValueChange = { pincodeInput = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Enter Pincode") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.weight(1f).testTag("product_pincode_input")
                        )
                        Button(
                            onClick = { onCheckDelivery(pincodeInput, product.id) },
                            enabled = !discoveryState.isCheckingDelivery && pincodeInput.length == 6,
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.heightIn(min = 52.dp).testTag("product_pincode_check_btn")
                        ) {
                            Text(if (discoveryState.isCheckingDelivery) "…" else "Check")
                        }
                    }
                    val check = discoveryState.deliveryCheck
                    if (check != null && check.pincode == pincodeInput) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (check.isServiceable) ext.emerald.copy(alpha = 0.12f) else ext.crimson.copy(alpha = 0.12f)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = check.deliveryMessage.ifBlank {
                                        if (check.isServiceable) "Estimated delivery in ${check.estimatedDaysMin}–${check.estimatedDaysMax} days"
                                        else "Currently unserviceable for ${check.pincode}"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (check.isServiceable) ext.emerald else ext.crimson,
                                    fontWeight = FontWeight.Bold
                                )
                                if (check.city.isNotBlank() || check.state.isNotBlank()) {
                                    Text(
                                        text = "${check.city} ${check.state} · COD ${if (check.codAvailable) "Available" else "Prepaid Only"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.muted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verified Reviews Header & Write Review CTA
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VERIFIED REVIEWS",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = ext.goldAccent
                    )
                    Text(
                        text = "${reviews.size} Buyer Reviews · ${"%.1f".format(product.rating)} ★",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (uiState.user == null) onNavigateAuth() else showWriteReviewSheet = true
                    },
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("write_review_button")
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Write Review")
                }
            }
        }

        if (reviews.isEmpty()) {
            item {
                Text(
                    text = "No verified buyer reviews yet. Verified purchasers can review with OTP confirmation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
            }
        } else {
            items(reviews, key = { "rev_${it.id}" }) { rev ->
                ProductReviewCard(
                    review = rev,
                    onToggleHelpful = { rid ->
                        if (uiState.user == null) onNavigateAuth() else onToggleReviewHelpful(rid)
                    }
                )
            }
        }

        // More From Brand Horizontal Shelf
        val brandRecommendations = discoveryState.recommendedBrandProducts.filter { it.id != product.id }
        if (brandRecommendations.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "HOUSE ARCHIVE",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.4.sp,
                                color = ext.goldAccent
                            )
                            Text(
                                text = if (product.brandName.isNotBlank()) "More from ${product.brandName}" else "More from This Label",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = MelvoreSerifFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if ((product.brandId ?: 0) > 0 || product.brandSlug.isNotBlank()) {
                            Text(
                                text = "Visit Label →",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.goldAccent,
                                modifier = Modifier
                                    .clickable { onOpenBrand(product.brandId ?: 0, product.brandSlug) }
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(brandRecommendations, key = { "brand_rec_${it.id}" }) { rec ->
                            ProductCard(
                                product = rec,
                                currency = uiState.currency,
                                isWishlisted = uiState.wishlistIds.contains(rec.id),
                                onProductClick = { onOpenProduct(rec) },
                                onWishlistClick = { onToggleWishlist(rec.id) },
                                onAddToCartClick = {
                                    onAddToCart(rec, rec.colors.firstOrNull()?.name ?: "", rec.sizes.firstOrNull() ?: "", 1)
                                },
                                modifier = Modifier.width(172.dp)
                            )
                        }
                    }
                }
            }
        }

        // Similar & Curated Pieces Horizontal Shelf
        val similarItems = discoveryState.recommendedSimilarProducts
            .filter { it.id != product.id }
            .ifEmpty { related }
        if (similarItems.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text(
                            text = "COMPLETE THE WARDROBE",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.4.sp,
                            color = ext.goldAccent
                        )
                        Text(
                            text = "Similar & Curated Pieces",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = MelvoreSerifFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(similarItems, key = { "rel_${it.id}" }) { rel ->
                            ProductCard(
                                product = rel,
                                currency = uiState.currency,
                                isWishlisted = uiState.wishlistIds.contains(rel.id),
                                onProductClick = { onOpenProduct(rel) },
                                onWishlistClick = { onToggleWishlist(rel.id) },
                                onAddToCartClick = {
                                    onAddToCart(rel, rel.colors.firstOrNull()?.name ?: "", rel.sizes.firstOrNull() ?: "", 1)
                                },
                                modifier = Modifier.width(172.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Sticky Liquid Glass Bottom Action Bar
    LiquidGlassBar(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .testTag("product_details_glass_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(0.85f)) {
                Text(
                    text = formatPrice(effectivePrice, uiState.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = surfaces.onSurface
                )
                Text(
                    text = if (effectiveInStock) "In stock · Free > ₹999" else "Out of stock",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (effectiveInStock) surfaces.success else surfaces.sale,
                    maxLines = 1
                )
            }
            MelvoreWorldActionButton(
                text = if (effectiveInStock) "Add to Bag" else "Sold Out",
                onClick = { onAddToCart(product, selectedColor, selectedSize, quantity) },
                state = if (!effectiveInStock) MelvoreWorldButtonState.Disabled else MelvoreWorldButtonState.Default,
                compact = true,
                leadingIcon = Icons.Filled.ShoppingBag,
                modifier = Modifier
                    .weight(1.15f)
                    .testTag("details_sticky_add_to_bag")
            )
            OutlinedButton(
                onClick = { onBuyNow(product, selectedColor, selectedSize, quantity) },
                enabled = effectiveInStock,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, surfaces.border),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("details_sticky_buy_now")
            ) {
                Text("Buy", style = MaterialTheme.typography.labelLarge, color = surfaces.onSurface)
            }
        }
    }
    }

    if (showWriteReviewSheet) {
        WriteVerifiedReviewSheet(
            productId = product.id,
            productName = product.name,
            eligibleItems = discoveryState.reviewList.eligibleOrderItems,
            discoveryState = discoveryState,
            onDismiss = { showWriteReviewSheet = false },
            onRequestOtp = onRequestReviewOtp,
            onVerifyOtp = onVerifyReviewOtp,
            onUploadPhoto = onUploadReviewPhoto,
            onRemovePhoto = onRemoveReviewPhoto,
            onSubmitReview = { pid, oiid, rating, title, comment ->
                onSubmitVerifiedReview(pid, oiid, rating, title, comment) {
                    showWriteReviewSheet = false
                }
            }
        )
    }
}

@Composable
private fun PerkTag(icon: ImageVector, label: String) {
    val ext = LocalMelvoreColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = ext.muted, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = ext.muted)
    }
}
