package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

class SoundEffects(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var soundEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                stopJungleAdventureMusic()
            }
        }

    var hapticsEnabled: Boolean = true

    private var musicJob: Job? = null
    private val isMusicPlaying = AtomicBoolean(false)

    /**
     * Subtle jungle ambience: birds singing, rustling leaves, distant wildlife and flowing water stream.
     */
    fun playJungleAmbience() {
        if (!soundEnabled) return
        scope.launch {
            try {
                val sampleRate = 22050
                val durationSec = 2.4f
                val numSamples = (durationSec * sampleRate).toInt()
                val buffer = ShortArray(numSamples)
                var noiseFilter = 0f

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate

                    // Distant flowing water / rustling leaves (filtered brown noise)
                    val white = (Math.random() * 2.0 - 1.0).toFloat()
                    noiseFilter = (noiseFilter * 0.96f) + (white * 0.04f)
                    val waterStream = noiseFilter * 0.18f

                    // Gentle tropical birdsong: 3 distinct melodic bird chirps
                    var birds = 0f
                    // Bird 1: chirp at 0.4s to 0.7s
                    if (t in 0.35f..0.65f) {
                        val bt = t - 0.35f
                        val freq = 2600f + sin(bt * 40f) * 600f
                        val env = sin(bt / 0.30f * Math.PI.toFloat())
                        birds += sin(2.0 * Math.PI * freq * t).toFloat() * env * 0.22f
                    }
                    // Bird 2: dual twitter at 1.1s to 1.5s
                    if (t in 1.10f..1.45f) {
                        val bt = t - 1.10f
                        val freq = 3100f + cos(bt * 55f) * 700f
                        val env = sin(bt / 0.35f * Math.PI.toFloat())
                        birds += sin(2.0 * Math.PI * freq * t).toFloat() * env * 0.20f
                    }
                    // Bird 3: distant sweet warble at 1.7s to 2.1s
                    if (t in 1.70f..2.10f) {
                        val bt = t - 1.70f
                        val freq = 2400f + sin(bt * 30f) * 450f
                        val env = sin(bt / 0.40f * Math.PI.toFloat())
                        birds += sin(2.0 * Math.PI * freq * t).toFloat() * env * 0.18f
                    }

                    val totalSample = (waterStream + birds) * 0.75f
                    buffer[i] = (totalSample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }

                playPcmBuffer(buffer, sampleRate)
            } catch (_: Exception) {}
        }
    }

    /**
     * Original African-inspired jungle adventure background music:
     * - Hand drums (resonant djembe bass & sharp slap accents)
     * - Wooden percussion (clackers / woodblocks)
     * - Marimba / Balafon melody in D minor pentatonic
     * - Breathy bamboo-flute melodies
     * - Ambient river stream & birdsong
     */
    fun startJungleAdventureMusic() {
        if (!soundEnabled || isMusicPlaying.get()) return
        isMusicPlaying.set(true)

        musicJob = scope.launch {
            try {
                val sampleRate = 22050
                val loopDurationSec = 3.6f
                val numSamples = (loopDurationSec * sampleRate).toInt()
                val musicBuffer = ShortArray(numSamples)

                val bpm = 124f
                val beatDuration = 60f / bpm // ~0.484s per beat

                // Precompute 1 loop
                var waterNoise = 0f
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    var mix = 0f

                    // 1. Ambient River Water & Canopy Rustle
                    val white = (Math.random() * 2.0 - 1.0).toFloat()
                    waterNoise = (waterNoise * 0.95f) + (white * 0.05f)
                    mix += waterNoise * 0.12f

                    // 2. African Hand Drums (Djembe body & slap)
                    // Bass hit on beats 0, 2, 4, 6... (every 2 beats)
                    val beatIndex = (t / (beatDuration * 0.5f)).toInt()
                    val timeInSubBeat = t % (beatDuration * 0.5f)

                    // Djembe Bass (low resonant thump 75Hz)
                    if (beatIndex % 4 == 0 || beatIndex % 8 == 6) {
                        val decay = exp(-timeInSubBeat * 14.0).toFloat()
                        val bassTone = sin(2.0 * Math.PI * 72.0 * timeInSubBeat).toFloat()
                        mix += bassTone * decay * 0.40f
                    }

                    // Djembe Slap / Wooden Rim (sharp transient 650Hz + wood noise)
                    if (beatIndex % 4 == 2 || beatIndex % 8 == 5 || beatIndex % 8 == 7) {
                        val decay = exp(-timeInSubBeat * 32.0).toFloat()
                        val slapTone = (sin(2.0 * Math.PI * 580.0 * timeInSubBeat) + sin(2.0 * Math.PI * 920.0 * timeInSubBeat) * 0.5).toFloat()
                        mix += slapTone * decay * 0.28f
                    }

                    // 3. Marimba / Balafon Wooden Percussion Melody in D-minor Pentatonic
                    // Notes: D4 (293.7), F4 (349.2), G4 (392.0), A4 (440.0), C5 (523.3), D5 (587.3)
                    val marimbaNotes = floatArrayOf(293.7f, 349.2f, 440.0f, 392.0f, 523.3f, 440.0f, 587.3f, 440.0f)
                    val stepDuration = beatDuration * 0.5f
                    val stepIdx = ((t / stepDuration).toInt()) % marimbaNotes.size
                    val tInStep = t % stepDuration
                    val marimbaFreq = marimbaNotes[stepIdx]
                    val marimbaDecay = exp(-tInStep * 11.0).toFloat()
                    val marimbaHarmonic = (sin(2.0 * Math.PI * marimbaFreq * tInStep) +
                            sin(2.0 * Math.PI * marimbaFreq * 2.0 * tInStep) * 0.35 +
                            sin(2.0 * Math.PI * marimbaFreq * 3.0 * tInStep) * 0.15).toFloat()
                    mix += marimbaHarmonic * marimbaDecay * 0.24f

                    // 4. Bamboo Flute Melodic Call (warm, breathy flute with vibrato)
                    // Plays a lyrical phrase across the 3.6s loop
                    val fluteFreq = when {
                        t < 0.9f -> 587.3f // D5
                        t < 1.8f -> 659.3f // E5
                        t < 2.7f -> 880.0f // A5
                        else -> 784.0f     // G5
                    }
                    val vibrato = 1.0 + sin(t * 32.0) * 0.02
                    val fluteBreath = (Math.random() * 2.0 - 1.0).toFloat() * 0.04f
                    val fluteTone = (sin(2.0 * Math.PI * (fluteFreq * vibrato) * t).toFloat() + fluteBreath) * 0.18f
                    mix += fluteTone

                    // 5. Tropical Birds chirp accents
                    if ((t in 0.8f..1.1f) || (t in 2.4f..2.7f)) {
                        val chirpT = t % 0.3f
                        val bFreq = 2800f + sin(chirpT * 60f) * 500f
                        mix += sin(2.0 * Math.PI * bFreq * t).toFloat() * 0.10f
                    }

                    musicBuffer[i] = (mix.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }

                // Play looping AudioTrack
                while (isActive && isMusicPlaying.get() && soundEnabled) {
                    playPcmBuffer(musicBuffer, sampleRate)
                }
            } catch (_: Exception) {
            } finally {
                isMusicPlaying.set(false)
            }
        }
    }

    fun stopJungleAdventureMusic() {
        isMusicPlaying.set(false)
        musicJob?.cancel()
        musicJob = null
    }

    fun playEnterJungleSound() {
        vibrateMedium()
        if (!soundEnabled) return
        scope.launch {
            try {
                // Energetic drum roll crescendo into big resonant jungle thud!
                val sampleRate = 22050
                val durationSec = 0.55f
                val numSamples = (durationSec * sampleRate).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    val progress = i.toFloat() / numSamples
                    val rollFreq = 120f + progress * 240f
                    val roll = sin(2.0 * Math.PI * rollFreq * t) * (progress * 0.45f)
                    val bass = if (t > 0.35f) {
                        val bt = t - 0.35f
                        sin(2.0 * Math.PI * 65.0 * bt) * exp(-bt * 8.0) * 0.6f
                    } else 0.0
                    buffer[i] = ((roll + bass).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                }
                playPcmBuffer(buffer, sampleRate)
            } catch (_: Exception) {}
        }
    }

    private fun playPcmBuffer(buffer: ShortArray, sampleRate: Int) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep((buffer.size.toFloat() / sampleRate * 1000f).toLong())
            audioTrack.release()
        } catch (_: Exception) {}
    }

    fun playIntroChime() {
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(523.25f, 1318.51f, 0.40f)
        }
    }

    fun playTitleSting() {
        vibrateLight()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(261.63f, 784.00f, 0.35f)
        }
    }

    fun playCoin() {
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(987.77f, 1318.51f, 0.08f)
        }
    }

    fun playJump() {
        vibrateLight()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(300f, 650f, 0.12f)
        }
    }

    fun playSlide() {
        vibrateLight()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(450f, 220f, 0.14f)
        }
    }

    fun playLaneSwitch() {
        vibrateTick()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(500f, 580f, 0.05f)
        }
    }

    fun playPowerUp() {
        vibrateMedium()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(523.25f, 1046.50f, 0.22f)
        }
    }

    fun playShieldBreak() {
        vibrateMedium()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(800f, 200f, 0.25f)
        }
    }

    fun playCrash() {
        vibrateHeavy()
        if (!soundEnabled) return
        scope.launch {
            playNoiseThud(0.35f)
        }
    }

    fun playMysteryBoxCollect() {
        vibrateMedium()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(600f, 1200f, 0.16f)
        }
    }

    fun playMysteryBoxOpen() {
        vibrateHeavy()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(400f, 1600f, 0.32f)
        }
    }

    fun playSkyBoosterCollect() {
        vibrateMedium()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(700f, 1500f, 0.20f)
        }
    }

    fun playAnimalToken() {
        vibrateTick()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(880f, 1100f, 0.08f)
        }
    }

    fun playTransformation() {
        vibrateHeavy()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(250f, 950f, 0.40f)
        }
    }

    fun playMissionComplete() {
        vibrateMedium()
        if (!soundEnabled) return
        scope.launch {
            playToneSweep(523f, 1318f, 0.28f)
        }
    }

    private fun playToneSweep(startFreq: Float, endFreq: Float, durationSec: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (durationSec * sampleRate).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val progress = i.toFloat() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                val envelope = 1f - progress
                val sample = sin(2.0 * Math.PI * currentFreq * t) * envelope * 0.4
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep((durationSec * 1000).toLong() + 50)
            audioTrack.release()
        } catch (_: Exception) {}
    }

    private fun playNoiseThud(durationSec: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (durationSec * sampleRate).toInt()
            val buffer = ShortArray(numSamples)
            var last = 0f
            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val white = (Math.random() * 2 - 1).toFloat()
                last = (last + (0.05f * white)) / 1.05f
                val envelope = (1f - progress) * (1f - progress)
                buffer[i] = (last * envelope * 0.6f * Short.MAX_VALUE).toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep((durationSec * 1000).toLong() + 50)
            audioTrack.release()
        } catch (_: Exception) {}
    }

    private fun vibrateTick() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateLight() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateMedium() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateHeavy() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 120), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (_: Exception) {}
    }
}
