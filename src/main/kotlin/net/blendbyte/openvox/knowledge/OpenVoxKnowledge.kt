package net.blendbyte.openvox.knowledge

data class OpenVoxFunction(
    val name: String,
    val signatures: List<String>,
    val returnType: String,
    val doc: String,
) {
    fun presentableSignature(): String =
        signatures.firstOrNull()?.let { "$name($it)" } ?: "$name(…)"
}

data class OpenVoxAttribute(
    val typeName: String,
    val name: String,
    val kind: String,
    val values: List<String>,
    val doc: String,
)

data class OpenVoxResourceType(
    val name: String,
    val doc: String,
    val attributes: List<OpenVoxAttribute>,
)

data class OpenVoxDataType(val name: String, val doc: String)

data class OpenVoxFact(val path: String, val doc: String) {
    val root: String get() = path.substringBefore('.')
}

object OpenVoxKnowledge {

    val functions: Map<String, OpenVoxFunction> by lazy {
        read("functions.tsv") { columns ->
            val name = columns[0]
            name to OpenVoxFunction(
                name = name,
                signatures = columns.getOrElse(1) { "" }.split(';').filter { it.isNotBlank() },
                returnType = columns.getOrElse(2) { "" },
                doc = columns.getOrElse(3) { "" },
            )
        }.toMap()
    }

    val resourceTypes: Map<String, OpenVoxResourceType> by lazy {
        val attributes = read("type-attributes.tsv") { columns ->
            OpenVoxAttribute(
                typeName = columns[0],
                name = columns[1],
                kind = columns.getOrElse(2) { "parameter" },
                values = columns.getOrElse(3) { "" }.split(',').filter { it.isNotBlank() },
                doc = columns.getOrElse(4) { "" },
            )
        }.groupBy { it.typeName }

        read("types.tsv") { columns ->
            val name = columns[0]
            name to OpenVoxResourceType(name, columns.getOrElse(1) { "" }, attributes[name].orEmpty())
        }.toMap()
    }

    val metaparameters: List<OpenVoxAttribute> by lazy {
        read("metaparameters.tsv") { columns ->
            OpenVoxAttribute("", columns[0], "metaparameter", emptyList(), columns.getOrElse(1) { "" })
        }
    }

    val dataTypes: Map<String, OpenVoxDataType> by lazy {
        read("datatypes.tsv") { columns ->
            columns[0] to OpenVoxDataType(columns[0], columns.getOrElse(1) { "" })
        }.toMap()
    }

    val facts: List<OpenVoxFact> by lazy {
        read("facts.tsv") { columns -> OpenVoxFact(columns[0], columns.getOrElse(1) { "" }) }
    }

    fun attributesFor(typeName: String): List<OpenVoxAttribute> =
        (resourceTypes[typeName.lowercase()]?.attributes.orEmpty() + metaparameters)
            .distinctBy { it.name }

    fun attribute(typeName: String, attributeName: String): OpenVoxAttribute? =
        attributesFor(typeName).firstOrNull { it.name == attributeName }

    private fun <T> read(resource: String, parse: (List<String>) -> T): List<T> {
        val stream = OpenVoxKnowledge::class.java.getResourceAsStream("/openvox/$resource")
            ?: return emptyList()
        return stream.bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith("#") }
                .map { line -> parse(line.split('\t')) }
                .toList()
        }
    }
}
