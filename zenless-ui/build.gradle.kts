import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.JavadocJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "moe.tlaster.zenlessui"
        compileSdk = 37
        minSdk = 31
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    iosArm64()
    iosSimulatorArm64()
    wasmJs { browser() }
    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.ui)
            implementation(compose.animation)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        getByName("androidMain").dependencies { implementation("androidx.activity:activity-compose:1.13.0") }
        getByName("desktopTest").dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.compose.ui:ui-test-junit4:1.12.1")
        }
    }
}
mavenPublishing {
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty(), sourcesJar = true))
    publishToMavenCentral()
    signAllPublications()
    coordinates("moe.tlaster.zenlessui", "zenless-ui")
    pom {
        name.set("zenless-ui")
        description.set("Fixed dark-theme Compose Multiplatform components with expressive motion")
        url.set("https://github.com/Tlaster/zenless-ui")
        licenses { license { name.set("MIT License"); url.set("https://opensource.org/licenses/MIT") } }
        developers { developer { id.set("Tlaster"); name.set("Tlaster"); url.set("https://github.com/Tlaster") } }
        scm {
            url.set("https://github.com/Tlaster/zenless-ui")
            connection.set("scm:git:git://github.com/Tlaster/zenless-ui.git")
            developerConnection.set("scm:git:ssh://git@github.com/Tlaster/zenless-ui.git")
        }
    }
}

tasks.withType<Jar>().configureEach {
    from(listOf(rootProject.file("LICENSE"), rootProject.file("THIRD_PARTY_NOTICES.md"))) { into("META-INF") }
}
extensions.configure<SigningExtension> {
    setRequired { gradle.taskGraph.allTasks.any { it is PublishToMavenRepository } }
}
