plugins {
    kotlin("multiplatform") version "2.4.10" apply false
    kotlin("plugin.compose") version "2.4.10" apply false
    id("org.jetbrains.compose") version "1.12.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.4.1" apply false
    id("com.android.application") version "9.4.1" apply false
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
}
allprojects {
    group = "moe.tlaster.zenlessui"
    version = providers.gradleProperty("VERSION_NAME").get()
}
