plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinSerialization)
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
