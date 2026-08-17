# LingoFlow 代码审查报告（Code Review）

> **审查性质**：只读审查，未修改任何源码。
> **审查版本**：`v1.1.2`（versionCode 5），分支 `feature/streaming` @ `ba9cceb`（内容与 `main` @ `cbd3b95` 一致）。
> **审查范围**：`app/src/main` 全部 88 个 Kotlin 源文件 + `app/src/test` 30 个测试文件 + 构建/资源/清单配置。
> **审查方式**：人工通读 data / domain / di / build 层 + 两个专项审查（Compose UI 层、ViewModel/测试层）。

---

## 1. 总体结论

架构骨架（Clean Architecture + MVVM + Hilt）**健康**：分层清晰、依赖方向正确、协程作用域基本规范（统一 `viewModelScope` + `stateIn(WhileSubscribed)`）、错误用 sealed exception 映射、解析器和引擎测试覆盖良好。**未发现确定性的内存泄漏或线程崩溃**。

主要风险集中在四类：

1. **并发竞态（stale result）** —— 多处异步结果无取消/epoch 守卫，快速操作会显示错误数据。
2. **一次性事件用"值"当 `LaunchedEffect` key** —— Snackbar 等可能被吞或重复触发。
3. **两个"看似可选实则不可用"的 LLM 供应商**（Anthropic / Gemini）。
4. **安全/发布技术债** —— 备份规则未配置、`EncryptedSharedPreferences` 已弃用、R8 未启用、release 用 debug 签名。

---

## 2. 架构健康度（做得好的部分）

- 分层清晰：`ui` → `domain`（model / engine / repository / usecase）→ `data`，依赖方向单向。
- `TranslationRouter` 统一路由 STANDARD→ML Kit、其余→LLM，无 Key 时回退 ML Kit，职责清晰。
- 错误映射规范：`DictionaryException` / `LlmException` / `TranslationException` 均带用户可读信息，引擎细节不外泄。
- 协程作用域规范：ViewModels 全部使用 `viewModelScope`；`stateIn` 均带 `SharingStarted.WhileSubscribed(5_000)`。
- 解析器健壮：`MwJsonParser` 对 MW 嵌套 `sseq`、`{bc}/{wi}` 标记、音频 URL 子目录、拼写建议均有处理且有测试。
- Compose 细节正确：`LazyColumn.items` 都带 `key`；`imePadding`/`statusBarsPadding`/`navigationBarsPadding` 应用正确；`StreamingText` 的 `weight(1f, fill=false)` 正确；`BlinkingCursor` 使用 `rememberInfiniteTransition`（无 `while(true)` 轮询）。
- 依赖注入正确：AGP 9 内置 Kotlin，未添加 `org.jetbrains.kotlin.android`，符合项目约束。

---

## 3. 发现清单（按严重度）

### 🔴 P1 — 高优先级（正确性 / 竞态 / 崩溃风险）

**1. 词典查询的陈旧结果竞态（两个 ViewModel）**
- `ui/dictionary/DictionaryViewModel.kt:56-74` —— `lookUp()` 一次性 `launch` 两个协程（MW 查询 + LLM 中文释义），**无取消 / 无请求 epoch 守卫**。快速连点不同单词时，先发起的请求可能后返回，覆盖新单词的 `_uiState` 与 `_lookupInfo`，显示错误释义。
- `ui/home/HomeViewModel.kt:267-283` —— `maybeLoadWordLookup()` 同样无守卫，连续翻译两个"英文单词→中文"时，上一个词的 `wordLookup` 可能串到下一个词上。
- **修复**：为每次查询维护递增 `requestId`（或 `Job?.cancel()` 取消上一个），回写状态前校验 id 仍是当前请求。

**2. 潜在崩溃：`WordPreviewSheet.kt:87` 的 `state.entries.first()`**
- `ui/dictionary/WordPreviewSheet.kt:87` —— `Success` 分支直接 `.first()`，若 `entries` 为空抛 `NoSuchElementException`。当前 `MwJsonParser` 保证成功时非空，属"未爆发的隐患"，但把不变量从隐式约定变成显式防御更稳。
- **修复**：`firstOrNull() ?: <空状态分支>`，或先判 `state.entries.isEmpty()`。

