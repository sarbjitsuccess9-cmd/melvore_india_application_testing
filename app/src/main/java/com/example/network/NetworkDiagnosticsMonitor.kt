package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.SecureSessionStore
import com.example.model.DesignManifest
import com.example.model.FeedDiagnostics
import com.example.model.ManifestSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class NetworkRequestTrace(
    val timestampMs: Long,
    val method: String,
    val pathAndQuery: String,
    val httpStatus: Int,
    val latencyMs: Long,
    val etag: String = "",
    val successFlag: Boolean? = null,
    val message: String = "",
    val errorSnippet: String = ""
) {
    val isHealthy: Boolean
        get() = httpStatus in 200..399 && successFlag != false
}

data class ImageLoadFailureTrace(
    val timestampMs: Long,
    val url: String,
    val reason: String
)

data class EndpointCheckResult(
    val label: String,
    val path: String,
    val httpStatus: Int,
    val latencyMs: Long,
    val successFlag: Boolean? = null,
    val message: String = "",
    val payloadSummary: String = "",
    val etag: String = "",
    val failureBodySnippet: String = "",
    val missingTables: List<String> = emptyList(),
    val emptyModules: List<String> = emptyList()
) {
    val isHealthy: Boolean
        get() = httpStatus in 200..399 && successFlag != false
}

/**
 * Always-on, zero-secret network telemetry monitor for the Melvore Connection Diagnostics screen.
 * Keeps a small ring buffer of the last 50 HTTP requests and last 30 Coil image failures.
 * Strictly never logs Authorization headers, Bearer tokens, cookies, or OTP/password secrets.
 */
object NetworkDiagnosticsMonitor {
    private const val TAG = "MelvoreNetDiag"
    private const val MAX_REQUEST_TRACES = 50
    private const val MAX_IMAGE_FAILURES = 30
    private val SENSITIVE_QUERY_KEYS = setOf("token", "auth", "password", "otp", "secret", "key", "signature")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val lock = Any()
    private val requestBuffer = ArrayDeque<NetworkRequestTrace>(MAX_REQUEST_TRACES + 1)
    private val imageFailureBuffer = ArrayDeque<ImageLoadFailureTrace>(MAX_IMAGE_FAILURES + 1)

    private val _recentRequests = MutableStateFlow<List<NetworkRequestTrace>>(emptyList())
    val recentRequests: StateFlow<List<NetworkRequestTrace>> = _recentRequests.asStateFlow()

    private val _imageFailures = MutableStateFlow<List<ImageLoadFailureTrace>>(emptyList())
    val imageFailures: StateFlow<List<ImageLoadFailureTrace>> = _imageFailures.asStateFlow()

