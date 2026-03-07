import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-datetime")) {
            useVersion(libs.versions.kotlinxDatetime.get())
            because("backend и contract собраны под kotlinx-datetime ${libs.versions.kotlinxDatetime.get()}, Compose подтягивает 0.7.x и ломает Clock.System на рантайме")
        }
    }
}

dependencies {
    implementation(project(":app-shared"))
    implementation(project(":backend"))
    implementation(libs.zxingCore)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "org.expert.link.app.desktop.DesktopMainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "expert-link"
            packageVersion = "1.0.0"
        }
    }
}
