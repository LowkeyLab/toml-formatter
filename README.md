# TOML Formatter

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.lowkeylab.toml-formatter?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/io.github.lowkeylab.toml-formatter)

A TOML formatter for the JVM, backed by [Taplo](https://taplo.tamasfe.dev/). The repository includes a Kotlin/JVM formatter library and a Gradle plugin for formatting and checking TOML files in Gradle builds.

## Gradle plugin

```kotlin
plugins {
    id("io.github.lowkeylab.toml-formatter")
}
```

The plugin contains two tasks:

```shell
./gradlew formatToml      # formats selected files in place
./gradlew checkTomlFormat # fails if selected files are not formatted
```

When the Gradle `base` plugin is applied, `checkTomlFormat` is wired into the `check` lifecycle task.

## Configuring inputs

`tomlFormatter.inputs` is a `ConfigurableFileCollection`, and so can accept anything that can be passed into a FileCollection:

Format one file:

```kotlin
tomlFormatter {
    inputs.from("config/app.toml")
}
```

Or:

```kotlin
tomlFormatter {
    inputs.from(fileTree("config") {
        include("**/*.toml")
        exclude("**/generated/**")
    })
}
```

## Versioning

The project is versioned in `yyyy.mm.dd+buildNumber` format, for example `2026.06.20+42`.

## Publishing

The build uses the [Vanniktech Gradle Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/) to publish.

## GitHub Actions

CI runs on pull requests and pushes to `main`.
