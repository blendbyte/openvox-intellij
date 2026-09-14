package net.blendbyte.openvox.highlighting

import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import net.blendbyte.openvox.OpenVoxIcons

class OpenVoxColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = OpenVoxIcons.FILE
    override fun getHighlighter() = OpenVoxSyntaxHighlighter()
    override fun getAdditionalHighlightingTagToDescriptorMap() = null
    override fun getAttributeDescriptors() = DESCRIPTORS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName() = "OpenVox / Puppet"

    override fun getDemoText() = """
        # Manage the OpenVox agent itself.
        class profile::openvox::agent (
          Enum['running', 'stopped'] ${'$'}ensure  = 'running',
          Optional[Sensitive[String[1]]] ${'$'}token = undef,
          Hash[String[1], Variant[String, Integer]] ${'$'}settings = {},
        ) {
          ${'$'}conf = "/etc/puppetlabs/puppet/puppet.conf"

          file { ${'$'}conf:
            ensure  => file,
            owner   => 'root',
            mode    => '0644',
            content => epp('profile/puppet.conf.epp', { 'settings' => ${'$'}settings }),
          }

          service { 'puppet':
            ensure => ${'$'}ensure,
            enable => ${'$'}ensure ? { 'running' => true, default => false },
          }

          File[${'$'}conf] ~> Service['puppet']
        }
    """.trimIndent()

    private companion object {
        val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Keyword", OpenVoxColors.KEYWORD),
            AttributesDescriptor("Comment", OpenVoxColors.COMMENT),
            AttributesDescriptor("String", OpenVoxColors.STRING),
            AttributesDescriptor("String//Escape sequence", OpenVoxColors.ESCAPE),
            AttributesDescriptor("String//Interpolation", OpenVoxColors.INTERPOLATION),
            AttributesDescriptor("Number", OpenVoxColors.NUMBER),
            AttributesDescriptor("Variable", OpenVoxColors.VARIABLE),
            AttributesDescriptor("Data type or resource type", OpenVoxColors.DATA_TYPE),
            AttributesDescriptor("Identifier", OpenVoxColors.IDENTIFIER),
            AttributesDescriptor("Operator", OpenVoxColors.OPERATOR),
            AttributesDescriptor("Regular expression", OpenVoxColors.REGEX),
            AttributesDescriptor("Braces", OpenVoxColors.BRACES),
            AttributesDescriptor("Brackets", OpenVoxColors.BRACKETS),
            AttributesDescriptor("Parentheses", OpenVoxColors.PARENTHESES),
            AttributesDescriptor("Comma and semicolon", OpenVoxColors.COMMA_SEMI),
            AttributesDescriptor("EPP//Tag", OpenVoxColors.EPP_TAG),
            AttributesDescriptor("EPP//Template text", OpenVoxColors.EPP_TEXT),
        )
    }
}
