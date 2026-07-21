package io.github.lowkeylab.tomlformatter.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import java.util.zip.ZipFile

private const val SHADED_PATH = "io/github/lowkeylab/tomlformatter/gradle/internal/shaded"

class ShadedPluginArtifactTest :
    FunSpec({
        test("shades all runtime dependencies while preserving plugin API and Kotlin") {
            val archiveEntries = zipEntries(requiredFileProperty("tomlFormatter.shadedJar"))
            val allowedClassPrefixes = setOf("io/github/lowkeylab/tomlformatter/gradle/", "kotlin/")
            val unexpectedClasses = archiveEntries.filter { entry ->
                entry.endsWith(".class") &&
                    allowedClassPrefixes.none(entry::startsWith) &&
                    !entry.endsWith("module-info.class")
            }
            val requiredEntries =
                setOf(
                    "META-INF/gradle-plugins/io.github.lowkeylab.toml-formatter.properties",
                    "META-INF/LICENSE.txt",
                    "META-INF/THIRD-PARTY-NOTICES.txt",
                    "META-INF/licenses/Apache-2.0.txt",
                    "META-INF/licenses/BSD-3-Clause.txt",
                    "META-INF/licenses/MIT.txt",
                    "META-INF/licenses/Unicode-3.0.txt",
                    "io/github/lowkeylab/tomlformatter/gradle/TomlFormatterPlugin.class",
                    "$SHADED_PATH/io/github/lowkeylab/tomlformatter/TomlFormatterError.class",
                    "$SHADED_PATH/arrow/core/Either.class",
                    "$SHADED_PATH/com/dylibso/chicory/runtime/Instance.class",
                    "$SHADED_PATH/com/google/protobuf/Message.class",
                    "$SHADED_PATH/org/intellij/lang/annotations/Flow.class",
                    "$SHADED_PATH/org/jetbrains/annotations/NotNull.class",
                    "$SHADED_PATH/taplo_wasm/FormatToml.class",
                    "kotlin/Unit.class",
                    "wasm/taplo_wasm.wasm",
                )

            unexpectedClasses.shouldBeEmpty()
            (requiredEntries - archiveEntries).shouldBeEmpty()
        }

        test("publishes a dependency-free shaded artifact with AGPLv3 metadata") {
            val pluginPom = requiredFileProperty("tomlFormatter.pluginPom").readText()
            val pluginModuleMetadata =
                requiredFileProperty("tomlFormatter.pluginModuleMetadata").readText()
            val markerPom = requiredFileProperty("tomlFormatter.markerPom").readText()
            val pluginVersion = requiredProperty("tomlFormatter.pluginVersion")

            listOf(pluginPom, markerPom).forEach { pom ->
                pom shouldContain "GNU Affero General Public License, Version 3"
                pom shouldContain "https://www.gnu.org/licenses/agpl-3.0.txt"
                pom shouldContain "<distribution>repo</distribution>"
            }
            pluginPom shouldNotContain "<dependencies>"
            pluginModuleMetadata shouldNotContain "\"dependencies\""
            pluginModuleMetadata shouldContain "\"name\": \"shadowRuntimeElements\""
            pluginModuleMetadata shouldContain "\"org.gradle.dependency.bundling\": \"shadowed\""
            pluginModuleMetadata shouldContain "\"name\": \"gradle-plugin-$pluginVersion.jar\""
            Regex("<dependency>").findAll(markerPom).count() shouldBe 1
            markerPom shouldContain "<artifactId>gradle-plugin</artifactId>"
        }
    })

private fun requiredProperty(name: String): String =
    checkNotNull(System.getProperty(name)) { "Missing system property $name" }

private fun requiredFileProperty(name: String): File = File(requiredProperty(name))

private fun zipEntries(file: File): Set<String> =
    ZipFile(file).use { zip -> zip.entries().asSequence().map { it.name }.toSet() }
