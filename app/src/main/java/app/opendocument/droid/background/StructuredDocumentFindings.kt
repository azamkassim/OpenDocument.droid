package app.opendocument.droid.background

enum class FindingCategory(val wireName: String, val heading: String) {
    SUMMARY("SUMMARY", "Summary"),
    RISK("RISK", "Risks"),
    OBLIGATION("OBLIGATION", "Obligations"),
    DATE("DATE", "Dates"),
    FIGURE("FIGURE", "Figures"),
    ACTION_ITEM("ACTION_ITEM", "Action items");

    companion object {
        fun fromWireName(value: String): FindingCategory? = entries.firstOrNull {
            it.wireName == value.trim().uppercase()
        }
    }
}

data class DocumentFinding(
    val category: FindingCategory,
    val text: String,
    val sources: List<String>,
)

data class StructuredDocumentFindings(val findings: List<DocumentFinding>) {
    fun isEmpty(): Boolean = findings.isEmpty()

    fun formatForModel(): String =
        FindingCategory.entries
            .mapNotNull { category ->
                val entries = findings.filter { it.category == category }
                if (entries.isEmpty()) return@mapNotNull null

                buildString {
                    append("## ")
                    append(category.heading)
                    append('\n')
                    entries.forEach { finding ->
                        append("- ")
                        append(finding.text)
                        if (finding.sources.isNotEmpty()) {
                            append(' ')
                            append(finding.sources.joinToString(" "))
                        }
                        append('\n')
                    }
                }
                    .trimEnd()
            }
            .joinToString("\n\n")
}

object StructuredDocumentFindingsParser {
    private val sourceRegex = Regex("\\[SOURCE [^]]+]", RegexOption.IGNORE_CASE)

    fun parse(text: String): StructuredDocumentFindings {
        val findings =
            text
                .lineSequence()
                .mapNotNull { line ->
                    parseLine(line)
                }
                .toList()
        return StructuredDocumentFindings(findings)
    }

    private fun parseLine(line: String): DocumentFinding? {
        val parts = line.split('|', limit = 3)
        if (parts.size != 3) return null

        val category = FindingCategory.fromWireName(parts[0]) ?: return null
        val sources = sourceRegex.findAll(parts[1]).map { it.value }.distinct().toList()
        if (sources.isEmpty()) return null

        val findingText = parts[2].trim().removePrefix("-").trim()
        if (findingText.isEmpty()) return null

        return DocumentFinding(category, findingText, sources)
    }
}
