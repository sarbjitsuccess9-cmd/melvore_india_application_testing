package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.OfflineCacheBanner
import com.example.ui.components.ProductCard
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.viewmodel.MelvoreUiState
import kotlinx.coroutines.delay

@Composable
fun ShopScreen(
    uiState: MelvoreUiState,
    initialCategory: String,
    initialCollection: String,
    initialQuery: String,
    initialBrandId: Int = 0,
    onFetchCatalog: (category: String, collection: String, search: String, sort: String, brandId: Int) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    var activeCategory by remember(initialCategory) { mutableStateOf(initialCategory.ifBlank { "all" }) }
    var activeCollection by remember(initialCollection) { mutableStateOf(initialCollection) }
    var activeBrandId by remember(initialBrandId) { mutableStateOf(initialBrandId) }
    var localQuery by remember(initialQuery) { mutableStateOf(initialQuery) }
    var sortBy by remember { mutableStateOf("featured") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(activeCategory, activeCollection, localQuery, sortBy, activeBrandId) {
        if (localQuery.isNotBlank()) delay(250)
        onFetchCatalog(activeCategory, activeCollection, localQuery, sortBy, activeBrandId)
    }

    val sortLabel = when (sortBy) {
        "price_asc" -> "Price: Low to High"
        "price_desc" -> "Price: High to Low"
        "rating" -> "Top Rated"
        "newest" -> "Newest"
        else -> "Featured"
    }

    val displayedProducts = uiState.catalogProducts

    val activeBrand = remember(uiState.brands, activeBrandId) {
        uiState.brands.find { it.id == activeBrandId }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.isShowingCachedData) {
            item(key = "shop_offline_cache_banner") {
                OfflineCacheBanner(
                    cachedItemCount = uiState.cachedProductCount.takeIf { it > 0 } ?: displayedProducts.size,
                    onRetry = { onFetchCatalog(activeCategory, activeCollection, localQuery, sortBy, activeBrandId) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            activeBrand != null -> activeBrand.name
                            activeCollection.equals("under-999", ignoreCase = true) -> "Under ₹999 Edit"
                            activeCollection.equals("under-1999", ignoreCase = true) -> "Under ₹1,999 Edit"
                            activeCollection.equals("up-to-90-off", ignoreCase = true) -> "Up to 90% Off"
                            activeCollection.isNotBlank() -> activeCollection.replace("-", " ").split(" ").joinToString(" ") { w ->
                                w.replaceFirstChar { it.uppercase() }
                            }
                            activeCategory.isNotBlank() && !activeCategory.equals("all", ignoreCase = true) ->
                                activeCategory.replace("-", " ").split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
                            else -> "All Products"
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${displayedProducts.size} ${if (displayedProducts.size == 1) "item" else "items"} available",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = ext.muted
                    )
                }
                if (activeCollection.isNotBlank() || activeBrandId > 0) {
                    OutlinedButton(
                        onClick = {
                            activeCollection = ""
                            activeCategory = "all"
                            activeBrandId = 0
                        },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Clear filter", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = localQuery,
                    onValueChange = { localQuery = it },
                    placeholder = { Text("Search jackets, labels, silhouettes…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (localQuery.isNotEmpty()) {
                            IconButton(onClick = { localQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear filter")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_search_input")
                )

                if (uiState.brands.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryFilterChip("All Brands", activeBrandId == 0) { activeBrandId = 0 }
                        uiState.brands.forEach { b ->
                            CategoryFilterChip(b.name, activeBrandId == b.id) { activeBrandId = b.id }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryFilterChip("All", activeCategory == "all") { activeCategory = "all" }
                        uiState.categories.forEach { cat ->
                            CategoryFilterChip(cat.name, activeCategory == cat.slug) { activeCategory = cat.slug }
                        }
                    }

                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        Surface(
                            onClick = { sortMenuExpanded = true },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ext.borderSubtle),
                            modifier = Modifier.testTag("shop_sort_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Outlined.FilterList, contentDescription = "Sort", modifier = Modifier.size(16.dp))
                                Text(sortLabel, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            listOf(
                                "featured" to "Featured",
                                "newest" to "Newest",
                                "price_asc" to "Price: Low to High",
                                "price_desc" to "Price: High to Low",
                                "rating" to "Top Rated"
                            ).forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        sortBy = key
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isCatalogLoading) {
            item {
                MelvoreLoadingState("Fetching products from Melvore…")
            }
        } else if (uiState.catalogError != null) {
            item {
                MelvoreErrorState(
                    message = uiState.catalogError,
                    onRetry = { onFetchCatalog(activeCategory, activeCollection, localQuery, sortBy, activeBrandId) }
                )
            }
        } else if (displayedProducts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("No matching products found", style = MaterialTheme.typography.titleMedium)
                        Text("Try clearing your search or switching filters.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                        Button(
                            onClick = {
                                activeCategory = "all"
                                activeCollection = ""
                                activeBrandId = 0
                                localQuery = ""
                                sortBy = "featured"
                            },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text("Reset Filters")
                        }
                    }
                }
            }
        } else {
            items(displayedProducts.chunked(2)) { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { product ->
                        ProductCard(
                            product = product,
                            currency = uiState.currency,
                            isWishlisted = uiState.wishlistIds.contains(product.id),
                            onProductClick = { onOpenProduct(product) },
                            onWishlistClick = { onToggleWishlist(product.id) },
                            onAddToCartClick = { onQuickAdd(product) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (selected) null else BorderStroke(1.dp, ext.borderSubtle)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else ext.muted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun CategoriesScreen(
    uiState: MelvoreUiState,
    onRetry: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    onOpenProduct: (Product) -> Unit = {},
    onIncrementCart: (Product) -> Unit = {},
    onDecrementCart: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current

    if (uiState.isHomeLoading && uiState.categories.isEmpty() && uiState.products.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MelvoreLoadingState("Loading categories…")
        }
        return
    }

    if (uiState.homeError != null && uiState.categories.isEmpty() && uiState.products.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            MelvoreErrorState(message = uiState.homeError, onRetry = onRetry)
        }
        return
    }

    val allCatalog = remember(uiState.products, uiState.catalogProducts) {
        (uiState.products + uiState.catalogProducts).distinctBy { it.id }
    }
    val railCategories = uiState.categories
    var selectedCategorySlug by remember(railCategories) {
        mutableStateOf(railCategories.firstOrNull()?.slug ?: "all")
    }
    var activeSort by remember { mutableStateOf("popular") }
    var onlyOnSale by remember { mutableStateOf(false) }

    val activeCategory = remember(railCategories, selectedCategorySlug) {
        railCategories.firstOrNull { it.slug.equals(selectedCategorySlug, ignoreCase = true) }
    }

    val filteredProducts = remember(allCatalog, selectedCategorySlug, activeCategory, activeSort, onlyOnSale) {
        val byCat = if (selectedCategorySlug == "all" || activeCategory == null) {
            allCatalog
        } else {
            allCatalog.filter { p ->
                p.categoryId == activeCategory.id ||
                    p.category.equals(activeCategory.name, ignoreCase = true) ||
                    p.category.equals(activeCategory.slug, ignoreCase = true) ||
                    p.category.contains(activeCategory.name, ignoreCase = true)
            }.ifEmpty { allCatalog }
        }
        val bySale = if (onlyOnSale) {
            byCat.filter { it.discountPercent > 0 }
        } else byCat

        when (activeSort) {
            "price_asc" -> bySale.sortedBy { it.effectivePrice }
            "price_desc" -> bySale.sortedByDescending { it.effectivePrice }
            "discount" -> bySale.sortedByDescending { it.discountPercent }
            else -> bySale
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("categories_left_rail_screen")
    ) {
        // Left vertical category sidebar (Blinkit / Instamart style)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier
                .width(90.dp)
                .fillMaxHeight()
                .testTag("categories_left_rail")
        ) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item(key = "rail_all") {
                    LeftRailCategoryItem(
                        name = "All",
                        imageUrl = null,
                        selected = selectedCategorySlug == "all",
                        onClick = { selectedCategorySlug = "all" },
                        testTag = "rail_cat_all"
                    )
                }
                items(railCategories, key = { it.id }) { cat ->
                    LeftRailCategoryItem(
                        name = cat.name,
                        imageUrl = cat.imageUrl,
                        selected = selectedCategorySlug.equals(cat.slug, ignoreCase = true),
                        onClick = { selectedCategorySlug = cat.slug },
                        testTag = "rail_cat_${cat.slug}"
                    )
                }
            }
        }

        // Divider between left rail and right product grid
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(ext.borderSubtle)
        )

        // Right pane: header + sticky filter/sort chips + 2-column QuickCommerceProductCard grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Sticky filter/sort chips bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = activeCategory?.name ?: "All Styles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${filteredProducts.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.muted
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("categories_filter_chips")
                ) {
                    item(key = "chip_popular") {
                        CategoryFilterChip(
                            label = "Popular",
                            selected = activeSort == "popular",
                            onClick = { activeSort = "popular" }
                        )
                    }
                    item(key = "chip_price_asc") {
                        CategoryFilterChip(
                            label = "Low to High",
                            selected = activeSort == "price_asc",
                            onClick = { activeSort = "price_asc" }
                        )
                    }
                    item(key = "chip_price_desc") {
                        CategoryFilterChip(
                            label = "High to Low",
                            selected = activeSort == "price_desc",
                            onClick = { activeSort = "price_desc" }
                        )
                    }
                    item(key = "chip_discount") {
                        CategoryFilterChip(
                            label = "Best Deals",
                            selected = activeSort == "discount",
                            onClick = { activeSort = "discount" }
                        )
                    }
                    item(key = "chip_sale") {
                        CategoryFilterChip(
                            label = "On Sale",
                            selected = onlyOnSale,
                            onClick = { onlyOnSale = !onlyOnSale }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ext.borderSubtle)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredProducts.isEmpty()) {
                    item(key = "cat_empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No items match these filters",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Try clearing sale filter or exploring all styles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.muted
                            )
                        }
                    }
                } else {
                    items(filteredProducts.chunked(2), key = { row -> row.joinToString("_") { it.id.toString() } }) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { prod ->
                                com.example.ui.components.QuickCommerceProductCard(
                                    product = prod,
                                    currency = uiState.currency,
                                    cartQuantity = uiState.cartQuantityForProduct(prod.id),
                                    onProductClick = { onOpenProduct(prod) },
                                    onIncrement = { onIncrementCart(prod) },
                                    onDecrement = { onDecrementCart(prod.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                if (uiState.collections.isNotEmpty() && selectedCategorySlug == "all") {
                    item(key = "cat_collections_hdr") {
                        Text(
                            text = "Curated Collections",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(uiState.collections.take(4), key = { "col_${it.id}" }) { col ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(118.dp)
                                .clip(RoundedCornerShape(14.dp))
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
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.82f))))
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                if (col.subtitle.isNotBlank()) {
                                    Text(col.subtitle, color = surfaces.onImageOverlay.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
                                }
                                Text(col.name, color = surfaces.onImageOverlay, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftRailCategoryItem(
    name: String,
    imageUrl: String?,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val ext = LocalMelvoreColors.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) accent.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Active left indicator bar (Blinkit / Instamart signature)
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(if (selected) accent else Color.Transparent)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) accent else ext.borderSubtle
                ),
                modifier = Modifier.size(50.dp)
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    MelvoreNetworkImage(
                        url = imageUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = name.take(2).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}
