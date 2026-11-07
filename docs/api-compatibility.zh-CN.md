# 原生 SDK 与 KMP 接口差异说明

[英文文档](api-compatibility.md) · [项目介绍](../README.md) · [使用指南](usage.md) · [已知限制](known-limitations.md)

本文帮助使用者判断：原生 iOS、Android SDK 提供哪些接口，当前 KMP 是否提供对应接口，以及使用 KMP 时需要注意的差异。

原始审计基准：Android Analytics／Core **1.22.4**、Amplitude-Swift **1.16.2**。
下表及原生源码引用保留该审计基准，不代表已发布 Maven 制品的全部行为。

当前仓库已升级至 Android Analytics／Core **1.23.1**，iOS 依赖约束为 **`~> 1.16.6`**
（`>= 1.16.6, < 1.17.0`）。针对这两个目标版本的源码比较未发现当前封装所调用入口需要调整，
但不代表已对所有原生 API 重新完成全面审计。实际 iOS 及传递依赖版本以使用方的 CocoaPods 锁文件为准。

本次验证（2026-11-07）：Android 编译及现有 5 项主机单元测试通过；iOS 因环境中找不到 CocoaPods，
尚未完成依赖解析、编译及链接验证。当前工作区没有 Pod 锁文件，历史 iOS 构建记录不能视为本次升级验证结果。

审计后更新：当前仓库已删除 KMP 自定义协程便利接口及 Android 初始化 await 便利接口，本文相关部分已同步更新。
当前仓库也已修复 iOS Revenue 签名映射及 64 位时长映射，并补充 iOS 映射回归测试。
Android 已修复 Revenue 收据／签名独立赋值映射及回调公共字段遗漏；iOS 回调属性字典的桥接限制保持不变。

表格以公开函数、属性和枚举项为单位。签名省略部分默认值、修饰符和参数标签，保留与差异有关的参数类型及返回值。
各列中的 `Amplitude`、`Configuration`、`BaseEvent` 等名称均指该列所属 SDK 的类型，不代表跨平台可直接互换。
Android 列也包含从 Core 继承的接口；其中部分方法实际返回 `com.amplitude.core.Amplitude`。
iOS 列主要列出 Swift 公开接口；影响 KMP 使用的 Objective-C 桥接差异会另行注明。

“等价封装”指该接口在说明范围内提供对应行为，不代表整个 SDK 完全兼容。返回事件、插件处理和配置时序等限制见对应表格。
“—”表示没有对应的公开接口，不代表底层平台完全无法实现相关功能。

## 1. 初始化与实例接口

| 接口／属性 | iOS 原生接口 | Android 原生接口 | KMP 接口 | 等价情况与差异 |
|---|---|---|---|---|
| `Amplitude(configuration)` | `init(configuration: Configuration)` | `Amplitude(configuration: Configuration)` | `Amplitude(configuration: Configuration)` | **部分等价**：KMP 将配置转换为独立原生对象；iOS 的初始 deviceId／sessionId 通过构造后的实例操作设置，见配置表。 |
| `Amplitude(apiKey, context, configs)` | — | `Amplitude(apiKey: String, context: Context, configs: Configuration.() -> Unit)` | 同名形式，仅 `androidMain` 可用 | **仅 Android 提供**；不能在 `commonMain` 使用这个重载。 |
| `Amplitude(apiKey, configs)` | —，使用 `init(configuration:)` | Android 入口需要 Context；另有 JVM Core 工厂函数 | `Amplitude(apiKey: String, configs: Configuration.() -> Unit)`，仅 `iosMain` 可用 | **KMP 便利接口**，不是 Swift 原生的同名 DSL；不能在 `commonMain` 使用。 |
| `Amplitude.isBuilt` | — | `isBuilt: Deferred<Boolean>` | 不直接暴露 | **未等价包装**；仅 Android 的 `isInitialized()` 可检查任务是否结束，KMP 不提供初始化完成等待接口，公共代码也没有同等入口。 |
| 初始化完成等待 | — | `isBuilt: Deferred<Boolean>`，可调用 `await()` | — | **未包装原生异步任务**；已删除 KMP 自定义 `invokeAndAwait()` 和 `awaitInitialization()`，使用 `Amplitude(configuration)` 构造实例，不额外保证初始化已完成。 |
| `Amplitude.isInitialized()` | — | `isBuilt.isCompleted` | `isInitialized(): Boolean`，仅 Android | **部分等价**：只表示初始化任务已经结束，不单独保证成功。 |

两端原生 SDK 与 KMP 均允许创建多个实例。使用者应保存 `Amplitude(configuration)` 的返回值，
通过参数或依赖注入传递；需要区分多个实例时设置不同的 `instanceName`。KMP 不提供全局实例访问入口。

## 2. 事件、Identify、Revenue 与分组接口

