package com.example.ui.effects

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Privacy-respecting procedural ambient soundscape & UI SFX synthesizer using Android AudioTrack.
 * - Strictly OFF by default until explicit user opt-in consent.
 * - Separates Ambient Soundscape toggle from subtle UI SFX toggle.
 * - Honors AudioManager focus, stops cleanly on ON_STOP, mute, or exiting a Fashion World.
 */
class AmbientEffectsController(
    context: Context
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var ambientJob: Job? = null
    private var currentTrackKey: String = "none"
    private var hasAudioFocus: Boolean = false
    private var focusRequest: AudioFocusRequest? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
        ) {
            stopAmbient()
        }
    }

    fun syncAmbient(
        trackKey: String,
        ambientAudioEnabled: Boolean,
        consentAnswered: Boolean,
        isWorldOrOccasionActive: Boolean,
        isAppForeground: Boolean
    ) {
        val cleanKey = trackKey.trim().lowercase()
        val shouldPlay = consentAnswered &&
            ambientAudioEnabled &&
            isAppForeground &&
            isWorldOrOccasionActive &&
            cleanKey.isNotBlank() &&
            cleanKey != "none"

        if (!shouldPlay) {
            stopAmbient()
            return
        }

        if (currentTrackKey == cleanKey && ambientJob?.isActive == true) {
            return
        }

        stopAmbient()
        if (!requestAudioFocus()) return

        currentTrackKey = cleanKey
        ambientJob = scope.launch {
            streamProceduralPad(cleanKey)
        }
    }

    fun playUiSfx(
        sfxProfile: String,
        sfxType: String,
        uiSfxEnabled: Boolean,
        consentAnswered: Boolean
    ) {
        if (!consentAnswered || !uiSfxEnabled) return
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val durationMs = when (sfxType) {
                    "offer" -> 110
                    "enter_world" -> 140
                    else -> 65
                }
                val numSamples = (sampleRate * durationMs) / 1000
                val pcm = ShortArray(numSamples)
                val baseFreq = when (sfxProfile.lowercase()) {
                    "cyber", "y2k" -> if (sfxType == "offer") 880.0 else 660.0
                    "velvet", "goth" -> if (sfxType == "offer") 330.0 else 246.94
                    "chime", "coquette" -> if (sfxType == "offer") 1046.5 else 783.99
                    "bass", "street" -> if (sfxType == "offer") 440.0 else 293.66
                    else -> if (sfxType == "offer") 659.25 else 523.25
                }
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 28.0) * (1.0 - exp(-t * 400.0))
                    val harmonic = sin(2.0 * PI * baseFreq * t) + 0.35 * sin(2.0 * PI * (baseFreq * 1.5) * t)
                    pcm[i] = (harmonic * env * 2200.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val format = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val bufBytes = maxOf(minBuf, pcm.size * 2)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                try {
                    track.write(pcm, 0, pcm.size)
                    track.play()
                    kotlinx.coroutines.delay(durationMs.toLong() + 30L)
                } finally {
                    runCatching { track.stop() }
                    runCatching { track.release() }
                }
            }
        }
    }

    fun stopAmbient() {
        currentTrackKey = "none"
        ambientJob?.cancel()
        ambientJob = null
        abandonAudioFocus()
    }

    fun release() {
        stopAmbient()
        scope.cancel()
    }

    private fun requestAudioFocus(): Boolean {
        val mgr = audioManager ?: return false
        if (hasAudioFocus) return true
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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

    private suspend fun CoroutineScope.streamProceduralPad(trackKey: String) {
        val sampleRate = 22050
        val freqs = frequenciesForTrack(trackKey)
        val bufferFrames = 2048
        val pcm = ShortArray(bufferFrames)
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(bufferFrames * 4)

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        var sampleIndex = 0L
        try {
            track.play()
            while (isActive) {
                for (i in 0 until bufferFrames) {
                    val t = (sampleIndex + i).toDouble() / sampleRate
                    val fadeIn = (t / 1.2).coerceIn(0.0, 1.0)
                    val lfo = 0.72 + 0.28 * sin(2.0 * PI * 0.18 * t)
                    var mix = 0.0
                    for (idx in freqs.indices) {
                        mix += sin(2.0 * PI * freqs[idx] * t) / freqs.size
                    }
                    val sample = (mix * lfo * fadeIn * 2100.0)
                        .toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    pcm[i] = sample.toShort()
                }
                sampleIndex += bufferFrames
                val written = track.write(pcm, 0, bufferFrames, AudioTrack.WRITE_BLOCKING)
                if (written < 0) break
            }
        } finally {
            runCatching { track.pause() }
            runCatching { track.flush() }
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    private fun frequenciesForTrack(trackKey: String): DoubleArray = when {
        trackKey.contains("y2k") -> doubleArrayOf(261.63, 329.63, 392.00, 493.88) // Cmaj7 dreamy shimmer
        trackKey.contains("goth") -> doubleArrayOf(130.81, 155.56, 196.00, 233.08) // Cm7 cathedral noir
        trackKey.contains("seoul") || trackKey.contains("k_fashion") -> doubleArrayOf(220.00, 261.63, 329.63, 392.00) // Am7 lofi pad
        trackKey.contains("coquette") -> doubleArrayOf(349.23, 440.00, 523.25, 659.25) // Fmaj7 music box
        trackKey.contains("street") -> doubleArrayOf(146.83, 174.61, 220.00, 261.63) // Dm7 warm sub pad
        trackKey.contains("diwali") || trackKey.contains("festive") -> doubleArrayOf(196.00, 293.66, 392.00, 493.88) // Festive open 5th drone
        else -> doubleArrayOf(220.00, 277.18, 329.63)
    }
}
