package com.lingoflow.app.data.tts

/** No-op [TtsEngine] for ViewModel tests. */
class FakeTtsEngine(
    override val isReady: Boolean = true
) : TtsEngine {

    val spoken = mutableListOf<String>()

    override fun speak(text: String, language: String) {
        spoken += text
    }

    override fun stop() = Unit

    override fun shutdown() = Unit
}
