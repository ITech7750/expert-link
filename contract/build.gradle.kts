plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    api(libs.kotlinxDatetime)
    api(libs.kotlinxSerializationJson)
    api(libs.kotlinxCoroutinesCore)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testCore)
}
