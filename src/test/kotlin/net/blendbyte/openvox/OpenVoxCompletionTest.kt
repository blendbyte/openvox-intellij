package net.blendbyte.openvox

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class OpenVoxCompletionTest : BasePlatformTestCase() {

    private var autoInserted: String? = null

    private fun completions(text: String): List<String> {
        myFixture.configureByText("site.pp", text)
        myFixture.completeBasic()
        val items = myFixture.lookupElementStrings
        autoInserted = if (items == null) myFixture.file.text else null
        return items.orEmpty()
    }

    private fun assertOffers(items: List<String>, vararg expected: String) {
        val inserted = autoInserted
        if (inserted != null) {
            expected.forEach { assertTrue("$it was not offered or inserted", inserted.contains(it)) }
        } else {
            assertContainsElements(items, *expected)
        }
    }

    fun testResourceAttributeNames() {
        val items = completions("file { '/etc/motd':\n  <caret>\n}")
        assertOffers(items, "ensure", "owner", "group", "mode", "content", "require", "notify")
    }

    fun testAttributeValuesForEnsure() {
        val items = completions("file { '/etc/motd':\n  ensure => <caret>\n}")
        assertOffers(items, "file", "directory", "absent", "link")
    }

    fun testServiceEnsureValues() {
        val items = completions("service { 'nginx':\n  ensure => <caret>\n}")
        assertOffers(items, "running", "stopped")
    }

    fun testDataTypes() {
        val items = completions("class demo (<caret>) { }")
        assertOffers(items, "Optional", "Variant", "Sensitive", "Struct", "Enum")
    }

    fun testBuiltInFunctions() {
        val items = completions("\$x = spl<caret>")
        assertOffers(items, "split")
    }

    fun testProjectClassesAreOffered() {
        myFixture.addFileToProject("apache/manifests/init.pp", "class apache { }")
        val items = completions("include apa<caret>")
        assertOffers(items, "apache")
    }

    fun testTypeAliasesOfferedWhereATypeIsExpected() {
        myFixture.addFileToProject("profile/manifests/types.pp", "type Profile::Port = Integer[1, 65535]")
        val items = completions("class demo (Prof<caret>) { }")
        assertOffers(items, "Profile::Port")
    }

    fun testVariablesInScope() {
        val items = completions(
            """
            class demo (String ${'$'}greeting = 'hi') {
              ${'$'}target = '/etc/motd'
              notify { 'x': message => ${'$'}<caret> }
            }
            """.trimIndent(),
        )
        assertOffers(items, "\$greeting", "\$target")
    }
}
