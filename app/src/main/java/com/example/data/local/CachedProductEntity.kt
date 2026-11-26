package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.Category
import com.example.model.ColorVariant
import com.example.model.Product
import com.example.model.ProductReview
import com.example.model.ProductVariation
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal val ProductCacheJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

@Serializable
internal data class CachedColorVariantPayload(
    val name: String = "",
    val hex: String = "#141414"
)

@Serializable
internal data class CachedProductVariationPayload(
    val id: Int = 0,
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

@Serializable
internal data class CachedProductReviewPayload(
    val id: Int = 0,
    val userName: String = "",
    val rating: Int = 5,
    val title: String = "",
    val comment: String = "",
    val isVerifiedPurchase: Boolean = false,
    val helpfulCount: Int = 0,
    val hasVotedHelpful: Boolean = false,
    val mediaUrls: List<String> = emptyList(),
    val createdAt: String = ""
)

/**
 * Local Room entity caching Melvore catalog and detail product data for offline viewing.
 * Nested collections (gallery, colors, sizes, variations, reviews, related IDs, videos)
 * are stored as compact JSON strings so no extra join tables are needed on offline reads.
 */
@Entity(
    tableName = "cached_products",
    indices = [
        Index(value = ["categorySlug"]),
        Index(value = ["brandId"]),
        Index(value = ["collection"]),
        Index(value = ["isFeatured"]),
        Index(value = ["cachedAt"])
    ]
)
data class CachedProductEntity(
    @PrimaryKey val id: Int,
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
    val galleryUrlsJson: String = "[]",
    val shortDescription: String = "",
    val description: String = "",
    val weight: Double? = null,
    val dimensions: String = "",
    val colorsJson: String = "[]",
    val sizesJson: String = "[]",
    val variationsJson: String = "[]",
    val reviewsJson: String = "[]",
    val relatedProductIdsJson: String = "[]",
    val badge: String? = null,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val stockQuantity: Int = 0,
    val inStock: Boolean = true,
    val isFeatured: Boolean = false,
    val isSponsored: Boolean = false,
    val expressDelivery: Boolean = false,
    val videoUrlsJson: String = "[]",
    val hasFullDetails: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Local Room entity caching Melvore categories so category strips and shop filters
 * stay usable alongside cached products when offline.
 */
@Entity(
    tableName = "cached_categories",
    indices = [Index(value = ["slug"]), Index(value = ["displayOrder"])]
)
data class CachedCategoryEntity(
    @PrimaryKey val id: Int,
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
    val displayOrder: Int = 0,
    val cachedAt: Long = System.currentTimeMillis()
)

internal fun Product.toCachedEntity(
    isDetailed: Boolean,
    existing: CachedProductEntity? = null,
    nowMs: Long = System.currentTimeMillis()
): CachedProductEntity {
    val mergedGallery = galleryUrls.ifEmpty {
        existing?.let { decodeStringList(it.galleryUrlsJson) }.orEmpty()
    }
    val mergedColors = colors.ifEmpty {
        existing?.let { decodeColorList(it.colorsJson) }.orEmpty()
    }
    val mergedSizes = sizes.ifEmpty {
        existing?.let { decodeStringList(it.sizesJson) }.orEmpty()
    }
    val mergedVariations = variations.ifEmpty {
        existing?.let { decodeVariationList(it.variationsJson) }.orEmpty()
    }
    val mergedReviews = reviews.ifEmpty {
        existing?.let { decodeReviewList(it.reviewsJson) }.orEmpty()
    }
    val mergedRelatedIds = relatedProducts.map { it.id }.filter { it > 0 }.ifEmpty {
        existing?.let { decodeIntList(it.relatedProductIdsJson) }.orEmpty()
    }
    val mergedVideos = videoUrls.ifEmpty {
        existing?.let { decodeStringList(it.videoUrlsJson) }.orEmpty()
    }

    return CachedProductEntity(
        id = id,
        sku = sku.ifBlank { existing?.sku.orEmpty() },
        name = name,
        slug = slug.ifBlank { existing?.slug.orEmpty() },
        category = category.ifBlank { existing?.category.orEmpty() },
        categorySlug = categorySlug.ifBlank { existing?.categorySlug.orEmpty() },
        categoryId = if (categoryId > 0) categoryId else (existing?.categoryId ?: 0),
        brandId = brandId ?: existing?.brandId,
        brandName = brandName.ifBlank { existing?.brandName.orEmpty() },
        brandSlug = brandSlug.ifBlank { existing?.brandSlug.orEmpty() },
        originCountry = originCountry.ifBlank { existing?.originCountry.orEmpty() },
        isImported = isImported || (existing?.isImported == true),
        collection = collection.ifBlank { existing?.collection.orEmpty() },
        regularPrice = regularPrice,
        salePrice = salePrice,
        discountPercentOverride = discountPercentOverride ?: existing?.discountPercentOverride,
        imageUrl = imageUrl?.takeIf { it.isNotBlank() } ?: existing?.imageUrl,
        galleryUrlsJson = encodeStringList(mergedGallery),
        shortDescription = shortDescription.ifBlank { existing?.shortDescription.orEmpty() },
        description = description.ifBlank { existing?.description.orEmpty() },
        weight = weight ?: existing?.weight,
        dimensions = dimensions.ifBlank { existing?.dimensions.orEmpty() },
        colorsJson = encodeColorList(mergedColors),
        sizesJson = encodeStringList(mergedSizes),
        variationsJson = encodeVariationList(mergedVariations),
        reviewsJson = encodeReviewList(mergedReviews),
        relatedProductIdsJson = encodeIntList(mergedRelatedIds),
        badge = badge ?: existing?.badge,
        rating = if (rating > 0.0) rating else (existing?.rating ?: 0.0),
        reviewCount = maxOf(reviewCount, existing?.reviewCount ?: 0),
        stockQuantity = stockQuantity,
        inStock = inStock,
        isFeatured = isFeatured || (existing?.isFeatured == true),
        isSponsored = isSponsored || (existing?.isSponsored == true),
        expressDelivery = expressDelivery || (existing?.expressDelivery == true),
        videoUrlsJson = encodeStringList(mergedVideos),
        hasFullDetails = isDetailed || (existing?.hasFullDetails == true),
        cachedAt = nowMs
    )
}

internal fun CachedProductEntity.toDomain(
    relatedProducts: List<Product> = emptyList()
): Product = Product(
    id = id,
    sku = sku,
    name = name,
    slug = slug,
    category = category,
    categorySlug = categorySlug,
    categoryId = categoryId,
    brandId = brandId,
    brandName = brandName,
    brandSlug = brandSlug,
    originCountry = originCountry,
    isImported = isImported,
    collection = collection,
    regularPrice = regularPrice,
    salePrice = salePrice,
    discountPercentOverride = discountPercentOverride,
    imageUrl = imageUrl,
    galleryUrls = decodeStringList(galleryUrlsJson),
    shortDescription = shortDescription,
    description = description,
    weight = weight,
    dimensions = dimensions,
    colors = decodeColorList(colorsJson),
    sizes = decodeStringList(sizesJson),
    variations = decodeVariationList(variationsJson),
    reviews = decodeReviewList(reviewsJson),
    relatedProducts = relatedProducts,
    badge = badge,
    rating = rating,
    reviewCount = reviewCount,
    stockQuantity = stockQuantity,
    inStock = inStock,
    isFeatured = isFeatured,
    isSponsored = isSponsored,
    expressDelivery = expressDelivery,
    videoUrls = decodeStringList(videoUrlsJson)
)

internal fun CachedProductEntity.relatedIds(): List<Int> = decodeIntList(relatedProductIdsJson)

internal fun Category.toCachedEntity(nowMs: Long = System.currentTimeMillis()): CachedCategoryEntity =
    CachedCategoryEntity(
        id = id,
        name = name,
        slug = slug,
        description = description,
        imageUrl = imageUrl,
        itemCount = itemCount,
        isFeatured = isFeatured,
        coverImageUrl = coverImageUrl,
        motifKey = motifKey,
        editorialBadge = editorialBadge,
        editorialSubtitle = editorialSubtitle,
        accentHex = accentHex,
        bgHex = bgHex,
        animationKey = animationKey,
        tileSpan = tileSpan,
        displayOrder = displayOrder,
        cachedAt = nowMs
    )

internal fun CachedCategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    slug = slug,
    description = description,
    imageUrl = imageUrl,
    itemCount = itemCount,
    isFeatured = isFeatured,
    coverImageUrl = coverImageUrl,
    motifKey = motifKey,
    editorialBadge = editorialBadge,
    editorialSubtitle = editorialSubtitle,
    accentHex = accentHex,
    bgHex = bgHex,
    animationKey = animationKey,
    tileSpan = tileSpan,
    displayOrder = displayOrder
)

private fun encodeStringList(values: List<String>): String =
    runCatching { ProductCacheJson.encodeToString(ListSerializer(String.serializer()), values) }
        .getOrDefault("[]")

private fun decodeStringList(raw: String): List<String> =
    if (raw.isBlank() || raw == "[]") emptyList()
    else runCatching { ProductCacheJson.decodeFromString(ListSerializer(String.serializer()), raw) }
        .getOrDefault(emptyList())

private fun encodeIntList(values: List<Int>): String =
    runCatching { ProductCacheJson.encodeToString(ListSerializer(Int.serializer()), values) }
        .getOrDefault("[]")

private fun decodeIntList(raw: String): List<Int> =
    if (raw.isBlank() || raw == "[]") emptyList()
    else runCatching { ProductCacheJson.decodeFromString(ListSerializer(Int.serializer()), raw) }
        .getOrDefault(emptyList())

private fun encodeColorList(values: List<ColorVariant>): String =
    runCatching {
        val payloads = values.map { CachedColorVariantPayload(it.name, it.hex) }
        ProductCacheJson.encodeToString(ListSerializer(CachedColorVariantPayload.serializer()), payloads)
    }.getOrDefault("[]")

private fun decodeColorList(raw: String): List<ColorVariant> =
    if (raw.isBlank() || raw == "[]") emptyList()
    else runCatching {
        ProductCacheJson.decodeFromString(ListSerializer(CachedColorVariantPayload.serializer()), raw)
            .map { ColorVariant(it.name, it.hex) }
    }.getOrDefault(emptyList())

private fun encodeVariationList(values: List<ProductVariation>): String =
    runCatching {
        val payloads = values.map {
            CachedProductVariationPayload(
                id = it.id,
                attributeName = it.attributeName,
                attributeValue = it.attributeValue,
                color = it.color,
                size = it.size,
                regularPrice = it.regularPrice,
                salePrice = it.salePrice,
                stockQuantity = it.stockQuantity,
                isActive = it.isActive,
                imageUrl = it.imageUrl
            )
        }
        ProductCacheJson.encodeToString(ListSerializer(CachedProductVariationPayload.serializer()), payloads)
    }.getOrDefault("[]")

private fun decodeVariationList(raw: String): List<ProductVariation> =
    if (raw.isBlank() || raw == "[]") emptyList()
    else runCatching {
        ProductCacheJson.decodeFromString(ListSerializer(CachedProductVariationPayload.serializer()), raw)
            .map {
                ProductVariation(
                    id = it.id,
                    attributeName = it.attributeName,
                    attributeValue = it.attributeValue,
                    color = it.color,
                    size = it.size,
                    regularPrice = it.regularPrice,
                    salePrice = it.salePrice,
                    stockQuantity = it.stockQuantity,
                    isActive = it.isActive,
                    imageUrl = it.imageUrl
                )
            }
    }.getOrDefault(emptyList())

private fun encodeReviewList(values: List<ProductReview>): String =
    runCatching {
        val payloads = values.map {
            CachedProductReviewPayload(
                id = it.id,
                userName = it.userName,
                rating = it.rating,
                title = it.title,
                comment = it.comment,
                isVerifiedPurchase = it.isVerifiedPurchase,
                helpfulCount = it.helpfulCount,
                hasVotedHelpful = it.hasVotedHelpful,
                mediaUrls = it.mediaUrls,
                createdAt = it.createdAt
            )
        }
        ProductCacheJson.encodeToString(ListSerializer(CachedProductReviewPayload.serializer()), payloads)
    }.getOrDefault("[]")

private fun decodeReviewList(raw: String): List<ProductReview> =
    if (raw.isBlank() || raw == "[]") emptyList()
    else runCatching {
        ProductCacheJson.decodeFromString(ListSerializer(CachedProductReviewPayload.serializer()), raw)
            .map {
                ProductReview(
                    id = it.id,
                    userName = it.userName,
                    rating = it.rating,
                    title = it.title,
                    comment = it.comment,
                    isVerifiedPurchase = it.isVerifiedPurchase,
                    helpfulCount = it.helpfulCount,
                    hasVotedHelpful = it.hasVotedHelpful,
                    mediaUrls = it.mediaUrls,
                    createdAt = it.createdAt
                )
            }
    }.getOrDefault(emptyList())
