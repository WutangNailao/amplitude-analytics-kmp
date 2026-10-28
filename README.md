# amplitude-analytics-kmp

[![Maven Central](https://img.shields.io/maven-central/v/io.github.wutangnailao/amplitude-analytics-kmp)](https://central.sonatype.com/artifact/io.github.wutangnailao/amplitude-analytics-kmp)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A community-maintained Kotlin Multiplatform wrapper for [Amplitude Analytics](https://amplitude.com).
It wraps the official Android and Swift SDKs and provides a shared Kotlin API for Android and iOS applications.

## Why Amplitude KMP?

Track events, manage user identity, and record revenue from shared Kotlin code while
using the native Amplitude SDK on each platform. The API follows the Android SDK's
conventions; platform differences and unsupported legacy APIs are documented in the
[usage guide](docs/usage.md) and [known limitations](docs/known-limitations.md).

## Features

- **Shared Kotlin API** for Android and iOS
- **Native SDK integration** with the official Android and Swift SDKs
- **Event tracking** with event properties and callbacks
- **User properties** through the type-safe Identify API
- **Revenue tracking** and group operations
- **Coroutines support** through suspend function extensions
- **Custom plugins**, with documented iOS limitations
- **Session management**, automatic capture, and offline event queueing

## Platforms Supported

| Platform      | Minimum Version     |
|---------------|---------------------|
| Android       | API 21 (Android 5.0) |
| iOS           | iOS 13.0            |
| iOS Simulator | iOS 13.0            |

## Documentation

- [Usage Guide](docs/usage.md) — installation, quick start, configuration, API examples, migration, and troubleshooting
- [Known Limitations](docs/known-limitations.md) — iOS plugin and callback restrictions
- [Contributing](CONTRIBUTING.md) — project structure, building, testing, publishing, and contributing
- [Changelog](CHANGELOG.md) — release notes and migration information
- [Sample Applications](samples/README.md) — sample project overview
- [Official Amplitude Documentation](https://www.docs.developers.amplitude.com/)

## Native SDK Dependencies

The current build declares these native SDK versions:

- **Android**: [`com.amplitude:analytics-android:1.22.4`](https://github.com/amplitude/Amplitude-Kotlin)
- **iOS**: [`AmplitudeSwift ~> 1.16.2`](https://github.com/amplitude/Amplitude-Swift) through CocoaPods

See [gradle/libs.versions.toml](gradle/libs.versions.toml) for the version catalog.
The iOS constraint permits compatible patch releases; the resolved version depends on the CocoaPods lockfile.

## Message to the Amplitude Team

Hello Amplitude Team, I've made this Kotlin Multiplatform SDK to help developers use Amplitude in their multiplatform
apps because of [this issue](https://github.com/amplitude/Amplitude-Kotlin/issues/114). Please consider officially
supporting and maintaining this SDK as part of the Amplitude family. I'll be happy to talk about this, you can reach me
out from [my GitHub profile](https://github.com/nathanfallet). We could transfer this repository to the Amplitude
organization and publish it under the official `com.amplitude` Maven group. Thank you!

## Support

- [GitHub Issues](https://github.com/wutangnailao/amplitude-analytics-kmp/issues)
- [Amplitude Help Center](https://help.amplitude.com/)
- [Community Forum](https://community.amplitude.com/)
- [Amplitude Email Support](mailto:support@amplitude.com)

## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).

## Related Projects

- [Amplitude-Kotlin](https://github.com/amplitude/Amplitude-Kotlin) — native Android SDK
- [Amplitude-Swift](https://github.com/amplitude/Amplitude-Swift) — native iOS SDK
- [Amplitude-TypeScript](https://github.com/amplitude/Amplitude-TypeScript) — JavaScript/TypeScript SDK
- [Amplitude-Flutter](https://github.com/amplitude/Amplitude-Flutter) — Flutter SDK
- [Amplitude-React-Native](https://github.com/amplitude/Amplitude-React-Native) — React Native SDK
