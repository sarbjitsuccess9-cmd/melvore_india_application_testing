package com.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Marker interface for read-endpoint responses where HTTP 200 with `success == false`
 * must be surfaced as [ApiResult.Error] rather than silently parsed as empty data.
 */
interface StrictSuccessResponse {
    val success: Boolean
    val message: String?
    val errorCode: String?
}

@Serializable
data class ApiErrorEnvelope(
    val success: Boolean = false,
    @SerialName("error_code") val errorCode: String? = null,
    val message: String? = null
)

@Serializable
data class ApiMessageResponse(
    val success: Boolean = false,
    val message: String? = null,
    @SerialName("error_code") val errorCode: String? = null
)

@Serializable
data class HealthResponse(
    override val success: Boolean = false,
    val status: String = "",
    val service: String = "",
    @SerialName("active_products") val activeProducts: Int = 0,
    val timestamp: String = "",
    override val message: String? = null,
    @SerialName("error_code") override val errorCode: String? = null
) : StrictSuccessResponse

// ==================== DESIGN BUNDLE ====================

@Serializable
data class DesignBundleResponse(
    val success: Boolean = true,
    @SerialName("site_settings") val siteSettings: SiteSettingsDto = SiteSettingsDto(),
    @SerialName("hero_slides") val heroSlides: List<HeroSlideDto> = emptyList(),
    val banners: List<PromoBannerDto> = emptyList(),
    val marquee: List<MarqueeItemDto> = emptyList(),
    @SerialName("feature_tiles") val featureTiles: List<FeatureTileDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val brands: List<BrandDto> = emptyList(),
    val collections: List<CollectionDto> = emptyList(),
    @SerialName("trending_tags") val trendingTags: List<TrendingTagDto> = emptyList(),
    val featured: List<ProductDto> = emptyList(),
    val testimonials: List<TestimonialDto> = emptyList(),
    @SerialName("site_stats") val siteStats: List<SiteStatDto> = emptyList(),
    @SerialName("team_members") val teamMembers: List<TeamMemberDto> = emptyList(),
    val milestones: List<MilestoneDto> = emptyList(),
    val faqs: List<FaqDto> = emptyList()
)

@Serializable
data class SiteSettingsDto(
    @SerialName("brand_name") val brandName: String = "Melvore",
    @SerialName("site_url") val siteUrl: String = "https://melvore.co.in",
    val currency: String = "₹",
    @SerialName("shipping_charge") val shippingCharge: Double = 0.0,
    @SerialName("free_shipping_minimum") val freeShippingMinimum: Double = 0.0,
    @SerialName("enable_cod") val enableCod: Boolean = true,
    @SerialName("enable_online_payment") val enableOnlinePayment: Boolean = true,
    @SerialName("contact_email") val contactEmail: String = "connect@melvore.co.in",
    @SerialName("contact_phone") val contactPhone: String = "+91 70423 36155",
    @SerialName("site_address") val siteAddress: String = "NCR, New Delhi 110033",
    @SerialName("working_hours") val workingHours: String = "",
    @SerialName("footer_copyright") val footerCopyright: String = "© 2026 Melvore. All rights reserved.",
    @SerialName("show_tracking_id_to_customer") val showTrackingIdToCustomer: Boolean = true,
    @SerialName("show_courier_name_to_customer") val showCourierNameToCustomer: Boolean = true,
    @SerialName("tracking_page_support_text") val trackingPageSupportText: String = "",
    @SerialName("order_tracking_hero_text") val orderTrackingHeroText: String = "Your order, every step of the way."
)

@Serializable
data class HeroSlideDto(
    val id: Int = 0,
    val title: String = "",
    val subtitle: String = "",
    val eyebrow: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("mobile_image_url") val mobileImageUrl: String? = null,
    @SerialName("desktop_image_url") val desktopImageUrl: String? = null,
    @SerialName("cta_text") val ctaText: String = "",
    @SerialName("cta_link") val ctaLink: String = "",
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("is_active") val isActive: JsonElement? = null
) {
    fun isActiveBool(): Boolean {
        val prim = isActive as? JsonPrimitive ?: return true
        return prim.booleanOrNull ?: ((prim.intOrNull ?: 1) != 0)
    }
}

