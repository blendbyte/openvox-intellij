package net.blendbyte.openvox.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel

class OpenVoxConfigurable : BoundConfigurable("OpenVox / Puppet") {

    override fun createPanel(): DialogPanel {
        val settings = OpenVoxSettings.getInstance()
        return panel {
            group("Validation") {
                row {
                    checkBox("Run puppet parser validate")
                        .bindSelected(settings::runParserValidate)
                        .comment("Reports the compiler's own syntax errors as you edit.")
                }
                row("Puppet executable:") {
                    textField()
                        .bindText(settings::puppetExecutable)
                        .columns(40)
                        .comment(settings.resolvedPuppet()?.let { "Found: $it" } ?: "Not found on PATH")
                }
                row {
                    checkBox("Run puppet-lint")
                        .bindSelected(settings::runPuppetLint)
                        .comment("Reports style-guide violations.")
                }
                row("puppet-lint executable:") {
                    textField()
                        .bindText(settings::puppetLintExecutable)
                        .columns(40)
                        .comment(settings.resolvedPuppetLint()?.let { "Found: $it" } ?: "Not found on PATH")
                }
            }
        }
    }
}
