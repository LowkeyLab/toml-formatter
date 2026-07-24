package io.github.lowkeylab.tomlformatter.gradle

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
public abstract class CheckTomlFormatTask : DefaultTask() {
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sourceFiles: ConfigurableFileCollection

    @get:Internal public abstract val projectDirectory: DirectoryProperty

    @get:OutputFile
    protected val successMarker: File
        get() = temporaryDir.resolve("success.txt")

    @TaskAction
    public fun check() {
        successMarker.delete()

        val projectDir = projectDirectory.asFile.get()
        val files = resolveSourceFiles(projectDir, sourceFiles)
        val unformattedFiles =
            context(DefaultTomlFileSystem) { unformattedTomlFiles(files, projectDir) }

        if (unformattedFiles.isNotEmpty()) {
            throw GradleException(tomlFormatFailureMessage(unformattedFiles, projectDir))
        }

        successMarker.writeText("")
    }
}
