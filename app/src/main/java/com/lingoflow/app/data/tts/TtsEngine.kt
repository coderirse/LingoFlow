package com.lingoflow.app.data.tts

import kotlinx.coroutines.flow.StateFlow

/** Text-to-speech abstraction for word/translation pronunciation. */
enum class TtsPlaybackState {
    /** No utterance is currently being spoken. */
    IDLE,

    /** An utterance is currently being spoken. */
    SPEAKING
}

interface TtsEngine {

    /** True once the underlying engine is ready to speak. */
    val isReady: StateFlow<Boolean>

    /** Current playback state, used to drive the play/pause button. */
    val playbackState: StateFlow<TtsPlaybackState>

    fun speak(text: String, language: String = "en-US")

    fun stop()

    fun shutdown()
}
