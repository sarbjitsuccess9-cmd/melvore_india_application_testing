package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProductReview
import com.example.model.ReviewEligibilityItem
import com.example.ui.components.LiquidGlassSheetContainer
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.viewmodel.DiscoveryUiState

@Composable
fun ProductReviewCard(
    review: ProductReview,
    onToggleHelpful: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(review.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (review.isVerifiedPurchase) {
                        Surface(
                            shape = RoundedCornerShape(1.dp),
                            color = ext.emerald.copy(alpha = 0.16f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Outlined.Verified, contentDescription = null, tint = ext.emerald, modifier = Modifier.size(11.dp))
                                Text("VERIFIED BUYER", style = MaterialTheme.typography.labelSmall, color = ext.emerald, fontSize = 9.sp)
                            }
                        }
                    }
                }
                Text(
                    text = "★".repeat(review.rating.coerceIn(1, 5)),
                    color = ext.goldAccent,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (review.title.isNotBlank()) {
                Text(review.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            if (review.comment.isNotBlank()) {
                Text(review.comment, style = MaterialTheme.typography.bodySmall, color = ext.muted)
            }

            if (review.mediaUrls.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 2.dp)
                ) {
                    items(review.mediaUrls) { mediaUrl ->
                        MelvoreNetworkImage(
                            url = mediaUrl,
                            contentDescription = "Customer review photo",
                            modifier = Modifier.size(78.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (review.createdAt.isNotBlank()) {
                    Text(review.createdAt.take(10), style = MaterialTheme.typography.labelSmall, color = ext.muted)
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Surface(
                    onClick = { onToggleHelpful(review.id) },
                    shape = RoundedCornerShape(2.dp),
                    color = if (review.hasVotedHelpful) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, ext.borderSubtle),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("review_helpful_${review.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (review.hasVotedHelpful) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Helpful",
                            tint = if (review.hasVotedHelpful) MaterialTheme.colorScheme.onPrimary else ext.muted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Helpful (${review.helpfulCount})",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (review.hasVotedHelpful) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteVerifiedReviewSheet(
    productId: Int,
    productName: String,
    eligibleItems: List<ReviewEligibilityItem>,
    discoveryState: DiscoveryUiState,
    onDismiss: () -> Unit,
    onRequestOtp: (orderItemId: Int) -> Unit,
    onVerifyOtp: (otp: String) -> Unit,
    onUploadPhoto: (Uri) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onSubmitReview: (productId: Int, orderItemId: Int, rating: Int, title: String, comment: String) -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedItem by remember(eligibleItems) { mutableStateOf(eligibleItems.firstOrNull()) }
    var otpInput by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(5) }
    var titleInput by remember { mutableStateOf("") }
    var commentInput by remember { mutableStateOf("") }

    // Zero-permission Android Photo Picker
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUploadPhoto(uri)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        LiquidGlassSheetContainer(
            modifier = Modifier.fillMaxWidth()
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "VERIFIED PURCHASE REVIEW",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.6.sp,
                color = ext.goldAccent,
                fontWeight = FontWeight.Bold
            )
            Text(productName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (eligibleItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Text(
                        text = "Only verified buyers who purchased and received this piece can submit a review. Sign in with the purchasing account or complete delivery first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.muted,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Close")
                }
                return@LiquidGlassSheetContainer
            }

            // Step 1: Select Eligible Order Item
            Text("1. Select Purchased Order", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            eligibleItems.forEach { item ->
                val isSelected = selectedItem?.orderItemId == item.orderItemId
                Surface(
                    onClick = { selectedItem = item },
                    shape = RoundedCornerShape(2.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else ext.borderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Order #${item.orderNumber}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(item.productName, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                        }
                        if (item.orderedAt.isNotBlank()) {
                            Text(item.orderedAt.take(10), style = MaterialTheme.typography.labelSmall, color = ext.muted)
                        }
                    }
                }
            }

            // Step 2: Email OTP Verification
            Text("2. Confirm Delivery via Email OTP", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            if (discoveryState.isReviewOtpBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            val otpState = discoveryState.reviewOtpState
            if (!discoveryState.isReviewOtpVerified) {
                if (otpState == null || otpState.orderItemId != selectedItem?.orderItemId) {
                    Button(
                        onClick = { selectedItem?.let { onRequestOtp(it.orderItemId) } },
                        enabled = selectedItem != null && !discoveryState.isReviewOtpBusy,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("review_request_otp_btn")
                    ) {
                        Text("Send Verification Code")
                    }
                } else {
                    Text(
                        text = "Enter the 6-digit code sent to ${otpState.sentTo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.emerald
                    )
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("6-Digit OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("review_otp_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { selectedItem?.let { onRequestOtp(it.orderItemId) } },
                            enabled = !discoveryState.isReviewOtpBusy,
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text("Resend")
                        }
                        Button(
                            onClick = { onVerifyOtp(otpInput) },
                            enabled = otpInput.length >= 4 && !discoveryState.isReviewOtpBusy,
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("review_verify_otp_btn")
                        ) {
                            Text("Verify OTP")
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = ext.emerald.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ext.emerald.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Icon(Icons.Outlined.Verified, contentDescription = null, tint = ext.emerald)
                        Text("Verified purchase confirmed", style = MaterialTheme.typography.labelLarge, color = ext.emerald)
                    }
                }

                // Step 3: Rating, Photos (via Android Photo Picker) & Written Review
                Text("3. Rate & Share Photos", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { rating = star },
                            modifier = Modifier.minimumInteractiveComponentSize().size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "$star stars",
                                tint = if (star <= rating) ext.goldAccent else ext.muted,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Headline (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    label = { Text("Fit, fabric & styling notes") },
                    minLines = 3,
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("review_comment_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !discoveryState.isReviewUploading && discoveryState.uploadedReviewPhotoUrls.size < 3,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (discoveryState.isReviewUploading) "Uploading…"
                            else "Add Photo (${discoveryState.uploadedReviewPhotoUrls.size}/3)"
                        )
                    }
                }

                if (discoveryState.uploadedReviewPhotoUrls.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(discoveryState.uploadedReviewPhotoUrls) { url ->
                            Box(modifier = Modifier.size(72.dp)) {
                                MelvoreNetworkImage(
                                    url = url,
                                    contentDescription = "Uploaded review photo",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    onClick = { onRemovePhoto(url) },
                                    shape = RoundedCornerShape(0.dp),
                                    color = surfaces.imageOverlay.copy(alpha = 0.72f),
                                    modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = surfaces.onImageOverlay, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        selectedItem?.let { item ->
                            onSubmitReview(productId, item.orderItemId, rating, titleInput, commentInput)
                        }
                    },
                    enabled = !discoveryState.isSubmittingReview && commentInput.trim().length >= 5,
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("review_submit_btn")
                ) {
                    Text(if (discoveryState.isSubmittingReview) "Publishing…" else "Publish Verified Review")
                }
            }
        }
        }
    }
}
