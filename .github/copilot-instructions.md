# Copilot Instructions for mParticle Android Integration for Mixpanel

## Repository Overview

**Android library kit** integrating Mixpanel analytics with mParticle Android SDK. Small focused repo (~600KB): 622 lines of Kotlin across 3 main files, 12 test files.

**Stack**: Kotlin, Gradle 8.5 (Kotlin DSL), Android Library (compileSdk 34, minSdk 21), JUnit 4, Mockito, Robolectric. Dependencies: mParticle Kit Base 5.+, Mixpanel Android SDK 7.+.

## Critical Build Prerequisites

**MUST HAVE** before any build attempt:
1. Android SDK installed with `ANDROID_SDK_ROOT` or `ANDROID_HOME` set
2. `local.properties` file in project root: `sdk.dir=/path/to/android/sdk` (or `sdk.dir=$ANDROID_SDK_ROOT`)
3. Java 17 installed

**Common Build Error**: "Plugin [id: 'com.android.library'] was not found" → Missing `local.properties` or Android SDK not configured.

## Build & Test Commands

**Build**: `./gradlew clean build` (30-120s first time, outputs to `build/outputs/aar/`)
**Test**: `./gradlew testDebugUnitTest` (20-60s, reports at `build/reports/tests/testDebugUnitTest/index.html`)
**Full Validation**: `./gradlew clean testDebugUnitTest` (ALWAYS run before committing)
**Lint**: `./gradlew lint` (Note: `MParticleVersionInconsistency` and `GradleDynamicVersion` lints intentionally disabled)

**Testing**: JUnit 4 + Mockito + Robolectric. Use `TestableMixpanelKit` for tests (exposes protected setters). Mocks in `src/test/kotlin/com/mparticle/kits/mocks/`. ALWAYS follow existing test patterns.

## Project Structure

**Root Files**: `build.gradle.kts` (Kotlin DSL config), `settings.gradle.kts` (repos: Google, Maven Central), `gradle.properties` (AndroidX=true, JVM 2GB), `consumer-proguard.pro` (ProGuard rules), `local.properties` (SDK location, gitignored).

**Source**: `src/main/kotlin/com/mparticle/kits/`:
- `MixpanelKit.kt` (589 lines) - Main kit, implements EventListener, CommerceListener, IdentityListener, UserAttributeListener
- `UserIdentificationType.kt` (17 lines) - Enum: CUSTOMER_ID, MPID, OTHER, OTHER_2, OTHER_3, OTHER_4
- `Constants.kt` (16 lines) - Config keys (KEY_TOKEN, KEY_SERVER_URL, KEY_USER_ID_TYPE, KEY_USE_PEOPLE), LOG_TAG

**Tests**: `src/test/kotlin/com/mparticle/kits/`: MixpanelKitTest, EventForwardingTest, CommerceTest, IdentityTest, UserAttributeTest, ErrorHandlingTest, IntegrationTest (largest, 11K), UserIdentificationTypeTest, TestableMixpanelKit, TestDataProvider, mocks/.

## Coding Guidelines

**Error Handling**: Try-catch around Mixpanel SDK calls, log with `Log.e(LOG_TAG, msg, t)`, return empty lists/null (don't propagate to mParticle). Exception: validation errors in `onKitCreate()` throw `IllegalArgumentException`.

**Null Safety**: Check `_isStarted` flag before processing. Validate `mixpanelInstance` not null. Use Kotlin nullable types.

**Thread Safety**: `mixpanelInstance` and `_isStarted` are `@Volatile`.

**Style**: Kotlin official style (`kotlin.code.style=official`). Minimal logging: DEBUG for operations, WARN for non-critical, ERROR for exceptions.

**Configuration**: `onKitCreate()` receives settings map. Required: `token` (validates non-empty). Optional: `serverURL`, `userIdentificationType`, `useMixpanelPeople`.

**Testing Pattern**: Extend `TestableMixpanelKit`, mock `Context`, use Robolectric for Android deps, test file: `{Feature}Test.kt`.

## Common Tasks

**Add Feature**: Modify `MixpanelKit.kt`, add constants to `Constants.kt`, create tests, run `./gradlew testDebugUnitTest`, update README.md.

**Add Config Option**: Add to `Constants.kt`, parse in `onKitCreate()`, test in `MixpanelKitTest.kt`, document in README.md.

**Fix Bug**: Write failing test, fix code, verify test passes, run full build.

## Key Architecture Points

- `MixpanelKit` manages Mixpanel SDK instance, validates token on init, tracks events/commerce/identity/attributes
- Thread-safe lazy initialization in `onKitCreate()`
- All event handlers return `List<ReportingMessage>` or null (mParticle API contract)
- Dynamic versions for mParticle/Mixpanel deps (5.+, 7.+) to maintain compatibility
- Kit loaded by mParticle via reflection - maintain API compatibility
- No CI/CD workflows configured, no publishing config (external), library-only (no runnable app)

## Validation Checklist

Before committing: Build passes (`./gradlew clean build`), tests pass (`./gradlew testDebugUnitTest`), no new lint warnings, Kotlin official style, new APIs documented in README.md, backward compatible with mParticle 5.+ and Mixpanel 7.+.

## Troubleshooting

**"Plugin not found"**: Verify `local.properties` with `sdk.dir`, check `ANDROID_SDK_ROOT` env var, verify network for plugin downloads.
**OutOfMemoryError**: Increase `gradle.properties`: `org.gradle.jvmargs=-Xmx4096m`.
**Robolectric errors**: Check Android SDK configured, verify `testOptions.unitTests.isIncludeAndroidResources = true`.
**Mockito errors**: Initialize mocks in `@Before`, use Mockito-Kotlin for Kotlin classes.

## Trust These Instructions

Only search/explore if information here is incomplete or incorrect. This file contains validated build steps, project structure, and patterns specific to this repository.