| 接口／属性 | iOS 原生接口 | Android 原生接口 | KMP 接口 | 等价情况与差异 |
|---|---|---|---|---|
| `Amplitude.track(eventType, eventProperties, options)` | `track(eventType: String, eventProperties: [String: Any]?, options: EventOptions?) -> Amplitude` | `track(eventType: String, eventProperties: Map<String, Any?>?, options: EventOptions?): Amplitude` | 对应 Kotlin 签名 | **基本等价**：上报事件及属性。iOS 包装层会先读取当前 userId／deviceId，再构造并转发事件；属性值仍受各端序列化支持范围限制。 |
| `Amplitude.track(event, options, callback)` | `track(event: BaseEvent, options: EventOptions?, callback: EventCallback?) -> Amplitude` | `track(event: BaseEvent, options: EventOptions?, callback: EventCallBack?): Amplitude` | 对应 Kotlin 签名 | **部分等价**：原生对象由 KMP 转换创建，SDK 后续修改不会写回输入的 KMP 对象；回调事件字段不完整，见回调表。 |
| `Amplitude.identify(userProperties, options)` | `identify(userProperties: [String: Any]?, options: EventOptions?) -> Amplitude` | `identify(userProperties: Map<String, Any?>?, options: EventOptions?): Amplitude` | 对应 Kotlin 签名 | **已包装**：iOS 通过 `AMPIdentify` 转发；Android 原生跳过值为 null 的 Map 项，跨端属性值转换不应视为完全相同。 |
| `Amplitude.identify(identify, options)` | `identify(identify: Identify, options: EventOptions?) -> Amplitude` | `identify(identify: Identify, options: EventOptions?): Amplitude` | 对应 Kotlin 签名 | **等价封装基本操作**：将 KMP Identify 转为原生构建器，再调用原生 identify。自定义插件和任意属性值仍有后文限制。 |
| `Amplitude.revenue(revenue, options)` | `revenue(revenue: Revenue, options: EventOptions?) -> Amplitude` | `revenue(revenue: Revenue, options: EventOptions?): Amplitude` | 对应 Kotlin 签名 | **已修复收据映射**：两端均独立传递 receipt／receiptSig，使用原生 price 校验；原生验证规则及插件限制仍适用，见 Revenue 表。 |
| `Amplitude.revenue(event)` | `revenue(event: RevenueEvent) -> Amplitude`；OC 无对应类型化重载 | `revenue(event: RevenueEvent): Amplitude` | `revenue(event: RevenueEvent): Amplitude` | **部分等价**：两端包装层都转换为原生 BaseEvent 并调用 track，不能保留原生 RevenueEvent 子类身份及对应的类型化插件分发。 |
| `Amplitude.setGroup(groupType, groupName, options)` | `setGroup(groupType: String, groupName: String, options: EventOptions?) -> Amplitude` | `setGroup(groupType: String, groupName: String, options: EventOptions?): Amplitude` | 对应 Kotlin 签名 | **等价封装单值分组**；自定义插件仍可能影响生成的事件。 |
| `Amplitude.setGroup(groupType, groupNames, options)` | Swift：`setGroup(groupType: String, groupName: [String], options: EventOptions?)`；OC 参数名为 `groupNames` | `setGroup(groupType: String, groupName: Array<String>, options: EventOptions?): Amplitude` | `setGroup(groupType: String, groupName: Array<String>, options: EventOptions?): Amplitude` | **等价封装多值传递**：完整数组一次传给原生，不是逐项调用单值 setter。 |
| `Amplitude.groupIdentify(groupType, groupName, groupProperties, options)` | `groupIdentify(groupType: String, groupName: String, groupProperties: [String: Any]?, options: EventOptions?)` | 对应 `Map<String, Any?>?` 重载 | 对应 Kotlin 签名 | **已包装**：iOS 在包装层构造 AMPIdentify，再调用 OC 接口。 |
| `Amplitude.groupIdentify(groupType, groupName, identify, options)` | `groupIdentify(groupType: String, groupName: String, identify: Identify, options: EventOptions?)` | 对应 Identify 重载 | 对应 Kotlin 签名 | **等价封装基本操作**；属性转换与插件限制仍适用。 |
| `Amplitude.logEvent(event)` | 已弃用，推荐 `track(event:)` | 已弃用，推荐 `track(event)` | — | **不提供弃用别名**，使用 `track()`。 |
| `Amplitude.logRevenue(...)` | 有已弃用接口，原生签名与 Android 不同 | 已弃用，推荐 `revenue()` | — | **不提供弃用别名**，使用 `revenue()`。 |

## 3. 身份、会话与刷新接口

| 接口／属性 | iOS 原生接口 | Android 原生接口 | KMP 接口 | 等价情况与差异 |
|---|---|---|---|---|
| `Amplitude.setUserId(userId)` | `setUserId(userId: String?) -> Amplitude` | `setUserId(userId: String?): Amplitude` | `setUserId(userId: String?): Amplitude` | **等价转发**：支持 null；Android 原生更新异步执行，方法返回不代表更新完成。 |
| `Amplitude.getUserId()` | `getUserId() -> String?` | `getUserId(): String?` | `getUserId(): String?` | **等价读取**；Android 初始化或更新未完成时，读取结果受原生时序影响。 |
| `Amplitude.setDeviceId(deviceId)` | `setDeviceId(deviceId: String?) -> Amplitude` | `setDeviceId(deviceId: String): Amplitude` | `setDeviceId(deviceId: String): Amplitude` | **部分等价**：KMP 不接受 null，未提供 iOS 原生可空 setter 的全部语义；Android 更新为异步操作。 |
| `Amplitude.getDeviceId()` | `getDeviceId() -> String?` | `getDeviceId(): String?` | `getDeviceId(): String?` | **等价读取**，不额外等待原生初始化或更新。 |
| `Amplitude.reset()` | `reset() -> Amplitude` | `reset(): Amplitude` | `reset(): Amplitude` | **等价转发**，但两端原生重置策略不同；例如 Swift 清理用户属性并通知原生插件，Android 依据自身设备 ID 策略重置。 |
| `Amplitude.sessionId` | `sessionId: Int64` | `sessionId: Long` | `sessionId: Long` | **等价读取**，KMP 为只读属性。 |
| `Amplitude.getSessionId()` | `getSessionId() -> Int64` | 无独立方法，读取 `sessionId` | 无独立方法，读取 `sessionId` | **以属性形式提供对应读取能力**，不是同名函数。 |
| `Amplitude.setSessionId(timestamp)` | `setSessionId(timestamp: Int64) -> Amplitude`；OC 为 `setSessionIdWithTimestamp` | 无对应公开实例方法 | — | **未包装**：不能在运行时通过 KMP 调用原生会话切换；初始化 `Configuration.sessionId` 不能替代该接口。 |
| `Amplitude.setSessionId(date)` | `setSessionId(date: Date) -> Amplitude` | — | — | **未包装**。 |
| `Amplitude.onEnterForeground(timestamp)` | 非公开 SDK 方法，由原生生命周期处理 | `onEnterForeground(timestamp: Long)`，需要 GuardedAmplitudeFeature opt-in | `onEnterForeground(timestamp: Long)` | **仅 Android 有效**；iOS 调用不执行操作。 |
| `Amplitude.onExitForeground(timestamp)` | 非公开 SDK 方法，由原生生命周期处理 | `onExitForeground(timestamp: Long)`，需要 GuardedAmplitudeFeature opt-in | `onExitForeground(timestamp: Long)` | **仅 Android 有效**；iOS 调用不执行操作。 |
| `Amplitude.flush()` | `flush() -> Amplitude` | `flush(): Unit` | `flush(): Unit` | **行为对应、返回值不完全等价**：均请求后台刷新，不等待全部上传完成；KMP 不保留 Swift 的链式返回值。 |

## 4. 配置属性

以下表格说明**初始化时**的配置能力。KMP `Configuration` 不是原生配置对象的实时引用：
构造 SDK 后再修改 `config.optOut`、`config.offline` 等值，不会同步修改原生 SDK。
原生实例配置访问的差异另见第 9 节。

