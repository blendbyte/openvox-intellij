package net.blendbyte.openvox.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.blendbyte.openvox.resolve.OpenVoxReferenceContext
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxUnknownAttributeInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.node?.elementType != T.ATTRIBUTE_OPERATION) return
                val nameElement = element.firstChild ?: return
                val name = nameElement.text
                if (name == "*") return

                val resource = generateSequence(element) { it.parent }.firstOrNull {
                    it.node?.elementType == T.RESOURCE_EXPRESSION ||
                        it.node?.elementType == T.RESOURCE_DEFAULTS_EXPRESSION
                } ?: return
                val typeElement = resource.node?.findChildByType(T.RESOURCE_TYPE)
                    ?: resource.node?.findChildByType(T.TYPE_REFERENCE)
                    ?: return

                val classResource = typeElement.findChildByType(T.CLASS) != null
                val owner = if (classResource) OpenVoxReferenceContext.classResourceName(element) ?: return else typeElement.text
                val accepted = if (classResource) {
                    OpenVoxInspectionSupport.classAttributeNames(element, owner)
                } else {
                    OpenVoxInspectionSupport.attributeNamesFor(element, owner)
                } ?: return
                if (name in accepted) return

                holder.registerProblem(
                    nameElement,
                    "'$owner' has no attribute '$name'",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                )
            }
        }
}
