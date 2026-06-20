plugins {
    id("base.versioning")
    alias(libs.plugins.gradle.plugin.publish) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

allprojects {
    group = "com.github.lowkeylab.toml-formatter"
}
