// Root build file — SoundPilot
// نسخه‌ها در gradle/libs.versions.toml نگهداری می‌شوند.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