    /**
     * Always-on OkHttp interceptor feeding the diagnostics ring buffer.
     * Never accesses or stores Authorization / Cookie headers.
     */
    val interceptor: Interceptor = Interceptor { chain ->
        val req = chain.request()
        val startNs = System.nanoTime()
        val safePath = sanitizePathAndQuery(req)
        try {
            val response = chain.proceed(req)
            val latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs).coerceAtLeast(0L)
            recordResponse(req.method, safePath, response, latencyMs)
            response
        } catch (ex: Exception) {
            val latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs).coerceAtLeast(0L)
            appendRequestTrace(
                NetworkRequestTrace(
                    timestampMs = System.currentTimeMillis(),
                    method = req.method,
                    pathAndQuery = safePath,
                    httpStatus = 0,
                    latencyMs = latencyMs,
                    successFlag = false,
                    message = ex.javaClass.simpleName,
                    errorSnippet = (ex.message ?: "Network error").take(200)
                )
            )
            throw ex
        }
    }

    private fun recordResponse(
        method: String,
        safePath: String,
        response: Response,
        latencyMs: Long
    ) {
        val code = response.code
        val etag = response.header("ETag")?.trim().orEmpty()
        val contentType = response.header("Content-Type").orEmpty().lowercase()
        val isTextLike = contentType.contains("json") ||
            contentType.contains("text") ||
            contentType.contains("xml") ||
            contentType.isBlank()

        var successFlag: Boolean? = if (code == 304) true else null
        var message = ""
        var errorSnippet = ""

        if (code != 304 && code != 204 && isTextLike) {
            val rawPeek = runCatching { response.peekBody(4096).string() }.getOrNull().orEmpty()
            val trimmed = rawPeek.trim()
            if (trimmed.startsWith("{")) {
                val obj = runCatching { json.decodeFromString<JsonObject>(trimmed) }.getOrNull()
                if (obj != null) {
                    successFlag = obj["success"]?.jsonPrimitive?.booleanOrNull
                    message = obj["message"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                }
            }
            if (!response.isSuccessful || successFlag == false) {
                errorSnippet = sanitizeSnippet(trimmed)
            }
        }

        if (!response.isSuccessful || successFlag == false) {
            runCatching {
                Log.w(
                    TAG,
                    "$method $safePath -> HTTP $code (${latencyMs}ms) success=$successFlag msg=$message"
                )
            }
        }

        appendRequestTrace(
            NetworkRequestTrace(
                timestampMs = System.currentTimeMillis(),
                method = method,
                pathAndQuery = safePath,
                httpStatus = code,
                latencyMs = latencyMs,
                etag = etag,
                successFlag = successFlag,
                message = message,
                errorSnippet = errorSnippet
            )
        )
    }

    private fun appendRequestTrace(trace: NetworkRequestTrace) {
        val snapshot = synchronized(lock) {
            requestBuffer.addFirst(trace)
            while (requestBuffer.size > MAX_REQUEST_TRACES) {
                requestBuffer.removeLast()
            }
            requestBuffer.toList()
        }
        _recentRequests.value = snapshot
    }

    fun recordImageFailure(url: String, reason: String) {
        val cleanUrl = url.trim().ifBlank { "(empty)" }
        val cleanReason = reason.trim().ifBlank { "Image decode/load failed" }.take(200)
        runCatching { Log.w(TAG, "Image load failed: $cleanUrl — $cleanReason") }
        val snapshot = synchronized(lock) {
            imageFailureBuffer.addFirst(
                ImageLoadFailureTrace(
                    timestampMs = System.currentTimeMillis(),
                    url = cleanUrl,
                    reason = cleanReason
                )
            )
            while (imageFailureBuffer.size > MAX_IMAGE_FAILURES) {
                imageFailureBuffer.removeLast()
            }
            imageFailureBuffer.toList()
        }
        _imageFailures.value = snapshot
    }

    fun clearForTest() {
        synchronized(lock) {
            requestBuffer.clear()
            imageFailureBuffer.clear()
        }
        _recentRequests.value = emptyList()
        _imageFailures.value = emptyList()
    }

    data class EndpointSpec(val label: String, val path: String)

    val diagnosticEndpoints: List<EndpointSpec> = listOf(
        EndpointSpec("Index / Router", "index.php"),
        EndpointSpec("Health", "api/health.php"),
        EndpointSpec("Bootstrap", "api/bootstrap.php"),
        EndpointSpec("Design Manifest API", "api/design/manifest.php?slot=live"),
        EndpointSpec("Static Live Manifest", "design-system/live/manifest.json"),
        EndpointSpec("Design Version", "api/design/version.php"),
        EndpointSpec("Design Hub", "design.php?section=all"),
        EndpointSpec("Home Feed", "api/home/feed.php"),
        EndpointSpec("Themes Config", "api/themes/config.php"),
        EndpointSpec("Active Theme", "api/themes/active.php"),
        EndpointSpec("Active Occasions", "api/occasions/active.php"),
        EndpointSpec("Occasions List", "api/occasions/list.php"),
        EndpointSpec("Active Campaigns", "api/campaigns/active.php"),
        EndpointSpec("Products List", "api/products/list.php?limit=10"),
        EndpointSpec("Categories List", "api/categories/list.php"),
        EndpointSpec("Brands List", "api/brands/list.php"),
        EndpointSpec("Style Worlds List", "api/styles/list.php"),
        EndpointSpec("Videos Feed", "api/videos/feed.php?limit=5"),
        EndpointSpec("Elite Home", "api/elite/home.php"),
        EndpointSpec("For You", "api/personalization/for_you.php?limit=8"),
        EndpointSpec("World Map", "api/discovery/world_map.php"),
        EndpointSpec("Eligible Offers", "api/offers/eligible.php"),
        EndpointSpec("Search Suggestions", "api/search/suggestions.php?q=dress"),
        EndpointSpec("Help Topics", "api/help/topics.php")
    )

    suspend fun runAllChecks(
        apiClient: MelvoreApiClient,
        sessionStore: SecureSessionStore,
        onStepCompleted: ((EndpointCheckResult) -> Unit)? = null
    ): List<EndpointCheckResult> = withContext(Dispatchers.IO) {
        val baseUrl = sessionStore.getApiBaseUrl().trimEnd('/')
        val client: OkHttpClient = apiClient.rawOkHttpClient()
        val results = mutableListOf<EndpointCheckResult>()

        for (spec in diagnosticEndpoints) {
            val result = probeSingleEndpoint(client, baseUrl, spec)
            results += result
            onStepCompleted?.invoke(result)
        }
        results
    }

    private fun probeSingleEndpoint(
        client: OkHttpClient,
        baseUrl: String,
        spec: EndpointSpec
    ): EndpointCheckResult {
        val fullUrl = "$baseUrl/${spec.path.trimStart('/')}"
        val req = Request.Builder()
            .url(fullUrl)
            .get()
            .header("Accept", "application/json")
            .build()
        val startNs = System.nanoTime()
        return try {
            client.newCall(req).execute().use { resp ->
                val latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs).coerceAtLeast(0L)
                val code = resp.code
                val etag = resp.header("ETag")?.trim().orEmpty()
                val rawBody = runCatching { resp.body?.string() }.getOrNull().orEmpty()
                val trimmed = rawBody.trim()
                val rootObj = if (trimmed.startsWith("{")) {
                    runCatching { json.decodeFromString<JsonObject>(trimmed) }.getOrNull()
                } else null

                val successFlag = rootObj?.get("success")?.jsonPrimitive?.booleanOrNull
                val message = rootObj?.get("message")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                    .ifBlank { rootObj?.get("status")?.jsonPrimitive?.contentOrNull?.trim().orEmpty() }
                val dataObj = (rootObj?.get("data") as? JsonObject) ?: rootObj

                val missingTables = extractStringList(dataObj?.get("diagnostics") as? JsonObject, "missing_tables")
                val emptyModules = extractStringList(dataObj?.get("diagnostics") as? JsonObject, "empty_modules")

                val summary = if (rootObj != null) {
                    summarizePayload(spec.path, rootObj, dataObj)
                } else if (trimmed.isEmpty()) {
                    "0-byte response body"
                } else {
                    "non-JSON body (${trimmed.length} chars)"
                }

                val isFailure = !resp.isSuccessful || successFlag == false
                val failureSnippet = if (isFailure) sanitizeSnippet(trimmed) else ""

                EndpointCheckResult(
                    label = spec.label,
                    path = spec.path,
                    httpStatus = code,
                    latencyMs = latencyMs,
                    successFlag = successFlag,
                    message = message,
                    payloadSummary = summary,
                    etag = etag,
                    failureBodySnippet = failureSnippet,
                    missingTables = missingTables,
                    emptyModules = emptyModules
                )
            }
        } catch (ex: Exception) {
            val latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs).coerceAtLeast(0L)
            EndpointCheckResult(
                label = spec.label,
                path = spec.path,
                httpStatus = 0,
                latencyMs = latencyMs,
                successFlag = false,
                message = ex.javaClass.simpleName,
                payloadSummary = "Connection failed",
                failureBodySnippet = sanitizeSnippet(ex.message ?: ex.toString())
            )
        }
    }

    private fun extractStringList(obj: JsonObject?, key: String): List<String> {
        val arr = obj?.get(key) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { s -> s.isNotBlank() } }
    }

    private fun summarizePayload(path: String, root: JsonObject, data: JsonObject?): String {
        val target = data ?: root
        val parts = mutableListOf<String>()

        fun addCount(key: String, alias: String = key) {
            val el = target[key] ?: root[key] ?: return
            when (el) {
                is JsonArray -> parts += "$alias=${el.size}"
                is JsonObject -> parts += "$alias=${el.size}"
                else -> Unit
            }
        }

        fun addScalar(key: String, alias: String = key) {
            val p = (target[key] ?: root[key]) as? JsonPrimitive ?: return
            val v = p.contentOrNull?.trim().orEmpty()
            if (v.isNotBlank()) parts += "$alias=$v"
        }

        when {
            path.contains("manifest") -> {
                val manifestObj = (target["manifest"] as? JsonObject) ?: target
                val ver = (manifestObj["design_version"] as? JsonPrimitive)?.contentOrNull
                    ?: (target["design_version"] as? JsonPrimitive)?.contentOrNull
                if (!ver.isNullOrBlank()) parts += "design_version=$ver"
                val worlds = manifestObj["worlds"] as? JsonObject
                if (worlds != null) {
                    val v2Count = worlds.values.count {
                        ((it as? JsonObject)?.get("kit") as? JsonObject)?.containsKey("v2") == true
                    }
                    val v3Count = worlds.values.count {
                        ((it as? JsonObject)?.get("kit") as? JsonObject)?.containsKey("v3") == true
                    }
                    parts += "worlds=${worlds.size} (v2=$v2Count, v3=$v3Count)"
                }
                val assetBase = (target["asset_base_url"] as? JsonPrimitive)?.contentOrNull
                if (!assetBase.isNullOrBlank()) parts += "asset_base=$assetBase"
            }
            path.contains("bootstrap") -> {
                addScalar("config_version", "cfg")
                addScalar("design_version", "design")
                addCount("fashion_worlds", "worlds")
                addCount("occasions")
                addCount("smart_offers", "offers")
                addCount("world_map_origins", "pins")
            }
            path.contains("home/feed") -> {
                addCount("hero_slides")
                addCount("categories")
                addCount("style_worlds", "worlds")
                addCount("brands")
                addCount("sections")
                addCount("discovery_products", "products")
            }
            path.contains("health") -> {
                addScalar("status")
                addScalar("active_products")
                addScalar("service")
            }
            path.contains("design.php") -> {
                addCount("hero_slides")
                addCount("categories")
                addCount("brands")
                addCount("featured")
                addCount("faqs")
            }
            else -> {
                listOf(
                    "products", "categories", "brands", "style_worlds", "worlds",
                    "fashion_worlds", "occasions", "campaigns", "videos", "packs",
                    "offers", "pins", "topics", "suggestions", "endpoints"
                ).forEach { addCount(it) }
                if (parts.isEmpty()) {
                    addScalar("design_version")
                    addScalar("status")
                    if (parts.isEmpty()) {
                        parts += "keys=${target.keys.take(5).joinToString(",")}"
                    }
                }
            }
        }
        return parts.joinToString(", ").ifBlank { "OK" }
    }

    fun formatUpdatedAgo(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        if (timestampMs <= 0L) return "Never synced"
        val diffSec = ((nowMs - timestampMs) / 1000L).coerceAtLeast(0L)
        return when {
            diffSec < 60L -> "Updated just now"
            diffSec < 3600L -> "Updated ${diffSec / 60L} min ago"
            diffSec < 86_400L -> "Updated ${diffSec / 3600L} hr ago"
            else -> "Updated ${diffSec / 86_400L} d ago"
        }
    }

    fun formatTextReport(
        sessionStore: SecureSessionStore,
        isLoggedIn: Boolean,
        manifest: DesignManifest,
        manifestSource: ManifestSource,
        manifestEtag: String,
        manifestHash: String,
        assetBaseUrl: String,
        manifestFallbackReason: String?,
        manifestWarnings: List<String>,
        homeFeedDiagnostics: FeedDiagnostics,
        checkResults: List<EndpointCheckResult>
    ): String {
        val sb = StringBuilder()
        val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sb.appendLine("=== MELVORE CONNECTION DIAGNOSTICS ===")
        sb.appendLine("Generated: ${timeFmt.format(Date())}")
        sb.appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) [${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}]")
        sb.appendLine("BuildConfig.MELVORE_API_BASE: ${BuildConfig.MELVORE_API_BASE}")
        sb.appendLine("Effective API Base: ${sessionStore.getApiBaseUrl().trimEnd('/')}")
        sb.appendLine("Custom Override Active: ${sessionStore.isUsingCustomApiBaseUrl()} (${sessionStore.getSavedApiBaseUrlOverride() ?: "none"})")
        sb.appendLine("Guest ID: ${sessionStore.getMaskedGuestId()}")
        sb.appendLine("Logged In: $isLoggedIn")
        sb.appendLine()
        sb.appendLine("--- DESIGN MANIFEST ---")
        sb.appendLine("Source: ${manifestSource.wireName}")
        sb.appendLine("Version: ${manifest.designVersion} (slot=${manifest.slot})")
        sb.appendLine("Manifest Hash: ${manifestHash.ifBlank { "(none)" }}")
        sb.appendLine("ETag: ${manifestEtag.ifBlank { "(none)" }}")
        sb.appendLine("Asset Base URL: ${assetBaseUrl.ifBlank { "(default API base)" }}")
        sb.appendLine("Worlds: ${manifest.worlds.size} | Kits: ${manifest.worldKitCount()} | kit.v2=${manifest.hasWorldKitV2()} | kit.v3=${manifest.hasWorldKitV3()}")
        sb.appendLine("Manifest Sync: ${formatUpdatedAgo(sessionStore.getManifestSavedAtMs())}")
        sb.appendLine("Bootstrap Sync: ${formatUpdatedAgo(sessionStore.getBootstrapSavedAtMs())}")
        sb.appendLine("Home Feed Sync: ${formatUpdatedAgo(sessionStore.getHomeFeedSavedAtMs())}")
        if (!manifestFallbackReason.isNullOrBlank()) {
            sb.appendLine("Fallback Reason: $manifestFallbackReason")
        }
        if (manifestWarnings.isNotEmpty()) {
            sb.appendLine("Parse Warnings (${manifestWarnings.size}):")
            manifestWarnings.forEach { sb.appendLine("  - $it") }
        }
        sb.appendLine()
        sb.appendLine("--- HOME FEED SERVER DIAGNOSTICS ---")
        val homeCheck = checkResults.firstOrNull { it.path.contains("home/feed") }
        val missingTables = (homeCheck?.missingTables.orEmpty() + homeFeedDiagnostics.missingTables).distinct()
        val emptyModules = (homeCheck?.emptyModules.orEmpty() + homeFeedDiagnostics.emptyModules).distinct()
        sb.appendLine("missing_tables: ${if (missingTables.isEmpty()) "none" else missingTables.joinToString(", ")}")
        sb.appendLine("empty_modules: ${if (emptyModules.isEmpty()) "none" else emptyModules.joinToString(", ")}")
        sb.appendLine()
        sb.appendLine("--- ENDPOINT CHECKS (${checkResults.size}) ---")
        if (checkResults.isEmpty()) {
            sb.appendLine("(Not run yet — tap 'Run all checks')")
        } else {
            checkResults.forEach { r ->
                val statusTag = if (r.isHealthy) "OK" else "FAIL"
                sb.appendLine(
                    "[$statusTag] GET ${r.path} | HTTP ${r.httpStatus} | ${r.latencyMs}ms | success=${r.successFlag} | ${r.payloadSummary}"
                )
                if (r.etag.isNotBlank()) sb.appendLine("      ETag: ${r.etag}")
                if (r.message.isNotBlank()) sb.appendLine("      Message: ${r.message}")
                if (r.failureBodySnippet.isNotBlank()) sb.appendLine("      Body(200): ${r.failureBodySnippet}")
            }
        }
        val imgErrors = imageFailures.value
        if (imgErrors.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("--- RECENT IMAGE FAILURES (${imgErrors.size}) ---")
            imgErrors.take(10).forEach { e ->
                sb.appendLine("  - ${e.url} :: ${e.reason}")
            }
        }
        val traces = recentRequests.value
        if (traces.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("--- RECENT HTTP LOG (last ${traces.take(20).size} of ${traces.size}) ---")
            traces.take(20).forEach { t ->
                sb.appendLine("  ${t.method} ${t.pathAndQuery} -> HTTP ${t.httpStatus} (${t.latencyMs}ms) success=${t.successFlag}")
            }
        }
        return sb.toString()
    }

    private fun sanitizePathAndQuery(req: Request): String {
        val url = req.url
        val path = url.encodedPath
        if (url.querySize == 0) return path
        val safeParams = (0 until url.querySize).map { i ->
            val name = url.queryParameterName(i)
            val value = if (SENSITIVE_QUERY_KEYS.any { name.contains(it, ignoreCase = true) }) {
                "[REDACTED]"
            } else {
                url.queryParameterValue(i).orEmpty()
            }
            "$name=$value"
        }
        return "$path?${safeParams.joinToString("&")}"
    }

    private fun sanitizeSnippet(raw: String): String {
        val collapsed = raw.replace(Regex("\\s+"), " ").trim()
        val redacted = collapsed.replace(
            Regex("""("?(?:token|authorization|bearer|password|secret|otp)"?\s*[:=]\s*"?)([^"\s,}]+)""", RegexOption.IGNORE_CASE),
            "$1[REDACTED]"
        )
        return redacted.take(200)
    }
}
