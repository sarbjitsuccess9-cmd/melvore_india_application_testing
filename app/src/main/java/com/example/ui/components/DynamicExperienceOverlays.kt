package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LocalPreferences
import com.example.model.SmartOffer
import com.example.model.formatPrice
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.ResolvedFashionWorldTheme
import com.example.ui.theme.contrastOnColor
import com.example.ui.theme.parseHexColor

@Composable
fun WorldAudioConsentDialog(
    worldName: String,
    onAllow: () -> Unit,
    onKeepMuted: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onKeepMuted,
        icon = {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = cs.secondary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.world_audio_prompt_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.world_audio_prompt_body,
                    worldName.ifBlank { "Melvore Style World" }
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onAllow,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("world_audio_consent_allow")
            ) {
                Text(stringResource(R.string.world_audio_prompt_allow))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onKeepMuted,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("world_audio_consent_mute")
            ) {
                Text(stringResource(R.string.world_audio_prompt_mute))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorySettingsBottomSheet(
    preferences: LocalPreferences,
    onToggleAmbientAudio: (Boolean) -> Unit,
    onToggleUiSfx: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onToggleVideoAutoplayMuted: (Boolean) -> Unit = {},
    onToggleDataSaverMode: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = com.example.ui.theme.LocalMelvoreSurfaces.current
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        dragHandle = null
    ) {
        LiquidGlassSheetContainer(
            modifier = Modifier.fillMaxWidth()
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("sensory_settings_sheet")
        ) {
            Text(
                text = stringResource(R.string.sensory_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = cs.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.sensory_settings_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted
            )
            Spacer(modifier = Modifier.height(16.dp))

            SensoryToggleRow(
                title = stringResource(R.string.sensory_ambient_audio_label),
                subtitle = stringResource(R.string.sensory_ambient_audio_desc),
                checked = preferences.ambientAudioEnabled,
                onCheckedChange = onToggleAmbientAudio,
                tag = "sensory_toggle_ambient"
            )
            HorizontalDivider(color = ext.borderSubtle)
            SensoryToggleRow(
                title = stringResource(R.string.sensory_ui_sfx_label),
                subtitle = stringResource(R.string.sensory_ui_sfx_desc),
                checked = preferences.uiSfxEnabled,
                onCheckedChange = onToggleUiSfx,
                tag = "sensory_toggle_sfx"
            )
            HorizontalDivider(color = ext.borderSubtle)
            SensoryToggleRow(
                title = stringResource(R.string.sensory_reduced_motion_label),
                subtitle = stringResource(R.string.sensory_reduced_motion_desc),
                checked = preferences.reducedMotion,
                onCheckedChange = onToggleReducedMotion,
                tag = "sensory_toggle_reduced_motion"
            )
            HorizontalDivider(color = ext.borderSubtle)
            SensoryToggleRow(
                title = stringResource(R.string.sensory_video_autoplay_label),
                subtitle = stringResource(R.string.sensory_video_autoplay_desc),
                checked = preferences.videoAutoplayMuted,
                onCheckedChange = onToggleVideoAutoplayMuted,
                tag = "sensory_toggle_video_autoplay_muted"
            )
            HorizontalDivider(color = ext.borderSubtle)
            SensoryToggleRow(
                title = stringResource(R.string.sensory_data_saver_label),
                subtitle = stringResource(R.string.sensory_data_saver_desc),
                checked = preferences.dataSaverMode,
                onCheckedChange = onToggleDataSaverMode,
                tag = "sensory_toggle_data_saver"
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        }
    }
}

@Composable
private fun SensoryToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LocalMelvoreColors.current.muted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
fun WorldContextQuickBar(
    resolvedTheme: ResolvedFashionWorldTheme,
    ambientAudioEnabled: Boolean,
    onExitWorld: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenSensorySheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!resolvedTheme.isWorldActive) return

    val accent = parseHexColor(resolvedTheme.accentHex, MaterialTheme.colorScheme.secondary)
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("world_context_quick_bar"),
        shape = RoundedCornerShape(999.dp),
        color = cs.surfaceVariant.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent
                ) {
                    Text(
                        text = resolvedTheme.worldName.ifBlank { resolvedTheme.worldSlug.orEmpty() }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contrastOnColor(accent),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.world_active_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("world_audio_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (ambientAudioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = stringResource(R.string.cd_world_audio_toggle),
                        tint = cs.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onOpenSensorySheet,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("world_sensory_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.cd_sensory_settings),
                        tint = cs.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                TextButton(
                    onClick = onExitWorld,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("world_exit_chip")
                ) {
                    Text(
                        text = stringResource(R.string.world_exit_button),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
fun SmartContextualOfferBanner(
    offer: SmartOffer?,
    cartSubtotal: Double,
    currency: String,
    onApplyOffer: (SmartOffer) -> Unit,
    onDismissOffer: (SmartOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = offer != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        if (offer == null) return@AnimatedVisibility
        val accent = parseHexColor(offer.accentHex, MaterialTheme.colorScheme.secondary)
        val cs = MaterialTheme.colorScheme
        val ext = LocalMelvoreColors.current

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("smart_offer_banner"),
            shape = RoundedCornerShape(16.dp),
            color = cs.surface,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (offer.badgeText.isNotBlank()) {
                        Text(
                            text = offer.badgeText.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = offer.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitleText = if (offer.minCartValue > 0.0 && cartSubtotal < offer.minCartValue) {
                        val remaining = (offer.minCartValue - cartSubtotal).coerceAtLeast(0.0)
                        "${offer.subtitle} · Add ${formatPrice(remaining, currency)} more"
                    } else {
                        "${offer.subtitle} · Code: ${offer.couponCode}"
                    }
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (offer.couponCode.isNotBlank()) {
                    Button(
                        onClick = { onApplyOffer(offer) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = contrastOnColor(accent)
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("smart_offer_apply_button")
                    ) {
                        Text(
                            text = offer.ctaText.ifBlank { stringResource(R.string.smart_offer_apply) },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = { onDismissOffer(offer) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("smart_offer_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_dismiss_offer),
                        tint = ext.muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
