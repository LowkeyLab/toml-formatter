package io.github.lowkeylab.tomlformatter.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files
import org.gradle.testfixtures.ProjectBuilder

class TomlFormatterOperationsTest :
    FunSpec({
        test("formats changed files through context-provided file system and logger") {
            val projectDir = Files.createTempDirectory("toml-formatter-operations-test").toFile()
            val fileSystem = testFileSystem()
            val logger = testFormatLogger()
            val changed = projectDir.resolve("changed.toml")
            val unchanged = projectDir.resolve("unchanged.toml")
            fileSystem.files[changed] = "key=\"value\""
            fileSystem.files[unchanged] = "key = \"value\"\n"

            val count =
                context(fileSystem, logger) {
                    formatTomlFiles(listOf(changed, unchanged), projectDir)
                }

            count shouldBe 1
            fileSystem.files[changed] shouldBe "key = \"value\"\n"
            fileSystem.files[unchanged] shouldBe "key = \"value\"\n"
            logger.formattedPaths shouldContainExactly listOf("changed.toml")
        }

        test("finds unformatted files without mutating them") {
            val projectDir = Files.createTempDirectory("toml-formatter-operations-test").toFile()
            val fileSystem = testFileSystem()
            val unformatted = projectDir.resolve("unformatted.toml")
            val formatted = projectDir.resolve("formatted.toml")
            fileSystem.files[unformatted] = "key=\"value\""
            fileSystem.files[formatted] = "key = \"value\"\n"

            val result =
                context(fileSystem) {
                    unformattedTomlFiles(listOf(unformatted, formatted), projectDir)
                }

            result shouldContainExactly listOf(unformatted)
            fileSystem.files[unformatted] shouldBe "key=\"value\""
            fileSystem.writes shouldBe emptyList()
        }

        test("builds check failure messages with display paths") {
            val projectDir = Files.createTempDirectory("toml-formatter-operations-test").toFile()
            val first = projectDir.resolve("a.toml")
            val second = projectDir.resolve("nested/b.toml")

            val message = tomlFormatFailureMessage(listOf(first, second), projectDir)

            message shouldContain "TOML formatting check failed for:"
            message shouldContain " - a.toml"
            message shouldContain " - nested/b.toml"
            message shouldContain "Run `./gradlew formatToml` to fix."
        }

        test("resolves source files with display-path sorting and canonical deduplication") {
            val projectDir = Files.createTempDirectory("toml-formatter-operations-test").toFile()
            val inputDir = projectDir.resolve("input")
            val nested = inputDir.resolve("nested")
            nested.mkdirs()
            val first = inputDir.resolve("a.toml").also { it.writeText("key=\"a\"") }
            val second = nested.resolve("b.toml").also { it.writeText("key=\"b\"") }
            val duplicate = File(inputDir, "nested/../a.toml")
            val sources = ProjectBuilder.builder().build().files(inputDir, duplicate)

            val result = resolveSourceFiles(projectDir, sources)

            result.map { it.canonicalFile } shouldContainExactly
                listOf(first.canonicalFile, second.canonicalFile)
            result.map { it.displayPath(projectDir) } shouldContainExactly
                listOf("input/a.toml", "input/nested/b.toml")
        }
    })

private class TestTomlFileSystem : TomlFileSystem {
    val files: MutableMap<File, String> = mutableMapOf()
    val writes: MutableList<Pair<File, String>> = mutableListOf()

    override fun readText(file: File): String = checkNotNull(files[file]) { "missing $file" }

    override fun writeText(file: File, text: String) {
        files[file] = text
        writes += file to text
    }
}

private class TestTomlFormatLogger : TomlFormatLogger {
    val formattedPaths: MutableList<String> = mutableListOf()

    override fun formatted(displayPath: String) {
        formattedPaths += displayPath
    }
}

private fun testFileSystem(): TestTomlFileSystem = TestTomlFileSystem()

private fun testFormatLogger(): TestTomlFormatLogger = TestTomlFormatLogger()
