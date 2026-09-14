package net.blendbyte.openvox

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import net.blendbyte.openvox.lexer.OpenVoxLexerAdapter
import net.blendbyte.openvox.psi.OpenVoxNamedElement
import net.blendbyte.openvox.psi.OpenVoxTypes as T
import net.blendbyte.openvox.structure.OpenVoxStructureViewFactory
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder

class OpenVoxRegressionTest : BasePlatformTestCase() {
    fun testTopScopeAssignmentResolves() {
        myFixture.configureByText("site.pp", "\$greeting = 'hello'\nnotice(\$gree<caret>ting)")
        assertNotNull("top-scope assignment should resolve", myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve())
    }

    fun testLambdaAssignmentDoesNotLeak() {
        myFixture.configureByText("site.pp", "class demo { [1].each |\$i| { \$hidden = \$i }\nnotice(\$hid<caret>den) }")
        assertNull("lambda-local variable must not resolve outside lambda", myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve())
    }

    fun testRenameDefineUpdatesResources() {
        val declaration = myFixture.addFileToProject("demo/manifests/item.pp", "define demo::item { }")
        myFixture.configureByText("site.pp", "demo::it<caret>em { 'x': }\nDemo::Item['x']")
        val named = PsiTreeUtil.findChildOfType(declaration, OpenVoxNamedElement::class.java)!!
        myFixture.renameElement(named, "demo::renamed")
        assertEquals("demo::renamed { 'x': }\nDemo::Renamed['x']", myFixture.file.text)
    }

    fun testRenameParameterUpdatesUsage() {
        myFixture.configureByText("site.pp", "class demo(String \$greeting) { notice(\$greeting) }")
        val parameter = PsiTreeUtil.findChildrenOfType(myFixture.file, OpenVoxNamedElement::class.java)
            .first { it.node.elementType == T.PARAMETER }
        myFixture.renameElement(parameter, "\$salutation")
        assertEquals("class demo(String \$salutation) { notice(\$salutation) }", myFixture.file.text)
    }

    fun testQuotedIncludeResolves() {
        myFixture.addFileToProject("demo/manifests/init.pp", "class demo { }")
        myFixture.configureByText("site.pp", "include('de<caret>mo')")
        assertNotNull("quoted class name should resolve", myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve())
    }

    fun testStructureViewHasDeclarations() {
        myFixture.configureByText("site.pp", "class demo { }")
        val builder = OpenVoxStructureViewFactory().getStructureViewBuilder(myFixture.file) as TreeBasedStructureViewBuilder
        val model = builder.createStructureViewModel(myFixture.editor)
        try {
            assertTrue("structure root should contain class demo", model.root.children.isNotEmpty())
        } finally {
            com.intellij.openapi.util.Disposer.dispose(model)
        }
    }

    fun testIncrementalLexerPreservesInterpolation() {
        val text = "\$x = \"hello \${facts['os']} world\"\n\$y = 1"
        val lexer = OpenVoxLexerAdapter()
        lexer.start(text)
        while (lexer.tokenType != null && lexer.tokenText != "facts") lexer.advance()
        assertNotNull(lexer.tokenType)
        val offset = lexer.tokenStart
        val state = lexer.state
        val expected = mutableListOf<String>()
        while (lexer.tokenType != null) { expected += "${lexer.tokenType}:${lexer.tokenText}"; lexer.advance() }
        val restarted = OpenVoxLexerAdapter()
        restarted.start(text, offset, text.length, state)
        val actual = mutableListOf<String>()
        while (restarted.tokenType != null) { actual += "${restarted.tokenType}:${restarted.tokenText}"; restarted.advance() }
        assertEquals("restart inside ordinary string interpolation must preserve tokens", expected, actual)
    }

    fun testRenameTypeAliasUpdatesUsage() {
        val declaration = myFixture.addFileToProject("profile/types/port.pp", "type Profile::Port = Integer[1, 65535]")
        myFixture.configureByText("site.pp", "class demo(Profile::Port \$port) { }")
        val named = PsiTreeUtil.findChildOfType(declaration, OpenVoxNamedElement::class.java)!!
        myFixture.renameElement(named, "Profile::ListenPort")
        assertEquals("class demo(Profile::ListenPort \$port) { }", myFixture.file.text)
    }
}
