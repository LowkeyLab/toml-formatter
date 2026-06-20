plugins {
    id("base.versioning")
    alias(libs.plugins.gradle.plugin.publish) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

allprojects {
    group = "io.github.lowkeylab"
}
