package com.github.lowkeylab.tomlformatter

import arrow.core.raise.either
import com.github.lowkeylab.tomlformatter.internal.decodeFormatterResult
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import taplo_wasm.FormatToml.FormatTomlFailure
import taplo_wasm.FormatToml.FormatTomlResult

class FormatTomlTest : FunSpec({
    test("formats valid TOML through the public Raise API") {
        val result = either<TomlFormatterError, String> {
            formatToml("key=\"value\"")
        }

        result.fold(
            ifLeft = { error -> fail("expected formatted TOML, got $error") },
            ifRight = { formatted -> formatted shouldBe "key = \"value\"\n" },
        )
    }

    test("raises protobuf decode errors through Arrow Raise") {
        val result = either<TomlFormatterError, String> {
            decodeFormatterResult(byteArrayOf(0xff.toByte()))
        }

        result.fold(
            ifLeft = { error -> error.shouldBeInstanceOf<TomlFormatterError.ProtobufDecodeFailure>() },
            ifRight = { formatted -> fail("expected protobuf decode failure, got $formatted") },
        )
    }

    test("maps formatter core protobuf failures to FormatterCoreFailure") {
        val message = "taplo could not format this input"
        val failureBytes = FormatTomlResult.newBuilder()
            .setFailure(FormatTomlFailure.newBuilder().setMessage(message))
            .build()
            .toByteArray()

        val result = either<TomlFormatterError, String> {
            decodeFormatterResult(failureBytes)
        }

        result.fold(
            ifLeft = { error -> error shouldBe TomlFormatterError.FormatterCoreFailure(message) },
            ifRight = { formatted -> fail("expected formatter core failure, got $formatted") },
        )
    }

    test("raises missing result for a protobuf message without a success or failure") {
        val result = either<TomlFormatterError, String> {
            decodeFormatterResult(FormatTomlResult.getDefaultInstance().toByteArray())
        }

        result.fold(
            ifLeft = { error -> error shouldBe TomlFormatterError.MissingProtobufResult },
            ifRight = { formatted -> fail("expected missing result failure, got $formatted") },
        )
    }
})
