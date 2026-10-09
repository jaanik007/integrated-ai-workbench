---
name: skill-ux-review
description: Audits the plugin against the project UX checklist and produces a structured, evidence-based review that can be used directly as remediation planning input.
---

# UX Review

## Purpose

Review the requested plugin scope against the project UX requirements.

This skill is audit-only.

Do not modify implementation, configuration, documentation, or Git state.

## Context

Before reviewing:

1. Read `AGENTS.md`.
2. Read the root `README.md`.
3. Read the applicable subsystem `README.md` files.
4. Inspect the actual implementation and configuration relevant to the review scope.
5. Base every result on observable project evidence.

Do not infer that a feature works because it is documented or planned.

## Status values

Use exactly:

- `PASS` — requirement is implemented and supported by evidence.
- `FAIL` — requirement is missing or violated.
- `UNKNOWN` — available evidence is insufficient to verify it.
- `N/A` — requirement does not apply to the reviewed scope.

## Priority values

Use exactly:

- `P0` — release-blocking or serious stability/security issue.
- `P1` — important UX or platform-compliance issue.
- `P2` — normal improvement required.
- `P3` — minor refinement.
- `-` — no remediation required.

## Review checklist

### Usability

| ID | Requirement |
|---|---|
| UX-USE-01 | Core functionality works with sensible defaults after installation. |
| UX-USE-02 | Common functionality and settings are easy to discover. |
| UX-USE-03 | Settings are placed in appropriate IntelliJ settings locations. |
| UX-USE-04 | Actions are placed in appropriate IntelliJ action groups. |
| UX-USE-05 | Users do not need to understand internal architecture to use the feature. |

### Stability

| ID | Requirement |
|---|---|
| UX-STB-01 | Plugin failures do not unnecessarily pollute IDE notifications or messages. |
| UX-STB-02 | Failed AI operations leave the project and IDE in a valid state. |
| UX-STB-03 | High-impact generated changes are validated before execution or application. |
| UX-STB-04 | Critical workflows have appropriate automated validation or tests. |
| UX-STB-05 | Problems can be diagnosed from available logs without exposing secrets. |
| UX-STB-06 | A user-accessible issue reporting path exists for released versions. |

### Performance

| ID | Requirement |
|---|---|
| UX-PERF-01 | LLM, grounding, evaluation, training, network, and long-running operations do not block the UI thread. |
| UX-PERF-02 | Project scanning is scoped and performed only when required. |
| UX-PERF-03 | Features support IntelliJ dumb mode where technically possible. |
| UX-PERF-04 | Grounding avoids unnecessary full-project analysis. |
| UX-PERF-05 | Slow providers or runtimes do not freeze the IDE. |
| UX-PERF-06 | Long-running operations support cancellation or timeout where applicable. |

### Remote Development and Split Mode

| ID | Requirement |
|---|---|
| UX-REMOTE-01 | UI and direct user interaction remain on the frontend side where appropriate. |
| UX-REMOTE-02 | Project files, PSI, indexes, and project-dependent processing remain on the backend side where appropriate. |
| UX-REMOTE-03 | Only necessary data crosses frontend/backend boundaries. |
| UX-REMOTE-04 | Runtime location is explicit when local and remote environments may differ. |

### Distribution

| ID | Requirement |
|---|---|
| UX-DIST-01 | Large language models are not bundled with the plugin. |
| UX-DIST-02 | Python, CUDA, model runtimes, or other large external runtimes are not bundled without a justified requirement. |
| UX-DIST-03 | Existing IntelliJ Platform libraries are reused where appropriate. |
| UX-DIST-04 | Duplicate or unnecessary dependencies are avoided. |
| UX-DIST-05 | Icons and other packaged assets are reasonably optimized. |
| UX-DIST-06 | Large optional components are externalized or loaded only when required. |

### IntelliJ Platform Consistency

| ID | Requirement |
|---|---|
| UX-IDE-01 | Existing IntelliJ functionality is reused instead of unnecessarily reimplemented. |
| UX-IDE-02 | Tool Windows follow normal IntelliJ behaviour. |
| UX-IDE-03 | Settings follow normal IntelliJ behaviour. |
| UX-IDE-04 | Actions follow normal IntelliJ behaviour. |
| UX-IDE-05 | Notifications and dialogs follow normal IntelliJ behaviour. |
| UX-IDE-06 | Stable public IntelliJ Platform APIs are preferred. |
| UX-IDE-07 | Standard keyboard, focus, navigation, cancellation, and interaction patterns are preserved. |

### Visual Consistency

| ID | Requirement |
|---|---|
| UX-VIS-01 | Standard IntelliJ UI components are preferred. |
| UX-VIS-02 | Custom UI is used only when standard components are insufficient. |
| UX-VIS-03 | Icons follow JetBrains platform conventions. |
| UX-VIS-04 | Light theme is supported correctly. |
| UX-VIS-05 | Dark theme is supported correctly. |
| UX-VIS-06 | Layout and interaction patterns are visually consistent with the IDE. |

### UI Text

| ID | Requirement |
|---|---|
| UX-TXT-01 | Labels, buttons, and field names are concise and unambiguous. |
| UX-TXT-02 | Error messages explain what happened. |
| UX-TXT-03 | Error messages provide an actionable next step where possible. |
| UX-TXT-04 | Internal implementation terminology is not unnecessarily exposed to users. |
| UX-TXT-05 | User-facing English text is clear and consistent. |
| UX-TXT-06 | UI text is structured so future localization remains possible. |

### Marketplace Presentation

| ID | Requirement |
|---|---|
| UX-MKT-01 | Plugin description clearly explains its purpose. |
| UX-MKT-02 | Screenshots represent actual implemented functionality. |
| UX-MKT-03 | Implemented, partial, experimental, and planned functionality are clearly distinguished. |
| UX-MKT-04 | Installation and initial setup instructions are clear. |
| UX-MKT-05 | Provider and external runtime requirements are clearly documented. |

