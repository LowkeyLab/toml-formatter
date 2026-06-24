package io.github.lowkeylab.tomlformatter

import arrow.core.raise.either
import io.github.lowkeylab.tomlformatter.internal.WasmBuffer
import io.github.lowkeylab.tomlformatter.internal.WasmExport
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
                either<TomlFormatterError, WasmBuffer> {
                    context(testMemory()) { unpackResultBuffer(packed, wasmLinearMemory()) }
                }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.InvalidPackedResult(
                    packed,
                    "pointer does not fit Chicory int address/length: 2147483648",
                )
        }

        test("rejects packed lengths that do not fit a Chicory int") {
            val packed = packed(pointer = 0, length = Int.MAX_VALUE.toLong() + 1L)
            val result =
                either<TomlFormatterError, WasmBuffer> {
                    context(testMemory()) { unpackResultBuffer(packed, wasmLinearMemory()) }
                }

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
                    context(testMemory(byteSize = 10)) {
                        unpackResultBuffer(packed, wasmLinearMemory())
                    }
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
            val fakeMemory = testMemory(reads = mapOf(20 to resultBytes))
            val runtime =
                context(fakeMemory) {
                    wasmRuntime(
                        alloc = WasmExport { longArrayOf(10) },
                        dealloc = WasmExport { longArrayOf() },
                        formatToml =
                            WasmExport {
                                longArrayOf(
                                    packed(pointer = 20, length = resultBytes.size.toLong())
                                )
                            },
                    )
                }

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
