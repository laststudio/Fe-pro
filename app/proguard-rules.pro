# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Vendored androidx.navigationevent.compose ABI consumed by the miuix-navigation3
# binary. Keep the exact class/member names it links against.
-keep class androidx.navigationevent.compose.** { *; }
-dontwarn androidx.navigationevent.compose.**

# WebView JavaScript bridge used by ChangyanWebLoginActivity.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Shizuku manager reflects on these API methods at runtime.
-keep class rikka.shizuku.Shizuku { *; }

# kotlinx.coroutines debug agent, referenced via reflection in some toolchains.
-dontwarn kotlinx.coroutines.debug.**
