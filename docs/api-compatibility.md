# Native SDK and KMP Compatibility

[Project overview](../README.md) · [Usage guide](usage.md) · [Known limitations](known-limitations.md)

This document compares the native Android and iOS SDKs with the current KMP wrapper.
It records missing public APIs, platform differences, bridge limitations, and behavior
that is not equivalent even when the method names look similar. The wrapper is not
a complete replacement for every native API, and import-only migration is not guaranteed.

## Scope and Versions

Original source audit date: **2026-10-28**. KMP audit baseline: commit **`33affb6`**.
The tables and baseline source references below retain that audit's native versions;
they are not a claim about an already published Maven artifact.

The current checkout declares **Android Analytics/Core 1.23.1** and
**`AmplitudeSwift ~> 1.16.6`**. A source comparison against these upgrade targets
found no required changes to the native entry points used by the wrapper. It does
not replace the original audit with an exhaustive review of every native API.

Post-audit update: the current checkout removes the KMP-only coroutine helpers and
Android initialization await conveniences described in the original audit. Sections
below reflect that removal; unrelated findings retain the audit baseline above.
The current checkout also fixes iOS Revenue signature mapping (R1) and 64-bit
duration mapping (F2), with iOS mapper regression tests.
Android Revenue now preserves independently assigned receipt/signature fields, and
Android callback mapping preserves all shared public event fields (C1). The iOS
callback property-map limitation (C2) remains unchanged.

| Component | Historical audit version | Evidence recorded by the original audit |
|---|---|---|
| Android Analytics | `com.amplitude:analytics-android:1.22.4` | Original version catalog and published sources |
| Android Core | `com.amplitude:analytics-core:1.22.4` | Transitive dependency and published sources |
| Amplitude-Swift baseline | `v1.16.2` | Official tagged Swift and Objective-C wrapper sources |
| Previously resolved iOS dependency | `AmplitudeSwift 1.16.6` | Historical build record; not revalidated in the current upgrade check |
| Previously resolved native dependencies | `AmplitudeCore 1.5.4`, `AnalyticsConnector 1.3.3` | The same historical record; not the confirmed current dependency resolution |

The current iOS constraint **`~> 1.16.6`** means `>= 1.16.6, < 1.17.0`, not an exact
version. Resolved native dependency versions must be checked in the consuming app's
CocoaPods lockfile; no lockfile is currently present in this checkout.

Upgrade validation on **2026-11-07**: Android compilation and all five existing host
tests passed. iOS compilation/linking and dependency resolution remain unverified:
the build stopped because the CocoaPods executable was not available. Historical
iOS build results above must not be treated as validation of this upgrade.

Findings below are based on source inspection unless explicitly described as a prior
build/test result. This audit does not establish exhaustive runtime, networking,
binary compatibility, or privacy-compliance guarantees. It focuses on SDK entry
points, configuration, event/Identify/Revenue models, callbacks, plugins, and coroutine
extensions, rather than every public native utility type.

### Classification

- **Available**: the shared entry point is present; not a claim of identical behavior.
- **Wrapper gap**: a native public capability is not exposed or fully mapped.
- **Bridge limitation**: Swift supports it, but the public Objective-C interface limits access.
- **Platform difference**: native SDKs expose different concepts or behavior.
- **Intentional omission**: an obsolete, misleading, or KMP-only convenience API has been removed.
- **Fixed in repository**: an earlier defect is no longer present in the audited checkout.

## Important Current Differences

| ID | Area | Difference | Classification |
|---|---|---|---|
| P1 | iOS custom plugins | Property maps are not copied; typed Identify/GroupIdentify/Revenue events can also be dropped when rebuilt as a base event. | Wrapper gap + bridge limitation |
| C1 | Android callbacks | All shared public event fields are copied, including plan, metadata, extra, and callable event callbacks. Native subclass identity and internal retry state are not reproduced. | Fixed in repository |
| C2 | iOS callbacks | Real event type and readable scalar/plan/metadata fields are copied, but property maps are unavailable. | Bridge limitation |
| P2 | Android custom plugins | `plan`, `ingestionMetadata`, `extra`, and `callback` are not copied into the KMP event and can be lost on reconstruction. | Wrapper gap |
| R1 | iOS revenue receipts | `Revenue.receiptSig` is now passed to `AMPRevenue`, independently of receipt. | Fixed in repository |
| T1 | Privacy helpers | KMP's COPPA convenience options are not the same field set as the native helpers; some individual controls are missing. | Wrapper gap / platform difference |
| F1 | Configuration updates | Changing the original KMP `Configuration` after construction does not update native scalar configuration. | Wrapper gap |
| F2 | iOS durations | Both `Long` durations are now passed directly to the 64-bit native APIs. | Fixed in repository |

