package com.lingoflow.app.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [TtsEngine] backed by the platform [TextToSpeech] API.
 *
 * The platform engine has no pause API, so pause is emulated at the chunk
 * level: text is pre-split into sentence-sized chunks ([TtsTextChunker]),
 * pause stops the engine while remembering which chunk was playing, and
 * resume replays from that chunk. Precision is therefore sentence-level —
 * a pause mid-sentence restarts that sentence.
 */
@Singleton
class AndroidTtsEngine @Inject constructor(
    @ApplicationContext context: Context
) : TtsEngine {

    private var tts: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _playbackState = MutableStateFlow(TtsPlaybackState.IDLE)
    override val playbackState: StateFlow<TtsPlaybackState> =
        _playbackState.asStateFlow()

    /** Monotonic session counter; invalidates progress callbacks from old sessions. */
    private val sessionCounter = AtomicInteger(0)

    /** Chunks of the active (speaking or paused) session. */
    private var sessionChunks: List<String> = emptyList()
    private var sessionLanguage: String = "en-US"
    private var sessionCounterValue: Int = -1

    /** Index of the chunk currently playing, or the pause point when paused. */
    @Volatile
    private var activeChunkIndex: Int = -1

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                _isReady.value = true
            } else {
                _isReady.value = false
            }
        }
        tts?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val (session, chunk) = parseUtteranceId(utteranceId) ?: return
                    if (session != sessionCounterValue) return
                    activeChunkIndex = chunk
                    _playbackState.value = TtsPlaybackState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    val (session, chunk) = parseUtteranceId(utteranceId) ?: return
                    if (session != sessionCounterValue) return
                    if (chunk == sessionChunks.lastIndex) {
                        // The last chunk finished naturally: the whole text
                        // has been spoken.
                        clearSession(TtsPlaybackState.IDLE)
                    }
                    // Non-final chunks: the queued next chunk's onStart takes
                    // over; no state change here.
                }

                override fun onError(utteranceId: String?) {
                    fail(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    fail(utteranceId)
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    val (session, chunk) = parseUtteranceId(utteranceId) ?: return
                    if (session != sessionCounterValue) return
                    if (_playbackState.value == TtsPlaybackState.PAUSED) {
                        // pause() stopped us on purpose; the pause point is
                        // already recorded — keep PAUSED untouched.
                        return
                    }
                    if (chunk == activeChunkIndex) {
                        clearSession(TtsPlaybackState.IDLE)
                    }
                }

                private fun fail(utteranceId: String?) {
                    val (session, chunk) = parseUtteranceId(utteranceId) ?: return
                    if (session != sessionCounterValue) return
                    if (chunk == activeChunkIndex ||
                        _playbackState.value != TtsPlaybackState.PAUSED
                    ) {
                        clearSession(TtsPlaybackState.IDLE)
                    }
                }
            }
        )
    }

    override fun speak(text: String, language: String) {
        if (!_isReady.value || text.isBlank()) return
        // Long translations arrive as several queued utterances: a single
        // very long one would be rejected or truncated by many engines.
        val chunks = TtsTextChunker.chunk(text)
        if (chunks.isEmpty()) return
        sessionChunks = chunks
        sessionLanguage = language
        tts?.language = Locale.forLanguageTag(language)
        sessionCounterValue = sessionCounter.incrementAndGet()
        playFrom(0)
    }

    override fun pause() {
        if (_playbackState.value != TtsPlaybackState.SPEAKING) return
        // Record PAUSED first so the engine's onStop callback knows the stop
        // was intentional and leaves the session intact for resume().
        _playbackState.value = TtsPlaybackState.PAUSED
        tts?.stop()
    }

    override fun resume() {
        if (_playbackState.value != TtsPlaybackState.PAUSED) return
        val index = activeChunkIndex
        if (index !in sessionChunks.indices) {
            clearSession(TtsPlaybackState.IDLE)
            return
        }
        playFrom(index)
    }

    override fun stop() {
        clearSession(TtsPlaybackState.IDLE)
        tts?.stop()
    }

    override fun shutdown() {
        clearSession(TtsPlaybackState.IDLE)
        _isReady.value = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    /** Queues [sessionChunks] starting at [from]; index 0 flushes the queue. */
    private fun playFrom(from: Int) {
        activeChunkIndex = from
        sessionChunks.forEachIndexed { index, chunk ->
            if (index < from) return@forEachIndexed
            val queueMode = if (index == from) {
                TextToSpeech.QUEUE_FLUSH
            } else {
                TextToSpeech.QUEUE_ADD
            }
            val result = tts?.speak(chunk, queueMode, null, utteranceId(index))
            if (result != TextToSpeech.SUCCESS) {
                // A partial queue must never keep playing half the text.
                clearSession(TtsPlaybackState.IDLE)
                tts?.stop()
                return
            }
        }
        // Optimistically SPEAKING; onStart confirms (or a queue failure above
        // has already settled IDLE).
        _playbackState.value = TtsPlaybackState.SPEAKING
    }

    private fun utteranceId(chunkIndex: Int): String =
        "$sessionCounterValue:$chunkIndex"

    private fun parseUtteranceId(utteranceId: String?): Pair<Int, Int>? {
        val parts = utteranceId?.split(":") ?: return null
        val session = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val chunk = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return session to chunk
    }

    private fun clearSession(state: TtsPlaybackState) {
        sessionChunks = emptyList()
        sessionCounterValue = -1
        activeChunkIndex = -1
        _playbackState.value = state
    }
}
