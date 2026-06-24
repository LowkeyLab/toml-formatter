package io.github.lowkeylab.tomlformatter.internal

import arrow.core.raise.Raise
import io.github.lowkeylab.tomlformatter.TomlFormatterError

context(raise: Raise<TomlFormatterError>)
internal fun formatTomlWithWasm(source: String): String =
    formatTomlWithRuntime(instantiateFormatter(loadFormatterWasm()), encodeSource(source))

internal fun encodeSource(source: String): ByteArray = source.encodeToByteArray()

context(raise: Raise<TomlFormatterError>)
internal fun formatTomlWithRuntime(runtime: WasmFormatterRuntime, input: ByteArray): String =
    runtime.withInputBuffer(input) { inputBuffer ->
        runtime.invokeFormatter(inputBuffer).useResultBuffer(runtime) { resultBytes ->
            decodeFormatterResult(resultBytes)
        }
    }

context(raise: Raise<TomlFormatterError>)
private fun WasmFormatterRuntime.withInputBuffer(
    input: ByteArray,
    block: (WasmBuffer) -> String,
): String {
    val inputBuffer = allocate(input.size)
    return withDeallocated(inputBuffer) {
        writeBuffer(inputBuffer, input)
        block(inputBuffer)
    }
}

context(raise: Raise<TomlFormatterError>)
internal fun WasmFormatterRuntime.invokeFormatter(input: WasmBuffer): WasmBuffer =
    unpackResultBuffer(
        invokeSingle("format_toml", input.pointer.toLong(), input.length.toLong()),
        memory,
    )

context(raise: Raise<TomlFormatterError>)
internal fun WasmBuffer.useResultBuffer(
    runtime: WasmFormatterRuntime,
    block: (ByteArray) -> String,
): String = runtime.withDeallocated(this) { block(runtime.readBuffer(this)) }

context(raise: Raise<TomlFormatterError>)
internal fun unpackResultBuffer(packed: Long, memory: WasmLinearMemory): WasmBuffer =
    packed.toWasmBuffer().also { ensurePackedRange(packed, memory, it) }

context(raise: Raise<TomlFormatterError>)
internal fun Long.toWasmBuffer(): WasmBuffer =
    WasmBuffer(
        unpackU32(value = this ushr 32, packed = this, field = "pointer"),
        unpackU32(value = this and LOWER_U32_MASK, packed = this, field = "length"),
    )

context(raise: Raise<TomlFormatterError>)
internal fun unpackU32(value: Long, packed: Long, field: String): Int {
    if (value > Int.MAX_VALUE) {
        raise.raise(
            TomlFormatterError.InvalidPackedResult(
                packed,
                "$field does not fit Chicory int address/length: $value",
            )
        )
    }
    return value.toInt()
}

context(raise: Raise<TomlFormatterError>)
internal fun ensurePackedRange(
    packed: Long,
    memory: WasmLinearMemory,
    buffer: WasmBuffer,
): WasmBuffer {
    val end = buffer.pointer.toLong() + buffer.length.toLong()
    if (end > memory.byteSize) {
        raise.raise(
            TomlFormatterError.InvalidPackedResult(
                packed,
                "range $buffer exceeds memory size ${memory.byteSize}",
            )
        )
    }
    return buffer
}

private const val LOWER_U32_MASK = 0xFFFF_FFFFL
