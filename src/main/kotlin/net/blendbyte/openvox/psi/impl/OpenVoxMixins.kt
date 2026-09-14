package net.blendbyte.openvox.psi.impl

import com.intellij.lang.ASTNode

abstract class OpenVoxVariableMixin(node: ASTNode) : OpenVoxPsiElement(node) {
    val variableName: String
        get() = text.removePrefix("$").removePrefix("::")
}

abstract class OpenVoxTypeReferenceMixin(node: ASTNode) : OpenVoxPsiElement(node) {
    val typeName: String
        get() = text.removePrefix("::")
}