**3. 一次性事件用"值"当 `LaunchedEffect` key**
- `ui/settings/SettingsScreen.kt:82-87` —— `LaunchedEffect(uiState.saveSuccess)` key 是 `Boolean`。
- `ui/home/HomeScreen.kt:154-159` —— `LaunchedEffect(uiState.snackbarMessage)` key 是 `String?`。
- 上一条 Snackbar 仍在显示期间又来相同消息/再次保存时，key 不变 → 事件被合并丢弃；clear-then-set 时又可能重放。
- **修复**：改用单调递增事件 id、`Channel`/`SharedFlow`，或 `SnackbarEvent(id)` 数据类。

**4. Settings 主题/语言即时保存的快照竞态**
- `ui/settings/SettingsViewModel.kt:76-89` —— `updateThemeMode`/`updateAppLanguage` 在 `launch` 内读取 `_uiState.value.settings`（非启动时快照）。快速切换主题+语言时，两个协程可能用不同时刻的 settings 互相覆盖。
- **修复**：先取 `snapshot` 再 `launch`（与 `saveSettings()` 一致）。

**5. 回退提示可能被静默丢弃**
- `data/engine/TranslationRouter.kt:36-37` + `:73-76` / `:93-95` —— `_fallbackMessages` 是 `MutableSharedFlow(extraBufferCapacity = 1)`（**无 replay**），`tryEmit` 在 buffer 满或无人订阅时返回 false 直接丢弃。"未配 API Key 走 ML Kit 回退" 的提示可能丢。
- **修复**：改 `MutableSharedFlow(replay = 1)`，或让 ViewModel 主动读取回退状态而非依赖一次性 emit。

**6. 主题/语言保存静默吞错**
- `ui/settings/SettingsViewModel.kt:76-89` —— `updateThemeMode`/`updateAppLanguage` 的 `saveSettings` **无 try/catch**（对比 `saveSettings()` 方法 `:91-101` 有 try/catch 且写 `error`），保存失败用户无感知。
- **修复**：统一走带错误处理的保存路径。

**7. 默认翻译模式的初始化竞态**
- `ui/home/HomeViewModel.kt:78-81` —— `init` 异步读 `getSettings()` 后写 `translationMode`，若用户在此完成前手动切换了模式，用户的即时选择会被默认值覆盖。
- **修复**：仅在用户尚未手动选择时应用默认值（加一个 `modeTouchedByUser` 标记）。

**8. 收藏切换乐观更新 + 无失败处理**
- `ui/home/HomeViewModel.kt:306-312` —— `onToggleFavoriteTranslation` 先本地翻转 `isCurrentFavorite`，再异步 `toggleFavorite`；写入失败时 UI 与实际不一致，且快速连点存在 toggle 竞态。
- **修复**：等待仓库结果后回写状态，或失败时回滚。

**9. 取消流式后 `translatedText` 为空**
- `ui/home/HomeViewModel.kt:213-217` —— 取消流式时保留了部分 `streamingText` 在屏上，但 `translationResponse` 从未赋值，`translatedText` 访问器返回 `""`（复制/分享拿不到已有部分文本）。
- **修复**：明确"取消后保留部分文本"的预期，若保留则同步设置一个 `Standard` 响应供复制/分享。

---

### 🟠 P2 — 中优先级（安全 / 功能缺陷 / 技术债）

