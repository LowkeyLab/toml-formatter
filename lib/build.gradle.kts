import com.google.protobuf.gradle.proto
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.chicory.runtime)
    implementation(libs.protobuf.java)

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

sourceSets {
    main {
        proto {
            srcDir(rootProject.layout.projectDirectory.dir("wasm/proto"))
            include("format_toml.proto")
        }
    }
}

val protobufVersion = libs.versions.protobuf.java.get()

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(tasks.named("generateProto"))
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

tasks.named<Test>("test") {
    useJUnitPlatform()
}
