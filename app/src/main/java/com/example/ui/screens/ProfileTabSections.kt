package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Address
import com.example.model.LocalPreferences
import com.example.model.Order
import com.example.model.Product
import com.example.model.SupportTicket
import com.example.model.User
import com.example.model.formatPrice
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun ProfileOverviewSection(
    user: User,
    uiState: MelvoreUiState,
    onSaveProfile: (name: String, phone: String, alternatePhone: String) -> Unit
) {
    val ext = LocalMelvoreColors.current
    var name by remember(user.id, user.name) { mutableStateOf(user.name) }
    var phone by remember(user.id, user.phone) { mutableStateOf(user.phone) }
    var altPhone by remember(user.id, user.alternatePhone) { mutableStateOf(user.alternatePhone) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                "Orders" to uiState.orders.size.toString(),
                "Wishlist" to uiState.wishlistIds.size.toString(),
                "Addresses" to uiState.addresses.size.toString()
            ).forEach { (label, count) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(count, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
                        Text(label, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                    }
                }
            }
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
                    Text("Personal Details", style = MaterialTheme.typography.titleMedium)
                    if (user.emailVerified) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Verified, contentDescription = null, tint = ext.emerald, modifier = Modifier.size(16.dp))
                            Text("Verified", style = MaterialTheme.typography.labelSmall, color = ext.emerald)
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Primary Mobile") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = altPhone,
                    onValueChange = { altPhone = it },
                    label = { Text("Alternate Mobile (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = !uiState.isProfileBusy,
                    onClick = { onSaveProfile(name, phone, altPhone) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_save_button")
                ) {
                    Text(if (uiState.isProfileBusy) "Saving…" else "Save Changes")
                }
            }
        }
    }
}

@Composable
fun ProfileOrdersSection(
    orders: List<Order>,
    currency: String,
    isBusy: Boolean,
    onTrackOrder: (String) -> Unit,
    onCancelOrder: (Int) -> Unit,
    onRetryRazorpay: (Int) -> Unit,
    onBrowseShop: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    var cancelTarget by remember { mutableStateOf<Order?>(null) }

    if (orders.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ext.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = ext.muted, modifier = Modifier.size(32.dp))
                Text("No orders yet", style = MaterialTheme.typography.titleMedium)
                Text("Explore Melvore jackets and second-skin loungewear.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                Button(onClick = onBrowseShop, shape = RoundedCornerShape(12.dp)) {
                    Text("Browse Shop")
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            orders.forEach { order ->
                val statusColor = when (order.status.lowercase()) {
                    "delivered" -> ext.emerald
                    "cancelled" -> ext.crimson
                    "pending" -> ext.amber
                    else -> ext.accentNavy
                }
                val isUnpaidOnline = !order.paymentMethod.equals("cod", ignoreCase = true) &&
                    order.paymentStatus.equals("pending", ignoreCase = true) &&
                    !order.status.equals("cancelled", ignoreCase = true)

                Surface(
                    shape = RoundedCornerShape(18.dp),
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
                                Text("#${order.orderNumber}", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                                Text(
                                    "${order.createdAt.take(10)} · ${order.paymentMethod.uppercase()} (${order.paymentStatus})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.muted
                                )
                            }
                            Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.14f)) {
                                Text(
                                    text = order.status.replace("_", " ").uppercase(),
                                    color = statusColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        order.items.take(2).forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MelvoreNetworkImage(
                                    url = item.imageUrl,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val meta = listOfNotNull(
                                        item.color.takeIf { it.isNotBlank() },
                                        item.size.takeIf { it.isNotBlank() },
                                        "× ${item.quantity}"
                                    ).joinToString(" · ")
                                    Text(meta, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                }
                                Text(
                                    formatPrice(item.totalPrice, currency),
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = ext.borderSubtle)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total: ${formatPrice(order.total, currency)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isUnpaidOnline) {
                                    Button(
                                        enabled = !isBusy,
                                        onClick = { onRetryRazorpay(order.id) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Outlined.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Pay Now")
                                    }
                                }
                                if (order.status.lowercase() !in setOf("delivered", "cancelled", "shipped", "out_for_delivery")) {
                                    OutlinedButton(
                                        enabled = !isBusy,
                                        onClick = { cancelTarget = order },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Cancel", color = ext.crimson)
                                    }
                                }
                                Button(
                                    onClick = { onTrackOrder(order.orderNumber) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Outlined.LocalShipping, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Track")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val target = cancelTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel Order #${target.orderNumber}?") },
            text = { Text("This cancels your order and triggers refund processing if already paid.") },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelOrder(target.id)
                        cancelTarget = null
                    }
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text("Keep Order")
                }
            }
        )
    }
}