@Serializable
data class PromoBannerDto(
    @SerialName("banner_id") val bannerId: Int = 0,
    val title: String = "",
    val subtitle: String = "",
    val tag: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("cta_text") val ctaText: String = "",
    @SerialName("cta_link") val ctaLink: String = "",
    @SerialName("filter_tag") val filterTag: String = "",
    val position: String = ""
)

@Serializable
data class MarqueeItemDto(
    val id: Int = 0,
    val text: String = "",
    val icon: String = ""
)

@Serializable
data class FeatureTileDto(
    @SerialName("feature_id") val featureId: Int = 0,
    val icon: String = "",
    val title: String = "",
    val description: String = ""
)

@Serializable
data class CategoryDto(
    @SerialName("category_id") val categoryId: Int = 0,
    val name: String = "",
    val slug: String = "",
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("motif_key") val motifKey: String = "editorial_frame",
    @SerialName("editorial_badge") val editorialBadge: String? = null,
    @SerialName("editorial_subtitle") val editorialSubtitle: String? = null,
    @SerialName("accent_hex") val accentHex: String = "#141414",
    @SerialName("bg_hex") val bgHex: String = "#F8F7F4",
    @SerialName("animation_key") val animationKey: String = "zoom_reveal",
    @SerialName("tile_span") val tileSpan: String = "regular",
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("product_count") val productCount: Int = 0
)

@Serializable
data class BrandDto(
    @SerialName("brand_id") val brandId: Int = 0,
    val id: Int = 0,
    val name: String = "",
    @SerialName("brand_name") val brandName: String = "",
    val slug: String = "",
    val description: String? = null,
    val logo: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("brand_url") val brandUrl: String? = null,
    @SerialName("origin_country") val originCountry: String? = null,
    @SerialName("is_imported") val isImported: JsonElement? = null,
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("is_active") val isActive: JsonElement? = null,
    val status: String? = null
) {
    fun resolvedId(): Int = if (brandId > 0) brandId else id
    fun resolvedName(): String = brandName.ifBlank { name }.trim()
    fun resolvedLogo(): String? = logoUrl?.takeIf { it.isNotBlank() } ?: logo?.takeIf { it.isNotBlank() }
    fun isActiveBrand(): Boolean {
        val statusLower = status?.trim()?.lowercase().orEmpty()
        if (statusLower == "inactive" || statusLower == "hidden" || statusLower == "deleted") return false
        val prim = isActive as? JsonPrimitive ?: return true
        return prim.booleanOrNull ?: ((prim.intOrNull ?: 1) != 0)
    }
    fun isImportedBool(): Boolean {
        val prim = isImported as? JsonPrimitive ?: return !originCountry.isNullOrBlank()
        return prim.booleanOrNull ?: ((prim.intOrNull ?: 0) != 0)
    }
}

@Serializable
data class CollectionDto(
    val id: Int = 0,
    val name: String = "",
    val subtitle: String = "",
    val slug: String = "",
    @SerialName("banner_url") val bannerUrl: String? = null
)

@Serializable
data class TrendingTagDto(
    @SerialName("tag_id") val tagId: Int = 0,
    val name: String = "",
    val icon: String? = null
)

@Serializable
data class TestimonialDto(
    @SerialName("testimonial_id") val testimonialId: Int = 0,
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_image") val customerImage: String? = null,
    @SerialName("customer_title") val customerTitle: String = "Verified Buyer",
    val rating: Int = 5,
    val content: String = ""
)

@Serializable
data class SiteStatDto(
    @SerialName("stat_id") val statId: Int = 0,
    val label: String = "",
    val value: String = ""
)

