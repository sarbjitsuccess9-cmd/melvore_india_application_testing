package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.auth.GoogleCredentialHelper
import com.example.auth.GoogleSignInOutcome
import com.example.ui.components.MelvoreWordmark
import com.example.ui.theme.LocalMelvoreColors
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    initialTab: String,
    isAuthBusy: Boolean,
    onLogin: (email: String, password: String, onSuccess: () -> Unit) -> Unit,
    onRegister: (name: String, email: String, phone: String, password: String, onSuccess: () -> Unit) -> Unit,
    onGoogleIdToken: (idToken: String, onSuccess: () -> Unit) -> Unit,
    onSendOtp: (email: String, onSent: () -> Unit) -> Unit,
    onVerifyOtp: (email: String, otp: String, onSuccess: () -> Unit) -> Unit,
    onRequestPasswordReset: (email: String, onSent: () -> Unit) -> Unit,
    onResetPassword: (email: String, otp: String, newPassword: String, onSuccess: () -> Unit) -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember(initialTab) {
        mutableStateOf(
            when (initialTab) {
                "register" -> "register"
                "otp" -> "otp"
                "forgot" -> "forgot"
                else -> "login"
            }
        )
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var resetOtpSent by remember { mutableStateOf(false) }
    var newResetPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var isGoogleLaunching by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MelvoreWordmark(fontSize = 30.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    when (activeTab) {
                        "register" -> "Join Melvore for curated outerwear & member perks"
                        "otp" -> "Sign in with a 6-digit email verification code"
                        "forgot" -> "Reset your Melvore account password"
                        else -> "Sign in to manage orders, wishlist & addresses"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
            }
        }

        // Segmented Tabs (Login / Sign Up)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf("login" to "Login", "register" to "Sign Up").forEach { (key, label) ->
                        val selected = activeTab == key || (key == "login" && activeTab in setOf("otp", "forgot"))
                        Surface(
                            onClick = {
                                activeTab = key
                                localError = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = if (selected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("auth_tab_$key")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else ext.muted
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeTab == "register") {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    if (activeTab == "register") {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_phone_input")
                        )
                    }

                    if (activeTab == "login" || activeTab == "register") {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (showPassword) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )
                    }

                    if (activeTab == "register") {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_confirm_password_input")
                        )
                    }

                    if (activeTab == "otp" && otpSent) {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("6-Digit OTP") },
                            leadingIcon = { Icon(Icons.Outlined.Pin, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (activeTab == "forgot" && resetOtpSent) {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("Reset OTP") },
                            leadingIcon = { Icon(Icons.Outlined.Pin, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newResetPassword,
                            onValueChange = { newResetPassword = it },
                            label = { Text("New Password (min 6 chars)") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (localError != null) {
                        Text(
                            text = localError!!,
                            color = ext.crimson,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            localError = null
                            when (activeTab) {
                                "login" -> onLogin(email, password, onAuthSuccess)
                                "register" -> {
                                    if (password != confirmPassword) {
                                        localError = "Passwords do not match"
                                    } else {
                                        onRegister(fullName, email, phone, password, onAuthSuccess)
                                    }
                                }
                                "otp" -> {
                                    if (!otpSent) {
                                        onSendOtp(email) { otpSent = true }
                                    } else {
                                        onVerifyOtp(email, otpCode, onAuthSuccess)
                                    }
                                }
                                "forgot" -> {
                                    if (!resetOtpSent) {
                                        onRequestPasswordReset(email) { resetOtpSent = true }
                                    } else {
                                        onResetPassword(email, otpCode, newResetPassword) {
                                            resetOtpSent = false
                                            otpCode = ""
                                            newResetPassword = ""
                                            activeTab = "login"
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isAuthBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button")
                    ) {
                        if (isAuthBusy) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                when (activeTab) {
                                    "register" -> "Create Account"
                                    "otp" -> if (otpSent) "Verify OTP & Sign In" else "Send Login OTP"
                                    "forgot" -> if (resetOtpSent) "Reset Password" else "Send Reset OTP"
                                    else -> "Sign In"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                localError = null
                                activeTab = if (activeTab == "otp") "login" else "otp"
                            }
                        ) {
                            Text(if (activeTab == "otp") "Password Sign-In" else "Email OTP Login")
                        }
                        TextButton(
                            onClick = {
                                localError = null
                                activeTab = if (activeTab == "forgot") "login" else "forgot"
                            }
                        ) {
                            Text(if (activeTab == "forgot") "Back to Login" else "Forgot Password?")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ext.borderSubtle)
                        Text("OR", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ext.borderSubtle)
                    }

                    OutlinedButton(
                        onClick = {
                            if (isGoogleLaunching || isAuthBusy) return@OutlinedButton
                            localError = null
                            isGoogleLaunching = true
                            scope.launch {
                                val outcome = GoogleCredentialHelper.signIn(
                                    context = context,
                                    webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                                )
                                isGoogleLaunching = false
                                when (outcome) {
                                    is GoogleSignInOutcome.Success -> onGoogleIdToken(outcome.idToken, onAuthSuccess)
                                    is GoogleSignInOutcome.Cancelled -> localError = outcome.message
                                    is GoogleSignInOutcome.Error -> localError = outcome.message
                                }
                            }
                        },
                        enabled = !isAuthBusy && !isGoogleLaunching,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_google_button")
                    ) {
                        Text(
                            if (isGoogleLaunching) "Connecting Google…" else "Continue with Google",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
