plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    api(project(":contract"))

    implementation(project(":backend:runtime"))
    implementation(project(":backend:data"))
    implementation(project(":backend:application"))
    implementation(libs.kotlinxDatetime)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testCore)
}
