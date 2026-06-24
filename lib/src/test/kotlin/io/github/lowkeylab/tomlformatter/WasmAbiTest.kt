package io.github.lowkeylab.tomlformatter

import arrow.core.raise.either
import io.github.lowkeylab.tomlformatter.internal.WasmBuffer
import io.github.lowkeylab.tomlformatter.internal.WasmExport
import io.github.lowkeylab.tomlformatter.internal.WasmFormatterRuntime
import io.github.lowkeylab.tomlformatter.internal.WasmLinearMemory
import io.github.lowkeylab.tomlformatter.internal.encodeSource
import io.github.lowkeylab.tomlformatter.internal.formatTomlWithRuntime
import io.github.lowkeylab.tomlformatter.internal.toWasmBuffer
import io.github.lowkeylab.tomlformatter.internal.unpackResultBuffer
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import taplo_wasm.FormatToml.FormatTomlResult
import taplo_wasm.FormatToml.FormatTomlSuccess

class WasmAbiTest :
    FunSpec({
        test("encodes source as UTF-8 bytes") {
            encodeSource("føø").decodeToString() shouldBe "føø"
        }

        test("unpacks a valid packed result buffer") {
            val result =
                either<TomlFormatterError, WasmBuffer> {
                    packed(pointer = 4, length = 3).toWasmBuffer()
                }

            result.shouldBeRight() shouldBe WasmBuffer(pointer = 4, length = 3)
        }

        test("rejects packed pointers that do not fit a Chicory int") {
            val packed = packed(pointer = Int.MAX_VALUE.toLong() + 1L, length = 0)
            val result =
                either<TomlFormatterError, WasmBuffer> { unpackResultBuffer(packed, memory()) }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.InvalidPackedResult(
                    packed,
                    "pointer does not fit Chicory int address/length: 2147483648",
                )
        }

        test("rejects packed lengths that do not fit a Chicory int") {
            val packed = packed(pointer = 0, length = Int.MAX_VALUE.toLong() + 1L)
            val result =
                either<TomlFormatterError, WasmBuffer> { unpackResultBuffer(packed, memory()) }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.InvalidPackedResult(
                    packed,
                    "length does not fit Chicory int address/length: 2147483648",
                )
        }

        test("rejects packed result buffers outside memory") {
            val packed = packed(pointer = 8, length = 4)
            val result =
                either<TomlFormatterError, WasmBuffer> {
                    unpackResultBuffer(packed, memory(byteSize = 10))
                }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.InvalidPackedResult(
                    packed,
                    "range WasmBuffer(pointer=8, length=4) exceeds memory size 10",
                )
        }

        test("formats with adapter-backed runtime without real Chicory") {
            val formatted = "key = \"value\"\n"
            val resultBytes =
                FormatTomlResult.newBuilder()
                    .setSuccess(FormatTomlSuccess.newBuilder().setFormatted(formatted))
                    .build()
                    .toByteArray()
            val fakeMemory = memory(reads = mapOf(20 to resultBytes))
            val runtime =
                abiRuntime(
                    memory = fakeMemory,
                    alloc = WasmExport { longArrayOf(10) },
                    dealloc = WasmExport { longArrayOf() },
                    formatToml =
                        WasmExport {
                            longArrayOf(packed(pointer = 20, length = resultBytes.size.toLong()))
                        },
                )

            val result =
                either<TomlFormatterError, String> {
                    formatTomlWithRuntime(runtime, byteArrayOf(1, 2))
                }

            result.shouldBeRight() shouldBe formatted
            fakeMemory.writes shouldBe mapOf(10 to byteArrayOf(1, 2).toList())
        }
    })

private fun packed(pointer: Long, length: Long): Long =
    (pointer shl 32) or (length and 0xFFFF_FFFFL)

private fun abiRuntime(
    memory: AbiTestMemory = memory(),
    alloc: WasmExport = WasmExport { longArrayOf(0) },
    dealloc: WasmExport = WasmExport { longArrayOf() },
    formatToml: WasmExport = WasmExport { longArrayOf(0) },
): WasmFormatterRuntime = WasmFormatterRuntime(memory, alloc, dealloc, formatToml)

private fun memory(byteSize: Long = 64, reads: Map<Int, ByteArray> = emptyMap()): AbiTestMemory =
    AbiTestMemory(byteSize = byteSize, reads = reads)

private class AbiTestMemory(override val byteSize: Long, private val reads: Map<Int, ByteArray>) :
    WasmLinearMemory {
    val writes = mutableMapOf<Int, List<Byte>>()

    override fun write(pointer: Int, bytes: ByteArray) {
        writes[pointer] = bytes.toList()
    }

    override fun readBytes(pointer: Int, length: Int): ByteArray =
        checkNotNull(reads[pointer]) { "missing read at $pointer" }.also { it.size shouldBe length }
}
