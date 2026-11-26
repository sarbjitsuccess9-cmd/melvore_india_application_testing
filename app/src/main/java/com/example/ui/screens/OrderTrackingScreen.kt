package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.formatPrice
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun OrderTrackingScreen(
    initialOrderNumber: String,
    uiState: MelvoreUiState,
    onTrackOrder: (orderNumber: String, pincode: String?, mobile: String?) -> Unit,
    onRetryRazorpay: (orderId: Int) -> Unit,
    onCancelUnpaidOrder: (orderId: Int) -> Unit,
    onNavigateContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val settings = uiState.design.siteSettings
    val defaultOrderNo = initialOrderNumber.ifBlank {
        uiState.orders.firstOrNull()?.orderNumber ?: ""
    }

    var orderIdInput by remember(defaultOrderNo) { mutableStateOf(defaultOrderNo) }
    var verificationInput by remember { mutableStateOf("") }

    fun submitTrackLookup(orderNo: String) {
        val cleanVerify = verificationInput.trim()
        val isPincode = cleanVerify.length == 6 && cleanVerify.all { it.isDigit() }
        onTrackOrder(
            orderNo,
            if (isPincode) cleanVerify else null,
            if (!isPincode && cleanVerify.isNotEmpty()) cleanVerify else null
        )
    }

    LaunchedEffect(defaultOrderNo) {
        if (defaultOrderNo.isNotBlank()) {
            onTrackOrder(defaultOrderNo, null, null)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Track Order", style = MaterialTheme.typography.headlineMedium)
                Text(
                    settings.orderTrackingHeroText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
            }
        }

        // Lookup Form
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = orderIdInput,
                        onValueChange = { orderIdInput = it },
                        label = { Text("Order Number (e.g. MLV-...)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("track_order_input")
                    )
                    OutlinedTextField(
                        value = verificationInput,
                        onValueChange = { verificationInput = it },
                        label = {
                            Text(
                                if (uiState.user == null) "Delivery PIN Code or Mobile (for verification)"
                                else "PIN Code or Mobile (optional)"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = !uiState.isTrackingBusy,
                        onClick = { submitTrackLookup(orderIdInput) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("track_submit_button")
                    ) {
                        if (uiState.isTrackingBusy) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Track Shipment")
                        }
                    }

                    if (uiState.orders.isNotEmpty()) {
                        Text("Your Recent Orders", style = MaterialTheme.typography.labelMedium, color = ext.muted)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.orders.take(5).forEach { ord ->
                                val selected = uiState.trackedOrder?.orderNumber == ord.orderNumber
                                Surface(
                                    onClick = {
                                        orderIdInput = ord.orderNumber
                                        onTrackOrder(ord.orderNumber, null, null)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "#${ord.orderNumber}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isTrackingBusy && uiState.trackedOrder == null) {
            item {
                MelvoreLoadingState(message = "Fetching shipment status…")
            }
        } else if (uiState.trackingError != null && uiState.trackedOrder == null) {
            item {
                MelvoreErrorState(
                    message = uiState.trackingError,
                    onRetry = { submitTrackLookup(orderIdInput) }
                )
            }
        }

        val order = uiState.trackedOrder
        if (order != null) {
            val isUnpaidOnline = !order.paymentMethod.equals("cod", ignoreCase = true) &&
                order.paymentStatus.equals("pending", ignoreCase = true) &&
                !order.status.equals("cancelled", ignoreCase = true)

            if (isUnpaidOnline) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ext.amber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, ext.amber.copy(alpha = 0.4f))
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
                                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ext.amber)
                                Text(
                                    "Online Payment Pending",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ext.amber
                                )
                            }
                            Text(
                                "This order awaits Razorpay payment verification before dispatch.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    enabled = !uiState.isCheckoutBusy,
                                    onClick = { onCancelUnpaidOrder(order.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel Order", color = ext.crimson)
                                }
                                Button(
                                    enabled = !uiState.isCheckoutBusy,
                                    onClick = { onRetryRazorpay(order.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("track_complete_payment_button")
                                ) {
                                    Icon(Icons.Outlined.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Pay Now")
                                }
                            }
                        }
                    }
                }
            }

            // Order Summary Card
            item {
                val statusColor = when (order.status.lowercase()) {
                    "delivered" -> ext.emerald
                    "cancelled" -> ext.crimson
                    "pending" -> ext.amber
                    else -> ext.accentNavy
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "#${order.orderNumber}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (order.createdAt.isNotBlank()) {
                                    Text(
                                        "Placed ${order.createdAt.take(10)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ext.muted
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = statusColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = order.status.replace("_", " ").uppercase(),
                                    color = statusColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = ext.borderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Estimated Delivery", style = MaterialTheme.typography.labelMedium, color = ext.muted)
                                Text(
                                    order.estimatedDelivery?.takeIf { it.isNotBlank() } ?: "Will update upon dispatch",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Payment", style = MaterialTheme.typography.labelMedium, color = ext.muted)
                                Text(
                                    "${order.paymentMethod.uppercase()} · ${order.paymentStatus}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        val showCourier = settings.showCourierNameToCustomer && !order.trackingCourier.isNullOrBlank()
                        val showTrackingId = settings.showTrackingIdToCustomer && !order.trackingNumber.isNullOrBlank()
                        if (showCourier || showTrackingId) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = ext.accentNavy)
                                    Column {
                                        if (showCourier) {
                                            Text(order.trackingCourier!!, style = MaterialTheme.typography.labelLarge)
                                        }
                                        if (showTrackingId) {
                                            Text(
                                                "AWB: ${order.trackingNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = ext.muted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Timeline
            if (order.timeline.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ext.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "Shipment Journey",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            val designKit = com.example.ui.theme.LocalDesignKit.current
                            val worldKit = com.example.ui.theme.LocalWorld.current
                            val orderStepsUrl = remember(designKit, worldKit) {
                                designKit.url(worldKit?.widgetPath("order_steps"))
                            }
                            if (!orderStepsUrl.isNullOrBlank()) {
                                com.example.ui.design.AnimatedSvg(
                                    url = orderStepsUrl,
                                    contentDescription = "Order tracking steps animation",
                                    reducedMotion = uiState.preferences.reducedMotion,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .padding(bottom = 10.dp)
                                )
                            }
                            order.timeline.forEachIndexed { idx, step ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (step.completed) ext.emerald else MaterialTheme.colorScheme.surfaceVariant,
                                            border = if (step.completed) null else BorderStroke(1.dp, ext.borderSubtle),
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (step.completed) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = com.example.ui.theme.ensureContrast(
                                                            com.example.ui.theme.LocalMelvoreSurfaces.current.onAccent,
                                                            ext.emerald
                                                        ),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (idx < order.timeline.lastIndex) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(38.dp)
                                                    .background(if (step.completed) ext.emerald.copy(alpha = 0.5f) else ext.borderSubtle)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.padding(bottom = 14.dp)) {
                                        Text(
                                            step.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (step.completed) MaterialTheme.colorScheme.onSurface else ext.muted
                                        )
                                        if (step.description.isNotBlank()) {
                                            Text(step.description, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                        }
                                        if (step.timestamp.isNotBlank()) {
                                            Text(
                                                step.timestamp,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = ext.muted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ordered Items & Shipping Address
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Items in Package", style = MaterialTheme.typography.titleMedium)
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MelvoreNetworkImage(
                                    url = item.imageUrl,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val meta = listOfNotNull(
                                        item.color.takeIf { it.isNotBlank() },
                                        item.size.takeIf { it.isNotBlank() }?.let { "Size $it" },
                                        "Qty ${item.quantity}"
                                    ).joinToString(" · ")
                                    Text(meta, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                }
                                Text(
                                    formatPrice(item.totalPrice, uiState.currency),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        order.shippingAddress?.let { addr ->
                            HorizontalDivider(color = ext.borderSubtle)
                            Text("Shipping To", style = MaterialTheme.typography.labelMedium, color = ext.muted)
                            Text(
                                buildString {
                                    append(addr.fullName)
                                    if (addr.phone.isNotBlank()) append(" · ${addr.phone}")
                                    append("\n")
                                    append(listOf(addr.street, addr.city, addr.state, addr.pincode).filter { it.isNotBlank() }.joinToString(", "))
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        HorizontalDivider(color = ext.borderSubtle)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Order Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                formatPrice(order.total, uiState.currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Concierge CTA
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.HeadsetMic, contentDescription = null, tint = ext.accentNavy)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Need Assistance?", style = MaterialTheme.typography.titleMedium)
                        Text(
                            settings.trackingPageSupportText.ifBlank { "Reach our New Delhi concierge for delivery support." },
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.muted
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateContact,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("track_contact_button")
                    ) {
                        Text("Support")
                    }
                }
            }
        }
    }
}
