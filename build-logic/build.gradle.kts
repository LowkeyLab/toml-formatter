plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("dev.detekt:dev.detekt.gradle.plugin:${libs.versions.detekt.get()}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation(libs.ktfmt.gradle.plugin)
    implementation("com.google.protobuf:protobuf-gradle-plugin:${libs.versions.protobuf.gradle.plugin.get()}")
}
