package io.github.lowkeylab.tomlformatter.gradle

import arrow.core.raise.either
import io.github.lowkeylab.tomlformatter.TomlFormatterError
import io.github.lowkeylab.tomlformatter.formatToml
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger

internal interface TomlFileSystem {
    fun readText(file: File): String

    fun writeText(file: File, text: String)
}

internal interface TomlFormatLogger {
    fun formatted(displayPath: String)
}

internal object DefaultTomlFileSystem : TomlFileSystem {
    override fun readText(file: File): String = file.readText()

    override fun writeText(file: File, text: String): Unit = file.writeText(text)
}

internal class GradleTomlFormatLogger(private val logger: Logger) : TomlFormatLogger {
    override fun formatted(displayPath: String): Unit = logger.lifecycle("Formatted $displayPath")
}

context(fileSystem: TomlFileSystem, formatLogger: TomlFormatLogger)
internal fun formatTomlFiles(files: List<File>, projectDir: File): Int = files.count { file ->
    formatTomlFileIfChanged(file, projectDir)
}

context(fileSystem: TomlFileSystem, formatLogger: TomlFormatLogger)
internal fun formatTomlFileIfChanged(file: File, projectDir: File): Boolean {
    val original = fileSystem.readText(file)
    val formatted = formatTomlFileContents(original, file, projectDir)

    if (formatted == original) return false

    fileSystem.writeText(file, formatted)
    formatLogger.formatted(file.displayPath(projectDir))
    return true
}

context(fileSystem: TomlFileSystem)
internal fun unformattedTomlFiles(files: List<File>, projectDir: File): List<File> =
    files.filter { file ->
        val original = fileSystem.readText(file)
        val formatted = formatTomlFileContents(original, file, projectDir)
        formatted != original
    }

internal fun tomlFormatFailureMessage(unformattedFiles: List<File>, projectDir: File): String =
    buildString {
        appendLine("TOML formatting check failed for:")
        unformattedFiles.forEach { file -> appendLine(" - ${file.displayPath(projectDir)}") }
        appendLine()
        appendLine("Run `./gradlew formatToml` to fix.")
    }

internal fun formatTomlFileContents(source: String, file: File, projectDir: File): String =
    either<TomlFormatterError, String> { formatToml(source) }
        .fold(
            ifLeft = { error ->
                throw GradleException(
                    "Failed to format ${file.displayPath(projectDir)}: ${error.toDisplayMessage()}"
                )
            },
            ifRight = { formatted -> formatted },
        )

internal fun resolveSourceFiles(projectDir: File, sources: FileCollection): List<File> =
    sources.files
        .asSequence()
        .flatMap { source -> source.candidateFiles() }
        .distinctBy { file -> file.canonicalFile }
        .sortedBy { file -> file.displayPath(projectDir) }
        .toList()

internal fun File.displayPath(projectDir: File): String =
    relativeToOrSelf(projectDir).invariantSeparatorsPath

private fun File.candidateFiles(): Sequence<File> =
    when {
        isFile -> sequenceOf(this)
        isDirectory -> walkTopDown().filter { candidate -> candidate.isFile }
        else -> emptySequence()
    }

private fun TomlFormatterError.toDisplayMessage(): String =
    when (this) {
        TomlFormatterError.WasmResourceMissing -> "formatter WASM resource is missing"
        is TomlFormatterError.WasmResourceUnreadable ->
            "formatter WASM resource is unreadable: $message"
        is TomlFormatterError.ChicoryInstantiationFailure ->
            "failed to instantiate formatter runtime: $message"
        is TomlFormatterError.WasmInvocationFailure ->
            "formatter invocation failed during $operation: $message"
        is TomlFormatterError.WasmMemoryFailure ->
            "formatter memory operation failed during $operation: $message"
        is TomlFormatterError.InvalidPackedResult ->
            "formatter returned invalid result pointer $packed: $reason"
        is TomlFormatterError.ProtobufDecodeFailure ->
            "failed to decode formatter response: $message"
        TomlFormatterError.MissingProtobufResult -> "formatter response did not contain a result"
        is TomlFormatterError.FormatterCoreFailure -> message
    }
