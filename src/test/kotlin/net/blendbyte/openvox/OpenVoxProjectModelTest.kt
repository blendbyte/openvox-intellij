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

    fun testTemplateResolvesInModuleRepositoryNamedAfterItsAuthor() {
        myFixture.addFileToProject("puppetlabs-apache/metadata.json", "{\n  \"name\": \"puppetlabs-apache\"\n}\n")
        myFixture.addFileToProject("puppetlabs-apache/templates/vhost.conf.epp", "<VirtualHost>\n")
        myFixture.addFileToProject("other-apache/templates/vhost.conf.epp", "<VirtualHost>\n")
        myFixture.configureByText("site.pp", "\$c = epp('apache/vho<caret>st.conf.epp')")
        val targets = (myFixture.file.findReferenceAt(myFixture.caretOffset) as com.intellij.psi.PsiPolyVariantReference)
            .multiResolve(false).mapNotNull { (it.element as? PsiFile)?.virtualFile?.path }
        assertEquals(1, targets.size)
        assertTrue(targets.single().contains("puppetlabs-apache"))
    }

    fun testHieraGutterSupportsHieradataButNotDefines() {
        myFixture.addFileToProject("hieradata/common.yaml", "profile::agent::ensure: running\nprofile::site::port: 80\n")
        myFixture.configureByText(
            "init.pp",
            "class profile::agent (String \$ensure = 'running') { }\ndefine profile::site (Integer \$port) { }",
        )
        val tooltips = myFixture.findAllGutters().mapNotNull { it.tooltipText }
        assertTrue(tooltips.any { it.contains("profile::agent::ensure") })
        assertFalse(tooltips.any { it.contains("profile::site::port") })
    }
}
