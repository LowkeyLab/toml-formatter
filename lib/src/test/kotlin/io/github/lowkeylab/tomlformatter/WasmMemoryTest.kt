package io.github.lowkeylab.tomlformatter

import arrow.core.raise.either
import io.github.lowkeylab.tomlformatter.internal.WasmBuffer
import io.github.lowkeylab.tomlformatter.internal.WasmExport
import io.github.lowkeylab.tomlformatter.internal.allocate
import io.github.lowkeylab.tomlformatter.internal.ensureRange
import io.github.lowkeylab.tomlformatter.internal.invokeSingle
import io.github.lowkeylab.tomlformatter.internal.readBuffer
import io.github.lowkeylab.tomlformatter.internal.singleResult
import io.github.lowkeylab.tomlformatter.internal.validateAllocationLength
import io.github.lowkeylab.tomlformatter.internal.validateWriteSize
import io.github.lowkeylab.tomlformatter.internal.withDeallocated
import io.github.lowkeylab.tomlformatter.internal.writeBuffer
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WasmMemoryTest :
    FunSpec({
        test("rejects negative allocation lengths") {
            val result = either<TomlFormatterError, Int> { validateAllocationLength(-1) }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.WasmMemoryFailure("allocate", "length must be non-negative: -1")
        }

        test("allocates a buffer through the alloc export") {
            val runtime =
                context(testMemory()) { wasmRuntime(alloc = WasmExport { longArrayOf(12) }) }
            val result = either<TomlFormatterError, WasmBuffer> { runtime.allocate(5) }

            result.shouldBeRight() shouldBe WasmBuffer(pointer = 12, length = 5)
        }

        test("rejects alloc pointers that do not fit a Chicory int") {
            val runtime =
                context(testMemory()) {
                    wasmRuntime(alloc = WasmExport { longArrayOf(Int.MAX_VALUE.toLong() + 1L) })
                }
            val result = either<TomlFormatterError, WasmBuffer> { runtime.allocate(5) }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.WasmMemoryFailure(
                    "alloc",
                    "pointer does not fit Chicory int address: 2147483648",
                )
        }

        test("rejects write byte counts that do not match the target buffer") {
            val result =
                either<TomlFormatterError, ByteArray> {
                    validateWriteSize(WasmBuffer(pointer = 0, length = 2), byteArrayOf(1))
                }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.WasmMemoryFailure(
                    "write",
                    "byte count 1 did not match buffer length 2",
                )
        }

        test("rejects negative memory ranges") {
            val result =
                either<TomlFormatterError, WasmBuffer> {
                    context(testMemory()) {
                        ensureRange(
                            wasmLinearMemory(),
                            WasmBuffer(pointer = -1, length = 1),
                            "read",
                        )
                    }
                }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.WasmMemoryFailure(
                    "read",
                    "negative pointer or length: WasmBuffer(pointer=-1, length=1)",
                )
        }

        test("rejects memory ranges beyond byte size") {
            val result =
                either<TomlFormatterError, WasmBuffer> {
                    context(testMemory(byteSize = 4)) {
                        ensureRange(
                            wasmLinearMemory(),
                            WasmBuffer(pointer = 3, length = 2),
                            "write",
                        )
                    }
                }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.WasmMemoryFailure(
                    "write",
                    "range WasmBuffer(pointer=3, length=2) exceeds memory size 4",
                )
        }

        test("writes and reads through adapter-backed memory") {
            val testMemory = testMemory(reads = mapOf(4 to byteArrayOf(9, 8)))
            val runtime = context(testMemory) { wasmRuntime() }

            val writeResult =
                either<TomlFormatterError, Unit> {
                    runtime.writeBuffer(WasmBuffer(pointer = 2, length = 2), byteArrayOf(1, 2))
                }
            val readResult =
                either<TomlFormatterError, ByteArray> {
                    runtime.readBuffer(WasmBuffer(pointer = 4, length = 2))
                }

            writeResult.shouldBeRight()
            readResult.shouldBeRight().toList() shouldBe listOf<Byte>(9, 8)
            testMemory.writes shouldBe mapOf(2 to listOf<Byte>(1, 2))
        }

        test("maps unknown operation to invocation failure") {
            val runtime = context(testMemory()) { wasmRuntime() }
            val result = either<TomlFormatterError, Long> { runtime.invokeSingle("missing") }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.WasmInvocationFailure("missing", "unknown exported function")
        }

        test("maps export exceptions to invocation failure") {
            val runtime =
                context(testMemory()) { wasmRuntime(formatToml = WasmExport { error("boom") }) }
            val result = either<TomlFormatterError, Long> { runtime.invokeSingle("format_toml") }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.WasmInvocationFailure("format_toml", "boom")
        }

        test("requires exactly one WASM export result") {
            val result =
                either<TomlFormatterError, Long> { singleResult("alloc", longArrayOf(1, 2)) }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.WasmInvocationFailure("alloc", "expected one result, got 2")
        }

        test("raises cleanup failures when the protected block succeeds") {
            val runtime =
                context(testMemory()) { wasmRuntime(dealloc = WasmExport { error("cannot free") }) }
            val result =
                either<TomlFormatterError, String> {
                    runtime.withDeallocated(WasmBuffer(pointer = 1, length = 2)) { "ok" }
                }

            result.shouldBeLeft() shouldBe
                TomlFormatterError.WasmMemoryFailure("dealloc", "cannot free")
        }
    })
