# TOML Formatter

Kotlin/JVM TOML formatter with a Gradle plugin backed by the project formatter library.

## Repository versioning

The root build applies the `base.versioning` convention plugin from `build-logic`. It assigns every project a version in `day.month.year+buildNumber` format, for example `20.6.2026+42`.

The date defaults to the current local date and can be fixed with `-PversionDate=YYYY-MM-DD`. The build number comes from `-PbuildNumber`, then `BUILD_NUMBER`, and defaults to `0` for local builds.

```shell
./gradlew properties -PversionDate=2026-06-20 -PbuildNumber=42
```

## Publishing

The build uses the [Vanniktech Gradle Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/) to publish `:lib` and `:gradle-plugin` to Maven Central. The Gradle plugin project also applies `com.gradle.plugin-publish` so the plugin can be released to the Gradle Plugin Portal.

Publish coordinates use group `com.github.lowkeylab.toml-formatter` and the repository version described above. The GitHub Actions publish workflow passes `-PbuildNumber` from the workflow run number. Before releasing, provide Maven Central and signing credentials through Gradle properties or environment variables:

```shell
export ORG_GRADLE_PROJECT_mavenCentralUsername=...
export ORG_GRADLE_PROJECT_mavenCentralPassword=...
export ORG_GRADLE_PROJECT_signingInMemoryKey=...
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=...
```

Validate and publish Maven Central artifacts:

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

Manual publishing is available from the `Publish` workflow. It can publish Maven Central artifacts, validate or publish the Gradle Plugin Portal release, or do both in one run. Maven Central publishing uses `publishAndReleaseToMavenCentral`, so a successful workflow releases the artifacts automatically.

The workflow uses the repository version format `day.month.year+buildNumber`; `buildNumber` is the GitHub Actions run number. The optional `version_date` input accepts `YYYY-MM-DD`; if it is omitted, the workflow uses the current UTC date.

Configure these repository secrets before publishing:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_IN_MEMORY_KEY`
- `SIGNING_IN_MEMORY_KEY_PASSWORD`
- `GRADLE_PUBLISH_KEY`
- `GRADLE_PUBLISH_SECRET`

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
