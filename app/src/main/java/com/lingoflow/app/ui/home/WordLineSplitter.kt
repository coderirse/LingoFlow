package com.lingoflow.app.ui.home

/**
 * Line model behind the tap-to-lookup rendering: paragraph breaks from the
 * translation must survive the whitespace-based word split, so text is
 * broken into display lines first and blank entries mark paragraph gaps.
 */
object WordLineSplitter {

    private val lineBreaks = Regex("\r\n|\r|\n")

    /**
     * Splits [text] into lines; empty strings mark paragraph gaps. Leading
     * and trailing gaps are dropped and runs of blank lines collapse into
     * a single gap.
     */
    fun lines(text: String): List<String> {
        val raw = text.split(lineBreaks)
        val result = mutableListOf<String>()
        var previousWasBlank = true
        raw.forEach { line ->
            if (line.isBlank()) {
                if (!previousWasBlank) result += ""
                previousWasBlank = true
            } else {
                result += line
                previousWasBlank = false
            }
        }
        // Gap runs touching either end are not paragraph gaps.
        return result.dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() }
    }
}
