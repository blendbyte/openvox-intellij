package net.blendbyte.openvox

import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import net.blendbyte.openvox.annotator.OpenVoxLintLayout
import net.blendbyte.openvox.completion.OpenVoxDocumentationProvider
import net.blendbyte.openvox.inspections.OpenVoxUnknownAttributeInspection
import net.blendbyte.openvox.psi.OpenVoxNamedElement
import net.blendbyte.openvox.structure.OpenVoxStructureViewFactory
import java.nio.file.Files

class OpenVoxEditorFeaturesTest : BasePlatformTestCase() {

    private fun completions(text: String): List<String> {
        myFixture.configureByText("site.pp", text)
        myFixture.completeBasic()
        return myFixture.lookupElementStrings.orEmpty()
    }

    fun testClassResourceTitleResolvesToClass() {
        myFixture.addFileToProject("apache/manifests/init.pp", "class apache (Integer \$port = 80) { }")
        myFixture.configureByText("site.pp", "class { 'apa<caret>che': port => 80 }\n")
        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve() as? PsiNamedElement
        assertEquals("apache", target?.name)
    }

    fun testClassResourceAttributesAreChecked() {
        myFixture.addFileToProject("apache/manifests/init.pp", "class apache (Integer \$port = 80) { }")
        myFixture.enableInspections(OpenVoxUnknownAttributeInspection())
        myFixture.configureByText(
            "site.pp",
            "class { 'apache':\n  port => 80,\n  require => Package['x'],\n" +
                "  <warning descr=\"'apache' has no attribute 'prot'\">prot</warning> => 1,\n}\n",
        )
        myFixture.checkHighlighting()
    }

    fun testDefineAndClassParametersAreCompleted() {
        myFixture.addFileToProject("apache/manifests/vhost.pp", "define apache::vhost (Integer \$port, String \$docroot) { }")
        myFixture.addFileToProject("ntp/manifests/init.pp", "class ntp (Array \$servers = []) { }")
        assertContainsElements(completions("apache::vhost { 'x':\n  <caret>\n}"), "port", "docroot", "require")
        assertContainsElements(completions("class { 'ntp':\n  <caret>\n}"), "servers", "before")
    }

    fun testResourceTypesAndKeywordsAtStatementStart() {
        // A single match is inserted directly, so check the document as well as the lookup.
        for ((prefix, expected) in listOf("pack" to "package", "unl" to "unless")) {
            val items = completions("class demo {\n  $prefix<caret>\n}")
            assertTrue("$expected was not offered", expected in items || myFixture.file.text.contains(expected))
        }
    }

    fun testStructureViewShowsTitlesAndNestedResources() {
        val file = myFixture.configureByText(
            "site.pp",
            "class demo {\n  file { '/etc/motd': ensure => file }\n  if \$x {\n    service { 'nginx': }\n  }\n}\n",
        )
        val builder = OpenVoxStructureViewFactory().getStructureViewBuilder(file) as TreeBasedStructureViewBuilder
        val model = builder.createStructureViewModel(null)
        try {
            val demo = model.root.children.single()
            assertEquals("demo", demo.presentation.presentableText)
            val names = demo.children.map { it.presentation.presentableText }
            assertEquals(listOf("file '/etc/motd'", "service 'nginx'"), names)
        } finally {
            Disposer.dispose(model)
        }
    }

    fun testQuickDocRendersPuppetStrings() {
        val file = myFixture.configureByText(
            "init.pp",
            """
            # @summary Manages the web server.
            #
            # Installs and configures nginx.
            #
            # @param port
            #   The port to listen on.
            # @example Basic use
            #   include demo
            class demo (Integer ${'$'}port = 80) { }
            """.trimIndent(),
        )
        val declaration = PsiTreeUtil.findChildOfType(file, OpenVoxNamedElement::class.java)!!
        val doc = OpenVoxDocumentationProvider().generateDoc(declaration, null)!!
        assertTrue(doc, doc.contains("<p>Manages the web server.</p>"))
        assertTrue(doc, doc.contains("<p>Installs and configures nginx.</p>"))
        assertTrue(doc, doc.contains("<code>port</code> &ndash; The port to listen on."))
        assertTrue(doc, doc.contains("<pre><code>include demo</code></pre>"))
    }

    fun testLintLayoutMirrorsModulePath() {
        val root = Files.createTempDirectory("openvox-layout")
        try {
            val manifest = root.resolve("site/profile/manifests/web/nginx.pp")
            Files.createDirectories(manifest.parent)
            Files.writeString(root.resolve(".puppet-lint.rc"), "--no-140chars-check\n")
            val layout = OpenVoxLintLayout.of(manifest)
            assertEquals("manifests/web/nginx.pp", layout.relativePath)
            assertEquals(root, layout.workDirectory)

            val loose = root.resolve("scratch/test.pp")
            assertEquals("test.pp", OpenVoxLintLayout.of(loose).relativePath)
            assertEquals("manifest.pp", OpenVoxLintLayout.of(null).relativePath)
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
