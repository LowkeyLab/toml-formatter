package io.github.lowkeylab.tomlformatter.internal

import arrow.core.raise.Raise
import io.github.lowkeylab.tomlformatter.TomlFormatterError

internal data class WasmBuffer(val pointer: Int, val length: Int)

context(raise: Raise<TomlFormatterError>)
internal fun WasmFormatterRuntime.allocate(length: Int): WasmBuffer =
    WasmBuffer(
        invokeSingle("alloc", validateAllocationLength(length).toLong()).toIntPointer("alloc"),
        length,
    )

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
    validateWriteSize(buffer, bytes)
    ensureRange(memory, buffer, "write")
    writeMemory(memory, buffer.pointer, bytes)
}

context(raise: Raise<TomlFormatterError>)
private fun writeMemory(memory: WasmLinearMemory, pointer: Int, bytes: ByteArray): Unit =
    runCatching { memory.write(pointer, bytes) }
        .getOrElse { error ->
            raise.raise(TomlFormatterError.WasmMemoryFailure("write", error.describe()))
        }

context(raise: Raise<TomlFormatterError>)
internal fun WasmFormatterRuntime.readBuffer(buffer: WasmBuffer): ByteArray {
    ensureRange(memory, buffer, "read")
    return readMemory(memory, buffer)
}

context(raise: Raise<TomlFormatterError>)
private fun readMemory(memory: WasmLinearMemory, buffer: WasmBuffer): ByteArray =
    runCatching { memory.readBytes(buffer.pointer, buffer.length) }
        .getOrElse { error ->
            raise.raise(TomlFormatterError.WasmMemoryFailure("read", error.describe()))
        }
