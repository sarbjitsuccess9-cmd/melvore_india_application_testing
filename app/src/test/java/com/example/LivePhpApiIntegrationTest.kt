package com.example

import com.example.data.MelvoreRepository
import com.example.data.RepoResult
import com.example.data.SecureSessionStore
import com.example.model.Address
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LivePhpApiIntegrationTest {

    private fun createFreshRepo(): MelvoreRepository {
        val context = RuntimeEnvironment.getApplication()
        val store = SecureSessionStore(context)
        store.clearAuthToken()
        store.setPendingPaymentOrderId(null)
        store.setApiBaseUrl("https://app.melvore.co.in")
        return MelvoreRepository(store)
    }

    @Test
    fun verifyLiveEndToEndPhpApiIntegration() = runTest(timeout = 90.seconds) {
        val repo = createFreshRepo()

        // 1. Verify API connectivity
        val healthRes = repo.checkHealth()
        assertTrue("Expected health.php success, got: $healthRes", healthRes is RepoResult.Success)
        val designRes = repo.fetchDesignHub()
        assertTrue("Expected design.php success, got: $designRes", designRes is RepoResult.Success)
        val design = (designRes as RepoResult.Success).data
        assertTrue("Expected brandName to be populated", design.design.siteName.isNotBlank())
        println("[TEST 1 PASS] Connected to ${repo.getApiBaseUrl()} (${design.design.siteName})")

        // 2. Fetch real products & product details from existing database
        val productsRes = repo.fetchProducts(page = 1, limit = 50)
        assertTrue("Expected products/list.php success, got: $productsRes", productsRes is RepoResult.Success)
        val products = (productsRes as RepoResult.Success).data
        assertTrue("Expected non-empty catalog from MySQL", products.isNotEmpty())

        val targetSummary = products.firstOrNull { it.inStock && it.stockQuantity > 0 && it.effectivePrice > 0.0 }
            ?: products.first()
        val detailRes = repo.fetchProductDetails(targetSummary.id)
        assertTrue("Expected products/details.php success, got: $detailRes", detailRes is RepoResult.Success)
        val productDetail = (detailRes as RepoResult.Success).data
        assertEquals(targetSummary.id, productDetail.id)
        assertTrue(productDetail.name.isNotBlank())

        val activeVariation = productDetail.variations.firstOrNull { it.isActive && it.stockQuantity > 0 }
        val chosenColor = activeVariation?.color?.ifBlank { null }
            ?: productDetail.colors.firstOrNull()?.name
        val chosenSize = activeVariation?.size?.ifBlank { null }
            ?: productDetail.sizes.firstOrNull()
        println("[TEST 2 PASS] Fetched ${products.size} products; inspected #${productDetail.id} '${productDetail.name}'")

        // 3. Register a fresh test customer
        val stamp = System.currentTimeMillis()
        val testEmail = "android.e2e.$stamp@melvore.co.in"
        val testPhone = "98${(stamp % 100_000_000L).toString().padStart(8, '0')}"
        val testPassword = "MelvoreTest#${stamp % 10_000}"

        val regRes = repo.register(
            name = "Melvore Android Test",
            email = testEmail,
            phone = testPhone,
            password = testPassword
        )
        assertTrue("Expected auth/register.php success, got: $regRes", regRes is RepoResult.Success)
        assertTrue("Expected Bearer token stored after registration", repo.hasAuthToken())
        println("[TEST 3 PASS] Registered test customer: $testEmail")

        // 4. Logout, Log in, and retrieve customer profile
        repo.logout()
        assertFalse("Expected auth token cleared on logout", repo.hasAuthToken())

        val loginRes = repo.login(email = testEmail, password = testPassword)
        assertTrue("Expected auth/login.php success, got: $loginRes", loginRes is RepoResult.Success)
        assertEquals(testEmail, (loginRes as RepoResult.Success).data.email)

        val profileRes = repo.fetchProfile()
        assertTrue("Expected auth/profile.php success, got: $profileRes", profileRes is RepoResult.Success)
        val profile = (profileRes as RepoResult.Success).data
        assertEquals(testEmail, profile.user.email)
        assertEquals("Melvore Android Test", profile.user.name)
        println("[TEST 4 PASS] Logged in and verified profile for userId=${profile.user.id}")

        // 5. Add and remove cart items
        val addRes = repo.addToCart(
            productId = productDetail.id,
            variationId = activeVariation?.id,
            quantity = 1,
            color = chosenColor,
            size = chosenSize
        )
        assertTrue("Expected cart/add.php success, got: $addRes", addRes is RepoResult.Success)
        val addedCart = (addRes as RepoResult.Success).data
        assertTrue("Expected added item in cart", addedCart.items.any { it.productId == productDetail.id })
        val cartItemId = addedCart.items.first { it.productId == productDetail.id }.cartId

        val removeRes = repo.removeCartItem(cartItemId)
        assertTrue("Expected cart/remove.php success, got: $removeRes", removeRes is RepoResult.Success)
        val afterRemove = (removeRes as RepoResult.Success).data
        assertFalse("Expected item removed from cart", afterRemove.items.any { it.cartId == cartItemId })
        println("[TEST 5 PASS] Added cartId=$cartItemId and removed it cleanly")

        // 6. Validate a real backend coupon
        val reAddRes = repo.addToCart(
            productId = productDetail.id,
            variationId = activeVariation?.id,
            quantity = 1,
            color = chosenColor,
            size = chosenSize
        )
        assertTrue("Expected cart/add.php success before coupon check, got: $reAddRes", reAddRes is RepoResult.Success)
        val currentSubtotal = (reAddRes as RepoResult.Success).data.summary.subtotal

        val couponsRes = repo.fetchActiveCoupons()
        assertTrue("Expected GET coupons/validate.php success, got: $couponsRes", couponsRes is RepoResult.Success)
        val activeCoupons = (couponsRes as RepoResult.Success).data
        assertTrue("Expected at least one active coupon on backend", activeCoupons.isNotEmpty())

        val qualifyingCoupon = activeCoupons.firstOrNull { it.minOrderAmount <= currentSubtotal } ?: activeCoupons.first()
        // If minOrderAmount exceeds single unit price, verify or adjust quantity temporarily
        if (qualifyingCoupon.minOrderAmount > currentSubtotal && productDetail.effectivePrice > 0.0) {
            val neededQty = ((qualifyingCoupon.minOrderAmount / productDetail.effectivePrice).toInt() + 1)
                .coerceAtMost(productDetail.stockQuantity.coerceAtLeast(1))
            if (neededQty > 1) {
                val cid = reAddRes.data.items.first().cartId
                repo.updateCartQty(cid, neededQty - 1)
            }
        }
        val couponEval = repo.validateCoupon(qualifyingCoupon.code, paymentMethod = "online")
        assertTrue(
            "Expected active coupon '${qualifyingCoupon.code}' validation true, got: ${couponEval.message}",
            couponEval.valid
        )
        assertEquals(qualifyingCoupon.code, couponEval.code)

        val invalidCouponEval = repo.validateCoupon("NONEXISTENT_CODE_XYZ", paymentMethod = "online")
        assertFalse("Expected bogus coupon to be rejected", invalidCouponEval.valid)
        println("[TEST 6 PASS] Validated live coupon '${qualifyingCoupon.code}' (discount=${couponEval.discountAmount}) & rejected invalid code")

        // Normalize cart back to quantity 1 for order creation tests
        val freshCart = (repo.fetchCart() as RepoResult.Success).data
        freshCart.items.forEach { repo.removeCartItem(it.cartId) }
        val codCart = (repo.addToCart(
            productId = productDetail.id,
            variationId = activeVariation?.id,
            quantity = 1,
            color = chosenColor,
            size = chosenSize
        ) as RepoResult.Success).data
        assertTrue(codCart.items.isNotEmpty())

        // Save delivery address first
        val addrRes = repo.saveAddress(
            Address(
                id = 0,
                label = "home",
                fullName = "Melvore Integration QA",
                phone = testPhone,
                street = "14 Atelier Road, Civil Lines",
                addressLine2 = "Near North Campus",
                city = "New Delhi",
                state = "Delhi",
                pincode = "110033",
                isDefault = true
            )
        )
        assertTrue("Expected addresses/save.php success, got: $addrRes", addrRes is RepoResult.Success)
        val savedAddress = (addrRes as RepoResult.Success).data
        assertTrue(savedAddress.id > 0)

        // 7. Create a test COD order & verify tracking + cleanup cancel
        val codOrderRes = repo.createOrder(
            shippingAddressId = savedAddress.id,
            paymentMethod = "cod",
            couponCode = "",
            notes = "Automated Android COD verification test",
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertTrue("Expected COD orders/create.php success, got: $codOrderRes", codOrderRes is RepoResult.Success)
        val codOrder = (codOrderRes as RepoResult.Success).data
        assertTrue(codOrder.orderNumber.isNotBlank())
        assertEquals("cod", codOrder.paymentMethod.lowercase())

        val trackCodRes = repo.trackOrder(codOrder.orderNumber)
        assertTrue("Expected orders/track.php success for COD order, got: $trackCodRes", trackCodRes is RepoResult.Success)
        assertEquals(codOrder.orderNumber, (trackCodRes as RepoResult.Success).data.orderNumber)

        val cancelCodRes = repo.cancelOrder(codOrder.id, "Automated integration test cleanup")
        assertTrue("Expected COD order cancel cleanup success, got: $cancelCodRes", cancelCodRes is RepoResult.Success)
        println("[TEST 7 PASS] Created COD order #${codOrder.orderNumber} (id=${codOrder.id}), verified tracking, and cancelled cleanly")

        // 8 & 9. Create Online order, test Razorpay Test Mode initiate, verify payment recovery & cancel cleanup
        val onlineCartRes = repo.addToCart(
            productId = productDetail.id,
            variationId = activeVariation?.id,
            quantity = 1,
            color = chosenColor,
            size = chosenSize
        )
        assertTrue("Expected cart/add.php success for online order, got: $onlineCartRes", onlineCartRes is RepoResult.Success)

        val onlineOrderRes = repo.createOrder(
            shippingAddressId = savedAddress.id,
            paymentMethod = "online",
            couponCode = "",
            notes = "Automated Razorpay Test Mode verification",
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertTrue("Expected online orders/create.php success, got: $onlineOrderRes", onlineOrderRes is RepoResult.Success)
        val onlineOrder = (onlineOrderRes as RepoResult.Success).data
        assertEquals(onlineOrder.id, repo.sessionStore.getPendingPaymentOrderId())

        val rzpInitRes = repo.initiateRazorpayPayment(onlineOrder.id)
        assertTrue("Expected payments/initiate.php success, got: $rzpInitRes", rzpInitRes is RepoResult.Success)
        val payload = (rzpInitRes as RepoResult.Success).data
        assertTrue("Expected Razorpay test key, got '${payload.keyId}'", payload.keyId.startsWith("rzp_"))
        assertTrue("Expected Razorpay order_id, got '${payload.razorpayOrderId}'", payload.razorpayOrderId.startsWith("order_"))
        assertTrue("Expected positive amountPaise", payload.amountPaise > 0L)
        assertEquals("INR", payload.currency)
        println("[TEST 8 PASS] Initiated Razorpay Test Mode order ${payload.razorpayOrderId} (key=${payload.keyId}, paise=${payload.amountPaise}) for #${onlineOrder.orderNumber}")

        // Simulate failure -> verify order stays pending & recoverable
        val failPendingRes = repo.failOrCancelPayment(onlineOrder.id, cancelOrder = false, reason = "Automated recovery probe")
        assertTrue("Expected payments/fail.php action=fail success, got: $failPendingRes", failPendingRes is RepoResult.Success)
        assertNotNull("Expected pendingPaymentOrderId preserved for recovery", repo.sessionStore.getPendingPaymentOrderId())

        val detailPendingRes = repo.fetchOrderDetails(onlineOrder.id)
        assertTrue("Expected orders/details.php success, got: $detailPendingRes", detailPendingRes is RepoResult.Success)
        val pendingOrder = (detailPendingRes as RepoResult.Success).data
        assertEquals("pending", pendingOrder.paymentStatus.lowercase())
        assertFalse(pendingOrder.status.equals("cancelled", ignoreCase = true))

        // Cancel unpaid order -> restores stock and clears pending recovery ID
        val cancelUnpaidRes = repo.failOrCancelPayment(onlineOrder.id, cancelOrder = true, reason = "Automated test cleanup")
        assertTrue("Expected payments/fail.php action=cancel_order success, got: $cancelUnpaidRes", cancelUnpaidRes is RepoResult.Success)
        assertNull("Expected pendingPaymentOrderId cleared after cancel", repo.sessionStore.getPendingPaymentOrderId())

        val finalDetail = (repo.fetchOrderDetails(onlineOrder.id) as RepoResult.Success).data
        assertEquals("cancelled", finalDetail.status.lowercase())
        println("[TEST 9 PASS] Verified payment recovery status & clean unpaid order cancellation for #${onlineOrder.orderNumber}")

        // 10. Verify forgot_password.php uses valid actions ('request' & 'verify', not 'reset')
        val fpReqRes = repo.requestPasswordReset(testEmail)
        if (fpReqRes is RepoResult.Error && fpReqRes.httpStatus == 429) {
            println("[TEST 10 INFO] forgot_password.php rate-limited (429): ${fpReqRes.message}")
        } else {
            assertTrue(
                "Expected forgot_password.php action=request success, got: $fpReqRes",
                fpReqRes is RepoResult.Success
            )
        }
        val fpVerifyRes = repo.resetPassword(testEmail, "000000", "MelvoreReset#999")
        assertTrue("Expected RepoResult.Error for dummy OTP, got: $fpVerifyRes", fpVerifyRes is RepoResult.Error)
        val fpErr = fpVerifyRes as RepoResult.Error
        assertFalse(
            "Must never return Invalid action / VALIDATION_ERROR: $fpErr",
            fpErr.errorCode == "VALIDATION_ERROR" || fpErr.message.contains("Invalid action", ignoreCase = true)
        )
        assertTrue(
            "Expected INVALID_OTP (confirming action='verify' accepted) or 429 rate-limit on repeat runs, got: $fpErr",
            fpErr.errorCode == "INVALID_OTP" || fpErr.httpStatus == 429
        )
        println("[TEST 10 PASS] Verified forgot_password.php 'request' & 'verify' accepted (${fpErr.errorCode})")

        // 11. Verify social.php rejects malformed Google ID token with INVALID_SOCIAL_TOKEN (no simulated fallback)
        val socialRes = repo.socialGoogleLogin("invalid.google.id.token")
        assertTrue("Expected RepoResult.Error for invalid Google token, got: $socialRes", socialRes is RepoResult.Error)
        val socialErr = socialRes as RepoResult.Error
        assertEquals("INVALID_SOCIAL_TOKEN", socialErr.errorCode)
        println("[TEST 11 PASS] Verified social.php server-side token validation (${socialErr.errorCode})")
    }
}