@Composable
fun ProfileWishlistSection(
    products: List<Product>,
    currency: String,
    onOpenProduct: (Product) -> Unit,
    onMoveToCart: (Product) -> Unit,
    onRemoveWishlist: (Int) -> Unit,
    onBrowseShop: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    if (products.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ext.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Your wishlist is empty", style = MaterialTheme.typography.titleMedium)
                Text("Tap the heart on any product to save it here.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                Button(onClick = onBrowseShop, shape = RoundedCornerShape(12.dp)) {
                    Text("Explore Catalog")
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            products.forEach { product ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle),
                    modifier = Modifier.clickable { onOpenProduct(product) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MelvoreNetworkImage(
                            url = product.imageUrl,
                            contentDescription = product.name,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(product.category, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                            Text(
                                formatPrice(product.effectivePrice, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onMoveToCart(product) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Move to Cart", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { onRemoveWishlist(product.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Remove", style = MaterialTheme.typography.labelMedium, color = ext.crimson)
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
fun ProfileAddressesSection(
    addresses: List<Address>,
    isBusy: Boolean,
    onAddAddress: (Address, onSuccess: () -> Unit) -> Unit,
    onSetDefault: (Int) -> Unit,
    onDeleteAddress: (Int) -> Unit
) {
    val ext = LocalMelvoreColors.current
    var showAddForm by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("home") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var addressLine2 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { showAddForm = !showAddForm },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("profile_add_address_toggle")
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (showAddForm) "Close Form" else "Add New Address")
        }

        if (showAddForm) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("home" to "Home", "work" to "Work", "other" to "Other").forEach { (key, title) ->
                            val selected = label == key
                            Surface(
                                onClick = { label = key },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Recipient Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("House / Street / Locality") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = addressLine2, onValueChange = { addressLine2 = it }, label = { Text("Landmark / Apartment (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = pincode, onValueChange = { pincode = it }, label = { Text("PIN Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        enabled = !isBusy,
                        onClick = {
                            onAddAddress(
                                Address(
                                    id = 0,
                                    label = label,
                                    fullName = fullName,
                                    phone = phone,
                                    street = street,
                                    addressLine2 = addressLine2,
                                    city = city,
                                    state = state,
                                    pincode = pincode,
                                    isDefault = addresses.isEmpty()
                                )
                            ) {
                                showAddForm = false
                                fullName = ""
                                phone = ""
                                street = ""
                                addressLine2 = ""
                                city = ""
                                state = ""
                                pincode = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isBusy) "Saving…" else "Save Address")
                    }
                }
            }
        }

        addresses.forEach { addr ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (addr.isDefault) MaterialTheme.colorScheme.primary else ext.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("${addr.label.replaceFirstChar { it.uppercase() }} · ${addr.fullName}", style = MaterialTheme.typography.titleMedium)
                        }
                        if (addr.isDefault) {
                            Surface(shape = CircleShape, color = ext.emerald.copy(alpha = 0.14f)) {
                                Text(
                                    "DEFAULT",
                                    color = ext.emerald,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "${addr.street}${if (addr.addressLine2.isNotBlank()) ", ${addr.addressLine2}" else ""}, ${addr.city}, ${addr.state} ${addr.pincode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted
                    )
                    Text(addr.phone, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!addr.isDefault) {
                            TextButton(enabled = !isBusy, onClick = { onSetDefault(addr.id) }) {
                                Text("Make Default")
                            }
                        }
                        IconButton(enabled = !isBusy, onClick = { onDeleteAddress(addr.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete address", tint = ext.crimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTicketsSection(
    tickets: List<SupportTicket>,
    isBusy: Boolean,
    onCreateTicket: (
        subject: String,
        orderNo: String?,
        category: String,
        priority: String,
        message: String,
        onSuccess: () -> Unit
    ) -> Unit,
    onReplyTicket: (ticketId: Int, message: String, onDone: () -> Unit) -> Unit
) {
    val ext = LocalMelvoreColors.current
    var showNewTicket by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var orderNumber by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("general") }
    var priority by remember { mutableStateOf("medium") }
    var message by remember { mutableStateOf("") }
    var expandedTicketId by remember { mutableStateOf(tickets.firstOrNull()?.id) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { showNewTicket = !showNewTicket },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (showNewTicket) "Close Ticket Form" else "New Support Ticket")
        }

        if (showNewTicket) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = orderNumber, onValueChange = { orderNumber = it }, label = { Text("Order Number (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Category", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("general", "order", "return", "payment", "product").forEach { cat ->
                            val selected = category == cat
                            Surface(
                                onClick = { category = cat },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    cat.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Text("Priority", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("low" to "Low", "medium" to "Normal", "high" to "High", "urgent" to "Urgent").forEach { (key, label) ->
                            val selected = priority == key
                            Surface(
                                onClick = { priority = key },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("How can we help?") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = !isBusy,
                        onClick = {
                            onCreateTicket(subject, orderNumber.ifBlank { null }, category, priority, message) {
                                showNewTicket = false
                                subject = ""
                                orderNumber = ""
                                message = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isBusy) "Submitting…" else "Submit Ticket")
                    }
                }
            }
        }

        tickets.forEach { ticket ->
            val isExpanded = expandedTicketId == ticket.id
            var replyText by remember(ticket.id) { mutableStateOf("") }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedTicketId = if (isExpanded) null else ticket.id },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${ticket.ticketNumber} · ${ticket.subject}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${ticket.category.uppercase()} · ${ticket.priority.uppercase()} · ${ticket.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.muted
                            )
                        }
                        Text(if (isExpanded) "Hide" else "View", style = MaterialTheme.typography.labelMedium, color = ext.accentNavy)
                    }

                    if (isExpanded) {
                        HorizontalDivider(color = ext.borderSubtle)
                        if (ticket.message.isNotBlank()) {
                            TicketBubble(sender = "You", text = ticket.message, isUser = true)
                        }
                        ticket.replies.forEach { msg ->
                            val isUser = !msg.sender.equals("admin", ignoreCase = true) &&
                                !msg.sender.equals("support", ignoreCase = true)
                            TicketBubble(
                                sender = if (isUser) "You" else "Melvore Concierge",
                                text = msg.text,
                                isUser = isUser
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = { Text("Write a reply…") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (replyText.isNotBlank()) {
                                        onReplyTicket(ticket.id, replyText) {
                                            replyText = ""
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketBubble(sender: String, text: String, isUser: Boolean) {
    val ext = LocalMelvoreColors.current
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = sender,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else ext.muted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ProfileSettingsSection(
    user: User,
    themeMode: String,
    preferences: LocalPreferences,
    isAuthBusy: Boolean,
    onSetTheme: (String) -> Unit,
    onUpdatePreferences: ((LocalPreferences) -> LocalPreferences) -> Unit,
    onRequestPasswordReset: (email: String, onSent: () -> Unit) -> Unit,
    onResetPassword: (email: String, otp: String, newPassword: String, onSuccess: () -> Unit) -> Unit,
    onOpenHelpCenter: () -> Unit,
    discoveryState: com.example.ui.viewmodel.DiscoveryUiState = com.example.ui.viewmodel.DiscoveryUiState(),
    onOpenDesignAudit: () -> Unit = {},
    onOpenConnectionDiagnostics: () -> Unit = {},
    onRefreshDesignManifest: () -> Unit = {}
) {
    val ext = LocalMelvoreColors.current
    val surfaces = com.example.ui.theme.LocalMelvoreSurfaces.current
    var resetOtpSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var showLegalDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Appearance (Light / Dark / System) & Currency
        ProfileAppearanceCard(
            themeMode = themeMode,
            onSetTheme = onSetTheme
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = surfaces.surface,
            border = BorderStroke(1.dp, surfaces.border)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Currency", style = MaterialTheme.typography.titleMedium, color = surfaces.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("₹" to "₹ INR", "$" to "$ USD").forEach { (symbol, label) ->
                        val selected = preferences.currencyOverride == symbol
                        Surface(
                            onClick = { onUpdatePreferences { it.copy(currencyOverride = symbol) } },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) surfaces.accent else surfaces.surfaceElevated,
                            border = BorderStroke(1.dp, if (selected) surfaces.accent else surfaces.border),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) surfaces.onAccent else surfaces.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Local Device Preferences
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ext.borderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Notification Preferences", style = MaterialTheme.typography.titleMedium)
                SettingToggleRow("Order Status Updates", preferences.orderNotifications) { v ->
                    onUpdatePreferences { it.copy(orderNotifications = v) }
                }
                SettingToggleRow("Drop & Promo Alerts", preferences.promoNotifications) { v ->
                    onUpdatePreferences { it.copy(promoNotifications = v) }
                }
                SettingToggleRow("Price Drop Alerts", preferences.priceDropAlerts) { v ->
                    onUpdatePreferences { it.copy(priceDropAlerts = v) }
                }
            }
        }

        // Fashion Worlds Sensory & Motion Controls
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ext.borderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Fashion Worlds Audio & Motion", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Soundscapes start muted by default. Ambient audio and micro-SFX can be controlled independently.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
                SettingToggleRow("Ambient World Soundscape", preferences.ambientAudioEnabled) { v ->
                    onUpdatePreferences { it.copy(ambientAudioEnabled = v, audioConsentAnswered = true) }
                }
                SettingToggleRow("Subtle UI Micro-SFX", preferences.uiSfxEnabled) { v ->
                    onUpdatePreferences { it.copy(uiSfxEnabled = v, audioConsentAnswered = true) }
                }
                SettingToggleRow("Reduced Motion & FX", preferences.reducedMotion) { v ->
                    onUpdatePreferences { it.copy(reducedMotion = v) }
                }
            }
        }

        // Password Reset via Server Email OTP
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ext.borderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Reset Account Password", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Verify via a 6-digit OTP sent to ${user.email}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
                if (!resetOtpSent) {
                    Button(
                        enabled = !isAuthBusy,
                        onClick = {
                            onRequestPasswordReset(user.email) {
                                resetOtpSent = true
                            }
                        },
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAuthBusy) "Sending OTP…" else "Send Reset OTP")
                    }
                } else {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        label = { Text("Email Reset OTP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPw,
                        onValueChange = { newPw = it },
                        label = { Text("New Password (min 6 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { resetOtpSent = false },
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            enabled = !isAuthBusy,
                            onClick = {
                                onResetPassword(user.email, otpCode, newPw) {
                                    resetOtpSent = false
                                    otpCode = ""
                                    newPw = ""
                                }
                            },
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Text("Confirm Reset")
                        }
                    }
                }
            }
        }

        // Concierge Help & Legal Policies
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = surfaces.surface,
            border = BorderStroke(1.dp, surfaces.border)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Help & Policies", style = MaterialTheme.typography.titleMedium, color = surfaces.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenHelpCenter,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("Help Center")
                    }
                    OutlinedButton(
                        onClick = { showLegalDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("Legal & Terms")
                    }
                }
            }
        }

        // About & DB Design System Diagnostics
        ProfileAboutDesignSystemCard(
            discoveryState = discoveryState,
            onRefreshDesignManifest = onRefreshDesignManifest,
            onOpenDesignAudit = onOpenDesignAudit,
            onOpenConnectionDiagnostics = onOpenConnectionDiagnostics
        )
    }

    if (showLegalDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            confirmButton = {
                Button(onClick = { showLegalDialog = false }, shape = RoundedCornerShape(2.dp)) {
                    Text("Close")
                }
            },
            title = { Text("Melvore Legal & Policies") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "• Returns & Exchanges: 7-day reverse pickup window on unworn items with original tags intact.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Payments: All prepaid transactions are processed via Razorpay encrypted checkout. COD is subject to pincode serviceability.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Privacy & Data: Pincode selection is stored locally without requiring GPS location permission. Photos attached to verified reviews or support requests use Android's zero-permission Photo Picker.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
    }
}

@Composable
fun ProfileAppearanceCard(
    themeMode: String,
    onSetTheme: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = com.example.ui.theme.LocalMelvoreSurfaces.current
    val normalized = themeMode.lowercase().ifBlank { "light" }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = surfaces.surface,
        border = BorderStroke(1.dp, surfaces.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, color = surfaces.onSurface)
            Text(
                "Light is the default Melvore storefront mode. Melvore Luxe always presents its dedicated dark showroom.",
                style = MaterialTheme.typography.bodySmall,
                color = surfaces.onSurfaceMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (key, label) ->
                    val selected = normalized == key
                    Surface(
                        onClick = { onSetTheme(key) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) surfaces.accent else surfaces.surfaceElevated,
                        border = BorderStroke(1.dp, if (selected) surfaces.accent else surfaces.border),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("appearance_mode_$key")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) surfaces.onAccent else surfaces.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileAboutDesignSystemCard(
    discoveryState: com.example.ui.viewmodel.DiscoveryUiState,
    onRefreshDesignManifest: () -> Unit,
    onOpenDesignAudit: () -> Unit,
    onOpenConnectionDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val surfaces = com.example.ui.theme.LocalMelvoreSurfaces.current
    val version = discoveryState.manifestVersion.ifBlank { discoveryState.activeManifest.designVersion }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = surfaces.surface,
        border = BorderStroke(1.dp, surfaces.border),
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_about_design_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("About & Design System", style = MaterialTheme.typography.titleMedium, color = surfaces.onSurface)
            Text(
                text = "Source: ${discoveryState.manifestSource.wireName.uppercase()} · Version: $version · ETag: ${discoveryState.manifestEtag.ifBlank { "none" }}",
                style = MaterialTheme.typography.bodySmall,
                color = surfaces.onSurfaceMuted
            )
            if (discoveryState.manifestFallbackReason != null) {
                Text(
                    text = "Fallback: ${discoveryState.manifestFallbackReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaces.sale
                )
            }
            if (discoveryState.manifestWarnings.isNotEmpty()) {
                Text(
                    text = "Warnings: ${discoveryState.manifestWarnings.size} section fallback(s) active",
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaces.sale
                )
            }
            Button(
                onClick = onOpenConnectionDiagnostics,
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = surfaces.accent,
                    contentColor = surfaces.onAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("profile_open_connection_diagnostics_btn")
            ) {
                Text("Connection Diagnostics")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRefreshDesignManifest,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("profile_refresh_design_btn")
                ) {
                    Text("Sync Design")
                }
                if (com.example.BuildConfig.DEBUG) {
                    OutlinedButton(
                        onClick = onOpenDesignAudit,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("profile_open_design_audit_btn")
                    ) {
                        Text("Design Audit")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
