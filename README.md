# mParticle Android Integration for Mixpanel

[![Maven Central](https://img.shields.io/maven-central/v/com.mparticle/android-mixpanel-kit.svg)](https://search.maven.org/artifact/com.mparticle/android-mixpanel-kit)

This repository contains the Mixpanel integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

## Installation

Add the kit dependency to your app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.mparticle:android-mixpanel-kit:5+'
}
```

The kit includes the Mixpanel Android SDK as a transitive dependency.

## Configuration

Configure the Mixpanel integration through the mParticle dashboard with the following settings:

| Setting | Description |
|---------|-------------|
| `token` | **(Required)** Your Mixpanel project token |
| `serverURL` | (Optional) Custom server URL for Mixpanel data |
| `userIdentificationType` | User ID type: `CustomerId`, `MPID`, `Other`, `Other2`, `Other3`, or `Other4` |
| `useMixpanelPeople` | Enable Mixpanel People API for user attributes (default: `true`) |

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
│   ├── MixpanelKit.kt           # Main kit implementation
│   └── UserIdentificationType.kt # User ID type enum
└── test/kotlin/com/mparticle/kits/
    ├── MixpanelKitTest.kt       # Core kit tests
    ├── EventForwardingTest.kt   # Event forwarding tests
    ├── CommerceTest.kt          # Commerce event tests
    ├── IdentityTest.kt          # Identity handling tests
    ├── UserAttributeTest.kt     # User attribute tests
    ├── IntegrationTest.kt       # Integration tests
    └── TestableMixpanelKit.kt   # Test helper class
```

## License

Apache License 2.0

## Support

- [mParticle Documentation](https://docs.mparticle.com)
- [Mixpanel Documentation](https://developer.mixpanel.com)
- [GitHub Issues](https://github.com/mparticle/mparticle-android-integration-mixpanel/issues)
