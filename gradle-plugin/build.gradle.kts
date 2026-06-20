import com.vanniktech.maven.publish.GradlePublishPlugin
import org.gradle.plugin.compatibility.compatibility

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
            description =
                "Formats TOML files in Gradle builds using a Kotlin/JVM wrapper around taplo."
            tags = listOf("toml", "formatter", "formatting")
            implementationClass = "io.github.lowkeylab.tomlformatter.gradle.TomlFormatterPlugin"
            compatibility { features { configurationCache.set(true) } }
        }
    }
}

mavenPublishing {
    configure(GradlePublishPlugin())

    pom {
        name.set("TOML Formatter Gradle Plugin")
        description.set(
            "Gradle plugin that formats and checks TOML files using the TOML Formatter JVM library."
        )
        url.set("https://github.com/lowkeylab/toml-formatter")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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
