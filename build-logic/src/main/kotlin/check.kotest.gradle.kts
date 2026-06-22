import org.gradle.api.tasks.testing.Test

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    environment(properties.filter { it.key == "selfie" })
    inputs.files(
        fileTree("src/test") {
            include("**/*.ss")
        }
    )
}
