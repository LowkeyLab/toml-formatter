package io.github.lowkeylab.tomlformatter.gradle

import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory

public open class TomlFormatterExtension @Inject constructor(objects: ObjectFactory) {
    public val inputs: ConfigurableFileCollection = objects.fileCollection()
}
