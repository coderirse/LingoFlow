package com.lingoflow.app.data.dictionary

/**
 * Generates base-form candidates for an inflected English word so a failed
 * dictionary lookup can be retried (Merriam-Webster indexes headwords, not
 * inflections like "served" or "studies"). Rule-based and intentionally
 * conservative: candidates are ordered most-to-least likely.
 */
object InflectionStemmer {

    fun candidates(word: String): List<String> {
        val w = word.trim().lowercase()
        if (w.length < 3 || !w.all { it.isLetter() }) return emptyList()

        val stems = linkedSetOf<String>()

        // -ing forms: running → run, making → make, playing → play
        if (w.endsWith("ing") && w.length > 4) {
            val base = w.dropLast(3)
            stems += base
            stems += "$base" + "e"
            stems += undouble(base)
        }

        // -ed forms: served → serve, stopped → stop, played → play
        if (w.endsWith("ed") && w.length > 3) {
            val base = w.dropLast(2)
            stems += w.dropLast(1) // served → serve
            stems += base
            stems += undouble(base)
        }

        // -ies forms: studies → study
        if (w.endsWith("ies") && w.length > 4) {
            stems += w.dropLast(3) + "y"
        }

        // -es forms: watches → watch, boxes → box
        if (w.endsWith("es") && w.length > 3) {
            stems += w.dropLast(2)
        }

        // -s forms: cats → cat (skip -ss to avoid class → clas)
        if (w.endsWith("s") && !w.endsWith("ss") && w.length > 3) {
            stems += w.dropLast(1)
        }

        return stems.filter { it.length >= 2 && it != w }
    }

    /** running → runing → run; stopped → stoped → stop (applied to the base). */
    private fun undouble(base: String): String =
        if (base.length >= 2 && base.last() == base[base.length - 2] &&
            base.last() !in "aeiou"
        ) {
            base.dropLast(1)
        } else {
            base
        }
}
