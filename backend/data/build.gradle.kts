plugins {
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(17)
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir("src/main/kotlin")
            dependencies {
                api(libs.kotlinxDatetime)
                api(libs.kotlinxSerializationJson)
                api(libs.kotlinxCoroutinesCore)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmTest {
            kotlin.srcDir("src/test/kotlin")
            dependencies {
                implementation(libs.bundles.testCore)
            }
        }
    }
}
