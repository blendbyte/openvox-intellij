package net.blendbyte.openvox

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import net.blendbyte.openvox.inspections.OpenVoxUnresolvedVariableInspection
import net.blendbyte.openvox.inspections.OpenVoxUnusedParameterInspection
import net.blendbyte.openvox.psi.OpenVoxNamedElement
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxRefactoringTest : BasePlatformTestCase() {
    fun testRenameClassPreservesQuotedAndAbsoluteReferences() {
        val declaration = myFixture.addFileToProject("demo/manifests/init.pp", "class demo { }")
        myFixture.configureByText("site.pp", "include demo\ninclude('demo')\ncontain(\"demo\")\nrequire ::demo")
        val named = PsiTreeUtil.findChildOfType(declaration, OpenVoxNamedElement::class.java)!!
        assertEquals(4, ReferencesSearch.search(named).findAll().size)
        myFixture.renameElement(named, "renamed")
        assertEquals("include renamed\ninclude('renamed')\ncontain(\"renamed\")\nrequire ::renamed", myFixture.file.text)
    }

    fun testRenameAssignmentRespectsShadowingAndTopScopeQualification() {
        myFixture.configureByText("site.pp", """
            ${'$'}value = 1
            notice(${'$'}value)
            class demo(Integer ${'$'}value = 2) {
              notice(${'$'}value)
              notice(${'$'}::value)
            }
        """.trimIndent())
        val assignment = PsiTreeUtil.findChildrenOfType(myFixture.file, OpenVoxNamedElement::class.java)
            .first { it.node.elementType == T.ASSIGN_EXPR }
        myFixture.renameElement(assignment, "renamed")
        assertEquals("""
            ${'$'}renamed = 1
            notice(${'$'}renamed)
            class demo(Integer ${'$'}value = 2) {
              notice(${'$'}value)
              notice(${'$'}::renamed)
            }
        """.trimIndent(), myFixture.file.text)
    }

    fun testRenameParameterUpdatesBothInterpolationForms() {
        myFixture.configureByText("site.pp", "function demo(String \$greeting) { \"\$greeting \${greeting}\" }")
        val parameter = PsiTreeUtil.findChildrenOfType(myFixture.file, OpenVoxNamedElement::class.java)
            .first { it.node.elementType == T.PARAMETER }
        myFixture.renameElement(parameter, "salutation")
        assertEquals("function demo(String \$salutation) { \"\$salutation \${salutation}\" }", myFixture.file.text)
    }

    fun testNestedFunctionArgumentsAreNotClassReferences() {
        myFixture.addFileToProject("demo/manifests/init.pp", "class demo { }")
        myFixture.configureByText("site.pp", "include sprintf('de<caret>mo')")
        assertNull(myFixture.file.findReferenceAt(myFixture.caretOffset))
    }

    fun testTopScopeAndBracedInterpolationHaveNoFalseWarnings() {
        myFixture.enableInspections(OpenVoxUnresolvedVariableInspection(), OpenVoxUnusedParameterInspection())
        myFixture.configureByText("site.pp", "\$global = 'hello'\nnotice(\$global)\nfunction demo(String \$greeting) { \"\${greeting}\" }")
        myFixture.checkHighlighting()
    }

    fun testParameterInEppResolves() {
        myFixture.configureByText("template.epp", "<% | String \$greeting | %>Hello <%= \$gree<caret>ting %>")
        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve() as? PsiNamedElement
        assertEquals("greeting", target?.name)
    }

    fun testScopeCacheTracksEditedDeclarations() {
        myFixture.configureByText("site.pp", "class demo { \$local = 1\nnotice(\$lo<caret>cal) }")
        val offset = myFixture.caretOffset
        assertNotNull(myFixture.file.findReferenceAt(offset)?.resolve())
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            val document = myFixture.editor.document
            val start = document.text.indexOf("local")
            document.replaceString(start, start + 5, "other")
            com.intellij.psi.PsiDocumentManager.getInstance(project).commitAllDocuments()
        }
        assertNull(myFixture.file.findReferenceAt(offset)?.resolve())
    }
}
