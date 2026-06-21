import org.gradle.api.attributes.Attribute

plugins {
    base
}

val tomlFormatterArtifactKind = Attribute.of("io.github.lowkeylab.tomlformatter.artifact-kind", String::class.java)

val wasmArtifact = layout.projectDirectory.file(
    "target/wasm32-unknown-unknown/release/taplo_wasm.wasm",
)

val buildWasm by tasks.registering(Exec::class) {
    group = "build"
    description = "Builds the Rust formatter WASM artifact."

    workingDir = layout.projectDirectory.asFile
    commandLine("cargo", "build", "--target", "wasm32-unknown-unknown", "--release")

    inputs.files(
        layout.projectDirectory.file("Cargo.toml"),
        layout.projectDirectory.file("Cargo.lock"),
    )
    inputs.dir(layout.projectDirectory.dir(".cargo"))
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.dir(layout.projectDirectory.dir("proto"))
    outputs.file(wasmArtifact)
}

val wasmBinaryElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    description = "Outgoing raw compiled formatter WASM binary."

    attributes {
        attribute(tomlFormatterArtifactKind, "wasm-binary")
    }
}

val protoSourceElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    description = "Outgoing raw formatter protobuf source directory."

    attributes {
        attribute(tomlFormatterArtifactKind, "proto-sources")
    }
}

artifacts {
    add(wasmBinaryElements.name, wasmArtifact) {
        builtBy(buildWasm)
    }
    add(protoSourceElements.name, layout.projectDirectory.dir("proto"))
}
