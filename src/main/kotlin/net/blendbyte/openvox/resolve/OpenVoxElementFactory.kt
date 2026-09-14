package net.blendbyte.openvox.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import net.blendbyte.openvox.OpenVoxFileType
import net.blendbyte.openvox.psi.OpenVoxFile
import net.blendbyte.openvox.psi.OpenVoxTypes as T

object OpenVoxElementFactory {
    fun createFile(project: Project, text: String): OpenVoxFile =
        PsiFileFactory.getInstance(project).createFileFromText("dummy.pp", OpenVoxFileType, text) as OpenVoxFile

    fun renameToken(identifier: PsiElement, name: String): PsiElement {
        val text = when (identifier.node.elementType) {
            T.VARIABLE -> "\$${name.removePrefix("$")}"
            T.SQ_STRING -> "'${name.replace("\\", "\\\\").replace("'", "\\'")}'"
            T.DQ_TEXT -> name.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
            else -> name
        }
        val source = if (identifier.node.elementType == T.DQ_TEXT) "\"$text\"" else text
        val file = createFile(identifier.project, "\$dummy = $source")
        val replacement = generateSequence(PsiTreeUtil.getDeepestFirst(file), PsiTreeUtil::nextLeaf)
            .firstOrNull { it.text == text && it.node.elementType == identifier.node.elementType }
            ?: throw IncorrectOperationException("'$name' is not a valid OpenVox name")
        return identifier.replace(replacement)
    }
}
