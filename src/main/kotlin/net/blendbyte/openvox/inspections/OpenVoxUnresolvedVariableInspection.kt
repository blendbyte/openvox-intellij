package net.blendbyte.openvox.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import net.blendbyte.openvox.psi.EppFile
import net.blendbyte.openvox.psi.OpenVoxEppParameters
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
                if (name.all(Char::isDigit)) return // regex match captures: $0, $1, ...
                if (OpenVoxInspectionSupport.isTopScopeVariable(name)) return
                if (isAssignmentTarget(element)) return
                if (isUntypedTemplate(element.containingFile)) return
                if (OpenVoxScope.resolve(element, name) != null) return

                holder.registerProblem(
                    element,
                    "Variable '${element.text}' is not declared in this scope",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                )
            }
        }

    /** An EPP template without a parameter tag sees every key of the hash passed to epp(). */
    private fun isUntypedTemplate(file: PsiFile): Boolean =
        file is EppFile && CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(
                PsiTreeUtil.findChildOfType(file, OpenVoxEppParameters::class.java) == null,
                file,
            )
        }

    private fun isAssignmentTarget(element: PsiElement): Boolean {
        val parent = element.parent ?: return false
        return parent.node?.elementType == T.ASSIGN_EXPR && parent.firstChild === element
    }
}
