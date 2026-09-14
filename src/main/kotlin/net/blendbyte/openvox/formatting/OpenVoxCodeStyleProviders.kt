package net.blendbyte.openvox.formatting

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import net.blendbyte.openvox.OpenVoxLanguage

class OpenVoxCodeStyleSettingsProvider : CodeStyleSettingsProvider() {

    override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings =
        OpenVoxCodeStyleSettings(settings)

    override fun getConfigurableDisplayName(): String = "OpenVox / Puppet"

    override fun getLanguage(): Language = OpenVoxLanguage

    override fun createConfigurable(
        settings: CodeStyleSettings,
        modelSettings: CodeStyleSettings,
    ): CodeStyleConfigurable = object : CodeStyleAbstractConfigurable(settings, modelSettings, "OpenVox / Puppet") {
        override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
            object : TabbedLanguageCodeStylePanel(OpenVoxLanguage, currentSettings, settings) {}

        override fun getHelpTopic(): String? = null
    }
}

class OpenVoxLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language = OpenVoxLanguage

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        when (settingsType) {
            SettingsType.SPACING_SETTINGS -> {
                consumer.showCustomOption(
                    OpenVoxCodeStyleSettings::class.java,
                    "SPACE_WITHIN_HASH_BRACES",
                    "Within hash braces",
                    "OpenVox",
                )
            }
            SettingsType.WRAPPING_AND_BRACES_SETTINGS -> {
                consumer.showCustomOption(
                    OpenVoxCodeStyleSettings::class.java,
                    "ALIGN_RESOURCE_ARROWS",
                    "Align attribute arrows in a resource body",
                    "OpenVox",
                )
                consumer.showCustomOption(
                    OpenVoxCodeStyleSettings::class.java,
                    "ALIGN_HASH_ARROWS",
                    "Align arrows in a hash literal",
                    "OpenVox",
                )
                consumer.showCustomOption(
                    OpenVoxCodeStyleSettings::class.java,
                    "ALIGN_SELECTOR_ARROWS",
                    "Align arrows in a selector",
                    "OpenVox",
                )
            }
            else -> Unit
        }
    }

    override fun getIndentOptionsEditor() = com.intellij.application.options.SmartIndentOptionsEditor()

    override fun customizeDefaults(
        commonSettings: com.intellij.psi.codeStyle.CommonCodeStyleSettings,
        indentOptions: com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions,
    ) {
        indentOptions.INDENT_SIZE = 2
        indentOptions.CONTINUATION_INDENT_SIZE = 2
        indentOptions.TAB_SIZE = 2
        indentOptions.USE_TAB_CHARACTER = false
    }

    override fun getCodeSample(settingsType: SettingsType): String = """
        class profile::webserver (
          Enum['running', 'stopped'] ${'$'}ensure = 'running',
          Hash[String[1], String[1]] ${'$'}vhosts = {},
        ) {
          ${'$'}defaults = {
            'owner' => 'root',
            'group' => 'root',
            'mode'  => '0644',
          }

          file { '/etc/nginx/nginx.conf':
            ensure  => file,
            content => epp('profile/nginx.conf.epp'),
            owner   => ${'$'}defaults['owner'],
            require => Package['nginx'],
          }

          service { 'nginx':
            ensure => ${'$'}ensure,
            enable => ${'$'}ensure ? {
              'running' => true,
              default   => false,
            },
          }
        }
    """.trimIndent()
}
