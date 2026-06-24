package io.github.lowkeylab.tomlformatter.internal

import arrow.core.raise.Raise
import io.github.lowkeylab.tomlformatter.TomlFormatterError

context(raise: Raise<TomlFormatterError>)
internal fun validateAllocationLength(length: Int): Int {
    if (length < 0) {
        raise.raise(
            TomlFormatterError.WasmMemoryFailure("allocate", "length must be non-negative: $length")
        )
    }
    return length
}

context(raise: Raise<TomlFormatterError>)
internal fun validateWriteSize(buffer: WasmBuffer, bytes: ByteArray): ByteArray {
    if (bytes.size != buffer.length) {
        raise.raise(
            TomlFormatterError.WasmMemoryFailure(
                "write",
                "byte count ${bytes.size} did not match buffer length ${buffer.length}",
            )
        )
    }
    return bytes
}

context(raise: Raise<TomlFormatterError>)
internal fun Long.toIntPointer(operation: String): Int {
    if (this < 0 || this > Int.MAX_VALUE) {
        raise.raise(
            TomlFormatterError.WasmMemoryFailure(
                operation,
                "pointer does not fit Chicory int address: $this",
            )
        )
    }
    return toInt()
}

context(raise: Raise<TomlFormatterError>)
internal fun ensureRange(
    memory: WasmLinearMemory,
    buffer: WasmBuffer,
    operation: String,
): WasmBuffer {
    if (buffer.pointer < 0 || buffer.length < 0) {
        raise.raise(
            TomlFormatterError.WasmMemoryFailure(operation, "negative pointer or length: $buffer")
        )
    }

    val end = buffer.pointer.toLong() + buffer.length.toLong()
    if (end > memory.byteSize) {
        raise.raise(
            TomlFormatterError.WasmMemoryFailure(
                operation,
                "range $buffer exceeds memory size ${memory.byteSize}",
            )
        )
    }
    return buffer
}
