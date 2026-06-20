import com.vanniktech.maven.publish.GradlePublishPlugin

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.vanniktech.maven.publish)
    id("base.repositories")
    id("base.java-toolchain")
    id("feature.kotlin-jvm")
    id("check.ktfmt")
    id("check.detekt")
    id("check.kotest")
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.arrow.core)

    testImplementation(gradleTestKit())
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website = "https://github.com/lowkeylab/toml-formatter"
    vcsUrl = "https://github.com/lowkeylab/toml-formatter.git"

    plugins {
        create("tomlFormatter") {
            id = "io.github.lowkeylab.toml-formatter"
            displayName = "TOML Formatter"
            description = "Gradle TOML formatter that uses Taplo."
            tags = listOf("toml", "formatter", "formatting")
            implementationClass = "io.github.lowkeylab.tomlformatter.gradle.TomlFormatterPlugin"
        }
    }
}

mavenPublishing {
    coordinates(group.toString(), "toml-formatter-gradle-plugin", version.toString())
    configure(GradlePublishPlugin())

    pom {
        name.set("TOML Formatter Gradle Plugin")
        description.set("Gradle TOML formatter that uses Taplo.")
        url.set("https://github.com/lowkeylab/toml-formatter")
        licenses {
            license {
                name.set("GNU Affero General Public License, Version 3")
                url.set("https://www.gnu.org/licenses/agpl-3.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("lowkeylab")
                name.set("Lowkey Lab")
                email.set("lowkeylab@users.noreply.github.com")
                organization.set("Lowkey Lab")
                organizationUrl.set("https://github.com/lowkeylab")
                url.set("https://github.com/lowkeylab")
            }
        }
        scm {
            url.set("https://github.com/lowkeylab/toml-formatter")
            connection.set("scm:git:https://github.com/lowkeylab/toml-formatter.git")
            developerConnection.set("scm:git:ssh://git@github.com/lowkeylab/toml-formatter.git")
        }
    }
}
