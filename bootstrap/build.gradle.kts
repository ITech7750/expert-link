plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinSerialization)
    application
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
