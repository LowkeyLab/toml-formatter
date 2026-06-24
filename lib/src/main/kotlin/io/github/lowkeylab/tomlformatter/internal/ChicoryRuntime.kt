package io.github.lowkeylab.tomlformatter.internal

import arrow.core.raise.Raise
import com.dylibso.chicory.runtime.ExportFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.wasm.Parser
import io.github.lowkeylab.tomlformatter.TomlFormatterError

internal data class WasmFormatterRuntime(
    val memory: WasmLinearMemory,
    val alloc: WasmExport,
    val dealloc: WasmExport,
    val formatToml: WasmExport,
)

internal interface WasmLinearMemory {
    val byteSize: Long

    fun write(pointer: Int, bytes: ByteArray)

    fun readBytes(pointer: Int, length: Int): ByteArray
}

internal fun interface WasmExport {
    fun apply(vararg args: Long): LongArray
}

private class ChicoryLinearMemory(private val memory: Memory) : WasmLinearMemory {
    override val byteSize: Long
        get() = memory.pages().toLong() * Memory.PAGE_SIZE.toLong()

    override fun write(pointer: Int, bytes: ByteArray): Unit = memory.write(pointer, bytes)

    override fun readBytes(pointer: Int, length: Int): ByteArray = memory.readBytes(pointer, length)
}

private class ChicoryExport(private val function: ExportFunction) : WasmExport {
    override fun apply(vararg args: Long): LongArray = function.apply(*args) ?: longArrayOf()
}

context(raise: Raise<TomlFormatterError>)
internal fun instantiateFormatter(wasmBytes: ByteArray): WasmFormatterRuntime =
    instantiateModule(wasmBytes).toFormatterRuntime()

context(raise: Raise<TomlFormatterError>)
private fun Instance.toFormatterRuntime(): WasmFormatterRuntime =
    WasmFormatterRuntime(
        memory = ChicoryLinearMemory(memory()),
        alloc = exportFunction("alloc"),
        dealloc = exportFunction("dealloc"),
        formatToml = exportFunction("format_toml"),
    )

context(raise: Raise<TomlFormatterError>)
private fun instantiateModule(wasmBytes: ByteArray): Instance =
    try {
        Instance.builder(Parser.parse(wasmBytes)).build()
    } catch (error: Throwable) {
        raise.raise(TomlFormatterError.ChicoryInstantiationFailure(error.describe()))
    }

context(raise: Raise<TomlFormatterError>)
private fun Instance.exportFunction(name: String): WasmExport =
    try {
        ChicoryExport(export(name))
    } catch (error: Throwable) {
        raise.raise(
            TomlFormatterError.WasmInvocationFailure("lookup export '$name'", error.describe())
        )
    }
