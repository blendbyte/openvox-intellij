package net.blendbyte.openvox

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class OpenVoxFormatterTest : BasePlatformTestCase() {

    private fun reformat(before: String): String {
        myFixture.configureByText("test.pp", before)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        return myFixture.file.text
    }

    fun testResourceArrowsAlign() {
        val result = reformat(
            """
            class demo {
              file { '/etc/motd':
              ensure => file,
              owner => 'root',
              content => 'hello',
              }
            }
            """.trimIndent(),
        )
        assertEquals(
            """
            class demo {
              file { '/etc/motd':
                ensure  => file,
                owner   => 'root',
                content => 'hello',
              }
            }
            """.trimIndent(),
            result,
        )
    }

    fun testHashArrowsAlign() {
        val result = reformat(
            """
            ${'$'}defaults = {
            'owner' => 'root',
            'mode' => '0644',
            }
            """.trimIndent(),
        )
        assertEquals(
            """
            ${'$'}defaults = {
              'owner' => 'root',
              'mode'  => '0644',
            }
            """.trimIndent(),
            result,
        )
    }

    fun testOperatorSpacingAndIndent() {
        val result = reformat(
            """
            class demo(Integer ${'$'}count=1) {
              if ${'$'}count>0 {
              notify{'x':}
              }
            }
            """.trimIndent(),
        )
        assertEquals(
            """
            class demo (Integer ${'$'}count = 1) {
              if ${'$'}count > 0 {
                notify { 'x': }
              }
            }
            """.trimIndent(),
            result,
        )
    }
}
