plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":contract"))
                implementation(libs.kotlinxCoroutinesCore)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended)
            }
        }
    }
}
