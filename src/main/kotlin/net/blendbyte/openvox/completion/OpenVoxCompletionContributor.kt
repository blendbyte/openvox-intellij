package net.blendbyte.openvox.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import net.blendbyte.openvox.OpenVoxIcons
import net.blendbyte.openvox.OpenVoxLanguage
import net.blendbyte.openvox.index.OpenVoxDeclarationKind
import net.blendbyte.openvox.knowledge.OpenVoxKnowledge
import net.blendbyte.openvox.resolve.OpenVoxResolver
import net.blendbyte.openvox.resolve.OpenVoxScope

class OpenVoxCompletionContributor : CompletionContributor() {

    override fun beforeCompletion(context: com.intellij.codeInsight.completion.CompletionInitializationContext) {
        context.dummyIdentifier = "openvoxdummy"
    }

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(OpenVoxLanguage),
            OpenVoxCompletionProvider,
        )
    }
}

private object OpenVoxCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val project = position.project

        when (val place = OpenVoxCompletionContext.placeAt(position)) {
            is OpenVoxCompletionPlace.AttributeName -> {
                OpenVoxKnowledge.attributesFor(place.resourceType).forEach { attribute ->
                    result.addElement(
                        LookupElementBuilder.create(attribute.name)
                            .withTypeText(attribute.kind)
                            .withIcon(OpenVoxIcons.FILE)
                            .withInsertHandler(ArrowInsertHandler),
                    )
                }
            }

            is OpenVoxCompletionPlace.AttributeValue -> {
                val attribute = OpenVoxKnowledge.attribute(place.resourceType, place.attributeName)
                attribute?.values?.forEach { value ->
                    result.addElement(LookupElementBuilder.create(value).withTypeText("value"))
                }
                addVariables(parameters, result)
            }

            OpenVoxCompletionPlace.DataType -> {
                OpenVoxKnowledge.dataTypes.values.forEach { type ->
                    result.addElement(
                        LookupElementBuilder.create(type.name)
                            .withTypeText("data type")
                            .withIcon(OpenVoxIcons.FILE),
                    )
                }
                OpenVoxResolver.allNames(project, OpenVoxDeclarationKind.TYPE_ALIAS).forEach { name ->
                    result.addElement(LookupElementBuilder.create(capitalise(name)).withTypeText("type alias"))
                }
                OpenVoxResolver.allNames(project, OpenVoxDeclarationKind.DEFINE).forEach { name ->
                    result.addElement(LookupElementBuilder.create(capitalise(name)).withTypeText("defined type"))
                }
            }

            OpenVoxCompletionPlace.ResourceType -> {
                OpenVoxKnowledge.resourceTypes.values.forEach { type ->
                    result.addElement(
                        LookupElementBuilder.create(type.name)
                            .withTypeText("resource type")
                            .withIcon(OpenVoxIcons.FILE),
                    )
                }
                OpenVoxResolver.allNames(project, OpenVoxDeclarationKind.DEFINE).forEach { name ->
                    result.addElement(LookupElementBuilder.create(name).withTypeText("defined type"))
                }
            }

            is OpenVoxCompletionPlace.FactName -> {
                val paths = if (place.trusted) TRUSTED_FACTS else OpenVoxKnowledge.facts.map { it.path }
                paths.asSequence().filter { it.startsWith(place.prefix) }
                    .map { it.removePrefix(place.prefix).substringBefore('.') }
                    .distinct().forEach { key ->
                    result.addElement(
                        LookupElementBuilder.create(key)
                            .withTypeText("fact"),
                    )
                }
            }

            OpenVoxCompletionPlace.Statement -> {
                OpenVoxKnowledge.functions.values.forEach { function ->
                    result.addElement(
                        LookupElementBuilder.create(function.name)
                            .withTailText(function.signatures.firstOrNull()?.let { "($it)" } ?: "()", true)
                            .withTypeText(function.returnType.ifEmpty { "function" })
                            .withIcon(OpenVoxIcons.FILE),
                    )
                }
                OpenVoxResolver.allNames(project, OpenVoxDeclarationKind.FUNCTION).forEach { name ->
                    result.addElement(LookupElementBuilder.create(name).withTypeText("function"))
                }
                OpenVoxResolver.allNames(project, OpenVoxDeclarationKind.CLASS).forEach { name ->
                    result.addElement(LookupElementBuilder.create(name).withTypeText("class"))
                }
                OpenVoxResolver.allNames(project, OpenVoxDeclarationKind.DEFINE).forEach { name ->
                    result.addElement(LookupElementBuilder.create(name).withTypeText("defined type"))
                }
                addVariables(parameters, result)
            }
        }
    }

    private fun addVariables(parameters: CompletionParameters, result: CompletionResultSet) {
        OpenVoxScope.declarationsVisibleFrom(parameters.position)
            .mapNotNull { it.name?.let { name -> "\$$name" } }
            .distinct()
            .forEach { result.addElement(LookupElementBuilder.create(it).withTypeText("variable")) }
    }

    private fun capitalise(name: String): String =
        name.split("::").joinToString("::") { it.replaceFirstChar(Char::uppercaseChar) }

    private val TRUSTED_FACTS = listOf("authenticated", "certname", "domain", "extensions", "external", "hostname")
}

private object ArrowInsertHandler : com.intellij.codeInsight.completion.InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val tail = document.charsSequence.subSequence(context.tailOffset, document.textLength).take(4).toString()
        if (!tail.trimStart().startsWith("=>")) {
            document.insertString(context.tailOffset, " => ")
            context.editor.caretModel.moveToOffset(context.tailOffset + 4)
        }
    }
}
