package io.github.lowkeylab.tomlformatter.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

public class TomlFormatterPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.applyTomlFormatterPlugin()
}

internal fun Project.applyTomlFormatterPlugin() {
    val extension = createTomlFormatterExtension()
    registerFormatTomlTask(extension)
    val checkTomlFormat = registerCheckTomlFormatTask(extension)
    wireTomlFormatterCheckLifecycle(checkTomlFormat)
}

internal fun Project.createTomlFormatterExtension(): TomlFormatterExtension =
    extensions.create("tomlFormatter", TomlFormatterExtension::class.java)

internal fun Project.registerFormatTomlTask(
    extension: TomlFormatterExtension
): TaskProvider<FormatTomlTask> =
    tasks.register("formatToml", FormatTomlTask::class.java) { task ->
        task.group = "formatting"
        task.description = "Formats configured TOML files in place."
        task.projectDirectory.set(layout.projectDirectory)
        task.sourceFiles.from(extension.inputs)
    }

internal fun Project.registerCheckTomlFormatTask(
    extension: TomlFormatterExtension
): TaskProvider<CheckTomlFormatTask> =
    tasks.register("checkTomlFormat", CheckTomlFormatTask::class.java) { task ->
        task.group = "verification"
        task.description = "Checks that configured TOML files are formatted."
        task.projectDirectory.set(layout.projectDirectory)
        task.sourceFiles.from(extension.inputs)
    }

internal fun Project.wireTomlFormatterCheckLifecycle(
    checkTomlFormat: TaskProvider<CheckTomlFormatTask>
) {
    pluginManager.withPlugin("base") {
        tasks.named("check") { task -> task.dependsOn(checkTomlFormat) }
    }
}
