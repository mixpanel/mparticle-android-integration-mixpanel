plugins {
    id("com.android.library") version "8.2.0"
    id("org.jetbrains.kotlin.android") version "1.9.22"
    id("mixpanel.maven-publish")
}

// Session Replay is an optional dependency. The kit compiles against the oldest supported
// version so it only uses API that exists in every supported release; CI runs the unit tests
// against newer versions by passing -PsessionReplayTestVersion=<version>.
val sessionReplayMinVersion = "1.0.6"
val sessionReplayTestVersion = providers.gradleProperty("sessionReplayTestVersion").getOrElse(sessionReplayMinVersion)

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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    lint {
        // Disable mParticle's lint detector that crashes with Kotlin DSL
        disable += "MParticleVersionInconsistency"
    }
}

// Session Replay 1.3.0+ pulls in AndroidX libraries that require compileSdk 35. They are only
// on the unit test classpath (for the Session Replay compatibility matrix), so skip the AAR
// metadata check for unit tests rather than raising the kit's compileSdk in this change.
tasks.configureEach {
    if (name.matches(Regex("check.*UnitTestAarMetadata"))) enabled = false
}

dependencies {
    api("com.mparticle:android-kit-base:5.78.5")
    api("com.mixpanel.android:mixpanel-android:8.4.0")
    compileOnly("com.mixpanel.android:mixpanel-android-session-replay:$sessionReplayMinVersion")
    testImplementation("com.mixpanel.android:mixpanel-android-session-replay:$sessionReplayTestVersion")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
}
