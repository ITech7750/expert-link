plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":backend:data"))
    implementation(libs.bundles.kotlinxCore)
    implementation(libs.slf4jApi)
    implementation(libs.kotlinLogging)

    testImplementation(project(":backend:infra"))
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testCore)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
