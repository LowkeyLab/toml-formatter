package io.github.lowkeylab.tomlformatter

import io.github.lowkeylab.tomlformatter.internal.WasmExport
import io.github.lowkeylab.tomlformatter.internal.WasmFormatterRuntime
import io.github.lowkeylab.tomlformatter.internal.WasmLinearMemory

internal class TestWasmMemory(
    val byteSize: Long = 64,
    private val reads: Map<Int, ByteArray> = emptyMap(),
) {
    val writes: MutableMap<Int, List<Byte>> = mutableMapOf()

    fun write(pointer: Int, bytes: ByteArray) {
        writes[pointer] = bytes.toList()
    }

    fun readBytes(pointer: Int, length: Int): ByteArray =
        checkNotNull(reads[pointer]) { "missing read at $pointer" }
            .also {
                check(it.size == length) { "expected $length bytes at $pointer, got ${it.size}" }
            }
}

internal fun testMemory(
    byteSize: Long = 64,
    reads: Map<Int, ByteArray> = emptyMap(),
): TestWasmMemory = TestWasmMemory(byteSize = byteSize, reads = reads)

context(memory: TestWasmMemory)
internal fun wasmLinearMemory(): WasmLinearMemory =
    object : WasmLinearMemory {
        override val byteSize: Long
            get() = memory.byteSize

        override fun write(pointer: Int, bytes: ByteArray): Unit = memory.write(pointer, bytes)

        override fun readBytes(pointer: Int, length: Int): ByteArray =
            memory.readBytes(pointer, length)
    }

context(memory: TestWasmMemory)
internal fun wasmRuntime(
    alloc: WasmExport = WasmExport { longArrayOf(0) },
    dealloc: WasmExport = WasmExport { longArrayOf() },
    formatToml: WasmExport = WasmExport { longArrayOf(0) },
): WasmFormatterRuntime = WasmFormatterRuntime(wasmLinearMemory(), alloc, dealloc, formatToml)
