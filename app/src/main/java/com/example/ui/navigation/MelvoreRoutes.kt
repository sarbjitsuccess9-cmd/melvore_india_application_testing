package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object DiscoverRoute

@Serializable
object VideosRoute

@Serializable
object EliteRoute

@Serializable
data class ElitePackRoute(
    val packId: Int = 0,
    val slug: String = ""
)

@Serializable
object BagRoute

@Serializable
data class BrandDetailRoute(
    val brandId: Int = 0,
    val slug: String = ""
)

@Serializable
data class StyleWorldRoute(
    val styleWorldId: Int = 0,
    val slug: String = ""
)

@Serializable
data class HelpSupportRoute(
    val topicSlug: String = ""
)

@Serializable
data class ShopRoute(
    val category: String = "all",
    val collection: String = "",
    val query: String = "",
    val brandId: Int = 0
)

@Serializable
object CategoriesRoute

@Serializable
data class ProductDetailsRoute(
    val productId: Int,
    val worldSlug: String = ""
)

@Serializable
object CheckoutRoute

@Serializable
data class OrderTrackingRoute(
    val orderNumber: String = ""
)

@Serializable
data class AuthRoute(
    val initialTab: String = "login"
)

@Serializable
data class ProfileRoute(
    val tab: String = "overview"
)

@Serializable
object AboutRoute

@Serializable
object BlogRoute

@Serializable
object FaqRoute

@Serializable
object ContactRoute

@Serializable
object DesignAuditRoute

@Serializable
object ConnectionDiagnosticsRoute
