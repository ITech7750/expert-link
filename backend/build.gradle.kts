plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":contract"))

    implementation(project(":backend:runtime"))
    implementation(project(":backend:data"))
    implementation(project(":backend:application"))
    implementation(libs.kotlinxDatetime)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.bundles.testCore)
    testImplementation(project(":backend:infra"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
