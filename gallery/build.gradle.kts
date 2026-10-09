import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
}
kotlin {
    android {
        namespace = "moe.tlaster.zenlessui.gallery"
        compileSdk = 37
        minSdk = 31
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "Gallery"; isStatic = true }
    }
    wasmJs { browser(); binaries.executable() }
    sourceSets {
        commonMain.dependencies { implementation(project(":zenless-ui")) }
        getByName("androidMain").dependencies { implementation("androidx.activity:activity-compose:1.13.0") }
        getByName("wasmJsMain").dependencies { implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0") }
        getByName("desktopMain").dependencies { implementation(compose.desktop.currentOs) }
        commonTest.dependencies { implementation(kotlin("test")) }
        getByName("desktopTest").dependencies { implementation("org.jetbrains.compose.ui:ui-test-junit4:1.12.1") }
    }
}
compose.desktop {
    application {
        mainClass = "moe.tlaster.zenlessui.gallery.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Zenless UI Gallery"
            packageVersion = "0.1.0"
            description = "zenless-ui component gallery"
            vendor = "Tlaster"
            modules("jdk.unsupported")
            windows { menuGroup = "Zenless UI"; upgradeUuid = "984f7a50-34fd-4f41-a91c-b9e237c33d42" }
            linux { packageName = "zenless-ui-gallery" }
            macOS {
                bundleID = "moe.tlaster.zenlessui.gallery"
                // jpackage requires a nonzero major version for the macOS bundle.
                packageVersion = "1.0.0"
            }
        }
    }
}