The sections below explain the scope and consequences of each difference.

## 1. SDK Entry Points

### Shared Methods

| KMP API | Android native SDK | iOS native SDK | KMP status / difference |
|---|---|---|---|
| `Amplitude(configuration)` | Constructor | Constructor | Available; KMP converts to a separate native configuration. |
| `track(eventType, properties, options)` | Available | Available | Available. iOS additionally builds a KMP event and snapshots current user/device IDs before forwarding. |
| `track(BaseEvent, options, callback)` | Available | Swift and Objective-C overloads | Available; Android shared public fields are mapped (C1 fixed); iOS callback fields remain incomplete (C2). |
| `identify(Map, options)` | Available; null map values are skipped | Available in Swift; KMP builds `AMPIdentify` for Objective-C | Available; value conversion is platform-specific. |
| `identify(Identify, options)` | Available | Available | Available; uses native Identify builders rather than raw event serialization. |
| `setGroup(String)` / `setGroup(Array<String>)` | Both available | Swift array overload; Objective-C uses `groupNames` | Both available; full-array forwarding is fixed in the repository. |
| `groupIdentify(Map/Identify, options)` | Available | Swift supports both; Objective-C accepts Identify | Available through conversion. |
| `revenue(Revenue, options)` | Available, validates required price | Available, validates required price | Available; receipt and signature are independently mapped on both platforms (R1 fixed). Native validation and plugin limitations still apply. |
| `revenue(RevenueEvent)` | Native typed RevenueEvent | Swift typed RevenueEvent; no corresponding typed Objective-C overload | Both KMP implementations convert to native **BaseEvent** and call `track()`, losing native subclass identity. |
| `setUserId` / `getUserId` | Available | Available | Available; Android identity setters run asynchronously in the native SDK. |
| `setDeviceId` / `getDeviceId` | Setter takes non-null String | Setter accepts nullable String | KMP setter requires non-null String on both platforms; the Swift nullable operation is not exposed. |
| `reset()` | Available | Available | Delegated to each platform; native reset policies and plugin notifications are not identical. |
| `sessionId` | Read-only property | Property and `getSessionId()` | Available as a read-only shared property. |
| `onEnterForeground` / `onExitForeground` | Guarded public functions | Internal Swift functions | Android forwards; iOS is an intentional no-op. iOS uses native lifecycle handling. |
| `flush()` | Returns `Unit`, schedules work | Returns native instance, schedules work | Shared API returns `Unit`; neither public native entry point waits for all uploads. |
| `add` / `remove` | Native Plugin interfaces | Swift UniversalPlugin / Objective-C AMPPlugin | Available only for the simplified KMP Plugin interface; see section 5. |

Sources: [shared API](../kmp/src/commonMain/kotlin/com/amplitude/kmp/Amplitude.kt),
[Android implementation](../kmp/src/androidMain/kotlin/com/amplitude/kmp/Amplitude.android.kt),
[iOS implementation](../kmp/src/iosMain/kotlin/com/amplitude/kmp/Amplitude.ios.kt),
[Android core API][android-amplitude], [Android-specific API][android-specific-amplitude],
[Swift API][swift-amplitude], [Objective-C API][objc-amplitude].

### Native Public Capabilities Not Exposed in Shared Code

