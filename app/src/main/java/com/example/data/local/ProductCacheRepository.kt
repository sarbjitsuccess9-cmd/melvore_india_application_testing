package com.example.data.local

import com.example.model.Category
import com.example.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Local Room-backed cache repository for Melvore product catalog & detail records.
 * Abstracts all DAO access away from ViewModels/UI, preserves full detail fields across
 * lightweight list refreshes, and supports offline filtering, searching, and sorting.
 */
class ProductCacheRepository(
    private val dao: ProductCacheDao
) {
    val cachedProductsFlow: Flow<List<Product>> = dao.observeAllProducts()
        .map { entities -> entities.map { it.toDomain() } }

    val cachedCategoriesFlow: Flow<List<Category>> = dao.observeCategories()
        .map { entities -> entities.map { it.toDomain() } }

    val cachedProductCountFlow: Flow<Int> = dao.observeProductCount()

    val lastSyncedAtFlow: Flow<Long?> = dao.observeLatestCachedAt()

    fun observeProduct(productId: Int): Flow<Product?> =
        dao.observeProductById(productId).map { it?.toDomain() }

    suspend fun cacheProducts(
        products: List<Product>,
        isDetailed: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val valid = products.filter { it.id > 0 }.distinctBy { it.id }
        if (valid.isEmpty()) return@withContext

        val now = System.currentTimeMillis()
        val existingById = dao.getProductsByIds(valid.map { it.id }).associateBy { it.id }
        val entities = valid.map { product ->
            product.toCachedEntity(
                isDetailed = isDetailed,
                existing = existingById[product.id],
                nowMs = now
            )
        }
        dao.upsertProducts(entities)

        // Also warm cache with any nested related products when present
        val nestedRelated = valid.flatMap { it.relatedProducts }
            .filter { it.id > 0 && it.id !in existingById }
            .distinctBy { it.id }
        if (nestedRelated.isNotEmpty()) {
            val nestedExisting = dao.getProductsByIds(nestedRelated.map { it.id }).associateBy { it.id }
            dao.upsertProducts(
                nestedRelated.map { related ->
                    related.toCachedEntity(
                        isDetailed = false,
                        existing = nestedExisting[related.id],
                        nowMs = now
                    )
                }
            )
        }
    }

    suspend fun cacheProductDetail(product: Product) = withContext(Dispatchers.IO) {
        if (product.id <= 0) return@withContext
        val now = System.currentTimeMillis()
        val existing = dao.getProductById(product.id)
        dao.upsertProduct(
            product.toCachedEntity(
                isDetailed = true,
                existing = existing,
                nowMs = now
            )
        )
        if (product.relatedProducts.isNotEmpty()) {
            val related = product.relatedProducts.filter { it.id > 0 && it.id != product.id }.distinctBy { it.id }
            if (related.isNotEmpty()) {
                val relatedExisting = dao.getProductsByIds(related.map { it.id }).associateBy { it.id }
                dao.upsertProducts(
                    related.map { rel ->
                        rel.toCachedEntity(
                            isDetailed = false,
                            existing = relatedExisting[rel.id],
                            nowMs = now
                        )
                    }
                )
            }
        }
    }

    suspend fun cacheCategories(categories: List<Category>) = withContext(Dispatchers.IO) {
        val valid = categories.filter { it.id > 0 }.distinctBy { it.id }
        if (valid.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        dao.upsertCategories(valid.map { it.toCachedEntity(now) })
    }

    suspend fun getCachedCategories(): List<Category> = withContext(Dispatchers.IO) {
        dao.getCategories().map { it.toDomain() }
    }

    suspend fun getCachedProductById(productId: Int): Product? = withContext(Dispatchers.IO) {
        if (productId <= 0) return@withContext null
        val entity = dao.getProductById(productId) ?: return@withContext null
        val relatedIds = entity.relatedIds().filter { it > 0 && it != productId }
        val related = if (relatedIds.isNotEmpty()) {
            val byId = dao.getProductsByIds(relatedIds).associateBy { it.id }
            relatedIds.mapNotNull { byId[it]?.toDomain() }
        } else {
            emptyList()
        }
        entity.toDomain(relatedProducts = related)
    }

    suspend fun hasCachedProducts(): Boolean = withContext(Dispatchers.IO) {
        dao.getAllProducts().isNotEmpty()
    }

    /**
     * Queries Room-cached products with full client-side filtering, searching, and sorting
     * so Home, Shop, Category, Brand, Collection, and Search screens keep working offline.
     */
    suspend fun queryCachedProducts(
        category: String? = null,
        collection: String? = null,
        brandId: Int? = null,
        search: String? = null,
        sort: String? = null,
        featured: Int? = null,
        onSale: Int? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        limit: Int = 100
    ): List<Product> = withContext(Dispatchers.IO) {
        val all = dao.getAllProducts().map { it.toDomain() }
        if (all.isEmpty()) return@withContext emptyList()

        val cleanCategory = category?.trim()?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotEmpty() && it != "all" }
        val cleanCollection = collection?.trim()?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotEmpty() && it != "all" }
        val cleanBrandId = brandId?.takeIf { it > 0 }
        val cleanSearch = search?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
        val cleanSort = sort?.trim()?.lowercase(Locale.ROOT).orEmpty()

        val filtered = all.filter { product ->
            val categoryOk = if (cleanCategory == null) {
                true
            } else {
                val catSlug = product.categorySlug.lowercase(Locale.ROOT)
                val catName = product.category.lowercase(Locale.ROOT)
                val catNameSlug = catName.replace(" ", "-")
                catSlug == cleanCategory ||
                    catName == cleanCategory ||
                    catNameSlug == cleanCategory ||
                    catSlug.contains(cleanCategory) ||
                    catName.contains(cleanCategory.replace("-", " "))
            }

            val collectionOk = when (cleanCollection) {
                null -> true
                "sale", "up-to-90-off" -> product.hasDiscount
                "under-999" -> product.effectivePrice <= 999.0
                "under-1999" -> product.effectivePrice <= 1999.0
                "featured" -> product.isFeatured
                else -> {
                    val col = product.collection.lowercase(Locale.ROOT)
                    val colSlug = col.replace(" ", "-")
                    col == cleanCollection ||
                        colSlug == cleanCollection ||
                        col.contains(cleanCollection.replace("-", " "))
                }
            }

            val brandOk = cleanBrandId == null || product.brandId == cleanBrandId

            val searchOk = if (cleanSearch == null) {
                true
            } else {
                val tokens = cleanSearch.split(Regex("\\s+")).filter { it.isNotBlank() }
                val haystack = buildString {
                    append(product.name).append(' ')
                    append(product.brandName).append(' ')
                    append(product.category).append(' ')
                    append(product.collection).append(' ')
                    append(product.sku).append(' ')
                    append(product.shortDescription).append(' ')
                    append(product.description)
                }.lowercase(Locale.ROOT)
                tokens.all { token -> haystack.contains(token) }
            }

            val featuredOk = featured == null || featured <= 0 || product.isFeatured
            val onSaleOk = onSale == null || onSale <= 0 || product.hasDiscount
            val minOk = minPrice == null || product.effectivePrice >= minPrice
            val maxOk = maxPrice == null || product.effectivePrice <= maxPrice

            categoryOk && collectionOk && brandOk && searchOk && featuredOk && onSaleOk && minOk && maxOk
        }

        val sorted = when (cleanSort) {
            "price_asc" -> filtered.sortedBy { it.effectivePrice }
            "price_desc" -> filtered.sortedByDescending { it.effectivePrice }
            "rating" -> filtered.sortedWith(
                compareByDescending<Product> { it.rating }.thenByDescending { it.reviewCount }
            )
            "newest" -> filtered.sortedByDescending { it.id }
            else -> filtered
        }

        if (limit > 0) sorted.take(limit) else sorted
    }

    suspend fun pruneOlderThan(maxAgeMs: Long) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        dao.deleteStaleProducts(cutoff)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        dao.clearProducts()
        dao.clearCategories()
    }
}
