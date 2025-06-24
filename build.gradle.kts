// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    // Compose
    alias(libs.plugins.compose.compiler) apply false

    // Kotlin
    alias(libs.plugins.jetbrains.kotlin.android) apply false

    // 참고 : https://cocoslime.github.io/blog/Android-Library-Maven-Central/
    alias(libs.plugins.maven.publish) apply false // 대체 플러그인
}