| Capability | Native availability | KMP difference |
|---|---|---|
| Live SDK `configuration` | Android and Swift; Objective-C also exposes configuration | No public `Amplitude.configuration`. No shared runtime setters for opt-out, offline mode, or general configuration changes. |
| Runtime `setSessionId(timestamp)` / `setSessionId(date)` | Swift and Objective-C | No shared runtime session setter. `Configuration.sessionId` is only applied during wrapper construction. |
| Native storage inspection | Android `storage` / identity-related storage; Objective-C `storage.getEventsStrings()` and `getInterceptedIdentifiesStrings()` | Not exposed. |
| Native logger access | Android `logger`; Swift `logger` and configuration logger provider | No shared logger handle or custom logger provider. |
| Advanced Android state | `store`, `timeline`, `isBuilt`, coroutine scope/dispatchers, diagnostics, remote-config client, `signalFlow` | Not exposed as shared native handles. Some are advanced infrastructure rather than ordinary tracking APIs. |
| Swift identity/context | `identity`, `amplitudeContext` | No shared identity object, user-properties snapshot, or native context handle. |
| Swift plugin lookup | `plugin(name:)`, `plugins(type:)`, `apply(closure:)` | Not exposed. Swift-only/generic surfaces are not automatically available through Objective-C cinterop. |
| Subclassing SDK/configuration | Android native classes are open | Shared KMP Amplitude and Configuration are not equivalent extension points. |
| Native helper methods/types | Android configuration `isValid()` / `getApiHost()`, `autocaptureOptions { ... }`, native event/storage/plugin classes | Not all are mirrored. Equivalent tracking behavior does not imply source-compatible helper APIs. |
| Additional Apple targets | Swift SDK supports platforms beyond iOS | KMP builds Android plus the configured iOS device/simulator targets, not the full Swift platform set. |

### Initialization and Instance Management

- Shared code constructs instances with `Amplitude(configuration)`. Keep the returned
  instance and pass it explicitly or through dependency injection.
- `getInstance()` and its global cached instance have been removed. Multiple instances
  remain supported; use distinct `instanceName` values when separating instances.
- `Amplitude(apiKey, context, configs)` is Android-only; `Amplitude(apiKey, configs)`
  is iOS-only. They are not declared in the common `expect` companion.
- Android exposes `isInitialized()` in `androidMain`, not shared code. It checks
  `Deferred.isCompleted`, which does not itself establish successful completion.
- KMP-only `invokeAndAwait()` and `awaitInitialization()` conveniences have been
  removed. Construct instances with `Amplitude(configuration)`; the wrapper does
  not expose an initialization-completion await or callback API.

## 2. Configuration and Automatic Capture

### Configuration Mapping

| Configuration / capability | Android wrapper | iOS wrapper | Difference |
|---|---|---|---|
| API key, instance name, flush queue/interval/retries, opt-out, partner ID, callback, minimum ID length, server zone/URL, batch mode, plan, ingestion metadata | Mapped | Mapped | Callback and default-value differences remain as documented below. |
| `logLevel` | Converted to `Logger.LogMode` using a Logcat logger provider | Converted to `AMPLogLevel` | Available. `LOG` maps to Android INFO; `VERBOSE` falls back to DEBUG on both native APIs. |
| `autocapture` | Maps shared enum set | Maps to `AMPAutocaptureOptions` | Available, but supported option sets differ. |
| `trackingOptions`, `enableCoppaControl`, `flushEventsOnClose`, `interactionsOptions` | Mapped | Mapped | Privacy control coverage differs; see section 3. Interaction enabled flags are mapped on both platforms. |
| `minTimeBetweenSessionsMillis`, `identifyBatchIntervalMillis` | Passed as `Long` | Passed directly as 64-bit values | **F2 fixed:** no intermediate 32-bit narrowing. |
| `deviceId` | Native initialization configuration | Instance setter after native construction | Early automatically captured iOS events can precede the override. |
| `sessionId` | Native timeline initial value | Calls `setSessionIdWithTimestamp()` after construction | iOS queues a session operation, can emit session events, and is not synchronous initial-field assignment. Negative timestamps request session end in the native API. |
| `useAdvertisingIdForDeviceId`, `useAppSetIdForDeviceId`, `newDeviceIdPerInstall`, `locationListening` | Mapped | Ignored | Android-specific options, not unimplemented iOS equivalents. |
| `migrateLegacyData` | Mapped | Mapped | The KMP comment groups it with Android-only options, but the iOS implementation also applies it. |
| Nullable `offline` | Native `Boolean?`, including `null` to disable the connectivity checker | Native `Bool?` with a corresponding nil mode | KMP exposes only non-null `Boolean`; that native third state is not expressible. |
| Custom storage/identity/Identify storage providers | Public native provider configuration | Public Swift storage providers; Objective-C exposure is narrower | No shared provider injection. |
| Custom logger provider | Public native provider configuration | Swift provider and Objective-C callback provider | No shared injection; selecting `logLevel` is not equivalent to supplying a logger. |
| Custom HTTP client | Android native `httpClient` | No corresponding constructor option in the audited Swift Configuration | Android capability not exposed by KMP. |
| `networkTrackingOptions` | No matching autocapture enum option in Android 1.22.4 | Swift and Objective-C public configuration | Not exposed by KMP. |
| `maxQueuedEventCount` | No matching constructor option in the audited Android Configuration | Public Swift option | Not exposed; also not offered by the audited Objective-C Configuration wrapper. |
| `enableAutoCaptureRemoteConfig` | Native Android implementation manages its own remote configuration | Public Swift option and Objective-C initializer | KMP has no option to configure the iOS switch. |
| `enableDiagnostics` | Native diagnostics infrastructure exists | Public Swift option | KMP has no shared diagnostics switch; the audited Objective-C Configuration wrapper also lacks this option. |

