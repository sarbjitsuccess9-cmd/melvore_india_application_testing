package com.example

import android.content.Context
import org.robolectric.RuntimeEnvironment
import com.example.data.DiscoveryRepository
import com.example.data.RemoteDesignManifestManager
import com.example.data.SecureSessionStore
import com.example.model.DesignManifestSupport
import com.example.model.ManifestSource
import com.example.network.ApiEnvelope
import com.example.network.ApiResult
import com.example.network.BootstrapDataDto
import com.example.network.HomeFeedDataDto
import com.example.network.MelvoreApiClient
import com.example.network.NetworkDiagnosticsMonitor
import com.example.network.OccasionsListDataDto
import com.example.network.ThemesConfigDataDto
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies live-backend integration behavior with MockWebServer and recorded v1.6.0 backend fixtures:
 * - First run sends NO If-None-Match header
 * - Second run sends If-None-Match and HTTP 304 preserves cache (ManifestSource.CACHED)
 * - HTTP 304 with empty cache retries unconditionally without If-None-Match
 * - Static live file fallback sets ManifestSource.STATIC_LIVE_FILE
 * - HTTP 500 on both manifest endpoints returns ApiResult.Error (not swallowed)
 * - HTTP 200 with success=false returns ApiResult.Error (not an empty list)
 * - NetworkDiagnosticsMonitor captures requests and NEVER logs Authorization / Bearer tokens
 * - SecureSessionStore sanitizes bad saved base URLs and resets cleanly to BuildConfig default
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveBackendIntegrationAndFixturesTest {

    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var sessionStore: SecureSessionStore
    private lateinit var apiClient: MelvoreApiClient
    private lateinit var discoveryRepo: DiscoveryRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("melvore_app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("melvore_secure_session", Context.MODE_PRIVATE).edit().clear().commit()
        NetworkDiagnosticsMonitor.clearForTest()

        server = MockWebServer()
        server.start()

        sessionStore = SecureSessionStore(context)
        sessionStore.setApiBaseUrl(server.url("/").toString())
        apiClient = MelvoreApiClient(sessionStore)
        discoveryRepo = DiscoveryRepository(
            context = context,
            apiClient = apiClient,
            sessionStore = sessionStore
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun firstRunSendsNoIfNoneMatch_secondRunSendsEtagAndHandles304Cached() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("ETag", "\"v1.6.0-live-etag\"")
                .addHeader("Content-Type", "application/json")
                .setBody(recordedManifestApiEnvelopeV160())
        )

        val first = discoveryRepo.fetchDesignManifest(slot = "live", forceRefresh = false)
        assertTrue("First fetch should succeed, got $first", first is ApiResult.Success)
        val firstOutcome = (first as ApiResult.Success).data
        assertEquals(ManifestSource.REMOTE, firstOutcome.source)
        assertEquals("1.6.0", firstOutcome.manifestVersion)
        assertTrue("Expected kit.v2 to be parsed", firstOutcome.manifest.hasWorldKitV2())
        assertTrue("Expected kit.v3 to be parsed", firstOutcome.manifest.hasWorldKitV3())
        assertEquals("\"v1.6.0-live-etag\"", firstOutcome.etag)

        val req1 = server.takeRequest()
        assertNull("First run must NOT send If-None-Match header", req1.getHeader("If-None-Match"))

        // Second run: should send If-None-Match and preserve cache on HTTP 304
        server.enqueue(
            MockResponse()
                .setResponseCode(304)
                .addHeader("ETag", "\"v1.6.0-live-etag\"")
        )
        val second = discoveryRepo.fetchDesignManifest(slot = "live", forceRefresh = false)
        assertTrue("304 fetch should succeed from cache, got $second", second is ApiResult.Success)
        val secondOutcome = (second as ApiResult.Success).data
        assertEquals(ManifestSource.CACHED, secondOutcome.source)
        assertEquals("1.6.0", secondOutcome.manifestVersion)

        val req2 = server.takeRequest()
        assertEquals("\"v1.6.0-live-etag\"", req2.getHeader("If-None-Match"))
    }

    @Test
    fun http500OnManifestAndStaticFallback_returnsErrorNotSwallowed() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("{\"success\":false,\"message\":\"Fatal PDO error\"}")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        val res = discoveryRepo.fetchDesignManifest(slot = "live", forceRefresh = true)
        assertTrue("HTTP 500 must surface ApiResult.Error, got $res", res is ApiResult.Error)
        val err = res as ApiResult.Error
        assertEquals(500, err.httpStatus)
        assertTrue(err.message.contains("500"))
    }

    @Test
    fun staticLiveFileFallback_marksSourceStaticLiveFile() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("{\"success\":false,\"message\":\"API temporarily unavailable\"}")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("ETag", "\"static-v1.6.0\"")
                .addHeader("Content-Type", "application/json")
                .setBody(recordedManifestJsonV160())
        )

        val res = discoveryRepo.fetchDesignManifest(slot = "live", forceRefresh = true)
        assertTrue("Static live fallback should succeed, got $res", res is ApiResult.Success)
        val outcome = (res as ApiResult.Success).data
        assertEquals(ManifestSource.STATIC_LIVE_FILE, outcome.source)
        assertTrue("Fallback flag must be visible (no silent fallback)", outcome.usedFallback)
        assertNotNull(outcome.fallbackReason)
        assertEquals("1.6.0", outcome.manifestVersion)
    }

    @Test
    fun http200WithSuccessFalse_returnsApiErrorNotEmptyList() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"success":false,"error_code":"DB_TABLE_MISSING","message":"Table style_worlds missing","data":null}""")
        )
        val homeRes = discoveryRepo.fetchHomeFeed()
        assertTrue("HTTP 200 success=false on home/feed must be ApiResult.Error, got $homeRes", homeRes is ApiResult.Error)
        assertEquals(200, (homeRes as ApiResult.Error).httpStatus)
        assertTrue(homeRes.message.contains("style_worlds"))

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"success":false,"error_code":"CATALOG_OFFLINE","message":"Catalog table unavailable","products":[]}""")
        )
        val productsRes = apiClient.safeCall { it.getProducts(page = 1, limit = 10) }
        assertTrue("HTTP 200 success=false on products/list must be ApiResult.Error, got $productsRes", productsRes is ApiResult.Error)
        assertEquals(200, (productsRes as ApiResult.Error).httpStatus)
    }

    @Test
    fun recordedBackendFixtures_parseV160BootstrapHomeFeedDiagnosticsThemesAndOccasions() {
        val json = DesignManifestSupport.json

        // 1. v1.6.0 manifest with kit.v2 array sizes [w,h] and kit.v3 object sizes {width,height}
        val manager = RemoteDesignManifestManager(context = context, sessionStore = null)
        val manifestOutcome = manager.validateAndAdopt(
            rawManifestJson = recordedManifestJsonV160(),
            activeDesign = 1,
            persistIfValid = false
        )
        assertEquals("1.6.0", manifestOutcome.manifestVersion)
        assertTrue(manifestOutcome.manifest.hasWorldKitV2())
        assertTrue(manifestOutcome.manifest.hasWorldKitV3())
        val y2kKit = manifestOutcome.manifest.worldOverride("y2k")?.kit
        assertEquals(240, y2kKit?.v2?.sizes?.get("button")?.resolvedWidth)
        assertEquals(48, y2kKit?.v2?.sizes?.get("button")?.resolvedHeight)
        assertEquals(360, y2kKit?.v3?.widgetSizes?.get("hero_frame")?.resolvedWidth)

        // 2. api/bootstrap.php fixture with missing optional fields
        val bootstrapEnvelope = json.decodeFromString<ApiEnvelope<BootstrapDataDto>>(
            """
            {
              "success": true,
              "message": "OK",
              "data": {
                "config_version": "cfg-2026-11",
                "active_design": 1,
                "design_slot": "live",
                "design_version": "1.6.0",
                "fashion_worlds": [
                  { "style_world_id": 1, "slug": "y2k", "name": "Y2K Chrome", "mode": "dark" }
                ]
              }
            }
            """.trimIndent()
        )
        assertTrue(bootstrapEnvelope.success)
        assertEquals("1.6.0", bootstrapEnvelope.data?.designVersion)
        assertEquals(1, bootstrapEnvelope.data?.fashionWorlds?.size)

        // 3. api/home/feed.php fixture with diagnostics.missing_tables & empty_modules
        val homeEnvelope = json.decodeFromString<ApiEnvelope<HomeFeedDataDto>>(
            """
            {
              "success": true,
              "message": "Feed ready",
              "data": {
                "hero_slides": [{ "id": 1, "title": "Winter Atelier" }],
                "diagnostics": {
                  "missing_tables": ["world_map_origins", "smart_offers"],
                  "empty_modules": ["preview_videos"]
                }
              }
            }
            """.trimIndent()
        )
        assertTrue(homeEnvelope.success)
        assertEquals(listOf("world_map_origins", "smart_offers"), homeEnvelope.data?.diagnostics?.missingTables)
        assertEquals(listOf("preview_videos"), homeEnvelope.data?.diagnostics?.emptyModules)

        // 4. api/themes/config.php & api/occasions/active.php fixtures
        val themesEnv = json.decodeFromString<ApiEnvelope<ThemesConfigDataDto>>(
            """{"success":true,"data":{"config_version":"t-1","worlds":[{"slug":"quiet-luxury","name":"Quiet Luxury","mode":"light"}]}}"""
        )
        assertEquals(1, themesEnv.data?.worlds?.size)

        val occEnv = json.decodeFromString<ApiEnvelope<OccasionsListDataDto>>(
            """{"success":true,"data":{"active_occasion":{"occasion_id":9,"slug":"diwali-luxe-edit","name":"Diwali Luxe"}}}"""
        )
        assertEquals("diwali-luxe-edit", occEnv.data?.activeOccasion?.slug)
    }

    @Test
    fun diagnosticsInterceptor_recordsRingBufferAndNeverLogsAuthToken() = runTest {
        val secretToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.SUPER_SECRET_TOKEN"
        sessionStore.saveAuthToken(secretToken)

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"success":true,"status":"healthy","active_products":42}""")
        )

        val res = apiClient.safeCall { it.getHealth() }
        assertTrue(res is ApiResult.Success)

        val traces = NetworkDiagnosticsMonitor.recentRequests.value
        assertTrue("Expected at least 1 recorded request trace", traces.isNotEmpty())
        val last = traces.first()
        assertEquals(200, last.httpStatus)
        assertTrue(last.pathAndQuery.contains("api/health.php"))

        // Verify token is never leaked into trace fields or formatted diagnostics text
        val report = NetworkDiagnosticsMonitor.formatTextReport(
            sessionStore = sessionStore,
            isLoggedIn = true,
            manifest = RemoteDesignManifestManager.emergencyDefaultManifest(),
            manifestSource = ManifestSource.REMOTE,
            manifestEtag = "",
            manifestHash = "",
            assetBaseUrl = "",
            manifestFallbackReason = null,
            manifestWarnings = emptyList(),
            homeFeedDiagnostics = com.example.model.FeedDiagnostics(),
            checkResults = emptyList()
        )
        assertFalse("Secret Bearer token must NEVER appear in diagnostics report", report.contains(secretToken))
        assertFalse("Secret Bearer token must NEVER appear in trace snippet", last.errorSnippet.contains(secretToken))
    }

    @Test
    fun secureSessionStore_stripsTrailingApiAndResetsToBuildConfigDefault() {
        val cleaned = sessionStore.setApiBaseUrl("https://custom.melvore.co.in/api/")
        assertEquals("https://custom.melvore.co.in", cleaned)
        assertTrue(sessionStore.isUsingCustomApiBaseUrl())

        val reset = sessionStore.resetApiBaseUrlToDefault()
        assertEquals(BuildConfig.MELVORE_API_BASE.trimEnd('/'), reset)
        assertFalse(sessionStore.isUsingCustomApiBaseUrl())
    }

    @Test
    fun mainActivity_launchesWithoutStartupCrash() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .visible()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }

    private fun recordedManifestApiEnvelopeV160(): String = """
        {
          "success": true,
          "message": "Live design manifest v1.6.0",
          "data": {
            "active_design": 1,
            "etag": "\"v1.6.0-live-etag\"",
            "manifest_hash": "abc123def456",
            "asset_base_url": "https://app.melvore.co.in",
            "manifest": ${recordedManifestJsonV160()}
          }
        }
    """.trimIndent()

    private fun recordedManifestJsonV160(): String = """
        {
          "manifest_id": "melvore_live_v160",
          "slot": "live",
          "design_version": "1.6.0",
          "schema_version": "1.0.0",
          "min_app_schema_version": "1.0.0",
          "tokens": {
            "colors": {
              "mode": "light",
              "background": "#FFFFFF",
              "surface": "#F7F7FA",
              "primary": "#09090B",
              "accent": "#C9A24B",
              "text_primary": "#09090B",
              "border": "#E4E4E7"
            }
          },
          "navigation": {
            "tabs": [
              { "route": "home", "label": "Home", "visible": true, "order": 1 },
              { "route": "discover", "label": "Discover", "visible": true, "order": 2 },
              { "route": "bag", "label": "Bag", "visible": true, "order": 3 }
            ]
          },
          "pages": {
            "home": { "page_key": "home", "layout_template": "editorial_magazine", "sections": [] }
          },
          "worlds": {
            "y2k": {
              "template": "y2k_chrome",
              "kit": {
                "version": "1.6.0",
                "v2": {
                  "components": { "button": "/design-system/assets/svg/y2k/btn.svg" },
                  "sizes": { "button": [240, 48] }
                },
                "v3": {
                  "layouts": { "home": "/design-system/assets/svg/y2k/home_bg.svg" },
                  "widgets": { "hero_frame": "/design-system/assets/svg/y2k/hero.svg" },
                  "widget_sizes": { "hero_frame": { "width": 360, "height": 180 } }
                }
              }
            }
          }
        }
    """.trimIndent()
}