## Review method

For every checklist item:

1. inspect relevant source code, resources, configuration, tests, and documentation;
2. assign one status;
3. cite concrete project evidence using file paths, classes, methods, configuration keys, or UI components;
4. for `FAIL`, describe the smallest required remediation;
5. for `UNKNOWN`, state exactly what must be verified;
6. assign remediation priority.

Do not mark an item `PASS` based only on planned documentation.

Do not treat an unimplemented future feature as a failure unless it belongs to the reviewed scope or current release target.

## Output

Return exactly this structure.

# UX Review Result

**Scope:** `<reviewed scope>`  
**Date:** `<date if available>`

## Summary

| Status | Count |
|---|---:|
| PASS | 0 |
| FAIL | 0 |
| UNKNOWN | 0 |
| N/A | 0 |

## Checklist

| ID | Requirement | Status | Evidence | Required action | Priority |
|---|---|---|---|---|---|
| UX-USE-01 | Core functionality works with sensible defaults after installation. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-USE-02 | Common functionality and settings are easy to discover. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-USE-03 | Settings are placed in appropriate IntelliJ settings locations. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-USE-04 | Actions are placed in appropriate IntelliJ action groups. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-USE-05 | Users do not need to understand internal architecture to use the feature. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-STB-01 | Plugin failures do not unnecessarily pollute IDE notifications or messages. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-STB-02 | Failed AI operations leave the project and IDE in a valid state. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-STB-03 | High-impact generated changes are validated before execution or application. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-STB-04 | Critical workflows have appropriate automated validation or tests. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-STB-05 | Problems can be diagnosed from available logs without exposing secrets. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-STB-06 | A user-accessible issue reporting path exists for released versions. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-PERF-01 | Long-running operations do not block the UI thread. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-PERF-02 | Project scanning is scoped and performed only when required. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-PERF-03 | Features support IntelliJ dumb mode where technically possible. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-PERF-04 | Grounding avoids unnecessary full-project analysis. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-PERF-05 | Slow providers or runtimes do not freeze the IDE. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-PERF-06 | Long-running operations support cancellation or timeout where applicable. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-REMOTE-01 | UI and direct user interaction remain on the frontend side where appropriate. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-REMOTE-02 | Project-dependent processing remains on the backend side where appropriate. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-REMOTE-03 | Only necessary data crosses frontend/backend boundaries. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-REMOTE-04 | Runtime location is explicit when local and remote environments may differ. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-DIST-01 | Large language models are not bundled with the plugin. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-DIST-02 | Large external runtimes are not bundled without justification. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-DIST-03 | Existing IntelliJ Platform libraries are reused where appropriate. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-DIST-04 | Duplicate or unnecessary dependencies are avoided. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-DIST-05 | Packaged assets are reasonably optimized. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-DIST-06 | Large optional components are externalized or loaded only when required. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-IDE-01 | Existing IntelliJ functionality is reused instead of unnecessarily reimplemented. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-IDE-02 | Tool Windows follow normal IntelliJ behaviour. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-IDE-03 | Settings follow normal IntelliJ behaviour. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-IDE-04 | Actions follow normal IntelliJ behaviour. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-IDE-05 | Notifications and dialogs follow normal IntelliJ behaviour. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-IDE-06 | Stable public IntelliJ Platform APIs are preferred. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-IDE-07 | Standard keyboard, focus, navigation, cancellation, and interaction patterns are preserved. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-VIS-01 | Standard IntelliJ UI components are preferred. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-VIS-02 | Custom UI is used only when standard components are insufficient. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-VIS-03 | Icons follow JetBrains platform conventions. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-VIS-04 | Light theme is supported correctly. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-VIS-05 | Dark theme is supported correctly. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-VIS-06 | Layout and interaction patterns are visually consistent with the IDE. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-TXT-01 | Labels, buttons, and field names are concise and unambiguous. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-TXT-02 | Error messages explain what happened. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-TXT-03 | Error messages provide an actionable next step where possible. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-TXT-04 | Internal implementation terminology is not unnecessarily exposed to users. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-TXT-05 | User-facing English text is clear and consistent. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-TXT-06 | UI text allows future localization. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-MKT-01 | Plugin description clearly explains its purpose. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-MKT-02 | Screenshots represent actual implemented functionality. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-MKT-03 | Implemented, partial, experimental, and planned functionality are clearly distinguished. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-MKT-04 | Installation and initial setup instructions are clear. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |
| UX-MKT-05 | Provider and external runtime requirements are clearly documented. | PASS / FAIL / UNKNOWN / N/A | `<evidence>` | `<action or ->` | `<priority>` |

## Remediation Input

Include only `FAIL` and actionable `UNKNOWN` items.

Order by:

`P0 → P1 → P2 → P3`

Use:

| ID | Priority | Problem | Required change | Evidence / verification target |
|---|---|---|---|---|
| `<ID>` | `<P0-P3>` | `<confirmed deficiency>` | `<smallest correct change>` | `<file, component, behaviour, or test>` |

If there are no remediation items, write:

`No UX remediation items identified in the reviewed scope.`

## Completion rule

The review is complete only when every applicable checklist item has:

- one status;
- concrete evidence or a reason why evidence is unavailable;
- a remediation action for every `FAIL`;
- a verification target for every actionable `UNKNOWN`.

Never convert `UNKNOWN` to `PASS` based on assumption.

## Output file

Write the completed review to:

`_local/reviews/ux-review.md`

Create the directory if it does not exist.

Overwrite the previous review on every complete run.

Do not modify any other project files.