| 接口／属性 | iOS 原生接口 | Android 原生接口 | KMP 接口 | 等价情况与差异 |
|---|---|---|---|---|
| `Configuration.apiKey` | `apiKey: String`，构造参数 | `apiKey: String` | `apiKey: String` | **等价传递初始化值**。 |
| `Configuration.instanceName` | `instanceName: String` | `instanceName: String` | `instanceName: String` | **等价传递初始化值**，实例名标准化等行为由原生 SDK 决定。 |
| `Configuration.flushQueueSize` | `flushQueueSize: Int` | `flushQueueSize: Int` | `flushQueueSize: Int` | **等价映射**。 |
| `Configuration.flushIntervalMillis` | `flushIntervalMillis: Int` | `flushIntervalMillis: Int` | `flushIntervalMillis: Int` | **等价映射 KMP 可表示范围内的值**；原生 Swift Int 为 64 位，Kotlin Int 为 32 位。 |
| `Configuration.flushMaxRetries` | `flushMaxRetries: Int`，默认 6 | `flushMaxRetries: Int`，默认 5 | `flushMaxRetries: Int`，默认 5 | **部分等价**：KMP 统一使用 5，不沿用 Swift 默认值 6。 |
| `Configuration.optOut` | `optOut: Bool` | `optOut: Boolean` | `optOut: Boolean` | **等价映射初始值**；KMP 配置对象不是运行时 opt-out 控制接口。 |
| `Configuration.logLevel` | `logLevel: LogLevelEnum` | 无同名配置属性，原生日志器使用 `Logger.logMode` | `logLevel: LogLevel` | **等价映射基本日志等级**；KMP LOG 对应 Android INFO，VERBOSE 在两端均按 DEBUG 处理。KMP 默认 WARN，Android 原生日志器默认 INFO。 |
| `Configuration.minIdLength` | `minIdLength: Int?` | `minIdLength: Int?` | `minIdLength: Int?` | **等价映射 KMP 可表示范围内的值**。 |
| `Configuration.partnerId` | `partnerId: String?` | `partnerId: String?` | `partnerId: String?` | **等价映射**。 |
| `Configuration.callback` | `callback: EventCallback?` | `callback: EventCallBack?` | `callback: EventCallBack?` | **部分等价**：收到的事件字段不完整，见回调表。 |
| `Configuration.serverZone` | `serverZone: ServerZone` | `serverZone: ServerZone` | `serverZone: ServerZone` | **已映射 US／EU**；KMP 的 iOS 路径通过 OC 配置属性设置。 |
| `Configuration.serverUrl` | `serverUrl: String?` | `serverUrl: String?` | `serverUrl: String?` | **等价映射**。 |
| `Configuration.useBatch` | `useBatch: Bool` | `useBatch: Boolean` | `useBatch: Boolean` | **等价映射**。 |
| `Configuration.plan` | `plan: Plan?` | `plan: Plan?` | `plan: Plan?` | **等价映射模型字段**；插件和回调中的字段保留情况见对应表格。 |
| `Configuration.ingestionMetadata` | `ingestionMetadata: IngestionMetadata?` | `ingestionMetadata: IngestionMetadata?` | `ingestionMetadata: IngestionMetadata?` | **等价映射模型字段**；插件和回调中的字段保留情况见对应表格。 |
| `Configuration.minTimeBetweenSessionsMillis` | `minTimeBetweenSessionsMillis: Int`，64 位 | `minTimeBetweenSessionsMillis: Long` | `minTimeBetweenSessionsMillis: Long` | **映射已修复**：iOS 直接传递 64 位值，不再经由 32 位 Int 截断。 |
| `Configuration.identifyBatchIntervalMillis` | `identifyBatchIntervalMillis: Int`，64 位 | `identifyBatchIntervalMillis: Long` | `identifyBatchIntervalMillis: Long` | **映射已修复**：iOS 直接传递 64 位值，不再经由 32 位 Int 截断。 |
| `Configuration.flushEventsOnClose` | `flushEventsOnClose: Bool` | `flushEventsOnClose: Boolean` | `flushEventsOnClose: Boolean` | **等价映射开关**；不表示进程退出前一定上传成功。 |
| `Configuration.offline` | `offline: Bool?` | `offline: Boolean?` | `offline: Boolean` | **部分等价**：不能传 null／nil，因此无法表达原生“禁用网络连通性检查器”的第三种模式。 |
| `Configuration.deviceId` | 无同名初始化参数，使用实例 `setDeviceId()` | `deviceId: String?` 初始化参数 | `deviceId: String?` | **部分等价**：Android 初始化时传入；iOS 构造原生实例后设置，早期自动事件可能先于该操作。 |
| `Configuration.sessionId` | 无同名初始化参数，使用实例 `setSessionId(timestamp:)` | `sessionId: Long?` 初始化参数 | `sessionId: Long?` | **部分等价**：iOS 通过异步会话操作应用，可能产生会话事件，并非同步赋初值。 |
| `Configuration.autocapture` | `autocapture: AutocaptureOptions` | `autocapture: Set<AutocaptureOption>` | `autocapture: Set<AutocaptureOption>` | **部分等价**：各枚举项支持范围见第 5 节。 |
| `Configuration.trackingOptions` | `trackingOptions: TrackingOptions` | `trackingOptions: TrackingOptions` | `trackingOptions: TrackingOptions` | **部分等价**：各字段控制项见第 6 节。 |
| `Configuration.enableCoppaControl` | `enableCoppaControl: Bool` | `enableCoppaControl: Boolean` | `enableCoppaControl: Boolean` | **等价转发原生开关**；实际禁用字段取决于各端原生实现，不等于合规保证。 |
| `Configuration.interactionsOptions` | `InteractionsOptions(rageClick, deadClick)` | `InteractionsOptions(rageClick, deadClick)` | `InteractionsOptions(rageClick, deadClick)` | **等价映射两类 enabled 开关**，交互检测本身由各端原生 SDK 实现。 |
| `Configuration.useAdvertisingIdForDeviceId` | — | `useAdvertisingIdForDeviceId: Boolean` | 同名 Boolean 属性 | **仅 Android 有效**；iOS 忽略。 |
| `Configuration.useAppSetIdForDeviceId` | — | `useAppSetIdForDeviceId: Boolean` | 同名 Boolean 属性 | **仅 Android 有效**；iOS 忽略。 |
| `Configuration.newDeviceIdPerInstall` | — | `newDeviceIdPerInstall: Boolean` | 同名 Boolean 属性 | **仅 Android 有效**；iOS 忽略。 |
| `Configuration.locationListening` | — | `locationListening: Boolean` | 同名 Boolean 属性 | **仅 Android 有效**；iOS 忽略。 |
| `Configuration.migrateLegacyData` | `migrateLegacyData: Bool` | `migrateLegacyData: Boolean` | `migrateLegacyData: Boolean` | **两端均映射**，不是仅 Android 生效的参数。 |
| `Configuration.storageProvider` | `storageProvider: Storage`，Swift 初始化可注入 | `storageProvider: StorageProvider` | — | **未包装**，不能通过公共 KMP 配置替换事件存储实现。 |
| `Configuration.identifyStorageProvider` | `identifyStorageProvider: Storage` | 对应 `identifyInterceptStorageProvider: StorageProvider` | — | **未包装**。 |
| `Configuration.identityStorageProvider` | 无同名独立配置参数 | `identityStorageProvider: IdentityStorageProvider` | — | **未包装 Android 接口**。 |
| `Configuration.loggerProvider` | Swift logger provider；OC 支持回调形式 | `loggerProvider: LoggerProvider` | — | **未包装**；设置 logLevel 不能替代注入自定义日志器。 |
| `Configuration.httpClient` | 无对应初始化选项 | `httpClient: HttpClientInterface?` | — | **未包装 Android 接口**。 |
| `Configuration.networkTrackingOptions` | `networkTrackingOptions: NetworkTrackingOptions`，OC 也提供 | 无对应自动采集配置入口 | — | **未包装 iOS 接口**，包括采集规则和排除域名。 |
| `Configuration.maxQueuedEventCount` | `maxQueuedEventCount: Int`，Swift 提供 | — | — | **未包装 iOS 接口**；对应 OC 配置也未提供该属性。 |
| `Configuration.enableAutoCaptureRemoteConfig` | 同名 Bool 选项，OC 有对应初始化参数 | 无对应同名配置开关 | — | **未包装 iOS 接口**，无法通过 KMP 配置该开关。 |
| `Configuration.enableDiagnostics` | `enableDiagnostics: Bool`，Swift 提供 | 无对应同名配置开关 | — | **未包装 iOS 接口**；对应 OC 配置也未提供该属性。 |
| `Configuration.trackingSessionEvents` | 无同名属性，旧配置通过 `defaultTracking.sessions` 表达 | 已弃用的 Boolean 属性／构造参数 | — | **不提供旧接口**，使用 `autocapture` 中是否包含 SESSIONS 控制会话事件。 |
| `Configuration.defaultTracking` | 已弃用的 `DefaultTrackingOptions` | 已弃用的 `DefaultTrackingOptions` | — | **不提供旧接口及该类型**，使用 `autocapture`。 |
| `Configuration.isValid()` | 非公开方法 | `isValid(): Boolean` | — | **未包装公开校验方法**；原生初始化仍会进行自身校验。 |
| `Configuration.getApiHost()` | 无对应同名公开方法 | `getApiHost(): String` | — | **未包装 Android 辅助方法**。 |

