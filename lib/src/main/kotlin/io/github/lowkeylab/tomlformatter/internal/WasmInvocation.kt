package io.github.lowkeylab.tomlformatter.internal

import arrow.core.raise.Raise
import io.github.lowkeylab.tomlformatter.TomlFormatterError

context(raise: Raise<TomlFormatterError>)
internal fun WasmFormatterRuntime.invokeSingle(operation: String, vararg args: Long): Long =
    selectExport(operation).invokeSingle(operation, *args)

context(raise: Raise<TomlFormatterError>)
internal fun WasmFormatterRuntime.selectExport(operation: String): WasmExport =
    when (operation) {
        "alloc" -> alloc
        "format_toml" -> formatToml
        else ->
            raise.raise(
                TomlFormatterError.WasmInvocationFailure(operation, "unknown exported function")
            )
    }

context(raise: Raise<TomlFormatterError>)
internal fun WasmExport.invokeSingle(operation: String, vararg args: Long): Long =
    singleResult(operation, invokeExport(operation, *args))

context(raise: Raise<TomlFormatterError>)
private fun WasmExport.invokeExport(operation: String, vararg args: Long): LongArray =
    runCatching { apply(*args) }
        .getOrElse { error ->
            raise.raise(TomlFormatterError.WasmInvocationFailure(operation, error.describe()))
        }

context(raise: Raise<TomlFormatterError>)
internal fun singleResult(operation: String, results: LongArray): Long =
    results.singleOrNull()
        ?: raise.raise(
            TomlFormatterError.WasmInvocationFailure(
                operation,
                "expected one result, got ${results.size}",
            )
        )
