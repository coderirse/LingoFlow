package com.lingoflow.app.ui.i18n

import androidx.compose.runtime.compositionLocalOf
import com.lingoflow.app.domain.model.settings.AppLanguage
import com.lingoflow.app.domain.model.translation.TranslationErrors

/**
 * All user-facing UI strings, provided per AppLanguage via [LocalStrings].
 * Engine-layer failures arrive as stable codes ([TranslationErrors]) and are
 * localized here via [localizedError]; unknown codes fall back to the
 * generic translation-failed message.
 */
data class AppStrings(
    // Which language this table speaks; drives parameterized messages.
    val lang: AppLanguage,
    // Tabs & top bar
    val tabTranslate: String,
    val tabHistory: String,
    val tabLearning: String,
    val settings: String,
    // Input
    val enterText: String,
    val paste: String,
    val clearInput: String,
    // Translate button & status
    val translate: String,
    val translating: String,
    val preparingModel: String,
    val cancel: String,
    // Result card
    val translationTitle: String,
    val speakTranslation: String,
    val pauseTranslation: String,
    val resumeTranslation: String,
    val noticeLlmKeyMissing: String,
    val noticeLlmFailed: String,
    val noticeStreamInterrupted: String,
    val emptyResult: String,
    val analysis: String,
    val keyWords: String,
    val copy: String,
    val copied: String,
    val shareTranslation: String,
    val favoriteTranslation: String,
    val synonymsPrefix: String,
    // Dictionary
    val dictionary: String,
    val englishWordHint: String,
    val lookUpWord: String,
    val lookUp: String,
    val enterWordToLookup: String,
    val viewFullDefinition: String,
    val playPronunciation: String,
    val addToFavorites: String,
    val removeFromFavorites: String,
    val phrasalVerbs: String,
    val etymology: String,
    val noApiKey: String,
    val invalidApiKey: String,
    val networkError: String,
    val parseError: String,
    val wordNotFound: String,
    val noResults: String,
    val chineseMeaningUnavailable: String,
    val goToSettings: String,
    // Branding
    val copyright: String,
    val viewOnGitHub: String,
    // History
    val clearAll: String,
    val clearAllTitle: String,
    val clearAllMessage: String,
    val clear: String,
    val noHistory: String,
    val deleteRecord: String,
    val recordDeleted: String,
    val undo: String,
    // Learning
    val noFavoriteWords: String,
    val noFavoriteTranslations: String,
    val favoriteWordsSection: String,
    val favoriteTranslationsSection: String,
    // Settings
    val back: String,
    val llmProvider: String,
    val provider: String,
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val temperature: String,
    val dictionarySection: String,
    val mwApiKey: String,
    val translationSection: String,
    val defaultMode: String,
    val appearance: String,
    val theme: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val interfaceStyle: String,
    val styleModern: String,
    val styleEditorial: String,
    val language: String,
    val about: String,
    val currentVersion: String,
    val checkForUpdates: String,
    val checking: String,
    val upToDate: String,
    val updateFailed: String,
    val saveSettings: String,
    val settingsSaved: String,
    val unsavedChangesTitle: String,
    val unsavedChangesMessage: String,
    val discard: String,
    val keepEditing: String,
    val show: String,
    val hide: String
) {
    fun wordNotFoundWithSuggestions(suggestions: String) =
        "$wordNotFound $suggestions"

    fun newVersionAvailable(tag: String) =
        if (lang == AppLanguage.CHINESE) "发现新版本：$tag" else "New version available: $tag"

    fun downloadVersion(tag: String) =
        if (lang == AppLanguage.CHINESE) "下载 $tag" else "Download $tag"

    /** Localized text for a [TranslationErrors] code (unknown → generic). */
    fun localizedError(code: String?): String = when (code) {
        TranslationErrors.NOTHING_TO_TRANSLATE -> errNothingToTranslate
        TranslationErrors.LANGUAGE_DETECT_UNAVAILABLE -> errLanguageDetectUnavailable
        TranslationErrors.LANGUAGE_UNSUPPORTED -> errLanguageUnsupported
        TranslationErrors.LANGUAGE_UNDETECTED -> errLanguageUndetected
        TranslationErrors.MODEL_UNAVAILABLE -> errModelUnavailable
        TranslationErrors.NOT_ENOUGH_SPACE -> errNotEnoughSpace
        TranslationErrors.LLM_KEY_MISSING -> errLlmKeyMissing
        TranslationErrors.LLM_KEY_INVALID -> errLlmKeyInvalid
        TranslationErrors.LLM_RATE_LIMITED -> errLlmRateLimited
        TranslationErrors.LLM_NETWORK -> errLlmNetwork
        TranslationErrors.LLM_SERVER -> errLlmServer
        TranslationErrors.LLM_GENERIC -> errLlmGeneric
        TranslationErrors.INVALID_BASE_URL -> errInvalidBaseUrl
        TranslationErrors.TRUNCATED -> errTruncated
        "err_settings_load_failed" -> errSettingsLoadFailed
        "err_settings_save_failed" -> errSettingsSaveFailed
        null, TranslationErrors.GENERIC -> errTranslationFailed
        else -> errTranslationFailed
    }

    // Localized error texts (referenced by [localizedError]).
    val errNothingToTranslate: String
        get() = if (lang == AppLanguage.CHINESE) "没有可翻译的内容。" else "Nothing to translate."

    val errLanguageDetectUnavailable: String
        get() = if (lang == AppLanguage.CHINESE) "语言检测不可用，请手动选择源语言。" else "Language detection is unavailable. Please select the source language manually."

    val errLanguageUnsupported: String
        get() = if (lang == AppLanguage.CHINESE) "暂不支持检测到的语言。" else "Detected language is not supported yet."

    val errLanguageUndetected: String
        get() = if (lang == AppLanguage.CHINESE) "无法识别语言，请手动选择。" else "Couldn't detect the language. Please select it manually."

    val errModelUnavailable: String
        get() = if (lang == AppLanguage.CHINESE) "翻译模型不可用，请检查网络连接。" else "Translation model is unavailable. Check your network connection."

    val errNotEnoughSpace: String
        get() = if (lang == AppLanguage.CHINESE) "存储空间不足，无法下载翻译模型。" else "Not enough storage to download the translation model."

    val errTranslationFailed: String
        get() = if (lang == AppLanguage.CHINESE) "翻译失败，请重试。" else "Translation failed. Please try again."

    val errLlmKeyMissing: String
        get() = if (lang == AppLanguage.CHINESE) "未配置 LLM API Key，请在设置中填写。" else "LLM API key is not configured. Please check your Settings."

    val errLlmKeyInvalid: String
        get() = if (lang == AppLanguage.CHINESE) "LLM API Key 无效，请检查设置。" else "LLM API key is invalid. Please check your Settings."

    val errLlmRateLimited: String
        get() = if (lang == AppLanguage.CHINESE) "LLM 请求频率超限，请稍后再试。" else "LLM rate limit reached. Please try again later."

    val errLlmNetwork: String
        get() = if (lang == AppLanguage.CHINESE) "网络错误，请检查网络连接。" else "Network error. Please check your connection."

    val errLlmServer: String
        get() = if (lang == AppLanguage.CHINESE) "模型服务返回错误，请稍后再试。" else "The model service returned an error. Please try again later."

    val errLlmGeneric: String
        get() = if (lang == AppLanguage.CHINESE) "翻译失败，请重试。" else "Translation failed. Please try again."

    val errInvalidBaseUrl: String
        get() = if (lang == AppLanguage.CHINESE) "Base URL 缺失或无效，请在设置中检查。" else "Base URL is missing or invalid. Please check your Settings."

    val errTruncated: String
        get() = if (lang == AppLanguage.CHINESE) "译文被模型输出上限截断，请尝试更短的文本。" else "The translation was cut off by the model's output limit. Try a shorter text."

    val errSettingsLoadFailed: String
        get() = if (lang == AppLanguage.CHINESE) "设置加载失败，请重试。" else "Failed to load settings. Please try again."

    val errSettingsSaveFailed: String
        get() = if (lang == AppLanguage.CHINESE) "设置保存失败，请重试。" else "Failed to save settings. Please try again."
}

