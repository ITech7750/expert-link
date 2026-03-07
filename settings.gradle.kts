pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
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
include(":backend:application")
include(":backend:infra")
include(":backend:runtime")
include(":bootstrap")
include(":simulator")
include(":composeApp")

project(":backend:data").projectDir = file("backend/data")
project(":backend:application").projectDir = file("backend/application")
project(":backend:infra").projectDir = file("backend/infra")
project(":backend:runtime").projectDir = file("backend/runtime")
