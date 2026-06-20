package io.github.lowkeylab.tomlformatter.internal

import arrow.core.raise.Raise
import com.dylibso.chicory.runtime.Memory
import io.github.lowkeylab.tomlformatter.TomlFormatterError

context(raise: Raise<TomlFormatterError>)
internal fun formatTomlWithWasm(source: String): String {
    val runtime = instantiateFormatter(loadFormatterWasm())
    val input = source.encodeToByteArray()
    val inputBuffer = runtime.allocate(input.size)

    return runtime.withDeallocated(inputBuffer) {
        runtime.writeBuffer(inputBuffer, input)
        runtime.invokeFormatter(inputBuffer).useResultBuffer(runtime) { resultBytes ->
            decodeFormatterResult(resultBytes)
        }
    }
}

context(raise: Raise<TomlFormatterError>)
private fun WasmFormatterRuntime.invokeFormatter(input: WasmBuffer): WasmBuffer {
    val packed = invokeSingle("format_toml", input.pointer.toLong(), input.length.toLong())
    return unpackResultBuffer(packed, memory)
}

context(raise: Raise<TomlFormatterError>)
private fun WasmBuffer.useResultBuffer(
    runtime: WasmFormatterRuntime,
    block: (ByteArray) -> String,
): String = runtime.withDeallocated(this) { block(runtime.readBuffer(this)) }

context(raise: Raise<TomlFormatterError>)
internal fun unpackResultBuffer(packed: Long, memory: Memory): WasmBuffer {
    val pointer = unpackU32(packed ushr 32, packed, "pointer")
    val length = unpackU32(packed and LOWER_U32_MASK, packed, "length")
    val buffer = WasmBuffer(pointer, length)
    ensurePackedRange(packed, memory, buffer)
    return buffer
}

context(raise: Raise<TomlFormatterError>)
private fun unpackU32(value: Long, packed: Long, field: String): Int {
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
private fun ensurePackedRange(packed: Long, memory: Memory, buffer: WasmBuffer) {
    val end = buffer.pointer.toLong() + buffer.length.toLong()
    val memorySize = memory.pages().toLong() * Memory.PAGE_SIZE.toLong()
    if (end > memorySize) {
        raise.raise(
            TomlFormatterError.InvalidPackedResult(
                packed,
                "range $buffer exceeds memory size $memorySize",
            )
        )
    }
}

private const val LOWER_U32_MASK = 0xFFFF_FFFFL
