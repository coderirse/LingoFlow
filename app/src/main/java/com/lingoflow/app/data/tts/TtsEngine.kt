package com.lingoflow.app.data.tts

import kotlinx.coroutines.flow.StateFlow

/** Text-to-speech abstraction for word/translation pronunciation. */
enum class TtsPlaybackState {
    /** No utterance is currently being spoken and nothing is held for resume. */
    IDLE,

    /** An utterance is currently being spoken. */
    SPEAKING,

    /**
     * Playback was paused mid-text: the platform engine is stopped but the
     * text position is held, so [TtsEngine.resume] continues from the
     * sentence after the pause point.
     */
    PAUSED
}

interface TtsEngine {

    /** True once the underlying engine is ready to speak. */
    val isReady: StateFlow<Boolean>

    /** Current playback state, used to drive the play/pause button. */
    val playbackState: StateFlow<TtsPlaybackState>

    /** Starts (or restarts) speaking [text] from the beginning. */
    fun speak(text: String, language: String = "en-US")

    /**
     * Pauses playback: the platform TTS has no pause API, so this stops the
     * engine while holding the text position; a following [resume] continues
     * from the sentence after the pause point. No-op when not speaking.
     */
    fun pause()

    /** Resumes a [pause]d session from the held position. No-op otherwise. */
    fun resume()

    /** Stops playback and discards any held resume position. */
    fun stop()

    fun shutdown()
}
