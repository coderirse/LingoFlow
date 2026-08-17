package com.lingoflow.app.data.dictionary

import com.lingoflow.app.domain.exception.DictionaryException
import com.lingoflow.app.domain.model.dictionary.Definition
import com.lingoflow.app.domain.model.dictionary.DictionaryEntry
import com.lingoflow.app.domain.model.dictionary.Example
import com.lingoflow.app.domain.model.dictionary.PartOfSpeechEntry
import com.lingoflow.app.domain.model.dictionary.Phonetic
import com.lingoflow.app.domain.model.dictionary.PhrasalVerb
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Converts Merriam-Webster Collegiate Dictionary JSON into domain
 * [DictionaryEntry] models. Pure Kotlin, no Android dependencies.
 *
 * A top-level string array means the word was not found and MW is returning
 * spelling suggestions; that becomes [DictionaryException.NotFound].
 */
object MwJsonParser {

    private val json = Json { ignoreUnknownKeys = true }

    private const val AUDIO_BASE_URL =
        "https://media.merriam-webster.com/audio/prons/en/us/mp3"

    fun parse(jsonString: String): Result<List<DictionaryEntry>> {
        val root = try {
            json.parseToJsonElement(jsonString)
        } catch (e: Exception) {
            return Result.failure(DictionaryException.ParseError(e))
        }

        val array = root as? JsonArray
            ?: return Result.failure(
                DictionaryException.ParseError(IllegalStateException("Unexpected payload shape"))
            )

        // Spelling suggestions come back as a plain string array.
        if (array.isEmpty() || array.first() is JsonPrimitive) {
            val suggestions = array.mapNotNull {
                (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
            }
            return Result.failure(DictionaryException.NotFound(suggestions))
        }

        return try {
            val dtos = json.decodeFromJsonElement<List<MwEntryDto>>(root)
            Result.success(dtos.map { it.toDictionaryEntry() })
        } catch (e: Exception) {
            Result.failure(DictionaryException.ParseError(e))
        }
    }

    private fun MwEntryDto.toDictionaryEntry(): DictionaryEntry {
        val headword = hwi.hw.replace("*", "")
        val definitions = parseDefinitions(def)

        return DictionaryEntry(
            word = meta.id.substringBefore(":"),
            phonetics = hwi.prs.orEmpty().mapNotNull { prs ->
                prs.mw?.let {
                    Phonetic(text = it, audioUrl = prs.sound?.audio?.let(::audioUrl))
                }
            },
            entries = listOf(
                PartOfSpeechEntry(
                    partOfSpeech = fl ?: "",
                    definitions = definitions
                )
            ),
            phrases = dros.orEmpty().map { dro ->
                val droDefinitions = parseDefinitions(dro.def)
                PhrasalVerb(
                    phrase = dro.drp.replace("*", ""),
                    meaning = droDefinitions.firstOrNull()?.meaning ?: "",
                    examples = droDefinitions.firstOrNull()?.examples ?: emptyList()
                )
            },
            etymology = et?.firstOrNull { it.firstOrNull() == "text" }
                ?.getOrNull(1)
                ?.let(::cleanMwText)
        )
    }

    /**
     * Walks MW's nested sseq structure, unwrapping plain senses and "bs"
     * (bound sense) wrappers, and extracts sense number, defining text,
     * verbal illustrations and status labels.
     */
    private fun parseDefinitions(defs: List<DefDto>?): List<Definition> {
        val definitions = mutableListOf<Definition>()

        defs.orEmpty().forEach { defDto ->
            defDto.sseq.forEach { senseGroup ->
                senseGroup.forEach { element ->
                    val pair = element as? JsonArray ?: return@forEach
                    val tag = (pair.getOrNull(0) as? JsonPrimitive)?.contentOrNull
                    val payload = pair.getOrNull(1) as? JsonObject ?: return@forEach

                    val sense = when (tag) {
                        "sense" -> payload
                        "bs" -> payload["sense"] as? JsonObject
                        else -> null
                    } ?: return@forEach

                    definitions += Definition(
                        meaning = parseMeaning(sense),
                        senseNumber = sense.stringOrNull("sn"),
                        labels = sense.stringList("sls"),
                        examples = parseExamples(sense)
                    )
                }
            }
        }
        return definitions
    }

    private fun parseMeaning(sense: JsonObject): String {
        val texts = sense.dtEntries()
            .filter { it.first == "text" }
            .mapNotNull { (it.second as? JsonPrimitive)?.contentOrNull }
            .map(::cleanMwText)
            .filter { it.isNotBlank() }
        return texts.joinToString(" ")
    }

    private fun parseExamples(sense: JsonObject): List<Example> =
        sense.dtEntries()
            .filter { it.first == "vis" }
            .flatMap { (_, payload) ->
                (payload as? JsonArray).orEmpty()
            }
            .mapNotNull { vis ->
                (vis as? JsonObject)?.stringOrNull("t")
                    ?.let(::cleanMwText)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { Example(sentence = it, source = null) }
            }

    /** Flattens a sense's "dt" array into (type, payload) pairs. */
    private fun JsonObject.dtEntries(): List<Pair<String, JsonElement>> =
        (this["dt"] as? JsonArray).orEmpty().mapNotNull { item ->
            val pair = item as? JsonArray ?: return@mapNotNull null
            val type = (pair.getOrNull(0) as? JsonPrimitive)?.contentOrNull
                ?: return@mapNotNull null
            val payload = pair.getOrNull(1) ?: return@mapNotNull null
            type to payload
        }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.stringList(key: String): List<String> =
        (this[key] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }

    /**
     * Strips Merriam-Webster markup tokens ({bc}, {wi}…{/wi}, {a_link|x}, …)
     * and unescapes {\/} sequences, leaving plain readable text.
     */
    internal fun cleanMwText(raw: String): String {
        var text = raw

        // {\/} and {\/wi}-style escapes become plain "/" after JSON decoding,
        // so only real tokens remain at this point.
        val pairedTags = Regex(
            "\\{(?:wi|it|phrase|qword|sc|b|inf|sup|parahw|dx_def|dx_ety|dx|ma)\\}" +
                "(.*?)" +
                "\\{/(?:wi|it|phrase|qword|sc|b|inf|sup|parahw|dx_def|dx_ety|dx|ma)\\}"
        )
        text = pairedTags.replace(text) { it.groupValues[1] }

        // {a_link|word}, {sx|word||}, {d_link|word|id}, {mat|word|id} → word
        text = Regex("\\{(?:a_link|sx|d_link|i_link|et_link|mat)\\|([^|}]+)[^}]*\\}")
            .replace(text, "$1")

        text = text
            .replace("{bc}", "")
            .replace("{ldquo}", "“")
            .replace("{rdquo}", "”")
            .replace("{gloss}", "")
            .replace("{/gloss}", "")
            .replace("{sc}", "")
            .replace("{/sc}", "")

        // Drop any remaining simple {token} / {/token} markers.
        text = Regex("\\{/?[a-z_]+\\}").replace(text, "")

        return text.replace(Regex("\\s+"), " ").trim()
    }

    private fun audioUrl(audio: String): String {
        val subdir = when {
            audio.startsWith("bix") -> "bix"
            audio.startsWith("gg") -> "gg"
            audio.first().isDigit() || !audio.first().isLetterOrDigit() -> "number"
            else -> audio.first().toString()
        }
        return "$AUDIO_BASE_URL/$subdir/$audio.mp3"
    }
}
