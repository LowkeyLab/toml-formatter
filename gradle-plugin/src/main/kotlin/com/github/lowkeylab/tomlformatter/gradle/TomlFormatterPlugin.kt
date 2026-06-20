package com.github.lowkeylab.tomlformatter.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree

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
                            project.defaultTomlSources()
                        }
                    }
                )

        project.tasks.register("formatToml", FormatTomlTask::class.java) { task ->
            task.group = "formatting"
            task.description = "Formats configured TOML files in place."
            task.sourceFiles.from(sourceFiles)
        }

        val checkTomlFormat =
            project.tasks.register("checkTomlFormat", CheckTomlFormatTask::class.java) { task ->
                task.group = "verification"
                task.description = "Checks that configured TOML files are formatted."
                task.sourceFiles.from(sourceFiles)
            }

        project.pluginManager.withPlugin("base") {
            project.tasks.named("check") { task -> task.dependsOn(checkTomlFormat) }
        }
    }
}

private fun Project.defaultTomlSources(): FileTree =
    fileTree(layout.projectDirectory.asFile).apply {
        include("**/*.toml")
        exclude(
            "build/**",
            ".gradle/**",
            ".kotlin/**",
            ".direnv/**",
            "**/build/**",
            "**/.gradle/**",
            "**/.kotlin/**",
            "**/.direnv/**",
            "wasm/target/**",
            "**/target/**",
        )
    }
