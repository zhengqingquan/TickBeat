package com.tickbeat.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Plays short pitched tones via AudioTrack.
 * Additive harmonics + exponential decay — closer to piano than a pure sine.
 */
class TonePlaybackEngine {

    companion object {
        private const val SAMPLE_RATE = 44_100
        private const val DEFAULT_DURATION_MS = 700
        private const val ATTACK_MS = 4
        /** Relative levels for partials 1..N (piano-ish bright attack, softer highs). */
        private val HARMONIC_GAIN = doubleArrayOf(
            1.00, 0.52, 0.32, 0.18, 0.12, 0.07, 0.04, 0.025
        )
    }

    private val playing = AtomicBoolean(false)
    private val generation = AtomicInteger(0)

    private var worker: Thread? = null
    private var track: AudioTrack? = null

    fun playNote(midi: Int, durationMs: Int = DEFAULT_DURATION_MS) {
        stop()
        playing.set(true)
        val gen = generation.incrementAndGet()
        val hz = midiToHz(midi)
        val samples = synthesizePianoTone(hz, durationMs.coerceIn(80, 2_500))

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuf.coerceAtLeast(samples.size * 2))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        track = audioTrack
        audioTrack.play()

        worker = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            if (generation.get() == gen && playing.get()) {
                writeFully(audioTrack, samples)
            }
            if (generation.get() == gen) {
                playing.set(false)
            }
        }, "TonePlayback").also {
            it.isDaemon = true
            it.start()
        }
    }

    @Synchronized
    fun stop() {
        playing.set(false)
        generation.incrementAndGet()

        val endingWorker = worker
        val endingTrack = track
        worker = null
        track = null
        if (endingWorker == null && endingTrack == null) return

        Thread({
            endingWorker?.join(500)
            endingTrack?.let {
                runCatching {
                    it.pause()
                    it.flush()
                    it.stop()
                    it.release()
                }
            }
        }, "TonePlayback-stop").apply {
            isDaemon = true
            start()
        }
    }

    private fun writeFully(track: AudioTrack, data: ShortArray) {
        var offset = 0
        while (offset < data.size && playing.get()) {
            val written = track.write(data, offset, data.size - offset)
            if (written <= 0) break
            offset += written
        }
    }

    private fun midiToHz(midi: Int): Double =
        440.0 * 2.0.pow((midi - 69) / 12.0)

    /**
     * Additive synthesis: several decaying harmonics with a short hammer attack.
     * Not a sampled piano, but much less "pure sine / electronic beep".
     */
    private fun synthesizePianoTone(hz: Double, durationMs: Int): ShortArray {
        val length = SAMPLE_RATE * durationMs / 1000
        val attack = (SAMPLE_RATE * ATTACK_MS / 1000).coerceAtMost(length / 8).coerceAtLeast(1)
        // Slight stretch for higher partials (string inharmonicity).
        val inharm = 0.0003
        val samples = ShortArray(length)
        val twoPi = 2.0 * PI

        for (i in 0 until length) {
            val t = i.toDouble() / SAMPLE_RATE
            val attackEnv = if (i < attack) i.toDouble() / attack else 1.0
            // Overall decay ~ piano sustain; higher partials die faster.
            var mixed = 0.0
            for (h in HARMONIC_GAIN.indices) {
                val n = h + 1
                val partialHz = hz * n * (1.0 + inharm * n * n)
                val tau = 0.55 / (1.0 + 0.35 * (n - 1))
                val harmEnv = exp(-t / tau)
                mixed += HARMONIC_GAIN[h] * harmEnv * sin(twoPi * partialHz * t)
            }
            // Soft clip into int16.
            val sample = (mixed * attackEnv * 0.28).coerceIn(-1.0, 1.0)
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }
}