val EnStrings = AppStrings(
    lang = AppLanguage.ENGLISH,
    tabTranslate = "Translate",
    tabHistory = "History",
    tabLearning = "Learning",
    settings = "Settings",
    enterText = "Enter text to translate...",
    paste = "Paste",
    clearInput = "Clear input",
    translate = "Translate",
    translating = "Translating...",
    preparingModel = "Preparing translation model...",
    cancel = "Cancel",
    translationTitle = "Translation",
    speakTranslation = "Speak translation",
    pauseTranslation = "Pause playback",
    resumeTranslation = "Resume playback",
    noticeLlmKeyMissing = "LLM API key not set. Using on-device translation.",
    noticeLlmFailed = "LLM translation failed. Using on-device translation.",
    noticeStreamInterrupted = "Translation interrupted. Partial result kept.",
    emptyResult = "Your translation will appear here.",
    analysis = "Analysis",
    keyWords = "Key Words",
    copy = "Copy",
    copied = "Copied",
    shareTranslation = "Share translation",
    favoriteTranslation = "Favorite translation",
    synonymsPrefix = "Synonyms: ",
    dictionary = "Dictionary",
    englishWordHint = "English word...",
    lookUpWord = "Look up word",
    lookUp = "Look Up",
    enterWordToLookup = "Enter a word to look it up.",
    viewFullDefinition = "View Full Definition →",
    playPronunciation = "Play pronunciation",
    addToFavorites = "Add to favorites",
    removeFromFavorites = "Remove from favorites",
    phrasalVerbs = "Phrasal verbs",
    etymology = "Etymology",
    noApiKey = "Please set your Merriam-Webster API key in Settings.",
    invalidApiKey = "Invalid API key. Please check your Settings.",
    networkError = "Network error. Please check your connection.",
    parseError = "Couldn't read the dictionary response. Please try again later.",
    wordNotFound = "Word not found. Did you mean: ",
    noResults = "No matching entry found.",
    chineseMeaningUnavailable = "Chinese definition unavailable.",
    goToSettings = "Go to Settings",
    copyright = "Copyright © 2026 Stafind. All rights reserved.",
    viewOnGitHub = "View on GitHub",
    clearAll = "Clear All",
    clearAllTitle = "Clear all history?",
    clearAllMessage = "History records will be permanently deleted. Favorited translations are kept.",
    clear = "Clear",
    noHistory = "No history yet",
    deleteRecord = "Delete record",
    recordDeleted = "Record deleted",
    undo = "Undo",
    noFavoriteWords = "No favorite words yet.\nLook up a word and tap the heart!",
    noFavoriteTranslations = "No favorite translations yet.\nTap the heart on a translation to save it!",
    favoriteWordsSection = "Words",
    favoriteTranslationsSection = "Translations",
    back = "Back",
    llmProvider = "LLM Provider",
    provider = "Provider",
    apiKey = "API Key",
    baseUrl = "Base URL",
    model = "Model",
    temperature = "Temperature",
    dictionarySection = "Dictionary",
    mwApiKey = "Merriam-Webster API Key",
    translationSection = "Translation",
    defaultMode = "Default Translation Mode",
    appearance = "Appearance",
    theme = "Theme",
    themeSystem = "Follow System",
    themeLight = "Light",
    themeDark = "Dark",
    interfaceStyle = "Interface Style",
    styleModern = "Modern",
    styleEditorial = "Editorial",
    language = "Language",
    about = "About",
    currentVersion = "Current version: ",
    checkForUpdates = "Check for Updates",
    checking = "Checking...",
    upToDate = "You're on the latest version.",
    updateFailed = "Update check failed. Please try again later.",
    saveSettings = "Save Settings",
    settingsSaved = "Settings saved",
    unsavedChangesTitle = "Discard changes?",
    unsavedChangesMessage = "You edited the settings but haven't saved yet.",
    discard = "Discard",
    keepEditing = "Keep Editing",
    show = "Show",
    hide = "Hide"
)

