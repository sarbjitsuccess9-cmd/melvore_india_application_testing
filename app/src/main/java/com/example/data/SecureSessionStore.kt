package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.BuildConfig
import com.example.model.LocalPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the API Bearer token encrypted via Android Keystore (AES-256-GCM)
 * alongside non-sensitive client preferences (guest cart session id, theme,
 * pending payment order id for Razorpay recovery, and configurable API base URL).
 */
class SecureSessionStore(context: Context) {

    private val securePrefs = context.getSharedPreferences(PREFS_SECURE, Context.MODE_PRIVATE)
    private val appPrefs = context.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    init {
        sanitizeSavedApiBaseUrl()
    }

    /**
     * Drops obviously broken saved API base URL overrides (blank, non-http(s), trailing /api
     * that causes /api/api/ 404s, or localhost/emulator hosts in release builds) so the app
     * cannot silently stay stuck on a stale dev override.
     */
    private fun sanitizeSavedApiBaseUrl() {
        val raw = appPrefs.getString(KEY_API_BASE_URL, null) ?: return
        val trimmed = raw.trim().trimEnd('/')
        val invalid = trimmed.isBlank() ||
            (!trimmed.startsWith("https://", ignoreCase = true) && !trimmed.startsWith("http://", ignoreCase = true)) ||
            trimmed.endsWith("/api", ignoreCase = true) ||
            (!BuildConfig.DEBUG && isLoopbackOrDevHost(trimmed))
        if (invalid) {
            runCatching { android.util.Log.w("SecureSessionStore", "Cleared invalid saved API base URL override: $raw") }
            appPrefs.edit().remove(KEY_API_BASE_URL).apply()
        }
    }

    private fun isLoopbackOrDevHost(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("://localhost") ||
            lower.contains("://127.0.0.1") ||
            lower.contains("://10.0.2.2") ||
            lower.startsWith("http://")
    }

    fun getOrCreateGuestId(): String {
        val existing = appPrefs.getString(KEY_GUEST_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val created = "mlv_" + bytes.joinToString("") { "%02x".format(it) }
        appPrefs.edit().putString(KEY_GUEST_ID, created).apply()
        return created
    }

    fun getMaskedGuestId(): String {
        val id = getOrCreateGuestId()
        return if (id.length <= 10) id else "${id.take(7)}…${id.takeLast(4)}"
    }

    fun getDefaultApiBaseUrl(): String = BuildConfig.MELVORE_API_BASE.trim().trimEnd('/')

    fun getSavedApiBaseUrlOverride(): String? =
        appPrefs.getString(KEY_API_BASE_URL, null)?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }

    fun isUsingCustomApiBaseUrl(): Boolean {
        val saved = getSavedApiBaseUrlOverride() ?: return false
        return !saved.equals(getDefaultApiBaseUrl(), ignoreCase = true)
    }

    fun resetApiBaseUrlToDefault(): String {
        appPrefs.edit().remove(KEY_API_BASE_URL).apply()
        return getDefaultApiBaseUrl()
    }

    fun getApiBaseUrl(): String {
        val saved = getSavedApiBaseUrlOverride()
        return if (saved != null && (saved.startsWith("https://", ignoreCase = true) || saved.startsWith("http://", ignoreCase = true))) {
            saved.trimEnd('/') + "/"
        } else {
            getDefaultApiBaseUrl() + "/"
        }
    }

    fun setApiBaseUrl(url: String): String {
        var normalized = url.trim().trimEnd('/')
        if (normalized.endsWith("/api", ignoreCase = true)) {
            normalized = normalized.dropLast(4).trimEnd('/')
        }
        val valid = if (normalized.startsWith("https://", ignoreCase = true) || normalized.startsWith("http://", ignoreCase = true)) {
            normalized
        } else {
            getDefaultApiBaseUrl()
        }
        if (valid.equals(getDefaultApiBaseUrl(), ignoreCase = true)) {
            appPrefs.edit().remove(KEY_API_BASE_URL).apply()
        } else {
            appPrefs.edit().putString(KEY_API_BASE_URL, valid).apply()
        }
        return valid
    }