**10. 备份规则是未改动的"样例文件"，API Key 会被云备份（安全）**
- `app/src/main/res/xml/backup_rules.xml`、`data_extraction_rules.xml` 仍是 Android Studio 生成的空白样例（含 `<!-- Sample backup rules file... -->` 注释），而 `AndroidManifest.xml` 里 `allowBackup="true"`。
- 后果：`EncryptedSharedPreferences`（`lingoflow_secrets`，含所有 LLM + MW API Key）被云备份；换机恢复时 **Keystore 主密钥不迁移**，导致解密失败/数据损坏，也是隐私问题。
- **修复**：在 `data_extraction_rules.xml` 明确 `<exclude domain="sharedpref" path="lingoflow_secrets.xml"/>`，DataStore 的 `lingoflow_settings.preferences_pb` 按需决定。

**11. `EncryptedSharedPreferences` / `MasterKeys` 已弃用**
- `di/SettingsModule.kt:44-51` —— 使用 `MasterKeys.getOrCreate(AES256_GCM_SPEC)` + `EncryptedSharedPreferences`（security-crypto 停留在 1.0.0，Google 已弃用整套 API）。
- **修复**：长期迁移到 Keystore 加密的 DataStore 或新版 Keystore 封装。

**12. Anthropic / Gemini 供应商实际不可用（UX 陷阱）**
- `domain/model/llm/LlmProviderId.kt:11-12` —— 默认 `defaultBaseUrl` 均非 OpenAI 兼容端点（Anthropic 需 `/v1/messages` + `x-api-key`；Gemini 的 OpenAI 网关是 `/v1beta/openai/`）。`OpenAiCompatibleProvider` 只会 POST `{baseUrl}/chat/completions`，选中并填 Key 后必定失败。
- **修复**：实现独立适配器，或在 UI 禁用/隐藏这两个选项。

**13. 共享 `OkHttpClient` 未配置超时**
- `di/DictionaryModule.kt:20` —— 直接 `OkHttpClient()`（默认 connect/read 均 10s），LLM 翻译与 SSE 流式在慢响应时可能超时中断。
- **修复**：显式 `connectTimeout` / `readTimeout`（流式 readTimeout 建议 30s+）/ `writeTimeout`，可选加日志/重试拦截器。

**14. TTS 就绪状态不响应式**
- `data/tts/AndroidTtsEngine.kt` —— `isReady` 是 `@Volatile` 布尔，非 Flow；`DictionaryViewModel.kt:54` 的 `ttsReady` 是懒 getter，不会触发重组；`HomeViewModel` 仅在 `init` 与翻译完成时快照一次。TTS 异步初始化完成后，播放按钮 `enabled` 停留旧值。
- **修复**：`isReady` 改 `StateFlow<Boolean>`（初始化回调里 set），UI 直接收集。

**15. 死代码（应清理）**

| 文件 | 状态 |
|---|---|
| `domain/model/dictionary/Collocation.kt` | 定义后从未被引用 |
| `domain/repository/TranslationRepository.kt` + `data/repository/TranslationRepositoryImpl.kt` | 在 `TranslationModule` 绑定但从未被注入 |
| `data/translator/FakeTranslator.kt` | Prompt 2 遗留，`@Inject @Singleton` 但未绑定，应移入 test 源集 |
| `ui/theme/Color.kt` 的 `Purple80/PurpleGrey80/Pink80/Purple40/PurpleGrey40/Pink40` | 模板默认色，未使用 |
| `app/src/main/keepRules/rules.keep` | 孤儿文件，build 实际用 `app/proguard-rules.pro` |

**16. 其他中/低问题**
- `data/repository/SettingsRepositoryImpl.kt:60-66` —— `observeSettings` 的 map 可能在主线程同步读 6 个 provider 的加密 key，有卡顿风险。
- `data/repository/SettingsRepositoryImpl.kt:85` —— `config.baseUrl?.let { prefs[...] = it }` 只在非空时写，用户在设置里清空 Base URL 后旧值残留（**无法清空**）。
- `data/engine/TranslationRouter.kt:84` —— `translateStream` 无模式守卫，直接委托 `llmEngine.translateStream`，其 `require()` 对 STANDARD/LEARNING 抛原始 `IllegalArgumentException`（当前靠 ViewModel 层守卫，属防御缺口）。
- `data/engine/LlmTranslationEngine.kt:174` —— LEARNING 模式恒返回空 `dictionaryEntries`，HomeScreen 的 "Key Words" 区块实际为空（LLM 现只返回 synonyms）。要么删区块，要么让 LLM 返回结构化词汇。

