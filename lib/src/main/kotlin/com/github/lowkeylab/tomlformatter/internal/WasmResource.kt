package com.github.lowkeylab.tomlformatter.internal

import arrow.core.raise.Raise
import com.github.lowkeylab.tomlformatter.TomlFormatterError

private const val WASM_RESOURCE_PATH = "/wasm/taplo_wasm.wasm"

context(raise: Raise<TomlFormatterError>)
internal fun loadFormatterWasm(): ByteArray {
    val stream =
        object {}.javaClass.getResourceAsStream(WASM_RESOURCE_PATH)
            ?: raise.raise(TomlFormatterError.WasmResourceMissing)

    return try {
        stream.use { it.readBytes() }
    } catch (error: Throwable) {
        raise.raise(TomlFormatterError.WasmResourceUnreadable(error.describe()))
    }
}

internal fun Throwable.describe(): String =
    message ?: this::class.qualifiedName ?: this::class.simpleName ?: "unknown error"
