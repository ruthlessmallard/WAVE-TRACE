package com.ruthlessmallard.wavetrace

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * Short, LOUD percussive clicks that report a change in signal.
 *
 * Rising = signal got stronger (RSSI moved toward 0 dBm); falling = weaker.
 *
 * A pure short sine reads as a faint tick, so each click is a broadband
 * transient — a pitched sweep (carries the up/down cue) plus a short noise
 * burst (the audible "clack") — synthesised once in static mode and replayed
 * cheaply on demand. No bundled assets, no third-party dependencies.
 *
 * WAVE-TRACE is not a safety device; this is a convenience cue only.
 */
class ToneFeedback(private val volume: Float = 0.8f) {

    private val sampleRate = 44100
    private var risingTrack: AudioTrack? = null
    private var fallingTrack: AudioTrack? = null

    fun rising() {
        if (risingTrack == null) risingTrack = buildClick(from = 1000.0, to = 1650.0)
        play(risingTrack)
    }

    fun falling() {
        if (fallingTrack == null) fallingTrack = buildClick(from = 1650.0, to = 900.0)
        play(fallingTrack)
    }

    private fun play(track: AudioTrack?) {
        val t = track ?: return
        try {
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.reloadStaticData()
            t.play()
        } catch (_: IllegalStateException) {
            // Audio focus or device changed under us; a dropped cue is not fatal.
        }
    }

    /**
     * ~40 ms click: fast pitch sweep (up vs down) mixed with a decaying noise
     * burst. Sharp ~1.5 ms attack, ~10 ms exponential tail.
     */
    private fun buildClick(from: Double, to: Double, durationMs: Int = 40): AudioTrack {
        val n = sampleRate * durationMs / 1000
        val samples = ShortArray(n)
        val decay = sampleRate * 0.010   // ~10 ms tail
        val attackSamples = (sampleRate * 0.0015).toInt() // ~1.5 ms
        var phase = 0.0
        for (i in 0 until n) {
            val frac = i.toDouble() / n
            val freq = from + (to - from) * frac
            phase += 2.0 * Math.PI * freq / sampleRate
            val noise = (Math.random() * 2.0 - 1.0) // -1..1 broadband "clack"
            val env = Math.exp(-i / decay)
            val attack = (i / attackSamples).coerceIn(0.0, 1.0)
            val tone = Math.sin(phase) * 0.6 + noise * 0.45
            samples[i] = (tone * env * attack * volume * Short.MAX_VALUE)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        return track
    }

    fun release() {
        risingTrack?.release()
        risingTrack = null
        fallingTrack?.release()
        fallingTrack = null
    }
}