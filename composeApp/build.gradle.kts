import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication) apply false
}

val androidSdkAvailable = System.getenv("ANDROID_SDK_ROOT") != null || System.getenv("ANDROID_HOME") != null
if (androidSdkAvailable) {
    apply(plugin = "com.android.application")
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-datetime")) {
            useVersion(libs.versions.kotlinxDatetime.get())
            because("backend и contract используют kotlinx-datetime ${libs.versions.kotlinxDatetime.get()}, Compose не должен поднимать runtime до 0.7.x")
        }
    }
}

kotlin {
    if (androidSdkAvailable) {
        androidTarget {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":contract"))
            implementation(libs.kotlinxCoroutinesCore)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
        }

        val jvmMain by getting {
            dependencies {
                implementation(project(":backend"))
                implementation(libs.zxingCore)
                implementation(compose.desktop.currentOs)
            }
        }

        if (androidSdkAvailable) {
            val androidMain by getting {
                dependencies {
                    implementation(project(":backend"))
                    implementation(libs.androidxCoreKtx)
                    implementation(libs.androidxActivityCompose)
                    implementation(libs.zxingCore)
                    implementation(libs.googleWebrtc)
                }
            }
        }
    }
}

if (androidSdkAvailable) {
    extensions.configure<ApplicationExtension>("android") {
        namespace = "org.expert.link.app.android"
        compileSdk = 35

        defaultConfig {
            applicationId = "org.expert.link.app.android"
            minSdk = 26
            targetSdk = 35
            versionCode = 1
            versionName = "0.1.0"
        }

        buildFeatures {
            compose = true
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.expert.link.app.desktop.DesktopMainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "expert-link"
            packageVersion = "1.0.0"
        }
    }
}