---

### 🟡 P3 — 低优先级（UI 质量 / 可维护性 / 测试）

**UI 层**
- `ui/home/HomeScreen.kt`（1075 行）god-composable：`TranslationResultCard`（228 行，4 分支）、copy/share/paste 回调、两阶段查词 overlay 应拆成独立 composable。
- `ui/home/HomeScreen.kt:950-964` —— `ClickableWords` 每次重组新建 `Regex("\\s+")` + 分割列表；把 Regex 提到顶层 `private val`，用 `remember(text)` 缓存 tokens。
- 硬编码颜色：`Color(0xFF00BCD4)` 在 `DictionaryBottomSheet.kt:192` 与 `WordPreviewSheet.kt:153` 重复（等价 `LingoFlowSecondary`）；且全文大量直接引用 `LingoFlowPrimary/Secondary` 而非 `MaterialTheme.colorScheme.*`。
- 浅色主题层级塌陷：`Color.kt` 里 `LingoFlowLightSurface == LingoFlowLightSurfaceElevated == 纯白`，卡片与背景对比度弱。
- i18n 遗漏：`HomeScreen.kt:534` 硬编码 `"Swap languages"`（无 `swapLanguages` key）、`:1082` `"GitHub"`、`SettingsScreen.kt:541` `"Custom"`；`DictionaryBottomSheet.kt:96` 复用 history 的 `strings.clear`（"清空"）表达"清除输入框"，语义错误。
- `AppStrings`：`ZhStrings = EnStrings.copy()` 脆弱（新增字符串静默回退英文）；`newVersionAvailable` 用 `this == ZhStrings` 引用相等判断。
- 无障碍：`HistoryScreen.kt:215` 删除按钮 32dp 触控目标（<48dp），`DictionaryBottomSheet.kt:207` 播放按钮同理。
- `HomeScreen.kt:162` —— `fullLookupWord` 用 `remember` 而非 `rememberSaveable`，配置变更丢失。

**测试层**
- `SettingsRepositoryImpl` **零测试覆盖**（最高覆盖缺口）；`MlKitTranslator`、`AndroidTtsEngine`、`UpdateChecker.checkLatestRelease` 亦零覆盖。
- `LookupWordUseCaseTest.kt:81-94` —— 缓存测试从未断言调用次数（`FakeLlmProvider` 无调用计数器），"命中缓存不二次调用"未被验证。
- `ExampleUnitTest.kt:13-16` —— 占位测试 `assertEquals(4, 2+2)`。
- `InflectionStemmerTest.kt:39-44` —— `A || B` 断言近乎同义反复。
- `LlmProviderIdTest.kt:16-19` —— 硬编码枚举数 `assertEquals(6, ...)`，增删供应商会因错误原因失败。
- `FavoritesRepositoryTest.kt` / `HistoryRepositoryTest.kt` —— DataStore 用解耦且从不 cancel 的 `TestScope(UnconfinedTestDispatcher)` + 真实临时文件，资源泄漏、测试污染、持久化时序不确定。
- `DictionaryRepositoryImplTest.kt:72,94` / `OpenAiCompatibleProviderTest.kt:72` —— MockWebServer `takeRequest(timeout)` 用真实时钟，CI 高负载下 flaky。
- `MainDispatcherRule.kt:14-16` —— 用独立 `StandardTestDispatcher` scheduler（与 `runTest` 不同虚拟时钟），迫使每个测试手动 `advanceUntilIdle()`。
- `HomeViewModelTest.kt:314-380` —— 流式测试靠 `delay(60_000)` 虚拟阻塞，脆弱，建议改 `CompletableDeferred` 门控。

---

## 4. 测试覆盖缺口表

