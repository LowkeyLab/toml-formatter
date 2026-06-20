package io.github.lowkeylab.tomlformatter

import arrow.core.raise.Raise
import io.github.lowkeylab.tomlformatter.internal.formatTomlWithWasm

context(raise: Raise<TomlFormatterError>)
public fun formatToml(source: String): String = formatTomlWithWasm(source)
