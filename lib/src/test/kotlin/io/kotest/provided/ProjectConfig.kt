package io.kotest.provided

import com.diffplug.selfie.kotest.SelfieExtension
import io.kotest.core.config.AbstractProjectConfig

class ProjectConfig : AbstractProjectConfig() {
    override val extensions = listOf(SelfieExtension(this))
}
