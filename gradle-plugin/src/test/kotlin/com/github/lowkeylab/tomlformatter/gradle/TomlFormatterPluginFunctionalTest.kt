package com.github.lowkeylab.tomlformatter.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class TomlFormatterPluginFunctionalTest :
    FunSpec({
        test("plugin applies and exposes formatter tasks") {
            val projectDir = testProject()
            projectDir.writeBuildFile(
                """
                plugins {
                    id("com.github.lowkeylab.toml-formatter")
                }
                """
                    .trimIndent()
            )

            val result = gradleRunner(projectDir, "tasks", "--all").build()

            result.output shouldContain "formatToml"
            result.output shouldContain "checkTomlFormat"
        }

        test("formatToml formats an explicitly configured file in place") {
            val projectDir = testProject()
            projectDir.writeBuildFile(
                """
                plugins {
                    id("com.github.lowkeylab.toml-formatter")
                }

                tomlFormatter {
                    inputs.from("sample.toml")
                }
                """
                    .trimIndent()
            )
            val sample = projectDir.resolve("sample.toml")
            sample.toFile().writeText("key=\"value\"")

            val result = gradleRunner(projectDir, "formatToml").build()

            result.task(":formatToml")?.outcome shouldBe TaskOutcome.SUCCESS
            sample.toFile().readText() shouldBe "key = \"value\"\n"
        }

        test("checkTomlFormat fails on an unformatted file without mutating it") {
            val projectDir = testProject()
            projectDir.writeBuildFile(
                """
                plugins {
                    id("com.github.lowkeylab.toml-formatter")
                }

                tomlFormatter {
                    inputs.from("sample.toml")
                }
                """
                    .trimIndent()
            )
            val sample = projectDir.resolve("sample.toml")
            val original = "key=\"value\""
            sample.toFile().writeText(original)

            val result = gradleRunner(projectDir, "checkTomlFormat").buildAndFail()

            result.task(":checkTomlFormat")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "sample.toml"
            sample.toFile().readText() shouldBe original
        }

        test("checkTomlFormat succeeds after formatToml runs") {
            val projectDir = testProject()
            projectDir.writeBuildFile(
                """
                plugins {
                    id("com.github.lowkeylab.toml-formatter")
                }

                tomlFormatter {
                    inputs.from("sample.toml")
                }
                """
                    .trimIndent()
            )
            projectDir.resolve("sample.toml").toFile().writeText("key=\"value\"")

            gradleRunner(projectDir, "formatToml").build()
            val result = gradleRunner(projectDir, "checkTomlFormat").build()

            result.task(":checkTomlFormat")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("extension configuration restricts glob inputs and excludes") {
            val projectDir = testProject()
            projectDir.writeBuildFile(
                """
                plugins {
                    id("com.github.lowkeylab.toml-formatter")
                }

                tomlFormatter {
                    inputs.from("config/**/*.toml")
                    excludes.set(listOf("**/ignored/**"))
                }
                """
                    .trimIndent()
            )
            val target = projectDir.resolve("config/target.toml")
            val ignored = projectDir.resolve("config/ignored/skipped.toml")
            val outside = projectDir.resolve("outside.toml")
            target.parent.toFile().mkdirs()
            ignored.parent.toFile().mkdirs()
            target.toFile().writeText("key=\"value\"")
            ignored.toFile().writeText("key=\"value\"")
            outside.toFile().writeText("key=\"value\"")

            gradleRunner(projectDir, "formatToml").build()

            target.toFile().readText() shouldBe "key = \"value\"\n"
            ignored.toFile().readText() shouldBe "key=\"value\""
            outside.toFile().readText() shouldBe "key=\"value\""
        }

        test("base check lifecycle invokes checkTomlFormat") {
            val projectDir = testProject()
            projectDir.writeBuildFile(
                """
                plugins {
                    base
                    id("com.github.lowkeylab.toml-formatter")
                }

                tomlFormatter {
                    inputs.from("sample.toml")
                }
                """
                    .trimIndent()
            )
            projectDir.resolve("sample.toml").toFile().writeText("key=\"value\"")

            val result = gradleRunner(projectDir, "check").buildAndFail()

            result.task(":checkTomlFormat")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "sample.toml"
        }
    })

private fun testProject(): Path {
    val projectDir = Files.createTempDirectory("toml-formatter-gradle-plugin-test")
    projectDir
        .resolve("settings.gradle.kts")
        .toFile()
        .writeText("rootProject.name = \"test-project\"\n")
    return projectDir
}

private fun Path.writeBuildFile(content: String) {
    resolve("build.gradle.kts").toFile().writeText("$content\n")
}

private fun gradleRunner(projectDir: Path, vararg arguments: String): GradleRunner =
    GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withArguments(arguments.toList() + "--stacktrace")
        .withPluginClasspath()
