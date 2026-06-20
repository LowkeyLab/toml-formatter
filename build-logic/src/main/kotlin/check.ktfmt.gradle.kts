plugins {
  id("com.ncorti.ktfmt.gradle")
}

ktfmt { kotlinLangStyle() }

tasks.named("check") { dependsOn("ktfmtCheck") }
