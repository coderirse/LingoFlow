package com.lingoflow.app.data.tts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** No-op [TtsEngine] for ViewModel tests. */
class FakeTtsEngine(
    ready: Boolean = true
) : TtsEngine {

    private val _isReady = MutableStateFlow(ready)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _playbackState = MutableStateFlow(TtsPlaybackState.IDLE)
    override val playbackState: StateFlow<TtsPlaybackState> =
        _playbackState.asStateFlow()

    val spoken = mutableListOf<String>()

    override fun speak(text: String, language: String) {
        spoken += text
        _playbackState.value = TtsPlaybackState.SPEAKING
    }

    override fun stop() {
        _playbackState.value = TtsPlaybackState.IDLE
    }

    override fun shutdown() {
        _playbackState.value = TtsPlaybackState.IDLE
        _isReady.value = false
    }

    /** Simulates the TTS engine finishing the current utterance. */
    fun finish() {
        _playbackState.value = TtsPlaybackState.IDLE
    }

    fun setReady(ready: Boolean) {
        _isReady.value = ready
    }
}
