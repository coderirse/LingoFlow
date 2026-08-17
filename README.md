# LingoFlow

A modern Android translation app powered by ML Kit, Merriam-Webster Dictionary, and AI — built with Jetpack Compose.

## Features

- **Instant Translation** — ML Kit on-device translation (English ↔ Chinese ↔ Japanese ↔ Korean)
- **Dictionary Lookup** — Authoritative definitions from Merriam-Webster with phonetics, examples, and etymology
- **AI-Powered Learning** — Context-aware explanations and multiple translation styles (Natural / Concise / Formal / Learning) via LLM
- **Privacy-First** — API keys stay on your device (EncryptedSharedPreferences)

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

## License

[MIT](LICENSE) © 2026 Stafind
