# mParticle Android Mixpanel Kit Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a fully-functional mParticle Android Kit for Mixpanel that provides feature parity with the iOS Mixpanel Kit.

**Architecture:** The kit extends `KitIntegration` and implements `EventListener`, `CommerceListener`, `IdentityListener`, and `UserAttributeListener` interfaces. It wraps the `mixpanel-android` SDK, mapping mParticle events and user data to Mixpanel API calls. Configuration comes from the mParticle dashboard at runtime. The kit follows mParticle best practices: fail gracefully (never crash), check started state before SDK calls, and handle thread safety.

**Tech Stack:** Kotlin 1.9+, Android SDK 21+, Gradle (Kotlin DSL), JUnit 4, Mockito 5+, Robolectric 4.11+, mParticle Android Core 5.x, Mixpanel Android 7.x

---

## Context Reference Overview

The `mparticle-kit-context/` directory contains all reference materials. **Read the relevant CLAUDE.md files before starting each task.**

| Directory | Purpose | Priority |
|-----------|---------|----------|
| `mparticle-kit-context/CLAUDE.md` | Top-level navigation and strategy | START HERE |
| `example-kits/mparticle-apple-integration-mixpanel/` | **PRIMARY REFERENCE** - iOS Mixpanel Kit | CRITICAL |
| `example-kits/mparticle-android-integration-example/simple-kit/` | Android Kit skeleton | HIGH |
| `sdk-source-references/mixpanel-android/` | Mixpanel Android SDK API | HIGH |
| `sdk-source-references/mparticle-android-sdk/android-kit-base/` | KitIntegration interfaces | MEDIUM |

### Key Reference Files

| File | What It Shows |
|------|---------------|
| `example-kits/mparticle-apple-integration-mixpanel/CLAUDE.md` | iOS → Android API mapping guide |
| `example-kits/mparticle-apple-integration-mixpanel/Sources/mParticle-Mixpanel/MPKitMixpanel.swift` | Complete iOS implementation (~387 lines) |
| `example-kits/mparticle-android-integration-example/simple-kit/build.gradle` | Android Kit Gradle config |
| `sdk-source-references/mixpanel-android/src/main/java/com/mixpanel/android/mpmetrics/MixpanelAPI.java` | Mixpanel API methods |

---

## Task 1: Project Setup

**Context:** `example-kits/mparticle-android-integration-example/CLAUDE.md`, `simple-kit/build.gradle`

