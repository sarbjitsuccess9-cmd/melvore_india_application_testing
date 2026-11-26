# Melvore — Native Android Storefront (Kotlin + Jetpack Compose)

**Melvore** is a luxury Indian streetwear, sculpted outerwear, and zipless comfort loungewear shopping application built in **Kotlin** and **Jetpack Compose (Material 3)**.

## Architecture & Project Structure

- **UI & Navigation**: `com.example.MainActivity`, `@Serializable` Navigation Compose routes (`MelvoreRoutes.kt`), Light & Dark Material 3 palettes (`Theme.kt`), custom adaptive launcher icon (`@mipmap/ic_launcher`), and bundled product/editorial photography (`res/drawable/`).
- **State & Local Persistence**: `MelvoreViewModel` (`AndroidViewModel` + `StateFlow`) backed by `MelvoreRepository` (`SharedPreferences` JSON persistence via `kotlinx.serialization`) and `MelvoreSeedData` (8 curated products, categories, collections, editorial slides, journal articles, FAQs, demo account, sample order `MLV-10482`, and support ticket `TKT-2091`).
- **Core Features**:
  - **Editorial Home**: Auto-rotating hero lookbook carousel with play/pause controls, shoppable categories, marquee strip, curated drop grid, brand promise tiles, and verified buyer testimonials.
  - **Catalog & Categories**: Live search, category & collection filtering, sort options (`Featured`, `Price: Low to High`, `Price: High to Low`, `Top Rated`), and visual collection showcases.
  - **Product Details**: Multi-image `HorizontalPager` gallery, color swatch & size selectors, quantity stepper, fabric & care guide, wishlist toggle, Add to Cart, and Buy Now.
  - **Search Discovery & Cart Sheet**: Full-screen search & discovery modal (trending tags, categories, arrivals, collections) and persistent Cart bottom sheet with ₹999 free express shipping progress.
  - **Checkout & Simulated Payment Verification**: Saved address selection / inline new address creation, promo codes (`MELVORE10`, `FIRSTORDER`, `WELCOME15`), COD (2% handling fee) and UPI/Card simulated online payment verification flow (`Simulate Payment Success` / `Simulate Failure` / `Cancel Order`).
  - **Order Tracking & Support**: Order number/email lookup, 5-step shipment timeline, pending online payment recovery, and support ticket creation/replies.
  - **Account & Preferences**: Login, Sign Up, Google Sign-In dialog, Quick Demo Sign-In (`aarav@melvore.com` / `Melvore@123`), profile editing, wishlist, addresses, Light/Dark mode, `₹`/`$` currency switch, and configurable Hostinger API base URL.
  - **Editorial Pages**: About Us, Melvore Journal (article list & reader), FAQ accordion, and Contact form.

## Remaining Integration TODOs

- **Live Hostinger REST Sync**: `MelvoreRepository` includes an API endpoint setting (`apiBaseUrl`); wire Retrofit/Ktor HTTP calls to `https://melvore.co.in/api` (or `https://app.melvore.co.in`) when connecting to a live PHP/MySQL backend.
- **Live Payment Gateway & Google Identity**: Replace the simulated UPI/Card verification dialog with the production Razorpay Android SDK and Jetpack `CredentialManager` Google Sign-In when live merchant/OAuth credentials are configured.
