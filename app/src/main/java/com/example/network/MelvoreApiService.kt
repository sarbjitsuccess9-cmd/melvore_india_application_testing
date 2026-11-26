package com.example.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface MelvoreApiService {

    @GET("api/health.php")
    suspend fun getHealth(): HealthResponse

    @GET("design.php")
    suspend fun getDesignBundle(@Query("section") section: String = "all"): DesignBundleResponse

    // Catalog & Reviews
    @GET("api/products/list.php")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("category") category: String? = null,
        @Query("collection") collection: String? = null,
        @Query("brand_id") brandId: Int? = null,
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
        @Query("featured") featured: Int? = null,
        @Query("on_sale") onSale: Int? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null
    ): ProductsListResponse

    @GET("api/products/details.php")
    suspend fun getProductDetails(@Query("id") productId: Int): ProductDetailsResponse

    @GET("api/products/categories.php")
    suspend fun getCategories(): CategoriesResponse

    @GET("api/reviews/list.php")
    suspend fun getProductReviews(@Query("product_id") productId: Int): ReviewsListResponse

    // Auth & Profile
    @POST("api/auth/login.php")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/register.php")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/auth/social.php")
    suspend fun socialAuth(@Body body: SocialAuthRequest): AuthResponse

    @POST("api/auth/otp.php")
    suspend fun otpAuth(@Body body: OtpRequest): AuthResponse

    @POST("api/auth/forgot_password.php")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): ApiMessageResponse

    @POST("api/auth/logout.php")
    suspend fun logout(): ApiMessageResponse

    @GET("api/auth/profile.php")
    suspend fun getProfile(): ProfileResponse

    @PUT("api/auth/profile.php")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): ProfileResponse

    // Cart & Coupons
    @GET("api/cart/list.php")
    suspend fun getCart(): CartResponse

    @POST("api/cart/add.php")
    suspend fun addToCart(@Body body: CartAddRequest): CartResponse

    @POST("api/cart/update.php")
    suspend fun updateCart(@Body body: CartUpdateRequest): CartResponse

    @POST("api/cart/remove.php")
    suspend fun removeFromCart(@Body body: CartRemoveRequest): CartResponse

    @GET("api/coupons/validate.php")
    suspend fun listActiveCoupons(): CouponsListResponse

    @POST("api/coupons/validate.php")
    suspend fun validateCoupon(@Body body: CouponValidateRequest): CouponValidateResponse

    // Wishlist
    @GET("api/wishlist/list.php")
    suspend fun getWishlist(): WishlistResponse

    @POST("api/wishlist/add.php")
    suspend fun addToWishlist(@Body body: WishlistMutateRequest): WishlistResponse

    @POST("api/wishlist/remove.php")
    suspend fun removeFromWishlist(@Body body: WishlistMutateRequest): WishlistResponse

    // Addresses
    @GET("api/addresses/list.php")
    suspend fun getAddresses(): AddressListResponse

    @POST("api/addresses/save.php")
    suspend fun saveAddress(@Body body: AddressSaveRequest): AddressSaveResponse

    @POST("api/addresses/delete.php")
    suspend fun deleteAddress(@Body body: AddressDeleteRequest): ApiMessageResponse

    // Orders
    @POST("api/orders/create.php")
    suspend fun createOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: OrderCreateRequest
    ): OrderCreateResponse

    @GET("api/orders/history.php")
    suspend fun getOrderHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): OrdersHistoryResponse

    @GET("api/orders/details.php")
    suspend fun getOrderDetails(@Query("order_id") orderId: Int): OrderDetailsResponse

    @GET("api/orders/track.php")
    suspend fun trackOrder(
        @Query("order_number") orderNumber: String,
        @Query("pincode") pincode: String? = null,
        @Query("mobile") mobile: String? = null
    ): OrderTrackResponse

    @POST("api/orders/cancel.php")
    suspend fun cancelOrder(@Body body: OrderCancelRequest): OrderCancelResponse

    // Payments (Razorpay)
    @POST("api/payments/initiate.php")
    suspend fun initiatePayment(@Body body: PaymentInitiateRequest): PaymentInitiateResponse

    @POST("api/payments/verify.php")
    suspend fun verifyPayment(@Body body: PaymentVerifyRequest): PaymentVerifyResponse

    @POST("api/payments/fail.php")
    suspend fun failPayment(@Body body: PaymentFailRequest): PaymentFailResponse

    // Content, Support & Contact
    @GET("api/blog/list.php")
    suspend fun getBlogList(): BlogListResponse

    @GET("api/blog/details.php")
    suspend fun getBlogDetails(@Query("id") postId: Int): BlogDetailsResponse

    @GET("api/cms/faqs.php")
    suspend fun getFaqs(): FaqsResponse

    @GET("api/cms/page.php")
    suspend fun getCmsPage(@Query("slug") slug: String = "about"): CmsPageResponse

    @GET("api/support/tickets.php")
    suspend fun getSupportTickets(): SupportTicketsResponse

    @POST("api/support/create.php")
    suspend fun createSupportTicket(@Body body: SupportCreateRequest): SupportCreateResponse

    @POST("api/support/reply.php")
    suspend fun replySupportTicket(@Body body: SupportReplyRequest): SupportReplyResponse

    @POST("api/forms/contact.php")
    suspend fun submitContact(@Body body: ContactRequest): ContactResponse
}
