package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductCacheDao {

    @Query("SELECT * FROM cached_products ORDER BY isFeatured DESC, cachedAt DESC, id DESC")
    fun observeAllProducts(): Flow<List<CachedProductEntity>>

    @Query("SELECT * FROM cached_products WHERE id = :productId LIMIT 1")
    fun observeProductById(productId: Int): Flow<CachedProductEntity?>

    @Query("SELECT COUNT(*) FROM cached_products")
    fun observeProductCount(): Flow<Int>

    @Query("SELECT MAX(cachedAt) FROM cached_products")
    fun observeLatestCachedAt(): Flow<Long?>

    @Query("SELECT * FROM cached_products ORDER BY isFeatured DESC, cachedAt DESC, id DESC")
    suspend fun getAllProducts(): List<CachedProductEntity>

    @Query("SELECT * FROM cached_products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Int): CachedProductEntity?

    @Query("SELECT * FROM cached_products WHERE id IN (:ids)")
    suspend fun getProductsByIds(ids: List<Int>): List<CachedProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: CachedProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(products: List<CachedProductEntity>)

    @Query("DELETE FROM cached_products WHERE cachedAt < :olderThanMs")
    suspend fun deleteStaleProducts(olderThanMs: Long)

    @Query("DELETE FROM cached_products")
    suspend fun clearProducts()

    @Query("SELECT * FROM cached_categories ORDER BY displayOrder ASC, name ASC")
    fun observeCategories(): Flow<List<CachedCategoryEntity>>

    @Query("SELECT * FROM cached_categories ORDER BY displayOrder ASC, name ASC")
    suspend fun getCategories(): List<CachedCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CachedCategoryEntity>)

    @Query("DELETE FROM cached_categories")
    suspend fun clearCategories()
}
