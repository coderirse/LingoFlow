package com.lingoflow.app.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** [TtsEngine] backed by the platform [TextToSpeech] API. */
@Singleton
class AndroidTtsEngine @Inject constructor(
    @ApplicationContext context: Context
) : TtsEngine {

    private var tts: TextToSpeech? = null

    @Volatile
    private var initialized = false

    override val isReady: Boolean get() = initialized

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                initialized = true
            }
        }
    }

    override fun speak(text: String, language: String) {
        if (!initialized || text.isBlank()) return
        tts?.language = Locale.forLanguageTag(language)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        initialized = false
    }
}
