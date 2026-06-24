package io.github.lowkeylab.tomlformatter.internal

import arrow.core.raise.Raise
import com.google.protobuf.InvalidProtocolBufferException
import io.github.lowkeylab.tomlformatter.TomlFormatterError
import taplo_wasm.FormatToml.FormatTomlResult
import taplo_wasm.FormatToml.FormatTomlResult.ResultCase

context(raise: Raise<TomlFormatterError>)
internal fun decodeFormatterResult(bytes: ByteArray): String =
    mapFormatterResult(decodeProtobuf(bytes))

context(raise: Raise<TomlFormatterError>)
internal fun mapFormatterResult(result: FormatTomlResult): String =
    when (result.resultCase) {
        ResultCase.SUCCESS -> result.success.formatted
        ResultCase.FAILURE ->
            raise.raise(TomlFormatterError.FormatterCoreFailure(result.failure.message))
        ResultCase.RESULT_NOT_SET -> raise.raise(TomlFormatterError.MissingProtobufResult)
        null -> raise.raise(TomlFormatterError.MissingProtobufResult)
    }

context(raise: Raise<TomlFormatterError>)
internal fun decodeProtobuf(bytes: ByteArray): FormatTomlResult =
    try {
        FormatTomlResult.parseFrom(bytes)
    } catch (error: InvalidProtocolBufferException) {
        raise.raise(TomlFormatterError.ProtobufDecodeFailure(error.describe()))
    }
