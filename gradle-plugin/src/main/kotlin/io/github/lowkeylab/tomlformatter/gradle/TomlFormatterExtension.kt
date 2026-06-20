package io.github.lowkeylab.tomlformatter.gradle

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

public open class TomlFormatterExtension
@Inject
constructor(objects: ObjectFactory, project: Project) {
    public val inputs: TomlFormatterInputs =
        objects.newInstance(TomlFormatterInputs::class.java, project)
}

public open class TomlFormatterInputs @Inject constructor(private val project: Project) {
    internal val sources: ConfigurableFileCollection = project.objects.fileCollection()
    internal val hasExplicitSources: Property<Boolean> =
        project.objects.property(Boolean::class.java).convention(false)

    public fun from(vararg sources: Any) {
        hasExplicitSources.set(true)
        this.sources.from(sources.map(::normalizeSource))
    }

    private fun normalizeSource(source: Any): Any =
        if (source is String && source.hasWildcard()) {
            project.fileTree(source.globBaseDirectory()).apply {
                include(source.globIncludePattern())
            }
        } else {
            source
        }
}

private fun String.hasWildcard(): Boolean = any { it == '*' || it == '?' || it == '[' }

private fun String.globBaseDirectory(): Any {
    val normalized = replace('\\', '/')
    val wildcardIndex = normalized.indexOfFirst { it == '*' || it == '?' || it == '[' }
    val slashBeforeWildcard = normalized.take(wildcardIndex).lastIndexOf('/')

    return when {
        slashBeforeWildcard < 0 -> "."
        slashBeforeWildcard == 0 -> normalized.substring(0, 1)
        else -> normalized.substring(0, slashBeforeWildcard)
    }
}

private fun String.globIncludePattern(): String {
    val normalized = replace('\\', '/')
    val wildcardIndex = normalized.indexOfFirst { it == '*' || it == '?' || it == '[' }
    val slashBeforeWildcard = normalized.take(wildcardIndex).lastIndexOf('/')

    return if (slashBeforeWildcard < 0) normalized
    else normalized.substring(slashBeforeWildcard + 1)
}
