package com.example.ui.design

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

/**
 * Renders SMIL-animated SVG artwork from the Melvore design kit inside a sandboxed,
 * JavaScript-disabled [WebView] with transparent background and lifecycle pause/resume.
 *
 * When [reducedMotion] is true, in Preview/Inspection mode, or when the WebView encounters
 * a load error, this automatically falls back to Coil's static [SvgDecoder] or [fallback].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AnimatedSvg(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
    fallback: @Composable () -> Unit = {}
) {
    val cleanUrl = url?.trim()?.takeIf {
        it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
    }
    if (cleanUrl == null) {
        Box(modifier = modifier) { fallback() }
        return
    }

    val isPreview = LocalInspectionMode.current
    var webViewFailed by remember(cleanUrl) { mutableStateOf(false) }

    val semanticModifier = if (!contentDescription.isNullOrBlank()) {
        modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        modifier
    }

    if (reducedMotion || isPreview || webViewFailed) {
        StaticSvgFallback(
            url = cleanUrl,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = semanticModifier,
            fallback = fallback
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fitCss = when (contentScale) {
        ContentScale.Crop -> "cover"
        ContentScale.FillBounds -> "fill"
        else -> "contain"
    }
    val safeEscapedUrl = remember(cleanUrl) {
        cleanUrl
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
    val htmlDocument = remember(safeEscapedUrl, fitCss) {
        """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
          <style>
            html, body {
              margin: 0; padding: 0; width: 100%; height: 100%;
              background: transparent; overflow: hidden;
              display: flex; align-items: center; justify-content: center;
            }
            img, object {
              width: 100%; height: 100%; object-fit: $fitCss;
              display: block; pointer-events: none; user-select: none;
            }
          </style>
        </head>
        <body>
          <img src="$safeEscapedUrl" alt="" />
        </body>
        </html>
        """.trimIndent()
    }

    val webView = remember(context) {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
            settings.apply {
                javaScriptEnabled = false
                allowFileAccess = false
                allowContentAccess = false
                domStorageEnabled = false
                databaseEnabled = false
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
        }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    Box(modifier = semanticModifier) {
        AndroidView(
            factory = {
                webView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        val failedUrl = request?.url?.toString().orEmpty()
                        if (request?.isForMainFrame == true || failedUrl.equals(cleanUrl, ignoreCase = true)) {
                            webViewFailed = true
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: android.webkit.WebResourceResponse?
                    ) {
                        val failedUrl = request?.url?.toString().orEmpty()
                        if (failedUrl.equals(cleanUrl, ignoreCase = true) && (errorResponse?.statusCode ?: 200) >= 400) {
                            webViewFailed = true
                        }
                    }
                }
                webView
            },
            update = { view ->
                if (view.tag != htmlDocument) {
                    view.tag = htmlDocument
                    val base = cleanUrl.substringBeforeLast('/') + "/"
                    view.loadDataWithBaseURL(base, htmlDocument, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun StaticSvgFallback(
    url: String,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier,
    fallback: @Composable () -> Unit
) {
    val context = LocalContext.current
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .decoderFactory(SvgDecoder.Factory())
            .crossfade(false)
            .build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { fallback() },
        error = { fallback() }
    )
}