| 生产类 | 测试状态 |
|---|---|
| `MwJsonParser` | ✅ 好（义项/例句/词源/音频子目录/建议列表全覆盖） |
| `VersionCompare` | ✅ 全（`VersionCompareTest`） |
| `HistoryRepositoryImpl` | ✅ 行为全（增删/清空/50 上限/toggleFavorite）；缺：坏 JSON 恢复、跨实例持久化 |
| `FavoritesRepositoryImpl` | ✅ 行为全（增删/去重/空词）；缺：跨实例持久化、坏状态 |
| `TranslationRouter` | ⚠️ 部分；缺：`_status` 镜像逻辑、回退分支错误传播 |
| `LlmTranslationEngine` | ⚠️ 部分；缺：CONCISE/FORMAL prompt、错误映射、流式错误传播 |
| `OpenAiCompatibleProvider` | ⚠️ 部分；缺：403/400/500 映射、ParseError、流式取消/中途解析错误 |
| `TranslateTextUseCase` | ⚠️ 好/空白错误覆盖；缺：`translateStream` 的 null 分支 |
| `LookupWordUseCase` | ⚠️ 好；缺：64 条缓存淘汰、非对象根拒绝 |
| `MlKitTranslationEngine` | ⚠️ 仅间接（经 UseCase/Router 测试）；无独立测试 |
| `SettingsRepositoryImpl` | ❌ **零覆盖** |
| `MlKitTranslator` | ❌ **零覆盖**（仅用 Fake 间接 mock） |
| `AndroidTtsEngine` | ❌ **零覆盖** |
| `UpdateChecker.checkLatestRelease` | ❌ **零覆盖**（`RELEASES_API_URL` 硬编码无法注入） |

---

## 5. 已验证正确（无需改动）

- 协程作用域规范：全 `viewModelScope`；`stateIn` 均带 `WhileSubscribed(5_000)`。
- `BlinkingCursor.kt` 无 `while(true)`/`isActive` 循环，正确使用 `rememberInfiniteTransition`。
- `HistoryScreen`/`LearningScreen`/`DictionaryBottomSheet` 的 `LazyColumn.items` 都带 `key`。
- `imePadding`/`statusBarsPadding`/`navigationBarsPadding` 应用正确；`StreamingText` 的 `weight(1f, fill=false)` 正确。
- `MwJsonParser` 覆盖充分；`VersionCompare` 语义正确（`1.10.0 > 1.9.0` 成立）。
- 依赖注入方向正确，无 `org.jetbrains.kotlin.android`，符合项目约束。

---

## 6. 建议优化路线（按投入产出排序）

**阶段 A — 本阶段就修（低风险高价值）**
1. 两处竞态加 requestId / 取消守卫（#1、#4）。
2. 一次性事件改 Channel / 事件 id（#3）。
3. 备份规则排除加密存储（#10，安全项，近乎零成本）。
4. `WordPreviewSheet` 的 `.first()` 改 `firstOrNull()`（#2）。
5. 补 `SettingsRepositoryImpl` 单测（覆盖最大空白）。

**阶段 B — 下个版本（产品正确性）**
1. 禁用/隐藏 Anthropic、Gemini，或实现独立适配器（#12）。
2. 配置 OkHttp 超时（#13）；TTS `isReady` 改 `StateFlow`（#14）。
3. 清理死代码（#15），`FakeTranslator` 移入 test 源集。

**阶段 C — 技术债 backlog**
1. 迁移弃用的 `EncryptedSharedPreferences`（#11）。
2. 拆分 `HomeScreen.kt`、统一颜色 token、补齐 i18n key、修浅色主题层级。
3. 修测试 flaky / 伪断言，接入 CI（当前无 CI 配置）。
4. 若上架 Play：启用 R8 + 独立 release keystore（当前 `optimization.enable=false`、debug 签名，仅适合侧载）。

---

*本报告为只读审查产物。所有行号基于 `v1.1.2`（`feature/streaming` @ `ba9cceb`），后续代码变动后行号可能偏移。*
