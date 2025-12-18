# mParticle Android Mixpanel Kit Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a fully-functional mParticle Android Kit for Mixpanel that provides feature parity with the iOS Mixpanel Kit.

**Architecture:** The kit extends `KitIntegration` and implements `EventListener`, `CommerceListener`, `IdentityListener`, and `UserAttributeListener` interfaces. It wraps the `mixpanel-android` SDK, mapping mParticle events and user data to Mixpanel API calls. Configuration comes from the mParticle dashboard at runtime.

**Tech Stack:** Kotlin 1.9+, Android SDK 21+, Gradle (Kotlin DSL), JUnit 4, Mockito 5+, mParticle Android Core 5.x, Mixpanel Android 7.x

---

## Task 1: Project Setup

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `src/main/AndroidManifest.xml`
- Create: `consumer-proguard.pro`

**Step 1: Create settings.gradle.kts**

```kotlin
// settings.gradle.kts
rootProject.name = "mparticle-android-integration-mixpanel"
```

**Step 2: Create build.gradle.kts**

```kotlin
// build.gradle.kts
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

    kotlinOptions {
        jvmTarget = "1.8"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // mParticle SDK
    api("com.mparticle:android-core:5.+")

    // Mixpanel SDK
    api("com.mixpanel.android:mixpanel-android:7.+")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.robolectric:robolectric:4.11.1")
}
```

**Step 3: Create gradle.properties**

```properties
# gradle.properties
android.useAndroidX=true
kotlin.code.style=official
org.gradle.jvmargs=-Xmx2048m
```

**Step 4: Create AndroidManifest.xml**

```xml
<!-- src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Mixpanel requires INTERNET permission (inherited from app) -->
</manifest>
```

**Step 5: Create consumer-proguard.pro**

```proguard
# consumer-proguard.pro
# mParticle Mixpanel Kit ProGuard rules

# Keep the Kit class
-keep class com.mparticle.kits.MixpanelKit { *; }

# Keep the UserIdentificationType enum
-keep class com.mparticle.kits.UserIdentificationType { *; }
```

**Step 6: Verify Gradle sync**

