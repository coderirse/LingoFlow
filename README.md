# LingoFlow

A modern Android translation app powered by ML Kit, Merriam-Webster Dictionary, and AI — built with Jetpack Compose.

## Features

- **Instant Translation** — ML Kit on-device translation (English ↔ Chinese ↔ Japanese ↔ Korean); long texts stream through the LLM with automatic layout formatting (paragraphs & lists) and can be cancelled at any time
- **Dictionary Lookup** — Authoritative definitions from Merriam-Webster with phonetics, examples, and etymology; tap any word in a translation to look it up
- **AI-Powered Learning** — Context-aware explanations and multiple translation styles (Natural / Concise / Formal / Learning) via LLM, with live streaming output
- **Listen & Pause** — Text-to-speech for translations with play/pause state; long passages are read sentence by sentence
- **Privacy-First** — API keys stay on your device (EncryptedSharedPreferences)

## Download

Grab the latest APK from [Releases](https://github.com/coderirse/LingoFlow/releases).

## Tech Stack

- Kotlin + Coroutines + Flow
- Jetpack Compose + Material 3
- Hilt (DI)
- ML Kit Translation
- OkHttp + kotlinx.serialization
- DataStore + EncryptedSharedPreferences

## Build

```bash
./gradlew :app:assembleDebug
```

### Release Build

1. 将 `lingoflow-release.jks` 放置于 `app/` 目录下
2. 在项目根目录 `local.properties`（已被 gitignore）中添加：
   ```
   RELEASE_STORE_PASSWORD=your_keystore_password
   RELEASE_KEY_ALIAS=lingoflow
   RELEASE_KEY_PASSWORD=your_key_password
   ```
3. 执行 `./gradlew :app:assembleRelease`（未配置密钥时自动回退 debug 签名，供 CI/侧载使用）

## License

[MIT](LICENSE) © 2026 Stafind
