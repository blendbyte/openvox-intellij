package net.blendbyte.openvox.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import net.blendbyte.openvox.EppFileType
import net.blendbyte.openvox.EppLanguage
import net.blendbyte.openvox.OpenVoxFileType
import net.blendbyte.openvox.OpenVoxLanguage

class OpenVoxFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, OpenVoxLanguage) {
    override fun getFileType() = OpenVoxFileType
    override fun toString() = "OpenVox manifest"
}

class EppFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, EppLanguage) {
    override fun getFileType() = EppFileType
    override fun toString() = "EPP template"
}
