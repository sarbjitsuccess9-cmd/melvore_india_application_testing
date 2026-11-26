package com.example.network

import com.example.BuildConfig
import com.example.data.SecureSessionStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(
        val message: String,
        val errorCode: String? = null,
        val httpStatus: Int? = null
    ) : ApiResult<Nothing>
}

class MelvoreApiClient(
    private val sessionStore: SecureSessionStore
) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val guestId = sessionStore.getOrCreateGuestId()
            val token = sessionStore.getAuthToken()
            val reqBuilder = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("X-Guest-Id", guestId)
            if (!token.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer $token")
            }
            chain.proceed(reqBuilder.build())
        }
        .addInterceptor(NetworkDiagnosticsMonitor.interceptor)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                        redactHeader("Cookie")
                    }
                )
            }
        }
        .build()

    @Volatile
    private var cachedBaseUrl: String = ""

    @Volatile
    private var cachedService: MelvoreApiService? = null

    @Volatile
    private var cachedDiscoveryService: DiscoveryApiService? = null

    fun rawOkHttpClient(): OkHttpClient = okHttpClient

    fun invalidateRetrofitCache() {
        synchronized(this) {
            cachedBaseUrl = ""
            cachedService = null
            cachedDiscoveryService = null
        }
    }

    fun service(): MelvoreApiService {
        val currentBase = sessionStore.getApiBaseUrl()
        val existing = cachedService
        if (existing != null && cachedBaseUrl == currentBase) {
            return existing
        }
        synchronized(this) {
            val recheckBase = sessionStore.getApiBaseUrl()
            val recheck = cachedService
            if (recheck != null && cachedBaseUrl == recheckBase) {
                return recheck
            }
            val retrofit = Retrofit.Builder()
                .baseUrl(recheckBase)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            val created = retrofit.create(MelvoreApiService::class.java)
            val createdDiscovery = retrofit.create(DiscoveryApiService::class.java)
            cachedBaseUrl = recheckBase
            cachedService = created
            cachedDiscoveryService = createdDiscovery
            return created
        }
    }

    fun discoveryService(): DiscoveryApiService {
        service()
        return cachedDiscoveryService ?: synchronized(this) {
            service()
            cachedDiscoveryService!!
        }
    }

    suspend fun <T> safeCall(block: suspend (MelvoreApiService) -> T): ApiResult<T> =
        executeSafely { block(service()) }

    suspend fun <T> safeDiscoveryCall(block: suspend (DiscoveryApiService) -> T): ApiResult<T> =
        executeSafely { block(discoveryService()) }

    private suspend fun <T> executeSafely(block: suspend () -> T): ApiResult<T> {
        return try {
            val result = block()
            // Enforce HTTP 200 with { "success": false } as an explicit ApiResult.Error
            // rather than silently succeeding with empty lists.
            if (result is ApiEnvelope<*> && !result.success) {
                return ApiResult.Error(
                    message = result.message?.takeIf { it.isNotBlank() }
                        ?: "Server returned success=false.",
                    errorCode = result.errorCode,
                    httpStatus = 200
                )
            }
            if (result is StrictSuccessResponse && !result.success) {
                return ApiResult.Error(
                    message = result.message?.takeIf { it.isNotBlank() }
                        ?: "Server returned success=false.",
                    errorCode = result.errorCode,
                    httpStatus = 200
                )
            }
            ApiResult.Success(result)
        } catch (e: HttpException) {
            val status = e.code()
            val rawBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val envelope = rawBody?.let {
                runCatching { json.decodeFromString<ApiErrorEnvelope>(it) }.getOrNull()
            }
            if (status == 401) {
                sessionStore.clearAuthToken()
            }
            val msg = envelope?.message?.takeIf { it.isNotBlank() }
                ?: when (status) {
                    401 -> "Session expired or authentication required. Please sign in again."
                    403 -> "Access denied for this request."
                    404 -> "Requested resource was not found on the Melvore server."
                    in 500..599 -> "Melvore server error ($status). Please try again shortly."
                    else -> "Request failed with HTTP $status."
                }
            ApiResult.Error(message = msg, errorCode = envelope?.errorCode, httpStatus = status)
        } catch (_: SocketTimeoutException) {
            ApiResult.Error(
                message = "Request timed out while contacting Melvore servers. Please try again.",
                errorCode = "TIMEOUT"
            )
        } catch (_: IOException) {
            ApiResult.Error(
                message = "Unable to connect to Melvore servers. Check your internet connection.",
                errorCode = "NETWORK_ERROR"
            )
        } catch (e: Exception) {
            ApiResult.Error(
                message = e.message ?: "Unexpected error communicating with Melvore.",
                errorCode = "UNKNOWN_ERROR"
            )
        }
    }
}
