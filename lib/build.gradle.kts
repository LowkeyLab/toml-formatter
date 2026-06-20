plugins {
    `java-library`
    id("base.repositories")
    id("base.java-toolchain")
    id("feature.kotlin-jvm")
    id("feature.protobuf-kotlin")
    id("check.kotest")
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.chicory.runtime)
    implementation(libs.protobuf.java)

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val wasmArtifact = rootProject.layout.projectDirectory.file(
    "wasm/target/wasm32-unknown-unknown/release/taplo_wasm.wasm",
)

val buildWasm by tasks.registering(Exec::class) {
    group = "build"
    description = "Builds the Rust formatter WASM artifact for JVM resource packaging."

    workingDir = rootProject.layout.projectDirectory.dir("wasm").asFile
    commandLine("cargo", "build", "--target", "wasm32-unknown-unknown", "--release")

    inputs.files(
        rootProject.layout.projectDirectory.file("wasm/Cargo.toml"),
        rootProject.layout.projectDirectory.file("wasm/Cargo.lock"),
    )
    inputs.dir(rootProject.layout.projectDirectory.dir("wasm/src"))
    inputs.dir(rootProject.layout.projectDirectory.dir("wasm/proto"))
    outputs.file(wasmArtifact)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(buildWasm)
    from(wasmArtifact) {
        into("wasm")
        rename { "taplo_wasm.wasm" }
    }
}
