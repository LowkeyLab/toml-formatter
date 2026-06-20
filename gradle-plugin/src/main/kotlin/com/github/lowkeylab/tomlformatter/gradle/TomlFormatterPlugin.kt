package com.github.lowkeylab.tomlformatter.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

public class TomlFormatterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create("tomlFormatter", TomlFormatterExtension::class.java, project)
        val sourceFiles =
            project.objects
                .fileCollection()
                .from(
                    project.provider {
                        if (extension.inputs.hasExplicitSources.get()) {
                            extension.inputs.sources
                        } else {
                            project.files(project.layout.projectDirectory)
                        }
                    }
                )

        project.tasks.register("formatToml", FormatTomlTask::class.java) { task ->
            task.group = "formatting"
            task.description = "Formats configured TOML files in place."
            task.sourceFiles.from(sourceFiles)
            task.includes.set(extension.includes)
            task.excludes.set(extension.excludes)
        }

        val checkTomlFormat =
            project.tasks.register("checkTomlFormat", CheckTomlFormatTask::class.java) { task ->
                task.group = "verification"
                task.description = "Checks that configured TOML files are formatted."
                task.sourceFiles.from(sourceFiles)
                task.includes.set(extension.includes)
                task.excludes.set(extension.excludes)
            }

        project.pluginManager.withPlugin("base") {
            project.tasks.named("check") { task -> task.dependsOn(checkTomlFormat) }
        }
    }
}