    fun getPreferences(): LocalPreferences {
        val raw = appPrefs.getString(KEY_UI_PREFS, null) ?: return LocalPreferences()
        return runCatching { json.decodeFromString<LocalPreferences>(raw) }
            .getOrElse { LocalPreferences() }
    }

    fun savePreferences(prefs: LocalPreferences) {
        appPrefs.edit()
            .putString(KEY_UI_PREFS, json.encodeToString(LocalPreferences.serializer(), prefs))
            .apply()
    }

    fun getPendingPaymentOrderId(): Int? {
        val id = appPrefs.getInt(KEY_PENDING_ORDER_ID, -1)
        return if (id > 0) id else null
    }

    fun setPendingPaymentOrderId(orderId: Int?) {
        if (orderId == null || orderId <= 0) {
            appPrefs.edit().remove(KEY_PENDING_ORDER_ID).apply()
        } else {
            appPrefs.edit().putInt(KEY_PENDING_ORDER_ID, orderId).apply()
        }
    }

    fun getDeliveryPincode(): String = appPrefs.getString(KEY_DELIVERY_PINCODE, "")?.trim().orEmpty()

    fun saveDeliveryPincode(pincode: String, state: String = "", city: String = "") {
        appPrefs.edit()
            .putString(KEY_DELIVERY_PINCODE, pincode.trim())
            .putString(KEY_DELIVERY_STATE, state.trim())
            .putString(KEY_DELIVERY_CITY, city.trim())
            .apply()
    }

    fun getDeliveryState(): String = appPrefs.getString(KEY_DELIVERY_STATE, "")?.trim().orEmpty()

    fun getDeliveryCity(): String = appPrefs.getString(KEY_DELIVERY_CITY, "")?.trim().orEmpty()

    fun getRecentSearches(): List<String> {
        val raw = appPrefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        val updated = (listOf(clean) + getRecentSearches().filterNot { it.equals(clean, ignoreCase = true) }).take(10)
        appPrefs.edit()
            .putString(KEY_RECENT_SEARCHES, json.encodeToString(updated))
            .apply()
    }

    fun clearRecentSearches() {
        appPrefs.edit().remove(KEY_RECENT_SEARCHES).apply()
    }

