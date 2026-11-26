package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.unit.dp
import com.example.data.CouponEvalResult
import com.example.model.Address
import com.example.model.Order
import com.example.model.formatPrice
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.viewmodel.MelvoreUiState
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CheckoutScreen(
    uiState: MelvoreUiState,
    onBackToShop: () -> Unit,
    onNavigateLogin: () -> Unit,
    onValidateCoupon: (code: String, paymentMethod: String, onResult: (CouponEvalResult) -> Unit) -> Unit,
    onPlaceOrder: (
        existingAddressId: Int,
        newAddressDraft: Address?,
        paymentMethod: String,
        couponCode: String,
        notes: String
    ) -> Unit,
    onRetryRazorpay: (orderId: Int) -> Unit,
    onCancelUnpaidOrder: (orderId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val settings = uiState.design.siteSettings
    val defaultAddr = uiState.addresses.find { it.isDefault } ?: uiState.addresses.firstOrNull()

    var selectedAddressId by remember(defaultAddr?.id, uiState.addresses.size) {
        mutableStateOf(defaultAddr?.id ?: -1)
    }
    var fullName by remember(uiState.user?.name) { mutableStateOf(uiState.user?.name ?: "") }
    var phone by remember(uiState.user?.phone) { mutableStateOf(uiState.user?.phone ?: "") }
    var street by remember { mutableStateOf("") }
    var addressLine2 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var orderNotes by remember { mutableStateOf("") }

    var paymentMethod by remember(settings.enableOnlinePayment, settings.enableCod) {
        mutableStateOf(
            when {
                settings.enableOnlinePayment -> "online"
                settings.enableCod -> "cod"
                else -> "online"
            }
        )
    }
    var couponInput by remember { mutableStateOf(uiState.cartSummary.couponCode) }
    var appliedCoupon by remember { mutableStateOf(uiState.cartSummary.couponCode) }
    var couponDiscount by remember { mutableDoubleStateOf(uiState.cartSummary.discountAmount) }
    var couponStatusMsg by remember { mutableStateOf<String?>(null) }
    var isValidatingCoupon by remember { mutableStateOf(false) }

    // Keep totals synced with backend CartSummary / coupon evaluation
    LaunchedEffect(paymentMethod) {
        if (uiState.cartItems.isNotEmpty()) {
            onValidateCoupon(appliedCoupon, paymentMethod) { res ->
                if (!res.valid && appliedCoupon.isNotBlank()) {
                    appliedCoupon = ""
                    couponDiscount = 0.0
                    couponStatusMsg = res.message
                } else {
                    couponDiscount = res.discountAmount
                }
            }
        }
    }

    val summary = uiState.cartSummary
    val subtotal = summary.subtotal.takeIf { it > 0.0 } ?: uiState.cartSubtotal
    val shippingFee = summary.shippingCharge
    val taxAmount = summary.taxAmount
    val codFee = if (paymentMethod == "cod") summary.codFee else 0.0
    val discount = if (appliedCoupon.isNotEmpty()) max(couponDiscount, summary.discountAmount) else 0.0
    val finalTotal = if (summary.totalAmount > 0.0) {
        summary.totalAmount
    } else {
        max(0.0, ((subtotal + shippingFee + taxAmount + codFee - discount) * 100.0).roundToInt() / 100.0)
    }

    val pendingRecovery = uiState.pendingPaymentOrder
    if (uiState.cartItems.isEmpty() && pendingRecovery == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Your cart is empty", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Add items to your cart before proceeding to checkout.",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBackToShop, shape = RoundedCornerShape(14.dp)) {
                Text("Browse Products")
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onBackToShop, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Continue Shopping")
                }
                Text("Checkout", style = MaterialTheme.typography.headlineMedium)
            }
        }

        // Pending Razorpay Payment Recovery Banner
        if (pendingRecovery != null) {
            item {
                PendingRazorpayRecoveryCard(
                    order = pendingRecovery,
                    currency = uiState.currency,
                    isBusy = uiState.isCheckoutBusy,
                    onRetry = { onRetryRazorpay(pendingRecovery.id) },
                    onCancel = { onCancelUnpaidOrder(pendingRecovery.id) }
                )
            }
        }

        // Auth Required Card (orders/create.php requires authenticated customer token)
        if (uiState.user == null && uiState.cartItems.isNotEmpty()) {
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
                        Text(
                            "Sign in to Complete Checkout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your Melvore bag is linked to this device and will automatically merge into your account when you sign in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.muted
                        )
                        Button(
                            onClick = onNavigateLogin,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("checkout_signin_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sign In / Create Account")
                        }
                    }
                }
            }
        }

        if (uiState.cartItems.isNotEmpty()) {
            // Delivery Address
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Delivery Address", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        uiState.addresses.forEach { addr ->
                            val selected = selectedAddressId == addr.id
                            Surface(
                                onClick = { selectedAddressId = addr.id },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (selected) 1.5.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.onBackground else ext.borderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(addr.label.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        if (selected) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = ext.emerald, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Text(addr.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${addr.street}${if (addr.addressLine2.isNotBlank()) ", ${addr.addressLine2}" else ""}, ${addr.city}, ${addr.state} — ${addr.pincode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ext.muted
                                    )
                                    Text(addr.phone, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = ext.muted)
                                }
                            }
                        }

                        if (uiState.addresses.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { selectedAddressId = -1 },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (selectedAddressId == -1) "Using New Address Form" else "+ Ship to another address")
                            }
                        }

                        if (selectedAddressId == -1 || uiState.addresses.isEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Full Name") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("Mobile Number") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = street,
                                    onValueChange = { street = it },
                                    label = { Text("House / Street / Locality") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = addressLine2,
                                    onValueChange = { addressLine2 = it },
                                    label = { Text("Landmark / Apartment (optional)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = city,
                                        onValueChange = { city = it },
                                        label = { Text("City") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = stateName,
                                        onValueChange = { stateName = it },
                                        label = { Text("State") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = pincode,
                                    onValueChange = { pincode = it },
                                    label = { Text("PIN Code (e.g. 110033)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Payment Method (Only Razorpay Online & Cash on Delivery)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Wallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        val availableMethods = buildList {
                            if (settings.enableOnlinePayment) {
                                add(Triple("online", "Razorpay Secure Online", "UPI · Cards · NetBanking · Wallets"))
                            }
                            if (settings.enableCod) {
                                add(Triple("cod", "Cash on Delivery", "Pay in cash or UPI upon delivery"))
                            }
                        }.ifEmpty {
                            listOf(
                                Triple("online", "Razorpay Secure Online", "UPI · Cards · NetBanking · Wallets"),
                                Triple("cod", "Cash on Delivery", "Pay upon delivery")
                            )
                        }

                        availableMethods.forEach { (id, label, sub) ->
                            val active = paymentMethod == id
                            Surface(
                                onClick = { paymentMethod = id },
                                shape = RoundedCornerShape(14.dp),
                                color = if (active) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (active) 1.5.dp else 1.dp,
                                    if (active) MaterialTheme.colorScheme.onBackground else ext.borderSubtle
                                ),
                                modifier = Modifier.testTag("payment_method_$id")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Text(sub, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                    }
                                    Icon(
                                        if (id == "online") Icons.Outlined.CreditCard else Icons.Outlined.Payment,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Order Summary & Coupon
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Order Summary", style = MaterialTheme.typography.titleLarge)

                        uiState.cartItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MelvoreNetworkImage(
                                    url = item.imageUrl,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                    val meta = listOfNotNull(
                                        item.color.takeIf { it.isNotBlank() },
                                        item.size.takeIf { it.isNotBlank() }?.let { "Size $it" },
                                        "Qty ${item.quantity}"
                                    ).joinToString(" · ")
                                    Text(meta, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                }
                                Text(
                                    formatPrice(item.lineTotal, uiState.currency),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        if (uiState.activeCoupons.isNotEmpty()) {
                            Text("Available Offers", style = MaterialTheme.typography.labelMedium, color = ext.muted)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.activeCoupons.forEach { offer ->
                                    Surface(
                                        onClick = { couponInput = offer.code },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, ext.borderSubtle)
                                    ) {
                                        Text(
                                            text = offer.code,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = couponInput,
                                onValueChange = { couponInput = it.uppercase() },
                                placeholder = { Text("Enter promo code") },
                                leadingIcon = { Icon(Icons.Outlined.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("checkout_coupon_input")
                            )
                            OutlinedButton(
                                enabled = !isValidatingCoupon && !uiState.isCheckoutBusy,
                                onClick = {
                                    isValidatingCoupon = true
                                    couponStatusMsg = null
                                    onValidateCoupon(couponInput.trim(), paymentMethod) { res ->
                                        isValidatingCoupon = false
                                        if (res.valid && !res.code.isNullOrBlank()) {
                                            appliedCoupon = res.code
                                            couponDiscount = res.discountAmount
                                            couponStatusMsg = res.message
                                        } else {
                                            appliedCoupon = ""
                                            couponDiscount = 0.0
                                            couponStatusMsg = res.message
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("checkout_apply_coupon_button")
                            ) {
                                Text(if (isValidatingCoupon) "..." else "Apply")
                            }
                        }

                        couponStatusMsg?.takeIf { it.isNotBlank() }?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (appliedCoupon.isNotBlank()) ext.emerald else ext.crimson
                            )
                        }

                        OutlinedTextField(
                            value = orderNotes,
                            onValueChange = { orderNotes = it },
                            label = { Text("Delivery instructions (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = ext.borderSubtle)
                        SummaryRow("Subtotal", formatPrice(subtotal, uiState.currency))
                        SummaryRow(
                            "Shipping",
                            if (shippingFee <= 0.0) "Free" else formatPrice(shippingFee, uiState.currency)
                        )
                        if (taxAmount > 0.0) {
                            SummaryRow("Tax", formatPrice(taxAmount, uiState.currency))
                        }
                        if (codFee > 0.0) {
                            SummaryRow("COD Handling Fee", formatPrice(codFee, uiState.currency))
                        }
                        if (discount > 0.0) {
                            SummaryRow(
                                "Discount${if (appliedCoupon.isNotBlank()) " ($appliedCoupon)" else ""}",
                                "-${formatPrice(discount, uiState.currency)}",
                                valueColor = ext.emerald
                            )
                        }
                        HorizontalDivider(color = ext.borderSubtle)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                formatPrice(finalTotal, uiState.currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            enabled = !uiState.isCheckoutBusy,
                            onClick = {
                                if (uiState.user == null) {
                                    onNavigateLogin()
                                    return@Button
                                }
                                val draft = if (selectedAddressId <= 0 || uiState.addresses.isEmpty()) {
                                    Address(
                                        id = 0,
                                        label = "home",
                                        fullName = fullName,
                                        phone = phone,
                                        street = street,
                                        addressLine2 = addressLine2,
                                        city = city,
                                        state = stateName,
                                        pincode = pincode,
                                        isDefault = uiState.addresses.isEmpty()
                                    )
                                } else null
                                onPlaceOrder(
                                    if (draft == null) selectedAddressId else 0,
                                    draft,
                                    paymentMethod,
                                    appliedCoupon,
                                    orderNotes
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("place_order_button")
                        ) {
                            if (uiState.isCheckoutBusy) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(Icons.Outlined.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (uiState.user == null) "Sign In to Place Order"
                                    else if (paymentMethod == "online") "Pay ${formatPrice(finalTotal, uiState.currency)} with Razorpay"
                                    else "Place COD Order · ${formatPrice(finalTotal, uiState.currency)}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingRazorpayRecoveryCard(
    order: Order,
    currency: String,
    isBusy: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ext.amber.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, ext.amber.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ext.amber)
                Column {
                    Text(
                        "Payment Recovery · #${order.orderNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Payable: ${formatPrice(order.total, currency)} · Payment ${order.paymentStatus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted
                    )
                }
            }
            Text(
                "Complete your Razorpay payment below to confirm dispatch, or cancel the unpaid order to release reserved stock.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    enabled = !isBusy,
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel Order", color = ext.crimson)
                }
                Button(
                    enabled = !isBusy,
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("checkout_retry_razorpay_button")
                ) {
                    Text("Complete Payment")
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    val ext = LocalMelvoreColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
