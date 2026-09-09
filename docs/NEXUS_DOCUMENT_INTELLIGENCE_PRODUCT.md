# NEXUS Document Intelligence — Useful-by-Default Product Upgrade

## Product goal

Turn the future LibreOffice/Collabora-based Android app from a passive office viewer/editor into a practical document workbench that can understand, verify and act on documents while preserving an offline-first privacy boundary.

## Primary user experience

Every opened document should expose one prominent **Intelligence** action. Tapping it opens a bottom sheet with the most useful actions first rather than a long settings menu.

### Quick actions

1. **Summarise** — concise executive summary with source references.
2. **Ask Document** — question-answering grounded only in the open document.
3. **Extract Data** — structured fields with confidence and source location.
4. **Key Risks** — identify risks, missing information and unusual clauses.
5. **Actions & Deadlines** — find obligations, dates, owners and next steps.
6. **Check Consistency** — highlight contradictory figures, dates or names.
7. **Explain Selection** — explain selected text in plain language.
8. **Rewrite Selection** — improve clarity without changing factual meaning.
9. **Redact Sensitive Data** — preview likely sensitive fields before any redaction is applied.
10. **Document Type** — classify the document and suggest the best next actions.

## Banking-oriented intelligence adapters

These remain optional adapters and must not be hard-wired into the office engine.

- **CAR Facts** — capture factual fields that may later populate a CAR workflow.
- **Financial Facts** — extract revenue, PBT, EBITDA, net worth, gearing and other verified financial facts where available.
- **Facility Facts** — identify amount, tenure, pricing, repayment, security and conditions.
- **Security Facts** — capture security descriptions and source references.
- **Contract Facts** — parties, term, commencement, termination, payment, obligations and renewal clauses.
- **Bank Statement Facts** — statement period, balances and transaction-derived summaries only after a dedicated parser validates the data.

The intelligence layer must distinguish **document facts** from **derived analysis**. Derived figures must retain their inputs and calculation method.

## Trust model

The app should never present model output as verified fact without grounding.

Every important answer should be able to return:

- confidence: low / medium / high;
- source page or section where available;
- short evidence excerpt;
- warnings when data is missing, contradictory or inferred;
- a visible distinction between extracted facts and model interpretation.

If evidence cannot be found, the correct output is **not found in this document**, not a guess.

## Privacy model

Default mode is **Local Only**.

- No automatic document uploads.
- No hidden telemetry containing document text.
- No credential capture.
- No direct internal bank portal automation.
- Temporary analysis data is minimised and clearable.
- External/cloud processing, if ever added, must be explicit and separately approved by the user and applicable organisation policy.

## Mobile design

Optimise for one-hand use:

- one Intelligence button in the document toolbar;
- bottom-sheet quick actions;
- recent questions and actions kept locally;
- selected-text actions accessible from the normal selection menu;
- progressive disclosure: short answer first, evidence/details on tap;
- long jobs cancellable;
- clear offline/local status indicator.

## Architecture

```text
LibreOffice / Collabora renderer-editor
              |
      Office Engine Adapter
              |
   Document Intelligence API
       /       |        \
 Local AI   Parsers   Rules/validators
       \       |        /
       Evidence + confidence
              |
         Android UX
              |
       Optional NEXUS adapters
```

## Delivery order

### Phase A — useful immediately

- document classification;
- summarise;
- ask document;
- explain selected text;
- key risks;
- actions and deadlines;
- evidence references;
- local-only mode.

### Phase B — reliable extraction

- structured extraction schema;
- confidence per field;
- dates/numbers/entity validation;
- document inconsistency checking;
- export extracted facts as JSON/CSV without altering the source document.

### Phase C — banking productivity

- CAR facts adapter;
- financial facts adapter;
- contract facts adapter;
- customer/application linking through NEXUS interfaces;
- review-before-write workflow for any generated output.

### Phase D — advanced workspace

- compare two documents;
- version-difference explanation;
- cross-document search;
- reusable local document index;
- configurable policy/rule adapters;
- user-created extraction templates.

## Validation gate

Before this migration is considered merge-ready, the same branch head must pass both the repository `format` and `build_test` workflows. The formatter output must be committed, unit tests must compile and pass, and no renderer replacement should be merged until the LibreOffice/Collabora source workspace is writable and reproducibly buildable.

## Non-goals

- Do not rebuild LibreOffice rendering/editing.
- Do not embed banking rules inside the renderer.
- Do not silently alter documents.
- Do not invent missing figures.
- Do not make cloud access mandatory.

## Success criteria

The upgraded app is successful when a user can open a document and, within a few taps, understand what it is, what matters, what is risky, what needs action, and where each answer came from — while retaining the ability to edit the original office file with the LibreOffice/Collabora engine.
