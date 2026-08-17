package com.lingoflow.app.domain.model.llm

/** Supported LLM providers with their out-of-the-box endpoints. */
enum class LlmProviderId(
    val defaultBaseUrl: String,
    val defaultModel: String
) {
    /** Default provider. */
    DEEPSEEK("https://api.deepseek.com/v1", "deepseek-chat"),
    OPENAI("https://api.openai.com/v1", "gpt-4o-mini"),
    ANTHROPIC("https://api.anthropic.com/v1", "claude-sonnet-4-20250514"),
    GEMINI("https://generativelanguage.googleapis.com/v1beta", "gemini-2.0-flash"),
    MOONSHOT("https://api.moonshot.cn/v1", "kimi-k2"),

    /** User-supplied endpoint and model. */
    CUSTOM("", "")
}
