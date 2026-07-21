import com.vanniktech.maven.publish.GradlePublishPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.shadow)
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

tasks.jar { enabled = false }

tasks.named<ProcessResources>("processResources") {
    from(rootProject.file("LICENSE")) {
        into("META-INF")
        rename { "LICENSE.txt" }
    }
}

val shadedPackage = "io.github.lowkeylab.tomlformatter.gradle.internal.shaded"

tasks.shadowJar {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    failOnDuplicateEntries = true

    relocate(
        "io.github.lowkeylab.tomlformatter",
        "$shadedPackage.io.github.lowkeylab.tomlformatter",
    ) {
        exclude("io.github.lowkeylab.tomlformatter.gradle.**")
    }
    relocate("arrow", "$shadedPackage.arrow")
    relocate("com.dylibso.chicory", "$shadedPackage.com.dylibso.chicory")
    relocate("com.google.protobuf", "$shadedPackage.com.google.protobuf")
    relocate("org.intellij.lang.annotations", "$shadedPackage.org.intellij.lang.annotations")
    relocate("org.jetbrains.annotations", "$shadedPackage.org.jetbrains.annotations")
    relocate("taplo_wasm", "$shadedPackage.taplo_wasm")
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

val shadedJar = tasks.shadowJar
val pluginPomFile = layout.buildDirectory.file("publications/pluginMaven/pom-default.xml")
val pluginModuleMetadataFile = layout.buildDirectory.file("publications/pluginMaven/module.json")
val markerPomFile =
    layout.buildDirectory.file("publications/tomlFormatterPluginMarkerMaven/pom-default.xml")
val pluginVersion = version.toString()

tasks.named<Test>("test") {
    dependsOn(
        shadedJar,
        "generateMetadataFileForPluginMavenPublication",
        "generatePomFileForPluginMavenPublication",
        "generatePomFileForTomlFormatterPluginMarkerMavenPublication",
    )
    systemProperty("tomlFormatter.shadedJar", shadedJar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("tomlFormatter.pluginPom", pluginPomFile.get().asFile.absolutePath)
    systemProperty(
        "tomlFormatter.pluginModuleMetadata",
        pluginModuleMetadataFile.get().asFile.absolutePath,
    )
    systemProperty("tomlFormatter.markerPom", markerPomFile.get().asFile.absolutePath)
    systemProperty("tomlFormatter.pluginVersion", pluginVersion)
}
