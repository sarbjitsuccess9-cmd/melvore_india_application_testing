package com.example.ui.effects

import android.content.Context
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pure decision helper so ambient playback gating is deterministic & unit-testable.
 */
fun shouldPlayWorldAmbientAudio(
    consentAnswered: Boolean,
    ambientAudioEnabled: Boolean,
    isWorldOrOccasionActive: Boolean,
    isAppForeground: Boolean,
    isVideoPlayingAudio: Boolean,
    audioUrl: String?,
    audioTrackKey: String
): Boolean {
    if (!consentAnswered || !ambientAudioEnabled || !isAppForeground || !isWorldOrOccasionActive || isVideoPlayingAudio) {
        return false
    }
    val hasRemote = !audioUrl.isNullOrBlank() &&
        (audioUrl.startsWith("https://", ignoreCase = true) || audioUrl.startsWith("http://", ignoreCase = true))
    val cleanKey = audioTrackKey.trim().lowercase()
    return hasRemote || (cleanKey.isNotBlank() && cleanKey != "none")
}

/**
 * Coordinates world-specific & occasion-specific background audio via Media3 ExoPlayer,
 * with procedural AudioTrack synthesis fallback when a remote audio_url isn't configured or fails.
 *
 * Guarantees:
 * - No automatic audio before explicit consent.
 * - Global mute persisted and honored when entering subsequent worlds.
 * - Pauses when app goes to background (`isAppForeground == false`).
 * - Pauses/ducks when unmuted product/promo/reel video plays (`isVideoPlayingAudio == true`).
 * - Smooth fade-in and gapless looping (`Player.REPEAT_MODE_ONE`).
 */
