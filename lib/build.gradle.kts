import com.google.protobuf.gradle.proto
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    id("base.repositories")
    id("base.java-toolchain")
    id("feature.kotlin-jvm")
    id("feature.protobuf-kotlin")
    id("check.ktfmt")
    id("check.detekt")
    id("check.kotest")
}

val tomlFormatterArtifactKind =
    Attribute.of("com.github.lowkeylab.tomlformatter.artifact-kind", String::class.java)

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