**F1: KMP configuration is not a live native configuration handle.** For example:

```kotlin
val config = Configuration(apiKey = "KEY", androidContext = platformContext)
val sdk = Amplitude(config)
config.optOut = true // Does not update the native SDK's optOut value.
```

Set configuration before constructing the wrapper. This differs from using a native
SDK's exposed live configuration, where supported mutable fields can be changed.

The defaults are not identical either: KMP's `flushMaxRetries` is **5**, whereas the
audited Swift native default is **6**. KMP's default log level is **WARN**, whereas
the default Android Logcat logger's mode is **INFO**. These are wrapper choices,
not evidence that the SDK ignored a mapped value.

Sources: [KMP configuration](../kmp/src/commonMain/kotlin/com/amplitude/kmp/Configuration.kt),
[Android mapper](../kmp/src/androidMain/kotlin/com/amplitude/kmp/mappings/android/ConfigurationMapper.kt),
[iOS mapper](../kmp/src/iosMain/kotlin/com/amplitude/kmp/mappings/ios/ConfigurationMapper.kt),
[Android configuration][android-configuration], [Swift configuration][swift-configuration],
[Objective-C configuration][objc-configuration].

### Automatic Capture Options

| Option | Android 1.22.4 | Swift 1.16.2 | KMP |
|---|---|---|---|
| Sessions, app lifecycles, screen views, element interactions, frustration interactions | Supported | Supported | Shared enums mapped to each SDK; platform UI detection is still native-specific. |
| Deep links | `DEEP_LINKS` | No matching autocapture option | Shared `DEEP_LINKS` is Android-only and ignored on iOS. It is no longer incorrectly mapped to screen views. |
| Network tracking | No matching enum in the audited Android SDK | `.networkTracking`, plus capture rules and exclusions | No shared enum or configuration surface; not exposed even on iOS. |
| All options | Native Android enum set | Native Swift `.all` includes network tracking | KMP `AutocaptureOption.ALL` is **not** equivalent to Swift `.all`. |
| Builder/OptionSet helpers | `autocaptureOptions { ... }` | `rawValue`, unions/intersections | KMP uses a Kotlin `Set`; native helper syntax is not mirrored. |

Sources: [Android autocapture][android-autocapture], [Swift autocapture][swift-autocapture].

## 3. Privacy Controls