    fun saveAuthToken(token: String) {
        if (token.isBlank()) {
            clearAuthToken()
            return
        }
        val encrypted = encryptWithKeystore(token)
        if (encrypted != null) {
            securePrefs.edit()
                .putString(KEY_TOKEN_CIPHER, encrypted.first)
                .putString(KEY_TOKEN_IV, encrypted.second)
                .remove(KEY_TOKEN_FALLBACK)
                .apply()
        } else {
            // Only reached when AndroidKeyStore provider is absent (e.g., Robolectric JVM unit tests)
            val encoded = Base64.encodeToString(token.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            securePrefs.edit()
                .putString(KEY_TOKEN_FALLBACK, encoded)
                .remove(KEY_TOKEN_CIPHER)
                .remove(KEY_TOKEN_IV)
                .apply()
        }
    }

    fun getAuthToken(): String? {
        val cipherB64 = securePrefs.getString(KEY_TOKEN_CIPHER, null)
        val ivB64 = securePrefs.getString(KEY_TOKEN_IV, null)
        if (!cipherB64.isNullOrBlank() && !ivB64.isNullOrBlank()) {
            return decryptWithKeystore(cipherB64, ivB64)
        }
        val fallback = securePrefs.getString(KEY_TOKEN_FALLBACK, null) ?: return null
        return runCatching {
            String(Base64.decode(fallback, Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrNull()
    }

    fun clearAuthToken() {
        securePrefs.edit()
            .remove(KEY_TOKEN_CIPHER)
            .remove(KEY_TOKEN_IV)
            .remove(KEY_TOKEN_FALLBACK)
            .apply()
    }

    private fun getOrCreateKeystoreSecretKey(): SecretKey? {
        return runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return@runCatching existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }.getOrNull()
    }

    private fun encryptWithKeystore(plaintext: String): Pair<String, String>? {
        val key = getOrCreateKeystoreSecretKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val ivBytes = cipher.iv
            Base64.encodeToString(cipherBytes, Base64.NO_WRAP) to
                Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun decryptWithKeystore(cipherB64: String, ivB64: String): String? {
        val key = getOrCreateKeystoreSecretKey() ?: return null
        return runCatching {
            val cipherBytes = Base64.decode(cipherB64, Base64.NO_WRAP)
            val ivBytes = Base64.decode(ivB64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, ivBytes))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrNull()
    }

    fun getBootstrapConfigJson(): String? = appPrefs.getString(KEY_BOOTSTRAP_JSON, null)

    fun getBootstrapConfigVersion(): String = appPrefs.getString(KEY_BOOTSTRAP_VERSION, "")?.trim().orEmpty()

    fun getBootstrapSavedAtMs(): Long = appPrefs.getLong(KEY_BOOTSTRAP_SAVED_AT, 0L)

    fun saveBootstrapConfigJson(version: String, rawJson: String) {
        val now = System.currentTimeMillis()
        appPrefs.edit()
            .putString(KEY_BOOTSTRAP_VERSION, version)
            .putString(KEY_BOOTSTRAP_JSON, rawJson)
            .putLong(KEY_BOOTSTRAP_SAVED_AT, now)
            .apply()
        recordEndpointUpdated("bootstrap", now)
    }

    fun getHomeFeedCacheJson(): String? = appPrefs.getString(KEY_HOME_FEED_JSON, null)

    fun getHomeFeedSavedAtMs(): Long = appPrefs.getLong(KEY_HOME_FEED_SAVED_AT, 0L)

    fun saveHomeFeedCacheJson(rawJson: String) {
        val now = System.currentTimeMillis()
        appPrefs.edit()
            .putString(KEY_HOME_FEED_JSON, rawJson)
            .putLong(KEY_HOME_FEED_SAVED_AT, now)
            .apply()
        recordEndpointUpdated("home_feed", now)
    }

    fun recordEndpointUpdated(endpointKey: String, timestampMs: Long = System.currentTimeMillis()) {
        val clean = endpointKey.trim().lowercase().replace(Regex("[^a-z0-9_]+"), "_")
        if (clean.isBlank()) return
        appPrefs.edit().putLong("${KEY_ENDPOINT_UPDATED_PREFIX}$clean", timestampMs).apply()
    }

    fun getEndpointUpdatedAtMs(endpointKey: String): Long {
        val clean = endpointKey.trim().lowercase().replace(Regex("[^a-z0-9_]+"), "_")
        if (clean.isBlank()) return 0L
        return appPrefs.getLong("${KEY_ENDPOINT_UPDATED_PREFIX}$clean", 0L)
    }

    fun getOfferCooldownUntilMs(offerId: Int): Long =
        appPrefs.getLong("${KEY_OFFER_COOLDOWN_PREFIX}$offerId", 0L)

    fun isOfferDismissedForever(offerId: Int): Boolean =
        appPrefs.getBoolean("${KEY_OFFER_DISMISSED_PREFIX}$offerId", false)

    fun recordOfferCooldown(offerId: Int, cooldownHours: Int, dismissedForever: Boolean = false) {
        val untilMs = System.currentTimeMillis() + (cooldownHours.coerceAtLeast(1) * 3_600_000L)
        appPrefs.edit()
            .putLong("${KEY_OFFER_COOLDOWN_PREFIX}$offerId", untilMs)
            .apply {
                if (dismissedForever) {
                    putBoolean("${KEY_OFFER_DISMISSED_PREFIX}$offerId", true)
                }
            }
            .apply()
    }

    fun canShowOffer(
        offerId: Int,
        maxPerSession: Int,
        sessionShownCount: Int,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (offerId <= 0) return false
        if (isOfferDismissedForever(offerId)) return false
        if (sessionShownCount >= maxPerSession.coerceAtLeast(1)) return false
        val cooldownUntil = getOfferCooldownUntilMs(offerId)
        return nowMs >= cooldownUntil
    }

    fun clearOfferStatesForTest() {
        val editor = appPrefs.edit()
        appPrefs.all.keys.filter {
            it.startsWith(KEY_OFFER_COOLDOWN_PREFIX) || it.startsWith(KEY_OFFER_DISMISSED_PREFIX)
        }.forEach { editor.remove(it) }
        editor.apply()
    }

    private fun currentFeedbackActorSuffix(): String {
        val token = getAuthToken()
        return if (!token.isNullOrBlank()) {
            "u_${token.hashCode().toUInt().toString(16)}"
        } else {
            "g_${getOrCreateGuestId()}"
        }
    }

    fun getSubmittedWorldIds(): Set<Int> {
        val raw = appPrefs.getString("${KEY_SUBMITTED_WORLD_IDS_PREFIX}${currentFeedbackActorSuffix()}", null)
            ?: return emptySet()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.toSet()
    }

    fun getSubmittedWorldSlugs(): Set<String> {
        val raw = appPrefs.getString("${KEY_SUBMITTED_WORLD_SLUGS_PREFIX}${currentFeedbackActorSuffix()}", null)
            ?: return emptySet()
        return raw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }

    fun hasSubmittedFeedbackWorld(worldId: Int, worldSlug: String): Boolean {
        val idMatch = worldId > 0 && worldId in getSubmittedWorldIds()
        val slugMatch = worldSlug.isNotBlank() && worldSlug.trim().lowercase() in getSubmittedWorldSlugs()
        return idMatch || slugMatch
    }

    fun recordSubmittedFeedbackWorld(worldId: Int, worldSlug: String) {
        val actor = currentFeedbackActorSuffix()
        val ids = getSubmittedWorldIds().toMutableSet().apply { if (worldId > 0) add(worldId) }
        val slugs = getSubmittedWorldSlugs().toMutableSet().apply {
            val clean = worldSlug.trim().lowercase()
            if (clean.isNotBlank()) add(clean)
        }
        appPrefs.edit()
            .putString("${KEY_SUBMITTED_WORLD_IDS_PREFIX}$actor", ids.joinToString(","))
            .putString("${KEY_SUBMITTED_WORLD_SLUGS_PREFIX}$actor", slugs.joinToString(","))
            .apply()
    }

    fun syncSubmittedWorldsFromBootstrap(
        worldIds: List<Int>,
        worldSlugs: List<String>,
        replaceExisting: Boolean = false
    ) {
        val actor = currentFeedbackActorSuffix()
        val ids = if (replaceExisting) mutableSetOf() else getSubmittedWorldIds().toMutableSet()
        val slugs = if (replaceExisting) mutableSetOf() else getSubmittedWorldSlugs().toMutableSet()
        worldIds.filter { it > 0 }.forEach { ids.add(it) }
        worldSlugs.map { it.trim().lowercase() }.filter { it.isNotBlank() }.forEach { slugs.add(it) }
        appPrefs.edit()
            .putString("${KEY_SUBMITTED_WORLD_IDS_PREFIX}$actor", ids.joinToString(","))
            .putString("${KEY_SUBMITTED_WORLD_SLUGS_PREFIX}$actor", slugs.joinToString(","))
            .apply()
    }

    fun saveLastKnownGoodManifest(
        manifestJson: String,
        activeDesign: Int,
        versionTag: String,
        checksum: String,
        etag: String = "",
        assetBaseUrl: String = "",
        manifestHash: String = ""
    ) {
        appPrefs.edit()
            .putString(KEY_LKG_DESIGN_MANIFEST_JSON, manifestJson)
            .putInt(KEY_LKG_ACTIVE_DESIGN, if (activeDesign == 1) 1 else 0)
            .putString(KEY_LKG_DESIGN_VERSION, versionTag)
            .putString(KEY_LKG_DESIGN_CHECKSUM, checksum)
            .putLong(KEY_LKG_DESIGN_SAVED_AT, System.currentTimeMillis())
            .apply {
                if (etag.isNotBlank()) putString(KEY_LKG_DESIGN_ETAG, etag)
                if (assetBaseUrl.isNotBlank()) putString(KEY_LKG_ASSET_BASE_URL, assetBaseUrl)
                if (manifestHash.isNotBlank()) putString(KEY_LKG_MANIFEST_HASH, manifestHash)
            }
            .apply()
    }

    fun getLastKnownGoodManifestJson(): String? =
        appPrefs.getString(KEY_LKG_DESIGN_MANIFEST_JSON, null)

    fun getLastKnownGoodActiveDesign(): Int =
        appPrefs.getInt(KEY_LKG_ACTIVE_DESIGN, 0)

    fun getLastKnownGoodDesignVersion(): String =
        appPrefs.getString(KEY_LKG_DESIGN_VERSION, "") ?: ""

    fun getLastKnownGoodChecksum(): String =
        appPrefs.getString(KEY_LKG_DESIGN_CHECKSUM, "") ?: ""

    fun getLastKnownGoodEtag(): String =
        appPrefs.getString(KEY_LKG_DESIGN_ETAG, "") ?: ""

    fun getLastKnownGoodAssetBaseUrl(): String =
        appPrefs.getString(KEY_LKG_ASSET_BASE_URL, "") ?: ""

    fun getLastKnownGoodManifestHash(): String =
        appPrefs.getString(KEY_LKG_MANIFEST_HASH, "") ?: ""

    fun getManifestSavedAtMs(): Long =
        appPrefs.getLong(KEY_LKG_DESIGN_SAVED_AT, 0L)

    fun getPreferredWorldSlug(): String? =
        appPrefs.getString(KEY_PREFERRED_WORLD_SLUG, null)?.trim()?.takeIf { it.isNotBlank() }

    fun savePreferredWorldSlug(slug: String?) {
        val clean = slug?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        appPrefs.edit().apply {
            if (clean == null) remove(KEY_PREFERRED_WORLD_SLUG)
            else putString(KEY_PREFERRED_WORLD_SLUG, clean)
        }.apply()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "melvore_auth_token_aes_key"
        private const val AES_GCM_TRANSFORM = "AES/GCM/NoPadding"

        private const val PREFS_SECURE = "melvore_secure_session"
        private const val PREFS_APP = "melvore_app_prefs"
        private const val KEY_TOKEN_CIPHER = "token_cipher"
        private const val KEY_TOKEN_IV = "token_iv"
        private const val KEY_TOKEN_FALLBACK = "token_fallback"
        private const val KEY_GUEST_ID = "guest_session_id"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_UI_PREFS = "ui_preferences"
        private const val KEY_PENDING_ORDER_ID = "pending_payment_order_id"
        private const val KEY_DELIVERY_PINCODE = "delivery_pincode"
        private const val KEY_DELIVERY_STATE = "delivery_state"
        private const val KEY_DELIVERY_CITY = "delivery_city"
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val KEY_BOOTSTRAP_JSON = "dynamic_bootstrap_bundle_json"
        private const val KEY_BOOTSTRAP_VERSION = "dynamic_bootstrap_version"
        private const val KEY_BOOTSTRAP_SAVED_AT = "dynamic_bootstrap_saved_at"
        private const val KEY_LKG_DESIGN_MANIFEST_JSON = "lkg_design_manifest_json"
        private const val KEY_LKG_ACTIVE_DESIGN = "lkg_active_design_flag"
        private const val KEY_LKG_DESIGN_VERSION = "lkg_design_version"
        private const val KEY_LKG_DESIGN_CHECKSUM = "lkg_design_checksum"
        private const val KEY_LKG_DESIGN_SAVED_AT = "lkg_design_saved_at"
        private const val KEY_LKG_DESIGN_ETAG = "lkg_design_etag"
        private const val KEY_LKG_ASSET_BASE_URL = "lkg_design_asset_base_url"
        private const val KEY_LKG_MANIFEST_HASH = "lkg_design_manifest_hash"
        private const val KEY_PREFERRED_WORLD_SLUG = "preferred_fashion_world_slug"
        private const val KEY_OFFER_COOLDOWN_PREFIX = "offer_cooldown_until_"
        private const val KEY_OFFER_DISMISSED_PREFIX = "offer_dismissed_"
        private const val KEY_SUBMITTED_WORLD_IDS_PREFIX = "submitted_world_ids_"
        private const val KEY_SUBMITTED_WORLD_SLUGS_PREFIX = "submitted_world_slugs_"
        private const val KEY_HOME_FEED_JSON = "cached_home_feed_json"
        private const val KEY_HOME_FEED_SAVED_AT = "cached_home_feed_saved_at"
        private const val KEY_ENDPOINT_UPDATED_PREFIX = "endpoint_updated_at_"
    }
}
