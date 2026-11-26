package com.example

import com.example.data.MelvoreRepository
import com.example.data.RepoResult
import com.example.data.SecureSessionStore
import com.example.data.local.MelvoreDatabase
import com.example.data.local.ProductCacheRepository
import com.example.model.Category
import com.example.model.ColorVariant
import com.example.model.Product
import com.example.model.ProductReview
import com.example.model.ProductVariation
import com.example.network.MelvoreApiClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductCacheRoomTest {

    private lateinit var database: MelvoreDatabase
    private lateinit var cacheRepository: ProductCacheRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = MelvoreDatabase.inMemory(context)
        cacheRepository = ProductCacheRepository(database.productCacheDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleProducts(): List<Product> = listOf(
        Product(
            id = 101,
            sku = "MLV-JKT-01",
            name = "Noir Cropped Leather Jacket",
            slug = "noir-cropped-leather-jacket",
            category = "Outerwear",
            categorySlug = "outerwear",
            categoryId = 1,
            brandId = 10,
            brandName = "Atelier Noir",
            collection = "autumn-edit",
            regularPrice = 4999.0,
            salePrice = 3499.0,
            imageUrl = "https://melvore.co.in/uploads/jacket.jpg",
            shortDescription = "Structured lambskin moto silhouette",
            badge = "BESTSELLER",
            rating = 4.9,
            reviewCount = 42,
            stockQuantity = 12,
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = 102,
            sku = "MLV-TEE-02",
            name = "Cyber Chrome Graphic Tee",
            slug = "cyber-chrome-graphic-tee",
            category = "Tops",
            categorySlug = "tops",
            categoryId = 2,
            brandId = 20,
            brandName = "Neo Seoul",
            collection = "y2k-drop",
            regularPrice = 1199.0,
            salePrice = 899.0,
            imageUrl = "https://melvore.co.in/uploads/tee.jpg",
            shortDescription = "Heavyweight cotton boxy tee",
            badge = "NEW",
            rating = 4.5,
            reviewCount = 19,
            stockQuantity = 25,
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = 103,
            sku = "MLV-DRS-03",
            name = "Satin Ribbon Midi Dress",
            slug = "satin-ribbon-midi-dress",
            category = "Dresses",
            categorySlug = "dresses",
            categoryId = 3,
            brandId = 10,
            brandName = "Atelier Noir",
            collection = "coquette-atelier",
            regularPrice = 2499.0,
            salePrice = null,
            imageUrl = "https://melvore.co.in/uploads/dress.jpg",
            shortDescription = "Bias-cut silk satin midi",
            rating = 4.7,
            reviewCount = 31,
            stockQuantity = 8,
            inStock = true,
            isFeatured = true
        )
    )

    @Test
    fun roomCacheEmitsReactiveFlowsAndPreservesFullProductDetailsAcrossListRefreshes() = runTest {
        val relatedItem = sampleProducts()[1]
        val fullDetail = sampleProducts()[0].copy(
            description = "<p>Hand-finished full-grain leather with satin lining.</p>",
            galleryUrls = listOf(
                "https://melvore.co.in/uploads/jacket.jpg",
                "https://melvore.co.in/uploads/jacket-back.jpg"
            ),
            colors = listOf(ColorVariant("Obsidian", "#0B0B0F")),
            sizes = listOf("S", "M", "L"),
            variations = listOf(
                ProductVariation(
                    id = 9001,
                    color = "Obsidian",
                    size = "M",
                    regularPrice = 4999.0,
                    salePrice = 3499.0,
                    stockQuantity = 6,
                    isActive = true
                )
            ),
            reviews = listOf(
                ProductReview(
                    id = 501,
                    userName = "Aanya",
                    rating = 5,
                    title = "Runway quality",
                    comment = "Incredible drape and finish.",
                    isVerifiedPurchase = true
                )
            ),
            relatedProducts = listOf(relatedItem),
            videoUrls = listOf("https://melvore.co.in/uploads/jacket-reel.mp4")
        )

        cacheRepository.cacheProductDetail(fullDetail)

        // Lightweight catalog list refresh arrives with updated price but omitted detail fields
        val lightweightListUpdate = sampleProducts()[0].copy(
            salePrice = 3299.0,
            description = "",
            galleryUrls = emptyList(),
            variations = emptyList(),
            reviews = emptyList(),
            relatedProducts = emptyList()
        )
        cacheRepository.cacheProducts(listOf(lightweightListUpdate, sampleProducts()[2]), isDetailed = false)

        val reactiveList = cacheRepository.cachedProductsFlow.first()
        assertEquals(3, reactiveList.size)
        assertEquals(3, cacheRepository.cachedProductCountFlow.first())

        // Full details must remain intact while picking up the fresh salePrice
        val restored = cacheRepository.getCachedProductById(101)
        assertNotNull(restored)
        assertEquals(3299.0, restored!!.salePrice!!, 0.01)
        assertTrue(restored.description.contains("full-grain leather"))
        assertEquals(2, restored.galleryUrls.size)
        assertEquals(listOf("S", "M", "L"), restored.sizes)
        assertEquals(1, restored.variations.size)
        assertEquals(1, restored.reviews.size)
        assertEquals(1, restored.relatedProducts.size)
        assertEquals(102, restored.relatedProducts.first().id)
        assertEquals(1, restored.videoUrls.size)
    }

    @Test
    fun offlineQueryFiltersAndSortsCachedProductsAccurately() = runTest {
        cacheRepository.cacheProducts(sampleProducts(), isDetailed = false)

        // Filter by category
        val tops = cacheRepository.queryCachedProducts(category = "tops")
        assertEquals(listOf(102), tops.map { it.id })

        // Filter by under-999 collection
        val under999 = cacheRepository.queryCachedProducts(collection = "under-999")
        assertEquals(listOf(102), under999.map { it.id })

        // Filter by sale + sort price ascending
        val onSaleAsc = cacheRepository.queryCachedProducts(onSale = 1, sort = "price_asc")
        assertEquals(listOf(102, 101), onSaleAsc.map { it.id })

        // Filter by brandId + sort price descending
        val atelierDesc = cacheRepository.queryCachedProducts(brandId = 10, sort = "price_desc")
        assertEquals(listOf(101, 103), atelierDesc.map { it.id })

        // Multi-token search across brand & title
        val searchHits = cacheRepository.queryCachedProducts(search = "atelier leather")
        assertEquals(listOf(101), searchHits.map { it.id })

        // Sort by rating
        val byRating = cacheRepository.queryCachedProducts(sort = "rating")
        assertEquals(listOf(101, 103, 102), byRating.map { it.id })
    }

    @Test
    fun melvoreRepositoryFallsBackToRoomCacheWhenNetworkIsUnavailable() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val sessionStore = SecureSessionStore(context)
        // Point API client to an unreachable local port to simulate offline state deterministically
        sessionStore.setApiBaseUrl("http://127.0.0.1:1")
        val offlineClient = MelvoreApiClient(sessionStore)
        val repository = MelvoreRepository(
            sessionStore = sessionStore,
            apiClient = offlineClient,
            productCache = cacheRepository
        )

        // 1. With empty Room cache and no network, errors are surfaced cleanly
        val emptyFetch = repository.fetchProducts(page = 1, limit = 20)
        assertTrue(emptyFetch is RepoResult.Error)
        val emptyDetail = repository.fetchProductDetails(101)
        assertTrue(emptyDetail is RepoResult.Error)

        // 2. Populate Room cache (simulating an earlier online session)
        cacheRepository.cacheProducts(sampleProducts(), isDetailed = false)
        cacheRepository.cacheCategories(
            listOf(
                Category(id = 1, name = "Outerwear", slug = "outerwear", itemCount = 14),
                Category(id = 2, name = "Tops", slug = "tops", itemCount = 22)
            )
        )

        // 3. Offline fetchProducts serves filtered/sorted data from Room with fromCache = true
        val cachedProductsRes = repository.fetchProducts(
            page = 1,
            limit = 20,
            sort = "price_asc"
        )
        assertTrue(cachedProductsRes is RepoResult.Success)
        val successProducts = cachedProductsRes as RepoResult.Success
        assertTrue(successProducts.fromCache)
        assertEquals(listOf(102, 103, 101), successProducts.data.map { it.id })

        // 4. Offline fetchProductDetails serves cached product with fromCache = true
        val cachedDetailRes = repository.fetchProductDetails(101)
        assertTrue(cachedDetailRes is RepoResult.Success)
        val successDetail = cachedDetailRes as RepoResult.Success
        assertTrue(successDetail.fromCache)
        assertEquals("Noir Cropped Leather Jacket", successDetail.data.name)

        // 5. Offline fetchCategories serves cached categories with fromCache = true
        val cachedCatsRes = repository.fetchCategories()
        assertTrue(cachedCatsRes is RepoResult.Success)
        val successCats = cachedCatsRes as RepoResult.Success
        assertTrue(successCats.fromCache)
        assertEquals(2, successCats.data.categories.size)
        assertFalse(repository.cachedCategoriesFlow.first().isEmpty())
    }
}
