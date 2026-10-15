package com.tickbeat.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.tickbeat.app.domain.Beatmap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Plays a [Beatmap] once through AudioTrack and reports audible-time beat callbacks.
 */
class RhythmPlaybackEngine {

    companion object {
        private const val SAMPLE_RATE = 44_100
        private const val CLICK_MS = 12
    }

    private val playing = AtomicBoolean(false)
    private val generation = AtomicInteger(0)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var worker: Thread? = null
    private var track: AudioTrack? = null

    private val accentClick = synthesize(1760.0)
    private val normalClick = synthesize(880.0)

    @Volatile
    var onEventAudible: ((index: Int, audibleUptimeMs: Long) -> Unit)? = null

    @Volatile
    var onStarted: ((audibleOriginUptimeMs: Long) -> Unit)? = null

    @Volatile
    var onCompleted: (() -> Unit)? = null

    /** Session origin (uptime millis) corresponding to tBeat=0 at the speaker. */
    @Volatile
    var audibleOriginUptimeMs: Long = 0L
        private set

    fun isPlaying(): Boolean = playing.get()

    @Synchronized
    fun play(beatmap: Beatmap, muted: Boolean = false) {
        stop()
        if (beatmap.events.isEmpty()) {
            onCompleted?.invoke()
            return
        }
        playing.set(true)
        val gen = generation.incrementAndGet()

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
            .setBufferSizeInBytes(minBuf)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        track = audioTrack
        audioTrack.play()
        val latencyMs = estimateLatencyMs(audioTrack)
        val beatMs = 60_000.0 / beatmap.bpm.coerceAtLeast(1)

        worker = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            val silence = ShortArray(256)
            var samplesWritten = 0L
            val writeStartUptime = SystemClock.uptimeMillis()
            audibleOriginUptimeMs = writeStartUptime + latencyMs
            mainHandler.post {
                if (generation.get() == gen) onStarted?.invoke(audibleOriginUptimeMs)
            }

            val schedule = beatmap.events.mapIndexed { index, event ->
                val sampleAt = (event.tBeat * beatMs * SAMPLE_RATE / 1000.0).toLong()
                Triple(index, sampleAt, event.accent)
            }.sortedBy { it.second }

            var next = 0
            while (playing.get() && generation.get() == gen && next < schedule.size) {
                val (index, sampleAt, accent) = schedule[next]
                val need = (sampleAt - samplesWritten).coerceAtLeast(0)
                var remaining = need
                while (remaining > 0 && playing.get() && generation.get() == gen) {
                    val n = min(silence.size.toLong(), remaining).toInt()
                    writeFully(audioTrack, silence, n)
                    samplesWritten += n
                    remaining -= n
                }
                if (!playing.get() || generation.get() != gen) break

                val click = if (accent) accentClick else normalClick
                val audibleAt = audibleOriginUptimeMs + (sampleAt * 1000L / SAMPLE_RATE)
                mainHandler.postAtTime(
                    {
                        if (generation.get() == gen) onEventAudible?.invoke(index, audibleAt)
                    },
                    audibleAt
                )
                if (!muted) {
                    writeFully(audioTrack, click)
                    samplesWritten += click.size
                } else {
                    // Keep timeline by writing equivalent silence.
                    var left = click.size
                    while (left > 0 && playing.get() && generation.get() == gen) {
                        val n = min(silence.size, left)
                        writeFully(audioTrack, silence, n)
                        left -= n
                    }
                    samplesWritten += click.size
                }
                next++
            }

            // Tail silence so the last click can leave the buffer.
            if (playing.get() && generation.get() == gen) {
                repeat(8) {
                    writeFully(audioTrack, silence)
                }
            }
            if (generation.get() == gen) {
                mainHandler.post { onCompleted?.invoke() }
            }
            playing.set(false)
        }, "RhythmPlayback").also {
            it.isDaemon = true
            it.start()
        }
    }

    @Synchronized
    fun stop() {
        playing.set(false)
        generation.incrementAndGet()
        mainHandler.removeCallbacksAndMessages(null)

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
        }, "RhythmPlayback-stop").apply {
            isDaemon = true
            start()
        }
    }

    private fun estimateLatencyMs(track: AudioTrack): Long {
        val bufferMs = track.bufferSizeInFrames * 1000L / SAMPLE_RATE
        return (bufferMs + 25L).coerceIn(30L, 180L)
    }

    private fun writeFully(track: AudioTrack, data: ShortArray, length: Int = data.size) {
        var offset = 0
        while (offset < length && playing.get()) {
            val written = track.write(data, offset, length - offset)
            if (written <= 0) break
            offset += written
        }
    }

    private fun synthesize(hz: Double): ShortArray {
        val length = SAMPLE_RATE * CLICK_MS / 1000
        val samples = ShortArray(length)
        for (i in 0 until length) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = 1.0 - i.toDouble() / length
            samples[i] = (sin(2.0 * PI * hz * t) * env * Short.MAX_VALUE * 0.55).toInt().toShort()
        }
        return samples
    }
}
