package com.lingoflow.app.domain.model.llm

/**
 * Supported LLM providers with their out-of-the-box endpoints. Every default
 * endpoint speaks the OpenAI chat-completions protocol; providers without a
 * native OpenAI-compatible API (e.g. Anthropic's /v1/messages) belong behind
 * a gateway configured as [CUSTOM].
 */
enum class LlmProviderId(
    val defaultBaseUrl: String,
    val defaultModel: String
) {
    /** Default provider. */
    DEEPSEEK("https://api.deepseek.com/v1", "deepseek-chat"),
    OPENAI("https://api.openai.com/v1", "gpt-4o-mini"),
    GEMINI("https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.0-flash"),
    MOONSHOT("https://api.moonshot.cn/v1", "kimi-k2"),

    /** User-supplied endpoint and model. */
    CUSTOM("", "")
}
