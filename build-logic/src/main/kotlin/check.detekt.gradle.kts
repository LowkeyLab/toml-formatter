import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
  id("dev.detekt")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<DetektExtension>("detekt") {
  toolVersion = libs.findVersion("detekt").get().requiredVersion
  config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
  buildUponDefaultConfig = true
  parallel = true
  basePath = projectDir
}
