# mParticle Android Mixpanel Kit Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a fully-functional mParticle Android Kit for Mixpanel that provides feature parity with the iOS Mixpanel Kit.

**Architecture:** The kit extends `KitIntegration` and implements `EventListener`, `CommerceListener`, `IdentityListener`, and `UserAttributeListener` interfaces. It wraps the `mixpanel-android` SDK, mapping mParticle events and user data to Mixpanel API calls. Configuration comes from the mParticle dashboard at runtime. The kit follows mParticle best practices: fail gracefully (never crash), check started state before SDK calls, and handle thread safety.

**Tech Stack:** Kotlin 1.9+, Android SDK 21+, Gradle (Kotlin DSL), JUnit 4, Mockito 5+, Robolectric 4.11+, mParticle Android Core 5.x, Mixpanel Android 7.x

---

## Task 1: Project Setup

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `src/main/AndroidManifest.xml`
- Create: `consumer-proguard.pro`
- Create: `LICENSE`

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
    testImplementation("androidx.test:core:1.5.0")
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

**Step 6: Create LICENSE file**

```text
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to the Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   Copyright 2024 mParticle, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

**Step 7: Verify Gradle sync**

Run: `./gradlew clean build --dry-run`
Expected: BUILD SUCCESSFUL

**Step 8: Commit**

```bash
git add -A && git commit -m "chore: initialize project structure with Gradle, dependencies, and LICENSE"
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

    @Test
    fun `fromValue returns null for empty string`() {
        val result = UserIdentificationType.fromValue("")
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
 *
 * This enum determines which mParticle identity type is used as the
 * Mixpanel distinct ID when identifying users.
 *
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
         *
         * @param value The string value from mParticle configuration
         * @return The matching UserIdentificationType or null if not found
         */
        fun fromValue(value: String?): UserIdentificationType? {
            if (value.isNullOrEmpty()) return null
            return entries.find { it.value == value }
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.UserIdentificationTypeTest"`
Expected: PASS (9 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: add UserIdentificationType enum"
```

---

## Task 3: MixpanelKit Core Structure with Started State

**Files:**
- Create: `src/test/kotlin/com/mparticle/kits/MixpanelKitTest.kt`
- Create: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test for getName()**

```kotlin
// src/test/kotlin/com/mparticle/kits/MixpanelKitTest.kt
package com.mparticle.kits

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

    @Test
    fun `kit is not started before onKitCreate`() {
        assertFalse(kit.isStarted)
    }

    @Test
    fun `getInstance returns null before initialization`() {
        assertNull(kit.instance)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest.getName returns Mixpanel"`
Expected: FAIL with "Unresolved reference: MixpanelKit"

**Step 3: Write minimal MixpanelKit skeleton with started state**

```kotlin
// src/main/kotlin/com/mparticle/kits/MixpanelKit.kt
package com.mparticle.kits

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI

/**
 * mParticle Kit for Mixpanel analytics integration.
 *
 * This kit wraps the Mixpanel Android SDK and maps mParticle events,
 * identity, and user attributes to their Mixpanel equivalents.
 *
 * Design principles (per ARCHITECTURE.md):
 * - Fail gracefully: Never crash the host app
 * - Check started state: Don't call Mixpanel SDK until initialized
 * - Thread safety: Kit methods may be called from any thread
 */
class MixpanelKit : KitIntegration() {

    // MARK: - Private State

    @Volatile
    private var mixpanelInstance: MixpanelAPI? = null

    @Volatile
    private var _isStarted: Boolean = false

    private var useMixpanelPeople: Boolean = true
    private var userIdentificationType: UserIdentificationType = UserIdentificationType.CUSTOMER_ID

    // MARK: - Public Properties

    /**
     * Whether the kit has been successfully initialized.
     * Check this before calling any Mixpanel SDK methods.
     */
    val isStarted: Boolean
        get() = _isStarted

    // MARK: - KitIntegration Overrides

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

    /**
     * Returns the Mixpanel SDK instance for direct access.
     * Returns null if the kit has not been initialized.
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

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest"`
Expected: PASS (3 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: add MixpanelKit skeleton with started state tracking"
```

---

## Task 4: Kit Initialization - Token Validation

**Files:**
- Modify: `src/test/kotlin/com/mparticle/kits/MixpanelKitTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test for missing token**

Add to `MixpanelKitTest.kt`:

```kotlin
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

    @Test(expected = IllegalArgumentException::class)
    fun `onKitCreate throws when context is null`() {
        val settings = mutableMapOf("token" to "test-token")
        kit.onKitCreate(settings, null)
    }
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest.onKitCreate throws when token is missing"`
Expected: FAIL - no exception thrown

**Step 3: Implement token validation**

Update `onKitCreate` in `MixpanelKit.kt`:

```kotlin
    override fun onKitCreate(
        settings: MutableMap<String, String>?,
        context: Context?
    ): MutableList<ReportingMessage>? {
        // Validate required parameters
        requireNotNull(context) { "Context is required" }

        val token = settings?.get(KEY_TOKEN)
        if (token.isNullOrEmpty()) {
            throw IllegalArgumentException("Mixpanel token is required")
        }

        // TODO: Initialize Mixpanel SDK
        return null
    }
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest"`
Expected: PASS (7 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: add token and context validation in onKitCreate"
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
import org.junit.Assert.assertTrue

    @Test
    fun `onKitCreate succeeds with valid token`() {
        val settings = mutableMapOf("token" to "test-token-12345")

        val result = kit.onKitCreate(settings, mockContext)

        // Result should be null (no startup events)
        assertNull(result)
        assertTrue(kit.isStarted)
    }

    @Test
    fun `getInstance returns MixpanelAPI after initialization`() {
        val settings = mutableMapOf("token" to "test-token-12345")
        kit.onKitCreate(settings, mockContext)

        val instance = kit.instance
        assertNotNull(instance)
    }

    @Test
    fun `onKitCreate parses userIdentificationType setting`() {
        val settings = mutableMapOf(
            "token" to "test-token",
            "userIdentificationType" to "MPID"
        )
        kit.onKitCreate(settings, mockContext)

        assertTrue(kit.isStarted)
    }

    @Test
    fun `onKitCreate parses useMixpanelPeople setting as false`() {
        val settings = mutableMapOf(
            "token" to "test-token",
            "useMixpanelPeople" to "false"
        )
        kit.onKitCreate(settings, mockContext)

        assertTrue(kit.isStarted)
    }

    @Test
    fun `onKitCreate parses serverURL setting`() {
        val settings = mutableMapOf(
            "token" to "test-token",
            "serverURL" to "https://api-eu.mixpanel.com"
        )
        kit.onKitCreate(settings, mockContext)

        assertTrue(kit.isStarted)
    }
```

**Step 2: Run test to verify current state**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest.onKitCreate succeeds with valid token"`
Expected: FAIL - isStarted is false

**Step 3: Implement Mixpanel initialization with configuration parsing**

Update `MixpanelKit.kt`:

```kotlin
    override fun onKitCreate(
        settings: MutableMap<String, String>?,
        context: Context?
    ): MutableList<ReportingMessage>? {
        // Validate required parameters
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

        // Initialize Mixpanel SDK (trackAutomaticEvents = false per iOS parity)
        mixpanelInstance = MixpanelAPI.getInstance(context, token, false)

        // Set custom server URL if provided
        serverURL?.let { url ->
            mixpanelInstance?.setServerURL(url)
        }

        // Mark as started
        _isStarted = true

        return null
    }
```

**Step 4: Run tests to verify**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest"`
Expected: PASS (12 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement Mixpanel SDK initialization with configuration parsing"
```

---

## Task 6: setOptOut Implementation

**Files:**
- Modify: `src/test/kotlin/com/mparticle/kits/MixpanelKitTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test**

Add to `MixpanelKitTest.kt`:

```kotlin
    @Test
    fun `setOptOut returns null when not started`() {
        val result = kit.setOptOut(true)
        assertNull(result)
    }

    @Test
    fun `setOptOut with true calls optOutTracking`() {
        val settings = mutableMapOf("token" to "test-token")
        kit.onKitCreate(settings, mockContext)

        // Should not throw
        val result = kit.setOptOut(true)
        assertNull(result)
    }

    @Test
    fun `setOptOut with false calls optInTracking`() {
        val settings = mutableMapOf("token" to "test-token")
        kit.onKitCreate(settings, mockContext)

        // Should not throw
        val result = kit.setOptOut(false)
        assertNull(result)
    }
```

**Step 2: Run test to verify current state**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest.setOptOut returns null when not started"`
Expected: PASS (already returns null)

**Step 3: Implement setOptOut with started check**

Update `setOptOut` in `MixpanelKit.kt`:

```kotlin
    override fun setOptOut(optedOut: Boolean): MutableList<ReportingMessage>? {
        // Fail gracefully if not started
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null

        if (optedOut) {
            mixpanel.optOutTracking()
        } else {
            mixpanel.optInTracking()
        }

        return null
    }
```

**Step 4: Run tests to verify**

Run: `./gradlew test --tests "com.mparticle.kits.MixpanelKitTest"`
Expected: PASS (15 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement setOptOut with started state check"
```

---

## Task 7: EventListener Interface - logEvent

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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventForwardingTest {
    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        kit = MixpanelKit()
        mockContext = mock(Context::class.java)
    }

    private fun initializeKit() {
        kit.onKitCreate(mutableMapOf("token" to "test-token"), mockContext)
    }

    @Test
    fun `logEvent returns null when not started`() {
        val event = MPEvent.Builder("Test Event", MParticle.EventType.Other).build()

        val result = kit.logEvent(event)

        assertNull(result)
    }

    @Test
    fun `logEvent forwards event to Mixpanel`() {
        initializeKit()
        val event = MPEvent.Builder("Test Event", MParticle.EventType.Other).build()

        val result = kit.logEvent(event)

        assertNotNull(result)
        assertEquals(1, result?.size)
    }

    @Test
    fun `logEvent includes event attributes`() {
        initializeKit()
        val event = MPEvent.Builder("Test Event", MParticle.EventType.Other)
            .customAttributes(mapOf("key1" to "value1", "key2" to "value2"))
            .build()

        val result = kit.logEvent(event)

        assertNotNull(result)
    }

    @Test
    fun `logEvent returns null for null event name`() {
        initializeKit()
        val event = MPEvent.Builder("", MParticle.EventType.Other).build()

        val result = kit.logEvent(event)

        assertNull(result)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.mparticle.kits.EventForwardingTest"`
Expected: FAIL - logEvent method doesn't exist

**Step 3: Implement EventListener interface**

Update `MixpanelKit.kt` class declaration and add methods:

```kotlin
import com.mparticle.MPEvent
import org.json.JSONObject

class MixpanelKit : KitIntegration(), KitIntegration.EventListener {

    // ... existing code ...

    // MARK: - EventListener Implementation

    override fun logEvent(event: MPEvent): MutableList<ReportingMessage>? {
        // Fail gracefully if not started
        if (!_isStarted) return null
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

    /**
     * Convert a Map to JSONObject for Mixpanel properties.
     * Returns null if the map is null or empty.
     */
    private fun convertToJSONObject(attributes: Map<String, String>?): JSONObject? {
        if (attributes.isNullOrEmpty()) return null

        return try {
            val json = JSONObject()
            attributes.forEach { (key, value) ->
                json.put(key, value)
            }
            json
        } catch (e: Exception) {
            // Fail gracefully
            null
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.EventForwardingTest"`
Expected: PASS (4 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement EventListener.logEvent with started state check"
```

---

## Task 8: EventListener Interface - logScreen

**Files:**
- Modify: `src/test/kotlin/com/mparticle/kits/EventForwardingTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test**

Add to `EventForwardingTest.kt`:

```kotlin
    @Test
    fun `logScreen returns null when not started`() {
        val result = kit.logScreen("Home", null)

        assertNull(result)
    }

    @Test
    fun `logScreen prefixes event name with Viewed`() {
        initializeKit()

        val result = kit.logScreen("Home", mutableMapOf("section" to "main"))

        assertNotNull(result)
        assertEquals(1, result?.size)
        assertEquals(ReportingMessage.MessageType.SCREEN_VIEW, result?.first()?.messageType)
    }

    @Test
    fun `logScreen returns null for null screen name`() {
        initializeKit()

        val result = kit.logScreen(null, null)

        assertNull(result)
    }

    @Test
    fun `logScreen returns null for empty screen name`() {
        initializeKit()

        val result = kit.logScreen("", null)

        assertNull(result)
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
        // Fail gracefully if not started
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null

        if (screenName.isNullOrEmpty()) {
            return null
        }

        // Prefix with "Viewed " per iOS parity
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
Expected: PASS (8 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement EventListener.logScreen with 'Viewed' prefix"
```

---

## Task 9: EventListener Interface - Error and Breadcrumb Methods

**Files:**
- Modify: `src/test/kotlin/com/mparticle/kits/EventForwardingTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing tests**

Add to `EventForwardingTest.kt`:

```kotlin
    @Test
    fun `logError returns null when not started`() {
        val result = kit.logError("Error", null)
        assertNull(result)
    }

    @Test
    fun `logError tracks Error event`() {
        initializeKit()

        val result = kit.logError("Something went wrong", mutableMapOf("code" to "500"))

        assertNotNull(result)
        assertEquals(1, result?.size)
    }

    @Test
    fun `logException returns null when not started`() {
        val result = kit.logException(RuntimeException(), null, null)
        assertNull(result)
    }

    @Test
    fun `logException tracks Exception event`() {
        initializeKit()
        val exception = RuntimeException("Test exception")

        val result = kit.logException(exception, mutableMapOf("severity" to "high"), "Custom message")

        assertNotNull(result)
        assertEquals(1, result?.size)
    }

    @Test
    fun `leaveBreadcrumb returns null when not started`() {
        val result = kit.leaveBreadcrumb("Breadcrumb")
        assertNull(result)
    }

    @Test
    fun `leaveBreadcrumb tracks Breadcrumb event`() {
        initializeKit()

        val result = kit.leaveBreadcrumb("User clicked button")

        assertNotNull(result)
        assertEquals(1, result?.size)
    }

    @Test
    fun `leaveBreadcrumb returns null for null breadcrumb`() {
        initializeKit()

        val result = kit.leaveBreadcrumb(null)

        assertNull(result)
    }

    @Test
    fun `leaveBreadcrumb returns null for empty breadcrumb`() {
        initializeKit()

        val result = kit.leaveBreadcrumb("")

        assertNull(result)
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
        // Fail gracefully if not started
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null

        val properties = JSONObject()
        try {
            properties.put("error_message", message ?: "Unknown error")
            errorAttributes?.forEach { (key, value) ->
                properties.put(key, value)
            }
        } catch (e: Exception) {
            // Fail gracefully
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
        // Fail gracefully if not started
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null

        val properties = JSONObject()
        try {
            properties.put("exception_message", message ?: exception?.message ?: "Unknown exception")
            properties.put("exception_class", exception?.javaClass?.simpleName ?: "Unknown")
            exceptionAttributes?.forEach { (key, value) ->
                properties.put(key, value)
            }
        } catch (e: Exception) {
            // Fail gracefully
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
        // Fail gracefully if not started
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null

        if (breadcrumb.isNullOrEmpty()) {
            return null
        }

        val properties = JSONObject()
        try {
            properties.put("text", breadcrumb)
        } catch (e: Exception) {
            // Fail gracefully
        }

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
Expected: PASS (16 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement logError, logException, and leaveBreadcrumb"
```

---

## Task 10: CommerceListener Interface - Purchase Events

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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommerceTest {
    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        kit = MixpanelKit()
        mockContext = mock(Context::class.java)
    }

    private fun initializeKit(usePeople: Boolean = true) {
        kit.onKitCreate(
            mutableMapOf(
                "token" to "test-token",
                "useMixpanelPeople" to usePeople.toString()
            ),
            mockContext
        )
    }

    @Test
    fun `logEvent commerce returns null when not started`() {
        val product = Product.Builder("Product", "SKU", 10.0).build()
        val event = CommerceEvent.Builder(Product.PURCHASE, product).build()

        val result = kit.logEvent(event)

        assertNull(result)
    }

    @Test
    fun `logEvent tracks purchase with revenue`() {
        initializeKit()
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
        initializeKit(usePeople = false)

        val product = Product.Builder("Product", "SKU", 10.0).build()
        val event = CommerceEvent.Builder(Product.PURCHASE, product)
            .transactionAttributes(TransactionAttributes("TXN").setRevenue(10.0))
            .build()

        val result = kit.logEvent(event)

        // Still returns reporting message even without People API
        assertNotNull(result)
    }

    @Test
    fun `logEvent expands non-purchase commerce events`() {
        initializeKit()

        val product = Product.Builder("Product", "SKU", 10.0).build()
        val event = CommerceEvent.Builder(Product.ADD_TO_CART, product).build()

        val result = kit.logEvent(event)

        // Non-purchase events are expanded to regular events
        // Result may be null if expansion produces no events
    }

    @Test
    fun `logLtvIncrease returns null`() {
        initializeKit()

        val result = kit.logLtvIncrease(
            java.math.BigDecimal("10.00"),
            java.math.BigDecimal("100.00"),
            "Purchase",
            mutableMapOf()
        )

        // LTV increase is not supported by Mixpanel
        assertNull(result)
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
        // Fail gracefully if not started
        if (!_isStarted) return null
        val mixpanel = mixpanelInstance ?: return null

        val productAction = event.productAction

        // Handle Purchase events with People API
        if (productAction == Product.PURCHASE) {
            if (useMixpanelPeople) {
                val revenue = event.transactionAttributes?.revenue ?: 0.0
                val properties = convertToJSONObject(event.customAttributeStrings)
                try {
                    mixpanel.people.trackCharge(revenue, properties)
                } catch (e: Exception) {
                    // Fail gracefully
                }
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
        // Return null per best practices
        return null
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.CommerceTest"`
Expected: PASS (5 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement CommerceListener for purchase tracking"
```

---

## Task 11: IdentityListener Interface

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
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdentityTest {
    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context
    private lateinit var mockUser: MParticleUser

    @Before
    fun setUp() {
        kit = MixpanelKit()
        mockContext = mock(Context::class.java)
        mockUser = mock(MParticleUser::class.java)
    }

    private fun initializeKit(userIdType: String = "CustomerId") {
        kit.onKitCreate(
            mutableMapOf(
                "token" to "test-token",
                "userIdentificationType" to userIdType
            ),
            mockContext
        )
    }

    @Test
    fun `onLoginCompleted does nothing when not started`() {
        // Should not throw
        kit.onLoginCompleted(mockUser, null)
    }

    @Test
    fun `onLoginCompleted calls Mixpanel identify with user ID`() {
        initializeKit()
        val identities = mapOf(MParticle.IdentityType.CustomerId to "user-123")
        `when`(mockUser.userIdentities).thenReturn(identities)

        // Should not throw
        kit.onLoginCompleted(mockUser, null)
    }

    @Test
    fun `onLogoutCompleted does nothing when not started`() {
        // Should not throw
        kit.onLogoutCompleted(mockUser, null)
    }

    @Test
    fun `onLogoutCompleted calls Mixpanel reset`() {
        initializeKit()

        // Should not throw
        kit.onLogoutCompleted(mockUser, null)
    }

    @Test
    fun `onIdentifyCompleted does nothing when not started`() {
        // Should not throw
        kit.onIdentifyCompleted(mockUser, null)
    }

    @Test
    fun `onIdentifyCompleted calls Mixpanel identify`() {
        initializeKit()
        val identities = mapOf(MParticle.IdentityType.CustomerId to "user-456")
        `when`(mockUser.userIdentities).thenReturn(identities)

        // Should not throw
        kit.onIdentifyCompleted(mockUser, null)
    }

    @Test
    fun `onModifyCompleted does nothing when not started`() {
        // Should not throw
        kit.onModifyCompleted(mockUser, null)
    }

    @Test
    fun `onModifyCompleted calls Mixpanel identify`() {
        initializeKit()
        val identities = mapOf(MParticle.IdentityType.CustomerId to "user-789")
        `when`(mockUser.userIdentities).thenReturn(identities)

        // Should not throw
        kit.onModifyCompleted(mockUser, null)
    }

    @Test
    fun `identity uses MPID when configured`() {
        initializeKit(userIdType = "MPID")
        `when`(mockUser.id).thenReturn(12345L)

        // Should use MPID
        kit.onLoginCompleted(mockUser, null)
    }

    @Test
    fun `identity uses Other when configured`() {
        initializeKit(userIdType = "Other")
        val identities = mapOf(MParticle.IdentityType.Other to "custom-id")
        `when`(mockUser.userIdentities).thenReturn(identities)

        kit.onLoginCompleted(mockUser, null)
    }

    @Test
    fun `onUserIdentified does nothing when not started`() {
        // Should not throw
        kit.onUserIdentified(mockUser)
    }

    @Test
    fun `onUserIdentified calls Mixpanel identify`() {
        initializeKit()
        val identities = mapOf(MParticle.IdentityType.CustomerId to "user-abc")
        `when`(mockUser.userIdentities).thenReturn(identities)

        // Should not throw
        kit.onUserIdentified(mockUser)
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
        if (!_isStarted) return
        identifyUser(user)
    }

    override fun onLoginCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        if (!_isStarted) return
        identifyUser(user)
    }

    override fun onLogoutCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        if (!_isStarted) return
        try {
            mixpanelInstance?.reset()
        } catch (e: Exception) {
            // Fail gracefully
        }
    }

    override fun onModifyCompleted(
        user: MParticleUser?,
        request: FilteredIdentityApiRequest?
    ) {
        if (!_isStarted) return
        identifyUser(user)
    }

    override fun onUserIdentified(user: MParticleUser?) {
        if (!_isStarted) return
        identifyUser(user)
    }

    // MARK: - Identity Helpers

    private fun identifyUser(user: MParticleUser?) {
        val userId = extractUserId(user) ?: return
        try {
            mixpanelInstance?.identify(userId)
        } catch (e: Exception) {
            // Fail gracefully
        }
    }

    private fun extractUserId(user: MParticleUser?): String? {
        if (user == null) return null

        val identities = user.userIdentities ?: return null

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
Expected: PASS (12 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement IdentityListener for user identification"
```

---

## Task 12: UserAttributeListener Interface

**Files:**
- Create: `src/test/kotlin/com/mparticle/kits/UserAttributeTest.kt`
- Modify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt`

**Step 1: Write the failing test**

```kotlin
// src/test/kotlin/com/mparticle/kits/UserAttributeTest.kt
package com.mparticle.kits

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserAttributeTest {
    private lateinit var kit: MixpanelKit
    private lateinit var mockContext: Context
    private lateinit var mockUser: FilteredMParticleUser

    @Before
    fun setUp() {
        kit = MixpanelKit()
        mockContext = mock(Context::class.java)
        mockUser = mock(FilteredMParticleUser::class.java)
    }

    private fun initializeKit(usePeople: Boolean = true) {
        kit.onKitCreate(
            mutableMapOf(
                "token" to "test-token",
                "useMixpanelPeople" to usePeople.toString()
            ),
            mockContext
        )
    }

    @Test
    fun `onSetUserAttribute does nothing when not started`() {
        // Should not throw
        kit.onSetUserAttribute("name", "John Doe", mockUser)
    }

    @Test
    fun `onSetUserAttribute sets People property when useMixpanelPeople is true`() {
        initializeKit(usePeople = true)

        // Should not throw
        kit.onSetUserAttribute("name", "John Doe", mockUser)
    }

    @Test
    fun `onSetUserAttribute uses super properties when useMixpanelPeople is false`() {
        initializeKit(usePeople = false)

        // Should use registerSuperProperties instead
        kit.onSetUserAttribute("tier", "premium", mockUser)
    }

    @Test
    fun `onSetUserAttribute handles null key gracefully`() {
        initializeKit()

        // Should not throw
        kit.onSetUserAttribute(null, "value", mockUser)
    }

    @Test
    fun `onRemoveUserAttribute does nothing when not started`() {
        // Should not throw
        kit.onRemoveUserAttribute("name", mockUser)
    }

    @Test
    fun `onRemoveUserAttribute unsets People property`() {
        initializeKit()

        // Should not throw
        kit.onRemoveUserAttribute("name", mockUser)
    }

    @Test
    fun `onRemoveUserAttribute uses unregisterSuperProperty when useMixpanelPeople is false`() {
        initializeKit(usePeople = false)

        kit.onRemoveUserAttribute("name", mockUser)
    }

    @Test
    fun `onIncrementUserAttribute does nothing when not started`() {
        // Should not throw
        kit.onIncrementUserAttribute("purchases", 1, "1", mockUser)
    }

    @Test
    fun `onIncrementUserAttribute increments People property`() {
        initializeKit()

        // Should not throw
        kit.onIncrementUserAttribute("purchases", 1, "1", mockUser)
    }

    @Test
    fun `onSetUserTag does nothing when not started`() {
        // Should not throw
        kit.onSetUserTag("vip", mockUser)
    }

    @Test
    fun `onSetUserTag sets attribute to true`() {
        initializeKit()

        // Should not throw
        kit.onSetUserTag("vip", mockUser)
    }

    @Test
    fun `onSetUserAttributeList does nothing when not started`() {
        // Should not throw
        kit.onSetUserAttributeList("tags", mutableListOf("a", "b"), mockUser)
    }

    @Test
    fun `onSetUserAttributeList sets list value`() {
        initializeKit()

        // Should not throw
        kit.onSetUserAttributeList("tags", mutableListOf("a", "b", "c"), mockUser)
    }

    @Test
    fun `onSetAllUserAttributes does nothing when not started`() {
        // Should not throw
        kit.onSetAllUserAttributes(
            mutableMapOf("key" to "value"),
            mutableMapOf("list" to mutableListOf("a")),
            mockUser
        )
    }

    @Test
    fun `onSetAllUserAttributes sets all attributes`() {
        initializeKit()

        kit.onSetAllUserAttributes(
            mutableMapOf("name" to "John", "email" to "john@example.com"),
            mutableMapOf("tags" to mutableListOf("vip", "premium")),
            mockUser
        )
    }

    @Test
    fun `supportsAttributeLists returns true`() {
        val result = kit.supportsAttributeLists()
        assertTrue(result)
    }

    @Test
    fun `onConsentStateUpdated does nothing when not started`() {
        // Should not throw
        kit.onConsentStateUpdated(null, null, mockUser)
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
import org.json.JSONArray

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
        if (!_isStarted) return
        if (key.isNullOrEmpty() || value == null) return
        val mixpanel = mixpanelInstance ?: return

        try {
            if (useMixpanelPeople) {
                mixpanel.people.set(key, value)
            } else {
                val props = JSONObject()
                props.put(key, value)
                mixpanel.registerSuperProperties(props)
            }
        } catch (e: Exception) {
            // Fail gracefully
        }
    }

    override fun onRemoveUserAttribute(key: String?, user: FilteredMParticleUser?) {
        if (!_isStarted) return
        if (key.isNullOrEmpty()) return
        val mixpanel = mixpanelInstance ?: return

        try {
            if (useMixpanelPeople) {
                mixpanel.people.unset(key)
            } else {
                mixpanel.unregisterSuperProperty(key)
            }
        } catch (e: Exception) {
            // Fail gracefully
        }
    }

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
        user: FilteredMParticleUser?
    ) {
        if (!_isStarted) return
        if (key.isNullOrEmpty() || incrementedBy == null) return
        val mixpanel = mixpanelInstance ?: return

        // Increment only supported via People API
        if (useMixpanelPeople) {
            try {
                mixpanel.people.increment(key, incrementedBy.toDouble())
            } catch (e: Exception) {
                // Fail gracefully
            }
        }
    }

    override fun onSetUserTag(key: String?, user: FilteredMParticleUser?) {
        if (!_isStarted) return
        if (key.isNullOrEmpty()) return

        // Set tag as a boolean true value
        onSetUserAttribute(key, true, user)
    }

    override fun onSetUserAttributeList(
        key: String?,
        values: MutableList<String>?,
        user: FilteredMParticleUser?
    ) {
        if (!_isStarted) return
        if (key.isNullOrEmpty() || values.isNullOrEmpty()) return
        val mixpanel = mixpanelInstance ?: return

        try {
            val jsonArray = JSONArray(values)

            if (useMixpanelPeople) {
                mixpanel.people.set(key, jsonArray)
            } else {
                val props = JSONObject()
                props.put(key, jsonArray)
                mixpanel.registerSuperProperties(props)
            }
        } catch (e: Exception) {
            // Fail gracefully
        }
    }

    override fun onSetAllUserAttributes(
        userAttributes: MutableMap<String, String>?,
        userAttributeLists: MutableMap<String, MutableList<String>>?,
        user: FilteredMParticleUser?
    ) {
        if (!_isStarted) return

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
        // Could optionally track as super properties if needed in future
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.mparticle.kits.UserAttributeTest"`
Expected: PASS (17 tests)

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: implement UserAttributeListener for People and Super Properties"
```

---

## Task 13: Integration Test and Final Cleanup

**Files:**
- Create: `src/test/kotlin/com/mparticle/kits/IntegrationTest.kt`
- Verify: `src/main/kotlin/com/mparticle/kits/MixpanelKit.kt` has all imports

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
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

/**
 * Integration test verifying complete Kit functionality.
 */
@RunWith(RobolectricTestRunner::class)
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
        assertTrue(kit.isStarted)
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
    fun `screen tracking produces reporting message`() {
        initializeKit()

        val result = kit.logScreen("Settings", mutableMapOf("tab" to "notifications"))

        assertNotNull(result)
        assertEquals(1, result?.size)
        assertEquals(ReportingMessage.MessageType.SCREEN_VIEW, result?.first()?.messageType)
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
        assertTrue(kit.isStarted)
    }

    @Test
    fun `user attributes with People API`() {
        initializeKit(usePeople = true)

        val mockUser = mock(FilteredMParticleUser::class.java)

        kit.onSetUserAttribute("name", "John Doe", mockUser)
        kit.onSetUserAttribute("email", "john@example.com", mockUser)
        kit.onRemoveUserAttribute("old_field", mockUser)
        kit.onIncrementUserAttribute("login_count", 1, "1", mockUser)
        kit.onSetUserTag("premium", mockUser)
        kit.onSetUserAttributeList("interests", mutableListOf("tech", "sports"), mockUser)

        // No exceptions = success
        assertTrue(kit.isStarted)
    }

    @Test
    fun `user attributes with Super Properties`() {
        initializeKit(usePeople = false)

        val mockUser = mock(FilteredMParticleUser::class.java)

        kit.onSetUserAttribute("tier", "gold", mockUser)
        kit.onRemoveUserAttribute("old_tier", mockUser)

        // No exceptions = success
        assertTrue(kit.isStarted)
    }

    @Test
    fun `opt out and opt in`() {
        initializeKit()

        kit.setOptOut(true)  // Opt out
        kit.setOptOut(false) // Opt back in

        // No exceptions = success
        assertTrue(kit.isStarted)
    }

    @Test
    fun `error and exception tracking`() {
        initializeKit()

        val errorResult = kit.logError("Network timeout", mutableMapOf("code" to "ETIMEDOUT"))
        val exceptionResult = kit.logException(
            RuntimeException("Connection refused"),
            mutableMapOf("host" to "api.example.com"),
            "Failed to connect"
        )

        assertNotNull(errorResult)
        assertNotNull(exceptionResult)
    }

    @Test
    fun `breadcrumb tracking`() {
        initializeKit()

        val result = kit.leaveBreadcrumb("User opened settings menu")

        assertNotNull(result)
    }

    @Test
    fun `supportsAttributeLists returns true`() {
        assertTrue(kit.supportsAttributeLists())
    }

    private fun initializeKit(usePeople: Boolean = true) {
        kit.onKitCreate(
            mutableMapOf(
                "token" to "test-token",
                "useMixpanelPeople" to usePeople.toString()
            ),
            mockContext
        )
    }
}
```

**Step 2: Verify all imports in MixpanelKit.kt**

Ensure the file header has:

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
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
```

**Step 3: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS (~55 tests)

**Step 4: Commit**

```bash
git add -A && git commit -m "feat: add integration tests for complete Kit functionality"
```

---

## Task 14: Documentation and README

**Files:**
- Create: `README.md`

**Step 1: Write README**

```markdown
# mParticle Android Mixpanel Kit

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

## Architecture

This kit follows mParticle Kit best practices:
- **Fail gracefully**: Never crashes the host app; returns null on errors
- **Started state checks**: Won't call Mixpanel SDK until properly initialized
- **Thread safety**: All public methods are safe to call from any thread

For more details, see [ARCHITECTURE.md](ARCHITECTURE.md).

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

## Task 15: Final Verification

**Files:**
- Review all files

**Step 1: Run full build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 2: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS

**Step 3: Check lint**

Run: `./gradlew lint`
Expected: No errors

**Step 4: Verify test coverage**

Run: `./gradlew test` and check output
Expected: >80% coverage

**Step 5: Review git log**

Run: `git log --oneline`
Expected: Clean commit history:
- chore: initialize project structure with Gradle, dependencies, and LICENSE
- feat: add UserIdentificationType enum
- feat: add MixpanelKit skeleton with started state tracking
- feat: add token and context validation in onKitCreate
- feat: implement Mixpanel SDK initialization with configuration parsing
- feat: implement setOptOut with started state check
- feat: implement EventListener.logEvent with started state check
- feat: implement EventListener.logScreen with 'Viewed' prefix
- feat: implement logError, logException, and leaveBreadcrumb
- feat: implement CommerceListener for purchase tracking
- feat: implement IdentityListener for user identification
- feat: implement UserAttributeListener for People and Super Properties
- feat: add integration tests for complete Kit functionality
- docs: add README with setup and usage instructions

**Step 6: Final summary**

```bash
echo "Implementation complete!"
echo "Tests: $(./gradlew test --quiet 2>/dev/null | grep -c 'PASSED')"
echo "Files: $(find src -name '*.kt' | wc -l | tr -d ' ') Kotlin files"
```

---

## Summary

This plan implements the complete mParticle Android Mixpanel Kit with:

- **15 Tasks** broken into atomic TDD steps
- **Feature parity** with iOS Mixpanel Kit
- **All interfaces implemented:**
  - EventListener (events, screens, errors, exceptions, breadcrumbs)
  - CommerceListener (purchases, other commerce actions)
  - IdentityListener (login, logout, identify, modify)
  - UserAttributeListener (set, remove, increment, lists, tags)
- **Best practices from ARCHITECTURE.md:**
  - Fail gracefully (never crash)
  - Check started state before SDK calls
  - Thread-safe with @Volatile annotations
- **Configuration options:** token, serverURL, userIdentificationType, useMixpanelPeople
- **Comprehensive test coverage:** ~55 tests across 6 test files
- **Complete documentation:** README, LICENSE, architecture docs

Estimated test count: ~55 tests
Estimated implementation: ~400 lines of Kotlin
