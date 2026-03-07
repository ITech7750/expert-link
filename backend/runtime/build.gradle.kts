plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":backend:data"))
    implementation(project(":backend:application"))
    implementation(project(":backend:infra"))
    implementation(libs.bundles.kotlinxCore)
    implementation(libs.bundles.ktorServer)
    implementation(libs.slf4jApi)
    implementation(libs.kotlinLogging)
    implementation(libs.logbackClassic)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testCore)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