class WorldAudioManager(
    context: Context
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val proceduralController = AmbientEffectsController(appContext)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var exoPlayer: ExoPlayer? = null
    private var fadeJob: Job? = null
    private var currentUrl: String? = null
    private var currentTrackKey: String = "none"
    private var targetVolume: Float = 0.45f
    private var lastSyncedLoop: Boolean = true
    private var remoteFailedUrls = mutableSetOf<String>()

    private var hasAudioFocus: Boolean = false
    private var focusRequest: AudioFocusRequest? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseAllAmbient()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                exoPlayer?.volume = (targetVolume * 0.25f).coerceIn(0.02f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (currentUrl != null) {
                    fadeVolumeTo(targetVolume)
                }
            }
        }
    }

    fun sync(
        audioUrl: String?,
        trackKey: String,
        loop: Boolean,
        volume: Float,
        ambientAudioEnabled: Boolean,
        consentAnswered: Boolean,
        isWorldOrOccasionActive: Boolean,
        isAppForeground: Boolean,
        isVideoPlayingAudio: Boolean
    ) {
        val cleanUrl = audioUrl?.trim()?.takeIf {
            it.isNotBlank() &&
                (it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)) &&
                !remoteFailedUrls.contains(it)
        }
        val cleanKey = trackKey.trim().ifBlank { "none" }
        val clampedVol = volume.coerceIn(0.05f, 1.0f)

        val allowed = shouldPlayWorldAmbientAudio(
            consentAnswered = consentAnswered,
            ambientAudioEnabled = ambientAudioEnabled,
            isWorldOrOccasionActive = isWorldOrOccasionActive,
            isAppForeground = isAppForeground,
            isVideoPlayingAudio = isVideoPlayingAudio,
            audioUrl = cleanUrl,
            audioTrackKey = cleanKey
        )

        if (!allowed) {
            pauseAllAmbient()
            return
        }

        if (cleanUrl != null) {
            proceduralController.stopAmbient()
            if (!requestAudioFocus()) return

            val player = ensurePlayer()
            val needsReload = currentUrl != cleanUrl || lastSyncedLoop != loop
            currentUrl = cleanUrl
            currentTrackKey = cleanKey
            targetVolume = clampedVol
            lastSyncedLoop = loop

            player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            if (needsReload) {
                player.volume = 0.02f
                player.setMediaItem(MediaItem.fromUri(cleanUrl))
                player.prepare()
                player.playWhenReady = true
                fadeVolumeTo(clampedVol)
            } else {
                if (!player.isPlaying) {
                    player.playWhenReady = true
                    player.play()
                }
                fadeVolumeTo(clampedVol)
            }
        } else {
            stopRemotePlayer()
            currentUrl = null
            currentTrackKey = cleanKey
            targetVolume = clampedVol
            proceduralController.syncAmbient(
                trackKey = cleanKey,
                ambientAudioEnabled = ambientAudioEnabled,
                consentAnswered = consentAnswered,
                isWorldOrOccasionActive = isWorldOrOccasionActive,
                isAppForeground = isAppForeground && !isVideoPlayingAudio
            )
        }
    }

    fun playUiSfx(
        sfxProfile: String,
        sfxType: String,
        uiSfxEnabled: Boolean,
        consentAnswered: Boolean
    ) {
        proceduralController.playUiSfx(
            sfxProfile = sfxProfile,
            sfxType = sfxType,
            uiSfxEnabled = uiSfxEnabled,
            consentAnswered = consentAnswered
        )
    }

    fun pauseAllAmbient() {
        fadeJob?.cancel()
        fadeJob = null
        exoPlayer?.let { p ->
            runCatching {
                if (p.isPlaying) p.pause()
                p.playWhenReady = false
            }
        }
        proceduralController.stopAmbient()
        abandonAudioFocus()
    }

    fun release() {
        fadeJob?.cancel()
        fadeJob = null
        runCatching { exoPlayer?.release() }
        exoPlayer = null
        currentUrl = null
        proceduralController.release()
        abandonAudioFocus()
        mainScope.cancel()
    }

    private fun stopRemotePlayer() {
        fadeJob?.cancel()
        fadeJob = null
        exoPlayer?.let { p ->
            runCatching {
                p.pause()
                p.stop()
                p.clearMediaItems()
            }
        }
    }

    private fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        val mediaAudioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val created = ExoPlayer.Builder(appContext)
            .build()
            .apply {
                setAudioAttributes(mediaAudioAttrs, false)
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        val failedUrl = currentUrl
                        if (!failedUrl.isNullOrBlank()) {
                            remoteFailedUrls.add(failedUrl)
                        }
                        currentUrl = null
                        stopRemotePlayer()
                        if (currentTrackKey.isNotBlank() && currentTrackKey != "none") {
                            proceduralController.syncAmbient(
                                trackKey = currentTrackKey,
                                ambientAudioEnabled = true,
                                consentAnswered = true,
                                isWorldOrOccasionActive = true,
                                isAppForeground = true
                            )
                        }
                    }
                })
            }
        exoPlayer = created
        return created
    }

    private fun fadeVolumeTo(desiredVolume: Float) {
        fadeJob?.cancel()
        val player = exoPlayer ?: return
        fadeJob = mainScope.launch {
            val startVol = player.volume.coerceIn(0f, 1f)
            val endVol = desiredVolume.coerceIn(0.05f, 1f)
            val steps = 8
            for (step in 1..steps) {
                if (!isActive) break
                val t = step.toFloat() / steps.toFloat()
                player.volume = startVol + (endVol - startVol) * t
                delay(45L)
            }
            if (isActive) {
                player.volume = endVol
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val mgr = audioManager ?: return false
        if (hasAudioFocus) return true
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    PlatformAudioAttributes.Builder()
                        .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
                        .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            focusRequest = req
            mgr.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            mgr.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        hasAudioFocus = granted
        return granted
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        val mgr = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { runCatching { mgr.abandonAudioFocusRequest(it) } }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            runCatching { mgr.abandonAudioFocus(focusListener) }
        }
        hasAudioFocus = false
    }
}
