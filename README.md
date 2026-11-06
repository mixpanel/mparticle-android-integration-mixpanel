# mParticle Android Integration for Mixpanel

[![Maven Central](https://img.shields.io/maven-central/v/com.mixpanel.android/mparticle-android-integration-mixpanel.svg)](https://central.sonatype.com/artifact/com.mixpanel.android/mparticle-android-integration-mixpanel)
[![CI](https://github.com/mixpanel/mparticle-android-integration-mixpanel/actions/workflows/ci.yml/badge.svg)](https://github.com/mixpanel/mparticle-android-integration-mixpanel/actions/workflows/ci.yml)

# Latest Version

##### _April 22, 2026_ - [v1.0.0](https://github.com/mixpanel/mparticle-android-integration-mixpanel/releases/tag/v1.0.0)

This repository contains the Mixpanel integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

## Installation

Add the kit dependency to your app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.mixpanel.android:mparticle-android-integration-mixpanel:1.+'

    // Optional: Add Session Replay SDK for session recording
    implementation 'com.mixpanel.android:mixpanel-android-session-replay:1.+'
}
```

The kit includes the Mixpanel Android SDK as a transitive dependency. The Session Replay SDK is optional and only required if you enable Session Replay.

## Configuration

Configure the Mixpanel integration through the mParticle dashboard with the following settings:

### Core Settings

| Setting | Description |
|---------|-------------|
| `token` | **(Required)** Your Mixpanel project token |
| `baseUrl` | (Optional) Custom server URL for Mixpanel data (maps to mParticle's "Mixpanel Target Server" setting) |
| `userIdentificationType` | User ID type: `CustomerId`, `MPID`, `Other`, `Other2`, `Other3`, or `Other4` |
| `useMixpanelPeople` | Enable Mixpanel People API for user attributes (default: `true`) |

### Session Replay Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `sessionReplayEnabled` | Enable Session Replay recording | `false` |
| `recordSessionsPercent` | Percentage of sessions to record (0.0-100.0) | `100.0` |
| `autoStartRecording` | Automatically start recording on initialization | `true` |
| `wifiOnly` | Only upload recordings on WiFi | `true` |
| `maskImages` | Mask images in recordings for privacy | `true` |
| `maskText` | Mask text in recordings for privacy | `true` |
| `maskWebViews` | Mask WebView content in recordings | `true` |
| `enableSessionReplayLogging` | Enable debug logging for Session Replay | `false` |
| `sessionReplayFlushInterval` | Flush interval in seconds (1-3600) | `10` |

## Features

### Event Tracking

All mParticle events are forwarded to Mixpanel:

- **Custom Events** - Tracked as Mixpanel events with the same name and properties
- **Screen Views** - Tracked as `"Viewed {ScreenName}"` events
- **Errors** - Tracked as `"Error"` events with error details
- **Exceptions** - Tracked as `"Exception"` events with exception info
- **Breadcrumbs** - Tracked as `"Breadcrumb"` events

### Commerce Events

- **Purchase events** - Revenue is tracked via Mixpanel's People `trackCharge()` API
- **Other commerce events** (Add to Cart, etc.) - Expanded to standard events

### User Identity

The kit supports multiple user identification strategies based on your `userIdentificationType` setting:

| Type | Description |
|------|-------------|
| `CustomerId` | Uses mParticle Customer ID |
| `MPID` | Uses mParticle ID (numeric) |
| `Other` | Uses mParticle Other identity |
| `Other2` | Uses mParticle Other2 identity |
| `Other3` | Uses mParticle Other3 identity |
| `Other4` | Uses mParticle Other4 identity |

Identity events handled:
- **Login** - Calls `Mixpanel.identify()`
- **Logout** - Calls `Mixpanel.reset()`
- **Identify** - Calls `Mixpanel.identify()`

### User Attributes

User attributes are set based on the `useMixpanelPeople` setting:

| Setting | Behavior |
|---------|----------|
| `true` | Attributes set via Mixpanel People API (`people.set()`) |
| `false` | Attributes set as Super Properties (`registerSuperProperties()`) |

Supported operations:
- Set single attribute
- Set attribute list (as JSON array)
- Remove attribute
- Increment numeric attribute
- Set user tag (set to `true`)

### Opt-Out

Calling `MParticle.setOptOut(true)` will call `Mixpanel.optOutTracking()`.
Calling `MParticle.setOptOut(false)` will call `Mixpanel.optInTracking()`.

When Session Replay is enabled, opting out will also stop session recording, and opting back in will resume recording (if `autoStartRecording` is enabled).

### Session Replay

Session Replay allows you to record and replay user sessions for debugging and UX analysis. To use Session Replay:

1. Add the Session Replay SDK dependency (see Installation)
2. Enable `sessionReplayEnabled` in the mParticle dashboard
3. Configure privacy masking options as needed

The kit automatically:
- Initializes Session Replay when the Mixpanel SDK initializes
- Syncs user identity with Session Replay on login/identify
- Stops recording when the user opts out of tracking

**Programmatic Control:**

```kotlin
val mixpanelKit = MParticle.getInstance()
    ?.getKitInstance(MParticle.ServiceProviders.MIXPANEL) as? MixpanelKit

// Start/stop recording manually
mixpanelKit?.startSessionReplayRecording()
mixpanelKit?.stopSessionReplayRecording()

// Check if Session Replay is active
val isEnabled = mixpanelKit?.isSessionReplayEnabled ?: false

// Get the current replay ID
val replayId = mixpanelKit?.getSessionReplayId()
```

**Note:** If the Session Replay SDK is not included in your app, enabling `sessionReplayEnabled` will have no effect and `isSessionReplayEnabled` will return `false`.

## Accessing the Mixpanel SDK

You can access the Mixpanel SDK instance directly:

```kotlin
val mixpanel = MParticle.getInstance()
    ?.getKitInstance(MParticle.ServiceProviders.MIXPANEL) as? MixpanelAPI
```

## Development

### Building

```bash
./gradlew build
```

### Testing

```bash
./gradlew testDebugUnitTest
```

### Project Structure

```
src/
├── main/kotlin/com/mparticle/kits/
│   ├── MixpanelKit.kt                  # Main kit implementation
│   ├── SessionReplayConfiguration.kt   # Session Replay config
│   ├── UserIdentificationType.kt       # User ID type enum
│   └── Constants.kt                    # Configuration keys
└── test/kotlin/com/mparticle/kits/
    ├── MixpanelKitTest.kt              # Core kit tests
    ├── EventForwardingTest.kt          # Event forwarding tests
    ├── CommerceTest.kt                 # Commerce event tests
    ├── IdentityTest.kt                 # Identity handling tests
    ├── UserAttributeTest.kt            # User attribute tests
    ├── IntegrationTest.kt              # Integration tests
    ├── SessionReplayConfigurationTest.kt # Session Replay config tests
    ├── SessionReplayBehaviorTest.kt    # Session Replay behavior tests
    └── TestableMixpanelKit.kt          # Test helper class
```

## License

Apache License 2.0

## Support

- [mParticle Documentation](https://docs.mparticle.com)
- [Mixpanel Documentation](https://developer.mixpanel.com)
- [GitHub Issues](https://github.com/mparticle/mparticle-android-integration-mixpanel/issues)