## 5. 自动采集枚举与辅助接口

| 接口／枚举项 | iOS 原生接口 | Android 原生接口 | KMP 接口 | 等价情况与差异 |
|---|---|---|---|---|
| `AutocaptureOption.SESSIONS` | `AutocaptureOptions.sessions` | `AutocaptureOption.SESSIONS` | `AutocaptureOption.SESSIONS` | **两端均映射**。 |
| `AutocaptureOption.APP_LIFECYCLES` | `AutocaptureOptions.appLifecycles` | `AutocaptureOption.APP_LIFECYCLES` | `AutocaptureOption.APP_LIFECYCLES` | **两端均映射**，具体生命周期事件由原生 SDK 决定。 |
| `AutocaptureOption.SCREEN_VIEWS` | `AutocaptureOptions.screenViews` | `AutocaptureOption.SCREEN_VIEWS` | `AutocaptureOption.SCREEN_VIEWS` | **两端均映射**，原生页面／视图识别机制不同。 |
| `AutocaptureOption.ELEMENT_INTERACTIONS` | `AutocaptureOptions.elementInteractions` | `AutocaptureOption.ELEMENT_INTERACTIONS` | `AutocaptureOption.ELEMENT_INTERACTIONS` | **两端均映射**，UI 交互识别范围由原生实现决定。 |
| `AutocaptureOption.FRUSTRATION_INTERACTIONS` | `AutocaptureOptions.frustrationInteractions` | `AutocaptureOption.FRUSTRATION_INTERACTIONS` | `AutocaptureOption.FRUSTRATION_INTERACTIONS` | **两端均映射**。 |
| `AutocaptureOption.DEEP_LINKS` | — | `AutocaptureOption.DEEP_LINKS` | `AutocaptureOption.DEEP_LINKS` | **仅 Android 有效**；iOS 没有对应自动采集开关，该选项被忽略。 |
| `AutocaptureOptions.networkTracking` | `AutocaptureOptions.networkTracking` | 无对应枚举项 | — | **未包装 iOS 选项**。 |
| `AutocaptureOption.ALL` | `AutocaptureOptions.all`，包含 networkTracking | `AutocaptureOption.ALL` | `AutocaptureOption.ALL` | **不完全等价**：KMP ALL 不包含 iOS 的 networkTracking；其中 DEEP_LINKS 在 iOS 不生效。 |
| `autocaptureOptions { ... }` | 使用 OptionSet 构造与集合操作 | `autocaptureOptions(init): Set<AutocaptureOption>` | 无同名 builder；使用 `setOf(...)` | **提供集合表达方式，但未复制原生辅助 API**。 |

## 6. TrackingOptions 接口

下表将成对的 `disable...()`／`shouldTrack...()` 放在同一行；KMP 列列出实际可调用的对应方法。

