import java.util.Properties

plugins {
    kotlin("jvm")
    id("net.neoforged.moddev")
}

evaluationDependsOn(":common")

val modId = property("mod_id") as String
val modVersion = property("mod_version") as String
val mcVersion = property("mcVersion") as String

// Per-target values (NeoForge build, Minecraft range) live in gradle/mc/<mcVersion>.properties.
val mcFile = rootProject.file("gradle/mc/$mcVersion.properties")
if (!mcFile.isFile) {
    val available = rootProject.file("gradle/mc").listFiles().orEmpty().map { it.nameWithoutExtension }.sorted()
    throw GradleException("Unsupported mcVersion '$mcVersion': gradle/mc/$mcVersion.properties not found (available: $available)")
}
val mc = Properties().apply { mcFile.inputStream().use(::load) }

base.archivesName = "armor-status-neoforge"
version = "$modVersion+mc$mcVersion"
group = property("mod_group_id") as String

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    maven("https://repo.nyon.dev/releases") {
        content { includeGroup("dev.nyon") }
    }
}

val commonMain = project(":common").extensions.getByType<SourceSetContainer>()["main"]

neoForge {
    version = mc.getProperty("neo_version")

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(commonMain)
        }
    }

    runs {
        register("client") {
            client()
        }
    }
}

dependencies {
    implementation(project(":common"))
    // KotlinLangForge is a mod loader provider; it is not shaded and not a remapped mod dependency.
    implementation("dev.nyon:KotlinLangForge:2.15.0-k2.4.21-3.1+neoforge")
}

// Ship common's classes inside the mod jar (no third-party libraries are shaded).
tasks.jar {
    from(commonMain.output)
}
