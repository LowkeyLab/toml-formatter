package io.github.lowkeylab.tomlformatter.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

public class TomlFormatterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create("tomlFormatter", TomlFormatterExtension::class.java)

        project.tasks.register("formatToml", FormatTomlTask::class.java) { task ->
            task.group = "formatting"
            task.description = "Formats configured TOML files in place."
            task.projectDirectory.set(project.layout.projectDirectory)
            task.sourceFiles.from(extension.inputs)
        }

        val checkTomlFormat =
            project.tasks.register("checkTomlFormat", CheckTomlFormatTask::class.java) { task ->
                task.group = "verification"
                task.description = "Checks that configured TOML files are formatted."
                task.projectDirectory.set(project.layout.projectDirectory)
                task.sourceFiles.from(extension.inputs)
            }

        project.pluginManager.withPlugin("base") {
            project.tasks.named("check") { task -> task.dependsOn(checkTomlFormat) }
        }
    }
}
