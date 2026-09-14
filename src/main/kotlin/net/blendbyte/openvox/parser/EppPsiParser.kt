package net.blendbyte.openvox.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.tree.IElementType

class EppPsiParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val b = GeneratedParserUtilBase.adapt_builder_(root, builder, OpenVoxParser(), OpenVoxParser.EXTENDS_SETS_)
        val marker = GeneratedParserUtilBase.enter_section_(b, 0, GeneratedParserUtilBase._COLLAPSE_, null)
        val result = OpenVoxParser.epp_template(b, 0)
        GeneratedParserUtilBase.exit_section_(b, 0, marker, root, result, true, GeneratedParserUtilBase.TRUE_CONDITION)
        return b.treeBuilt
    }
}
