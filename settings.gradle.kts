pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Plugins
            version("kotlin", "2.1.10")
            plugin("multiplatform", "org.jetbrains.kotlin.multiplatform").versionRef("kotlin")
            plugin("serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
            plugin("kover", "org.jetbrains.kotlinx.kover").version("0.8.3")
            plugin("ksp", "com.google.devtools.ksp").version("2.1.10-1.0.30")
            plugin("maven", "com.vanniktech.maven.publish").version("0.30.0")

            // Kaccelero
            version("kaccelero", "0.6.8")
            library("kaccelero-core", "dev.kaccelero", "core").versionRef("kaccelero")

            // Ktor
            version("ktor", "3.1.3")
            library("ktor-http", "io.ktor", "ktor-http").versionRef("ktor")
            library("ktor-network", "io.ktor", "ktor-network").versionRef("ktor")
            library("ktor-network-tls", "io.ktor", "ktor-network-tls").versionRef("ktor")

            // Tests
            library("tests-mockk", "io.mockk:mockk:1.13.12")
            library("tests-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        }
    }
}

rootProject.name = "kdriver-proxy"
include(":proxy")
