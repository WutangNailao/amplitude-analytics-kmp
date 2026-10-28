# Contributing

[Project overview](README.md) · [Usage guide](docs/usage.md) · [Known limitations](docs/known-limitations.md)

## Contents

- [Project Structure](#project-structure)
- [Building](#building)
- [Running Tests](#running-tests)
- [Continuous Integration](#continuous-integration)
- [Publishing to Maven Local](#publishing-to-maven-local)
- [Code Style](#code-style)
- [Contributing](#contributing)
- [Release History](#release-history)

## Project Structure

```text
amplitude-analytics-kmp/
├── kmp/                           # Main KMP module
│   ├── src/
│   │   ├── commonMain/            # Platform-agnostic code
│   │   │   └── kotlin/com/amplitude/kmp/
│   │   │       ├── Amplitude.kt   # Main API
│   │   │       ├── Configuration.kt
│   │   │       ├── events/        # Event models
│   │   │       ├── plugins/       # Plugin system
│   │   │       ├── ktx/           # Coroutines extensions
│   │   │       └── utils/         # Utilities
│   │   ├── androidMain/           # Android implementation and type mappers
│   │   ├── iosMain/               # iOS implementation and type converters
│   │   └── iosTest/               # iOS tests
│   └── build.gradle.kts
├── samples/                       # Sample applications
├── docs/                          # Usage and known limitations
├── CONTRIBUTING.md                # Development and contribution guide
└── README.md                      # Project overview
```

## Building

Run commands from the repository root:

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew :kmp:testDebugUnitTest
./gradlew :kmp:iosSimulatorArm64Test
```

## Continuous Integration

[GitHub Actions CI](.github/workflows/ci.yml) runs on pull requests, pushes to `main`,
and manual dispatch. It does not publish packages or require publishing secrets.

| Check | Environment | Scope |
|---|---|---|
| Code style | Ubuntu 24.04, JDK 21 | Ktlint checks for the root project, library, and sample sources/scripts; no automatic formatting |
| Unit tests (Android) | Ubuntu 24.04, JDK 21 | Debug and Release unit-test tasks for `:kmp` |
| Unit tests (iOS) | macOS 15 (ARM64), Xcode 16.4, JDK 21, CocoaPods 1.16.2 | Run `:kmp:iosSimulatorArm64Test` |
| Android Lint | Ubuntu 24.04, JDK 21 | Debug and Release lint tasks for `:kmp` |
| Android build | Ubuntu 24.04, JDK 21, Gradle Wrapper | Debug and Release AARs for `:kmp` |
| iOS build | macOS 15 (ARM64), Xcode 16.4, JDK 21, CocoaPods 1.16.2 | Link Debug frameworks for `iosArm64`, `iosSimulatorArm64`, and `iosX64` |

The workflow uses the standard Gradle setup/cache action and uploads available
Android AARs, lint/style/test reports, and the resolved CocoaPods lockfile as short-lived artifacts.
Actions are pinned to commit hashes. Repository access is read-only, checkout does
not retain Git credentials, and a newer run cancels older runs for the same PR/ref.

Code style, unit-test, Lint, and build checks run as independent matrix jobs with
`fail-fast: false`, so one failure does not cancel the other checks. Each job calls
the existing Gradle tasks directly. Failures fail the job; no checks are suppressed,
no build/test step uses `continue-on-error`, and CI does not autoformat source files.

CI checks the library and Kotlin formatting, not the sample app's Maven consumption
or package publication. There are currently no Android unit-test source files;
the Android test tasks still check their configured dependencies and test setup.

## Publishing to Maven Local

```bash
./gradlew publishToMavenLocal
```

## Code Style

This project follows [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).

## Contributing

We welcome contributions! To contribute:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

## Release History

See [CHANGELOG.md](CHANGELOG.md) for version changes and migration notes.

See [GitHub Releases](https://github.com/wutangnailao/amplitude-analytics-kmp/releases) for published release notes
and the [commit history](https://github.com/wutangnailao/amplitude-analytics-kmp/commits/main/) for changes.
