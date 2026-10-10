import java.util.Properties
import java.util.jar.JarFile

plugins {
    kotlin("jvm")
    id("net.neoforged.moddev")
}

evaluationDependsOn(":common")

val modId = property("mod_id") as String
val modVersion = property("mod_version") as String
val modName = property("mod_name") as String
val modLicense = property("mod_license") as String
val modAuthors = property("mod_authors") as String
val mcVersion = property("mcVersion") as String

// Per-target values (NeoForge build, Minecraft range) live in gradle/mc/<mcVersion>.properties.
val mcFile = rootProject.file("gradle/mc/$mcVersion.properties")
if (!mcFile.isFile) {
    val available =
        rootProject
            .file("gradle/mc")
            .listFiles()
            .orEmpty()
            .map { it.nameWithoutExtension }
            .sorted()
    throw GradleException(
        "Unsupported mcVersion '$mcVersion': gradle/mc/$mcVersion.properties not found (available: $available)",
    )
}
val mc = Properties().apply { mcFile.inputStream().use(::load) }

base.archivesName = "armor-status-neoforge"
version = "$modVersion+mc$mcVersion"
group = property("mod_group_id") as String

// Only the F1 "hide GUI" check differs between targets: 26.1.x reads Options#hideGui, 26.2 and later read
// Hud#isHidden. Everything else compiles unchanged on all three, so just two overlay source dirs exist and
// 26.2 and 26.3 share src/v26_2 (a separate v26_3 would be added only if a real difference appears).
val versionOverlay = if (mcVersion.startsWith("26.1")) "v26_1" else "v26_2"

kotlin {
    jvmToolchain(25)
    sourceSets.main {
        kotlin.srcDir("src/$versionOverlay/kotlin")
    }
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
            // Dev convenience only: -PquickPlay=<save folder under run/saves> joins that singleplayer world on launch.
            providers.gradleProperty("quickPlay").orNull?.let { save ->
                programArgument("--quickPlaySingleplayer")
                programArgument(save)
            }
        }
        // Used only to check that the client-only mod stays inert on a dedicated server.
        register("server") {
            server()
            programArgument("--nogui")
        }
    }
}

dependencies {
    implementation(project(":common"))
    // KotlinLangForge is a mod loader provider; it is not shaded and not a remapped mod dependency.
    implementation("dev.nyon:KotlinLangForge:2.15.0-k2.4.21-3.1+neoforge")
}

// Renders src/main/templates (neoforge.mods.toml) with the values of gradle.properties and gradle/mc/<mc>.properties.
val generateModMetadata =
    tasks.register<ProcessResources>("generateModMetadata") {
        val replaceProperties =
            mapOf(
                "mod_id" to modId,
                "mod_name" to modName,
                "mod_version" to modVersion,
                "mod_license" to modLicense,
                "mod_authors" to modAuthors,
                "neo_version" to mc.getProperty("neo_version"),
                "minecraft_version_range" to mc.getProperty("minecraft_version_range"),
            )
        inputs.properties(replaceProperties)
        expand(replaceProperties)
        from("src/main/templates")
        into(layout.buildDirectory.dir("generated/sources/modMetadata"))
    }
sourceSets.main
    .get()
    .resources
    .srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

// Ship common's classes inside the mod jar (no third-party libraries are shaded).
tasks.jar {
    from(commonMain.output)
}

// Asserts the BUILT jar's neoforge.mods.toml agrees with the build inputs, so a template or property mistake
// (wrong loader, drifted mod id, stale version range, unexpanded placeholder) fails the build instead of the game.
val verifyModMetadata =
    tasks.register("verifyModMetadata") {
        group = "verification"
        description = "Checks the jar's neoforge.mods.toml against gradle.properties, ModConstants.kt and gradle/mc."
        dependsOn(tasks.jar)

        val modJar = tasks.jar.flatMap { it.archiveFile }
        val modConstants =
            layout.projectDirectory
                .dir("src/main/kotlin/io/github/athegreat90/armorstatus")
                .file("ModConstants.kt")
        val minecraftRange = mc.getProperty("minecraft_version_range")
        val neoVersion = mc.getProperty("neo_version")
        inputs.files(modJar, modConstants)
        inputs.property("modId", modId)
        inputs.property("minecraftRange", minecraftRange)
        inputs.property("neoVersion", neoVersion)

        doLast {
            val jarFile = modJar.get().asFile
            val toml =
                JarFile(jarFile).use { jar ->
                    val entry =
                        jar.getJarEntry("META-INF/neoforge.mods.toml")
                            ?: throw GradleException("${jarFile.name} contains no META-INF/neoforge.mods.toml")
                    jar.getInputStream(entry).bufferedReader().readText()
                }
            val constantModId =
                Regex("""const val MOD_ID\s*=\s*"([^"]*)"""")
                    .find(modConstants.asFile.readText())
                    ?.groupValues
                    ?.get(1)

            fun String.tomlString(key: String): String? {
                val match = Regex("""(?m)^\s*$key\s*=\s*"([^"]*)"""").find(this)
                return match?.groupValues?.get(1)
            }

            // The text before the first table holds the top-level keys; every [[...]] table starts a new chunk.
            val tables = toml.split(Regex("""(?m)^(?=\[\[)"""))
            val header = tables.first()
            val jarModId = tables.firstOrNull { it.startsWith("[[mods]]") }?.tomlString("modId")
            val dependencies = tables.filter { it.startsWith("[[dependencies.") }
            val minecraftDep = dependencies.firstOrNull { it.tomlString("modId") == "minecraft" }
            val neoForgeDep = dependencies.firstOrNull { it.tomlString("modId") == "neoforge" }

            val problems = mutableListOf<String>()

            fun expect(
                what: String,
                actual: String?,
                expected: String?,
            ) {
                if (actual != expected) problems += "$what: expected '$expected' but found '$actual'"
            }

            expect("modLoader", header.tomlString("modLoader"), "klf")
            if (header.tomlString("license").isNullOrBlank()) problems += "license: missing or blank"
            expect("modId (jar) vs mod_id (gradle.properties)", jarModId, modId)
            expect("MOD_ID (ModConstants.kt) vs mod_id (gradle.properties)", constantModId, modId)
            expect("minecraft versionRange", minecraftDep?.tomlString("versionRange"), minecraftRange)
            val neoRange = neoForgeDep?.tomlString("versionRange")
            if (neoRange?.startsWith("[$neoVersion") != true) {
                problems += "neoforge versionRange: expected it to start with '[$neoVersion' but found '$neoRange'"
            }
            dependencies.forEach { dep ->
                expect("side of dependency '${dep.tomlString("modId")}'", dep.tomlString("side"), "CLIENT")
            }
            Regex("""\$\{[^}]*}""").findAll(toml).forEach { problems += "unexpanded placeholder: ${it.value}" }

            if (problems.isNotEmpty()) {
                val title = "${jarFile.name}: neoforge.mods.toml is invalid:"
                throw GradleException(problems.joinToString(separator = "\n  - ", prefix = "$title\n  - "))
            }
        }
    }
tasks.check { dependsOn(verifyModMetadata) }
