package com.lingoflow.app.data.tts

/**
 * Splits text into utterance-sized chunks for the platform TTS engine:
 * many engines reject or silently truncate one very long utterance, so
 * text is cut at sentence boundaries and packed into chunks of at most
 * [MAX_CHUNK_LENGTH] characters.
 */
object TtsTextChunker {

    /** Comfortably below common per-utterance engine limits (~4000). */
    const val MAX_CHUNK_LENGTH = 350

    private val sentenceBreak = Regex("(?<=[.。!？！?;；:])\\s*")

    fun chunk(text: String): List<String> {
        val sentences = text.split(sentenceBreak)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (sentences.isEmpty()) return emptyList()

        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        for (sentence in sentences) {
            if (sentence.length > MAX_CHUNK_LENGTH) {
                if (current.isNotEmpty()) {
                    chunks += current.toString()
                    current.clear()
                }
                chunks += hardSplit(sentence)
                continue
            }
            val separatorLength = if (current.isEmpty()) 0 else 1
            if (current.isNotEmpty() &&
                current.length + separatorLength + sentence.length > MAX_CHUNK_LENGTH
            ) {
                chunks += current.toString()
                current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        if (current.isNotEmpty()) chunks += current.toString()
        return chunks
    }

    /** Splits an oversized sentence at its last soft boundary, else hard. */
    private fun hardSplit(sentence: String): List<String> {
        val parts = mutableListOf<String>()
        var start = 0
        while (sentence.length - start > MAX_CHUNK_LENGTH) {
            val window = sentence.substring(start, start + MAX_CHUNK_LENGTH)
            val cut = window.lastIndexOfAny(charArrayOf(' ', ',', '、', '，', ';', '；'))
            val end = if (cut >= MAX_CHUNK_LENGTH / 2) {
                start + cut + 1
            } else {
                start + MAX_CHUNK_LENGTH
            }
            parts += sentence.substring(start, end)
            start = end
        }
        if (start < sentence.length) parts += sentence.substring(start)
        return parts
    }
}
