plugins {
    kotlin("jvm") version "2.4.21" apply false
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("com.modrinth.minotaur") version "2.10.0" apply false
    id("com.diffplug.spotless") version "8.10.4"
}

repositories {
    mavenCentral() // resolves the ktlint artifacts for Spotless
}

// Kotlin and Kotlin DSL only; ktlint reads the repo .editorconfig (ktlint_official, 120 columns).
// Applying the plugin here also hooks spotlessCheck into the root `check`.
spotless {
    kotlin {
        target("common/src/**/*.kt", "neoforge/src/**/*.kt")
        ktlint("1.8.0")
    }
    kotlinGradle {
        target("*.gradle.kts", "common/*.gradle.kts", "neoforge/*.gradle.kts")
        ktlint("1.8.0")
    }
}

// Pin the wrapper to the MDK baseline (9.2.1), not the Gradle used to generate it.
tasks.wrapper {
    gradleVersion = "9.2.1"
    distributionType = Wrapper.DistributionType.BIN
}

val supportedMcVersions = listOf("26.1.2", "26.2", "26.3")

// Builds every target sequentially. Each target is a separate wrapper invocation because a single Gradle
// build resolves one -PmcVersion (nested GradleBuild runs of the same directory are rejected by Gradle).
val wrapperScript = file(if (System.getProperty("os.name").startsWith("Windows")) "gradlew.bat" else "gradlew")
val buildTasks =
    supportedMcVersions.map { mc ->
        tasks.register<Exec>("build-mc$mc") {
            group = "build"
            description = "Runs 'build' for Minecraft $mc."
            workingDir = projectDir
            commandLine(wrapperScript.absolutePath, "build", "-PmcVersion=$mc")
        }
    }
buildTasks.zipWithNext { previous, next -> next.configure { mustRunAfter(previous) } }

tasks.register("buildAll") {
    group = "build"
    description = "Runs 'build' for every supported Minecraft version (${supportedMcVersions.joinToString()})."
    dependsOn(buildTasks)
}
