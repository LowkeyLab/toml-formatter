package com.github.lowkeylab.tomlformatter.gradle

import arrow.core.raise.either
import com.github.lowkeylab.tomlformatter.TomlFormatterError
import com.github.lowkeylab.tomlformatter.formatToml
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection

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

internal fun resolveTomlFiles(
    projectDir: File,
    sources: FileCollection,
    includes: List<String>,
    excludes: List<String>,
): List<File> =
    sources.files
        .asSequence()
        .flatMap { source -> source.candidateFiles() }
        .filter { file -> file.isIncluded(projectDir, includes, excludes) }
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

private fun File.isIncluded(
    projectDir: File,
    includes: List<String>,
    excludes: List<String>,
): Boolean {
    val path = displayPath(projectDir)
    val included = includes.isEmpty() || includes.any { pattern -> pattern.matchesGlob(path) }
    val excluded = excludes.any { pattern -> pattern.matchesGlob(path) }
    return included && !excluded
}

private const val DOUBLE_STAR_SLASH_LENGTH = 3

private fun String.matchesGlob(path: String): Boolean = globToRegex().matches(path)

private fun String.globToRegex(): Regex {
    val pattern = replace('\\', '/')
    val regex = StringBuilder("^")
    var index = 0

    while (index < pattern.length) {
        val char = pattern[index]
        when {
            char == '*' &&
                pattern.getOrNull(index + 1) == '*' &&
                pattern.getOrNull(index + 2) == '/' -> {
                regex.append("(?:.*/)?")
                index += DOUBLE_STAR_SLASH_LENGTH
            }
            char == '*' && pattern.getOrNull(index + 1) == '*' -> {
                regex.append(".*")
                index += 2
            }
            char == '*' -> {
                regex.append("[^/]*")
                index += 1
            }
            char == '?' -> {
                regex.append("[^/]")
                index += 1
            }
            else -> {
                regex.append(Regex.escape(char.toString()))
                index += 1
            }
        }
    }

    return Regex(regex.append('$').toString())
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
