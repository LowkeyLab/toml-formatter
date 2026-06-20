package com.github.lowkeylab.tomlformatter.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Initial check task has no declared output snapshot")
public abstract class CheckTomlFormatTask : DefaultTask() {
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    public fun check() {
        val projectDir = project.projectDir
        val unformattedFiles =
            resolveSourceFiles(projectDir, sourceFiles).filter { file ->
                val original = file.readText()
                val formatted = formatTomlFileContents(original, file, projectDir)
                formatted != original
            }

        if (unformattedFiles.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("TOML formatting check failed for:")
                    unformattedFiles.forEach { file ->
                        appendLine(" - ${file.displayPath(projectDir)}")
                    }
                    appendLine()
                    appendLine("Run `./gradlew formatToml` to fix.")
                }
            )
        }
    }
}
