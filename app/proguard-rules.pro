# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep data models
-keep class com.example.model.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Moshi & Retrofit
-dontwarn com.squareup.moshi.**
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Line number preservation for production stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

