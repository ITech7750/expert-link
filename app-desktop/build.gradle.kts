plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-datetime")) {
            useVersion(libs.versions.kotlinxDatetime.get())
            because("backend и contract собраны на kotlinx-datetime ${libs.versions.kotlinxDatetime.get()}, runtime должен использовать ту же версию")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":composeApp"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "org.expert.link.app.desktop.DesktopMainKt"
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
