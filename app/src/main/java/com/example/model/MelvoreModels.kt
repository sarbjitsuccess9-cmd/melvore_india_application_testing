package com.example.model

import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

data class ColorVariant(
    val name: String,
    val hex: String
)

data class ProductVariation(
    val id: Int,
    val attributeName: String = "",
    val attributeValue: String = "",
    val color: String = "",
    val size: String = "",
    val regularPrice: Double? = null,
    val salePrice: Double? = null,
    val stockQuantity: Int = 0,
    val isActive: Boolean = true,
    val imageUrl: String? = null
)

data class ProductReview(
    val id: Int,
    val userName: String,
    val rating: Int,
    val title: String = "",
    val comment: String,
    val isVerifiedPurchase: Boolean = false,
    val helpfulCount: Int = 0,
    val hasVotedHelpful: Boolean = false,
    val mediaUrls: List<String> = emptyList(),
    val createdAt: String = ""
)

data class Product(
    val id: Int,
    val sku: String = "",
    val name: String,
    val slug: String,
    val category: String,
    val categorySlug: String = "",
    val categoryId: Int = 0,
    val brandId: Int? = null,
    val brandName: String = "",
    val brandSlug: String = "",
    val originCountry: String = "",
    val isImported: Boolean = false,
    val collection: String = "",
    val regularPrice: Double,
    val salePrice: Double? = null,
    val discountPercentOverride: Int? = null,
    val imageUrl: String? = null,
    val galleryUrls: List<String> = emptyList(),
    val shortDescription: String = "",
    val description: String = "",
    val weight: Double? = null,
    val dimensions: String = "",
    val colors: List<ColorVariant> = emptyList(),
    val sizes: List<String> = emptyList(),
    val variations: List<ProductVariation> = emptyList(),
    val reviews: List<ProductReview> = emptyList(),
    val relatedProducts: List<Product> = emptyList(),
    val badge: String? = null,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val stockQuantity: Int = 0,
    val inStock: Boolean = true,
    val isFeatured: Boolean = false,
    val isSponsored: Boolean = false,
    val expressDelivery: Boolean = false,
    val videoUrls: List<String> = emptyList()
) {
    val effectivePrice: Double get() = salePrice ?: regularPrice
    val hasDiscount: Boolean get() = salePrice != null && salePrice < regularPrice
    val discountPercent: Int
        get() = discountPercentOverride ?: if (hasDiscount && regularPrice > 0) {
            (((regularPrice - effectivePrice) / regularPrice) * 100.0).toInt()
        } else 0
}

