-keepattributes *Annotation*
-keep class com.carbonwise.connect.data.model.** { *; }
-keep class com.carbonwise.connect.data.remote.** { *; }

# Fix for Missing classes detected while running R8
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
