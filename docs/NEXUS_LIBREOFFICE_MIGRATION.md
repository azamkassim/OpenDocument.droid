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
  |
  +-- Office Engine Adapter
  |     +-- Collabora/LibreOffice Android
  |
  +-- Local AI Adapter
        +-- llama.cpp / local server
```

## Safety boundary

This migration does not automate access to internal bank portals, credentials, or restricted systems. Any future direct integration with internal banking systems requires explicit compliance/IT approval.

## Next external prerequisite

A writable fork/repository of the Collabora Android source is required before the renderer/editor itself can be modified. The current GitHub connector can modify repositories already accessible to the user, but it cannot create a new repository or fork an upstream repository. Once that writable source repository exists, this branch provides the migration contract for porting the intelligence layer into it.
