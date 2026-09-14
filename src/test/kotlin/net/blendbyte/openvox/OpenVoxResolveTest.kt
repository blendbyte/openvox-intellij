package net.blendbyte.openvox

import com.intellij.psi.PsiNamedElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import net.blendbyte.openvox.index.OpenVoxDeclarationKind
import net.blendbyte.openvox.resolve.OpenVoxResolver

class OpenVoxResolveTest : BasePlatformTestCase() {

    private fun resolveAtCaret(): PsiNamedElement? {
        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
        return reference?.resolve() as? PsiNamedElement
    }

    fun testIncludeResolvesToClass() {
        myFixture.addFileToProject("apache/manifests/init.pp", "class apache { }")
        myFixture.configureByText("site.pp", "include apa<caret>che")
        assertEquals("apache", resolveAtCaret()?.name)
    }

    fun testQualifiedIncludeResolves() {
        myFixture.addFileToProject("apache/manifests/mod/ssl.pp", "class apache::mod::ssl { }")
        myFixture.configureByText("site.pp", "include apache::mod::s<caret>sl")
        assertEquals("apache::mod::ssl", resolveAtCaret()?.name)
    }

    fun testResourceTypeResolvesToDefine() {
        myFixture.addFileToProject("apache/manifests/vhost.pp", "define apache::vhost (String \$port) { }")
        myFixture.configureByText("site.pp", "apache::vh<caret>ost { 'example': port => '80' }")
        assertEquals("apache::vhost", resolveAtCaret()?.name)
    }

    fun testCapitalisedTypeReferenceResolvesToDefine() {
        myFixture.addFileToProject("apache/manifests/vhost.pp", "define apache::vhost { }")
        myFixture.configureByText("site.pp", "Apache::Vh<caret>ost['example'] -> Service['apache']")
        assertEquals("apache::vhost", resolveAtCaret()?.name)
    }

    fun testTypeAliasResolves() {
        myFixture.addFileToProject("profile/manifests/types.pp", "type Profile::Port = Integer[1, 65535]")
        myFixture.configureByText("site.pp", "class x (Profile::P<caret>ort \$p = 80) { }")
        assertNotNull(resolveAtCaret())
    }

    fun testFunctionCallResolves() {
        myFixture.addFileToProject("profile/manifests/fn.pp", "function profile::render(String \$t) >> String { \$t }")
        myFixture.configureByText("site.pp", "\$x = profile::ren<caret>der('a')")
        assertEquals("profile::render", resolveAtCaret()?.name)
    }

    fun testVariableResolvesToParameter() {
        myFixture.configureByText(
            "site.pp",
            """
            class demo (String ${'$'}greeting = 'hi') {
              notify { 'x': message => ${'$'}gree<caret>ting }
            }
            """.trimIndent(),
        )
        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve()
        assertNotNull("variable did not resolve", target)
        assertEquals("greeting", (target as PsiNamedElement).name)
    }

    fun testVariableResolvesToEarlierAssignment() {
        myFixture.configureByText(
            "site.pp",
            """
            class demo {
              ${'$'}conf = '/etc/demo.conf'
              file { ${'$'}co<caret>nf: ensure => file }
            }
            """.trimIndent(),
        )
        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve()
        assertNotNull("variable did not resolve", target)
        assertEquals("conf", (target as PsiNamedElement).name)
    }

    fun testIndexListsDeclaredNames() {
        myFixture.addFileToProject("apache/manifests/init.pp", "class apache { }")
        myFixture.addFileToProject("apache/manifests/vhost.pp", "define apache::vhost { }")
        val classes = OpenVoxResolver.allNames(project, OpenVoxDeclarationKind.CLASS)
        val defines = OpenVoxResolver.allNames(project, OpenVoxDeclarationKind.DEFINE)
        assertContainsElements(classes, "apache")
        assertContainsElements(defines, "apache::vhost")
    }
}
