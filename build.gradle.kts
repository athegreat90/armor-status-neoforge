plugins {
    kotlin("jvm") version "2.4.21" apply false
    id("net.neoforged.moddev") version "2.0.148" apply false
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
val buildTasks = supportedMcVersions.map { mc ->
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
