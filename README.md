# TOML Formatter

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.lowkeylab.toml-formatter?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/io.github.lowkeylab.toml-formatter)

A TOML formatter for the JVM, backed by [Taplo](https://taplo.tamasfe.dev/). The repository includes a Kotlin/JVM formatter library and a Gradle plugin for formatting and checking TOML files in Gradle builds.

## Gradle plugin

Apply the plugin to a Gradle project:

```kotlin
plugins {
    id("io.github.lowkeylab.toml-formatter")
}
```

With no additional configuration, `formatToml` and `checkTomlFormat` have no selected files. Configure `tomlFormatter.inputs` with Gradle file collection APIs to choose files.

```shell
./gradlew formatToml      # formats selected files in place
./gradlew checkTomlFormat # fails if selected files are not formatted
```

When the Gradle `base` plugin is applied, `checkTomlFormat` is wired into the `check` lifecycle task.

The plugin declares compatibility with Gradle's configuration cache in its Plugin Portal metadata.

## Configuring inputs

`tomlFormatter.inputs` is the single source of truth for file selection. It is a Gradle `ConfigurableFileCollection`, so Gradle file, directory, provider, and file tree inputs are accepted directly.

Format one file:

```kotlin
tomlFormatter {
    inputs.from("config/app.toml")
}
```

Use Gradle-native file collection APIs for wildcard patterns and custom filtering. String inputs are passed to Gradle as paths; use `fileTree` for glob-style matching:

```kotlin
tomlFormatter {
    inputs.from(fileTree("config") {
        include("**/*.toml")
        exclude("**/generated/**")
    })
}
```

Broad explicit inputs are honored as configured. For example, `inputs.from("config")` gives the task the files under `config`; narrow the selection with `fileTree` if only specific files should be processed.

## Repository versioning

The root build applies the `base.versioning` convention plugin from `build-logic`. It assigns every project a version in `yyyy.mm.dd+buildNumber` format, for example `2026.06.20+42`.

The date defaults to the current local date and can be fixed with `-PversionDate=YYYY-MM-DD`. The build number comes from `-PbuildNumber`, then `BUILD_NUMBER`, and defaults to `0` for local builds.

```shell
./gradlew properties -PversionDate=2026-06-20 -PbuildNumber=42
```

## Publishing

The build uses the [Vanniktech Gradle Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/) to publish `:lib` to Maven Central. The Gradle plugin project applies `com.gradle.plugin-publish` and is published only to the Gradle Plugin Portal, not Maven Central.

Publish coordinates use group `io.github.lowkeylab` and the repository version described above. The GitHub Actions publish workflow passes `-PbuildNumber` from the workflow run number. Before releasing the JVM library, provide Maven Central and signing credentials through Gradle properties or environment variables:

```shell
export ORG_GRADLE_PROJECT_mavenCentralUsername=...
export ORG_GRADLE_PROJECT_mavenCentralPassword=...
export ORG_GRADLE_PROJECT_signingInMemoryKey=...
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=...
```

Validate and publish the JVM library Maven Central artifact:

```shell
./gradlew publishToMavenCentral --no-configuration-cache
./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
```

Publish the Gradle plugin to the Plugin Portal with Portal credentials:

```shell
export GRADLE_PUBLISH_KEY=...
export GRADLE_PUBLISH_SECRET=...
./gradlew :gradle-plugin:publishPlugins --validate-only
./gradlew :gradle-plugin:publishPlugins
```

## GitHub Actions

CI runs on pull requests and pushes to `main`. The workflow uses the Nix development shell from `flake.nix`, then runs Gradle checks and publication metadata checks.

Manual publishing is available from the `Publish` workflow. It can publish the JVM library to Maven Central, validate or publish the Gradle Plugin Portal release, or do both in one run. Maven Central publishing uses `publishAndReleaseToMavenCentral`, so a successful workflow releases the library artifact automatically.

The workflow uses the repository version format `yyyy.mm.dd+buildNumber`; `buildNumber` is the GitHub Actions run number. The optional `version_date` input accepts `YYYY-MM-DD`; if it is omitted, the workflow uses the current UTC date.

Configure these repository secrets before publishing:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_TOKEN`
- `GPG_ARMORED_KEY`
- `GRADLE_PUBLISH_KEY`
- `GRADLE_PUBLISH_SECRET`
