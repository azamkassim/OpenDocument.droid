# NEXUS Document Intelligence — LibreOffice/Collabora Migration

This branch starts the migration from OpenDocument Reader to a LibreOffice/Collabora-powered document engine.

## Architecture

- LibreOffice/Collabora remains the office rendering/editing engine.
- NEXUS Document Intelligence is a separate intelligence layer.
- Existing offline AI components from OpenDocument.droid are retained and migrated rather than discarded.
- Banking documents must remain local-first by default; no automatic upload or external processing is introduced by this migration.

## Phase 1 scope

1. Preserve current OpenDocument.droid main unchanged.
2. Isolate reusable offline-AI client and document-intelligence concepts.
3. Define an engine-neutral document intelligence interface.
4. Prepare adapters for a future Collabora Android source fork.
5. Add user actions: Summarise, Ask Document, Extract Data, Identify Document, Analyse.
6. Keep policy/credit/financial integrations behind interfaces so they can connect to NEXUS engines later.

## Useful-by-default upgrade

The future LibreOffice/Collabora-based app should act as a document workbench, not just a viewer/editor. The migration branch now defines reusable contracts for:

- Summarise with evidence.
- Ask Document grounded only in the open file.
- Key Risks and missing-information detection.
- Actions & Deadlines extraction.
- Structured data extraction with confidence and source references.
- Consistency checks across figures, dates and names.
- Explain / Rewrite / Redact selected text.
- CAR, financial and contract adapters without coupling banking logic to the office renderer.

The Android UX should expose one prominent **Intelligence** action that opens a one-hand-friendly bottom sheet with the most useful actions first.

## Trust model

Important outputs should include confidence, evidence references and warnings. Extracted facts and derived/model analysis must remain visibly distinct. If supporting evidence cannot be found, the system should say so rather than guess.

## Privacy model

The default processing policy is Local Only:

- no automatic external upload;
- no document-text telemetry;
- no credential capture;
- review required before write-back;
- direct internal bank-system automation remains out of scope without explicit organisational approval.

## Target runtime structure

```text
NEXUS Android Shell
  |
  +-- Document Intelligence Layer
  |     +-- Summarise
  |     +-- Ask Document
  |     +-- Extract Data
  |     +-- Identify Document
  |     +-- Analyse
  |     +-- Risks / Actions / Consistency
  |
  +-- Office Engine Adapter
  |     +-- Collabora/LibreOffice Android
  |
  +-- Local AI Adapter
  |     +-- llama.cpp / local server
  |
  +-- Evidence + Validation Layer
        +-- confidence / source / warnings
```

## Current branch implementation

- `DocumentIntelligenceContract.kt` defines an engine-neutral request/result contract, evidence model, confidence model and action set.
- `LocalProcessingPolicy.kt` defines safe local-first defaults.
- `IntelligenceActionCatalog.kt` defines the mobile quick-action catalog for the future Intelligence bottom sheet.
- `docs/NEXUS_DOCUMENT_INTELLIGENCE_PRODUCT.md` defines the full useful-by-default product direction and phased delivery plan.

## Safety boundary

This migration does not automate access to internal bank portals, credentials, or restricted systems. Any future direct integration with internal banking systems requires explicit compliance/IT approval.

## Next external prerequisite

A writable fork/repository of the Collabora Android source is required before the renderer/editor itself can be modified. The current GitHub connector can modify repositories already accessible to the user, but it cannot create a new repository or fork an upstream repository. Once that writable source repository exists, this branch provides the migration contract for porting the intelligence layer into it.
