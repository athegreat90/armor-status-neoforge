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

// Pin the wrapper (version, type and checksum) here, not to whatever Gradle runs the task, so re-running
// `wrapper` cannot silently change it. 9.8.1 is the newest stable release, verified with `check build` on all
// three targets. Kotlin documents KGP 2.4.20-2.4.21 as tested up to Gradle 9.7.0, so 9.8.1 is outside that range.
// Fallback if it ever breaks: 9.7.1 (sha256 acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a).
// Run the task twice so the new wrapper regenerates its own scripts.
tasks.wrapper {
    gradleVersion = "9.8.1"
    distributionType = Wrapper.DistributionType.BIN
    distributionSha256Sum = "dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f"
}

// Reproducible archives: no timestamps and a stable entry order, so two builds on one machine give the same jar bytes.
allprojects {
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
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
