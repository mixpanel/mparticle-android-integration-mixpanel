# consumer-proguard.pro
# mParticle Mixpanel Kit ProGuard rules

# Keep the Kit class
-keep class com.mparticle.kits.MixpanelKit { *; }

# Keep the UserIdentificationType enum
-keep class com.mparticle.kits.UserIdentificationType { *; }

# Keep Session Replay configuration class
-keep class com.mparticle.kits.SessionReplayConfiguration { *; }

# Mixpanel Session Replay SDK (optional dependency, accessed via reflection)
-keep class com.mixpanel.android.sessionreplay.MPSessionReplay { *; }
-keep class com.mixpanel.android.sessionreplay.models.MPSessionReplayConfig { *; }
-keep class com.mixpanel.android.sessionreplay.sensitive_views.AutoMaskedView { *; }
-dontwarn com.mixpanel.android.sessionreplay.**
