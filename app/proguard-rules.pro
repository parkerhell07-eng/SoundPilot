# SoundPilot — قوانین ProGuard/R8
# Media3 و Compose قوانین consumer خود را همراه کتابخانه‌ها می‌آورند.

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.soundpilot.app.**$$serializer { *; }
-keepclassmembers class com.soundpilot.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.soundpilot.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
