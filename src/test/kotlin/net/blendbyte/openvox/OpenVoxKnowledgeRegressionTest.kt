package net.blendbyte.openvox

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import net.blendbyte.openvox.inspections.OpenVoxUnknownAttributeInspection
import net.blendbyte.openvox.knowledge.OpenVoxKnowledge

class OpenVoxKnowledgeRegressionTest : BasePlatformTestCase() {
    fun testRegsubstDefaultsToSharedSignatureAndDocumentsOpenvox8Overload() {
        val function = OpenVoxKnowledge.functions.getValue("regsubst")
        assertFalse(function.presentableSignature().contains("\$encoding"))
        assertTrue(function.signatures.any { it.contains("\$encoding") })
        assertTrue(function.doc.contains("OpenVox 9 removes it"))
    }

    fun testFileContentDocumentsVersionSpecificChecksumBehavior() {
        assertTrue(OpenVoxKnowledge.attribute("file", "content")!!.doc.contains("OpenVox 9 writes checksum-looking strings literally"))
    }

    fun testStandardExecGuardsAreAccepted() {
        myFixture.enableInspections(OpenVoxUnknownAttributeInspection())
        myFixture.configureByText("site.pp", "exec { '/bin/true': creates => '/tmp/done', onlyif => '/bin/true', unless => '/bin/false', refreshonly => true }")
        myFixture.checkHighlighting()
    }

    fun testServiceHasrestartIsAccepted() {
        myFixture.enableInspections(OpenVoxUnknownAttributeInspection())
        myFixture.configureByText("site.pp", "service { 'nginx': hasrestart => true }")
        myFixture.checkHighlighting()
    }

    fun testPackageProviderIsAccepted() {
        myFixture.enableInspections(OpenVoxUnknownAttributeInspection())
        myFixture.configureByText("site.pp", "package { 'foo': provider => gem }")
        myFixture.checkHighlighting()
    }

    fun testFactCompletionOffersRootKeys() {
        myFixture.configureByText("site.pp", "\$x = \$facts['<caret>']")
        myFixture.completeBasic()
        assertContainsElements(myFixture.lookupElementStrings.orEmpty(), "os", "networking", "memory")
    }


    fun testNestedFactCompletionOffersImmediateKeys() {
        myFixture.configureByText("site.pp", "\$x = \$facts['os']['<caret>']")
        myFixture.completeBasic()
        val items = myFixture.lookupElementStrings.orEmpty()
        assertContainsElements(items, "name", "family", "release", "distro")
        assertFalse(items.contains("networking"))
        assertFalse(items.contains("release.full"))
    }

    fun testNestedDoubleQuotedFactCompletion() {
        myFixture.configureByText("site.pp", "\$x = \$facts[\"os\"][\"release\"]['<caret>']")
        myFixture.completeBasic()
        assertContainsElements(myFixture.lookupElementStrings.orEmpty(), "full", "major", "minor")
    }

    fun testTrustedCompletionUsesTrustedKeys() {
        myFixture.configureByText("site.pp", "\$x = \$trusted['<caret>']")
        myFixture.completeBasic()
        val items = myFixture.lookupElementStrings.orEmpty()
        assertContainsElements(items, "certname", "authenticated", "extensions")
        assertFalse(items.contains("os"))
    }
}
