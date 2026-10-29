# Contributing

[Project overview](README.md) · [Usage guide](docs/usage.md) · [Known limitations](docs/known-limitations.md)

## Contents

- [Project Structure](#project-structure)
- [Building](#building)
- [Running Tests](#running-tests)
- [Continuous Integration](#continuous-integration)
- [Publishing to Maven Local](#publishing-to-maven-local)
- [Automated Releases](#automated-releases)
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
Actions use version tags. Repository access is read-only, checkout does
not retain Git credentials, and a newer run cancels older runs for the same PR/ref.

Android/style checks run as independent matrix jobs with `fail-fast: false`.
Only after they pass does one macOS job run both iOS tests and Debug framework
builds. This avoids spending macOS capacity on changes already rejected by Linux
checks, and shares CocoaPods setup, cinterop output, and the Gradle task graph.
Gradle's `--continue` allows independent iOS tasks to report failures in the same
invocation; any failure still fails the job. No build/test step uses
`continue-on-error`, and CI does not autoformat source files.

If branch protection previously required the separate `Unit tests (iOS)` and
`iOS build` matrix checks, replace them with the combined `iOS tests and build`
check. The Android check names are unchanged.

CI checks the library and Kotlin formatting, not the sample app's Maven consumption
or package publication. There are currently no Android unit-test source files;
the Android test tasks still check their configured dependencies and test setup.

## Publishing to Maven Local

```bash
./gradlew publishToMavenLocal
```

## Automated Releases

[Release workflow](.github/workflows/release.yml) runs the complete pipeline:
version review → CI checks → Maven Central → GitHub Release.

| Stage | Runner | Work |
|---|---|---|
| Metadata and quality checks | Ubuntu | Version/tag/changelog checks, formatting, Android tests, Lint, builds and Android Maven publication |
| Native build | **One macOS ARM64 job** | iOS simulator tests, all three architectures' Debug/Release frameworks, root metadata and iOS Maven publications |
| Review | Ubuntu | Merge staged files and check all five publications are present |
| Publish | Ubuntu | JReleaser signs files, generates checksums, uploads to Central and waits for validation/publication |
| GitHub Release | Ubuntu | Create release notes and attach the same staged Android AAR |

The macOS job starts only after Linux checks pass and combines iOS tests, framework
linking and publication generation in one Gradle invocation. The normal CI's iOS
job is skipped in this workflow to avoid duplicate work. Publishing uses the
already-built files: no platform is rebuilt, and no macOS runner is occupied
while waiting for Central. Build and review jobs receive no publishing secrets.

[`jreleaser.yml`](jreleaser.yml) configures signing and Maven Central deployment.
JReleaser is invoked with `deploy`, not `full-release`; GitHub Releases are created
separately with the GitHub CLI. We do not maintain a custom publishing client.
Its PomChecker validates the root POM locally; Android/native POM validation is
left to Central because PomChecker does not recognize `aar`/`klib` packaging.

### One-time setup

1. Verify the `io.github.wutangnailao` namespace in the [Central Portal](https://central.sonatype.com/).
2. Generate a **Central Portal user token**, not your account login password.
3. Create a GPG signing key and distribute its public key as required by
   [Maven Central](https://central.sonatype.org/publish/requirements/gpg/).
4. Create a GitHub Environment named **`release`**, with these environment secrets:

   | Secret | Value |
   |---|---|
   | `MAVEN_CENTRAL_USERNAME` | Central Portal token username |
   | `MAVEN_CENTRAL_PASSWORD` | Central Portal token password |
   | `SIGNING_IN_MEMORY_KEY` | Complete ASCII-armored private signing key |
   | `SIGNING_IN_MEMORY_KEY_PASSWORD` | Signing-key passphrase; empty for an unencrypted key |

   Allow version tags in this environment (and `main` for manual publishing from
   `main` with the `tag` input). Optional required reviewers add human approval.
   Restrict version-tag creation through repository rulesets.

The GPG import action handles key import and cleanup; JReleaser uses GnuPG for
signing and signature verification. GitHub uses the built-in `GITHUB_TOKEN`, not
a PAT. Only the GitHub Release job has `contents: write`. Secret names are unchanged.
If the Maven group changes, update both `gradle.properties` and `jreleaser.yml`.

### Prepare and run

1. Set `VERSION_NAME` to a stable `X.Y.Z` version.
2. Complete `## [X.Y.Z] - YYYY-MM-DD` in `CHANGELOG.md`; `Unreleased`, future
   dates and empty notes fail review.
3. Merge the release commit into `main`.
4. Push one matching tag, for example `git tag 1.0.5` then `git push origin 1.0.5`.
   `v1.0.5` also works; do not push both spellings for the same version.

For manual execution, select **Actions → Release → Run workflow**:
- `mode: check`: run checks and stage unsigned artifacts without secrets or publishing.
  An optional `tag` selects an existing tag; otherwise it checks the selected branch.
- `mode: publish`: publish after checks pass. Supply an existing version tag, or
  select that tag as the workflow ref. The commit must be in `main` history.

Manual dispatch requires the workflow on the default branch. The `release-maven`
artifact contains the merged, unsigned Maven publications for inspection. Reports,
artifacts and JReleaser deployment information are retained for 14 days.

### Recovery

- A check/build failure can be retried with **Re-run failed jobs**.
- If upload fails or times out, inspect the Central Portal **before retrying**.
  A deployment may still be processing. Wait for it, manually publish a validated
  deployment if needed, or resolve a failed deployment. There is no custom resume logic.
- Never move a published tag or overwrite an immutable Central version. JReleaser
  checks for already-published artifacts, but do not blindly retry a pending upload.
- If only GitHub Release creation failed, rerun that job. To recover manually after
  Central has published, download the staged AAR and `release-notes` artifacts and run
  `gh release create <tag> <aar> --verify-tag --notes-file <notes.md>`.
  Existing GitHub Releases are left unchanged; edit missing notes/assets manually.
- Active release workflows are never automatically cancelled. Publish versions one
  at a time; GitHub concurrency retains at most one pending run.

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
