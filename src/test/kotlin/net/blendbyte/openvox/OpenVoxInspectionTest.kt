package net.blendbyte.openvox

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import net.blendbyte.openvox.inspections.OpenVoxLegacyFactInspection
import net.blendbyte.openvox.inspections.OpenVoxUnknownAttributeInspection
import net.blendbyte.openvox.inspections.OpenVoxUnknownFunctionInspection
import net.blendbyte.openvox.inspections.OpenVoxUnresolvedVariableInspection
import net.blendbyte.openvox.inspections.OpenVoxUnusedParameterInspection

class OpenVoxInspectionTest : BasePlatformTestCase() {

    fun testUnknownFunctionIsReported() {
        myFixture.enableInspections(OpenVoxUnknownFunctionInspection())
        myFixture.configureByText(
            "site.pp",
            "\$a = <warning descr=\"Unknown function 'no_such_function'\">no_such_function</warning>('x')\n",
        )
        myFixture.checkHighlighting()
    }

    fun testBuiltInFunctionIsAccepted() {
        myFixture.enableInspections(OpenVoxUnknownFunctionInspection())
        myFixture.configureByText("site.pp", "\$a = split('a:b', ':')\n")
        myFixture.checkHighlighting()
    }

    fun testProjectFunctionIsAccepted() {
        myFixture.addFileToProject("profile/manifests/fn.pp", "function profile::render(String \$t) >> String { \$t }")
        myFixture.enableInspections(OpenVoxUnknownFunctionInspection())
        myFixture.configureByText("site.pp", "\$a = profile::render('x')\n")
        myFixture.checkHighlighting()
    }

    fun testRubyModuleFunctionIsAccepted() {
        myFixture.addFileToProject(
            "stdlib/lib/puppet/functions/stdlib/extname.rb",
            "Puppet::Functions.create_function(:'stdlib::extname') do\nend\n",
        )
        myFixture.enableInspections(OpenVoxUnknownFunctionInspection())
        myFixture.configureByText("site.pp", "\$a = stdlib::extname('x.txt')\n")
        myFixture.checkHighlighting()
    }

    fun testUnknownAttributeIsReported() {
        myFixture.enableInspections(OpenVoxUnknownAttributeInspection())
        myFixture.configureByText(
            "site.pp",
            "file { '/etc/motd':\n  <warning descr=\"'file' has no attribute 'contnt'\">contnt</warning> => 'x',\n}\n",
        )
        myFixture.checkHighlighting()
    }

    fun testKnownAttributesAndMetaparametersAreAccepted() {
        myFixture.enableInspections(OpenVoxUnknownAttributeInspection())
        myFixture.configureByText(
            "site.pp",
            "file { '/etc/motd':\n  ensure  => file,\n  content => 'x',\n  require => Package['x'],\n}\n",
        )
        myFixture.checkHighlighting()
    }

    fun testDefineParametersAreItsAttributes() {
        myFixture.addFileToProject("apache/manifests/vhost.pp", "define apache::vhost (Integer \$port = 80) { }")
        myFixture.enableInspections(OpenVoxUnknownAttributeInspection())
        myFixture.configureByText(
            "site.pp",
            "apache::vhost { 'x':\n  <warning descr=\"'apache::vhost' has no attribute 'porrt'\">porrt</warning> => 80,\n}\n",
        )
        myFixture.checkHighlighting()
    }

    fun testUnresolvedVariableIsReported() {
        myFixture.enableInspections(OpenVoxUnresolvedVariableInspection())
        myFixture.configureByText(
            "site.pp",
            "class demo {\n  notify { 'x': message => <warning descr=\"Variable '\$missing' is not declared in this scope\">\$missing</warning> }\n}\n",
        )
        myFixture.checkHighlighting()
    }

    fun testQualifiedAndBuiltInVariablesAreAccepted() {
        myFixture.enableInspections(OpenVoxUnresolvedVariableInspection())
        myFixture.configureByText(
            "site.pp",
            "class demo (String \$greeting = 'hi') {\n  \$local = \$apache::port\n  notify { 'x': message => \"\$greeting \$local \${facts['os']['family']} \$title\" }\n}\n",
        )
        myFixture.checkHighlighting()
    }

    fun testLegacyFactIsReportedAndFixed() {
        myFixture.enableInspections(OpenVoxLegacyFactInspection())
        myFixture.configureByText("site.pp", "\$x = \$::osfamily\n")
        val fix = myFixture.getAllQuickFixes().firstOrNull { it.text.contains("facts['os']['family']") }
        assertNotNull("quick fix was not offered", fix)
        myFixture.launchAction(fix!!)
        assertEquals("\$x = \$facts['os']['family']\n", myFixture.file.text)
    }

    fun testUnusedParameterIsReported() {
        myFixture.enableInspections(OpenVoxUnusedParameterInspection())
        myFixture.configureByText(
            "site.pp",
            "define demo (Integer <warning descr=\"Parameter '\$unused' is never used\">\$unused</warning>, String \$used) {\n  notify { \$used: }\n}\n",
        )
        myFixture.checkHighlighting()
    }
}
