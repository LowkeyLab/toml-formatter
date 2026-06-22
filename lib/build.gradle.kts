import com.google.protobuf.gradle.proto
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    id("base.repositories")
    id("base.java-toolchain")
    id("feature.kotlin-jvm")
    id("feature.protobuf-kotlin")
    alias(libs.plugins.vanniktech.maven.publish)
    id("check.ktfmt")
    id("check.detekt")
    id("check.kotest")
}

val tomlFormatterArtifactKind =
    Attribute.of("io.github.lowkeylab.tomlformatter.artifact-kind", String::class.java)

val wasmBinary by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false

    attributes { attribute(tomlFormatterArtifactKind, "wasm-binary") }
}

val wasmProtoSources by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false

    attributes { attribute(tomlFormatterArtifactKind, "proto-sources") }
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.chicory.runtime)
    implementation(libs.protobuf.java)

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.selfie.runner.kotest)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    wasmBinary(project(path = ":wasm", configuration = "wasmBinaryElements"))
    wasmProtoSources(project(path = ":wasm", configuration = "protoSourceElements"))
}

extensions.configure<JavaPluginExtension>("java") {
    sourceSets.named("main") {
        proto {
            srcDir(wasmProtoSources)
            include("format_toml.proto")
        }
    }
}

tasks.named("generateProto") { dependsOn(wasmProtoSources) }

tasks.named<ProcessResources>("processResources") {
    from(wasmBinary) {
        into("wasm")
        rename { "taplo_wasm.wasm" }
    }
}

val isPublishingToMavenCentral =
    gradle.startParameter.taskNames.any { it.contains("MavenCentral", ignoreCase = true) }

mavenPublishing {
    coordinates(group.toString(), "toml-formatter", version.toString())
    configure(KotlinJvm(javadocJar = JavadocJar.Empty(), sourcesJar = SourcesJar.Sources()))
    publishToMavenCentral()
    if (isPublishingToMavenCentral) {
        signAllPublications()
    }

    pom {
        name.set("TOML Formatter JVM")
        description.set("Kotlin/JVM TOML formatter backed by taplo WebAssembly.")
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