@Serializable
data class TeamMemberDto(
    val id: Int = 0,
    val name: String = "",
    val position: String = "",
    val bio: String = "",
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class MilestoneDto(
    val id: Int = 0,
    val year: String = "",
    val title: String = "",
    val description: String = ""
)

// ==================== CATALOG & REVIEWS ====================

@Serializable
data class ColorVariantDto(
    val name: String = "",
    val value: String = "#141414"
)

@Serializable
data class ProductVariationDto(
    @SerialName("variation_id") val variationId: Int = 0,
    @SerialName("attribute_name") val attributeName: String = "",
    @SerialName("attribute_value") val attributeValue: String = "",
    val color: String? = null,
    val size: String? = null,
    @SerialName("regular_price") val regularPrice: Double? = null,
    @SerialName("sale_price") val salePrice: Double? = null,
    @SerialName("stock_quantity") val stockQuantity: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class ReviewMediaDto(
    val url: String = "",
    val type: String = "image"
)

@Serializable
data class ReviewDto(
    @SerialName("review_id") val reviewId: Int = 0,
    @SerialName("user_name") val userName: String = "Customer",
    val rating: Int = 5,
    val title: String? = null,
    val comment: String = "",
    @SerialName("is_verified_purchase") val isVerifiedPurchase: Boolean = false,
    @SerialName("helpful_count") val helpfulCount: Int = 0,
    @SerialName("has_voted_helpful") val hasVotedHelpful: Boolean = false,
    val media: List<ReviewMediaDto> = emptyList(),
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ProductVideoMediaDto(
    val type: String = "video",
    val url: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("alt_text") val altText: String = "",
    @SerialName("is_primary") val isPrimary: Boolean = false
)

@Serializable
data class ProductDto(
    @SerialName("product_id") val productId: Int = 0,
    val sku: String = "",
    val name: String = "",
    val slug: String = "",
    val badge: String? = null,
    @SerialName("short_description") val shortDescription: String? = null,
    val description: String? = null,
    @SerialName("category_id") val categoryId: Int = 0,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("category_slug") val categorySlug: String? = null,
    @SerialName("brand_id") val brandId: Int? = null,
    @SerialName("brand_name") val brandName: String? = null,
    @SerialName("brand_slug") val brandSlug: String? = null,
    @SerialName("origin_country") val originCountry: String? = null,
    @SerialName("is_imported") val isImported: Boolean = false,
    @SerialName("regular_price") val regularPrice: Double = 0.0,
    @SerialName("sale_price") val salePrice: Double? = null,
    @SerialName("discount_percent") val discountPercent: Int? = null,
    @SerialName("stock_quantity") val stockQuantity: Int = 0,
    @SerialName("in_stock") val inStock: Boolean = true,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("is_sponsored") val isSponsored: Boolean = false,
    @SerialName("express_delivery") val expressDelivery: Boolean = false,
    val weight: Double? = null,
    val dimensions: String? = null,
    @SerialName("rating_average") val ratingAverage: Double = 0.0,
    @SerialName("rating_count") val ratingCount: Int = 0,
    @SerialName("primary_image") val primaryImage: String? = null,
    val images: List<String> = emptyList(),
    val videos: List<ProductVideoMediaDto> = emptyList(),
    @SerialName("available_colors") val availableColors: List<ColorVariantDto> = emptyList(),
    @SerialName("available_sizes") val availableSizes: List<String> = emptyList(),
    val variations: List<ProductVariationDto> = emptyList(),
    val reviews: List<ReviewDto> = emptyList(),
    @SerialName("related_products") val relatedProducts: List<ProductDto> = emptyList()
)

@Serializable
data class PaginationDto(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 1
)

@Serializable
data class ProductsListDataDto(
    val products: List<ProductDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto()
)

@Serializable
data class ProductsListResponse(
    override val success: Boolean = false,
    val products: List<ProductDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
    val data: ProductsListDataDto? = null,
    override val message: String? = null,
    @SerialName("error_code") override val errorCode: String? = null
) : StrictSuccessResponse {
    fun resolvedProducts(): List<ProductDto> = data?.products?.takeIf { it.isNotEmpty() } ?: products
    fun resolvedPagination(): PaginationDto = data?.pagination ?: pagination
}

@Serializable
data class ProductDetailsDataDto(
    val product: ProductDto? = null
)

@Serializable
data class ProductDetailsResponse(
    override val success: Boolean = false,
    val product: ProductDto? = null,
    val data: ProductDetailsDataDto? = null,
    override val message: String? = null,
    @SerialName("error_code") override val errorCode: String? = null
) : StrictSuccessResponse {
    fun resolvedProduct(): ProductDto? = data?.product ?: product
}

@Serializable
data class CategoriesDataDto(
    val categories: List<CategoryDto> = emptyList(),
    val brands: List<BrandDto> = emptyList(),
    val collections: List<CollectionDto> = emptyList()
)

@Serializable
data class CategoriesResponse(
    override val success: Boolean = false,
    val categories: List<CategoryDto> = emptyList(),
    val brands: List<BrandDto> = emptyList(),
    val collections: List<CollectionDto> = emptyList(),
    val data: CategoriesDataDto? = null,
    override val message: String? = null,
    @SerialName("error_code") override val errorCode: String? = null
) : StrictSuccessResponse {
    fun resolvedCategories(): List<CategoryDto> = data?.categories?.takeIf { it.isNotEmpty() } ?: categories
    fun resolvedBrands(): List<BrandDto> = data?.brands?.takeIf { it.isNotEmpty() } ?: brands
    fun resolvedCollections(): List<CollectionDto> = data?.collections?.takeIf { it.isNotEmpty() } ?: collections
}

@Serializable
data class ReviewsListDataDto(
    val reviews: List<ReviewDto> = emptyList()
)

@Serializable
data class ReviewsListResponse(
    override val success: Boolean = false,
    val reviews: List<ReviewDto> = emptyList(),
    val data: ReviewsListDataDto? = null,
    override val message: String? = null,
    @SerialName("error_code") override val errorCode: String? = null
) : StrictSuccessResponse {
    fun resolvedReviews(): List<ReviewDto> = data?.reviews?.takeIf { it.isNotEmpty() } ?: reviews
}

// ==================== AUTH & PROFILE ====================

@Serializable
data class UserDto(
    @SerialName("user_id") val userId: Int = 0,
    val name: String = "",
    val email: String = "",
    @SerialName("mobile_primary") val mobilePrimary: String? = null,
    @SerialName("mobile_alternate") val mobileAlternate: String? = null,
    val avatar: String? = null,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("mobile_verified") val mobileVerified: Boolean = false,
    @SerialName("is_subscribed") val isSubscribed: Boolean = false,
    @SerialName("social_provider") val socialProvider: String = "none",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String
)

@Serializable
data class SocialAuthRequest(
    val provider: String = "google",
    @SerialName("id_token") val idToken: String
)

@Serializable
data class OtpRequest(
    val action: String, // "send" or "verify"
    val email: String,
    val otp: String? = null
)

@Serializable
data class ForgotPasswordRequest(
    val action: String, // "request" or "verify"
    val email: String,
    val otp: String? = null,
    @SerialName("new_password") val newPassword: String? = null,
    val password: String? = null
)

@Serializable
data class AuthResponse(
    val success: Boolean = false,
    val token: String? = null,
    val user: UserDto? = null,
    val message: String? = null,
    @SerialName("error_code") val errorCode: String? = null
)

@Serializable
data class ProfileCountsDto(
    val cart: Int = 0,
    val wishlist: Int = 0,
    @SerialName("pending_orders") val pendingOrders: Int = 0
)

@Serializable
data class ProfileResponse(
    val success: Boolean = false,
    val user: UserDto? = null,
    val counts: ProfileCountsDto = ProfileCountsDto(),
    val addresses: List<AddressDto> = emptyList(),
    val message: String? = null
)

@Serializable
data class ProfileUpdateRequest(
    val name: String,
    @SerialName("mobile_primary") val mobilePrimary: String,
    @SerialName("mobile_alternate") val mobileAlternate: String? = null
)

// ==================== CART & COUPONS ====================

@Serializable
data class CartAttributesDto(
    val color: String? = null,
    val size: String? = null
)

@Serializable
data class CartItemDto(
    @SerialName("cart_id") val cartId: Int = 0,
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("variation_id") val variationId: Int? = null,
    val name: String = "",
    val sku: String = "",
    val color: String? = null,
    val size: String? = null,
    val attributes: CartAttributesDto? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("regular_price") val regularPrice: Double = 0.0,
    val quantity: Int = 1,
    @SerialName("line_total") val lineTotal: Double = 0.0,
    @SerialName("available_stock") val availableStock: Int = 0,
    @SerialName("in_stock") val inStock: Boolean = true
)

@Serializable
data class CartSummaryDto(
    val subtotal: Double = 0.0,
    @SerialName("shipping_charge") val shippingCharge: Double = 0.0,
    @SerialName("free_ship_min") val freeShipMin: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("cod_fee") val codFee: Double = 0.0,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("coupon_code") val couponCode: String = "",
    @SerialName("coupon_message") val couponMessage: String? = null,
    val currency: String = "₹"
)

@Serializable
data class CartResponse(
    val success: Boolean = false,
    val items: List<CartItemDto> = emptyList(),
    val count: Int = 0,
    val summary: CartSummaryDto = CartSummaryDto(),
    val message: String? = null
)

@Serializable
data class CartAddRequest(
    @SerialName("product_id") val productId: Int,
    @SerialName("variation_id") val variationId: Int? = null,
    val quantity: Int = 1,
    val color: String? = null,
    val size: String? = null
)

@Serializable
data class CartUpdateRequest(
    @SerialName("cart_id") val cartId: Int,
    val delta: Int? = null,
    val quantity: Int? = null
)

@Serializable
data class CartRemoveRequest(
    @SerialName("cart_id") val cartId: Int
)

@Serializable
data class CouponInfoDto(
    @SerialName("coupon_id") val couponId: Int = 0,
    val code: String = "",
    val description: String = "",
    @SerialName("discount_type") val discountType: String = "fixed",
    @SerialName("discount_value") val discountValue: Double = 0.0,
    @SerialName("min_order_amount") val minOrderAmount: Double = 0.0,
    @SerialName("max_discount_amount") val maxDiscountAmount: Double? = null
)

@Serializable
data class CouponsListResponse(
    val success: Boolean = false,
    val coupons: List<CouponInfoDto> = emptyList()
)

@Serializable
data class CouponValidateRequest(
    @SerialName("coupon_code") val couponCode: String,
    @SerialName("payment_method") val paymentMethod: String = "online"
)

@Serializable
data class CouponValidateResponse(
    val success: Boolean = false,
    val valid: Boolean = false,
    val code: String? = null,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    val summary: CartSummaryDto? = null,
    val message: String? = null
)

// ==================== WISHLIST & ADDRESSES ====================

@Serializable
data class WishlistItemDto(
    @SerialName("wishlist_id") val wishlistId: Int = 0,
    @SerialName("product_id") val productId: Int = 0,
    val sku: String = "",
    val name: String = "",
    val slug: String = "",
    @SerialName("category_name") val categoryName: String? = null,
    val badge: String? = null,
    @SerialName("regular_price") val regularPrice: Double = 0.0,
    @SerialName("sale_price") val salePrice: Double? = null,
    @SerialName("stock_quantity") val stockQuantity: Int = 0,
    @SerialName("in_stock") val inStock: Boolean = true,
    @SerialName("rating_average") val ratingAverage: Double = 0.0,
    @SerialName("primary_image") val primaryImage: String? = null
)

@Serializable
data class WishlistResponse(
    val success: Boolean = false,
    val items: List<WishlistItemDto> = emptyList(),
    @SerialName("product_ids") val productIds: List<Int> = emptyList(),
    val count: Int = 0,
    val message: String? = null
)

@Serializable
data class WishlistMutateRequest(
    @SerialName("product_id") val productId: Int
)

@Serializable
data class AddressDto(
    @SerialName("address_id") val addressId: Int = 0,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("address_line1") val addressLine1: String = "",
    @SerialName("address_line2") val addressLine2: String? = null,
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = "India",
    val mobile: String = "",
    @SerialName("is_default") val isDefault: JsonElement? = null,
    @SerialName("address_type") val addressType: String = "home",
    @SerialName("created_at") val createdAt: String? = null
) {
    fun isDefaultBool(): Boolean {
        val prim = isDefault as? JsonPrimitive ?: return false
        return prim.booleanOrNull ?: ((prim.intOrNull ?: 0) == 1)
    }
}

@Serializable
data class AddressListResponse(
    val success: Boolean = false,
    val addresses: List<AddressDto> = emptyList()
)

@Serializable
data class AddressSaveRequest(
    @SerialName("address_id") val addressId: Int? = null,
    @SerialName("full_name") val fullName: String,
    val mobile: String,
    @SerialName("address_line1") val addressLine1: String,
    @SerialName("address_line2") val addressLine2: String = "",
    val city: String,
    val state: String,
    val pincode: String,
    val country: String = "India",
    @SerialName("address_type") val addressType: String = "home",
    @SerialName("is_default") val isDefault: Boolean = false
)

@Serializable
data class AddressSaveResponse(
    val success: Boolean = false,
    val address: AddressDto? = null,
    val message: String? = null
)

@Serializable
data class AddressDeleteRequest(
    @SerialName("address_id") val addressId: Int
)

// ==================== ORDERS & PAYMENTS ====================

@Serializable
data class OrderCreateRequest(
    @SerialName("shipping_address_id") val shippingAddressId: Int,
    @SerialName("payment_method") val paymentMethod: String, // "cod" or "online"
    @SerialName("coupon_code") val couponCode: String = "",
    val notes: String = ""
)

@Serializable
data class OrderCreatedItemDto(
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("product_name") val productName: String = "",
    val sku: String = "",
    val quantity: Int = 1,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("total_price") val totalPrice: Double = 0.0,
    val color: String? = null,
    val size: String? = null
)

@Serializable
data class OrderCreatedDto(
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    val subtotal: Double = 0.0,
    @SerialName("shipping_charge") val shippingCharge: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("cod_fee") val codFee: Double = 0.0,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String = "cod",
    @SerialName("payment_status") val paymentStatus: String = "pending",
    @SerialName("order_status") val orderStatus: String = "pending",
    @SerialName("coupon_code") val couponCode: String? = null,
    @SerialName("estimated_delivery_date") val estimatedDeliveryDate: String? = null,
    @SerialName("requires_online_payment") val requiresOnlinePayment: Boolean = false,
    val items: List<OrderCreatedItemDto> = emptyList()
)

@Serializable
data class OrderCreateResponse(
    val success: Boolean = false,
    val order: OrderCreatedDto? = null,
    val message: String? = null
)

@Serializable
data class OrderItemDto(
    @SerialName("order_item_id") val orderItemId: Int = 0,
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("product_name") val productName: String = "",
    val sku: String = "",
    val quantity: Int = 1,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("total_price") val totalPrice: Double = 0.0,
    val attributes: CartAttributesDto? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class OrderHistoryDto(
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    val subtotal: Double = 0.0,
    @SerialName("shipping_charge") val shippingCharge: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("cod_fee") val codFee: Double = 0.0,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("order_status") val orderStatus: String = "",
    @SerialName("coupon_code") val couponCode: String? = null,
    @SerialName("tracking_number") val trackingNumber: String? = null,
    @SerialName("courier_name") val courierName: String? = null,
    @SerialName("estimated_delivery_date") val estimatedDeliveryDate: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    val items: List<OrderItemDto> = emptyList()
)

@Serializable
data class OrdersHistoryResponse(
    val success: Boolean = false,
    val orders: List<OrderHistoryDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto()
)

@Serializable
data class OrderDetailsDto(
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    val subtotal: Double = 0.0,
    @SerialName("shipping_charge") val shippingCharge: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("cod_fee") val codFee: Double = 0.0,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("order_status") val orderStatus: String = "",
    @SerialName("coupon_code") val couponCode: String? = null,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("estimated_delivery_date") val estimatedDeliveryDate: String? = null,
    @SerialName("tracking_number") val trackingNumber: String? = null,
    @SerialName("courier_name") val courierName: String? = null,
    @SerialName("tracking_url") val trackingUrl: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("shipping_address") val shippingAddress: AddressDto? = null,
    val items: List<OrderItemDto> = emptyList()
)

@Serializable
data class OrderDetailsResponse(
    val success: Boolean = false,
    val order: OrderDetailsDto? = null
)

@Serializable
data class OrderTimelineStepDto(
    val key: String = "",
    val title: String = "",
    val description: String? = null,
    val timestamp: String? = null,
    val completed: Boolean = false,
    val current: Boolean = false
)

@Serializable
data class OrderTrackingDto(
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("order_status") val orderStatus: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("tracking_number") val trackingNumber: String? = null,
    @SerialName("courier_name") val courierName: String? = null,
    @SerialName("tracking_url") val trackingUrl: String? = null,
    @SerialName("estimated_delivery_date") val estimatedDeliveryDate: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    val timeline: List<OrderTimelineStepDto> = emptyList(),
    @SerialName("order_tracking_hero_text") val heroText: String? = null,
    @SerialName("tracking_page_support_text") val supportText: String? = null
)

@Serializable
data class OrderTrackResponse(
    val success: Boolean = false,
    val tracking: OrderTrackingDto? = null
)

@Serializable
data class OrderCancelRequest(
    @SerialName("order_id") val orderId: Int,
    val reason: String = "Customer cancelled from Android app"
)

@Serializable
data class OrderCancelResponse(
    val success: Boolean = false,
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("order_status") val orderStatus: String = "",
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("refund_status") val refundStatus: String? = null,
    @SerialName("refund_reference") val refundReference: String? = null,
    val message: String? = null
)

@Serializable
data class PaymentInitiateRequest(
    @SerialName("order_id") val orderId: Int,
    val gateway: String = "razorpay"
)

@Serializable
data class PaymentPrefillDto(
    val name: String = "",
    val email: String = "",
    val contact: String = ""
)

@Serializable
data class PaymentInitiateResponse(
    val success: Boolean = false,
    val gateway: String = "razorpay",
    val environment: String = "TEST",
    @SerialName("key_id") val keyId: String = "",
    @SerialName("razorpay_order_id") val razorpayOrderId: String = "",
    val amount: Double = 0.0,
    @SerialName("amount_paise") val amountPaise: Long = 0L,
    val currency: String = "INR",
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    val prefill: PaymentPrefillDto = PaymentPrefillDto()
)

@Serializable
data class PaymentVerifyRequest(
    @SerialName("order_id") val orderId: Int,
    val gateway: String = "razorpay",
    @SerialName("razorpay_order_id") val razorpayOrderId: String,
    @SerialName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerialName("razorpay_signature") val razorpaySignature: String
)

@Serializable
data class PaymentVerifyResponse(
    val success: Boolean = false,
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("order_status") val orderStatus: String = "",
    val message: String? = null
)

@Serializable
data class PaymentFailRequest(
    @SerialName("order_id") val orderId: Int,
    val action: String, // "fail" or "cancel_order"
    val reason: String? = null
)

@Serializable
data class PaymentFailResponse(
    val success: Boolean = false,
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("order_status") val orderStatus: String = "",
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("can_retry") val canRetry: Boolean = false,
    val message: String? = null
)

// ==================== CMS, BLOG, SUPPORT & CONTACT ====================

@Serializable
data class BlogPostDto(
    val id: Int = 0,
    val title: String = "",
    val slug: String = "",
    val excerpt: String = "",
    val content: String = "",
    @SerialName("featured_image") val featuredImage: String? = null,
    @SerialName("category_name") val categoryName: String = "Journal",
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class BlogListResponse(
    val success: Boolean = false,
    val posts: List<BlogPostDto> = emptyList()
)

@Serializable
data class BlogDetailsResponse(
    val success: Boolean = false,
    val post: BlogPostDto? = null
)

@Serializable
data class FaqDto(
    @SerialName("faq_id") val faqId: Int = 0,
    val question: String = "",
    val answer: String = "",
    val category: String = "General",
    @SerialName("display_order") val displayOrder: Int = 0
)

@Serializable
data class FaqsResponse(
    val success: Boolean = false,
    val faqs: List<FaqDto> = emptyList()
)

@Serializable
data class AboutContentDto(
    val id: Int = 0,
    @SerialName("section_key") val sectionKey: String = "",
    val title: String = "",
    val content: String = ""
)

@Serializable
data class CmsPageResponse(
    val success: Boolean = false,
    @SerialName("about_content") val aboutContent: List<AboutContentDto> = emptyList()
)

@Serializable
data class SupportReplyDto(
    @SerialName("reply_id") val replyId: Int = 0,
    @SerialName("sender_type") val senderType: String = "user",
    @SerialName("sender_name") val senderName: String? = null,
    val message: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class SupportTicketDto(
    @SerialName("ticket_id") val ticketId: Int = 0,
    @SerialName("ticket_number") val ticketNumber: String = "",
    @SerialName("order_id") val orderId: Int? = null,
    val subject: String = "",
    val category: String = "general",
    val priority: String = "medium",
    val status: String = "open",
    val message: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val replies: List<SupportReplyDto> = emptyList()
)

@Serializable
data class SupportTicketsResponse(
    val success: Boolean = false,
    val tickets: List<SupportTicketDto> = emptyList()
)

@Serializable
data class SupportCreateRequest(
    val subject: String,
    @SerialName("order_id") val orderId: Int? = null,
    val category: String = "general",
    val priority: String = "medium",
    val message: String
)

@Serializable
data class SupportCreateResponse(
    val success: Boolean = false,
    val ticket: SupportTicketDto? = null,
    val message: String? = null
)

@Serializable
data class SupportReplyRequest(
    @SerialName("ticket_id") val ticketId: Int,
    val message: String
)

@Serializable
data class SupportReplyResponse(
    val success: Boolean = false,
    @SerialName("reply_id") val replyId: Int = 0,
    val message: String? = null
)

@Serializable
data class ContactRequest(
    val name: String,
    val email: String,
    val phone: String = "",
    val subject: String = "",
    val message: String
)

@Serializable
data class ContactResponse(
    val success: Boolean = false,
    @SerialName("contact_id") val contactId: Int = 0,
    val message: String? = null
)