| 接口／属性 | iOS 原生接口 | Android 原生接口 | KMP 接口 | 等价情况与差异 |
|---|---|---|---|---|
| `TrackingOptions.disableCarrier()`／`shouldTrackCarrier()` | `disableTrackCarrier()`／`shouldTrackCarrier()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**；KMP shouldTrack 查询的是本地选项。 |
| `TrackingOptions.disableCity()`／`shouldTrackCity()` | `disableTrackCity()`／`shouldTrackCity()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disableCountry()`／`shouldTrackCountry()` | `disableTrackCountry()`／`shouldTrackCountry()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disableDeviceManufacturer()`／`shouldTrackDeviceManufacturer()` | `disableTrackDeviceManufacturer()`／`shouldTrackDeviceManufacturer()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disableDeviceModel()`／`shouldTrackDeviceModel()` | `disableTrackDeviceModel()`／`shouldTrackDeviceModel()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disableDma()`／`shouldTrackDma()` | `disableTrackDMA()`／`shouldTrackDMA()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**，方法大小写不同。 |
| `TrackingOptions.disableIpAddress()`／`shouldTrackIpAddress()` | `disableTrackIpAddress()`／`shouldTrackIpAddress()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disableLanguage()`／`shouldTrackLanguage()` | `disableTrackLanguage()`／`shouldTrackLanguage()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disableOsName()`／`shouldTrackOsName()` | `disableTrackOsName()`／`shouldTrackOsName()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disableOsVersion()`／`shouldTrackOsVersion()` | `disableTrackOsVersion()`／`shouldTrackOsVersion()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disablePlatform()`／`shouldTrackPlatform()` | `disableTrackPlatform()`／`shouldTrackPlatform()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disableRegion()`／`shouldTrackRegion()` | `disableTrackRegion()`／`shouldTrackRegion()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disableVersionName()`／`shouldTrackVersionName()` | `disableTrackVersionName()`／`shouldTrackVersionName()` | 同 KMP 方法名 | 同左侧接口 | **两端均映射禁用项**。 |
| `TrackingOptions.disableAdid()`／`shouldTrackAdid()` | — | 同名方法 | 同左侧接口 | **仅 Android 映射**，iOS 忽略该禁用项。 |
| `TrackingOptions.disableAppSetId()`／`shouldTrackAppSetId()` | — | 同名方法 | 同左侧接口 | **仅 Android 映射**，iOS 忽略该禁用项。 |
| `TrackingOptions.disableDeviceBrand()`／`shouldTrackDeviceBrand()` | — | 同名方法 | 同左侧接口 | **仅 Android 映射**，iOS 忽略该禁用项。 |
| `TrackingOptions.disableLatLng()`／`shouldTrackLatLng()` | 无对应控制项 | 同名方法 | 同左侧接口 | **仅 Android 映射**，iOS 忽略该禁用项。 |
| `TrackingOptions.disableApiLevel()`／`shouldTrackApiLevel()` | — | 同名方法 | — | **未包装 Android 接口**；直接向 KMP disabledFields 写入 `"api_level"` 也不会应用。 |
| `TrackingOptions.disableTrackIDFV()`／`shouldTrackIDFV()` | 同名方法 | — | 无类型化方法 | **未完整包装**；iOS mapper 识别 disabledFields 中的 `"idfv"`，但没有对应的 KMP 便利方法。 |
| `TrackingOptions.disabledFields` | Swift 内部字段，不是公开集合 | `disabledFields: MutableSet<String>` | `disabledFields: MutableSet<String>` | **部分等价**：KMP mapper 仅处理已列入映射的字符串；shouldTrack 的本地返回值不证明原生已应用该配置。 |
| `TrackingOptions.mergeIn(other)` | 无同名公开方法 | `mergeIn(other: TrackingOptions): TrackingOptions` | `mergeIn(other: TrackingOptions): TrackingOptions` | **提供对应的本地集合合并**，未映射的禁用字段不会因合并而获得原生支持。 |
| `TrackingOptions.copyOf(other)` | 无同名公开方法 | `copyOf(other: TrackingOptions): TrackingOptions` | `copyOf(other: TrackingOptions): TrackingOptions` | **提供对应的本地选项复制**，不是复制原生运行时配置对象。 |
| `TrackingOptions.forCoppaControl()` | 无同名公开静态方法；配置有 `enableCoppaControl` | `forCoppaControl(): TrackingOptions` | `forCoppaControl(): TrackingOptions` | **不完全等价**：Android 原生集合包含广告 ID、城市、IP、经纬度，KMP 集合不含广告 ID；Swift 原生 COPPA 字段集合还涉及 IDFA／IDFV。 |

隐私控制方法名称相近不代表禁用字段完全相同，也不构成合规保证；需要核对各端实际生效的配置。

## 7. 事件模型、Identify、Revenue 与回调字段

### BaseEvent 与 EventOptions

以下字段映射指 KMP 事件**发送到原生 SDK**的方向，不代表回调或插件能够完整读取这些字段。

| 接口／属性 | iOS 原生接口 | Android 原生接口 | KMP 接口 | 等价情况与差异 |
|---|---|---|---|---|
| `BaseEvent.eventType` | `eventType: String` | `eventType: String`，需初始化 | `eventType: String`，默认空字符串 | **已映射**；构造和未赋值时的行为与 Android 原生不完全相同。 |
| `BaseEvent.eventProperties` | `[String: Any]?`；OC 为 AMPProperties | `MutableMap<String, Any?>?` | `MutableMap<String, Any?>?` | **支持出站映射**；iOS OC 接口不能枚举完整 Map，入站读取受限。 |
| `BaseEvent.userProperties` | `[String: Any]?`；OC 为 AMPProperties | `MutableMap<String, Any?>?` | `MutableMap<String, Any?>?` | **支持出站映射**；回调、插件读取情况见后文。 |
| `BaseEvent.groups` | `[String: Any]?`；OC 为 AMPProperties | `MutableMap<String, Any?>?` | `MutableMap<String, Any?>?` | **支持出站映射**；回调、插件读取情况见后文。 |
| `BaseEvent.groupProperties` | `[String: Any]?`；OC 为 AMPProperties | `MutableMap<String, Any?>?` | `MutableMap<String, Any?>?` | **支持出站映射**；回调、插件读取情况见后文。 |
| `EventOptions.userId`／`deviceId`／`timestamp`／`eventId`／`sessionId`／`insertId` | 对应 EventOptions 属性 | 对应 EventOptions 属性 | 对应可空 Kotlin 属性 | **两端均映射**；OC 用部分数值哨兵表示未设置值，入站转换不是原生对象本身。 |
| `EventOptions.locationLat`／`locationLng` | 对应可空 Double 属性 | 对应可空 Double 属性 | 对应可空 Double 属性 | **两端均出站映射**；OC 回读时使用 NaN 表示未设置值。 |
| `EventOptions.appVersion`／`versionName`／`platform`／`osName`／`osVersion` | 对应 String? 属性 | 对应 String? 属性 | 对应 String? 属性 | **两端均出站映射**。 |
| `EventOptions.deviceBrand`／`deviceManufacturer`／`deviceModel`／`carrier` | 对应 String? 属性 | 对应 String? 属性 | 对应 String? 属性 | **两端均出站映射**，不代表原生自动补充字段的规则相同。 |
| `EventOptions.country`／`region`／`city`／`dma` | 对应 String? 属性 | 对应 String? 属性 | 对应 String? 属性 | **两端均出站映射**。 |
| `EventOptions.idfa`／`idfv`／`adid` | 对应 String? 属性 | 对应 String? 属性 | 对应 String? 属性 | **两端均出站映射显式值**，与 SDK 是否自动采集这些 ID 是不同问题。 |
| `EventOptions.language`／`library`／`ip`／`partnerId` | 对应 String? 属性 | 对应 String? 属性 | 对应 String? 属性 | **两端均出站映射**。 |
| `EventOptions.plan`／`ingestionMetadata` | 对应原生模型属性 | 对应原生模型属性 | 对应 KMP 模型属性 | **支持出站映射**；Android 回调保留已修复，插件路径仍有遗漏。 |
| `EventOptions.revenue`／`price`／`quantity`／`productId`／`revenueType`／`currency` | 对应原生属性 | 对应原生属性 | 对应 KMP 属性 | **支持出站映射**；Android 回调标量字段遗漏已修复。 |
| `EventOptions.extra` | Swift 有对应属性，当前 OC 桥接路径未映射 | `extra: Map<String, Any>?` | `extra: Map<String, Any>?` | **仅 Android 出站映射**；iOS 不转发。 |
| `EventOptions.appSetId`／`androidId` | 无对应原生属性 | 对应 String? 属性 | 对应 String? 属性 | **仅 Android 出站映射**；iOS 忽略。 |
| `EventOptions.callback` | `EventCallback?` | `EventCallBack?` | `EventCallBack?` | **部分等价**：两端回调事件副本的字段范围不同。 |
| `BaseEvent.mergeEventOptions(options)` | `mergeEventOptions(eventOptions: EventOptions)` | `mergeEventOptions(options: EventOptions)` | `mergeEventOptions(options: EventOptions)` | **提供对应合并操作**，但 KMP 与原生不是同一个对象，原生后续补充的字段不会写回 KMP 实例。 |
| `BaseEvent.isValid()` | 内部校验方法 | `isValid(): Boolean`，接受非 null ID | `isValid(): Boolean`，拒绝空白 ID | **不完全等价**；此处只说明对象辅助校验，不代表服务端最终接受规则。 |
| `GroupIdentifyEvent` | 原生事件类型 | 原生事件类型 | 无对应公共类型 | **未包装该类型**；公共 `groupIdentify()` 方法仍可使用。 |
| `ScreenViewedEvent`／`DeepLinkOpenedEvent` | 公开原生专用事件类型 | 无相同的 Swift 类型接口 | 无对应公共类型 | **未包装这些 Swift 专用类型**；不等于 KMP 支持其全部原生构造接口。 |

