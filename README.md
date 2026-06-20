# TOML Formatter

Kotlin/JVM TOML formatter with a Gradle plugin backed by the project formatter library.

## Gradle plugin

Apply the plugin to a Gradle project:

```kotlin
plugins {
    id("com.github.lowkeylab.toml-formatter")
}
```

With no additional configuration, `formatToml` and `checkTomlFormat` scan the project for `**/*.toml` files. The default scan skips common generated and build/tooling directories such as `build`, `.gradle`, `.kotlin`, `.direnv`, and `target` directories.

```shell
./gradlew formatToml      # formats selected files in place
./gradlew checkTomlFormat # fails if selected files are not formatted
```

When the Gradle `base` plugin is applied, `checkTomlFormat` is wired into the `check` lifecycle task.

## Configuring inputs

`tomlFormatter.inputs` is the single source of truth for file selection. If you configure inputs, the default project scan is replaced by the files or file collections you provide.

Format one file:

```kotlin
tomlFormatter {
    inputs.from("config/app.toml")
}
```

Use the plugin's wildcard string convenience to create a Gradle file tree:

```kotlin
tomlFormatter {
    inputs.from("config/**/*.toml")
}
```

Use Gradle-native file collection APIs for custom filtering:

```kotlin
tomlFormatter {
    inputs.from(fileTree("config") {
        include("**/*.toml")
        exclude("**/generated/**")
    })
}
```

Broad explicit inputs are honored as configured. For example, `inputs.from("config")` gives the task the files under `config`; narrow the selection with `fileTree` if only specific files should be processed.