| Control | Native capability | KMP difference |
|---|---|---|
| Android API-level tracking | `disableApiLevel()` / `shouldTrackApiLevel()` | No matching KMP helper; the Android mapper also does not handle `"api_level"` in `disabledFields`. |
| iOS IDFV tracking | `disableTrackIDFV()` / `shouldTrackIDFV()` | No typed KMP helper. The iOS mapper recognizes `"idfv"`, so only direct manipulation of the string set can currently request it. |
| Android-only fields | Ad ID, App Set ID, device brand, latitude/longitude controls | KMP provides helpers and Android mappings; the corresponding flags are ignored by the iOS mapper. This does not imply an equivalent native iOS collection path exists. |
| `TrackingOptions.forCoppaControl()` | Android's helper disables Ad ID, city, IP address, latitude/longitude | KMP helper disables only city, IP address, latitude/longitude: Ad ID is omitted. |
| Swift COPPA option set | Native Swift includes IDFA/IDFV, city, IP address | KMP's convenience helper is not the same field set. `enableCoppaControl = true` is separately mapped to the native configuration. |
| Arbitrary `disabledFields` strings | Each native SDK recognizes its own names | Unknown/unmapped strings are silently ignored by the KMP mappers; adding a string does not establish that tracking was disabled. |

Do not equate a convenience helper's name with identical native privacy behavior,
or with a compliance guarantee. Review the actual native field collection and the
effective platform configuration.

Sources: [KMP privacy helpers](../kmp/src/commonMain/kotlin/com/amplitude/kmp/Configuration.kt),
[Android privacy implementation][android-tracking], [Swift privacy implementation][swift-tracking].

## 4. Events, Identify, Revenue, and Callbacks

### Event Models and Field Conversion

| Area | Difference |
|---|---|
| Type identity | KMP models are separate classes, not native type aliases. Conversion creates native BaseEvent objects rather than preserving native subclasses or subclass overrides. |
| Typed event classes | KMP has IdentifyEvent and RevenueEvent, but no shared GroupIdentifyEvent or complete mirror of native specialized event classes such as Swift ScreenViewedEvent/DeepLinkOpenedEvent. `groupIdentify()` itself is available. |
| Event mutation | Native `track(BaseEvent, options, callback)` mutates the supplied native object. KMP forwards a newly constructed native event; later SDK enrichment is not written back to the original KMP object. |
| `BaseEvent.isValid()` | KMP rejects blank IDs; the audited native base checks accept any non-null ID. This comparison concerns the helper, not proof that all such IDs will be accepted by server-side validation. |
| KMP to Android fields | Four property maps and the listed EventOptions fields are mapped. |
| KMP to iOS fields | Four property maps are now written through per-key setters. `extra`, `appSetId`, and `androidId` are not mapped; the Android-specific identifiers have no corresponding properties in the audited Objective-C EventOptions. |
| Swift-only serialization/helpers | Codable/Swift event construction and Android JSON/clone utilities are not a shared serialization API. Kotlin data-class helpers are not native serialization equivalents. |
| Arbitrary property values | The iOS `toNSObject()` helper handles primitives, maps, and lists, but falls back to `toString()` for unsupported values. Do not assume arbitrary Kotlin objects/array shapes serialize exactly like native Swift values. |

### Callbacks: C1 and C2

- **Android (C1 fixed):** the mapper copies all shared public event fields, including
  the four property maps, IDs, session, revenue, device/context fields, plan,
  ingestion metadata, extra properties, and callable event callbacks. The returned
  object is still a KMP BaseEvent, not the original native instance or subclass;
  native internal retry state is not exposed. Property maps are not deep snapshots.
- **iOS:** the mapper copies the actual event name and readable scalar fields,
  plan, and ingestion metadata. It cannot enumerate `AMPProperties`, so the four
  property maps are `null`. `extra` and Android-specific identifier fields are
  also not reconstructed. The fabricated `"callback_event"` has been removed.
- Status and message are forwarded on both platforms; on iOS the optional interop
  message falls back to an empty string. An unexpected null native event is not
  forwarded because the shared callback requires a non-null event.
- The callback represents an individual event result, not proof that every event
  in the SDK queue was uploaded successfully.

