plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("com.google.protobuf:protobuf-gradle-plugin:${libs.versions.protobuf.gradle.plugin.get()}")
}
