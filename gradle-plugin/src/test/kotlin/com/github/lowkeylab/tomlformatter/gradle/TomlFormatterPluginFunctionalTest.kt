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

        test("zero configuration formats TOML files only") {
            val projectDir = testProject()
            projectDir.writeBuildFile(
                """
                plugins {
                    id("com.github.lowkeylab.toml-formatter")
                }
                """
                    .trimIndent()
            )
            val sample = projectDir.resolve("sample.toml")
            val notes = projectDir.resolve("notes.txt")
            sample.toFile().writeText("key=\"value\"")
            notes.toFile().writeText("key=\"value\"")

            val result = gradleRunner(projectDir, "formatToml").build()

            result.task(":formatToml")?.outcome shouldBe TaskOutcome.SUCCESS
            sample.toFile().readText() shouldBe "key = \"value\"\n"
            notes.toFile().readText() shouldBe "key=\"value\""
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

        test("fileTree inputs control custom includes and excludes") {
            val projectDir = testProject()
            projectDir.writeBuildFile(
                """
                plugins {
                    id("com.github.lowkeylab.toml-formatter")
                }

                tomlFormatter {
                    inputs.from(fileTree("config") {
                        include("**/*.toml")
                        exclude("**/ignored/**")
                    })
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

        test("zero configuration excludes generated and build directories") {
            val projectDir = testProject()
            projectDir.writeBuildFile(
                """
                plugins {
                    id("com.github.lowkeylab.toml-formatter")
                }
                """
                    .trimIndent()
            )
            val normal = projectDir.resolve("normal.toml")
            val buildOutput = projectDir.resolve("build/generated.toml")
            val gradleCache = projectDir.resolve(".gradle/cache.toml")
            val kotlinSession = projectDir.resolve(".kotlin/session.toml")
            val wasmTarget = projectDir.resolve("wasm/target/generated.toml")
            listOf(buildOutput, gradleCache, kotlinSession, wasmTarget).forEach { file ->
                file.parent.toFile().mkdirs()
                file.toFile().writeText("key=\"value\"")
            }
            normal.toFile().writeText("key=\"value\"")

            gradleRunner(projectDir, "formatToml").build()

            normal.toFile().readText() shouldBe "key = \"value\"\n"
            buildOutput.toFile().readText() shouldBe "key=\"value\""
            gradleCache.toFile().readText() shouldBe "key=\"value\""
            kotlinSession.toFile().readText() shouldBe "key=\"value\""
            wasmTarget.toFile().readText() shouldBe "key=\"value\""
        }

        test("explicit directory inputs are expanded as configured") {
            val projectDir = testProject()
            projectDir.writeBuildFile(
                """
                plugins {
                    id("com.github.lowkeylab.toml-formatter")
                }

                tomlFormatter {
                    inputs.from("config")
                }
                """
                    .trimIndent()
            )
            val sample = projectDir.resolve("config/nested/sample.toml")
            val metadata = projectDir.resolve("config/metadata.txt")
            val outside = projectDir.resolve("outside.toml")
            sample.parent.toFile().mkdirs()
            sample.toFile().writeText("key=\"value\"")
            metadata.toFile().writeText("key=\"value\"")
            outside.toFile().writeText("key=\"value\"")

            gradleRunner(projectDir, "formatToml").build()

            sample.toFile().readText() shouldBe "key = \"value\"\n"
            metadata.toFile().readText() shouldBe "key = \"value\"\n"
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
