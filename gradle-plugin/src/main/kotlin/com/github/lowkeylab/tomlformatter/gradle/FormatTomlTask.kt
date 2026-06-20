package com.github.lowkeylab.tomlformatter.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Formats TOML files in place")
public abstract class FormatTomlTask : DefaultTask() {
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sourceFiles: ConfigurableFileCollection

    @get:Input public abstract val includes: ListProperty<String>

    @get:Input public abstract val excludes: ListProperty<String>

    @TaskAction
    public fun format() {
        val projectDir = project.projectDir
        val files = resolveTomlFiles(projectDir, sourceFiles, includes.get(), excludes.get())

        files.forEach { file ->
            val original = file.readText()
            val formatted = formatTomlFileContents(original, file, projectDir)

            if (formatted != original) {
                file.writeText(formatted)
                logger.lifecycle("Formatted ${file.displayPath(projectDir)}")
            }
        }
    }
}
