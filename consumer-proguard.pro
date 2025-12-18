# consumer-proguard.pro
# mParticle Mixpanel Kit ProGuard rules

# Keep the Kit class
-keep class com.mparticle.kits.MixpanelKit { *; }

# Keep the UserIdentificationType enum
-keep class com.mparticle.kits.UserIdentificationType { *; }
