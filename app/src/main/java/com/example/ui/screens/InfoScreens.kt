package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.example.R
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun AboutScreen(
    uiState: MelvoreUiState,
    onNavigateShop: () -> Unit,
    onNavigateContact: () -> Unit,
    onOpenConnectionDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val design = uiState.design

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                MelvoreNetworkImage(
                    url = null,
                    contentDescription = "Melvore Atelier",
                    fallbackDrawable = R.drawable.img_hero_atelier,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    surfaces.imageOverlay.copy(alpha = 0.22f),
                                    surfaces.imageOverlay.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Text(
                        "OUR STUDIO",
                        style = MaterialTheme.typography.labelMedium,
                        color = surfaces.onImageOverlay.copy(alpha = 0.78f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tailored outerwear meets zero-gravity comfort.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = surfaces.onImageOverlay
                    )
                }
            }
        }

        if (uiState.aboutSections.isNotEmpty()) {
            items(uiState.aboutSections, key = { it.id }) { section ->
                val cleanBody = remember(section.content) {
                    HtmlCompat.fromHtml(section.content, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        .toString()
                        .trim()
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (section.title.isNotBlank()) {
                            Text(section.title, style = MaterialTheme.typography.titleLarge)
                        }
                        if (cleanBody.isNotBlank()) {
                            Text(cleanBody, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
                        }
                    }
                }
            }
        }

        if (design.milestones.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Milestones", style = MaterialTheme.typography.titleLarge)
                    design.milestones.forEach { m ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ext.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        m.year,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.title, style = MaterialTheme.typography.titleMedium)
                                    Text(m.description, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (design.teamMembers.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Atelier Team", style = MaterialTheme.typography.titleLarge)
                    design.teamMembers.forEach { member ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ext.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!member.imageUrl.isNullOrBlank()) {
                                    MelvoreNetworkImage(
                                        url = member.imageUrl,
                                        contentDescription = member.name,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(member.name, style = MaterialTheme.typography.titleMedium)
                                    Text(member.position, style = MaterialTheme.typography.labelMedium, color = ext.accentNavy)
                                    Text(member.bio, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onNavigateContact,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Contact Studio")
                    }
                    Button(
                        onClick = onNavigateShop,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Explore Catalog")
                    }
                }
                OutlinedButton(
                    onClick = onOpenConnectionDiagnostics,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("about_connection_diagnostics_btn")
                ) {
                    Text("Connection Diagnostics")
                }
            }
        }
    }
}

@Composable
fun BlogScreen(
    uiState: MelvoreUiState,
    onOpenPost: (Int) -> Unit,
    onClearPost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val post = uiState.selectedBlogPost

    BackHandler(enabled = post != null) {
        onClearPost()
    }

    if (post != null) {
        val plainArticle = remember(post.id, post.contentHtml, post.excerpt) {
            val raw = post.contentHtml.ifBlank { post.excerpt }
            HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
        }
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedButton(
                    onClick = onClearPost,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("blog_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Back to Journal")
                }
            }
            if (uiState.isBlogLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            item {
                MelvoreNetworkImage(
                    url = post.imageUrl,
                    contentDescription = post.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }
            item {
                Text(post.category.uppercase(), style = MaterialTheme.typography.labelMedium, color = ext.accentNavy)
                Spacer(Modifier.height(4.dp))
                Text(post.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        if (post.publishedAt.isNotBlank()) append(post.publishedAt.take(10))
                        if (post.viewCount > 0) append(" · ${post.viewCount} views")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
            }
            if (uiState.blogError != null && post.contentHtml.isBlank()) {
                item {
                    MelvoreErrorState(
                        message = uiState.blogError,
                        onRetry = { onOpenPost(post.id) }
                    )
                }
            }
            item {
                Text(
                    plainArticle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Melvore Journal", style = MaterialTheme.typography.headlineMedium)
                Text("Lookbooks, garment care notes, and atelier essays.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
            }
        }
        if (uiState.blogs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No journal articles published yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.muted,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(uiState.blogs, key = { it.id }) { article ->
                Surface(
                    onClick = { onOpenPost(article.id) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle),
                    modifier = Modifier.testTag("blog_card_${article.id}")
                ) {
                    Column {
                        MelvoreNetworkImage(
                            url = article.imageUrl,
                            contentDescription = article.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                        )
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${article.category} · ${article.publishedAt.take(10)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.accentNavy
                            )
                            Text(article.title, style = MaterialTheme.typography.titleLarge)
                            Text(article.excerpt, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("Read Article", style = MaterialTheme.typography.labelLarge)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FaqScreen(
    uiState: MelvoreUiState,
    onNavigateContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    var expandedId by remember(uiState.faqs) { mutableStateOf(uiState.faqs.firstOrNull()?.id) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Frequently Asked Questions", style = MaterialTheme.typography.headlineMedium)
                Text("Sizing, dispatch timelines, care & returns.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
            }
        }
        items(uiState.faqs, key = { it.id }) { faq ->
            val expanded = expandedId == faq.id
            Surface(
                onClick = { expandedId = if (expanded) null else faq.id },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle),
                modifier = Modifier.testTag("faq_item_${faq.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(faq.category, style = MaterialTheme.typography.labelSmall, color = ext.accentNavy)
                            Text(faq.question, style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                    if (expanded) {
                        HorizontalDivider(color = ext.borderSubtle)
                        Text(faq.answer, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
                    }
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Still have questions?", style = MaterialTheme.typography.titleMedium)
                        Text("Reach our concierge directly.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                    }
                    Button(onClick = onNavigateContact, shape = RoundedCornerShape(12.dp)) {
                        Text("Contact Us")
                    }
                }
            }
        }
    }
}

@Composable
fun ContactScreen(
    uiState: MelvoreUiState,
    onSubmitContact: (
        name: String,
        email: String,
        phone: String,
        subject: String,
        message: String,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val settings = uiState.design.siteSettings

    var name by remember(uiState.user?.name) { mutableStateOf(uiState.user?.name ?: "") }
    var email by remember(uiState.user?.email) { mutableStateOf(uiState.user?.email ?: "") }
    var phone by remember(uiState.user?.phone) { mutableStateOf(uiState.user?.phone ?: "") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Contact Melvore", style = MaterialTheme.typography.headlineMedium)
                Text("Concierge support for sizing, orders, and custom inquiries.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = ext.accentNavy)
                        Column {
                            Text("Email", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                            Text(settings.contactEmail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, tint = ext.accentNavy)
                        Column {
                            Text("Concierge Phone", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                            Text(settings.contactPhone, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = ext.accentNavy)
                        Column {
                            Text("Studio", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                            Text(settings.siteAddress, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            if (submitted) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ext.emerald.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ext.emerald.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ext.emerald, modifier = Modifier.size(36.dp))
                        Text("Message Received", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Our concierge team will respond within 24 business hours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.muted
                        )
                        OutlinedButton(onClick = { submitted = false }, shape = RoundedCornerShape(12.dp)) {
                            Text("Send Another Message")
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Send a Message", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        Button(
                            enabled = !isSending,
                            onClick = {
                                isSending = true
                                onSubmitContact(name, email, phone, subject, message) { ok ->
                                    isSending = false
                                    if (ok) {
                                        submitted = true
                                        subject = ""
                                        message = ""
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("contact_submit_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text("Submit Inquiry")
                            }
                        }
                    }
                }
            }
        }
    }
}
