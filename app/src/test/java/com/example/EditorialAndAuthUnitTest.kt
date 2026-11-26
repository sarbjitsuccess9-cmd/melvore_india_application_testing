package com.example

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.auth.GoogleCredentialHelper
import com.example.auth.GoogleSignInOutcome
import com.example.model.HeroSlide
import com.example.network.DesignBundleResponse
import com.example.network.DtoMappers.toDomain
import com.example.network.HeroSlideDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorialAndAuthUnitTest {

    @Test
    fun heroSlidesFilterActiveSortByDisplayOrderAndPreserveImageOnlyMode() {
        val dto = DesignBundleResponse(
            heroSlides = listOf(
                HeroSlideDto(
                    id = 30,
                    eyebrow = "",
                    title = "",
                    subtitle = "",
                    imageUrl = "/uploads/hero3.jpg",
                    mobileImageUrl = "/uploads/hero3_mobile.jpg",
                    desktopImageUrl = "/uploads/hero3_desktop.jpg",
                    ctaText = "",
                    ctaLink = "/collections/winter",
                    displayOrder = 2,
                    isActive = JsonPrimitive(1)
                ),
                HeroSlideDto(
                    id = 10,
                    eyebrow = "26 FW",
                    title = "Sleek Editorial",
                    subtitle = "Crafted silhouettes",
                    imageUrl = "/uploads/hero1.jpg",
                    displayOrder = 1,
                    isActive = JsonPrimitive(true)
                ),
                HeroSlideDto(
                    id = 20,
                    title = "Inactive Slide",
                    imageUrl = "/uploads/hidden.jpg",
                    displayOrder = 0,
                    isActive = JsonPrimitive(0)
                )
            )
        )

        val domain = dto.toDomain()
        assertEquals(2, domain.heroSlides.size)

        val first = domain.heroSlides[0]
        assertEquals(10, first.id)
        assertTrue(first.hasAnyText)
        assertEquals("https://melvore.co.in/uploads/hero1.jpg", first.resolveImageUrl(isWideLayout = false))

        val second = domain.heroSlides[1]
        assertEquals(30, second.id)
        assertFalse("Image-only slide should have hasAnyText = false", second.hasAnyText)
        assertEquals(
            "https://melvore.co.in/uploads/hero3_mobile.jpg",
            second.resolveImageUrl(isWideLayout = false)
        )
        assertEquals(
            "https://melvore.co.in/uploads/hero3_desktop.jpg",
            second.resolveImageUrl(isWideLayout = true)
        )
    }

    @Test
    fun heroSlideDetectsTextWhenOnlyCtaIsEmptyOrViceVersa() {
        val imageOnly = HeroSlide(
            id = 1,
            eyebrow = "  ",
            title = "",
            subtitle = "",
            imageUrl = "https://melvore.co.in/hero.jpg",
            ctaText = "",
            ctaLink = "/shop"
        )
        assertFalse(imageOnly.hasAnyText)

        val withSubtitle = imageOnly.copy(subtitle = "Winter Drop")
        assertTrue(withSubtitle.hasAnyText)
    }

    @Test
    fun googleCredentialHelperValidatesClientIdAndMapsExceptions() = runTest {
        val ctx = RuntimeEnvironment.getApplication()

        val blankOutcome = GoogleCredentialHelper.signIn(ctx, "")
        assertTrue(blankOutcome is GoogleSignInOutcome.Error)
        assertTrue((blankOutcome as GoogleSignInOutcome.Error).message.contains("GOOGLE_WEB_CLIENT_ID"))

        val badFormatOutcome = GoogleCredentialHelper.signIn(ctx, "not-a-valid-client-id")
        assertTrue(badFormatOutcome is GoogleSignInOutcome.Error)
        assertTrue((badFormatOutcome as GoogleSignInOutcome.Error).message.contains(".apps.googleusercontent.com"))

        val reauthMsg = GoogleCredentialHelper.mapCredentialException(
            GetCredentialCustomException("custom", "Google account reauth failed (28444)")
        )
        assertTrue(reauthMsg.contains("re-authentication failed"))
        assertTrue(reauthMsg.contains("com.aistudio.melvore.klmvre"))

        val cancelMsg = GoogleCredentialHelper.mapCredentialException(GetCredentialCancellationException())
        assertTrue(cancelMsg.contains("cancelled"))

        val noCredMsg = GoogleCredentialHelper.mapCredentialException(NoCredentialException())
        assertTrue(noCredMsg.contains("No eligible Google account"))

        val unavailableMsg = GoogleCredentialHelper.mapCredentialException(
            GetCredentialProviderConfigurationException()
        )
        assertTrue(unavailableMsg.contains("unavailable"))
    }

    @Test
    fun repositorySendsExpectedForgotPasswordAndSocialPayloads() {
        val json = kotlinx.serialization.json.Json { encodeDefaults = true; explicitNulls = false }
        val verifyReq = com.example.network.ForgotPasswordRequest(
            action = "verify",
            email = "customer@melvore.co.in",
            otp = "123456",
            newPassword = "NewPassword#2026",
            password = "NewPassword#2026"
        )
        val encodedVerify = json.encodeToString(com.example.network.ForgotPasswordRequest.serializer(), verifyReq)
        assertTrue(encodedVerify.contains(""""action":"verify""""))
        assertFalse(encodedVerify.contains(""""action":"reset""""))
        assertTrue(encodedVerify.contains(""""new_password":"NewPassword#2026""""))

        val socialReq = com.example.network.SocialAuthRequest(
            provider = "google",
            idToken = "sample.id.token"
        )
        val encodedSocial = json.encodeToString(com.example.network.SocialAuthRequest.serializer(), socialReq)
        assertTrue(encodedSocial.contains(""""provider":"google""""))
        assertTrue(encodedSocial.contains(""""id_token":"sample.id.token""""))
    }

    @Test
    fun brandsMapBothSchemasFilterInactiveAndSortByDisplayOrder() {
        val raw = listOf(
            com.example.network.BrandDto(
                brandId = 3,
                brandName = "Ader Studio",
                logoUrl = "/uploads/brands/ader.png",
                brandUrl = "/shop.php?brand_id=3",
                originCountry = "South Korea",
                isImported = JsonPrimitive(1),
                displayOrder = 2,
                isActive = JsonPrimitive(1)
            ),
            com.example.network.BrandDto(
                brandId = 1,
                name = "Maison Noir",
                logo = "/uploads/brands/noir.png",
                displayOrder = 1,
                status = "active"
            ),
            com.example.network.BrandDto(
                brandId = 2,
                name = "Retired Label",
                status = "inactive",
                displayOrder = 0
            )
        )

        val mapped = raw
            .map { it.toDomain() }
            .filter { it.isActive && it.name.isNotBlank() }
            .sortedWith(compareBy({ it.displayOrder }, { it.name }))

        assertEquals(2, mapped.size)
        assertEquals("Maison Noir", mapped[0].name)
        assertEquals("https://melvore.co.in/uploads/brands/noir.png", mapped[0].logoUrl)
        assertEquals("Ader Studio", mapped[1].name)
        assertEquals("South Korea", mapped[1].originCountry)
        assertTrue(mapped[1].isImported)
        assertEquals("/shop.php?brand_id=3", mapped[1].brandUrl)
    }

    @Test
    fun productDtoMapsBrandAndImportedOrigin() {
        val dto = com.example.network.ProductDto(
            productId = 77,
            name = "Structured Technical Overcoat",
            slug = "structured-technical-overcoat",
            categoryName = "Outerwear",
            brandId = 3,
            brandName = "Ader Studio",
            brandSlug = "ader-studio",
            originCountry = "South Korea",
            regularPrice = 14999.0
        )
        val product = dto.toDomain()
        assertEquals(3, product.brandId)
        assertEquals("Ader Studio", product.brandName)
        assertEquals("South Korea", product.originCountry)
        assertTrue(product.isImported)
    }
}
