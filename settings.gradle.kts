pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    // Downloads a matching JDK on demand (ModDevGradle's downloadAssets needs a Java 21 launcher beside our JDK 25).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "armor-status-neoforge"

include("common", "neoforge")
