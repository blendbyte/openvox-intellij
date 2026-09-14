package net.blendbyte.openvox.editor

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.psi.tree.TokenSet
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxQuoteHandler : SimpleTokenSetQuoteHandler(
    TokenSet.create(T.SQ_STRING, T.DQ_START, T.DQ_END, T.DQ_TEXT),
)
