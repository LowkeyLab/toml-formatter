import com.google.protobuf.gradle.id
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.google.protobuf")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val protobufVersion = libs.findVersion("protobuf-java").get().requiredVersion

dependencies {
    add("implementation", libs.findLibrary("protobuf-kotlin").get())
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                id("kotlin")
            }
        }
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        sourceSets.named("main") {
            kotlin.srcDir(layout.buildDirectory.dir("generated/sources/proto/main/kotlin"))
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        dependsOn(tasks.named("generateProto"))
    }
}
