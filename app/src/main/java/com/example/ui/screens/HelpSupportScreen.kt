package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HelpFlowNode
import com.example.model.Order
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.viewmodel.DiscoveryUiState
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun HelpSupportScreen(
    initialTopicSlug: String = "",
    uiState: MelvoreUiState,
    discoveryState: DiscoveryUiState,
    onLoadHelp: () -> Unit,
    onOpenTopic: (topicId: Int?, slug: String?) -> Unit,
    onClearFlow: () -> Unit,
    onEvaluateOrder: (topicId: Int, flowId: Int, orderId: Int, orderItemId: Int, note: String) -> Unit,
    onUploadEvidencePhoto: (Uri) -> Unit,
    onEscalateTicket: (
        topicId: Int,
        flowId: Int,
        orderId: Int,
        orderItemId: Int,
        subject: String,
        category: String,
        note: String,
        onDone: () -> Unit
    ) -> Unit,
    onNavigateTickets: () -> Unit,
    onNavigateAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    var faqQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onLoadHelp()
        if (initialTopicSlug.isNotBlank()) {
            onOpenTopic(null, initialTopicSlug)
        }
    }

    val activeDetail = discoveryState.activeHelpFlow
    if (activeDetail != null) {
        GuidedHelpFlowSolver(
            detail = activeDetail,
            orders = uiState.orders,
            isSignedIn = uiState.user != null,
            discoveryState = discoveryState,
            onBackToTopics = onClearFlow,
            onEvaluateOrder = onEvaluateOrder,
            onUploadEvidencePhoto = onUploadEvidencePhoto,
            onEscalateTicket = { tid, fid, oid, oiid, sub, cat, note ->
                onEscalateTicket(tid, fid, oid, oiid, sub, cat, note, onNavigateTickets)
            },
            onNavigateAuth = onNavigateAuth,
            onNavigateTickets = onNavigateTickets,
            modifier = modifier
        )
        return
    }

    val allFaqs = discoveryState.helpFaqs.ifEmpty { uiState.faqs }
    val filteredFaqs = remember(allFaqs, faqQuery) {
        val q = faqQuery.trim().lowercase()
        if (q.isBlank()) allFaqs else allFaqs.filter {
            it.question.lowercase().contains(q) || it.answer.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("help_support_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("MELVORE CONCIERGE", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.6.sp, color = ext.goldAccent)
                Text("Guided Help & Order Resolution", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Instant automated checks against live order & shipping records, backed by dedicated human specialists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
            }
        }

        if (discoveryState.isHelpLoading || discoveryState.isHelpFlowLoading) {
            item { MelvoreLoadingState("Loading guided support topics…") }
        }

        // My Tickets Quick Access Card
        item {
            Surface(
                onClick = {
                    if (uiState.user == null) onNavigateAuth() else onNavigateTickets()
                },
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.HeadsetMic, contentDescription = null, tint = ext.goldAccent)
                        Column {
                            Text("My Support Tickets (${uiState.tickets.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (uiState.user == null) "Sign in to view or reply to existing tickets"
                                else "View replies and active resolution status",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.muted
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Guided Topic Grid
        if (discoveryState.helpTopics.isNotEmpty()) {
            item {
                Text("SELF-SERVICE TOPICS", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, color = ext.muted)
            }
            items(discoveryState.helpTopics.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { topic ->
                        Surface(
                            onClick = { onOpenTopic(topic.topicId, topic.slug) },
                            shape = RoundedCornerShape(2.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ext.borderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 84.dp)
                                .testTag("help_topic_${topic.slug}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(topic.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 2)
                                if (topic.subtitle.isNotBlank()) {
                                    Text(
                                        topic.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.muted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // Searchable FAQs
        item {
            HorizontalDivider(color = ext.borderSubtle)
            Spacer(Modifier.height(10.dp))
            Text("FREQUENTLY ASKED QUESTIONS", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, color = ext.muted)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = faqQuery,
                onValueChange = { faqQuery = it },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Search delivery, returns, COD, sizing…") },
                singleLine = true,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(filteredFaqs, key = { "help_faq_${it.id}_${it.question.hashCode()}" }) { faq ->
            var expanded by remember { mutableStateOf(false) }
            Surface(
                onClick = { expanded = !expanded },
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = ext.goldAccent, modifier = Modifier.size(16.dp))
                        Text(faq.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (expanded) {
                        Text(faq.answer, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedHelpFlowSolver(
    detail: com.example.model.HelpFlowDetail,
    orders: List<Order>,
    isSignedIn: Boolean,
    discoveryState: DiscoveryUiState,
    onBackToTopics: () -> Unit,
    onEvaluateOrder: (Int, Int, Int, Int, String) -> Unit,
    onUploadEvidencePhoto: (Uri) -> Unit,
    onEscalateTicket: (Int, Int, Int, Int, String, String, String) -> Unit,
    onNavigateAuth: () -> Unit,
    onNavigateTickets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val flow = detail.flow
    val nodes = flow?.nodes.orEmpty()

    var currentNodeId by remember(flow?.flowId) {
        mutableStateOf(flow?.startNodeId ?: nodes.firstOrNull()?.nodeId)
    }
    var selectedOrder by remember(detail.topic.topicId, orders) { mutableStateOf(orders.firstOrNull()) }
    var noteInput by remember { mutableStateOf("") }

    val activeNode: HelpFlowNode? = remember(nodes, currentNodeId) {
        nodes.find { it.nodeId == currentNodeId } ?: nodes.firstOrNull()
    }

    // Zero-permission Photo Picker for damage/wrong-item evidence
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUploadEvidencePhoto(uri)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("guided_help_flow"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(detail.topic.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = ext.goldAccent)
                    Text(detail.topic.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onBackToTopics,
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("All Topics")
                }
            }
        }

        if (discoveryState.isHelpSessionBusy) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        // Order Selection when required by topic or node
        if (detail.topic.requiresOrder || activeNode?.requiresOrderSelect == true) {
            item {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("1. Select Your Order", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        if (!isSignedIn) {
                            Text("Sign in to inspect your real order status, tracking & return eligibility.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                            Button(
                                onClick = onNavigateAuth,
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Text("Sign In")
                            }
                        } else if (orders.isEmpty()) {
                            Text("No orders found in your account yet.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(orders.take(10), key = { "help_ord_${it.id}" }) { ord ->
                                    val picked = selectedOrder?.id == ord.id
                                    Surface(
                                        onClick = { selectedOrder = ord },
                                        shape = RoundedCornerShape(2.dp),
                                        color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, ext.borderSubtle),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Inventory2,
                                                contentDescription = null,
                                                tint = if (picked) MaterialTheme.colorScheme.onPrimary else ext.muted,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                "#${ord.orderNumber} · ${ord.status}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (picked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            if (selectedOrder != null && flow != null) {
                                Button(
                                    onClick = {
                                        onEvaluateOrder(detail.topic.topicId, flow.flowId, selectedOrder!!.id, 0, noteInput)
                                    },
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("help_check_order_btn")
                                ) {
                                    Text("Run Automated Order Check")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Backend Order Report (returned by session.php)
        val report = discoveryState.activeHelpSession?.orderReport
        if (report != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.goldAccent)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("LIVE ORDER CHECK · #${report.orderNumber}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ext.goldAccent)
                        Text(
                            "Status: ${report.orderStatus.uppercase()} · Payment: ${report.paymentMethod.uppercase()} (${report.paymentStatus})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (report.statusExplanation.isNotBlank()) {
                            Text(report.statusExplanation, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                        }
                        if (!report.trackingNumber.isNullOrBlank()) {
                            Text(
                                "Courier: ${report.courierName ?: "Partner"} · Tracking: ${report.trackingNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.emerald
                            )
                        }
                        if (report.policySummary.isNotBlank()) {
                            Text(report.policySummary, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                        }
                    }
                }
            }
        }

        // Current Decision Tree Node
        if (activeNode != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(activeNode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (activeNode.bodyText.isNotBlank()) {
                            Text(activeNode.bodyText, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
                        }
                        if (activeNode.policyText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(activeNode.policyText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                            }
                        }
                        activeNode.options.forEach { opt ->
                            OutlinedButton(
                                onClick = {
                                    if (opt.nextNodeId != null) {
                                        currentNodeId = opt.nextNodeId
                                    } else if (opt.actionKey == "check_order" && selectedOrder != null && flow != null) {
                                        onEvaluateOrder(detail.topic.topicId, flow.flowId, selectedOrder!!.id, 0, noteInput)
                                    } else if (opt.escalateToTicket && flow != null) {
                                        onEscalateTicket(
                                            detail.topic.topicId,
                                            flow.flowId,
                                            selectedOrder?.id ?: 0,
                                            0,
                                            "${detail.topic.title}: ${opt.label}",
                                            detail.topic.category,
                                            noteInput.ifBlank { opt.label }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Text(opt.label)
                            }
                        }
                    }
                }
            }
        }

        // Evidence Photo Picker + Escalation to Support Ticket
        item {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Need Further Assistance?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Describe the issue or item details") },
                        minLines = 2,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("help_note_input")
                    )

                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !discoveryState.isSupportPhotoUploading && discoveryState.supportPhotoUrls.size < 4,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (discoveryState.isSupportPhotoUploading) "Uploading photo…"
                            else "Attach Evidence Photo (${discoveryState.supportPhotoUrls.size}/4)"
                        )
                    }

                    if (discoveryState.supportPhotoUrls.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(discoveryState.supportPhotoUrls) { url ->
                                MelvoreNetworkImage(
                                    url = url,
                                    contentDescription = "Evidence photo",
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }
                    }

                    val createdTicket = discoveryState.activeHelpSession?.ticket
                    if (createdTicket != null) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = ext.emerald.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, ext.emerald.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Ticket ${createdTicket.ticketNumber} Opened", style = MaterialTheme.typography.titleMedium, color = ext.emerald, fontWeight = FontWeight.Bold)
                                Text(createdTicket.note, style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(
                                    onClick = onNavigateTickets,
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.padding(top = 4.dp).heightIn(min = 48.dp)
                                ) {
                                    Text("View in Support Tickets")
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!isSignedIn) {
                                    onNavigateAuth()
                                } else {
                                    onEscalateTicket(
                                        detail.topic.topicId,
                                        flow?.flowId ?: 0,
                                        selectedOrder?.id ?: 0,
                                        0,
                                        detail.topic.title,
                                        detail.topic.category,
                                        noteInput.ifBlank { "Requested specialist support via guided help." }
                                    )
                                }
                            },
                            enabled = !discoveryState.isHelpSessionBusy,
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("help_escalate_ticket_btn")
                        ) {
                            Text(if (isSignedIn) "Open Support Ticket With This Summary" else "Sign In to Open Ticket")
                        }
                    }
                }
            }
        }
    }
}
