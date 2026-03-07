plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":app-shared"))
    implementation(project(":backend"))
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxActivityCompose)
    implementation(libs.zxingCore)
}
