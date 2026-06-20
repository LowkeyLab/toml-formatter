import org.gradle.api.plugins.JavaPluginExtension

pluginManager.withPlugin("java") {
    extensions.configure<JavaPluginExtension>("java") {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }
}
