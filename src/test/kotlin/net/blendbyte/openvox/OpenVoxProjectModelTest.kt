package net.blendbyte.openvox

import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class OpenVoxProjectModelTest : BasePlatformTestCase() {

    fun testEppArgumentResolvesToTemplate() {
        myFixture.addFileToProject("profile/templates/nginx.conf.epp", "server { }\n")
        myFixture.configureByText("site.pp", "\$c = epp('profile/ngi<caret>nx.conf.epp')")
        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve() as? PsiFile
        assertEquals("nginx.conf.epp", target?.name)
    }

    fun testTemplateArgumentResolvesToErb() {
        myFixture.addFileToProject("ntp/templates/ntp.conf.erb", "server <%= @x %>\n")
        myFixture.configureByText("site.pp", "\$c = template('ntp/ntp.co<caret>nf.erb')")
        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve() as? PsiFile
        assertEquals("ntp.conf.erb", target?.name)
    }

    fun testSourceAttributeResolvesToModuleFile() {
        myFixture.addFileToProject("apache/files/httpd.conf", "ServerRoot /etc/httpd\n")
        myFixture.configureByText(
            "site.pp",
            "file { '/etc/httpd.conf':\n  source => 'puppet:///modules/apache/htt<caret>pd.conf',\n}",
        )
        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve() as? PsiFile
        assertEquals("httpd.conf", target?.name)
    }

    fun testHieraGutterIconOnAParameterThatDataSets() {
        myFixture.addFileToProject("data/common.yaml", "profile::agent::ensure: running\n")
        myFixture.configureByText("init.pp", "class profile::agent (String \$ensure = 'running') { }")
        val markers = myFixture.findGuttersAtCaret() + myFixture.findAllGutters()
        assertTrue(
            "no Hiera gutter marker was produced",
            markers.any { it.tooltipText?.contains("profile::agent::ensure") == true },
        )
    }

    fun testHieraIndexFindsSingleQuotedKeysInYml() {
        myFixture.addFileToProject("data/nodes/demo.yml", "'profile::agent::ensure': running\n")
        myFixture.configureByText("init.pp", "class profile::agent (String \$ensure = 'running') { }")
        assertTrue(myFixture.findAllGutters().any { it.tooltipText?.contains("profile::agent::ensure") == true })
    }

    fun testHieraIndexTracksEditedKeys() {
        val data = myFixture.addFileToProject("data/common.yaml", "profile::agent::ensure: running\n")
        myFixture.configureByText("init.pp", "class profile::agent (String \$ensure = 'running') { }")
        assertFalse(myFixture.findAllGutters().isEmpty())
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            val manager = com.intellij.psi.PsiDocumentManager.getInstance(project)
            manager.getDocument(data)!!.setText("profile::other::ensure: running\n")
            manager.commitAllDocuments()
        }
        assertTrue(myFixture.findAllGutters().isEmpty())
    }
}
