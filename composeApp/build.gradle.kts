import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-datetime")) {
            useVersion(libs.versions.kotlinxDatetime.get())
            because("backend и contract используют kotlinx-datetime ${libs.versions.kotlinxDatetime.get()}, Compose не должен поднимать runtime до 0.7.x")
        }
    }
}

val webrtcClassifier: String by lazy {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    when {
        osName.contains("win") -> "windows-x86_64"
        osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64")) -> "macos-aarch64"
        osName.contains("mac") -> "macos-x86_64"
        osArch.contains("aarch64") || osArch.contains("arm64") -> "linux-aarch64"
        osArch.contains("arm") -> "linux-aarch32"
        else -> "linux-x86_64"
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

            // decompose
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.essenty.lifecycle)

            // koin
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        val jvmMain by getting {
            dependencies {
                implementation(project(":backend"))
                implementation(project(":database"))
                implementation(libs.zxingCore)
                implementation("org.apache.pdfbox:pdfbox:2.0.29")
                implementation(compose.desktop.currentOs)
                implementation(libs.webrtcJava)
                runtimeOnly(libs.logbackClassic)
                runtimeOnly("dev.onvoid.webrtc:webrtc-java:${libs.versions.webrtcJava.get()}:$webrtcClassifier")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(project(":backend"))
                implementation(project(":database"))
                implementation(libs.androidxCoreKtx)
                implementation(libs.androidxActivityCompose)
                implementation(libs.zxingCore)
                implementation(libs.zxingAndroidEmbedded)
                implementation(libs.googleWebrtc)

                implementation(libs.koin.android)
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

    lint {
        disable += "NullSafeMutableLiveData"
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
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

compose.desktop {
    application {
        mainClass = "org.expert.link.app.desktop.DesktopMainKt"
        jvmArgs += listOf(
            "--add-opens",
            "webrtc.java/dev.onvoid.webrtc=ALL-UNNAMED",
            "--add-opens",
            "webrtc.java/dev.onvoid.webrtc.logging=ALL-UNNAMED",
            "--add-opens",
            "webrtc.java/dev.onvoid.webrtc.media=ALL-UNNAMED",
            "--add-opens",
            "webrtc.java/dev.onvoid.webrtc.media.audio=ALL-UNNAMED",
            "--add-opens",
            "webrtc.java/dev.onvoid.webrtc.media.video=ALL-UNNAMED",
        )
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "expert-link"
            packageVersion = "1.0.0"
        }
    }
}
