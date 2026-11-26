package com.example.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.Category
import com.example.model.Product
import com.example.model.SearchFilterState
import com.example.model.formatPrice
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.viewmodel.DiscoveryUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchExperienceModal(
    discoveryState: DiscoveryUiState,
    categories: List<Category>,
    currency: String,
    initialLaunchVoice: Boolean = false,
    onClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmitQuery: (String) -> Unit,
    onUpdateFilters: (SearchFilterState) -> Unit,
    onSelectProduct: (Product) -> Unit,
    onSelectCategory: (String) -> Unit,
    onViewAllResults: (String) -> Unit,
    onVoiceError: (String) -> Unit
) {
    val ext = LocalMelvoreColors.current
    var showFilters by remember { mutableStateOf(false) }

    val placeholders = remember {
        listOf(
            "Search \"Leather Jackets\"",
            "Search \"Oversized T-Shirts\"",
            "Search \"Korean Fashion\"",
            "Search \"Co-Ord Sets\"",
            "Search \"Winter Collection\"",
            "Search \"Dresses\"",
            "Search \"Sneakers\""
        )
    }
    var phIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            phIdx = (phIdx + 1) % placeholders.size
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotBlank()) {
                onQueryChange(spoken)
                onSubmitQuery(spoken)
            }
        }
    }

    val launchVoiceRecognizer = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a style, brand, or product…")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            onVoiceError("Voice search is not supported on this device.")
        }
    }

    LaunchedEffect(initialLaunchVoice) {
        if (initialLaunchVoice) {
            launchVoiceRecognizer()
        }
    }

    val query = discoveryState.searchQuery
    val filters = discoveryState.searchFilters
    val suggestions = discoveryState.searchSuggestions
    val results = discoveryState.searchResults.products

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Sticky Liquid Glass Header + Intelligent Search Pill
            LiquidGlassSurface(
                darkGlass = false,
                shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                elevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MelvoreWordmark(fontSize = 22.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { showFilters = !showFilters },
                                shape = RoundedCornerShape(999.dp),
                                color = if (showFilters || filters.activeCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, ext.borderSubtle),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("search_filter_toggle")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Tune,
                                        contentDescription = "Toggle filters",
                                        tint = if (showFilters || filters.activeCount > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (filters.activeCount > 0) "Filters (${filters.activeCount})" else "Filters",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (showFilters || filters.activeCount > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                onClick = onClose,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, ext.borderSubtle),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(48.dp)
                                    .testTag("search_close_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                                }
                            }
                        }
                    }

                    // Full-width Intelligent Search Bar + Voice Mic
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = {
                            AnimatedContent(
                                targetState = placeholders[phIdx],
                                transitionSpec = {
                                    (slideInVertically(tween(240)) { h -> h / 2 } + fadeIn(tween(240))) togetherWith
                                        (slideOutVertically(tween(200)) { h -> -h / 2 } + fadeOut(tween(180)))
                                },
                                label = "modal_search_placeholder_ticker"
                            ) { hint ->
                                Text(
                                    text = hint,
                                    color = ext.muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQueryChange("") },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                                IconButton(
                                    onClick = launchVoiceRecognizer,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("search_voice_mic_button")
                                ) {
                                    Icon(
                                        Icons.Outlined.Mic,
                                        contentDescription = stringResource(R.string.cd_voice_search),
                                        tint = ext.goldAccent
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmitQuery(query) }),
                        shape = RoundedCornerShape(999.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = ext.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("search_overlay_input")
                    )
                }
            }

            // Collapsible Filter Bar
            AnimatedVisibility(visible = showFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("SORT BY", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = ext.muted)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "relevance" to "Relevance",
                            "price_asc" to "Price: Low-High",
                            "price_desc" to "Price: High-Low",
                            "rating" to "Top Rated",
                            "newest" to "Newest"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = filters.sort == key,
                                onClick = { onUpdateFilters(filters.copy(sort = key)) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Text("SIZE & BUDGET", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = ext.muted)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("XS", "S", "M", "L", "XL", "XXL").forEach { sz ->
                            val selected = filters.size == sz
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateFilters(filters.copy(size = if (selected) null else sz)) },
                                label = { Text(sz) },
                                shape = RoundedCornerShape(999.dp)
                            )
                        }
                        listOf(999.0 to "Under ${currency}999", 1999.0 to "Under ${currency}1,999", 3499.0 to "Under ${currency}3,499").forEach { (cap, label) ->
                            val selected = filters.maxPrice == cap
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateFilters(filters.copy(maxPrice = if (selected) null else cap)) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(999.dp)
                            )
                        }
                    }

                    if (filters.activeCount > 0) {
                        OutlinedButton(
                            onClick = { onUpdateFilters(SearchFilterState()) },
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Reset Filters")
                        }
                    }
                    HorizontalDivider(color = ext.borderSubtle)
                }
            }

            if (discoveryState.isSearchLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Autocomplete Suggestions
                if (suggestions.suggestions.isNotEmpty() && query.isNotBlank()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("SUGGESTIONS", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = ext.muted)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                suggestions.suggestions.forEach { sug ->
                                    Surface(
                                        onClick = { onSubmitQuery(sug) },
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, ext.borderSubtle),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                            Text(sug, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Server Search Results
                if (query.trim().length >= 2 || filters.activeCount > 0) {
                    if (!discoveryState.isSearchLoading && results.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, ext.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("No matching pieces found for \"$query\"", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Try removing filters or searching another edit like Leather Jackets, Oversized, or Korean Fashion.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ext.muted
                                    )
                                }
                            }
                        }
                    } else if (results.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${discoveryState.searchResults.total.coerceAtLeast(results.size)} MATCHES",
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.4.sp,
                                    color = ext.muted
                                )
                                Text(
                                    text = "Open Full Grid →",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ext.goldAccent,
                                    modifier = Modifier
                                        .clickable { onViewAllResults(query) }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                        items(results, key = { it.id }) { prod ->
                            Surface(
                                onClick = { onSelectProduct(prod) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, ext.borderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 76.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MelvoreNetworkImage(
                                        url = prod.imageUrl,
                                        contentDescription = prod.name,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (prod.brandName.isNotBlank()) {
                                            Text(
                                                text = prod.brandName.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.muted
                                            )
                                        }
                                        Text(prod.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = formatPrice(prod.effectivePrice, currency),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Recent Searches
                    if (suggestions.recent.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = ext.muted)
                                    Text("RECENT SEARCHES", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = ext.muted)
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    suggestions.recent.forEach { rec ->
                                        Surface(
                                            onClick = { onSubmitQuery(rec) },
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, ext.borderSubtle),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                                Text(rec, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Trending Searches
                    val trendingList = suggestions.trending.ifEmpty {
                        listOf("Leather Jackets", "Oversized T-Shirts", "Korean Fashion", "Co-Ord Sets", "Winter Collection", "Dresses")
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = ext.goldAccent)
                                Text("TRENDING ON MELVORE", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = ext.muted)
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                trendingList.forEach { tr ->
                                    Surface(
                                        onClick = { onSubmitQuery(tr) },
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, ext.borderSubtle),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                            Text(tr, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Categories shortcut
                    if (categories.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("BROWSE CATEGORIES", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = ext.muted)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categories.take(12).forEach { cat ->
                                        Surface(
                                            onClick = { onSelectCategory(cat.slug) },
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, ext.borderSubtle),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                                Text(cat.name, style = MaterialTheme.typography.bodySmall)
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
}

@Composable
fun PincodeSelectorDialog(
    currentPincode: String,
    isChecking: Boolean,
    onDismiss: () -> Unit,
    onApplyPincode: (String) -> Unit
) {
    val ext = LocalMelvoreColors.current
    var pinInput by remember(currentPincode) { mutableStateOf(currentPincode) }

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassBox(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = ext.goldAccent)
                    Text("Delivery Location & Regional Edits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Enter your 6-digit pincode to verify courier delivery speed and unlock regional festival campaigns without granting GPS access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("6-Digit Pincode") },
                    placeholder = { Text("e.g. 110001") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pincode_dialog_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onApplyPincode(pinInput)
                            onDismiss()
                        },
                        enabled = !isChecking && pinInput.length == 6,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("pincode_apply_button")
                    ) {
                        Text(if (isChecking) "Checking…" else "Apply")
                    }
                }
            }
        }
    }
}