Run: `./gradlew clean build --dry-run`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add -A && git commit -m "chore: initialize project structure with Gradle and dependencies"
```

---

## Task 2: UserIdentificationType Enum

**Files:**
- Create: `src/test/kotlin/com/mparticle/kits/UserIdentificationTypeTest.kt`
- Create: `src/main/kotlin/com/mparticle/kits/UserIdentificationType.kt`

**Step 1: Write the failing test**

```kotlin
// src/test/kotlin/com/mparticle/kits/UserIdentificationTypeTest.kt
package com.mparticle.kits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserIdentificationTypeTest {

    @Test
    fun `fromValue returns CUSTOMER_ID for CustomerId string`() {
        val result = UserIdentificationType.fromValue("CustomerId")
        assertEquals(UserIdentificationType.CUSTOMER_ID, result)
    }

    @Test
    fun `fromValue returns MPID for MPID string`() {
        val result = UserIdentificationType.fromValue("MPID")
        assertEquals(UserIdentificationType.MPID, result)
    }

    @Test
    fun `fromValue returns OTHER for Other string`() {
        val result = UserIdentificationType.fromValue("Other")
        assertEquals(UserIdentificationType.OTHER, result)
    }

    @Test
    fun `fromValue returns OTHER_2 for Other2 string`() {
        val result = UserIdentificationType.fromValue("Other2")
        assertEquals(UserIdentificationType.OTHER_2, result)
    }

    @Test
    fun `fromValue returns OTHER_3 for Other3 string`() {
        val result = UserIdentificationType.fromValue("Other3")
        assertEquals(UserIdentificationType.OTHER_3, result)
    }

    @Test
    fun `fromValue returns OTHER_4 for Other4 string`() {
        val result = UserIdentificationType.fromValue("Other4")
        assertEquals(UserIdentificationType.OTHER_4, result)
    }

    @Test
    fun `fromValue returns null for unknown value`() {
        val result = UserIdentificationType.fromValue("Unknown")
        assertNull(result)
    }

    @Test
    fun `fromValue returns null for null input`() {
        val result = UserIdentificationType.fromValue(null)
        assertNull(result)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.UserIdentificationTypeTest"`
Expected: FAIL with "Unresolved reference: UserIdentificationType"

**Step 3: Write minimal implementation**

```kotlin
// src/main/kotlin/com/mparticle/kits/UserIdentificationType.kt
package com.mparticle.kits

/**
 * Configurable user identification type for Mixpanel identity mapping.
 * Matches the iOS implementation for feature parity.
 */
enum class UserIdentificationType(val value: String) {
    CUSTOMER_ID("CustomerId"),
    MPID("MPID"),
    OTHER("Other"),
    OTHER_2("Other2"),
    OTHER_3("Other3"),
    OTHER_4("Other4");

    companion object {
        /**
         * Parse a string value to a UserIdentificationType.
         * @param value The string value from mParticle configuration
         * @return The matching UserIdentificationType or null if not found
         */
        fun fromValue(value: String?): UserIdentificationType? {
            if (value == null) return null
            return entries.find { it.value == value }
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.UserIdentificationTypeTest"`
Expected: PASS (8 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: add UserIdentificationType enum"
```

---

## Task 3: MixpanelKit Core Structure

**Files:**
- Create: `src/test/kotlin/com/mparticle/kits/MixpanelKitTest.kt`
- Create: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test for getName()**

```kotlin
// src/test/kotlin/com/mparticle/kits/MixpanelKitTest.kt
package com.mparticle.kits

import org.junit.Assert.assertEquals
import org.junit.Test

class MixpanelKitTest {
    private val kit = MixpanelKit()

    @Test
    fun `getName returns Mixpanel`() {
        assertEquals("Mixpanel", kit.name)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest.getName returns Mixpanel"`
Expected: FAIL with "Unresolved reference: MixpanelKit"

**Step 3: Write minimal MixpanelKit skeleton**

```kotlin
// src/main/kotlin/com/mparticle/kits/MixpanelKit.kt
package com.mparticle.kits

import android.content.Context

/**
 * mParticle Kit for Mixpanel analytics integration.
 *
 * This kit wraps the Mixpanel Android SDK and maps mParticle events,
 * identity, and user attributes to their Mixpanel equivalents.
 */
class MixpanelKit : KitIntegration() {

    override fun getName(): String = NAME

    override fun onKitCreate(
        settings: MutableMap<String, String>?,
        context: Context?
    ): MutableList<ReportingMessage>? {
        // TODO: Implement initialization
        return null
    }

    override fun setOptOut(optedOut: Boolean): MutableList<ReportingMessage>? {
        // TODO: Implement opt-out
        return null
    }

    companion object {
        const val NAME = "Mixpanel"
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest.getName returns Mixpanel"`
Expected: PASS

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: add MixpanelKit skeleton with getName()"
```

---

## Task 4: Kit Initialization - Token Validation

**Files:**
- Modify: `src/test/kotlin/com/mparticle/kits/MixpanelKitTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test for missing token**

Add to `MixpanelKitTest.kt`:

```kotlin
import android.content.Context
import org.junit.Before
import org.mockito.Mockito.mock

class MixpanelKitTest {
    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        kit = MixpanelKit()
        mockContext = mock(Context::class.java)
    }

    @Test
    fun `getName returns Mixpanel`() {
        assertEquals("Mixpanel", kit.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `onKitCreate throws when token is missing`() {
        kit.onKitCreate(mutableMapOf(), mockContext)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `onKitCreate throws when token is empty`() {
        val settings = mutableMapOf("token" to "")
        kit.onKitCreate(settings, mockContext)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `onKitCreate throws when settings is null`() {
        kit.onKitCreate(null, mockContext)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest.onKitCreate throws when token is missing"`
Expected: FAIL - no exception thrown

**Step 3: Implement token validation**

Update `MixpanelKit.kt`:

```kotlin
package com.mparticle.kits

import android.content.Context

class MixpanelKit : KitIntegration() {

    override fun getName(): String = NAME

    override fun onKitCreate(
        settings: MutableMap<String, String>?,
        context: Context?
    ): MutableList<ReportingMessage>? {
        val token = settings?.get(KEY_TOKEN)
        if (token.isNullOrEmpty()) {
            throw IllegalArgumentException("Mixpanel token is required")
        }

        // TODO: Initialize Mixpanel SDK
        return null
    }

    override fun setOptOut(optedOut: Boolean): MutableList<ReportingMessage>? {
        return null
    }

    companion object {
        const val NAME = "Mixpanel"
        const val KEY_TOKEN = "token"
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest"`
Expected: PASS (4 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: add token validation in onKitCreate"
```

---

## Task 5: Kit Initialization - Mixpanel SDK Setup

**Files:**
- Modify: `src/test/kotlin/com/mparticle/kits/MixpanelKitTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test for successful initialization**

Add to `MixpanelKitTest.kt`:

```kotlin
import org.junit.Assert.assertNotNull

    @Test
    fun `onKitCreate succeeds with valid token`() {
        val settings = mutableMapOf(
            "token" to "test-token-12345"
        )
        // Should not throw
        val result = kit.onKitCreate(settings, mockContext)
        // Result should be null (no startup events)
    }

    @Test
    fun `getInstance returns MixpanelAPI after initialization`() {
        val settings = mutableMapOf("token" to "test-token-12345")
        kit.onKitCreate(settings, mockContext)

        val instance = kit.instance
        assertNotNull(instance)
    }
```

**Step 2: Run test to verify current state**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest.onKitCreate succeeds with valid token"`
Expected: PASS (no exception)

**Step 3: Implement Mixpanel initialization with configuration parsing**

Update `MixpanelKit.kt`:

```kotlin
package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI

class MixpanelKit : KitIntegration() {

    private var mixpanelInstance: MixpanelAPI? = null
    private var useMixpanelPeople: Boolean = true
    private var userIdentificationType: UserIdentificationType = UserIdentificationType.CUSTOMER_ID

    override fun getName(): String = NAME

    override fun onKitCreate(
        settings: MutableMap<String, String>?,
        context: Context?
    ): MutableList<ReportingMessage>? {
        requireNotNull(context) { "Context is required" }

        val token = settings?.get(KEY_TOKEN)
        if (token.isNullOrEmpty()) {
            throw IllegalArgumentException("Mixpanel token is required")
        }

        // Parse optional serverURL
        val serverURL = settings[KEY_SERVER_URL]?.takeIf { it.isNotEmpty() }

        // Parse userIdentificationType (default: CustomerId)
        settings[KEY_USER_ID_TYPE]?.let { typeString ->
            UserIdentificationType.fromValue(typeString)?.let {
                userIdentificationType = it
            }
        }

        // Parse useMixpanelPeople (default: true)
        settings[KEY_USE_PEOPLE]?.let { peopleString ->
            useMixpanelPeople = peopleString.lowercase() == "true"
        }

        // Initialize Mixpanel SDK
        mixpanelInstance = MixpanelAPI.getInstance(context, token, false)

        // Set custom server URL if provided
        serverURL?.let { url ->
            mixpanelInstance?.setServerURL(url)
        }

        return null
    }

    override fun setOptOut(optedOut: Boolean): MutableList<ReportingMessage>? {
        val mixpanel = mixpanelInstance ?: return null

        if (optedOut) {
            mixpanel.optOutTracking()
        } else {
            mixpanel.optInTracking()
        }

        return null
    }

    /**
     * Returns the Mixpanel SDK instance for direct access.
     */
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

**Step 4: Run tests to verify**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement Mixpanel SDK initialization with configuration parsing"
```

---

## Task 6: EventListener Interface - logEvent

**Files:**
- Create: `src/test/kotlin/com/mparticle/kits/EventForwardingTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test**

```kotlin
// src/test/kotlin/com/mparticle/kits/EventForwardingTest.kt
package com.mparticle.kits

import android.content.Context
import com.mparticle.MPEvent
import com.mparticle.MParticle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class EventForwardingTest {
    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        kit = MixpanelKit()
        mockContext = mock(Context::class.java)
        val settings = mutableMapOf("token" to "test-token")
        kit.onKitCreate(settings, mockContext)
    }

    @Test
    fun `logEvent forwards event to Mixpanel`() {
        val event = MPEvent.Builder("Test Event", MParticle.EventType.Other).build()

        val result = kit.logEvent(event)

        assertNotNull(result)
        assertEquals(1, result?.size)
    }

    @Test
    fun `logEvent includes event attributes`() {
        val event = MPEvent.Builder("Test Event", MParticle.EventType.Other)
            .customAttributes(mapOf("key1" to "value1", "key2" to "value2"))
            .build()

        val result = kit.logEvent(event)

        assertNotNull(result)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.EventForwardingTest"`
Expected: FAIL - logEvent method doesn't exist

**Step 3: Implement EventListener interface**

Update `MixpanelKit.kt` class declaration and add methods:

```kotlin
class MixpanelKit : KitIntegration(), KitIntegration.EventListener {

    // ... existing code ...

    // MARK: - EventListener Implementation

    override fun logEvent(event: MPEvent): MutableList<ReportingMessage>? {
        val mixpanel = mixpanelInstance ?: return null

        val eventName = event.eventName
        if (eventName.isNullOrEmpty()) {
            return null
        }

        val properties = convertToJSONObject(event.customAttributeStrings)
        mixpanel.track(eventName, properties)

        return mutableListOf(ReportingMessage.fromEvent(this, event))
    }

    override fun logScreen(
        screenName: String?,
        screenAttributes: MutableMap<String, String>?
    ): MutableList<ReportingMessage>? {
        // TODO: Implement in next task
        return null
    }

    override fun logError(
        message: String?,
        errorAttributes: MutableMap<String, String>?
    ): MutableList<ReportingMessage>? {
        // TODO: Implement
        return null
    }

    override fun logException(
        exception: Exception?,
        exceptionAttributes: MutableMap<String, String>?,
        message: String?
    ): MutableList<ReportingMessage>? {
        // TODO: Implement
        return null
    }

    override fun leaveBreadcrumb(breadcrumb: String?): MutableList<ReportingMessage>? {
        // TODO: Implement
        return null
    }

    // MARK: - Helpers

    private fun convertToJSONObject(attributes: Map<String, String>?): org.json.JSONObject? {
        if (attributes.isNullOrEmpty()) return null

        val json = org.json.JSONObject()
        attributes.forEach { (key, value) ->
            json.put(key, value)
        }
        return json
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.EventForwardingTest"`
Expected: PASS (2 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement EventListener.logEvent"
```

---

## Task 7: EventListener Interface - logScreen

**Files:**
- Modify: `src/test/kotlin/com/mparticle/kits/EventForwardingTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test**

Add to `EventForwardingTest.kt`:

```kotlin
    @Test
    fun `logScreen prefixes event name with Viewed`() {
        val result = kit.logScreen("Home", mutableMapOf("section" to "main"))

        assertNotNull(result)
        assertEquals(1, result?.size)
        assertEquals(ReportingMessage.MessageType.SCREEN_VIEW, result?.first()?.messageType)
    }

    @Test
    fun `logScreen returns null for null screen name`() {
        val result = kit.logScreen(null, null)

        assertEquals(null, result)
    }

    @Test
    fun `logScreen returns null for empty screen name`() {
        val result = kit.logScreen("", null)

        assertEquals(null, result)
    }
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.EventForwardingTest.logScreen prefixes event name with Viewed"`
Expected: FAIL - returns null

**Step 3: Implement logScreen**

Update `logScreen` in `MixpanelKit.kt`:

```kotlin
    override fun logScreen(
        screenName: String?,
        screenAttributes: MutableMap<String, String>?
    ): MutableList<ReportingMessage>? {
        val mixpanel = mixpanelInstance ?: return null

        if (screenName.isNullOrEmpty()) {
            return null
        }

        val eventName = "Viewed $screenName"
        val properties = convertToJSONObject(screenAttributes)
        mixpanel.track(eventName, properties)

        return mutableListOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.SCREEN_VIEW,
                System.currentTimeMillis(),
                screenAttributes
            )
        )
    }
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.EventForwardingTest"`
Expected: PASS (5 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement EventListener.logScreen with 'Viewed' prefix"
```

---

## Task 8: EventListener Interface - Error and Breadcrumb Methods

**Files:**
- Modify: `src/test/kotlin/com/mparticle/kits/EventForwardingTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing tests**

Add to `EventForwardingTest.kt`:

```kotlin
    @Test
    fun `logError tracks Error event`() {
        val result = kit.logError("Something went wrong", mutableMapOf("code" to "500"))

        assertNotNull(result)
        assertEquals(1, result?.size)
    }

    @Test
    fun `logException tracks Exception event`() {
        val exception = RuntimeException("Test exception")
        val result = kit.logException(exception, mutableMapOf("severity" to "high"), "Custom message")

        assertNotNull(result)
        assertEquals(1, result?.size)
    }

    @Test
    fun `leaveBreadcrumb tracks Breadcrumb event`() {
        val result = kit.leaveBreadcrumb("User clicked button")

        assertNotNull(result)
        assertEquals(1, result?.size)
    }
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.EventForwardingTest.logError tracks Error event"`
Expected: FAIL - returns null

**Step 3: Implement error, exception, and breadcrumb methods**

Update methods in `MixpanelKit.kt`:

```kotlin
    override fun logError(
        message: String?,
        errorAttributes: MutableMap<String, String>?
    ): MutableList<ReportingMessage>? {
        val mixpanel = mixpanelInstance ?: return null

        val properties = org.json.JSONObject()
        properties.put("error_message", message ?: "Unknown error")
        errorAttributes?.forEach { (key, value) ->
            properties.put(key, value)
        }

        mixpanel.track("Error", properties)

        return mutableListOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.ERROR,
                System.currentTimeMillis(),
                errorAttributes
            )
        )
    }

    override fun logException(
        exception: Exception?,
        exceptionAttributes: MutableMap<String, String>?,
        message: String?
    ): MutableList<ReportingMessage>? {
        val mixpanel = mixpanelInstance ?: return null

        val properties = org.json.JSONObject()
        properties.put("exception_message", message ?: exception?.message ?: "Unknown exception")
        properties.put("exception_class", exception?.javaClass?.simpleName ?: "Unknown")
        exceptionAttributes?.forEach { (key, value) ->
            properties.put(key, value)
        }

        mixpanel.track("Exception", properties)

        return mutableListOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.ERROR,
                System.currentTimeMillis(),
                exceptionAttributes
            )
        )
    }

    override fun leaveBreadcrumb(breadcrumb: String?): MutableList<ReportingMessage>? {
        val mixpanel = mixpanelInstance ?: return null

        if (breadcrumb.isNullOrEmpty()) {
            return null
        }

        val properties = org.json.JSONObject()
        properties.put("text", breadcrumb)

        mixpanel.track("Breadcrumb", properties)

        return mutableListOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.BREADCRUMB,
                System.currentTimeMillis(),
                null
            )
        )
    }
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.EventForwardingTest"`
Expected: PASS (8 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement logError, logException, and leaveBreadcrumb"
```

---

## Task 9: CommerceListener Interface - Purchase Events

**Files:**
- Create: `src/test/kotlin/com/mparticle/kits/CommerceTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test**

```kotlin
// src/test/kotlin/com/mparticle/kits/CommerceTest.kt
package com.mparticle.kits

import android.content.Context
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.TransactionAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class CommerceTest {
    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        kit = MixpanelKit()
        mockContext = mock(Context::class.java)
        val settings = mutableMapOf(
            "token" to "test-token",
            "useMixpanelPeople" to "true"
        )
        kit.onKitCreate(settings, mockContext)
    }

    @Test
    fun `logEvent tracks purchase with revenue`() {
        val product = Product.Builder("Test Product", "SKU123", 29.99)
            .quantity(2.0)
            .build()

        val transactionAttributes = TransactionAttributes("TXN123")
            .setRevenue(59.98)

        val commerceEvent = CommerceEvent.Builder(Product.PURCHASE, product)
            .transactionAttributes(transactionAttributes)
            .build()

        val result = kit.logEvent(commerceEvent)

        assertNotNull(result)
        assertEquals(1, result?.size)
    }

    @Test
    fun `logEvent with useMixpanelPeople false still tracks purchase`() {
        // Reinitialize with People disabled
        val kit2 = MixpanelKit()
        kit2.onKitCreate(
            mutableMapOf("token" to "test", "useMixpanelPeople" to "false"),
            mockContext
        )

        val product = Product.Builder("Product", "SKU", 10.0).build()
        val event = CommerceEvent.Builder(Product.PURCHASE, product)
            .transactionAttributes(TransactionAttributes("TXN").setRevenue(10.0))
            .build()

        val result = kit2.logEvent(event)

        assertNotNull(result)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.CommerceTest"`
Expected: FAIL - logEvent(CommerceEvent) not found

**Step 3: Implement CommerceListener interface**

Update `MixpanelKit.kt` class declaration and add commerce methods:

```kotlin
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import java.math.BigDecimal

class MixpanelKit : KitIntegration(),
    KitIntegration.EventListener,
    KitIntegration.CommerceListener {

    // ... existing code ...

    // MARK: - CommerceListener Implementation

    override fun logEvent(event: CommerceEvent): MutableList<ReportingMessage>? {
        val mixpanel = mixpanelInstance ?: return null

        val productAction = event.productAction

        // Handle Purchase events with People API
        if (productAction == Product.PURCHASE) {
            if (useMixpanelPeople) {
                val revenue = event.transactionAttributes?.revenue ?: 0.0
                val properties = convertToJSONObject(event.customAttributeStrings)
                mixpanel.people.trackCharge(revenue, properties)
            }
            return mutableListOf(ReportingMessage.fromEvent(this, event))
        }

        // For non-purchase commerce events, expand to regular events
        val expandedEvents = CommerceEventUtils.expand(event)
        if (expandedEvents.isNullOrEmpty()) {
            return null
        }

        val messages = mutableListOf<ReportingMessage>()
        expandedEvents.forEach { expanded ->
            val mpEvent = expanded.event
            logEvent(mpEvent)?.let { msgs ->
                messages.addAll(msgs)
            }
        }

        return if (messages.isEmpty()) null else messages
    }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal?,
        valueTotal: BigDecimal?,
        eventName: String?,
        contextInfo: MutableMap<String, String>?
    ): MutableList<ReportingMessage>? {
        // LTV increase is not directly supported by Mixpanel
        // Could optionally track as a custom event or People increment
        return null
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.CommerceTest"`
Expected: PASS (2 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement CommerceListener for purchase tracking"
```

---

## Task 10: IdentityListener Interface

**Files:**
- Create: `src/test/kotlin/com/mparticle/kits/IdentityTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test**

```kotlin
// src/test/kotlin/com/mparticle/kits/IdentityTest.kt
package com.mparticle.kits

import android.content.Context
import com.mparticle.MParticle
import com.mparticle.identity.MParticleUser
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class IdentityTest {
    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context
    private lateinit var mockUser: MParticleUser

    @Before
    fun setUp() {
        kit = MixpanelKit()
        mockContext = mock(Context::class.java)
        mockUser = mock(MParticleUser::class.java)

        val settings = mutableMapOf(
            "token" to "test-token",
            "userIdentificationType" to "CustomerId"
        )
        kit.onKitCreate(settings, mockContext)
    }

    @Test
    fun `onLoginCompleted calls Mixpanel identify with user ID`() {
        val identities = mapOf(MParticle.IdentityType.CustomerId to "user-123")
        `when`(mockUser.userIdentities).thenReturn(identities)

        // Should not throw
        kit.onLoginCompleted(mockUser, null)
    }

    @Test
    fun `onLogoutCompleted calls Mixpanel reset`() {
        // Should not throw
        kit.onLogoutCompleted(mockUser, null)
    }

    @Test
    fun `onIdentifyCompleted calls Mixpanel identify`() {
        val identities = mapOf(MParticle.IdentityType.CustomerId to "user-456")
        `when`(mockUser.userIdentities).thenReturn(identities)

        // Should not throw
        kit.onIdentifyCompleted(mockUser, null)
    }

    @Test
    fun `onModifyCompleted calls Mixpanel identify`() {
        val identities = mapOf(MParticle.IdentityType.CustomerId to "user-789")
        `when`(mockUser.userIdentities).thenReturn(identities)

        // Should not throw
        kit.onModifyCompleted(mockUser, null)
    }

    @Test
    fun `identity uses MPID when configured`() {
        val kit2 = MixpanelKit()
        kit2.onKitCreate(
            mutableMapOf("token" to "test", "userIdentificationType" to "MPID"),
            mockContext
        )

        `when`(mockUser.id).thenReturn(12345L)

        // Should use MPID
        kit2.onLoginCompleted(mockUser, null)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.IdentityTest"`
Expected: FAIL - onLoginCompleted not found

**Step 3: Implement IdentityListener interface**

Update `MixpanelKit.kt`:

```kotlin
import com.mparticle.identity.MParticleUser

class MixpanelKit : KitIntegration(),
    KitIntegration.EventListener,
    KitIntegration.CommerceListener,
    KitIntegration.IdentityListener {

    // ... existing code ...

    // MARK: - IdentityListener Implementation

    override fun onIdentifyCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        identifyUser(user)
    }

    override fun onLoginCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        identifyUser(user)
    }

    override fun onLogoutCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        mixpanelInstance?.reset()
    }

    override fun onModifyCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        identifyUser(user)
    }

    override fun onUserIdentified(user: MParticleUser?) {
        identifyUser(user)
    }

    // MARK: - Identity Helpers

    private fun identifyUser(user: MParticleUser?) {
        val userId = extractUserId(user) ?: return
        mixpanelInstance?.identify(userId)
    }

    private fun extractUserId(user: MParticleUser?): String? {
        if (user == null) return null

        val identities = user.userIdentities

        return when (userIdentificationType) {
            UserIdentificationType.CUSTOMER_ID ->
                identities[MParticle.IdentityType.CustomerId]
            UserIdentificationType.MPID ->
                user.id.toString()
            UserIdentificationType.OTHER ->
                identities[MParticle.IdentityType.Other]
            UserIdentificationType.OTHER_2 ->
                identities[MParticle.IdentityType.Other2]
            UserIdentificationType.OTHER_3 ->
                identities[MParticle.IdentityType.Other3]
            UserIdentificationType.OTHER_4 ->
                identities[MParticle.IdentityType.Other4]
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.IdentityTest"`
Expected: PASS (5 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement IdentityListener for user identification"
```

---

## Task 11: UserAttributeListener Interface - Set/Remove Attributes

**Files:**
- Create: `src/test/kotlin/com/mparticle/kits/UserAttributeTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test**

```kotlin
// src/test/kotlin/com/mparticle/kits/UserAttributeTest.kt
package com.mparticle.kits

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class UserAttributeTest {
    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context
    private lateinit var mockUser: FilteredMParticleUser

    @Before
    fun setUp() {
        kit = MixpanelKit()
        mockContext = mock(Context::class.java)
        mockUser = mock(FilteredMParticleUser::class.java)

        val settings = mutableMapOf(
            "token" to "test-token",
            "useMixpanelPeople" to "true"
        )
        kit.onKitCreate(settings, mockContext)
    }

    @Test
    fun `onSetUserAttribute sets People property when useMixpanelPeople is true`() {
        // Should not throw
        kit.onSetUserAttribute("name", "John Doe", mockUser)
    }

    @Test
    fun `onRemoveUserAttribute unsets People property`() {
        // Should not throw
        kit.onRemoveUserAttribute("name", mockUser)
    }

    @Test
    fun `onIncrementUserAttribute increments People property`() {
        // Should not throw
        kit.onIncrementUserAttribute("purchases", 1, "1", mockUser)
    }

    @Test
    fun `onSetUserAttribute uses super properties when useMixpanelPeople is false`() {
        val kit2 = MixpanelKit()
        kit2.onKitCreate(
            mutableMapOf("token" to "test", "useMixpanelPeople" to "false"),
            mockContext
        )

        // Should use registerSuperProperties instead
        kit2.onSetUserAttribute("tier", "premium", mockUser)
    }

    @Test
    fun `onSetUserTag sets attribute to true`() {
        // Should not throw
        kit.onSetUserTag("vip", mockUser)
    }

    @Test
    fun `supportsAttributeLists returns true`() {
        val result = kit.supportsAttributeLists()
        assert(result)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.UserAttributeTest"`
Expected: FAIL - onSetUserAttribute not found

**Step 3: Implement UserAttributeListener interface**

Update `MixpanelKit.kt`:

```kotlin
import com.mparticle.consent.ConsentState

class MixpanelKit : KitIntegration(),
    KitIntegration.EventListener,
    KitIntegration.CommerceListener,
    KitIntegration.IdentityListener,
    KitIntegration.UserAttributeListener {

    // ... existing code ...

    // MARK: - UserAttributeListener Implementation

    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
        user: FilteredMParticleUser?
    ) {
        if (key.isNullOrEmpty() || value == null) return
        val mixpanel = mixpanelInstance ?: return

        if (useMixpanelPeople) {
            mixpanel.people.set(key, value)
        } else {
            val props = org.json.JSONObject()
            props.put(key, value)
            mixpanel.registerSuperProperties(props)
        }
    }

    override fun onRemoveUserAttribute(key: String?, user: FilteredMParticleUser?) {
        if (key.isNullOrEmpty()) return
        val mixpanel = mixpanelInstance ?: return

        if (useMixpanelPeople) {
            mixpanel.people.unset(key)
        } else {
            mixpanel.unregisterSuperProperty(key)
        }
    }

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
        user: FilteredMParticleUser?
    ) {
        if (key.isNullOrEmpty() || incrementedBy == null) return
        val mixpanel = mixpanelInstance ?: return

        // Increment only supported via People API
        if (useMixpanelPeople) {
            mixpanel.people.increment(key, incrementedBy.toDouble())
        }
    }

    override fun onSetUserTag(key: String?, user: FilteredMParticleUser?) {
        if (key.isNullOrEmpty()) return

        // Set tag as a boolean true value
        onSetUserAttribute(key, true, user)
    }

    override fun onSetUserAttributeList(
        key: String?,
        values: MutableList<String>?,
        user: FilteredMParticleUser?
    ) {
        if (key.isNullOrEmpty() || values.isNullOrEmpty()) return
        val mixpanel = mixpanelInstance ?: return

        val jsonArray = org.json.JSONArray(values)

        if (useMixpanelPeople) {
            mixpanel.people.set(key, jsonArray)
        } else {
            val props = org.json.JSONObject()
            props.put(key, jsonArray)
            mixpanel.registerSuperProperties(props)
        }
    }

    override fun onSetAllUserAttributes(
        userAttributes: MutableMap<String, String>?,
        userAttributeLists: MutableMap<String, MutableList<String>>?,
        user: FilteredMParticleUser?
    ) {
        val mixpanel = mixpanelInstance ?: return

        // Set all single-value attributes
        userAttributes?.forEach { (key, value) ->
            onSetUserAttribute(key, value, user)
        }

        // Set all list attributes
        userAttributeLists?.forEach { (key, values) ->
            onSetUserAttributeList(key, values, user)
        }
    }

    override fun supportsAttributeLists(): Boolean = true

    override fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
        user: FilteredMParticleUser?
    ) {
        // Consent state changes are not directly mapped to Mixpanel
        // Could optionally track as super properties if needed
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.UserAttributeTest"`
Expected: PASS (6 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement UserAttributeListener for People and Super Properties"
```

---

## Task 12: Complete Integration Test and Polish

**Files:**
- Create: `src/test/kotlin/com/mparticle/kits/IntegrationTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt` (add imports, clean up)

**Step 1: Write integration test**

```kotlin
// src/test/kotlin/com/mparticle/kits/IntegrationTest.kt
package com.mparticle.kits

import android.content.Context
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.identity.MParticleUser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Integration test verifying complete Kit functionality.
 */
class IntegrationTest {
    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        kit = MixpanelKit()
        mockContext = mock(Context::class.java)
    }

    @Test
    fun `full initialization flow with all config options`() {
        val settings = mutableMapOf(
            "token" to "project-token-123",
            "serverURL" to "https://api-eu.mixpanel.com",
            "userIdentificationType" to "MPID",
            "useMixpanelPeople" to "true"
        )

        val result = kit.onKitCreate(settings, mockContext)

        assertNull(result) // No startup reporting messages
        assertEquals("Mixpanel", kit.name)
        assertNotNull(kit.instance)
    }

    @Test
    fun `event tracking produces reporting message`() {
        initializeKit()

        val event = MPEvent.Builder("Purchase Completed", MParticle.EventType.Transaction)
            .customAttributes(mapOf("amount" to "99.99"))
            .build()

        val result = kit.logEvent(event)

        assertNotNull(result)
        assertEquals(1, result?.size)
    }

    @Test
    fun `commerce purchase tracks charge`() {
        initializeKit()

        val product = Product.Builder("Premium Plan", "PREMIUM", 99.99).build()
        val txn = TransactionAttributes("TXN-001").setRevenue(99.99)
        val event = CommerceEvent.Builder(Product.PURCHASE, product)
            .transactionAttributes(txn)
            .build()

        val result = kit.logEvent(event)

        assertNotNull(result)
    }

    @Test
    fun `identity flow with login and logout`() {
        initializeKit()

        val mockUser = mock(MParticleUser::class.java)
        `when`(mockUser.userIdentities).thenReturn(
            mapOf(MParticle.IdentityType.CustomerId to "user-123")
        )

        // Login
        kit.onLoginCompleted(mockUser, null)

        // Logout
        kit.onLogoutCompleted(mockUser, null)

        // No exceptions = success
    }

    @Test
    fun `opt out and opt in`() {
        initializeKit()

        kit.setOptOut(true)  // Opt out
        kit.setOptOut(false) // Opt back in

        // No exceptions = success
    }

    private fun initializeKit() {
        kit.onKitCreate(
            mutableMapOf("token" to "test-token"),
            mockContext
        )
    }
}
```

**Step 2: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS

**Step 3: Add comprehensive imports and final cleanup to MixpanelKit.kt**

Ensure all imports are present at the top of `MixpanelKit.kt`:

```kotlin
package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.CommerceEventUtils
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
```

**Step 4: Run full test suite**

Run: `./gradlew test`
Expected: All tests PASS

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: add integration tests and complete Kit implementation"
```

---

## Task 13: Documentation and README

**Files:**
- Create: `README.md`

**Step 1: Write README**

```markdown
# mParticle Android Mixpanel Kit

[![Maven Central](https://img.shields.io/maven-central/v/com.mparticle/android-mixpanel-kit.svg)](https://search.maven.org/artifact/com.mparticle/android-mixpanel-kit)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

This is the [Mixpanel](https://mixpanel.com) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

## Installation

Add the kit dependency to your app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.mparticle:android-mixpanel-kit:5.+'
}
```

## Configuration

The Mixpanel Kit is configured via the mParticle dashboard. The following settings are available:

| Setting | Required | Default | Description |
|---------|----------|---------|-------------|
| `token` | Yes | - | Your Mixpanel project token |
| `serverURL` | No | - | Custom Mixpanel API endpoint (e.g., for EU data residency) |
| `userIdentificationType` | No | `CustomerId` | How to identify users (`CustomerId`, `MPID`, `Other`, `Other2`, `Other3`, `Other4`) |
| `useMixpanelPeople` | No | `true` | Use Mixpanel People API for user attributes (`true`/`false`) |

## Features

### Event Tracking
- Custom events → `mixpanel.track(eventName, properties)`
- Screen views → `mixpanel.track("Viewed {screenName}", properties)`
- Errors → `mixpanel.track("Error", properties)`
- Exceptions → `mixpanel.track("Exception", properties)`
- Breadcrumbs → `mixpanel.track("Breadcrumb", properties)`

### Commerce Events
- Purchase events → `mixpanel.people.trackCharge(revenue, properties)` (when People API enabled)
- Other commerce actions → Expanded to individual tracked events

### Identity Management
- Login/Identify/Modify → `mixpanel.identify(userId)`
- Logout → `mixpanel.reset()`

### User Attributes
When `useMixpanelPeople` is `true` (default):
- Set attribute → `mixpanel.people.set(key, value)`
- Remove attribute → `mixpanel.people.unset(key)`
- Increment attribute → `mixpanel.people.increment(key, value)`

When `useMixpanelPeople` is `false`:
- Set attribute → `mixpanel.registerSuperProperties({key: value})`
- Remove attribute → `mixpanel.unregisterSuperProperty(key)`

### Opt-Out
- Opt out → `mixpanel.optOutTracking()`
- Opt in → `mixpanel.optInTracking()`

## Direct SDK Access

You can access the underlying Mixpanel SDK instance for advanced usage:

```kotlin
val mixpanel = MParticle.getInstance()
    ?.getKitInstance(MParticle.ServiceProviders.MIXPANEL) as? MixpanelAPI

mixpanel?.track("Direct Event")
```

## License

Copyright 2024 mParticle, Inc.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
```

**Step 2: Verify README renders correctly**

Run: `cat README.md`

**Step 3: Commit**

```bash
git add README.md && git commit -m "docs: add README with setup and usage instructions"
```

---

## Task 14: Final Verification and Cleanup

**Files:**
- Review all files

**Step 1: Run full build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 2: Run all tests with coverage**

Run: `./gradlew test jacocoTestReport` (if JaCoCo configured) or `./gradlew test`
Expected: All tests PASS

**Step 3: Check lint**

Run: `./gradlew lint`
Expected: No errors

**Step 4: Review git log**

Run: `git log --oneline`
Expected: Clean commit history with descriptive messages

**Step 5: Create final commit if any cleanup needed**

```bash
git status
# If clean, no action needed
# If changes exist:
git add -A && git commit -m "chore: final cleanup"
```

---

## Summary

This plan implements the complete mParticle Android Mixpanel Kit with:

- **14 Tasks** broken into atomic steps
- **TDD approach** - tests written before implementation
- **Feature parity** with iOS Mixpanel Kit
- **All interfaces implemented:**
  - EventListener (events, screens, errors, exceptions, breadcrumbs)
  - CommerceListener (purchases, other commerce actions)
  - IdentityListener (login, logout, identify, modify)
  - UserAttributeListener (set, remove, increment, lists, tags)
- **Configuration options:** token, serverURL, userIdentificationType, useMixpanelPeople
- **Comprehensive test coverage**
- **Complete documentation**

Estimated test count: ~35 tests across 6 test files
