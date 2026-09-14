package net.blendbyte.openvox.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.blendbyte.openvox.resolve.OpenVoxScope
import net.blendbyte.openvox.resolve.OpenVoxReferenceContext
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxUnresolvedVariableInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.node?.elementType != T.VARIABLE_EXPR && !OpenVoxReferenceContext.isInterpolationVariable(element)) return
                val name = OpenVoxScope.normalise(element.text)
                if ("::" in name) return
                if (name in OpenVoxInspectionSupport.BUILT_IN_VARIABLES) return
                if (name in OpenVoxInspectionSupport.LEGACY_FACTS) return
                if (isAssignmentTarget(element)) return
                if (OpenVoxScope.resolve(element, name) != null) return

                holder.registerProblem(
                    element,
                    "Variable '${element.text}' is not declared in this scope",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                )
            }
        }

    private fun isAssignmentTarget(element: PsiElement): Boolean {
        val parent = element.parent ?: return false
        return parent.node?.elementType == T.ASSIGN_EXPR && parent.firstChild === element
    }
}