**Files:** Create `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `src/main/AndroidManifest.xml`, `consumer-proguard.pro`, `LICENSE`

**Step 1: Create settings.gradle.kts**
```kotlin
rootProject.name = "mparticle-android-integration-mixpanel"
```

**Step 2: Create build.gradle.kts**
```kotlin
plugins {
    id("com.android.library") version "8.2.0"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

android {
    namespace = "com.mparticle.kits.mixpanel"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-proguard.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
    api("com.mparticle:android-core:5.+")
    api("com.mixpanel.android:mixpanel-android:7.+")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
}
```

**Step 3: Create gradle.properties**
```properties
android.useAndroidX=true
kotlin.code.style=official
org.gradle.jvmargs=-Xmx2048m
```

**Step 4: Create AndroidManifest.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
</manifest>
```

**Step 5: Create consumer-proguard.pro**
```proguard
-keep class com.mparticle.kits.MixpanelKit { *; }
-keep class com.mparticle.kits.UserIdentificationType { *; }
```

**Step 6: Create LICENSE** - Use standard Apache 2.0 license with copyright "2024 mParticle, Inc."

**Verify:** `./gradlew clean build --dry-run` → BUILD SUCCESSFUL

**Commit:** `chore: initialize project structure with Gradle, dependencies, and LICENSE`

---

## Task 2: UserIdentificationType Enum

**Context:** `example-kits/mparticle-apple-integration-mixpanel/Sources/mParticle-Mixpanel/UserIdentificationType.swift`

**Test:** `src/test/kotlin/com/mparticle/kits/UserIdentificationTypeTest.kt`
```kotlin
@Test fun `fromValue returns CUSTOMER_ID for CustomerId string`() { assertEquals(UserIdentificationType.CUSTOMER_ID, UserIdentificationType.fromValue("CustomerId")) }
@Test fun `fromValue returns MPID for MPID string`() { ... }
@Test fun `fromValue returns OTHER, OTHER_2, OTHER_3, OTHER_4 for respective strings`() { ... }
@Test fun `fromValue returns null for unknown, null, or empty input`() { ... }
```

**Implementation:** `src/main/kotlin/com/mparticle/kits/UserIdentificationType.kt`
```kotlin
enum class UserIdentificationType(val value: String) {
    CUSTOMER_ID("CustomerId"), MPID("MPID"), OTHER("Other"),
    OTHER_2("Other2"), OTHER_3("Other3"), OTHER_4("Other4");
    companion object {
        fun fromValue(value: String?): UserIdentificationType? {
            if (value.isNullOrEmpty()) return null
            return entries.find { it.value == value }
        }
    }
}
```

**Commit:** `feat: add UserIdentificationType enum`

---

## Task 3: MixpanelKit Core Structure with Started State

**Context:** `example-kits/mparticle-android-integration-example/simple-kit/src/main/kotlin/com/mparticle/kits/ExampleKit.kt`, `ARCHITECTURE.md`

**Test:** `src/test/kotlin/com/mparticle/kits/MixpanelKitTest.kt`
```kotlin
@Test fun `getName returns Mixpanel`() { assertEquals("Mixpanel", kit.name) }
@Test fun `kit is not started before onKitCreate`() { assertFalse(kit.isStarted) }
@Test fun `getInstance returns null before initialization`() { assertNull(kit.instance) }
```

**Implementation:** `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`
```kotlin
class MixpanelKit : KitIntegration() {
    @Volatile private var mixpanelInstance: MixpanelAPI? = null
    @Volatile private var _isStarted: Boolean = false
    private var useMixpanelPeople: Boolean = true
    private var userIdentificationType: UserIdentificationType = UserIdentificationType.CUSTOMER_ID

    val isStarted: Boolean get() = _isStarted
    override fun getName(): String = NAME
    override fun onKitCreate(settings: MutableMap<String, String>?, context: Context?): MutableList<ReportingMessage>? = null
    override fun setOptOut(optedOut: Boolean): MutableList<ReportingMessage>? = null
    override fun getInstance(): Any? = mixpanelInstance

    companion object {
        const val NAME = "Mixpanel"
        const val KEY_TOKEN = "token"
        const val KEY_SERVER_URL = "serverURL"
        const val KEY_USER_ID_TYPE = "userIdentificationType"
        const val KEY_USE_PEOPLE = "useMixpanelPeople"
    }
}
```

**Commit:** `feat: add MixpanelKit skeleton with started state tracking`

---

## Task 4: Kit Initialization - Token Validation

**Context:** iOS `didFinishLaunching(withConfiguration:)`, `ARCHITECTURE.md` "Fail gracefully"

**Tests:** Add to `MixpanelKitTest.kt`
```kotlin
@Test(expected = IllegalArgumentException::class) fun `onKitCreate throws when token is missing`() { ... }
@Test(expected = IllegalArgumentException::class) fun `onKitCreate throws when token is empty`() { ... }
@Test(expected = IllegalArgumentException::class) fun `onKitCreate throws when settings is null`() { ... }
@Test(expected = IllegalArgumentException::class) fun `onKitCreate throws when context is null`() { ... }
```

**Implementation:** Update `onKitCreate`:
```kotlin
override fun onKitCreate(settings: MutableMap<String, String>?, context: Context?): MutableList<ReportingMessage>? {
    requireNotNull(context) { "Context is required" }
    val token = settings?.get(KEY_TOKEN)
    if (token.isNullOrEmpty()) throw IllegalArgumentException("Mixpanel token is required")
    // TODO: Initialize Mixpanel SDK
    return null
}
```

**Commit:** `feat: add token and context validation in onKitCreate`

---

## Task 5: Kit Initialization - Mixpanel SDK Setup

**Context:** `sdk-source-references/mixpanel-android/src/main/java/com/mixpanel/android/mpmetrics/MixpanelAPI.java`

**Tests:**
```kotlin
@Test fun `onKitCreate succeeds with valid token`() { assertTrue(kit.isStarted) }
@Test fun `getInstance returns MixpanelAPI after initialization`() { assertNotNull(kit.instance) }
@Test fun `onKitCreate parses userIdentificationType, useMixpanelPeople, serverURL settings`() { ... }
```

**Implementation:** Complete `onKitCreate`:
```kotlin
override fun onKitCreate(settings: MutableMap<String, String>?, context: Context?): MutableList<ReportingMessage>? {
    requireNotNull(context) { "Context is required" }
    val token = settings?.get(KEY_TOKEN)
    if (token.isNullOrEmpty()) throw IllegalArgumentException("Mixpanel token is required")

    val serverURL = settings[KEY_SERVER_URL]?.takeIf { it.isNotEmpty() }
    settings[KEY_USER_ID_TYPE]?.let { UserIdentificationType.fromValue(it)?.let { userIdentificationType = it } }
    settings[KEY_USE_PEOPLE]?.let { useMixpanelPeople = it.lowercase() == "true" }

    mixpanelInstance = MixpanelAPI.getInstance(context, token, false)
    serverURL?.let { mixpanelInstance?.setServerURL(it) }
    _isStarted = true
    return null
}
```

**Commit:** `feat: implement Mixpanel SDK initialization with configuration parsing`

---

## Task 6: setOptOut Implementation

**Context:** `sdk-source-references/mixpanel-android` - `optOutTracking()`, `optInTracking()`

**Tests:**
```kotlin
@Test fun `setOptOut returns null when not started`() { assertNull(kit.setOptOut(true)) }
@Test fun `setOptOut with true/false calls optOutTracking/optInTracking`() { ... }
```

**Implementation:**
```kotlin
override fun setOptOut(optedOut: Boolean): MutableList<ReportingMessage>? {
    if (!_isStarted) return null
    val mixpanel = mixpanelInstance ?: return null
    if (optedOut) mixpanel.optOutTracking() else mixpanel.optInTracking()
    return null
}
```

**Commit:** `feat: implement setOptOut with started state check`

---

## Task 7: EventListener Interface - logEvent

**Context:** `KitIntegration.EventListener`, Mixpanel `track()`

**Test file:** `src/test/kotlin/com/mparticle/kits/EventForwardingTest.kt`
```kotlin
@Test fun `logEvent returns null when not started`() { ... }
@Test fun `logEvent forwards event to Mixpanel`() { ... }
@Test fun `logEvent includes event attributes`() { ... }
@Test fun `logEvent returns null for empty event name`() { ... }
```

**Implementation:** Add `KitIntegration.EventListener` interface:
```kotlin
class MixpanelKit : KitIntegration(), KitIntegration.EventListener {
    override fun logEvent(event: MPEvent): MutableList<ReportingMessage>? {
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null
        val eventName = event.eventName
        if (eventName.isNullOrEmpty()) return null
        mixpanel.track(eventName, convertToJSONObject(event.customAttributeStrings))
        return mutableListOf(ReportingMessage.fromEvent(this, event))
    }
    // Stub other EventListener methods: logScreen, logError, logException, leaveBreadcrumb
    private fun convertToJSONObject(attributes: Map<String, String>?): JSONObject? { /* ... */ }
}
```

**Commit:** `feat: implement EventListener.logEvent with started state check`

---

## Task 8: EventListener Interface - logScreen

**Context:** iOS uses "Viewed " prefix for screen events

**Tests:**
```kotlin
@Test fun `logScreen returns null when not started`() { ... }
@Test fun `logScreen prefixes event name with Viewed`() { ... }
@Test fun `logScreen returns null for null/empty screen name`() { ... }
```

**Implementation:**
```kotlin
override fun logScreen(screenName: String?, screenAttributes: MutableMap<String, String>?): MutableList<ReportingMessage>? {
    if (!_isStarted || screenName.isNullOrEmpty()) return null
    mixpanelInstance?.track("Viewed $screenName", convertToJSONObject(screenAttributes))
    return mutableListOf(ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), screenAttributes))
}
```

**Commit:** `feat: implement EventListener.logScreen with 'Viewed' prefix`

---

## Task 9: EventListener Interface - Error and Breadcrumb Methods

**Tests:** logError, logException, leaveBreadcrumb with started/not-started cases

**Implementation:**
```kotlin
override fun logError(message: String?, errorAttributes: MutableMap<String, String>?): MutableList<ReportingMessage>? {
    if (!_isStarted) return null
    val props = JSONObject().apply { put("error_message", message ?: "Unknown error"); errorAttributes?.forEach { put(it.key, it.value) } }
    mixpanelInstance?.track("Error", props)
    return mutableListOf(ReportingMessage(this, ReportingMessage.MessageType.ERROR, System.currentTimeMillis(), errorAttributes))
}
// Similar for logException (event "Exception") and leaveBreadcrumb (event "Breadcrumb")
```

**Commit:** `feat: implement logError, logException, and leaveBreadcrumb`

---

## Task 10: CommerceListener Interface - Purchase Events

**Context:** `KitIntegration.CommerceListener`, Mixpanel `people.trackCharge()`

**Test file:** `src/test/kotlin/com/mparticle/kits/CommerceTest.kt`
```kotlin
@Test fun `logEvent commerce returns null when not started`() { ... }
@Test fun `logEvent tracks purchase with revenue via People API`() { ... }
@Test fun `logEvent expands non-purchase commerce events`() { ... }
@Test fun `logLtvIncrease returns null`() { ... }
```

**Implementation:**
```kotlin
class MixpanelKit : KitIntegration(), KitIntegration.EventListener, KitIntegration.CommerceListener {
    override fun logEvent(event: CommerceEvent): MutableList<ReportingMessage>? {
        if (!_isStarted) return null
        if (event.productAction == Product.PURCHASE) {
            if (useMixpanelPeople) {
                mixpanelInstance?.people?.trackCharge(event.transactionAttributes?.revenue ?: 0.0, convertToJSONObject(event.customAttributeStrings))
            }
            return mutableListOf(ReportingMessage.fromEvent(this, event))
        }
        // Expand non-purchase events via CommerceEventUtils.expand()
        return CommerceEventUtils.expand(event)?.mapNotNull { logEvent(it.event) }?.flatten()?.toMutableList()
    }
    override fun logLtvIncrease(...): MutableList<ReportingMessage>? = null
}
```

**Commit:** `feat: implement CommerceListener for purchase tracking`

---

## Task 11: IdentityListener Interface

**Context:** `KitIntegration.IdentityListener`, Mixpanel `identify()`, `reset()`

**Test file:** `src/test/kotlin/com/mparticle/kits/IdentityTest.kt`
```kotlin
@Test fun `onLoginCompleted/onIdentifyCompleted/onModifyCompleted calls Mixpanel identify`() { ... }
@Test fun `onLogoutCompleted calls Mixpanel reset`() { ... }
@Test fun `identity uses configured userIdentificationType (CustomerId, MPID, Other, etc.)`() { ... }
```

**Implementation:**
```kotlin
class MixpanelKit : ... , KitIntegration.IdentityListener {
    override fun onIdentifyCompleted(user: MParticleUser?, request: FilteredIdentityApiRequest?) { if (_isStarted) identifyUser(user) }
    override fun onLoginCompleted(user: MParticleUser?, request: FilteredIdentityApiRequest?) { if (_isStarted) identifyUser(user) }
    override fun onLogoutCompleted(user: MParticleUser?, request: FilteredIdentityApiRequest?) { if (_isStarted) mixpanelInstance?.reset() }
    override fun onModifyCompleted(user: MParticleUser?, request: FilteredIdentityApiRequest?) { if (_isStarted) identifyUser(user) }
    override fun onUserIdentified(user: MParticleUser?) { if (_isStarted) identifyUser(user) }

    private fun identifyUser(user: MParticleUser?) {
        extractUserId(user)?.let { mixpanelInstance?.identify(it) }
    }
    private fun extractUserId(user: MParticleUser?): String? {
        return when (userIdentificationType) {
            UserIdentificationType.CUSTOMER_ID -> user?.userIdentities?.get(MParticle.IdentityType.CustomerId)
            UserIdentificationType.MPID -> user?.id?.toString()
            UserIdentificationType.OTHER -> user?.userIdentities?.get(MParticle.IdentityType.Other)
            // OTHER_2, OTHER_3, OTHER_4 similarly
        }
    }
}
```

**Commit:** `feat: implement IdentityListener for user identification`

---

## Task 12: UserAttributeListener Interface

**Context:** `KitIntegration.UserAttributeListener`, Mixpanel `people.set/unset/increment`, `registerSuperProperties/unregisterSuperProperty`

**Test file:** `src/test/kotlin/com/mparticle/kits/UserAttributeTest.kt`
```kotlin
@Test fun `onSetUserAttribute sets People property when useMixpanelPeople is true`() { ... }
@Test fun `onSetUserAttribute uses super properties when useMixpanelPeople is false`() { ... }
@Test fun `onRemoveUserAttribute, onIncrementUserAttribute, onSetUserTag, onSetUserAttributeList, onSetAllUserAttributes`() { ... }
@Test fun `supportsAttributeLists returns true`() { ... }
```

**Implementation:**
```kotlin
class MixpanelKit : ... , KitIntegration.UserAttributeListener {
    override fun onSetUserAttribute(key: String?, value: Any?, user: FilteredMParticleUser?) {
        if (!_isStarted || key.isNullOrEmpty() || value == null) return
        if (useMixpanelPeople) mixpanelInstance?.people?.set(key, value)
        else mixpanelInstance?.registerSuperProperties(JSONObject().apply { put(key, value) })
    }
    override fun onRemoveUserAttribute(key: String?, user: FilteredMParticleUser?) {
        if (!_isStarted || key.isNullOrEmpty()) return
        if (useMixpanelPeople) mixpanelInstance?.people?.unset(key)
        else mixpanelInstance?.unregisterSuperProperty(key)
    }
    override fun onIncrementUserAttribute(key: String?, incrementedBy: Number?, value: String?, user: FilteredMParticleUser?) {
        if (_isStarted && useMixpanelPeople && !key.isNullOrEmpty()) mixpanelInstance?.people?.increment(key, incrementedBy?.toDouble() ?: 0.0)
    }
    override fun onSetUserTag(key: String?, user: FilteredMParticleUser?) { onSetUserAttribute(key, true, user) }
    override fun onSetUserAttributeList(key: String?, values: MutableList<String>?, user: FilteredMParticleUser?) { /* set JSONArray */ }
    override fun onSetAllUserAttributes(attrs: MutableMap<String, String>?, attrLists: MutableMap<String, MutableList<String>>?, user: FilteredMParticleUser?) {
        attrs?.forEach { onSetUserAttribute(it.key, it.value, user) }
        attrLists?.forEach { onSetUserAttributeList(it.key, it.value, user) }
    }
    override fun supportsAttributeLists(): Boolean = true
    override fun onConsentStateUpdated(oldState: ConsentState?, newState: ConsentState?, user: FilteredMParticleUser?) { /* no-op */ }
}
```

**Commit:** `feat: implement UserAttributeListener for People and Super Properties`

---

## Task 13: Integration Test and Final Cleanup

**Test file:** `src/test/kotlin/com/mparticle/kits/IntegrationTest.kt`

Test complete flows: initialization with all config options, event tracking, screen tracking, commerce purchase, identity login/logout, user attributes with People API and Super Properties, opt-out/opt-in, error/exception/breadcrumb tracking.

**Commit:** `feat: add integration tests for complete Kit functionality`

---

## Task 14: Documentation and README

**Create:** `README.md` with installation, configuration table, feature mapping, direct SDK access example, architecture notes, license.

**Commit:** `docs: add README with setup and usage instructions`

---

## Task 15: Final Verification

1. `./gradlew clean build` → BUILD SUCCESSFUL
2. `./gradlew test` → All ~55 tests PASS
3. `./gradlew lint` → No errors
4. Verify >80% test coverage
5. Review git log for clean commit history

---

## Summary

**15 Tasks** implementing complete mParticle Android Mixpanel Kit with:
- Feature parity with iOS Mixpanel Kit
- All interfaces: EventListener, CommerceListener, IdentityListener, UserAttributeListener
- Best practices: fail gracefully, check started state, thread-safe (@Volatile)
- Config options: token, serverURL, userIdentificationType, useMixpanelPeople
- ~55 tests across 6 test files, ~400 lines of Kotlin