任意 Kotlin 自定义对象也不保证与原生 Swift 值具有相同的序列化结果：iOS 的值转换对不支持的类型会回退为字符串。

### Identify 与 Revenue

| 接口／属性 | iOS 原生接口 | Android 原生接口 | KMP 接口 | 等价情况与差异 |
|---|---|---|---|---|
| `Identify.set(property, value)`／`setOnce(property, value)` | 对应类型重载 | 对应类型重载 | 基本类型、Map、List、数组及 Any 重载 | **封装基本操作**；任意 Any 值仍受原生序列化及 iOS 值转换限制。 |
| `Identify.add(property, value)` | 数值类型重载 | 数值类型重载 | Double／Float／Int／Long 重载 | **等价封装数值操作**。 |
| `Identify.append(property, value)`／`prepend(property, value)` | 对应重载 | 对应重载 | 对应 KMP 重载 | **封装基本操作**，不代表所有容器或自定义值都能无差别转换。 |
| `Identify.preInsert(property, value)`／`postInsert(property, value)`／`remove(property, value)` | 对应重载 | 对应重载 | 对应 KMP 重载 | **封装基本操作**，属性值转换限制同上。 |
| `Identify.unset(property)` | `unset(property: String)` | `unset(property: String): Identify` | `unset(property: String): Identify` | **等价封装基本操作**。 |
| `Identify.clearAll()` | `clearAll() -> Identify` | `clearAll(): Identify` | `clearAll(): Identify` | **普通 identify 路径等价转发**；不代表 getOperations 或 toIdentifyEvent 的原始载荷等价。 |
| `Identify.properties` | 非公开模型字段 | `properties: MutableMap<String, Any?>` | 无同名属性；提供 `getOperations()` | **不等价接口**：KMP clearAll 快照为 `{"$clearAll":{"-":"-"}}`，Android 原生事件表示为 `{"$clearAll":"-"}`。 |
| `Identify.getOperations()` | 无对应公开方法 | 无同名方法，公开属性为 `properties` | `getOperations(): Map<String, Map<String, Any?>>` | **KMP 额外快照接口**；操作结构不能当作原生 properties 的完全等价替代。 |
| `IdentifyOperation`／`Identify.Operation` | Swift 内部操作枚举 | 顶层公开 `IdentifyOperation` | 嵌套 `Identify.Operation` | **不是相同类型或 import 路径**。 |
| `Revenue.price`／`quantity`／`productId`／`revenueType`／`currency`／`revenue` | 对应原生属性 | 对应原生属性 | 对应 KMP 属性 | **提供对应模型字段与基本映射**；使用 revenue(Revenue) 与直接发送 RevenueEvent 的校验路径不同。 |
| `Revenue.receipt` | `receipt: String?` | `receipt: String?` | `receipt: String?` | **两端均独立映射**：Android 不再要求签名同时非 null。 |
| `Revenue.receiptSig` | `receiptSig: String?`，OC 也暴露 setter | `receiptSig: String?` | `receiptSig: String?` | **两端均独立映射**：Android 不再要求收据同时非 null；不额外保证验证成功。 |
| `Revenue.setReceipt(receipt, receiptSignature)` | `setReceipt(receipt: String, receiptSignature: String) -> Revenue` | 对应 Kotlin 方法 | 对应 Kotlin 方法 | **两字段均已映射**：iOS 已补齐 receiptSig；包装层不新增收据验证逻辑。 |
| `Revenue.properties` | `[String: Any]?`；OC 为 AMPProperties | `MutableMap<String, Any?>?` | `MutableMap<String, Any?>?` | **两端支持自定义属性出站映射**。 |
| `Revenue.isValid()` | 非公开；原生 revenue 内部检查 price | `isValid(): Boolean` | `isValid(): Boolean` | **对应 Android 辅助接口**，检查 price 是否存在；不是 Swift 同名公开 API 的包装。 |
| `Revenue.toRevenueEvent()` | 非公开转换方法 | `toRevenueEvent(): RevenueEvent` | `toRevenueEvent(): RevenueEvent` | **提供 KMP 模型转换**，不保留原生事件子类身份；直接 track 也不执行 revenue(Revenue) 的入口校验。 |

### EventCallBack 中的事件字段

原生回调提供实际原生事件；KMP 回调提供转换后的 KMP 对象。下表专门说明**回调返回方向**。

