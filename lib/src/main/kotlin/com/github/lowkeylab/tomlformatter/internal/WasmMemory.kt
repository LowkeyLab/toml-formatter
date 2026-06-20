package com.github.lowkeylab.tomlformatter.internal

import arrow.core.raise.Raise
import com.dylibso.chicory.runtime.Memory
import com.github.lowkeylab.tomlformatter.TomlFormatterError

internal data class WasmBuffer(val pointer: Int, val length: Int)

context(raise: Raise<TomlFormatterError>)
internal fun WasmFormatterRuntime.allocate(length: Int): WasmBuffer {
    if (length < 0) {
        raise.raise(
            TomlFormatterError.WasmMemoryFailure("allocate", "length must be non-negative: $length")
        )
    }

    val pointer = invokeSingle("alloc", length.toLong()).toIntPointer("alloc")
    return WasmBuffer(pointer, length)
}

context(raise: Raise<TomlFormatterError>)
internal fun <T> WasmFormatterRuntime.withDeallocated(buffer: WasmBuffer, block: () -> T): T {
    var primaryFailure: Throwable? = null

    try {
        return block()
    } catch (error: Throwable) {
        primaryFailure = error
        throw error
    } finally {
        deallocateFailure(buffer)?.let { cleanupFailure ->
            val primary = primaryFailure
            if (primary == null) {
                raise.raise(cleanupFailure)
            } else {
                primary.addSuppressed(WasmCleanupFailure(cleanupFailure))
            }
        }
    }
}

internal fun WasmFormatterRuntime.deallocateFailure(
    buffer: WasmBuffer
): TomlFormatterError.WasmMemoryFailure? =
    runCatching { dealloc.apply(buffer.pointer.toLong(), buffer.length.toLong()) }
        .exceptionOrNull()
        ?.let { error -> TomlFormatterError.WasmMemoryFailure("dealloc", error.describe()) }

private class WasmCleanupFailure(failure: TomlFormatterError.WasmMemoryFailure) :
    RuntimeException("WASM cleanup failed: $failure")

context(raise: Raise<TomlFormatterError>)
internal fun WasmFormatterRuntime.writeBuffer(buffer: WasmBuffer, bytes: ByteArray) {
    if (bytes.size != buffer.length) {
        raise.raise(
            TomlFormatterError.WasmMemoryFailure(
                "write",
                "byte count ${bytes.size} did not match buffer length ${buffer.length}",
            )
        )
    }
    ensureRange(memory, buffer, "write")

    val result = runCatching { memory.write(buffer.pointer, bytes) }
    result.getOrElse { error ->
        raise.raise(TomlFormatterError.WasmMemoryFailure("write", error.describe()))
    }
}

context(raise: Raise<TomlFormatterError>)
internal fun WasmFormatterRuntime.readBuffer(buffer: WasmBuffer): ByteArray {
    ensureRange(memory, buffer, "read")

    return runCatching { memory.readBytes(buffer.pointer, buffer.length) }
        .getOrElse { error ->
            raise.raise(TomlFormatterError.WasmMemoryFailure("read", error.describe()))
        }
}

context(raise: Raise<TomlFormatterError>)
internal fun WasmFormatterRuntime.invokeSingle(operation: String, vararg args: Long): Long {
    val function =
        when (operation) {
            "alloc" -> alloc
            "format_toml" -> formatToml
            else ->
                raise.raise(
                    TomlFormatterError.WasmInvocationFailure(operation, "unknown exported function")
                )
        }

    val results =
        runCatching { function.apply(*args) }
            .getOrElse { error ->
                raise.raise(TomlFormatterError.WasmInvocationFailure(operation, error.describe()))
            }

    return results.singleOrNull()
        ?: raise.raise(
            TomlFormatterError.WasmInvocationFailure(
                operation,
                "expected one result, got ${results.size}",
            )
        )
}

context(raise: Raise<TomlFormatterError>)
private fun Long.toIntPointer(operation: String): Int {
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
internal fun ensureRange(memory: Memory, buffer: WasmBuffer, operation: String) {
    if (buffer.pointer < 0 || buffer.length < 0) {
        raise.raise(
            TomlFormatterError.WasmMemoryFailure(operation, "negative pointer or length: $buffer")
        )
    }

    val end = buffer.pointer.toLong() + buffer.length.toLong()
    val memorySize = memory.pages().toLong() * Memory.PAGE_SIZE.toLong()
    if (end > memorySize) {
        raise.raise(
            TomlFormatterError.WasmMemoryFailure(
                operation,
                "range $buffer exceeds memory size $memorySize",
            )
        )
    }
}
