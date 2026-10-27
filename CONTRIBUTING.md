# Contributing

[Project overview](README.md) · [Usage guide](docs/usage.md) · [Known limitations](docs/known-limitations.md)

## Contents

- [Project Structure](#project-structure)
- [Building](#building)
- [Running Tests](#running-tests)
- [Publishing to Maven Local](#publishing-to-maven-local)
- [Code Style](#code-style)
- [Contributing](#contributing)
- [Release History](#release-history)

## Project Structure

```text
amplitude-kmp/
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

See [GitHub Releases](https://github.com/nathanfallet/amplitude-kmp/releases) for published release notes
and the [commit history](https://github.com/nathanfallet/amplitude-kmp/commits/main/) for changes.
