package io.github.lowkeylab.tomlformatter.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class TomlFormatterPluginFunctionalTest :
    FunSpec({
        test("plugin applies and exposes formatter tasks") {
            context(testProject()) {
                writeBuildFile(
                    """
                    plugins {
                        id("io.github.lowkeylab.toml-formatter")
                    }
                    """
                        .trimIndent()
                )

                val result = runGradle("tasks", "--all")

                result.output shouldContain "formatToml"
                result.output shouldContain "checkTomlFormat"
            }
        }

        test("zero configuration leaves TOML files unselected") {
            context(testProject()) {
                writeBuildFile(tomlFormatterBuildFile())
                val sample = writeProjectFile("sample.toml", "key=\"value\"")

                val result = runGradle("formatToml")

                result.task(":formatToml")?.outcome shouldBe TaskOutcome.SUCCESS
                sample.readText() shouldBe "key=\"value\""
            }
        }

        test("formatToml formats an explicitly configured file in place") {
            context(testProject()) {
                writeBuildFile(tomlFormatterBuildFile("inputs.from(\"sample.toml\")"))
                val sample = writeProjectFile("sample.toml", "key=\"value\"")

                val result = runGradle("formatToml")

                result.task(":formatToml")?.outcome shouldBe TaskOutcome.SUCCESS
                sample.readText() shouldBe "key = \"value\"\n"
            }
        }

        test("checkTomlFormat fails on an unformatted file without mutating it") {
            context(testProject()) {
                writeBuildFile(tomlFormatterBuildFile("inputs.from(\"sample.toml\")"))
                val original = "key=\"value\""
                val sample = writeProjectFile("sample.toml", original)

                val result = runGradleAndFail("checkTomlFormat")

                result.task(":checkTomlFormat")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "sample.toml"
                sample.readText() shouldBe original
            }
        }

        test("checkTomlFormat succeeds after formatToml runs") {
            context(testProject()) {
                writeBuildFile(tomlFormatterBuildFile("inputs.from(\"sample.toml\")"))
                writeProjectFile("sample.toml", "key=\"value\"")

                runGradle("formatToml")
                val result = runGradle("checkTomlFormat")

                result.task(":checkTomlFormat")?.outcome shouldBe TaskOutcome.SUCCESS
            }
        }

        test("fileTree inputs control custom includes and excludes") {
            context(testProject()) {
                writeBuildFile(
                    tomlFormatterBuildFile(
                        """
                        inputs.from(fileTree("config") {
                            include("**/*.toml")
                            exclude("**/ignored/**")
                        })
                        """
                            .trimIndent()
                    )
                )
                val target = writeProjectFile("config/target.toml", "key=\"value\"")
                val ignored = writeProjectFile("config/ignored/skipped.toml", "key=\"value\"")
                val outside = writeProjectFile("outside.toml", "key=\"value\"")

                runGradle("formatToml")

                target.readText() shouldBe "key = \"value\"\n"
                ignored.readText() shouldBe "key=\"value\""
                outside.readText() shouldBe "key=\"value\""
            }
        }

        test("wildcard strings are delegated to Gradle file collection semantics") {
            context(testProject()) {
                writeBuildFile(tomlFormatterBuildFile("inputs.from(\"config/**/*.toml\")"))
                val target = writeProjectFile("config/nested/target.toml", "key=\"value\"")

                val result = runGradle("formatToml")

                result.task(":formatToml")?.outcome shouldBe TaskOutcome.SUCCESS
                target.readText() shouldBe "key=\"value\""
            }
        }

        test("explicit directory inputs are expanded as configured") {
            context(testProject()) {
                writeBuildFile(tomlFormatterBuildFile("inputs.from(\"config\")"))
                val sample = writeProjectFile("config/nested/sample.toml", "key=\"value\"")
                val metadata = writeProjectFile("config/metadata.txt", "key=\"value\"")
                val outside = writeProjectFile("outside.toml", "key=\"value\"")

                runGradle("formatToml")

                sample.readText() shouldBe "key = \"value\"\n"
                metadata.readText() shouldBe "key = \"value\"\n"
                outside.readText() shouldBe "key=\"value\""
            }
        }

        test("base check lifecycle invokes checkTomlFormat") {
            context(testProject()) {
                writeBuildFile(
                    tomlFormatterBuildFile(
                        configuration = "inputs.from(\"sample.toml\")",
                        extraPlugins = "base",
                    )
                )
                writeProjectFile("sample.toml", "key=\"value\"")

                val result = runGradleAndFail("check")

                result.task(":checkTomlFormat")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "sample.toml"
            }
        }
    })

internal class TestGradleProject(val dir: Path)

internal fun testProject(): TestGradleProject {
    val projectDir = Files.createTempDirectory("toml-formatter-gradle-plugin-test")
    projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"\n")
    return TestGradleProject(projectDir)
}

context(project: TestGradleProject)
internal fun writeBuildFile(content: String) {
    project.dir.resolve("build.gradle.kts").writeText("$content\n")
}

context(project: TestGradleProject)
internal fun writeProjectFile(path: String, content: String): Path =
    project.dir.resolve(path).also { file ->
        file.parent?.toFile()?.mkdirs()
        file.writeText(content)
    }

context(project: TestGradleProject)
internal fun runGradle(vararg arguments: String): BuildResult =
    gradleRunner(project.dir, *arguments).build()

context(project: TestGradleProject)
internal fun runGradleAndFail(vararg arguments: String): BuildResult =
    gradleRunner(project.dir, *arguments).buildAndFail()

internal fun tomlFormatterBuildFile(configuration: String = "", extraPlugins: String = ""): String =
    buildString {
            appendLine("plugins {")
            if (extraPlugins.isNotBlank()) appendLine("    $extraPlugins")
            appendLine("    id(\"io.github.lowkeylab.toml-formatter\")")
            appendLine("}")
            if (configuration.isNotBlank()) {
                appendLine()
                appendLine("tomlFormatter {")
                configuration.lineSequence().forEach { line -> appendLine("    $line") }
                appendLine("}")
            }
        }
        .trimEnd()

private fun Path.writeText(content: String) {
    toFile().writeText(content)
}

private fun Path.readText(): String = toFile().readText()

private fun gradleRunner(projectDir: Path, vararg arguments: String): GradleRunner =
    GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withArguments(arguments.toList() + "--stacktrace")
        .withPluginClasspath()
