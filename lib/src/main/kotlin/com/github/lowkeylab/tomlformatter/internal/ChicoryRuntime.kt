package com.github.lowkeylab.tomlformatter.internal

import arrow.core.raise.Raise
import com.dylibso.chicory.runtime.ExportFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.wasm.Parser
import com.github.lowkeylab.tomlformatter.TomlFormatterError

internal data class WasmFormatterRuntime(
    val memory: Memory,
    val alloc: ExportFunction,
    val dealloc: ExportFunction,
    val formatToml: ExportFunction,
)

context(raise: Raise<TomlFormatterError>)
internal fun instantiateFormatter(wasmBytes: ByteArray): WasmFormatterRuntime {
    val instance = instantiateModule(wasmBytes)

    return WasmFormatterRuntime(
        memory = instance.memory(),
        alloc = exportFunction(instance, "alloc"),
        dealloc = exportFunction(instance, "dealloc"),
        formatToml = exportFunction(instance, "format_toml"),
    )
}

context(raise: Raise<TomlFormatterError>)
private fun instantiateModule(wasmBytes: ByteArray): Instance = try {
    Instance.builder(Parser.parse(wasmBytes)).build()
} catch (error: Throwable) {
    raise.raise(TomlFormatterError.ChicoryInstantiationFailure(error.describe()))
}

context(raise: Raise<TomlFormatterError>)
private fun exportFunction(instance: Instance, name: String): ExportFunction = try {
    instance.export(name)
} catch (error: Throwable) {
    raise.raise(TomlFormatterError.WasmInvocationFailure("lookup export '$name'", error.describe()))
}
