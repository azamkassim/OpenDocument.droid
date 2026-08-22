package app.opendocument.droid.intelligence

/**
 * Privacy guardrails for document intelligence.
 *
 * The default is deliberately local-only. Any future remote processing must be
 * explicitly enabled by a separate integration layer and must never be implied
 * by this core contract.
 */
data class LocalProcessingPolicy(
    val localOnly: Boolean = true,
    val allowDocumentTextTelemetry: Boolean = false,
    val allowCredentialCapture: Boolean = false,
    val allowAutomaticExternalUpload: Boolean = false,
    val requireEvidenceForImportantClaims: Boolean = true,
    val requireReviewBeforeWriteBack: Boolean = true,
) {
    init {
        require(!allowCredentialCapture) { "Credential capture is not permitted." }
        if (localOnly) {
            require(!allowAutomaticExternalUpload) {
                "External upload cannot be enabled while localOnly is true."
            }
        }
    }
}

object NexusDefaultProcessingPolicy {
    val value = LocalProcessingPolicy()
}
