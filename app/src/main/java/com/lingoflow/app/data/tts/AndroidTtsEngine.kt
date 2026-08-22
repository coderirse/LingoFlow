package com.lingoflow.app.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [TtsEngine] backed by the platform [TextToSpeech] API. */
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

    private val utteranceSequence = AtomicLong(0)

    @Volatile
    private var activeUtteranceId: String? = null

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
                    if (utteranceId == activeUtteranceId) {
                        _playbackState.value = TtsPlaybackState.SPEAKING
                    }
                }

                override fun onDone(utteranceId: String?) {
                    finish(utteranceId)
                }

                override fun onError(utteranceId: String?) {
                    finish(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    finish(utteranceId)
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    finish(utteranceId)
                }

                private fun finish(utteranceId: String?) {
                    if (utteranceId == activeUtteranceId) {
                        activeUtteranceId = null
                        _playbackState.value = TtsPlaybackState.IDLE
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
        tts?.language = Locale.forLanguageTag(language)
        chunks.forEachIndexed { index, chunk ->
            val utteranceId = utteranceSequence.incrementAndGet().toString()
            val queueMode = if (index == 0) {
                TextToSpeech.QUEUE_FLUSH
            } else {
                TextToSpeech.QUEUE_ADD
            }
            val result = tts?.speak(chunk, queueMode, null, utteranceId)
            if (result == TextToSpeech.SUCCESS) {
                // The last queued chunk drives the speaking→idle transition.
                activeUtteranceId = utteranceId
            } else {
                // A partial queue must never keep playing half the text.
                activeUtteranceId = null
                _playbackState.value = TtsPlaybackState.IDLE
                tts?.stop()
                return
            }
        }
        _playbackState.value = TtsPlaybackState.SPEAKING
    }

    override fun stop() {
        activeUtteranceId = null
        _playbackState.value = TtsPlaybackState.IDLE
        tts?.stop()
    }

    override fun shutdown() {
        activeUtteranceId = null
        _playbackState.value = TtsPlaybackState.IDLE
        _isReady.value = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