| 接口／属性 | iOS 原生回调 | Android 原生回调 | KMP 回调 | 等价情况与差异 |
|---|---|---|---|---|
| `EventCallBack(event, status, message)` | `EventCallback(BaseEvent, Int, String)` | `EventCallBack(BaseEvent, Int, String)` | `EventCallBack(BaseEvent, Int, String)` | **部分等价**：转发状态及消息，不完整还原事件对象；单个回调不代表队列全部上传完成。 |
| `event.eventType` | 实际事件类型 | 实际事件类型 | 两端均复制真实事件类型 | **等价读取该字段**。 |
| `event.userId`／`deviceId`／`timestamp` | 原生事件字段 | 原生事件字段 | 两端均复制 | **保留对应字段**，iOS 未设置数值通过 OC 哨兵转换。 |
| `event.eventProperties`／`userProperties`／`groups`／`groupProperties` | 原生 Map；OC 包装不能枚举完整 key 集合 | 原生 Map | Android 复制；iOS 均为 null | **不完全等价**：不能依赖 iOS 回调读取属性 Map。 |
| `event.insertId`／`eventId`／`sessionId` | 原生事件字段 | 原生事件字段 | 两端均复制 | **Android 遗漏已修复**：保留原生结果，包括 sessionId 为 -1 的情况。 |
| `event.plan`／`ingestionMetadata` | 原生事件字段 | 原生事件字段 | 两端均复制 | **Android 遗漏已修复**。 |
| `event.locationLat`／`locationLng` | 原生事件字段 | 原生事件字段 | 两端均复制 | **Android 遗漏已修复**。 |
| `event.appVersion`／`versionName`／`platform`／`osName`／`osVersion` | 原生事件字段 | 原生事件字段 | 两端均复制 | **Android 遗漏已修复**。 |
| `event.deviceBrand`／`deviceManufacturer`／`deviceModel`／`carrier` | 原生事件字段 | 原生事件字段 | 两端均复制 | **Android 遗漏已修复**。 |
| `event.country`／`region`／`city`／`dma` | 原生事件字段 | 原生事件字段 | 两端均复制 | **Android 遗漏已修复**。 |
| `event.idfa`／`idfv`／`adid` | 原生事件字段 | 原生事件字段 | 两端均复制 | **Android 遗漏已修复**。 |
| `event.language`／`library`／`ip`／`partnerId` | 原生事件字段 | 原生事件字段 | 两端均复制 | **Android 遗漏已修复**。 |
| `event.revenue`／`price`／`quantity`／`productId`／`revenueType`／`currency` | 原生事件字段 | 原生事件字段 | 两端均复制标量字段 | **Android 遗漏已修复**。 |
| `event.extra` | Swift 原生事件字段 | 原生事件字段 | Android 已复制；iOS 未复制 | **iOS 限制保持不变**。 |

