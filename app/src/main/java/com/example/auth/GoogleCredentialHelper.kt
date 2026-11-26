package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom

sealed interface GoogleSignInOutcome {
    data class Success(val idToken: String) : GoogleSignInOutcome
    data class Cancelled(val message: String = "Google Sign-In was cancelled.") : GoogleSignInOutcome
    data class Error(val message: String) : GoogleSignInOutcome
}

object GoogleCredentialHelper {

    fun generateSecureNonce(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    suspend fun signIn(context: Context, webClientId: String): GoogleSignInOutcome {
        val cleanClientId = webClientId.trim()
        if (cleanClientId.isEmpty()) {
            return GoogleSignInOutcome.Error(
                "Google Web Client ID is not configured in AI Studio Secrets (GOOGLE_WEB_CLIENT_ID)."
            )
        }
        if (!cleanClientId.endsWith(".apps.googleusercontent.com")) {
            return GoogleSignInOutcome.Error(
                "Invalid GOOGLE_WEB_CLIENT_ID format. Expected a Google Web OAuth Client ID ending in .apps.googleusercontent.com."
            )
        }

        val activity = context.findActivity()
            ?: return GoogleSignInOutcome.Error("Unable to launch Google Sign-In without an active Activity window.")
        val activityContext = MutableContextWrapper(activity)
        val credentialManager = CredentialManager.create(activityContext)
        val nonce = generateSecureNonce()

        // Primary flow: explicit Sign in with Google button option
        val buttonOption = GetSignInWithGoogleOption.Builder(cleanClientId)
            .setNonce(nonce)
            .build()
        val primaryRequest = GetCredentialRequest.Builder()
            .addCredentialOption(buttonOption)
            .build()

        val primaryResult = try {
            return extractGoogleToken(
                credentialManager.getCredential(request = primaryRequest, context = activityContext)
            )
        } catch (cancelled: GetCredentialCancellationException) {
            return GoogleSignInOutcome.Cancelled("Google Sign-In was cancelled.")
        } catch (err: GetCredentialException) {
            err
        } catch (err: Exception) {
            return GoogleSignInOutcome.Error(err.message ?: "Unexpected error launching Google Sign-In.")
        }

        // Fallback flow: GetGoogleIdOption with filterByAuthorizedAccounts = false
        // Helps when GetSignInWithGoogleOption hits Play Services reauth/account-picker edge cases.
        val rawPrimaryMsg = primaryResult.errorMessage?.toString() ?: primaryResult.message.orEmpty()
        val isPlayServicesUnavailable = primaryResult is GetCredentialProviderConfigurationException ||
            primaryResult is GetCredentialUnsupportedException
        if (!isPlayServicesUnavailable) {
            val fallbackOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(cleanClientId)
                .setNonce(nonce)
                .build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(fallbackOption)
                .build()
            try {
                return extractGoogleToken(
                    credentialManager.getCredential(request = fallbackRequest, context = activityContext)
                )
            } catch (_: GetCredentialCancellationException) {
                return GoogleSignInOutcome.Cancelled("Google Sign-In was cancelled.")
            } catch (fallbackErr: GetCredentialException) {
                return GoogleSignInOutcome.Error(mapCredentialException(fallbackErr, rawPrimaryMsg))
            } catch (err: Exception) {
                return GoogleSignInOutcome.Error(err.message ?: mapCredentialException(primaryResult, rawPrimaryMsg))
            }
        }

        return GoogleSignInOutcome.Error(mapCredentialException(primaryResult, rawPrimaryMsg))
    }

    private fun extractGoogleToken(response: GetCredentialResponse): GoogleSignInOutcome {
        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleSignInOutcome.Error(
                "Received an unsupported credential type (${credential.type}). Expected a Google ID token."
            )
        }
        return try {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            if (googleCred.idToken.isBlank()) {
                GoogleSignInOutcome.Error("Google Sign-In returned an empty ID token.")
            } else {
                GoogleSignInOutcome.Success(googleCred.idToken)
            }
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInOutcome.Error("Failed to parse Google ID token: ${e.message ?: "invalid format"}")
        }
    }

    internal fun mapCredentialException(err: GetCredentialException, priorMessage: String = ""): String {
        val msg = err.errorMessage?.toString()?.takeIf { it.isNotBlank() }
            ?: err.message?.takeIf { it.isNotBlank() }
            ?: priorMessage
        val lower = msg.lowercase()

        if (lower.contains("reauth") || lower.contains("28444") || lower.contains("developer console")) {
            return "Google account re-authentication failed ($msg). " +
                "Verify in Android device Settings that your Google account is signed in cleanly, and ensure Google Cloud Console " +
                "has an Android OAuth client for package com.aistudio.melvore.klmvre matching your APK SHA-1 in the same project as GOOGLE_WEB_CLIENT_ID."
        }

        return when (err) {
            is GetCredentialCancellationException -> "Google Sign-In was cancelled."
            is NoCredentialException -> "No eligible Google account found on this device. Add or re-authenticate a Google account in device Settings and try again."
            is GetCredentialInterruptedException -> "Google Sign-In was interrupted. Please try again."
            is GetCredentialProviderConfigurationException,
            is GetCredentialUnsupportedException -> "Google Credential Manager is unavailable on this device. Update Google Play Services and try again."
            else -> msg.ifBlank { "Google Sign-In failed (${err.type}). Please try again." }
        }
    }

    suspend fun clearCredentialState(context: Context) {
        runCatching {
            val manager = CredentialManager.create(context)
            manager.clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
