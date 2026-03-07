plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":backend"))
    implementation(libs.bundles.kotlinxCore)
    implementation(libs.slf4jApi)
    implementation(libs.kotlinLogging)
    implementation(libs.logbackClassic)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testCore)
}

application {
    mainClass = "org.expert.link.mesh.bootstrap.MeshNodeCliKt"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
