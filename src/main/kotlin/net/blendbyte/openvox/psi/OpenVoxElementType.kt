package net.blendbyte.openvox.psi

import com.intellij.psi.tree.IElementType
import net.blendbyte.openvox.OpenVoxLanguage

class OpenVoxElementType(debugName: String) : IElementType(debugName, OpenVoxLanguage)

class OpenVoxTokenType(debugName: String) : IElementType(debugName, OpenVoxLanguage) {
    override fun toString() = "OpenVoxTokenType." + super.toString()
}
