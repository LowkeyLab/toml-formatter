package io.github.lowkeylab.tomlformatter.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
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

    @get:Internal public abstract val projectDirectory: DirectoryProperty

    @TaskAction
    public fun format() {
        val projectDir = projectDirectory.asFile.get()
        val files = resolveSourceFiles(projectDir, sourceFiles)

        context(DefaultTomlFileSystem, GradleTomlFormatLogger(logger)) {
            formatTomlFiles(files, projectDir)
        }
    }
}
