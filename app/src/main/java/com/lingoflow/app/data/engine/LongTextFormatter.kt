package com.lingoflow.app.data.engine

/**
 * Normalizes LLM output for long STANDARD translations so numbered lists,
 * bullets and paragraph breaks stay readable even when the model merges
 * them into one block of text.
 */
object LongTextFormatter {

    fun format(translated: String): String {
        if (translated.isBlank()) return ""

        // Some providers/models return escaped newlines (e.g. "\n") instead
        // of real line endings; normalize those before anything else.
        val raw = translated
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\\n", "\n")

        val withListBreaks = raw
            .replace(numberedItemRegex, "\n")
            .replace(bulletedItemRegex, "\n")
            .replace(headingRegex, "\n")
            .replace(trailingSpaceBeforeLineBreakRegex, "\n")

        return withListBreaks
            .replace(paragraphBreaksRegex, "\n\n")
            .trim()
    }

    // "1. item", "2) item"… A digit lookbehind keeps dates ("2026.") and
    // decimals ("3.14") intact while still splitting items that were
    // merged into one line without punctuation between them.
    private val numberedItemRegex =
        Regex("(?<![\\d\\n])(?=\\d{1,2}(?:[.):]|[、．])\\s)")

    // Bullets merged inline after a sentence ending, e.g. "…as follows: - one".
    // Mid-sentence dashes ("a rare - if delightful - mistake") must NOT
    // split, and markers already at a line start need no extra break, so
    // only a sentence-ending punctuation followed by a plain space counts.
    private val bulletedItemRegex =
        Regex("(?<=[.。!！?？;；:：][ \\t])(?=\\s*[-*•]\\s)")

    // Headings that follow a completed sentence ("Done. Evidence to
    // prepare: …"). A capitalized segment after an ordinary word inside a
    // sentence ("The Steps: three parts") is not a heading.
    private val headingRegex =
        Regex("(?<=[.。!！?？][ \\t])(?=[A-Z\\u4e00-\\u9fff][^.!?\\n]{2,80}[:：]\\s)")

    private val paragraphBreaksRegex =
        Regex("\\n{3,}")

    private val trailingSpaceBeforeLineBreakRegex =
        Regex("[ \\t]+\\n")
}
