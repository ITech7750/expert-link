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
    implementation(libs.bundles.kotlinxCore)
    implementation(libs.bundles.ktorClient)
    implementation(libs.slf4jApi)
    implementation(libs.kotlinLogging)
    implementation(libs.zxingCore)
    implementation("org.apache.pdfbox:pdfbox:2.0.29")

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testCore)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
