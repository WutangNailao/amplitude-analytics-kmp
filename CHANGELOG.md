# Changelog

Notable changes to amplitude-analytics-kmp are documented here.

## [1.0.5] - 2026-10-29

### Breaking changes

This release contains public API and Maven coordinate changes despite using a patch
version number. Update dependencies and affected calls, and rebuild consumers.

- Change Maven coordinates from `me.nathanfallet.amplitude:analytics-kmp` to
  **`io.github.wutangnailao:amplitude-analytics-kmp`**. Kotlin imports remain under
  `com.amplitude.kmp`; the iOS framework remains named `AmplitudeKMP`.
- Remove `Amplitude.getInstance()` and its global cached instance. Keep the result
  of `Amplitude(configuration)` and pass it explicitly or through dependency injection.
- Remove the `awaitFlush()` coroutine extension. Use `flush()` to request background
  uploads; it does not wait for upload completion and has no success result.
- Remove `Configuration.trackingSessionEvents`, `Configuration.defaultTracking`,
  and `DefaultTrackingOptions`. Use `Configuration.autocapture`; omit
  `AutocaptureOption.SESSIONS` to disable automatic session events, or use
  `emptySet()` to disable all autocapture.

### Fixed

- Write `BaseEvent.userProperties`, `groups`, and `groupProperties` to native iOS
  events through the Objective-C property wrapper's per-key setters.
- Preserve Revenue custom properties when converting to the native iOS Revenue object.
- Forward every value in the iOS `setGroup(groupType, groupName: Array<String>, options)`
  overload using the native `groupNames` interface instead of keeping only the first value.
- Forward native plugin initialization callbacks to KMP `Plugin.setup()` on both platforms.
- Retain native plugin adapters so ordinary `remove(plugin)` calls use the registered
  adapter instead of constructing a different object.
- Apply Android `Configuration.logLevel` through the native logger provider.
  `LogLevel.LOG` maps to INFO; DEBUG and VERBOSE map to DEBUG.
- Apply `Configuration.autocapture` to the native iOS configuration.
- Apply configured iOS `deviceId` and `sessionId` after constructing the native SDK.
  The session operation is asynchronous and can emit session events; this is not
  equivalent to synchronous initial assignment.
- Stop mapping iOS `AutocaptureOption.DEEP_LINKS` to screen-view capture. The option
  is ignored on iOS because the native SDK has no equivalent autocapture switch.
- Return the actual native iOS event type and readable event fields in callbacks
  instead of a fabricated `callback_event` object.

### Changed

- Rename the Gradle root project to `amplitude-analytics-kmp` and update project,
  publishing, and documentation links to the new repository.
- Ignore duplicate additions of the same KMP plugin instance to one SDK instance.
  The plugin can be added again after removal.
- Remove the explicit local `../AmplitudeCore-Swift` CocoaPods dependency. Native
  AmplitudeCore is still supplied transitively by AmplitudeSwift.
- Derive the CocoaPods package version from the Gradle project version.
- Make the sample's Maven dependency use the project's group and version.
- Split the README into a project overview, [usage guide](docs/usage.md), and
  [contribution guide](CONTRIBUTING.md).
- Document the iOS plugin and callback restrictions in [Known Limitations](docs/known-limitations.md).

### Migration

Use the new dependency coordinate:

```kotlin
implementation("io.github.wutangnailao:amplitude-analytics-kmp:1.0.5")
```

Keep your own SDK instance and configure automatic capture explicitly:

```kotlin
val amplitude = Amplitude(
    Configuration(
        apiKey = amplitudeApiKey,
        androidContext = platformContext, // Android Application context; null on iOS.
        autocapture = setOf(
            AutocaptureOption.APP_LIFECYCLES,
            AutocaptureOption.SCREEN_VIEWS,
        ),
    )
)

amplitude.track("App Opened")
amplitude.flush() // Requests background uploads; does not wait for completion.
```

### Remaining limitations

- The outbound iOS property fixes do not make native-to-KMP property enumeration
  available. iOS callback property maps remain unavailable, and custom KMP plugins
  can still lose event properties. Refer to [Known Limitations](docs/known-limitations.md).
- Plugin registration is not transactional across concurrent add/remove calls or
  reentrant removal during setup.
- Android callbacks and plugin conversions still do not preserve every native
  event field. This release does not claim complete native API or behavior parity.

## [1.0.4] - 2026-07-25

### Fixed

- Remove the explicit AnalyticsConnector cinterop declaration to avoid duplicate
  symbols when linking this library together with experiment-kmp. The native pod
  remains an AmplitudeSwift transitive dependency.

## [1.0.3] - 2026-04-01

### Fixed

- Set the native iOS server-zone property through KVC with its raw integer value
  to avoid Swift 6 / Xcode 26 enum interoperability build failures.

### Changed

- Update installation examples to use Amplitude-Swift 1.16.2.

## [1.0.2] - 2026-03-31

### Added

- Expose `Amplitude.getInstance()` in the common API. This historical API is not
  available in 1.0.5; see the migration notes above.

### Changed

- Move Maven publishing from `digital.guimauve.amplitude` to `me.nathanfallet.amplitude`.
- Update project and publisher metadata to the upstream maintainer's repository.

### Fixed

- Adjust iOS server-zone enum conversion for Swift 6 / Xcode 26 compatibility.

## [1.0.1] - 2025-12-30

### Fixed

- Declare `Amplitude(configuration)` in the common companion object and align
  Android/iOS `expect` / `actual` initialization declarations.
- Correct iOS offline configuration conversion and other platform compilation issues.

## [1.0.0] - 2025-12-30

### Added

- Initial Kotlin Multiplatform wrapper around the official Android and Swift
  Analytics SDKs, with Android and iOS device/simulator targets.
- Shared event tracking, Identify, Revenue, identity management, group operations,
  configuration models, and plugin interfaces.
- Coroutine convenience extensions and Android/iOS sample integrations.

### Fixed

- Carry user/device IDs and other readable native event fields through the iOS
  plugin conversion rather than reconstructing events from their type alone.
- Remove temporary iOS event/debug logging.

[1.0.5]: https://github.com/wutangnailao/amplitude-analytics-kmp/compare/1.0.4...main
[1.0.4]: https://github.com/nathanfallet/amplitude-kmp/compare/1.0.3...1.0.4
[1.0.3]: https://github.com/nathanfallet/amplitude-kmp/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/nathanfallet/amplitude-kmp/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/nathanfallet/amplitude-kmp/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/nathanfallet/amplitude-kmp/tree/1.0.0
