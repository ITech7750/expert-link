import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
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
    jvmToolchain(17)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
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

android {
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

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
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
