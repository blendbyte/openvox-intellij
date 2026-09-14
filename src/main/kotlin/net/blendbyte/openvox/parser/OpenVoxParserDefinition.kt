package net.blendbyte.openvox.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import net.blendbyte.openvox.EppLanguage
import net.blendbyte.openvox.OpenVoxLanguage
import net.blendbyte.openvox.lexer.EppLexerAdapter
import net.blendbyte.openvox.lexer.OpenVoxLexerAdapter
import net.blendbyte.openvox.psi.EppFile
import net.blendbyte.openvox.psi.OpenVoxFile
import net.blendbyte.openvox.psi.OpenVoxTokenSets
import net.blendbyte.openvox.psi.OpenVoxTypes

class OpenVoxParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = OpenVoxLexerAdapter()
    override fun createParser(project: Project?): PsiParser = OpenVoxParser()
    override fun getFileNodeType() = FILE
    override fun getCommentTokens(): TokenSet = OpenVoxTokenSets.PARSER_SKIPPED
    override fun getStringLiteralElements(): TokenSet = OpenVoxTokenSets.STRINGS
    override fun getWhitespaceTokens(): TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
    override fun createElement(node: ASTNode): PsiElement = OpenVoxTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = OpenVoxFile(viewProvider)

    companion object {
        val FILE = IFileElementType(OpenVoxLanguage)
    }
}

class EppParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = EppLexerAdapter()
    override fun createParser(project: Project?): PsiParser = EppPsiParser()
    override fun getFileNodeType() = FILE
    override fun getCommentTokens(): TokenSet = OpenVoxTokenSets.PARSER_SKIPPED
    override fun getStringLiteralElements(): TokenSet = OpenVoxTokenSets.STRINGS
    override fun getWhitespaceTokens(): TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
    override fun createElement(node: ASTNode): PsiElement = OpenVoxTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = EppFile(viewProvider)

    companion object {
        val FILE = IFileElementType(EppLanguage)
    }
}
