package app.opendocument.droid.background

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredDocumentFindingsTest {
    @Test
    fun parsesSupportedCategoriesWithSources() {
        val parsed =
            StructuredDocumentFindingsParser.parse(
                """
                SUMMARY | [SOURCE chars 0-99] | Contract is active.
                RISK | [SOURCE chars 100-199] | Revenue depends on one customer.
                OBLIGATION | [SOURCE chars 200-299] | Maintain insurance coverage.
                DATE | [SOURCE chars 300-399] | Maturity is 31 December 2030.
                FIGURE | [SOURCE chars 400-499] | Facility amount is RM40 million.
                ACTION_ITEM | [SOURCE chars 500-599] | Obtain board approval.
                """
                    .trimIndent()
            )

        assertEquals(6, parsed.findings.size)
        assertEquals(FindingCategory.SUMMARY, parsed.findings.first().category)
        assertEquals(listOf("[SOURCE chars 0-99]"), parsed.findings.first().sources)
    }

    @Test
    fun rejectsUnsupportedOrUnsourcedLines() {
        val parsed =
            StructuredDocumentFindingsParser.parse(
                """
                UNKNOWN | [SOURCE chars 0-99] | Ignore me.
                RISK | no source | Ignore me too.
                FIGURE | [SOURCE chars 100-199] | RM10 million.
                """
                    .trimIndent()
            )

        assertEquals(1, parsed.findings.size)
        assertEquals(FindingCategory.FIGURE, parsed.findings.single().category)
    }

    @Test
    fun formatterGroupsFindingsAndPreservesSourceMarkers() {
        val parsed =
            StructuredDocumentFindingsParser.parse(
                """
                RISK | [SOURCE chars 10-20] | Customer concentration.
                SUMMARY | [SOURCE chars 0-9] | Financing proposal.
                RISK | [SOURCE chars 21-30] | Construction delay.
                """
                    .trimIndent()
            )

        val formatted = parsed.formatForModel()

        assertTrue(formatted.contains("## Summary"))
        assertTrue(formatted.contains("## Risks"))
        assertTrue(formatted.contains("[SOURCE chars 10-20]"))
        assertTrue(formatted.contains("[SOURCE chars 21-30]"))
    }

    @Test
    fun parserDeduplicatesRepeatedSourceMarkers() {
        val parsed =
            StructuredDocumentFindingsParser.parse(
                "RISK | [SOURCE chars 0-99] [SOURCE chars 0-99] | Repeated source."
            )

        assertEquals(listOf("[SOURCE chars 0-99]"), parsed.findings.single().sources)
    }
}
