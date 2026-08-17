package com.lingoflow.app.data.tts

/** Text-to-speech abstraction for word/translation pronunciation. */
interface TtsEngine {

    /** True once the underlying engine is ready to speak. */
    val isReady: Boolean

    fun speak(text: String, language: String = "en-US")

    fun stop()

    fun shutdown()
}
