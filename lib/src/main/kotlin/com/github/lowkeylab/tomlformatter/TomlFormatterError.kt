package com.github.lowkeylab.tomlformatter

/** Typed failures raised by the TOML formatter JVM wrapper. */
public sealed interface TomlFormatterError {
    public data object WasmResourceMissing : TomlFormatterError

    public data class WasmResourceUnreadable(
        public val message: String,
    ) : TomlFormatterError

    public data class ChicoryInstantiationFailure(
        public val message: String,
    ) : TomlFormatterError

    public data class WasmInvocationFailure(
        public val operation: String,
        public val message: String,
    ) : TomlFormatterError

    public data class WasmMemoryFailure(
        public val operation: String,
        public val message: String,
    ) : TomlFormatterError

    public data class InvalidPackedResult(
        public val packed: Long,
        public val reason: String,
    ) : TomlFormatterError

    public data class ProtobufDecodeFailure(
        public val message: String,
    ) : TomlFormatterError

    public data object MissingProtobufResult : TomlFormatterError

    public data class FormatterCoreFailure(
        public val message: String,
    ) : TomlFormatterError
}