See [callback limitations](known-limitations.md#ios-callback-event-properties).
The iOS limitation is explicitly deferred: the current integration does not add a
Swift bridge, interception plugin, second SDK instance, or fork. An external Swift
extension cannot access the official wrapper's internal event; see
[bridge boundary and current status](known-limitations.md#current-status-and-bridge-boundary).
Sources: [Android event mapper](../kmp/src/androidMain/kotlin/com/amplitude/kmp/mappings/android/EventMapper.kt),
[iOS event mapper](../kmp/src/iosMain/kotlin/com/amplitude/kmp/mappings/ios/EventMapper.kt),
[Objective-C event properties][objc-properties].

### Identify

The ordinary `identify(Identify)` route maps set, setOnce, add, append, prepend,
preInsert, postInsert, remove, unset, and clearAll to native builder operations.
That does not make every model/helper representation interchangeable:

- Native Android exposes `Identify.properties` and the top-level
  `IdentifyOperation` enum. KMP exposes `getOperations()` and a nested `Identify.Operation`.
- KMP's operation snapshot represents clearAll as `{"$clearAll":{"-":"-"}}`.
  Native Android's actual event properties use `{"$clearAll":"-"}`. The ordinary
   builder mappers call native `clearAll()` and avoid this discrepancy, but the
   raw `toIdentifyEvent()` helper does not.
- The iOS mapper bridges values through `toNSObject()` rather than sharing the
  original Kotlin values with the Swift Identify object.

Sources: [KMP Identify](../kmp/src/commonMain/kotlin/com/amplitude/kmp/events/Identify.kt),
[Android Identify mapper](../kmp/src/androidMain/kotlin/com/amplitude/kmp/mappings/android/IdentifyMapper.kt),
[native Android Identify][android-identify].

### Revenue: R1

- `revenue(Revenue, options)` uses the native Revenue builder and its price validation.
- Android conversion now maps receipt and signature independently, matching the
  native public properties. The earlier requirement that both be non-null has
  been removed; a single supplied field is no longer discarded.
- iOS conversion now sets receipt and `receiptSig` independently when non-null.
  The earlier signature omission (R1) is fixed; no receipt validation logic is added
  by the wrapper.
- Revenue custom properties are now mapped to iOS through `AMPProperties.set()`.
- The shared `revenue(RevenueEvent)` route constructs a native BaseEvent and calls
  `track()`, rather than forwarding a native RevenueEvent. Typed plugin dispatch
  is therefore not equivalent.

Sources: [KMP Revenue](../kmp/src/commonMain/kotlin/com/amplitude/kmp/events/Revenue.kt),
[Android Revenue mapper](../kmp/src/androidMain/kotlin/com/amplitude/kmp/mappings/android/RevenueMapper.kt),
[iOS event/Revenue mapper](../kmp/src/iosMain/kotlin/com/amplitude/kmp/mappings/ios/EventMapper.kt),
[Objective-C Revenue][objc-revenue].

## 5. Plugin Capabilities

| Capability | Native SDKs | KMP |
|---|---|---|
| Initialization and generic event execution | Native setup/execute hooks | Exposes `setup(Amplitude)` and `execute(BaseEvent)`; setup forwarding is fixed. |
| Removal by registered instance | Native identity-based registration | Caches the native adapter; ordinary add/remove now uses the same adapter. |
| Duplicate registration | Native behavior depends on platform and plugin identity/name | Adding the same KMP instance twice to one wrapper is intentionally ignored. |
| Cleanup | Android `teardown()`; Swift native plugin cleanup | No KMP teardown hook to release developer-owned resources when removed. |
| Typed event hooks | Native EventPlugin track/identify/groupIdentify/revenue and flush hooks | Only generic `execute()` is exposed; the native EventPlugin/DestinationPlugin hierarchy is not reproduced. |
| Observe plugins | Android ObservePlugin has identity-change callbacks; Swift plugins have identity/session/opt-out hooks | KMP `Type.OBSERVE` is only an enum mapping, not those interfaces. It does not provide equivalent notifications. |
| Universal/context/signal plugins | Swift UniversalPlugin/PluginHost and interface signals; Android native signal facilities | Not exposed by the shared Plugin interface. |
| Native plugin installation/access | Native plugin types and host APIs | Cannot directly pass a native plugin to shared `add(Plugin)`, or obtain the underlying SDK through a public wrapper accessor. |

### P1: iOS Conversion Can Lose Properties and Typed Events

The KMP adapter creates a new KMP BaseEvent without the native property maps, runs
the developer plugin, and rebuilds a new native BaseEvent. Existing property maps
are lost even when the plugin returns its argument unchanged.

There is an additional native subclass constraint: Objective-C's plugin wrapper
handles Identify, GroupIdentify, and Revenue callbacks by casting the returned
native event back to the respective subclass. KMP returns an `AMPBaseEvent` instead,
so those casts can fail and yield `nil`, dropping that typed event from the chain.
This follows from the audited Swift Mediator/ObjCPluginWrapper implementation;
it is not a new recorded runtime reproduction in this audit.

Swift's native dictionaries are fully readable; the public `AMPProperties` wrapper
only exposes get/set/remove for known keys. The interface restriction and this
KMP adapter's reconstruction strategy are separate issues. See the existing
[iOS plugin warning](known-limitations.md#ios-custom-plugin-property-loss).

### P2: Android Conversion Also Has Partial Metadata Coverage

The Android plugin adapter preserves all four property maps, so it does **not** have
the same map-enumeration problem. However, it omits `plan`, `ingestionMetadata`,
`extra`, and `callback` on the native-to-KMP leg, and creates a plain native BaseEvent
on the return leg. Those values and native subclass identity are not preserved.

### Registration Boundaries

The current cache lock protects the adapter list, not the entire native add/remove
operation. Concurrent registration/removal, self-removal during `setup()`, or an
exception during setup can leave cache/native registration state out of sync.
These edge cases remain unresolved; ordinary sequential registration is not proof
of transactional or reentrant lifecycle behavior.

Sources: [KMP Plugin](../kmp/src/commonMain/kotlin/com/amplitude/kmp/plugins/Plugin.kt),
[Android adapter](../kmp/src/androidMain/kotlin/com/amplitude/kmp/mappings/android/PluginMapper.kt),
[iOS adapter](../kmp/src/iosMain/kotlin/com/amplitude/kmp/mappings/ios/PluginMapper.kt),
[Android plugin interfaces][android-plugin], [Swift plugin interfaces][swift-types],
[Swift mediator][swift-mediator], [Objective-C plugin wrapper][objc-plugin].

## 6. Removed KMP Coroutine Conveniences

The wrapper no longer exposes KMP-only `awaitTrack()` (both overloads),
`awaitIdentify()` (both overloads), `awaitRevenue()`, `awaitSetUserId()`,
`awaitSetDeviceId()`, or `awaitReset()` extensions. Android-only `invokeAndAwait()`
and `awaitInitialization()` conveniences have also been removed. These were
wrapper-defined APIs, not native methods with the same signatures.

Use `track()`, `identify()`, `revenue()`, `setUserId()`, `setDeviceId()`, and `reset()`
directly, and construct instances with `Amplitude(configuration)`. The optional
event callback on `track(BaseEvent, options, callback)` remains available; KMP does
not convert callbacks to suspend functions or add completion guarantees.

The removed Identify/Revenue suspend helpers used `track()` rather than the native
Identify/Revenue entry points (K1 in the original audit). Removing them also removes
that alternate path and the Map helper's null-to-empty-string conversion.

`awaitFlush()` has been removed. Neither audited native public `Amplitude.flush()`
entry point exposes completion of all uploads; `flush()` requests background work.
Swift's internal pipeline completion callback is not a public Objective-C API and
does not justify advertising a shared wait-for-all-upload-success operation.

See the [1.1.0 release notes](../CHANGELOG.md#110---2026-11-07) for migration details.

## 7. Intentional Omissions and Previously Fixed Issues

### Intentional API Differences

- Deprecated `trackingSessionEvents`, `defaultTracking`, and `DefaultTrackingOptions`
  are not provided. Use `autocapture` and omit SESSIONS to disable session events.
- Deprecated native aliases such as `logEvent` / `logRevenue` are not mirrored.
- Removed `awaitFlush()` is not replaced by a dummy success result or fixed delay.
- Removing public constructor parameters/types or extensions is a **source and
  potentially binary compatibility break**. Rebuild dependent code and version
  releases appropriately; Kotlin data-class constructor/copy signatures also change.

### Fixed in the Audited Repository

| Former issue | Current state | Relevant commit |
|---|---|---|
| iOS outbound event user/group maps and Revenue custom properties omitted | Per-key property writes added | `11621bd` |
| Local Core checkout required | Explicit local Core declaration removed; native Core remains transitive | `3619b93` |
| iOS array group setter kept only the first value | Uses native `groupNames` with the complete list | `d92d6c6` |
| Plugin setup skipped / removal constructed another adapter | Setup forwarded, adapter retained, same-instance duplicate add ignored | `359c68f` |
| Android log level and iOS autocapture ignored | Mapped; iOS IDs also applied after native construction with the limitations above | `5bf7243` |
| iOS callback always returned `callback_event` | Real type and readable fields copied; maps remain unavailable | `c189927` |
| `awaitFlush()` returned immediate success | API and misleading example removed | `33affb6` |

These fixes do not eliminate the other differences in this report. In particular,
outbound property writes do not solve inbound property enumeration, and successful
compilation/linking does not establish native behavioral equivalence.

## 8. Practical Guidance

1. Treat the common API as a **subset plus wrapper conveniences**, not a 100% native API mirror.
2. Use the current native versions and actual CocoaPods lockfile when assessing availability.
3. Configure the SDK before construction; do not use the original KMP Configuration as a live control object.
4. Avoid custom KMP plugins on iOS when properties or typed Identify/Revenue events must survive.
5. Observe the documented callback field limits, especially on iOS. The KMP-only suspend helpers have been removed; use the ordinary APIs without assuming they wait for native asynchronous work to complete.
6. Use native integrations for capabilities not exposed here, with separate integration work; the wrapper does not expose a public native-instance escape hatch.
7. Validate application event payloads and privacy settings on both platforms. Address the current gaps separately rather than interpreting this report as a claim they are fixed.

## Native Source References

The Android comparison also used the immutable published
[Android sources JAR](https://repo.maven.apache.org/maven2/com/amplitude/analytics-android/1.22.4/analytics-android-1.22.4-sources.jar)
and [Core sources JAR](https://repo.maven.apache.org/maven2/com/amplitude/analytics-core/1.22.4/analytics-core-1.22.4-sources.jar).

- [Android core Amplitude API][android-amplitude]
- [Android platform Amplitude API][android-specific-amplitude]
- [Android configuration][android-configuration]
- [Android tracking options][android-tracking]
- [Android automatic capture][android-autocapture]
- [Android Identify][android-identify]
- [Android plugin interfaces][android-plugin]
- [Swift Amplitude API][swift-amplitude]
- [Swift configuration][swift-configuration]
- [Swift tracking options][swift-tracking]
- [Swift automatic capture][swift-autocapture]
- [Swift plugin interfaces][swift-types]
- [Swift mediator][swift-mediator]
- [Objective-C Amplitude API][objc-amplitude]
- [Objective-C configuration][objc-configuration]
- [Objective-C properties][objc-properties]
- [Objective-C Revenue][objc-revenue]
- [Objective-C plugin wrapper][objc-plugin]
- [Objective-C tracking options, baseline][objc-tracking]

[android-amplitude]: https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/core/src/main/java/com/amplitude/core/Amplitude.kt
[android-specific-amplitude]: https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/android/src/main/java/com/amplitude/android/Amplitude.kt
[android-configuration]: https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/android/src/main/java/com/amplitude/android/Configuration.kt
[android-tracking]: https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/android/src/main/java/com/amplitude/android/TrackingOptions.kt
[android-autocapture]: https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/android/src/main/java/com/amplitude/android/AutocaptureOptions.kt
[android-identify]: https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/core/src/main/java/com/amplitude/core/events/Identify.kt
[android-plugin]: https://github.com/amplitude/Amplitude-Kotlin/blob/v1.22.4/core/src/main/java/com/amplitude/core/platform/Plugin.kt
[swift-amplitude]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/Amplitude.swift
[swift-configuration]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/Configuration.swift
[swift-tracking]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/TrackingOptions.swift
[swift-autocapture]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/AutocaptureOptions.swift
[swift-types]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/Types.swift
[swift-mediator]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/Mediator.swift
[objc-amplitude]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/ObjC/ObjCAmplitude.swift
[objc-configuration]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/ObjC/ObjCConfiguration.swift
[objc-properties]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/ObjC/ObjCProperties.swift
[objc-revenue]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/ObjC/ObjCRevenue.swift
[objc-plugin]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/ObjC/ObjCPlugin.swift
[objc-tracking]: https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/ObjC/ObjCTrackingOptions.swift
