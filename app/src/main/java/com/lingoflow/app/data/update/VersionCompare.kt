package com.lingoflow.app.data.update

/** Pure semver-ish comparison, tolerant of a leading "v" and missing parts. */
object VersionCompare {

    /** True when [latest] is strictly newer than [current] (e.g. "1.2.0" > "1.1.9"). */
    fun isNewer(latest: String, current: String): Boolean {
        val a = parts(latest)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun parts(version: String): List<Int> =
        version.trim().removePrefix("v")
            .split(".")
            .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
