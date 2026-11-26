package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Address
import com.example.model.LocalPreferences
import com.example.model.Product
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun ProfileScreen(
    initialTab: String,
    uiState: MelvoreUiState,
    onNavigateAuth: (String) -> Unit,
    onNavigateShop: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onSaveProfile: (name: String, phone: String, alternatePhone: String) -> Unit,
    onTrackOrder: (String) -> Unit,
    onCancelOrder: (Int) -> Unit,
    onRetryRazorpay: (Int) -> Unit,
    onAddAddress: (Address, onSuccess: () -> Unit) -> Unit,
    onSetDefaultAddress: (Int) -> Unit,
    onDeleteAddress: (Int) -> Unit,
    onCreateTicket: (
        subject: String,
        orderNo: String?,
        category: String,
        priority: String,
        message: String,
        onSuccess: () -> Unit
    ) -> Unit,
    onReplyTicket: (ticketId: Int, message: String, onDone: () -> Unit) -> Unit,
    onSetTheme: (String) -> Unit,
    onUpdatePreferences: ((LocalPreferences) -> LocalPreferences) -> Unit,
    onRequestPasswordReset: (email: String, onSent: () -> Unit) -> Unit,
    onResetPassword: (email: String, otp: String, newPassword: String, onSuccess: () -> Unit) -> Unit,
    onOpenHelpCenter: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    discoveryState: com.example.ui.viewmodel.DiscoveryUiState = com.example.ui.viewmodel.DiscoveryUiState(),
    onOpenDesignAudit: () -> Unit = {},
    onOpenConnectionDiagnostics: () -> Unit = {},
    onRefreshDesignManifest: () -> Unit = {}
) {
    val ext = LocalMelvoreColors.current
    val user = uiState.user

    if (user == null) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = ext.muted, modifier = Modifier.size(34.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Sign in to Melvore", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Access your saved addresses, wishlist, order tracking, and support tickets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.muted
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { onNavigateAuth("login") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_signin_cta")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Login")
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onNavigateAuth("register") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_register_cta")
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create Account")
                    }
                }
            }
            item {
                ProfileAppearanceCard(
                    themeMode = uiState.themeMode,
                    onSetTheme = onSetTheme
                )
            }
            item {
                ProfileAboutDesignSystemCard(
                    discoveryState = discoveryState,
                    onRefreshDesignManifest = onRefreshDesignManifest,
                    onOpenDesignAudit = onOpenDesignAudit,
                    onOpenConnectionDiagnostics = onOpenConnectionDiagnostics
                )
            }
        }
        return
    }

    var activeTab by remember(initialTab) { mutableStateOf(initialTab.ifBlank { "overview" }) }
    val tabs = listOf(
        "overview" to "Overview",
        "orders" to "Orders (${uiState.orders.size})",
        "wishlist" to "Wishlist (${uiState.wishlistIds.size})",
        "addresses" to "Addresses",
        "tickets" to "Support",
        "settings" to "Settings"
    )

    val initials = user.name.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "M" }

    val wishlistDisplayProducts = uiState.wishlistProducts.ifEmpty {
        uiState.products.filter { it.id in uiState.wishlistIds }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.isProfileBusy) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        // User Header Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(initials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.name, style = MaterialTheme.typography.titleLarge)
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                    }
                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("profile_logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Logout", tint = ext.crimson, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Logout", color = ext.crimson, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Scrollable Tab Bar
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { (key, label) ->
                    val selected = activeTab == key
                    Surface(
                        onClick = { activeTab = key },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = if (selected) null else BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier.testTag("profile_tab_$key")
                    ) {
                        Text(
                            text = label,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else ext.muted,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }

        item {
            when (activeTab) {
                "overview" -> ProfileOverviewSection(
                    user = user,
                    uiState = uiState,
                    onSaveProfile = onSaveProfile
                )
                "orders" -> ProfileOrdersSection(
                    orders = uiState.orders,
                    currency = uiState.currency,
                    isBusy = uiState.isProfileBusy || uiState.isCheckoutBusy,
                    onTrackOrder = onTrackOrder,
                    onCancelOrder = onCancelOrder,
                    onRetryRazorpay = onRetryRazorpay,
                    onBrowseShop = onNavigateShop
                )
                "wishlist" -> ProfileWishlistSection(
                    products = wishlistDisplayProducts,
                    currency = uiState.currency,
                    onOpenProduct = onOpenProduct,
                    onMoveToCart = onAddToCart,
                    onRemoveWishlist = onToggleWishlist,
                    onBrowseShop = onNavigateShop
                )
                "addresses" -> ProfileAddressesSection(
                    addresses = uiState.addresses,
                    isBusy = uiState.isProfileBusy,
                    onAddAddress = onAddAddress,
                    onSetDefault = onSetDefaultAddress,
                    onDeleteAddress = onDeleteAddress
                )
                "tickets" -> ProfileTicketsSection(
                    tickets = uiState.tickets,
                    isBusy = uiState.isProfileBusy,
                    onCreateTicket = onCreateTicket,
                    onReplyTicket = onReplyTicket
                )
                "settings" -> ProfileSettingsSection(
                    user = user,
                    themeMode = uiState.themeMode,
                    preferences = uiState.preferences,
                    isAuthBusy = uiState.isAuthBusy,
                    onSetTheme = onSetTheme,
                    onUpdatePreferences = onUpdatePreferences,
                    onRequestPasswordReset = onRequestPasswordReset,
                    onResetPassword = onResetPassword,
                    onOpenHelpCenter = onOpenHelpCenter,
                    discoveryState = discoveryState,
                    onOpenDesignAudit = onOpenDesignAudit,
                    onOpenConnectionDiagnostics = onOpenConnectionDiagnostics,
                    onRefreshDesignManifest = onRefreshDesignManifest
                )
            }
        }
    }
}