data class BrandItem(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String = "",
    val logoUrl: String? = null,
    val brandUrl: String = "",
    val originCountry: String = "",
    val isImported: Boolean = false,
    val productCount: Int = 0,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

data class Category(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String = "",
    val imageUrl: String? = null,
    val itemCount: Int = 0,
    val isFeatured: Boolean = false,
    val coverImageUrl: String? = null,
    val motifKey: String = "editorial_frame",
    val editorialBadge: String? = null,
    val editorialSubtitle: String? = null,
    val accentHex: String = "#141414",
    val bgHex: String = "#F8F7F4",
    val animationKey: String = "zoom_reveal",
    val tileSpan: String = "regular",
    val displayOrder: Int = 0
)

data class CollectionItem(
    val id: Int,
    val name: String,
    val subtitle: String = "",
    val slug: String,
    val bannerUrl: String? = null
)

data class TrendingTag(
    val id: Int,
    val name: String,
    val icon: String? = null
)

data class HeroSlide(
    val id: Int,
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null,
    val mobileImageUrl: String? = null,
    val desktopImageUrl: String? = null,
    val ctaText: String,
    val ctaLink: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true
) {
    fun resolveImageUrl(isWideLayout: Boolean): String? = if (isWideLayout) {
        desktopImageUrl ?: imageUrl ?: mobileImageUrl
    } else {
        mobileImageUrl ?: imageUrl ?: desktopImageUrl
    }

    val hasAnyText: Boolean
        get() = eyebrow.isNotBlank() || title.isNotBlank() || subtitle.isNotBlank()
}

data class PromoBanner(
    val id: Int,
    val title: String,
    val subtitle: String,
    val tag: String,
    val imageUrl: String? = null,
    val ctaText: String,
    val ctaLink: String = "",
    val filterTag: String,
    val position: String = ""
)

data class MarqueeItem(
    val id: Int,
    val text: String,
    val icon: String = ""
)

data class FeatureTile(
    val id: Int,
    val icon: String,
    val title: String,
    val description: String
)

data class TestimonialItem(
    val id: Int,
    val customerName: String,
    val customerImage: String? = null,
    val customerTitle: String,
    val rating: Int,
    val content: String
)

data class SiteStat(
    val id: Int,
    val label: String,
    val value: String
)

data class TeamMember(
    val id: Int,
    val name: String,
    val position: String,
    val bio: String,
    val imageUrl: String? = null
)

data class MilestoneItem(
    val id: Int,
    val year: String,
    val title: String,
    val description: String
)

data class AboutSection(
    val id: Int,
    val sectionKey: String,
    val title: String,
    val content: String
)

data class SiteSettingsConfig(
    val brandName: String = "Melvore",
    val siteUrl: String = "https://melvore.co.in",
    val currency: String = "₹",
    val shippingCharge: Double = 0.0,
    val freeShippingMinimum: Double = 0.0,
    val enableCod: Boolean = true,
    val enableOnlinePayment: Boolean = true,
    val contactEmail: String = "connect@melvore.co.in",
    val contactPhone: String = "+91 70423 36155",
    val siteAddress: String = "NCR, New Delhi 110033",
    val workingHours: String = "",
    val footerCopyright: String = "© 2026 Melvore. All rights reserved.",
    val showTrackingIdToCustomer: Boolean = true,
    val showCourierNameToCustomer: Boolean = true,
    val trackingPageSupportText: String = "",
    val orderTrackingHeroText: String = "Your order, every step of the way."
)

data class DesignBundle(
    val siteName: String = "Melvore",
    val currency: String = "₹",
    val siteSettings: SiteSettingsConfig = SiteSettingsConfig(),
    val heroSlides: List<HeroSlide> = emptyList(),
    val promoBanners: List<PromoBanner> = emptyList(),
    val marqueeItems: List<MarqueeItem> = emptyList(),
    val featureTiles: List<FeatureTile> = emptyList(),
    val testimonials: List<TestimonialItem> = emptyList(),
    val siteStats: List<SiteStat> = emptyList(),
    val teamMembers: List<TeamMember> = emptyList(),
    val milestones: List<MilestoneItem> = emptyList(),
    val featuredProducts: List<Product> = emptyList()
)

data class CartItem(
    val cartId: Int,
    val productId: Int,
    val variationId: Int? = null,
    val name: String,
    val sku: String = "",
    val price: Double,
    val regularPrice: Double = price,
    val lineTotal: Double = price,
    val imageUrl: String? = null,
    val color: String = "",
    val colorHex: String = "#141414",
    val size: String = "",
    val quantity: Int,
    val availableStock: Int = 0,
    val inStock: Boolean = true
)

data class CartSummary(
    val subtotal: Double = 0.0,
    val shippingCharge: Double = 0.0,
    val freeShipMin: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val codFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val couponCode: String = "",
    val couponMessage: String? = null,
    val currency: String = "₹"
)

data class Address(
    val id: Int,
    val label: String,
    val fullName: String,
    val phone: String,
    val street: String,
    val addressLine2: String = "",
    val city: String,
    val state: String,
    val pincode: String,
    val country: String = "India",
    val isDefault: Boolean = false
)

data class OrderTimelineStep(
    val key: String = "",
    val title: String,
    val description: String = "",
    val timestamp: String = "",
    val completed: Boolean,
    val current: Boolean = false
)

data class OrderItem(
    val orderItemId: Int = 0,
    val productId: Int,
    val name: String,
    val sku: String = "",
    val imageUrl: String? = null,
    val price: Double,
    val totalPrice: Double = price,
    val quantity: Int,
    val color: String = "",
    val size: String = ""
)

data class Order(
    val id: Int,
    val orderNumber: String,
    val status: String,
    val paymentMethod: String,
    val paymentStatus: String,
    val subtotal: Double,
    val shippingFee: Double,
    val taxAmount: Double = 0.0,
    val codFee: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double,
    val couponCode: String? = null,
    val shippingAddress: Address? = null,
    val items: List<OrderItem> = emptyList(),
    val trackingCourier: String? = null,
    val trackingNumber: String? = null,
    val trackingUrl: String? = null,
    val estimatedDelivery: String? = null,
    val cancellationReason: String? = null,
    val refundStatus: String? = null,
    val refundReference: String? = null,
    val timeline: List<OrderTimelineStep> = emptyList(),
    val createdAt: String = ""
)

data class SupportMessage(
    val id: Int,
    val sender: String, // user or support/admin
    val text: String,
    val createdAt: String = ""
)

data class SupportTicket(
    val id: Int,
    val ticketNumber: String,
    val orderId: Int? = null,
    val subject: String,
    val category: String = "general",
    val priority: String = "medium",
    val status: String = "open",
    val message: String = "",
    val createdAt: String = "",
    val replies: List<SupportMessage> = emptyList()
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String = "",
    val alternatePhone: String = "",
    val avatar: String? = null,
    val emailVerified: Boolean = false,
    val mobileVerified: Boolean = false,
    val isSubscribed: Boolean = false,
    val socialProvider: String = "none",
    val createdAt: String = ""
)

@Serializable
data class LocalPreferences(
    val themeMode: String = "light",
    val currencyOverride: String = "₹",
    val orderNotifications: Boolean = true,
    val promoNotifications: Boolean = true,
    val priceDropAlerts: Boolean = false,
    val ambientAudioEnabled: Boolean = false,
    val uiSfxEnabled: Boolean = false,
    val reducedMotion: Boolean = false,
    val videoAutoplayMuted: Boolean = true,
    val dataSaverMode: Boolean = false,
    val audioConsentAnswered: Boolean = false,
    val preferredWorldSlug: String? = null
)

fun requiresVariantSelection(product: Product): Boolean =
    product.colors.size > 1 || product.sizes.size > 1 || product.variations.isNotEmpty()

data class BlogPost(
    val id: Int,
    val slug: String,
    val title: String,
    val category: String,
    val publishedAt: String,
    val excerpt: String,
    val contentHtml: String = "",
    val imageUrl: String? = null,
    val viewCount: Int = 0
)

data class FaqItem(
    val id: Int,
    val category: String,
    val question: String,
    val answer: String,
    val displayOrder: Int = 0
)

data class CouponInfo(
    val couponId: Int,
    val code: String,
    val description: String,
    val discountType: String,
    val discountValue: Double,
    val minOrderAmount: Double,
    val maxDiscountAmount: Double?
)

data class RazorpayCheckoutPayload(
    val orderId: Int,
    val orderNumber: String,
    val keyId: String,
    val razorpayOrderId: String,
    val amountPaise: Long,
    val currency: String,
    val environment: String,
    val prefillName: String,
    val prefillEmail: String,
    val prefillContact: String
)

fun formatPrice(amount: Double, currency: String = "₹"): String {
    val value = if (currency == "$") amount / 84.0 else amount
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
    return "$currency${formatter.format(value)}"
}

fun parseHexColor(hex: String, defaultHex: Long = 0xFF141414L): Long {
    val clean = hex.removePrefix("#").trim()
    return when (clean.length) {
        6 -> ("FF$clean").toLongOrNull(16) ?: defaultHex
        8 -> clean.toLongOrNull(16) ?: defaultHex
        else -> defaultHex
    }
}