更多限制见 [iOS 回调说明](known-limitations.md#ios-callback-event-properties)。

**当前状态：未解决，暂缓处理。** iOS 单事件回调及全局回调中的四个属性 Map 和 `extra`
仍未完整还原；这不表示回调转换导致上传事件属性丢失，也不同于自定义插件重建事件的属性丢失问题。
当前继续使用官方依赖，不新增 Swift 桥接、SDK 实例或回调拦截插件，也不切换至 fork。
官方 ObjCBaseEvent 持有的 Swift 事件为 `internal`，ObjCAmplitude 持有的 Swift SDK 实例为 `private`，
独立 Swift 模块的 extension 无法绕过这些访问限制；单纯新增 `@objc` 方法不能直接取得完整属性。
不使用输入缓存冒充最终事件，也不通过反射访问私有成员。
后续可评估上游增加公开字典快照／键枚举接口，或维护包含该能力的 fork；届时还需更新 KMP 映射及下游集成。
详见 [桥接边界与当前状态](known-limitations.md#current-status-and-bridge-boundary)。

## 8. 插件接口

| 接口／属性 | iOS 原生接口 | Android 原生接口 | KMP 接口 | 等价情况与差异 |
|---|---|---|---|---|
| `Amplitude.add(plugin)` | `add(plugin: UniversalPlugin)`；OC 接受 AMPPlugin | `add(plugin: Plugin): Amplitude` | `add(plugin: Plugin): Amplitude` | **部分等价**：只接收 KMP Plugin，不能直接传原生插件；同一 SDK 重复添加同一个 KMP 插件对象会被忽略。 |
| `Amplitude.remove(plugin)` | `remove(plugin: UniversalPlugin)`／OC AMPPlugin | `remove(plugin: Plugin): Amplitude` | `remove(plugin: Plugin): Amplitude` | **普通顺序调用可移除对应包装器**；并发 add／remove、setup 中移除自己或 setup 异常时，不提供一致状态保证。 |
| `Plugin.type` | `type: PluginType` | `type: Plugin.Type` | `type: Plugin.Type` | **枚举名称映射**不等于原生 Plugin／EventPlugin／DestinationPlugin 类型层级也已包装。 |
| `Plugin.setup(amplitude)` | `setup(amplitude: Amplitude)` | `setup(amplitude: Amplitude)` | `setup(amplitude: Amplitude)` | **已转发初始化回调**，KMP 插件收到所属 KMP SDK 实例，而非原生 SDK 对象。 |
| `Plugin.execute(event)` | `execute(event: BaseEvent) -> BaseEvent?` | `execute(event: BaseEvent): BaseEvent?` | `execute(event: BaseEvent): BaseEvent?` | **部分等价**：Android 未保留 plan／ingestionMetadata／extra／callback；iOS 未保留四类属性 Map。两端重建事件均不保留原生子类身份。 |
| `Plugin.teardown()` | 原生插件有清理接口 | `teardown()` | — | **未包装**，开发者不能通过 KMP teardown 钩子在移除时清理资源。 |
| `EventPlugin.track(event)` | `track(event: BaseEvent)` | `track(payload: BaseEvent)` | — | **未包装类型化事件钩子**，KMP 仅提供通用 execute。 |
| `EventPlugin.identify(event)` | `identify(event: IdentifyEvent)` | `identify(payload: IdentifyEvent)` | — | **未包装类型化事件钩子**。 |
| `EventPlugin.groupIdentify(event)` | `groupIdentify(event: GroupIdentifyEvent)` | `groupIdentify(payload: GroupIdentifyEvent)` | — | **未包装类型化事件钩子**。 |
| `EventPlugin.revenue(event)` | `revenue(event: RevenueEvent)` | `revenue(payload: RevenueEvent)` | — | **未包装类型化事件钩子**。 |
| `EventPlugin.flush()` | `flush()` | `flush()` | — | **未包装插件刷新钩子**，与 SDK 实例的 flush() 不同。 |
| `Plugin.onUserIdChanged(userId)` | `onUserIdChanged(String?)` | `ObservePlugin.onUserIdChanged(String?)` | — | **未包装通知接口**；设置 KMP Type.OBSERVE 不会提供该回调。 |
| `Plugin.onDeviceIdChanged(deviceId)` | `onDeviceIdChanged(String?)` | `ObservePlugin.onDeviceIdChanged(String?)` | — | **未包装通知接口**。 |
| `Plugin.onSessionIdChanged(sessionId)` | `onSessionIdChanged(Int64)` | 无同名 Plugin 钩子 | — | **未包装 iOS 通知接口**。 |
| `Plugin.onOptOutChanged(optOut)` | `onOptOutChanged(Bool)` | 无同名 Plugin 钩子 | — | **未包装 iOS 通知接口**。 |
| `Amplitude.plugin(name)` | `plugin(name: String) -> UniversalPlugin?` | 无同名 SDK 实例接口 | — | **未包装 iOS 插件查询接口**。 |
| `Amplitude.plugins(type)` | `plugins<T: UniversalPlugin>(type: T.Type) -> [T]` | 无同名 SDK 实例接口 | — | **未包装 iOS 泛型查询接口**。 |
| `Amplitude.apply(closure)` | `apply(closure: (Plugin) -> Void)` | 可通过 `timeline.applyClosure(...)` 操作，非同名实例接口 | — | **未包装插件遍历接口**。 |

**iOS 使用注意：**原生 OC 插件处理 Identify／GroupIdentify／Revenue 时，要求返回对应的原生事件子类。
KMP execute 返回后重建的是基础事件，因此这些类型化事件还可能被丢弃。
如果事件属性和类型化事件必须完整保留，不应依赖当前的 iOS 自定义 KMP 插件路径。
详见 [iOS 插件限制](known-limitations.md#ios-custom-plugin-property-loss)。

## 9. 原生 SDK 对象与状态访问

| 接口／属性 | iOS 原生接口 | Android 原生接口 | KMP 接口 | 等价情况与差异 |
|---|---|---|---|---|
| `Amplitude.configuration` | 公开原生配置引用，OC 也提供 | `configuration: Configuration` | — | **未包装**，不能通过 KMP SDK 实例修改原生运行时配置。 |
| `Amplitude.identity` | 可访问原生 Identity | 无同名 SDK 属性，身份由原生身份管理器维护 | — | **未包装 iOS 身份对象**；KMP 仅提供独立 userId／deviceId 方法。 |
| `Amplitude.amplitudeContext` | `amplitudeContext: AmplitudeContext` | 无同名 SDK 属性 | — | **未包装 iOS 上下文对象**。 |
| `Amplitude.storage` | Swift 内部存储；OC 提供 AMPStorage 读取入口 | 公开 `storage: Storage` | — | **未包装存储对象访问**。 |
| `Amplitude.storage.getEventsStrings()` | OC `AMPStorage.getEventsStrings() -> [String]` | 无对应同名接口，原生 Storage 有自身操作 | — | **未包装 OC 队列内容读取接口**。 |
| `Amplitude.storage.getInterceptedIdentifiesStrings()` | OC `AMPStorage.getInterceptedIdentifiesStrings() -> [String]` | 无对应同名接口 | — | **未包装 OC Identify 存储读取接口**。 |
| `Amplitude.logger` | `logger`，原生日志器 | `logger: Logger` | — | **未包装日志器对象访问**。 |
| `Amplitude.store` | 无对应同名 SDK 属性 | `store: State` | — | **未包装 Android 状态对象**。 |
| `Amplitude.timeline` | 非公开 SDK 属性 | `timeline: Timeline` | — | **未包装 Android timeline**。 |
| `Amplitude.createTimeline()` | — | `open createTimeline(): Timeline` | — | **未包装 Android 扩展点**；KMP Amplitude 也不是对应的可继承 SDK 类型。 |
| `Amplitude.identityStorage`／`identifyInterceptStorage` | 无对应同名 SDK 属性 | 对应公开存储引用 | — | **未包装 Android 存储引用**。 |
| `Amplitude.amplitudeScope`／`amplitudeDispatcher`／`networkIODispatcher`／`storageIODispatcher` | 无对应 Kotlin 协程接口 | 对应公开作用域／调度器引用 | — | **未包装 Android 协程基础设施**。 |
| `Amplitude.diagnostics`／`remoteConfigClient` | 有原生上下文和配置中的相关设施，非对应同名 SDK 属性 | 对应公开属性 | — | **未包装这些 Android 原生对象引用**。 |
| `Amplitude.signalFlow` | 使用原生信号设施，非 Kotlin Flow | `signalFlow: SharedFlow<Signal>` | — | **未包装 Android 信号订阅接口**。 |

KMP 没有公开的 `getNativeInstance()` 一类接口。需要上述未提供能力时，需要另行设计原生集成，不能直接从公共 KMP 实例取得底层 SDK。

## 10. 已删除的 KMP 协程便利接口

KMP 不再提供自定义 `awaitTrack()`（两个重载）、`awaitIdentify()`（两个重载）、
`awaitRevenue()`、`awaitSetUserId()`、`awaitSetDeviceId()` 和 `awaitReset()`。
Android 专属的 `invokeAndAwait()`、`awaitInitialization()` 便利接口也已删除。
这些都是包装层新增的接口，并非原生 SDK 的同签名方法。

请直接使用 `track()`、`identify()`、`revenue()`、`setUserId()`、`setDeviceId()`、
`reset()`，通过 `Amplitude(configuration)` 创建实例。`track(BaseEvent, options, callback)`
的可选事件回调仍然保留，但 KMP 不再提供回调转协程封装，也不额外承诺等待操作完成。

旧 Identify／Revenue 协程接口使用 track 而非原生专用入口，以及 Map 接口将 null 转为空字符串的路径，均随接口删除。
迁移说明见 [1.1.0 变更记录](../CHANGELOG.md#110---2026-11-07)。

两端公开 `Amplitude.flush()` 都没有提供“全部事件上传成功”的完成信号，KMP 不提供对应等待接口。

## 接口定义参考

- [KMP 公共 Amplitude 接口](../kmp/src/commonMain/kotlin/com/amplitude/kmp/Amplitude.kt)
- [KMP 配置与枚举](../kmp/src/commonMain/kotlin/com/amplitude/kmp/Configuration.kt)
- [Android Core Amplitude 1.22.4](https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/core/src/main/java/com/amplitude/core/Amplitude.kt)
- [Android Amplitude 1.22.4](https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/android/src/main/java/com/amplitude/android/Amplitude.kt)
- [Android Configuration 1.22.4](https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/android/src/main/java/com/amplitude/android/Configuration.kt)
- [Android TrackingOptions 1.22.4](https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/android/src/main/java/com/amplitude/android/TrackingOptions.kt)
- [Android Plugin 1.22.4](https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/core/src/main/java/com/amplitude/core/platform/Plugin.kt)
- [Swift Amplitude 1.16.2](https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/Amplitude.swift)
- [Swift Configuration 1.16.2](https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/Configuration.swift)
- [Swift 插件接口 1.16.2](https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/Types.swift)
- [Objective-C Amplitude 1.16.2](https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/ObjC/ObjCAmplitude.swift)
- [Objective-C 属性接口 1.16.2](https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/ObjC/ObjCProperties.swift)
