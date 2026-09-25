# ProGuard rules for iOS Keyboard Clone

# Preserve IME Service
-keep public class org.iosclone.keyboard.service.IOSInputMethodService { *; }

# Preserve Compose runtime
-keep class androidx.compose.** { *; }

# Keep data models
-keepclassmembers class org.iosclone.keyboard.emoji.** { *; }
-keepclassmembers class org.iosclone.keyboard.layout.** { *; }
-keepclassmembers class org.iosclone.keyboard.clipboard.** { *; }
