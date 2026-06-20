plugins {
    `java-gradle-plugin`
    id("base.repositories")
    id("base.java-toolchain")
    id("feature.kotlin-jvm")
    id("check.ktfmt")
    id("check.detekt")
    id("check.kotest")
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.arrow.core)

    testImplementation(gradleTestKit())
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    plugins {
        create("tomlFormatter") {
            id = "com.github.lowkeylab.toml-formatter"
            implementationClass = "com.github.lowkeylab.tomlformatter.gradle.TomlFormatterPlugin"
        }
    }
}
