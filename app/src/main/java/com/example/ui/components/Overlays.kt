package com.example.ui.components

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CartItem
import com.example.model.CartSummary
import com.example.model.Category
import com.example.model.CollectionItem
import com.example.model.Product
import com.example.model.TrendingTag
import com.example.model.User
import com.example.model.formatPrice
import com.example.model.parseHexColor
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.ensureContrast
import kotlin.math.max

@Composable
fun SearchDiscoveryModal(
    products: List<Product>,
    categories: List<Category>,
    collections: List<CollectionItem>,
    trendingTags: List<TrendingTag>,
    currency: String,
    onClose: () -> Unit,
    onSelectProduct: (Product) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    onViewAllResults: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim().lowercase()
    val filtered = remember(trimmed, products) {
        if (trimmed.isEmpty()) emptyList()
        else products.filter {
            it.name.lowercase().contains(trimmed) ||
                it.category.lowercase().contains(trimmed) ||
                it.description.lowercase().contains(trimmed) ||
                it.sku.lowercase().contains(trimmed)
        }
    }
    val arrivals = remember(products) { products.take(4) }
    val recommended = remember(products) { products.sortedByDescending { it.rating }.take(4) }

    val surfaces = LocalMelvoreSurfaces.current
    val ext = LocalMelvoreColors.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaces.background.copy(alpha = 0.98f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MelvoreWordmark(fontSize = 22.sp, color = surfaces.onSurface)
                LiquidGlassBox(
                    onClick = onClose,
                    shape = CircleShape,
                    elevation = 2.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("search_close_button")
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Close search", tint = surfaces.onSurface)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search jackets, loungewear, Korean street…", color = surfaces.onSurfaceMuted) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = surfaces.accent) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = surfaces.onSurface)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = surfaces.onSurface,
                    unfocusedTextColor = surfaces.onSurface,
                    focusedContainerColor = surfaces.surfaceElevated,
                    unfocusedContainerColor = surfaces.surface,
                    focusedBorderColor = surfaces.accent,
                    unfocusedBorderColor = surfaces.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_overlay_input")
            )

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (trimmed.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = surfaces.surface,
                        border = BorderStroke(1.dp, surfaces.border)
                    ) {
                        if (filtered.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "No cached matches for \"$query\"",
                                    color = surfaces.onSurfaceMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { onViewAllResults(query) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Search Storefront Server")
                                }
                            }
                        } else {
                            Column {
                                Text(
                                    text = "Matching Products",
                                    color = surfaces.onSurfaceMuted,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 6.dp)
                                )
                                filtered.forEach { product ->
                                    SearchProductRow(
                                        product = product,
                                        currency = currency,
                                        onClick = { onSelectProduct(product) }
                                    )
                                    HorizontalDivider(color = surfaces.border)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onViewAllResults(query) }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "View all results for \"$query\"",
                                        color = surfaces.accent,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = surfaces.accent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                } else {
                    if (trendingTags.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = surfaces.surface,
                            border = BorderStroke(1.dp, surfaces.border)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = ext.amber, modifier = Modifier.size(16.dp))
                                    Text("Trending Now", color = surfaces.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    trendingTags.forEach { tag ->
                                        Surface(
                                            onClick = { query = tag.name },
                                            shape = CircleShape,
                                            color = surfaces.surfaceElevated,
                                            border = BorderStroke(1.dp, surfaces.border)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.LocalFireDepartment,
                                                    contentDescription = null,
                                                    tint = ext.amber,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(tag.name, color = surfaces.onSurface, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (categories.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = surfaces.surface,
                            border = BorderStroke(1.dp, surfaces.border)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.GridView, contentDescription = null, tint = surfaces.onSurfaceMuted, modifier = Modifier.size(16.dp))
                                    Text("Shop by Category", color = surfaces.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    categories.chunked(2).forEach { pair ->
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            pair.forEach { cat ->
                                                Surface(
                                                    onClick = { onSelectCategory(cat.slug) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = surfaces.surfaceElevated,
                                                    border = BorderStroke(1.dp, surfaces.border)
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        if (!cat.imageUrl.isNullOrBlank()) {
                                                            MelvoreNetworkImage(
                                                                url = cat.imageUrl,
                                                                contentDescription = cat.name,
                                                                modifier = Modifier
                                                                    .size(34.dp)
                                                                    .clip(CircleShape)
                                                            )
                                                        } else {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = surfaces.accent.copy(alpha = 0.14f),
                                                                modifier = Modifier.size(34.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        cat.name.take(1).uppercase(),
                                                                        color = surfaces.accent,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(cat.name, color = surfaces.onSurface, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (arrivals.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = surfaces.surface,
                            border = BorderStroke(1.dp, surfaces.border)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = surfaces.success, modifier = Modifier.size(16.dp))
                                    Text("New Arrivals", color = surfaces.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
                                }
                                arrivals.forEach { product ->
                                    SearchProductRow(product = product, currency = currency, onClick = { onSelectProduct(product) })
                                }
                            }
                        }
                    }

                    if (recommended.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = surfaces.surface,
                            border = BorderStroke(1.dp, surfaces.border)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = ext.amber, modifier = Modifier.size(16.dp))
                                    Text("Recommended", color = surfaces.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
                                }
                                recommended.forEach { product ->
                                    SearchProductRow(product = product, currency = currency, onClick = { onSelectProduct(product) })
                                }
                            }
                        }
                    }

                    if (collections.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = surfaces.surface,
                            border = BorderStroke(1.dp, surfaces.border)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.Layers, contentDescription = null, tint = surfaces.onSurfaceMuted, modifier = Modifier.size(16.dp))
                                    Text("Featured Collections", color = surfaces.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
                                }
                                collections.forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, surfaces.border, RoundedCornerShape(12.dp))
                                            .clickable { onSelectCollection(col.slug) }
                                    ) {
                                        MelvoreNetworkImage(
                                            url = col.bannerUrl,
                                            contentDescription = col.name,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            surfaces.imageOverlay.copy(alpha = 0.88f),
                                                            surfaces.imageOverlay.copy(alpha = 0.45f)
                                                        )
                                                    )
                                                )
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                if (col.subtitle.isNotBlank()) {
                                                    Text(col.subtitle, color = surfaces.onImageOverlay.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall)
                                                }
                                                Text(col.name, color = surfaces.onImageOverlay, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = surfaces.onImageOverlay.copy(alpha = 0.86f), modifier = Modifier.size(16.dp))
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
}

@Composable
private fun SearchProductRow(
    product: Product,
    currency: String,
    onClick: () -> Unit
) {
    val surfaces = LocalMelvoreSurfaces.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MelvoreNetworkImage(
            url = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, color = surfaces.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(product.category, color = surfaces.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = formatPrice(product.effectivePrice, currency),
            color = surfaces.onSurface,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartItems: List<CartItem>,
    cartCount: Int,
    cartSummary: CartSummary,
    isCartBusy: Boolean,
    currency: String,
    onDismiss: () -> Unit,
    onUpdateQty: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onBrowseProducts: () -> Unit,
    onCheckout: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val subtotal = cartSummary.subtotal.takeIf { it > 0.0 } ?: cartItems.sumOf { it.lineTotal }
    val freeShippingThreshold = cartSummary.freeShipMin
    val remainingForFreeShipping = if (freeShippingThreshold > 0.0) max(0.0, freeShippingThreshold - subtotal) else 0.0
    val shippingFee = cartSummary.shippingCharge

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cart", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "($cartCount)",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        color = ext.muted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close cart")
                }
            }

            if (isCartBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (cartItems.isNotEmpty() && freeShippingThreshold > 0.0) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (remainingForFreeShipping == 0.0) {
                            "Your order qualifies for free express shipping across India."
                        } else {
                            "Add ${formatPrice(remainingForFreeShipping, currency)} more for free express shipping."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (remainingForFreeShipping == 0.0) ext.emerald else ext.muted,
                        fontWeight = if (remainingForFreeShipping == 0.0) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }

            if (cartItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = ext.muted, modifier = Modifier.size(28.dp))
                        }
                    }
                    Text("Your cart is empty", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Explore Melvore jackets and second-skin loungewear.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.muted
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onBrowseProducts,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("cart_browse_button")
                    ) {
                        Text("Browse Products")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems, key = { it.cartId }) { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ext.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MelvoreNetworkImage(
                                    url = item.imageUrl,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .size(78.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = formatPrice(item.price, currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    val variantText = listOfNotNull(
                                        item.color.takeIf { it.isNotBlank() },
                                        item.size.takeIf { it.isNotBlank() }?.let { "Size $it" }
                                    ).joinToString(" · ")
                                    if (variantText.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(parseHexColor(item.colorHex)))
                                                    .border(1.dp, ext.borderSubtle, CircleShape)
                                            )
                                            Text(variantText, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, ext.borderSubtle)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { onUpdateQty(item.cartId, -1) },
                                                    enabled = !isCartBusy,
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(15.dp))
                                                }
                                                Text(
                                                    text = item.quantity.toString(),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                )
                                                IconButton(
                                                    onClick = { onUpdateQty(item.cartId, 1) },
                                                    enabled = !isCartBusy,
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity", modifier = Modifier.size(15.dp))
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable(enabled = !isCartBusy) { onRemoveItem(item.cartId) }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove item", tint = ext.crimson, modifier = Modifier.size(15.dp))
                                            Text("Remove", color = ext.crimson, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = ext.borderSubtle)
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", color = ext.muted, style = MaterialTheme.typography.bodyMedium)
                        Text(formatPrice(subtotal, currency), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Shipping", color = ext.muted, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (shippingFee <= 0.0) "Free" else formatPrice(shippingFee, currency),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estimated Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            formatPrice(cartSummary.totalAmount.takeIf { it > 0.0 } ?: (subtotal + shippingFee), currency),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onBrowseProducts,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Shop more")
                        }
                        Button(
                            onClick = onCheckout,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("cart_checkout_button")
                        ) {
                            Text("Checkout")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMenuBottomSheet(
    user: User?,
    pendingOrdersCount: Int,
    wishlistCount: Int,
    onDismiss: () -> Unit,
    onNavigateProfileTab: (String) -> Unit,
    onNavigateAuth: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateShop: () -> Unit,
    onNavigateCategories: () -> Unit,
    onNavigateElite: () -> Unit,
    onNavigateTrack: () -> Unit,
    onNavigateBlog: () -> Unit,
    onNavigateFaq: () -> Unit,
    onNavigateContact: () -> Unit,
    onLogout: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            if (user != null) {
                val initials = user.name.trim().split(" ").filter { it.isNotBlank() }.take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(initials.ifBlank { "M" }, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                    }
                }
                HorizontalDivider(color = ext.borderSubtle, modifier = Modifier.padding(vertical = 8.dp))

                Text("Account", style = MaterialTheme.typography.labelMedium, color = ext.muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                SheetMenuRow(Icons.Outlined.Person, "My Profile") { onNavigateProfileTab("overview") }
                SheetMenuRow(Icons.Outlined.ShoppingBag, "Orders", badge = pendingOrdersCount) { onNavigateProfileTab("orders") }
                SheetMenuRow(Icons.Outlined.FavoriteBorder, "Wishlist", badge = wishlistCount) { onNavigateProfileTab("wishlist") }
                SheetMenuRow(Icons.Outlined.LocationOn, "Addresses") { onNavigateProfileTab("addresses") }
                SheetMenuRow(Icons.Outlined.HeadsetMic, "Help & Tickets") { onNavigateFaq() }

                HorizontalDivider(color = ext.borderSubtle, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                Text("Explore & Account", style = MaterialTheme.typography.labelMedium, color = ext.muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                SheetMenuRow(Icons.Outlined.Diamond, "Melvore Luxe Showroom", tint = ext.goldAccent, onClick = onNavigateElite)
                SheetMenuRow(Icons.Outlined.LocalShipping, "Track Order", onClick = onNavigateTrack)
                SheetMenuRow(Icons.Outlined.AutoStories, "Editorial Journal", onClick = onNavigateBlog)
                SheetMenuRow(Icons.AutoMirrored.Outlined.HelpOutline, "FAQ & Help Center", onClick = onNavigateFaq)
                SheetMenuRow(Icons.Outlined.Phone, "Contact", onClick = onNavigateContact)
                SheetMenuRow(Icons.Outlined.Settings, "Preferences & Currency") { onNavigateProfileTab("settings") }
                HorizontalDivider(color = ext.borderSubtle, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                SheetMenuRow(
                    Icons.AutoMirrored.Outlined.Logout,
                    "Logout",
                    tint = ext.crimson,
                    onClick = onLogout
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Welcome to Melvore", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Sign in to manage orders, wishlist, and addresses",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    OutlinedButton(
                        onClick = { onNavigateAuth("login") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sheet_login_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Login")
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onNavigateAuth("register") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sheet_register_button")
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create Account")
                    }
                }
                HorizontalDivider(color = ext.borderSubtle, modifier = Modifier.padding(vertical = 12.dp))
                Text("Browse", style = MaterialTheme.typography.labelMedium, color = ext.muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                SheetMenuRow(Icons.Outlined.Home, "Home", onClick = onNavigateHome)
                SheetMenuRow(Icons.Outlined.Storefront, "Products", onClick = onNavigateShop)
                SheetMenuRow(Icons.Outlined.GridView, "Categories", onClick = onNavigateCategories)
                SheetMenuRow(Icons.Outlined.Diamond, "Melvore Luxe Showroom", tint = ext.goldAccent, onClick = onNavigateElite)
                SheetMenuRow(Icons.Outlined.LocalShipping, "Track Order", onClick = onNavigateTrack)
                SheetMenuRow(Icons.AutoMirrored.Outlined.HelpOutline, "FAQ & Help Center", onClick = onNavigateFaq)
                SheetMenuRow(Icons.Outlined.AutoStories, "Editorial Journal", onClick = onNavigateBlog)
                SheetMenuRow(Icons.Outlined.Phone, "Contact", onClick = onNavigateContact)
            }
        }
    }
}

@Composable
private fun SheetMenuRow(
    icon: ImageVector,
    label: String,
    badge: Int = 0,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (badge > 0) {
            val surfaces = LocalMelvoreSurfaces.current
            Surface(shape = CircleShape, color = surfaces.sale) {
                Text(
                    text = badge.toString(),
                    color = ensureContrast(surfaces.onAccent, surfaces.sale),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickVariationSelectorBottomSheet(
    product: Product,
    currency: String,
    isCartBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirmAdd: (color: String?, size: String?) -> Unit,
    onOpenFullDetails: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val variationColors = remember(product) {
        product.colors.ifEmpty {
            product.variations
                .mapNotNull { it.color.takeIf { c -> c.isNotBlank() } }
                .distinct()
                .map { com.example.model.ColorVariant(name = it, hex = "#18181B") }
        }
    }
    val variationSizes = remember(product) {
        product.sizes.ifEmpty {
            product.variations
                .mapNotNull { it.size.takeIf { s -> s.isNotBlank() } }
                .distinct()
        }
    }

    var selectedColor by remember(product.id) { mutableStateOf(variationColors.firstOrNull()?.name) }
    var selectedSize by remember(product.id) { mutableStateOf(variationSizes.firstOrNull()) }

    val matchedVariation = remember(product, selectedColor, selectedSize) {
        product.variations.find { v ->
            v.isActive &&
                (selectedColor == null || v.color.equals(selectedColor, ignoreCase = true) || v.color.isBlank()) &&
                (selectedSize == null || v.size.equals(selectedSize, ignoreCase = true) || v.size.isBlank())
        }
    }
    val activePrice = matchedVariation?.salePrice ?: matchedVariation?.regularPrice ?: product.effectivePrice
    val availableStock = matchedVariation?.stockQuantity ?: product.stockQuantity
    val canAdd = product.inStock && (product.variations.isEmpty() || matchedVariation == null || (matchedVariation.isActive && matchedVariation.stockQuantity > 0))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("quick_variation_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MelvoreNetworkImage(
                    url = matchedVariation?.imageUrl ?: product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = product.brandName.ifBlank { product.category }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.goldAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatPrice(activePrice, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close variation selector")
                }
            }

            if (variationColors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Color: ${selectedColor.orEmpty()}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        variationColors.forEach { cv ->
                            val picked = selectedColor.equals(cv.name, ignoreCase = true)
                            Surface(
                                onClick = { selectedColor = cv.name },
                                shape = RoundedCornerShape(4.dp),
                                color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (picked) MaterialTheme.colorScheme.primary else ext.borderSubtle),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(parseHexColor(cv.hex)))
                                            .border(1.dp, ext.borderSubtle, CircleShape)
                                    )
                                    Text(
                                        text = cv.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (picked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (variationSizes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Size: ${selectedSize.orEmpty()}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        variationSizes.forEach { sz ->
                            val picked = selectedSize.equals(sz, ignoreCase = true)
                            Surface(
                                onClick = { selectedSize = sz },
                                shape = RoundedCornerShape(4.dp),
                                color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (picked) MaterialTheme.colorScheme.primary else ext.borderSubtle),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("quick_var_size_$sz")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = sz,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (picked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (availableStock in 1..5) {
                Text(
                    text = "Only $availableStock left in this option",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.amber
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenFullDetails,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                ) {
                    Text("Details")
                }
                Button(
                    onClick = { onConfirmAdd(selectedColor, selectedSize) },
                    enabled = canAdd && !isCartBusy,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("quick_var_add_button")
                ) {
                    Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (canAdd) "Add to Bag" else "Out of Stock")
                }
            }
        }
    }
}
