pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "expert-link"

include(":contract")
include(":backend")
include(":backend:data")
include(":backend:engine")
include(":backend:infra")
include(":backend:runtime")
include(":bootstrap")
include(":simulator")

project(":backend:data").projectDir = file("backend/data")
project(":backend:engine").projectDir = file("backend/engine")
project(":backend:infra").projectDir = file("backend/infra")
project(":backend:runtime").projectDir = file("backend/runtime")
