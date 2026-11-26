package com.example.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Downloads and caches remote font files referenced by `kit.fonts` (if a `.ttf` or `.otf` URL
 * is provided by the manifest) into `context.cacheDir/melvore_fonts/`, falling back smoothly to
 * Melvore's bundled local fonts while loading or when offline.
 */
object RemoteFontCache {
    private val memoryCache = ConcurrentHashMap<String, FontFamily>()

    suspend fun loadFontFamily(context: Context, fontUrl: String?): FontFamily? {
        val clean = fontUrl?.trim()?.takeIf {
            (it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)) &&
                (it.endsWith(".ttf", ignoreCase = true) || it.endsWith(".otf", ignoreCase = true))
        } ?: return null

        memoryCache[clean]?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "melvore_fonts").apply { mkdirs() }
                val ext = if (clean.endsWith(".otf", ignoreCase = true)) "otf" else "ttf"
                val file = File(dir, "${sha256Short(clean)}.$ext")
                if (!file.exists() || file.length() < 512L) {
                    val conn = (URL(clean).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 8000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "font/ttf,font/otf,application/octet-stream")
                    }
                    try {
                        if (conn.responseCode in 200..299) {
                            conn.inputStream.use { input ->
                                file.outputStream().use { output -> input.copyTo(output) }
                            }
                        } else {
                            return@runCatching null
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
                val typeface = Typeface.createFromFile(file)
                val family = FontFamily(typeface)
                memoryCache[clean] = family
                family
            }.getOrNull()
        }
    }

    private fun sha256Short(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.take(12).joinToString("") { "%02x".format(it) }
    }
}

@Composable
fun rememberRemoteOrBundledFontFamily(
    remoteFontUrl: String?,
    fallbackStyleKey: String
): FontFamily {
    val context = LocalContext.current
    val fallback = remember(fallbackStyleKey) { fontFamilyForStyleKey(fallbackStyleKey) }
    var resolved by remember(remoteFontUrl, fallbackStyleKey) { mutableStateOf(fallback) }

    LaunchedEffect(remoteFontUrl) {
        if (!remoteFontUrl.isNullOrBlank()) {
            val loaded = RemoteFontCache.loadFontFamily(context, remoteFontUrl)
            if (loaded != null) {
                resolved = loaded
            }
        }
    }
    return resolved
}
