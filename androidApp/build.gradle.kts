plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}
android {
    namespace = "moe.tlaster.zenlessui.gallery.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "moe.tlaster.zenlessui.gallery"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":gallery"))
    implementation("androidx.activity:activity-compose:1.13.0")
}
