plugins {
    kotlin("jvm")
}

base.archivesName = "armor-status-common"

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation(kotlin("test"))
    // Aggregator: junit-jupiter-api, junit-jupiter-params and the engine, all at the BOM version.
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Fails the build when tests silently did not run: no results at all, a skipped test, or a *Test.kt file whose
// class produced no results (for example no discovered @Test). Failures and errors are also rejected as a second line
// of defence behind the test task itself. Files are only read in the action, never at configuration time.
val verifyTestResults =
    tasks.register("verifyTestResults") {
        group = "verification"
        description = "Fails if tests were not executed, were skipped, or a test class produced no results."
        mustRunAfter(tasks.test)

        val resultsDir = layout.buildDirectory.dir("test-results/test")
        val testSourceDir = layout.projectDirectory.dir("src/test/kotlin")
        inputs.files(fileTree(resultsDir) { include("*.xml") })
        inputs.dir(testSourceDir)

        // A filtered run (`--tests` on the command line or `filter.includeTestsMatching`) legitimately leaves other
        // classes without results. `--tests` is public only through the task arguments of the start parameters.
        val cliFiltered =
            gradle.startParameter.taskRequests.any { request ->
                request.args.any { it == "--tests" || it.startsWith("--tests=") }
            }
        val testsFiltered = tasks.test.map { cliFiltered || it.filter.includePatterns.isNotEmpty() }

        doLast {
            val problems = mutableListOf<String>()
            val sourceRoot = testSourceDir.asFile
            val xmlFiles =
                resultsDir
                    .get()
                    .asFile
                    .listFiles { file -> file.name.startsWith("TEST-") && file.extension == "xml" }
                    .orEmpty()
                    .sortedBy { it.name }

            // The attributes of the root <testsuite> element: tests, skipped, failures, errors.
            fun counts(xml: File): Map<String, Int> {
                val head = Regex("""<testsuite\b[^>]*>""").find(xml.readText())?.value.orEmpty()
                return listOf("tests", "skipped", "failures", "errors").associateWith { name ->
                    Regex("""\b$name="(\d+)"""")
                        .find(head)
                        ?.groupValues
                        ?.get(1)
                        ?.toInt() ?: 0
                }
            }

            val byFile = xmlFiles.associate { it.name to counts(it) }

            fun total(name: String) = byFile.values.sumOf { it.getValue(name) }

            // Every *Test.kt must have a TEST-<package>.<Name>.xml with at least one executed test, unless the run
            // was filtered to some tests on purpose.
            if (testsFiltered.get()) {
                logger.lifecycle("verifyTestResults: test filter active, per-class completeness check skipped")
            } else {
                sourceRoot
                    .walkTopDown()
                    .filter { it.isFile && it.name.endsWith("Test.kt") }
                    .map {
                        it
                            .relativeTo(sourceRoot)
                            .path
                            .removeSuffix(".kt")
                            .replace(File.separatorChar, '.')
                    }.sorted()
                    .forEach { className ->
                        val tests = byFile["TEST-$className.xml"]?.getValue("tests")
                        if (tests == null) {
                            problems += "test class $className has no result file TEST-$className.xml"
                        } else if (tests == 0) {
                            problems += "test class $className ran 0 tests"
                        }
                    }
            }

            if (total("tests") == 0) problems += "no tests were executed"
            if (total("skipped") > 0) problems += "${total("skipped")} test(s) were skipped"
            if (total("failures") + total("errors") > 0) {
                problems += "${total("failures")} failure(s) and ${total("errors")} error(s) were reported"
            }

            if (problems.isNotEmpty()) {
                val title = "Test results are not trustworthy (${xmlFiles.size} result files):"
                throw GradleException(problems.joinToString(separator = "\n  - ", prefix = "$title\n  - "))
            }
            logger.lifecycle(
                "verifyTestResults: ${total("tests")} tests in ${xmlFiles.size} suites, 0 skipped, 0 failed",
            )
        }
    }

tasks.test {
    useJUnitPlatform()
    // Gradle 9 already fails when no test is discovered; stated here so the rule stays visible.
    failOnNoDiscoveredTests = true
    testLogging {
        events("passed", "skipped", "failed")
    }
    reports {
        junitXml.required = true
        html.required = true
    }
    finalizedBy(verifyTestResults)
}

tasks.check { dependsOn(verifyTestResults) }
