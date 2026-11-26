package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.SecureSessionStore
import com.example.model.ManifestSource
import com.example.network.EndpointCheckResult
import com.example.network.MelvoreApiClient
import com.example.network.NetworkDiagnosticsMonitor
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.viewmodel.DiscoveryUiState
import com.example.ui.viewmodel.MelvoreUiState
import kotlinx.coroutines.launch

/**
 * In-app Connection Diagnostics screen visible in ALL build types (debug & release).
 * Proves the exact API base URL, active overrides, design manifest provenance,
 * server home-feed diagnostics (`missing_tables` / `empty_modules`), live endpoint checks,
 * recent OkHttp ring buffer (50 requests), and Coil image failures.
 */
@Composable
fun ConnectionDiagnosticsScreen(
    uiState: MelvoreUiState,
    discoveryState: DiscoveryUiState,
    apiClient: MelvoreApiClient,
    sessionStore: SecureSessionStore,
    onResetApiBaseUrl: () -> Unit,
    onForceSyncAll: () -> Unit,
    onCopyToast: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalMelvoreSurfaces.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val recentRequests by NetworkDiagnosticsMonitor.recentRequests.collectAsState()
    val imageFailures by NetworkDiagnosticsMonitor.imageFailures.collectAsState()

    val checkResults = remember { mutableStateListOf<EndpointCheckResult>() }
    var isRunningChecks by remember { mutableStateOf(false) }
    var showRecentLog by remember { mutableStateOf(false) }
    var showImageFailures by remember { mutableStateOf(false) }
    var refreshNonce by remember { mutableStateOf(0) }

    val effectiveBaseUrl = remember(uiState.apiBaseUrl, refreshNonce) {
        sessionStore.getApiBaseUrl().trimEnd('/')
    }
    val customOverrideActive = remember(uiState.apiBaseUrl, refreshNonce) {
        sessionStore.isUsingCustomApiBaseUrl()
    }
    val savedOverrideValue = remember(uiState.apiBaseUrl, refreshNonce) {
        sessionStore.getSavedApiBaseUrlOverride()
    }
    val maskedGuestId = remember { sessionStore.getMaskedGuestId() }
    val manifest = discoveryState.activeManifest
    val resolvedVersion = discoveryState.manifestVersion.ifBlank { manifest.designVersion }
    val resolvedAssetBase = discoveryState.designKit.assetBaseUrl
    val resolvedHash = discoveryState.manifestHash.ifBlank { manifest.checksumSha256 }

    val homeProbe = checkResults.firstOrNull { it.path.contains("home/feed") }
    val combinedMissingTables = remember(homeProbe, discoveryState.homeFeed.diagnostics) {
        (homeProbe?.missingTables.orEmpty() + discoveryState.homeFeed.diagnostics.missingTables).distinct()
    }
    val combinedEmptyModules = remember(homeProbe, discoveryState.homeFeed.diagnostics) {
        (homeProbe?.emptyModules.orEmpty() + discoveryState.homeFeed.diagnostics.emptyModules).distinct()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(surfaces.background)
            .testTag("connection_diagnostics_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Bar
        item(key = "diag_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("diag_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = surfaces.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Connection Diagnostics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = surfaces.onSurface
                    )
                    Text(
                        text = "Live backend verification & manifest provenance",
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaces.onSurfaceMuted
                    )
                }
                IconButton(
                    onClick = {
                        onForceSyncAll()
                        refreshNonce++
                    },
                    modifier = Modifier.testTag("diag_force_sync_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Force refresh all",
                        tint = surfaces.accent
                    )
                }
            }
        }

        // Primary Actions: Run all checks + Copy report
        item(key = "diag_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    enabled = !isRunningChecks,
                    onClick = {
                        scope.launch {
                            isRunningChecks = true
                            checkResults.clear()
                            NetworkDiagnosticsMonitor.runAllChecks(
                                apiClient = apiClient,
                                sessionStore = sessionStore
                            ) { stepResult ->
                                checkResults.add(stepResult)
                            }
                            isRunningChecks = false
                            refreshNonce++
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = surfaces.accent,
                        contentColor = surfaces.onAccent
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .heightIn(min = 48.dp)
                        .testTag("diag_run_all_checks_btn")
                ) {
                    if (isRunningChecks) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = surfaces.onAccent
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Checking ${checkResults.size}/${NetworkDiagnosticsMonitor.diagnosticEndpoints.size}")
                    } else {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Run all checks")
                    }
                }

                OutlinedButton(
                    onClick = {
                        val report = NetworkDiagnosticsMonitor.formatTextReport(
                            sessionStore = sessionStore,
                            isLoggedIn = uiState.user != null,
                            manifest = manifest,
                            manifestSource = discoveryState.manifestSource,
                            manifestEtag = discoveryState.manifestEtag,
                            manifestHash = resolvedHash,
                            assetBaseUrl = resolvedAssetBase,
                            manifestFallbackReason = discoveryState.manifestFallbackReason,
                            manifestWarnings = discoveryState.manifestWarnings,
                            homeFeedDiagnostics = discoveryState.homeFeed.diagnostics,
                            checkResults = checkResults.toList()
                        )
                        clipboard.setText(AnnotatedString(report))
                        onCopyToast("Diagnostics report copied to clipboard")
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("diag_copy_report_btn")
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy report")
                }
            }
            if (isRunningChecks) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = {
                        val total = NetworkDiagnosticsMonitor.diagnosticEndpoints.size.coerceAtLeast(1)
                        checkResults.size.toFloat() / total.toFloat()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Card 1: Environment & Base URL
        item(key = "diag_env_card") {
            DiagCard(title = "Backend Connection & Session") {
                DiagKeyValue("BuildConfig Base", BuildConfig.MELVORE_API_BASE)
                DiagKeyValue("Effective API Base", effectiveBaseUrl)
                DiagKeyValue(
                    "Saved Override",
                    if (customOverrideActive) "${savedOverrideValue ?: ""} (OVERRIDE ACTIVE)" else "None (using BuildConfig default)",
                    valueColor = if (customOverrideActive) surfaces.sale else surfaces.success
                )
                if (customOverrideActive) {
                    OutlinedButton(
                        onClick = {
                            onResetApiBaseUrl()
                            refreshNonce++
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("diag_reset_api_url_btn")
                    ) {
                        Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset to default (${BuildConfig.MELVORE_API_BASE})")
                    }
                }
                DiagKeyValue(
                    "App Version",
                    "${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE}) · ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}"
                )
                DiagKeyValue("Guest ID (masked)", maskedGuestId)
                DiagKeyValue(
                    "Account State",
                    uiState.user?.let { "Logged in (${it.email})" } ?: "Guest session",
                    valueColor = if (uiState.user != null) surfaces.success else surfaces.onSurfaceMuted
                )
            }
        }

        // Card 2: Design Manifest Provenance
        item(key = "diag_manifest_card") {
            val sourceColor = when (discoveryState.manifestSource) {
                ManifestSource.REMOTE -> surfaces.success
                ManifestSource.CACHED -> surfaces.accent
                ManifestSource.STATIC_LIVE_FILE -> surfaces.accent
                ManifestSource.BUNDLED -> surfaces.sale
            }
            DiagCard(title = "Design System Source") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Source",
                        style = MaterialTheme.typography.labelMedium,
                        color = surfaces.onSurfaceMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = sourceColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, sourceColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = discoveryState.manifestSource.wireName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = sourceColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                DiagKeyValue("Design Version", "$resolvedVersion (slot=${manifest.slot}; live target 1.6.0)")
                DiagKeyValue("Manifest Hash", resolvedHash.ifBlank { "(none)" })
                DiagKeyValue("ETag", discoveryState.manifestEtag.ifBlank { "(none)" })
                DiagKeyValue("Asset Base URL", resolvedAssetBase.ifBlank { "(default API base)" })
                DiagKeyValue(
                    "Worlds & Kits",
                    "${manifest.worlds.size} worlds · ${manifest.worldKitCount()} kits · kit.v2=${manifest.hasWorldKitV2()} · kit.v3=${manifest.hasWorldKitV3()}",
                    valueColor = if (manifest.hasWorldKitV2OrV3()) surfaces.success else surfaces.sale
                )
                DiagKeyValue("Manifest Sync", NetworkDiagnosticsMonitor.formatUpdatedAgo(sessionStore.getManifestSavedAtMs()))
                DiagKeyValue("Bootstrap Sync", NetworkDiagnosticsMonitor.formatUpdatedAgo(sessionStore.getBootstrapSavedAtMs()))
                DiagKeyValue("Home Feed Sync", NetworkDiagnosticsMonitor.formatUpdatedAgo(sessionStore.getHomeFeedSavedAtMs()))

                if (!discoveryState.manifestFallbackReason.isNullOrBlank()) {
                    Text(
                        text = "Fallback reason: ${discoveryState.manifestFallbackReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaces.sale
                    )
                }
                if (discoveryState.manifestWarnings.isNotEmpty()) {
                    Text(
                        text = "Per-section parse warnings (${discoveryState.manifestWarnings.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = surfaces.sale
                    )
                    discoveryState.manifestWarnings.forEach { warn ->
                        Text(
                            text = "• $warn",
                            style = MaterialTheme.typography.bodySmall,
                            color = surfaces.onSurfaceMuted
                        )
                    }
                }
            }
        }

        // Card 3: Home Feed Server Diagnostics
        item(key = "diag_home_server_card") {
            DiagCard(title = "Home Feed Server Diagnostics") {
                DiagKeyValue(
                    "missing_tables",
                    if (combinedMissingTables.isEmpty()) "None reported" else combinedMissingTables.joinToString(", "),
                    valueColor = if (combinedMissingTables.isEmpty()) surfaces.success else surfaces.sale
                )
                DiagKeyValue(
                    "empty_modules",
                    if (combinedEmptyModules.isEmpty()) "None reported" else combinedEmptyModules.joinToString(", "),
                    valueColor = if (combinedEmptyModules.isEmpty()) surfaces.success else surfaces.accent
                )
            }
        }

        // Section 4: Endpoint Check Results
        if (checkResults.isNotEmpty()) {
            item(key = "diag_checks_header") {
                val okCount = checkResults.count { it.isHealthy }
                Text(
                    text = "Endpoint Checks ($okCount / ${checkResults.size} healthy)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = surfaces.onSurface
                )
            }
            items(checkResults, key = { "chk_${it.path}" }) { item ->
                EndpointCheckRow(result = item)
            }
        }

        // Section 5: Always-on OkHttp Ring Buffer (last 50)
        item(key = "diag_recent_http_toggle") {
            Surface(
                onClick = { showRecentLog = !showRecentLog },
                shape = RoundedCornerShape(12.dp),
                color = surfaces.surface,
                border = BorderStroke(1.dp, surfaces.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("diag_toggle_http_log")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Live HTTP Requests (${recentRequests.size}/50)",
                        style = MaterialTheme.typography.titleSmall,
                        color = surfaces.onSurface
                    )
                    Icon(
                        imageVector = if (showRecentLog) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = "Toggle HTTP log",
                        tint = surfaces.onSurfaceMuted
                    )
                }
            }
        }
        if (showRecentLog) {
            items(recentRequests, key = { "req_${it.timestampMs}_${it.pathAndQuery}" }) { trace ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = surfaces.surfaceElevated,
                    border = BorderStroke(1.dp, surfaces.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${trace.method} ${trace.pathAndQuery}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = surfaces.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "HTTP ${trace.httpStatus} · ${trace.latencyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (trace.isHealthy) surfaces.success else surfaces.sale
                            )
                        }
                        if (trace.message.isNotBlank() || trace.etag.isNotBlank()) {
                            Text(
                                text = listOfNotNull(
                                    trace.successFlag?.let { "success=$it" },
                                    trace.etag.takeIf { it.isNotBlank() }?.let { "etag=$it" },
                                    trace.message.takeIf { it.isNotBlank() }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = surfaces.onSurfaceMuted
                            )
                        }
                        if (trace.errorSnippet.isNotBlank()) {
                            Text(
                                text = trace.errorSnippet,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = surfaces.sale,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Section 6: Coil Image Load Failures
        item(key = "diag_img_failures_toggle") {
            Surface(
                onClick = { showImageFailures = !showImageFailures },
                shape = RoundedCornerShape(12.dp),
                color = surfaces.surface,
                border = BorderStroke(1.dp, surfaces.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("diag_toggle_img_failures")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Image Load Failures (${imageFailures.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (imageFailures.isEmpty()) surfaces.onSurface else surfaces.sale
                    )
                    Icon(
                        imageVector = if (showImageFailures) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = "Toggle image failures",
                        tint = surfaces.onSurfaceMuted
                    )
                }
            }
        }
        if (showImageFailures) {
            if (imageFailures.isEmpty()) {
                item(key = "diag_img_none") {
                    Text(
                        text = "No image load failures recorded in this session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaces.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                items(imageFailures, key = { "img_${it.timestampMs}_${it.url}" }) { fail ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = surfaces.surfaceElevated,
                        border = BorderStroke(1.dp, surfaces.sale.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = fail.url,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = surfaces.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = fail.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = surfaces.sale
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagCard(
    title: String,
    content: @Composable () -> Unit
) {
    val surfaces = LocalMelvoreSurfaces.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = surfaces.surface,
        border = BorderStroke(1.dp, surfaces.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = surfaces.onSurface
            )
            content()
        }
    }
}

@Composable
private fun DiagKeyValue(
    label: String,
    value: String,
    valueColor: Color = LocalMelvoreSurfaces.current.onSurface
) {
    val surfaces = LocalMelvoreSurfaces.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = surfaces.onSurfaceMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}

@Composable
private fun EndpointCheckRow(result: EndpointCheckResult) {
    val surfaces = LocalMelvoreSurfaces.current
    val statusColor = if (result.isHealthy) surfaces.success else surfaces.sale
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = surfaces.surface,
        border = BorderStroke(1.dp, if (result.isHealthy) surfaces.border else surfaces.sale.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = result.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaces.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "HTTP ${result.httpStatus} · ${result.latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Text(
                text = "GET ${result.path}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = surfaces.onSurfaceMuted
            )
            val metaLine = buildString {
                append("success=${result.successFlag ?: "n/a"}")
                if (result.etag.isNotBlank()) append(" · ETag=${result.etag.take(18)}")
                if (result.message.isNotBlank()) append(" · ${result.message}")
            }
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                color = surfaces.onSurfaceMuted
            )
            if (result.payloadSummary.isNotBlank()) {
                Text(
                    text = result.payloadSummary,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = surfaces.onSurface
                )
            }
            if (result.failureBodySnippet.isNotBlank()) {
                Text(
                    text = "Body: ${result.failureBodySnippet}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = surfaces.sale
                )
            }
        }
    }
}