val ZhStrings = EnStrings.copy(
    lang = AppLanguage.CHINESE,
    tabTranslate = "翻译",
    tabHistory = "历史",
    tabLearning = "学习",
    settings = "设置",
    enterText = "输入要翻译的文本…",
    paste = "粘贴",
    clearInput = "清空输入",
    translate = "翻译",
    translating = "翻译中…",
    preparingModel = "正在准备翻译模型…",
    cancel = "取消",
    translationTitle = "译文",
    speakTranslation = "朗读译文",
    pauseTranslation = "暂停朗读",
    resumeTranslation = "继续朗读",
    noticeLlmKeyMissing = "未配置 LLM API Key，已改用离线翻译。",
    noticeLlmFailed = "LLM 翻译失败，已改用离线翻译。",
    noticeStreamInterrupted = "翻译中断，已保留部分结果。",
    emptyResult = "翻译结果将显示在这里。",
    analysis = "解析",
    keyWords = "关键词",
    copy = "复制",
    copied = "已复制",
    shareTranslation = "分享译文",
    favoriteTranslation = "收藏译文",
    synonymsPrefix = "近义词：",
    dictionary = "词典",
    englishWordHint = "英文单词…",
    lookUpWord = "查单词",
    lookUp = "查询",
    enterWordToLookup = "输入单词开始查询。",
    viewFullDefinition = "查看完整释义 →",
    playPronunciation = "播放发音",
    addToFavorites = "加入收藏",
    removeFromFavorites = "取消收藏",
    phrasalVerbs = "短语动词",
    etymology = "词源",
    noApiKey = "请在设置中配置 Merriam-Webster API Key。",
    invalidApiKey = "API Key 无效，请检查设置。",
    networkError = "网络错误，请检查网络连接。",
    parseError = "词典数据解析失败，请稍后重试。",
    wordNotFound = "未找到该单词。你是不是要找：",
    noResults = "未找到匹配的词条。",
    chineseMeaningUnavailable = "中文释义暂不可用。",
    goToSettings = "前往设置",
    copyright = "Copyright © 2026 Stafind. All rights reserved.",
    viewOnGitHub = "在 GitHub 查看",
    clearAll = "全部清空",
    clearAllTitle = "清空全部历史？",
    clearAllMessage = "历史记录将被永久删除。收藏的译文会保留。",
    clear = "清空",
    noHistory = "暂无历史记录",
    deleteRecord = "删除记录",
    recordDeleted = "已删除该记录",
    undo = "撤销",
    noFavoriteWords = "还没有收藏单词。\n查词后点击心形即可收藏！",
    noFavoriteTranslations = "还没有收藏译文。\n翻译后点击心形即可收藏！",
    favoriteWordsSection = "单词",
    favoriteTranslationsSection = "译文",
    back = "返回",
    llmProvider = "LLM 供应商",
    provider = "供应商",
    apiKey = "API Key",
    baseUrl = "Base URL",
    model = "模型",
    temperature = "Temperature",
    dictionarySection = "词典",
    mwApiKey = "Merriam-Webster API Key",
    translationSection = "翻译",
    defaultMode = "默认翻译模式",
    appearance = "外观",
    theme = "主题",
    themeSystem = "跟随系统",
    themeLight = "浅色",
    themeDark = "深色",
    interfaceStyle = "界面风格",
    styleModern = "现代",
    styleEditorial = "报刊",
    language = "语言",
    about = "关于",
    currentVersion = "当前版本：",
    checkForUpdates = "检查更新",
    checking = "检查中…",
    upToDate = "当前已是最新版本。",
    updateFailed = "检查更新失败，请稍后重试。",
    saveSettings = "保存设置",
    settingsSaved = "设置已保存",
    unsavedChangesTitle = "放弃更改？",
    unsavedChangesMessage = "你修改了设置但尚未保存。",
    discard = "放弃",
    keepEditing = "继续编辑",
    show = "显示",
    hide = "隐藏"
)

val LocalStrings = compositionLocalOf<AppStrings> { EnStrings }

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.ENGLISH -> EnStrings
    AppLanguage.CHINESE -> ZhStrings
}
