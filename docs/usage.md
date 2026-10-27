# Usage Guide

Installation, configuration, and API examples for Amplitude KMP.

[Project overview](../README.md) · [Known limitations](known-limitations.md) · [Contributing](../CONTRIBUTING.md)

## Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Core Features](#core-features)
- [Advanced Features](#advanced-features)
- [Migration from Android SDK](#migration-from-android-sdk)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Known Issues & API Compatibility](#known-issues--api-compatibility)
- [API Reference](#api-reference)

## Installation

### Gradle (Kotlin Multiplatform)

Add to your `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("me.nathanfallet.amplitude:analytics-kmp:1.0.3")
        }
    }
}
```

### Android Only

```kotlin
dependencies {
    implementation("me.nathanfallet.amplitude:analytics-kmp:1.0.3")
}
```

### iOS - Swift Package Manager (Recommended)

The KMP SDK wraps the native Amplitude Swift SDK. Add it via SPM in your Xcode project:

1. In Xcode: **File → Add Package Dependencies**
2. Enter: `https://github.com/amplitude/Amplitude-Swift`
3. Version: 1.16.2

Then add your Kotlin framework to the Xcode project via Gradle's `embedAndSignAppleFrameworkForXcode` task.

## Quick Start

### 1. Initialize in Shared Code (commonMain)

Create platform-agnostic configuration:

```kotlin
// commonMain/AmplitudeConfig.kt
expect val amplitudeApiKey: String
expect val platformContext: Any?  // Android Context or null for iOS
```

Implement for each platform:

```kotlin
// androidMain/AmplitudeConfig.android.kt
import android.content.Context

internal var androidContext: Context? = null
actual val amplitudeApiKey: String = "YOUR_ANDROID_API_KEY"
actual val platformContext: Any? get() = androidContext

// Set in Application.onCreate()
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        androidContext = applicationContext
    }
}
```

```kotlin
// iosMain/AmplitudeConfig.ios.kt
actual val amplitudeApiKey: String = "YOUR_IOS_API_KEY"
actual val platformContext: Any? = null  // iOS doesn't need context
```

Initialize Amplitude once in shared code:

```kotlin
// commonMain - In your App composable or init function
import com.amplitude.kmp.Amplitude
import com.amplitude.kmp.Configuration
import com.amplitude.kmp.AutocaptureOption

val configuration = Configuration(
    apiKey = amplitudeApiKey,
    androidContext = platformContext  // Only used on Android
).apply {
    autocapture = setOf(
        AutocaptureOption.SESSIONS,
        AutocaptureOption.APP_LIFECYCLES,
        AutocaptureOption.SCREEN_VIEWS
    )
    flushQueueSize = 30
    flushIntervalMillis = 30000
}

val amplitude = Amplitude(configuration)
amplitude.setUserId("user-123")
```

### 2. Track Events

```kotlin
// Track simple event
amplitude.track("Button Clicked")

// Track event with properties
amplitude.track(
    "Purchase Completed", mapOf(
        "amount" to 99.99,
        "currency" to "USD",
        "items" to 3
    )
)

// Set user properties
val identify = Identify()
    .set("email", "user@example.com")
    .set("plan", "premium")
    .add("loginCount", 1)
amplitude.identify(identify)

// Track revenue
val revenue = Revenue()
revenue.price = 9.99
revenue.productId = "premium_subscription"
revenue.quantity = 1
amplitude.revenue(revenue)
```

### 3. Platform-Specific Setup

**Android**: Store context in Application.onCreate()

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        androidContext = applicationContext
    }
}
```

**iOS**: No additional setup needed! Just use the shared Kotlin code.

## Core Features

### Event Tracking

```kotlin
// Simple event
amplitude.track("Button Clicked")

// Event with properties
amplitude.track(
    "Video Played", mapOf(
        "video_id" to "12345",
        "duration" to 180,
        "quality" to "HD"
    )
)

// Event with BaseEvent object
val event = BaseEvent(
    eventType = "Purchase",
    eventProperties = mutableMapOf(
        "amount" to 49.99,
        "items" to listOf("item1", "item2")
    )
)
amplitude.track(event)

// Event with callback
amplitude.track(event) { event, status, message ->
    if (status == 200) {
        println("Event tracked successfully")
    } else {
        println("Failed: $message")
    }
}
```

On iOS, callback events preserve the actual event type and readable fields, but their
property maps are unavailable. See [iOS callback limitations](known-limitations.md#ios-callback-event-properties).

### User Properties (Identify)

The Identify API provides 150+ type-safe methods for user property operations:

```kotlin
val identify = Identify()
    // Set properties
    .set("email", "user@example.com")
    .set("age", 25)
    .set("premium", true)

    // Set once (only if not already set)
    .setOnce("initial_source", "google")

    // Increment numeric properties
    .add("login_count", 1)
    .add("credits", 100)

    // Array operations
    .append("interests", "music")
    .prepend("notifications", "new_feature")
    .preInsert("tags", "vip")     // Insert at start if not present
    .postInsert("tags", "active")  // Insert at end if not present
    .remove("tags", "inactive")

    // Unset properties
    .unset("temporary_flag")

amplitude.identify(identify)

// Simple identify with map
amplitude.identify(
    mapOf(
        "email" to "user@example.com",
        "plan" to "premium"
    )
)
```

**Supported types** for each operation (14-15 overloads):

- Primitives: `Boolean`, `Double`, `Float`, `Int`, `Long`, `String`
- Collections: `Map<String, Any>`, `List<Any>`
- Arrays: `Array<Boolean>`, `Array<Double>`, `Array<Float>`, `Array<Int>`, `Array<Long>`, `Array<String>`
- Generic: `Any` (for set/setOnce only)

### Revenue Tracking

```kotlin
// Create revenue event
val revenue = Revenue()
revenue.productId = "premium_plan_monthly"
revenue.price = 9.99
revenue.quantity = 1
revenue.currency = "USD"
revenue.revenueType = "subscription"

// With receipt validation (Android)
revenue.receipt = "receipt_data"
revenue.receiptSig = "signature"

// Add custom properties
revenue.properties = mutableMapOf(
    "plan_name" to "Premium",
    "billing_cycle" to "monthly"
)

// Track the revenue
amplitude.revenue(revenue)

// Verify revenue is valid
if (revenue.isValid()) {
    amplitude.revenue(revenue)
} else {
    println("Invalid revenue: price is required")
}
```

### User & Device Management

```kotlin
// Set user ID
amplitude.setUserId("user_12345")

// Get user ID
val userId = amplitude.getUserId()

// Set device ID
amplitude.setDeviceId("device_67890")

// Get device ID
val deviceId = amplitude.getDeviceId()

// Reset user (clears userId, generates new deviceId)
amplitude.reset()

// Get current session ID
val sessionId = amplitude.sessionId
```

### Group Operations

```kotlin
// Set user to a single group
amplitude.setGroup("org_id", "amplitude")

// Set user to multiple groups
amplitude.setGroup("team_id", arrayOf("engineering", "product"))

// Set group properties
amplitude.groupIdentify(
    groupType = "org_id",
    groupName = "amplitude",
    groupProperties = mapOf(
        "plan" to "enterprise",
        "employees" to 500
    )
)

// Set group properties with Identify
amplitude.groupIdentify(
    groupType = "org_id",
    groupName = "amplitude",
    identify = Identify()
        .set("industry", "analytics")
        .add("num_projects", 1)
)
```

### Session Management

```kotlin
// Sessions are tracked automatically

// Access current session ID
val sessionId = amplitude.sessionId

// Configure session timeout (default: 5 minutes)
val amplitude = Amplitude(apiKey, context) {
    minTimeBetweenSessionsMillis = 300000  // 5 minutes
}

// Session events are tracked automatically
// Listen for: Amplitude.START_SESSION_EVENT and Amplitude.END_SESSION_EVENT
```

### Method Chaining

All main methods return the Amplitude instance for fluent chaining:

```kotlin
amplitude
    .setUserId("user_123")
    .track("App Opened")
    .identify(Identify().set("last_seen", System.currentTimeMillis()))
    .flush()
```

### Manual Flushing

```kotlin
// Flush events manually (normally happens automatically)
amplitude.flush()
```

## Advanced Features

### Coroutines Support

Optional suspend function extensions for async operations:

```kotlin
import com.amplitude.kmp.ktx.*

suspend fun trackUserJourney() {
    // Track with Result return type
    amplitude.awaitTrack(
        eventType = "Signup Started",
        eventProperties = mapOf("source" to "homepage")
    ).onSuccess {
        println("Event tracked!")
    }.onFailure { error ->
        println("Failed: ${error.message}")
    }

    // Identify user
    val identify = Identify()
        .set("email", "user@example.com")
        .set("signup_date", System.currentTimeMillis())
    amplitude.awaitIdentify(identify)

    // Track revenue
    val revenue = Revenue()
    revenue.price = 49.99
    revenue.productId = "premium_plan"
    amplitude.awaitRevenue(revenue)

    // Flush and wait for completion
    amplitude.awaitFlush()

    // Other suspend operations
    amplitude.awaitSetUserId("user_123")
    amplitude.awaitSetDeviceId("device_456")
    amplitude.awaitReset()
}
```

### Plugin System

> **iOS warning:** Custom KMP plugins can lose event, user, and group properties,
> even when they return the event unchanged. Avoid registering them on iOS when
> those properties must be preserved. See [Known Limitations](known-limitations.md#ios-custom-plugin-property-loss)
> before using the example below on iOS.

Extend functionality with custom plugins:

```kotlin
import com.amplitude.kmp.plugins.Plugin
import com.amplitude.kmp.events.BaseEvent

class CustomPlugin : Plugin {
    override val type = Plugin.Type.ENRICHMENT

    override fun setup(amplitude: Amplitude) {
        println("Plugin initialized")
    }

    override fun execute(event: BaseEvent): BaseEvent? {
        // Enrich event with custom data
        event.eventProperties?.put("app_version", "1.0.3")
        event.eventProperties?.put("build_number", "123")
        return event
    }
}

// Add plugin
amplitude.add(CustomPlugin())

// Remove plugin
amplitude.remove(plugin)
```

**Plugin Types:**

- `BEFORE` - Runs before event processing
- `ENRICHMENT` - Enriches events with additional data
- `DESTINATION` - Sends events to custom destinations
- `OBSERVE` - Observes events without modification
- `UTILITY` - Utility plugins

### Configuration Options

Comprehensive configuration for all use cases:

```kotlin
val amplitude = Amplitude(apiKey = "KEY", context = context) {
    // Flushing
    flushQueueSize = 30                    // Events before auto-flush
    flushIntervalMillis = 30000            // Auto-flush interval (30s)
    flushMaxRetries = 5                    // Max retry attempts
    flushEventsOnClose = true              // Flush on SDK close

    // Server & API
    serverZone = ServerZone.US             // US or EU data center
    serverUrl = "https://custom.api.com"   // Custom API endpoint
    useBatch = false                       // Use batch API

    // Logging
    logLevel = LogLevel.WARN               // Logging level
    // Levels: NONE, ERROR, WARN, LOG, DEBUG, VERBOSE

    // Session Management
    minTimeBetweenSessionsMillis = 300000  // Session timeout (5min)

    // Privacy & Tracking
    optOut = false                         // Opt-out of tracking
    trackingOptions = TrackingOptions()    // Customize tracked fields
    enableCoppaControl = false             // COPPA compliance mode

    // User & Device
    deviceId = "custom-device-id"          // Override device ID
    minIdLength = 5                        // Min length for IDs

    // Auto-capture (Android and iOS; supported options vary by platform)
    autocapture = setOf(
        AutocaptureOption.SESSIONS,              // Session events
        AutocaptureOption.APP_LIFECYCLES,        // App lifecycle
        AutocaptureOption.SCREEN_VIEWS,          // Screen views
        AutocaptureOption.ELEMENT_INTERACTIONS,  // Click events
    )

    // Android-specific
    useAdvertisingIdForDeviceId = false    // Use Android Ad ID
    useAppSetIdForDeviceId = false         // Use App Set ID
    locationListening = false              // Track location
    migrateLegacyData = true               // Migrate old SDK data

    // Advanced
    offline = false                        // Offline mode
    callback = { event, status, message -> // Global callback
        println("Event: ${event.eventType}, Status: $status")
    }
    partnerId = "partner_123"              // Partner attribution
    plan = Plan(...)                       // Tracking plan
    ingestionMetadata = IngestionMetadata(...) // Custom metadata
}
```

### Tracking Options (Privacy Controls)

Control which fields are tracked:

```kotlin
val trackingOptions = TrackingOptions()
    .disableIpAddress()
    .disableLatLng()
    .disableCity()
    .disableCountry()
    .disableRegion()
    .disableCarrier()
    .disableDeviceBrand()
    .disableDeviceModel()

val amplitude = Amplitude(apiKey, context) {
    trackingOptions = trackingOptions
}

// COPPA-compliant tracking
val coppaOptions = TrackingOptions.forCoppaControl()
```

### Event Options

Customize individual events:

```kotlin
val options = EventOptions().apply {
    userId = "custom_user_123"
    deviceId = "custom_device_456"
    timestamp = System.currentTimeMillis()
    sessionId = 1234567890
    locationLat = 37.7749
    locationLng = -122.4194
    ip = "192.168.1.1"
    plan = Plan(...)
    callback = { event, status, message ->
        println("Custom callback")
    }
}

amplitude.track("Custom Event", properties, options)
```

## Migration from Android SDK

The KMP SDK maintains **100% API compatibility** with the Android SDK. Migration requires **ONLY import changes**.

### Step 1: Update Dependencies

```kotlin
// Remove Android SDK
// implementation("com.amplitude:analytics-android:X.X.X")

// Add KMP SDK
implementation("me.nathanfallet.amplitude:analytics-kmp:1.0.3")
```

### Step 2: Update Imports

```kotlin
// Before (Android SDK)
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.core.events.Identify
import com.amplitude.core.events.Revenue
import com.amplitude.core.events.BaseEvent

// After (KMP SDK) - ONLY CHANGE NEEDED
import com.amplitude.kmp.Amplitude
import com.amplitude.kmp.Configuration
import com.amplitude.kmp.events.Identify
import com.amplitude.kmp.events.Revenue
import com.amplitude.kmp.events.BaseEvent
```

### Step 3: Done!

Your code works identically:

```kotlin
// This code is IDENTICAL for both SDKs
val amplitude = Amplitude(apiKey = "KEY", context = context)

amplitude.track("Button Clicked", mapOf("color" to "blue"))

val identify = Identify()
    .set("email", "user@example.com")
    .add("loginCount", 1)
amplitude.identify(identify)

val revenue = Revenue()
revenue.price = 9.99
revenue.productId = "item_123"
revenue.quantity = 2
amplitude.revenue(revenue)

amplitude.setUserId("user_123").flush()
```

**No other changes required!** 🎉

### Migration Checklist

The deprecated `trackingSessionEvents`, `defaultTracking`, and `DefaultTrackingOptions`
APIs are not provided. Use `autocapture` instead: include `AutocaptureOption.SESSIONS`
to enable session events, omit it to disable them, or use `emptySet()` to disable
all autocapture. Code using those legacy APIs must be updated; import-only migration
does not apply to those APIs.

- [ ] Update dependency from `analytics-android` to `analytics-kmp`
- [ ] Update imports from `com.amplitude.android.*` to `com.amplitude.kmp.*`
- [ ] Update imports from `com.amplitude.core.*` to `com.amplitude.kmp.*`
- [ ] Test your app
- [ ] Done!

## Best Practices

### 1. Initialize Early

Initialize Amplitude as early as possible in your app lifecycle:

```kotlin
class MyApplication : Application() {
    lateinit var amplitude: Amplitude

    override fun onCreate() {
        super.onCreate()
        amplitude = Amplitude(apiKey = "KEY", context = this)
    }
}
```

### 2. Set User ID on Login

```kotlin
fun onUserLogin(userId: String) {
    amplitude.setUserId(userId)
    amplitude.identify(
        Identify()
            .set("login_time", System.currentTimeMillis())
            .add("login_count", 1)
    )
}
```

### 3. Track User Journey

```kotlin
// Track key user actions
amplitude.track("Onboarding Started")
amplitude.track("Profile Created")
amplitude.track("First Purchase")
amplitude.track("Feature Discovered", mapOf("feature" to "export"))
```

### 4. Use Identify for User Properties

```kotlin
// Set properties that describe the user
val identify = Identify()
    .set("plan", "premium")
    .set("account_age_days", 30)
    .set("features_enabled", listOf("export", "collaboration"))
    .add("sessions_count", 1)

amplitude.identify(identify)
```

### 5. Validate Revenue Events

```kotlin
val revenue = Revenue()
revenue.price = 9.99
revenue.productId = "premium_monthly"

if (revenue.isValid()) {
    amplitude.revenue(revenue)
} else {
    println("Revenue event invalid: price is required")
}
```

## Troubleshooting

### Android Context Issues

**Problem**: `IllegalArgumentException: androidContext must be android.content.Context`

**Solution**: Make sure you pass an Android Context on Android:

```kotlin
// Correct
val amplitude = Amplitude(apiKey = "KEY", context = applicationContext)

// Incorrect
val amplitude = Amplitude(apiKey = "KEY", context = null)
```

### iOS Build Issues

**Problem**: CocoaPods dependency not found

**Solution**: Make sure CocoaPods are installed:

```bash
cd iosApp
pod install
```

### Events Not Appearing

**Common causes**:

1. **Wrong API key** - Verify your API key
2. **Opt-out enabled** - Check `optOut = false`
3. **Queue not flushed** - Call `amplitude.flush()` or wait for auto-flush
4. **Network issues** - Check device connectivity
5. **Invalid event data** - Check logs for validation errors

```kotlin
// Enable debug logging to see what's happening
val amplitude = Amplitude(apiKey, context) {
    logLevel = LogLevel.DEBUG
}
```

### Thread Safety

The SDK is thread-safe. You can call methods from any thread:

```kotlin
// Safe to call from background threads
GlobalScope.launch {
    amplitude.track("Background Event")
}
```

### Memory Leaks

Make sure to use application context, not activity context:

```kotlin
// Good
Amplitude(apiKey, applicationContext)

// Bad - may cause memory leak
Amplitude(apiKey, activityContext)
```

## Known Issues & API Compatibility

See [Known Limitations](known-limitations.md) for the current iOS custom plugin
property-loss limitation, its interoperability constraints, and recommended usage.

Report other compatibility issues through [GitHub Issues](https://github.com/nathanfallet/amplitude-kmp/issues).

### Our Commitment: 100% API Compatibility

**Goal**: The KMP SDK must have **identical API** to the Android SDK. Migration should require **ONLY import changes** -
no code modifications.

If you encounter **any API differences** after migrating from the Android SDK (beyond the known issues listed in the
limitations linked above), please:

1. **Report it immediately** by [opening an issue](https://github.com/nathanfallet/amplitude-kmp/issues)
2. Include:
    - Code that works in Android SDK
    - Code that fails/differs in KMP SDK
    - Expected behavior vs actual behavior
    - Android SDK version you're migrating from

We treat API compatibility breaks as **critical bugs** and will fix them with high priority.

**Expected**: Change imports only, everything else stays the same

```
// ONLY these should change:
import com.amplitude.android.* → import com.amplitude.kmp.*
import com.amplitude.core.* → import com.amplitude.kmp.*
```

**Not Expected**: Any code changes, refactoring, or API adjustments

## API Reference

### Complete Method List

```kotlin
// Initialization
Amplitude(apiKey: String, context: Context, configs: Configuration.() -> Unit = {})  // Android
Amplitude(apiKey: String, configs: Configuration.() -> Unit = {})                     // iOS
Amplitude(configuration: Configuration)                                                // Both

// Event Tracking
track(eventType: String, eventProperties: Map< String, Any?>? = null, options: EventOptions? = null): Amplitude
track(event: BaseEvent, options: EventOptions? = null, callback: EventCallBack? = null): Amplitude

// User Identity
identify(userProperties: Map< String, Any?>?, options: EventOptions? = null): Amplitude
identify(identify: Identify, options: EventOptions? = null): Amplitude
setUserId(userId: String?): Amplitude
getUserId(): String?

// Device Management
setDeviceId(deviceId: String): Amplitude
getDeviceId(): String?
reset(): Amplitude

// Revenue
revenue(revenue: Revenue, options: EventOptions? = null): Amplitude
revenue(event: RevenueEvent): Amplitude

// Groups
setGroup(groupType: String, groupName: String, options: EventOptions? = null): Amplitude
setGroup(groupType: String, groupName: Array< String >, options: EventOptions? = null): Amplitude
groupIdentify(
    groupType: String,
    groupName: String,
    groupProperties: Map< String, Any?>?, options: EventOptions? = null): Amplitude
groupIdentify(groupType: String, groupName: String, identify: Identify, options: EventOptions? = null): Amplitude

// Plugins
add(plugin: Plugin): Amplitude
remove(plugin: Plugin): Amplitude

// Flushing
flush()

// Properties
val sessionId: Long  // Read-only current session ID

// Constants
Amplitude.START_SESSION_EVENT  // "session_start"
Amplitude.END_SESSION_EVENT    // "session_end"
```
