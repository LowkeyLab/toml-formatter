package io.github.lowkeylab.tomlformatter

import arrow.core.raise.either
import io.github.lowkeylab.tomlformatter.internal.decodeFormatterResult
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import taplo_wasm.FormatToml.FormatTomlFailure
import taplo_wasm.FormatToml.FormatTomlResult

class FormatTomlTest :
    FunSpec({
        test("formats valid TOML through the public Raise API") {
            val result = either<TomlFormatterError, String> { formatToml("key=\"value\"") }

            result.shouldBeRight() shouldBe "key = \"value\"\n"
        }

        test("raises protobuf decode errors through Arrow Raise") {
            val result =
                either<TomlFormatterError, String> {
                    decodeFormatterResult(byteArrayOf(0xff.toByte()))
                }

            result.shouldBeLeft().shouldBeInstanceOf<TomlFormatterError.ProtobufDecodeFailure>()
        }

        test("maps formatter core protobuf failures to FormatterCoreFailure") {
            val message = "taplo could not format this input"
            val failureBytes =
                FormatTomlResult.newBuilder()
                    .setFailure(FormatTomlFailure.newBuilder().setMessage(message))
                    .build()
                    .toByteArray()

            val result = either<TomlFormatterError, String> { decodeFormatterResult(failureBytes) }

            result.shouldBeLeft() shouldBe TomlFormatterError.FormatterCoreFailure(message)
        }

        test("raises missing result for a protobuf message without a success or failure") {
            val result =
                either<TomlFormatterError, String> {
                    decodeFormatterResult(FormatTomlResult.getDefaultInstance().toByteArray())
                }

            result.shouldBeLeft() shouldBe TomlFormatterError.MissingProtobufResult
        }
    })
