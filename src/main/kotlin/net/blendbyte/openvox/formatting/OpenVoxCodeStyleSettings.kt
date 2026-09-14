package net.blendbyte.openvox.formatting

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

class OpenVoxCodeStyleSettings(container: CodeStyleSettings) :
    CustomCodeStyleSettings("OpenVoxCodeStyleSettings", container) {

    @JvmField
    var ALIGN_RESOURCE_ARROWS: Boolean = true

    @JvmField
    var ALIGN_HASH_ARROWS: Boolean = true

    @JvmField
    var ALIGN_SELECTOR_ARROWS: Boolean = true

    @JvmField
    var SPACE_WITHIN_HASH_BRACES: Boolean = true
}
