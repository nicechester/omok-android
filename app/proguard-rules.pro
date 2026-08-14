# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Kotlin
-keepclassmembers class * extends kotlin.coroutines.AbstractCoroutineContextElement {
    *** get(...);
}

# Keep data classes
-keepclassmembers class io.github.nicechester.omok.** {
    *** get(...);
}
