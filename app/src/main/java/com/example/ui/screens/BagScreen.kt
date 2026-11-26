package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CartItem
import com.example.model.formatPrice
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun BagScreen(
    uiState: MelvoreUiState,
    onUpdateQty: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onBrowseProducts: () -> Unit,
    onProceedCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val items = uiState.cartItems

    if (items.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("bag_empty_state"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = ext.muted, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Your Bag is Empty", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Explore editorial drops, aesthetic worlds, and luxury houses to build your wardrobe.",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBrowseProducts,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("bag_browse_button")
            ) {
                Text("Explore Styles")
            }
        }
        return
    }

    val subtotal = uiState.cartSubtotal
    val freeShipThreshold = uiState.cartSummary.freeShipMin.takeIf { it > 0.0 } ?: 999.0
    val remaining = (freeShipThreshold - subtotal).coerceAtLeast(0.0)
    val shippingFee = uiState.cartSummary.shippingCharge
    val progress = (subtotal / freeShipThreshold).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("bag_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("YOUR BAG", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.6.sp, color = ext.goldAccent)
                    Text("${uiState.cartCount} Items", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Free Shipping Threshold Indicator
        item {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Outlined.LocalShipping,
                            contentDescription = null,
                            tint = if (remaining <= 0.0) ext.emerald else ext.chromeSilver,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (remaining <= 0.0) {
                                "Unlocked Complimentary Shipping"
                            } else {
                                "Add ${formatPrice(remaining, uiState.currency)} more for complimentary delivery"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (remaining <= 0.0) ext.emerald else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (remaining <= 0.0) ext.emerald else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        items(items, key = { "bag_item_${it.cartId}" }) { item ->
            BagItemRow(
                item = item,
                currency = uiState.currency,
                isBusy = uiState.isCartBusy,
                onUpdateQty = onUpdateQty,
                onRemoveItem = onRemoveItem
            )
        }

        // Order Summary & Checkout CTA
        item {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", color = ext.muted, style = MaterialTheme.typography.bodyMedium)
                        Text(formatPrice(subtotal, uiState.currency), fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery", color = ext.muted, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (shippingFee <= 0.0) "Free" else formatPrice(shippingFee, uiState.currency),
                            fontFamily = FontFamily.Monospace,
                            color = if (shippingFee <= 0.0) ext.emerald else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(color = ext.borderSubtle)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatPrice(
                                uiState.cartSummary.totalAmount.takeIf { it > 0.0 } ?: (subtotal + shippingFee),
                                uiState.currency
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (uiState.activeCoupons.isNotEmpty()) {
                        Text(
                            text = "Available codes at checkout: ${uiState.activeCoupons.joinToString(", ") { it.code }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.goldAccent
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onBrowseProducts,
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Shop More")
                        }
                        Button(
                            onClick = onProceedCheckout,
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("bag_checkout_button")
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

@Composable
private fun BagItemRow(
    item: CartItem,
    currency: String,
    isBusy: Boolean,
    onUpdateQty: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    val ext = LocalMelvoreColors.current
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MelvoreNetworkImage(
                url = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier.size(84.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = listOfNotNull(
                    item.color.takeIf { it.isNotBlank() },
                    item.size.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                }
                Text(
                    text = formatPrice(item.lineTotal, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, ext.borderSubtle)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateQty(item.cartId, -1) },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = item.quantity.toString(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { onUpdateQty(item.cartId, 1) },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase quantity", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    IconButton(
                        onClick = { onRemoveItem(item.cartId) },
                        enabled = !isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Remove ${item.name}",
                            tint = ext.crimson
                        )
                    }
                }
            }
        }
    }
}
