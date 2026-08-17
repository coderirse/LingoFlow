package com.lingoflow.app.domain.model.settings

import com.lingoflow.app.domain.model.llm.LlmProviderId

/** User configuration for one LLM provider. */
data class ProviderConfig(
    val providerId: LlmProviderId,
    val apiKey: String,
    /** Overrides the provider's default endpoint when non-null. */
    val baseUrl: String?,
    val model: String,
    val temperature: Float = 0.7f
)
