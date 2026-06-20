import java.time.LocalDate

val versionDate =
    providers.gradleProperty("versionDate").map(LocalDate::parse).orElse(providers.provider { LocalDate.now() })
val buildNumber =
    providers.gradleProperty("buildNumber").orElse(providers.environmentVariable("BUILD_NUMBER")).orElse("0")

val repositoryVersion =
    versionDate.zip(buildNumber) { date, build ->
        "${date.dayOfMonth}.${date.monthValue}.${date.year}+$build"
    }

allprojects {
    version = repositoryVersion.get()
}
