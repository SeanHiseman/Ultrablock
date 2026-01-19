# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Stripe classes
-keep class com.stripe.** { *; }

# Keep Room entities
-keep class com.ultrablock.data.local.entity.** { *; }
