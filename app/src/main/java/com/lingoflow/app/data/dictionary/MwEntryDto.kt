package com.lingoflow.app.data.dictionary

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Minimal DTOs for the Merriam-Webster Collegiate Dictionary JSON payload. */
@Serializable
data class MwEntryDto(
    val meta: MetaDto,
    val hwi: HwiDto? = null,
    val fl: String? = null,
    val def: List<DefDto>? = null,
    val dros: List<DroDto>? = null,
    val et: List<List<String>>? = null,
    val shortdef: List<String>? = null
)

@Serializable
data class MetaDto(
    val id: String,
    val stems: List<String> = emptyList()
)

@Serializable
data class HwiDto(
    val hw: String,
    val prs: List<PrsDto>? = null
)

@Serializable
data class PrsDto(
    val mw: String? = null,
    val sound: SoundDto? = null
)

@Serializable
data class SoundDto(
    val audio: String? = null
)

@Serializable
data class DefDto(
    val sseq: List<List<JsonElement>> = emptyList()
)

@Serializable
data class DroDto(
    val drp: String,
    val def: List<DefDto>? = null
)
