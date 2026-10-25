package net.blendbyte.openvox

import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import net.blendbyte.openvox.annotator.OpenVoxLintLayout
import net.blendbyte.openvox.annotator.OpenVoxToolOutput
import net.blendbyte.openvox.completion.OpenVoxDocumentationProvider
import net.blendbyte.openvox.inspections.OpenVoxUnknownAttributeInspection
import net.blendbyte.openvox.settings.OpenVoxSettings
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
            assertEquals("template.epp", OpenVoxLintLayout.of(null, "template.epp").relativePath)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    // Captured from OpenVox 9.2.1 and puppet-lint 4.
    fun testToolOutputParsing() {
        val esc = "\u001B"
        val parser = "$esc[1;31mError: Could not parse for environment production: Syntax error at '}' " +
            "(file: /tmp/openvox-1/manifests/init.pp, line: 10, column: 3)$esc[0m\n"
        val parsed = OpenVoxToolOutput.parseValidate(parser).single()
        assertEquals(10, parsed.line)
        assertEquals(3, parsed.column)
        assertEquals("Syntax error at '}'", parsed.message)

        val epp = """
            Error: Syntax error at '%>' (file: /tmp/a.epp, line: 4, column: 15)
            Error: Syntax error at '
            ' (file: /tmp/b.epp, line: 2, column: 17)
            Error: Errors while validating epp
            Error: Try 'puppet help epp validate' for usage
        """.trimIndent()
        val messages = OpenVoxToolOutput.parseValidate(epp)
        assertEquals(listOf(4 to 15, 2 to 17), messages.map { it.line to it.column })
        assertEquals("Syntax error at '\\n'", messages[1].message)

        val lint = """
            1:7:error:mod::ok not in autoload module layout
            3:12:warning:indentation of => is not properly aligned (expected in column 13, but found it in column 12)
        """.trimIndent()
        val findings = OpenVoxToolOutput.parsePuppetLint(lint)
        assertEquals(listOf(HighlightSeverity.ERROR, HighlightSeverity.WARNING), findings.map { it.severity })
        assertTrue(findings[1].message.endsWith("found it in column 12)"))
    }

    fun testExecutableLookupOnPath() {
        val settings = OpenVoxSettings.getInstance()
        assertTrue(settings.findOnPath("sh")!!.endsWith("sh"))
        assertNull(settings.findOnPath("openvox-no-such-tool"))
    }
}
