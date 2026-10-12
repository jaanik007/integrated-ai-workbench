---
name: skill-pre-publication-hardening
description: Perform comprehensive pre-publication code, documentation, legal, security, and repository hygiene review and remediation for Integrated AI Workbench. Apply when preparing the entire project for public GitHub release, preserving verified behavior and protecting internal development knowledge.
---

# Integrated AI Workbench — Publication Hardening

## 1. Mission

Prepare Integrated AI Workbench for public GitHub publication through a comprehensive review and controlled remediation
of all repository files.

The desired outcome is a coherent, maintainable, legally compliant, professionally structured codebase and documentation
set consistent with careful human software engineering practice.

Apply:

- Clean Code principles.
- Existing project coding conventions.
- Applicable IntelliJ Platform and Kotlin best practices.
- Consistent code and documentation style.
- Single-source-of-truth documentation.
- Evidence-based verification.
- Corrective and preventive action management.
- Strict separation between public deliverables and private development knowledge.

**Primary invariant: Preserve existing functionality, public contracts, runtime behavior, configuration compatibility,
and verified integration results.**

Do not perform speculative refactoring, architectural redesign, dependency replacement, or feature development.

Fix a functional defect only when supported by reproducible evidence and the proposed correction is explicitly
authorized.

This skill operates on actual repository evidence, never on assumptions about expected files or functionality.

---

## 2. Scope and inventory

Inspect the entire repository and determine the precise public release surface.

Include:

- Tracked files.
- Staged files.
- Untracked files not excluded by Git.
- Source code, resources, configurations, scripts, and tests.
- Gradle and IntelliJ plugin descriptors.
- Agent definitions, skills, and runtime prompts.
- Markdown and other documentation formats.
- Licenses, third-party notices, and attribution.
- Public reports.
- Git ignore rules and release packaging.
- Referenced binaries, generated resources, and external dependencies.

Use Git to establish the publication baseline:

- `git status --short`
- `git ls-files`
- `git ls-files --others --exclude-standard`
- `git diff --check`
- Relevant `git check-ignore` and staged-diff checks.

Inspect ignored files separately when necessary to detect accidental exposure or dependencies on unpublished
configuration.

Inspect public Git history for previously committed sensitive or private material where applicable.

Do not assume that `.gitignore` protects files already tracked or already present in Git history.

Record the initial repository state before making changes.

Do not overwrite unrelated user modifications or interfere with another ongoing Codex task or audit.

### File classification

Assign every relevant file one disposition:

| Classification        | Meaning                                                                   | Action                         |
|-----------------------|---------------------------------------------------------------------------|--------------------------------|
| PUBLIC-RUNTIME        | Required application source or runtime asset                              | Retain and review              |
| PUBLIC-DEVELOPMENT    | Necessary build, test, or contributor resource                            | Retain and review              |
| PUBLIC-DOCUMENTATION  | Relevant documentation for users or maintainers                           | Standardize                    |
| PUBLIC-LEGAL          | License, attribution, or required notices                                 | Verify and retain              |
| PUBLIC-REPORT         | Latest publishable validation or audit report                             | Place in `docs/reports/`       |
| PRIVATE-KNOWHOW       | Internal planning, prompts, implementation instructions, historical tasks | Move to `_private/`            |
| PRIVATE-CONFIGURATION | Machine-specific or sensitive configuration                               | Exclude from Git               |
| GENERATED             | Reproducible build/cache output                                           | Ignore or safely remove        |
| REDUNDANT             | Verified obsolete or duplicated material                                  | Remove after dependency review |
| UNRESOLVED            | Ownership, purpose, or dependencies uncertain                             | Record and request decision    |

No publication candidate may remain unclassified.

Do not confuse internal agent-development files with runtime agent or skill definitions genuinely required by the
distributed application.

---

## 3. Clean Code and source harmonization

Review every applicable source file.

Follow existing well-established project conventions where they are coherent and technically correct.

### Naming and structure

Ensure:

1. Consistent package, class, interface, function, variable, and constant names.
2. Names describe domain intent and responsibility.
3. Consistent Kotlin and Java naming conventions where applicable.
4. Predictable file and declaration organization.
5. Imports are explicit, necessary, and consistently formatted.
6. Formatting, indentation, spacing, and line breaks follow the selected project conventions.
7. Visibility is no broader than necessary.
8. Unused declarations and imports are eliminated.
9. Duplicate logic is consolidated where doing so is demonstrably behavior-preserving.
10. Error handling and validation follow existing contracts.

Avoid introducing unnecessary abstractions, interfaces, wrappers, or helper functions solely to satisfy an aesthetic
preference.

### Separation of responsibilities

Respect the existing Integrated AI Workbench architecture.

Keep clearly separated:

- IntelliJ Tool Window and presentation state.
- Project/application services.
- Agent execution and orchestration contracts.
- Local model inference and managed runtime.
- MCP transport and tool invocation.
- Tool authorization and security policies.
- Grounding and prompt preparation.
- Structured output validation and evaluation.
- Runtime agent and skill definitions.

UI components must not accumulate inference, transport, or security responsibilities.

Transport adapters must not silently bypass domain validation or authorization.

Do not introduce Spring Boot-style Controller/Service/Repository classes into modules that do not need them.

### Functions and control flow

Prefer:

- Small cohesive functions with meaningful responsibilities.
- Explicit data flow.
- Clear guard clauses.
- Readable conditions.
- Deterministic resource ownership.
- Consistent exception handling.
- Explicit cancellation and disposal behavior.
- Immutable values where appropriate.
- Strongly typed contracts instead of ambiguous strings or unchecked maps.

Preserve existing thread-safety, lifecycle, permission, logging, and cancellation behavior.

Do not change API signatures, serialization, error codes, persistence, execution ordering, or observable output without
a verified requirement.

### Comments and documentation in code

Write concise technical explanations where they add value.

Comments should explain **why**, not merely repeat **what** the code states.

Remove:

- Obvious line-by-line narration.
- Misleading comments.
- Obsolete implementation descriptions.
- Duplicate explanations.
- Unnecessary AI-style commentary.
- Dead commented-out code.

Retain important explanations about:

- Non-obvious constraints.
- Security assumptions.
- Lifecycle requirements.
- Compatibility workarounds.
- External API contracts.
- Failure conditions.

Document public APIs and extension points consistently where required.

### API documentation — conditional

If the repository actually exposes REST endpoints or contains applicable OpenAPI contracts, review their documentation
against the implementation.

Follow the established training-project convention where relevant:

- Concise `@Operation` descriptions.
- `@ApiResponses` where different HTTP outcomes require explanation.
- Appropriate request, response, and schema descriptions.
- Correct imports and annotation placement.
- Consistency between Swagger/OpenAPI and actual implementation.

Do not introduce Swagger, SpringDoc, or REST annotations into unrelated Kotlin IntelliJ Platform code.

### Human-quality style

Prefer readable, purposeful, conventional code.

Avoid:

- Excessive abstraction.
- Inconsistent terminology.
- Decorative comments.
- Artificially verbose names.
- Redundant validation.
- Repeated boilerplate.
- Over-engineered design patterns.
- Changes made solely to increase code complexity.

The goal is maintainability and consistency, not concealing code provenance or fabricating human authorship.

---

## 4. Behavioral preservation and validation

Before changing code, identify applicable invariants and existing tests.

Separate changes into:

- STYLE-ONLY — formatting, imports, comments, equivalent renaming.
- STRUCTURAL — behavior-preserving internal refactoring.
- BUGFIX — correction of a verified functional defect.
- DOCUMENTATION — changes without runtime implications.
- REPOSITORY — classification, relocation, exclusion, or cleanup.

For STYLE-ONLY and STRUCTURAL changes:

- Preserve observable behavior.
- Verify callers and references.
- Preserve IntelliJ plugin lifecycle contracts.
- Preserve agent/tool permissions.
- Preserve runtime configuration compatibility.
- Preserve user-visible behavior.
- Run relevant automated verification.

For BUGFIX changes:

- Identify the defect.
- Record reproducible evidence.
- Explain the root cause.
- Identify the minimum safe correction.
- Record regression coverage.
- Obtain approval before changing functionality.

If verification is unavailable, report it. Never equate successful formatting or compilation with full functional
equivalence.

---

## 5. Documentation ownership and single source of truth

Treat the root `README.md` as the public entry point, not as a container for all technical details.

Every specific architectural rule, configuration contract, workflow, or technical decision must have exactly one
authoritative documentation location.

Other documents may summarize its purpose in one or two sentences and link to the authoritative document.

Do not maintain independently editable copies of the same specification.

### Documentation ownership model

Use existing appropriate documents where possible; create new files only when genuinely needed.

| Subject                                             | Authoritative location                          |
|-----------------------------------------------------|-------------------------------------------------|
| Project overview and navigation                     | `README.md`                                     |
| System architecture                                 | `docs/architecture.md`                          |
| Installation and configuration                      | `docs/setup.md`                                 |
| End-user operation                                  | `docs/user-guide.md`                            |
| Agent/tool execution architecture                   | Relevant existing architecture specification    |
| Security and permissions                            | `docs/security.md`                              |
| Development and contribution instructions           | `CONTRIBUTING.md`, if applicable                |
| Runtime API contract                                | Actual API schema or applicable source contract |
| Copyright and distribution license                  | Root `LICENSE`                                  |
| Third-party legal notices                           | `THIRD_PARTY_NOTICES.md`, if required           |
| Latest technical reviews and test reports           | `docs/reports/`                                 |
| Corrective and preventive actions                   | `docs/reports/corrective-preventive-actions.md` |
| Private planning and historical development records | `_private/`                                     |

These paths are preferred destinations, not instructions to duplicate already maintained authoritative documents.

Build a documentation ownership map from the actual repository.

### Required review

For each public document verify:

- Correct location and meaningful filename.
- Defined audience and purpose.
- Appropriate scope.
- Accuracy against current implementation.
- Consistent terminology.
- Consistent section ordering and heading hierarchy.
- Concise and grammatically correct wording.
- Working relative links.
- No unsupported claims.
- No contradictory definitions.
- No unnecessary repetition.
- No undisclosed private implementation knowledge.

Clearly distinguish:

**IMPLEMENTED** — confirmed by source and relevant verification.

**PLANNED** — not yet implemented.

**HISTORICAL** — past decision or development event.

**UNVERIFIED** — implementation exists but required evidence is incomplete.

Never present planned or merely coded functionality as verified behavior.

---

## 6. Documentation presentation standard

Prefer short, well-structured technical documents.

Each substantial document should use applicable elements from this standard:

1. Descriptive title.
2. Purpose and intended audience.
3. Scope or applicability.
4. Implementation/verification status where relevant.
5. Essential technical content.
6. Appropriate visual representation.
7. Related authoritative documents.
8. Verification or maintenance notes, if required.

Do not force empty sections into short documents.

### Visual documentation

Use visual elements when they materially improve understanding:

- Mermaid architecture diagrams.
- Mermaid sequence diagrams.
- State diagrams.
- Component relationship diagrams.
- Decision trees.
- Structured Markdown tables.
- Repository trees.
- Compact request/response examples.

Prefer GitHub-renderable Mermaid diagrams for architecture and workflows.

Each diagram must:

- Match actual implementation.
- Use consistent component names.
- Have clear directional meaning.
- Identify relevant boundaries.
- Avoid invented relationships.
- Remain legible in GitHub Markdown.
- Avoid duplicating the authoritative specification text.

Keep diagrams near the specification they explain.

Other documents should link to those diagrams rather than maintaining alternative copies.

### Harmonization

Apply consistent:

- Technical terminology.
- Capitalization.
- Headings.
- Tables.
- Diagram syntax.
- Code block language identifiers.
- File references.
- Relative Markdown links.
- Status terminology.

Choose a consistent documentation language, normally English for public project documentation, while preserving
meaningful user-visible localization and existing API identifiers.

---

## 7. Public versus private repository content

Create or maintain a repository-root `_private/` directory for materials that must remain local.

Ensure it is excluded from public Git publication.

Suggested internal organization, only where needed:

- `_private/planning/`
- `_private/tasks/`
- `_private/decisions/`
- `_private/agent-instructions/`
- `_private/experiments/`
- `_private/reports-history/`
- `_private/local-configuration/`

### Mandatory private classification

Move development-only materials into `_private/` when they do not serve the completed application's runtime, user
experience, public maintenance, or legally required documentation.

Examples:

- Internal planning documents.
- Historical task execution instructions.
- Temporary implementation guides.
- Internal AI prompts and Codex workflows.
- Exploratory designs not used by the application.
- Intermediate agent instructions.
- Duplicate development notes.
- Outdated working drafts.
- Superseded internal technical reports.
- Personal troubleshooting notes.

Do not relocate essential runtime agent prompts or skills simply because they use Markdown or contain agent
instructions.

Preserve necessary public contributor instructions and build documentation.

### Relocation safety

Before moving any file:

1. Check imports, resource loading, scripts, Gradle configuration, and documentation links.
2. Determine whether it is referenced at runtime or during builds.
3. Update legitimate references when moving is safe.
4. Preserve important historical information privately.
5. Confirm the destination is ignored by Git.
6. Check whether the original path is already tracked or present in Git history.
7. Verify that release packaging still works.

Do not assume relocation alone removes previously committed information from Git history.

If sensitive material has already been committed, create a publication blocker and recommend an explicit history
remediation procedure. Do not rewrite Git history automatically.

Do not delete source, records, or configuration of uncertain purpose.

Remove only clearly identified disposable files and safe generated artifacts automatically. Require approval for
destructive removal of non-generated material.

---

## 8. Security and repository hygiene

Review the public Git file set for:

- API keys and access tokens.
- Passwords and credentials.
- Private certificates and SSH material.
- Personal machine paths and usernames.
- Absolute paths embedded in public configuration.
- Local environment files.
- Database credentials.
- Temporary logs containing sensitive data.
- Private development documents.
- Generated Gradle caches.
- IntelliJ user-specific settings.
- Large model weights.
- Downloaded third-party binaries.
- Unnecessary compiled artifacts.
- Duplicate test outputs.
- Oversized files.
- Broken symlinks and submodule references.

Ensure `.gitignore` covers applicable local and generated resources.

Do not accidentally ignore required source, runtime definitions, or `docs/reports/`.

Use sanitized example configuration files when public configuration instructions are necessary.

Where secrets were previously committed, record the exposure and recommend credential rotation and history remediation.

Do not print actual secret values in public reports.

---

## 9. Legal documentation and attribution

Review root-level legal documentation before publication.

Verify the actual:

- Project copyright holder.
- Copyright year or years.
- Selected project license.
- License compatibility.
- Third-party copyright obligations.
- Redistribution conditions.
- Trademark references.
- Model licensing requirements, if applicable.

Distinguish software bundled with the plugin from separately installed tools and development-only utilities.

Review at minimum where relevant:

- llama.cpp — https://github.com/ggml-org/llama.cpp
- OpenCode — https://github.com/anomalyco/opencode
- IntelliJ Platform — https://github.com/JetBrains/intellij-community

Inspect actual build dependencies for additional licensing requirements.

Do not claim that OpenCode is bundled unless it is actually distributed with the project.

Do not assume third-party inference binaries or GGUF weights are included in the release.

Preserve applicable license texts and copyright notices when distributing third-party materials.

A non-commercial development purpose does not automatically restrict downstream commercial usage. The actual project
license controls such rights.

Do not invent the author's legal identity or select a project license without owner approval.

Create or update `THIRD_PARTY_NOTICES.md` where required by actual redistribution obligations.

Ensure the project is not presented as officially affiliated with or endorsed by JetBrains or other upstream
maintainers.

Any unresolved licensing issue is a release blocker.

---

## 10. Reports and audit consolidation

Use `docs/reports/` as the public location for the latest publishable technical reports.

Retain the latest relevant version for each report category, such as:

- Build and test verification.
- Code quality review.
- Security review.
- Documentation consistency review.
- Dependency and licensing audit.
- Publication readiness assessment.
- Corrective and preventive actions.

Do not publish redundant intermediate report versions.

Preserve older reports privately where they remain useful.

Public reports must contain validated technical observations without secrets, internal-only know-how, or personal
machine details.

### Report conventions

Each report should include, where applicable:

- Purpose.
- Scope.
- Baseline identifier or revision.
- Execution date.
- Method.
- Evidence.
- Findings.
- Status.
- Corrections applied.
- Verification results.
- Outstanding limitations.

Use consistent status values:

- PASS
- FAIL
- BLOCKED
- NOT VERIFIED
- NOT APPLICABLE

Distinguish observations from interpretations and recommendations.

Never claim a test passed unless it was actually executed successfully.

---

## 11. Corrective and Preventive Actions (CAPA)

Create or update:

`docs/reports/corrective-preventive-actions.md`

This document is the canonical register of discovered defects, inconsistencies, risks, and preventive improvements.

### CAPA record schema

Every finding must have:

| Field        | Requirement                                             |
|--------------|---------------------------------------------------------|
| ID           | Stable identifier, e.g. CAPA-001                        |
| Category     | CODE, DOC, SECURITY, LEGAL, BUILD, TEST, REPOSITORY, UX |
| Severity     | P0, P1, P2, P3                                          |
| Location     | Relevant file or component                              |
| Finding      | Specific observed nonconformity                         |
| Evidence     | Reproducible reference                                  |
| Root cause   | Verified cause or explicitly unknown                    |
| Correction   | Immediate action required                               |
| Prevention   | Repeatable measure to prevent recurrence                |
| Verification | Observable acceptance criteria                          |
| Status       | OPEN, IN PROGRESS, RESOLVED, BLOCKED                    |
| Result       | Verified outcome or remaining blocker                   |

Severity definitions:

- **P0** — Critical security, legal, data-loss, or publication blocker.
- **P1** — Functional correctness, significant quality, or major documentation failure.
- **P2** — Maintainability, consistency, or moderate repository hygiene issue.
- **P3** — Minor editorial or optional improvement.

### Required coverage

Record:

- Confirmed code defects.
- Inconsistent formatting and naming.
- Architectural violations.
- Duplicated specifications.
- Contradictory documents.
- Inadequate or obsolete instructions.
- Incorrect project status statements.
- Missing required documentation.
- Broken internal links.
- Unwanted public files.
- Missing Git exclusions.
- Missing attribution or licensing evidence.
- Unverified critical functionality.
- Incomplete or misleading test reports.
- Unsafe configuration.
- Unnecessary dependencies or artifacts.

Do not create duplicate CAPA records for the same underlying cause.

Link related findings to one canonical remediation item where appropriate.

### Reusable remediation format

Write correction and prevention instructions so that each CAPA can readily become:

- An executable Codex task.
- A focused review checklist.
- A validation script.
- A reusable skill.
- A specialized agent workflow.

Use a clear operational sequence:

**Preconditions → Inputs → Action → Expected result → Verification → Failure handling**

Prefer objective acceptance criteria over subjective statements such as "improve quality."

Do not expose private implementation details merely to make a public CAPA actionable.

Where necessary, keep detailed internal evidence in `_private/` and use sanitized references in the public report.

---

## 12. Execution workflow

Perform the workflow in this order.

### Phase A — Establish baseline

1. Inspect repository and Git state.
2. Check for concurrent edits or incomplete tasks.
3. Inventory files and dependencies.
4. Identify current verified behavior.
5. Determine the actual public release surface.
6. Identify immediate security and legal blockers.

### Phase B — Code review

1. Inspect all applicable source files.
2. Determine existing conventions.
3. Identify deviations.
4. Perform safe, minimal style corrections.
5. Avoid speculative architectural changes.
6. Record actual defects in CAPA.
7. Run focused validation.

### Phase C — Documentation review

1. Inventory documents.
2. Establish ownership of each technical subject.
3. Identify duplicates and contradictions.
4. Reconcile claims against code and verified tests.
5. Standardize structure, terminology, and style.
6. Add useful diagrams or structured visuals.
7. Replace duplicated specifications with links.
8. Verify links and document locations.

### Phase D — Public/private separation

1. Classify every publication candidate.
2. Relocate private development knowledge safely.
3. Apply appropriate Git exclusions.
4. Consolidate latest public reports.
5. Remove verified disposable resources.
6. Identify history-related exposure risks.
7. Confirm runtime/build references remain valid.

### Phase E — Legal and publication review

1. Verify project licensing.
2. Verify third-party obligations.
3. Review README and attribution.
4. Review public package contents.
5. Check secrets and personal information.
6. Resolve or report publication blockers.

### Phase F — Final verification

Use the actual project build and test configuration.

Run applicable checks, including:

- Kotlin compilation.
- Relevant automated tests.
- Gradle `check`.
- Git whitespace/error checks.
- IntelliJ plugin packaging validation where supported.
- Documentation link checks.
- Runtime-resource and configuration checks.
- Git publication surface checks.

Inspect the resulting plugin artifact where feasible.

Do not download large runtime dependencies or new models without justification and available disk capacity.

Do not repeat expensive integration tests unnecessarily if valid evidence is available and the relevant code is
unchanged.

Clearly identify tests that still require manual IntelliJ interaction.

---

## 13. Acceptance criteria

The skill is complete only when:

- Every public release candidate is classified.
- Source conventions are consistent across applicable files.
- No unintended functional change was introduced.
- Required source and runtime files remain available.
- Public documentation accurately describes implemented functionality.
- Each technical subject has one authoritative source.
- Related documents reference that source consistently.
- Documentation links are valid.
- Appropriate technical visuals are present.
- Internal development knowledge is excluded from publication.
- Generated and machine-local artifacts are excluded.
- Required legal information exists or is explicitly blocked pending owner decision.
- Third-party redistribution obligations are accounted for.
- The latest publishable reports are consolidated in `docs/reports/`.
- All identified nonconformities are represented in CAPA.
- Relevant automated checks pass or their limitations are reported.
- The final Git publication surface has been re-inspected.

Do not declare the repository publication-ready when unresolved P0 or release-blocking P1 issues remain.

---

## 14. Change authorization and safety

Allowed without additional approval:

- Correcting formatting.
- Removing unused imports.
- Fixing clear typographical mistakes.
- Repairing documentation links.
- Consolidating duplicate documentation without information loss.
- Adding useful documentation diagrams.
- Adding missing Git ignore patterns.
- Updating public reports.
- Recording CAPA findings.
- Safe relocation of verified private, non-runtime resources where their references and Git exposure have been checked.

Require approval before:

- Changing functional behavior.
- Modifying public interfaces or persisted formats.
- Altering agent/tool permissions.
- Changing runtime or MCP configuration.
- Choosing or changing the project license.
- Deleting non-generated project files.
- Removing tracked content from Git history.
- Rewriting Git history.
- Replacing build/runtime dependencies.
- Downloading substantial external components.
- Publishing or pushing any repository changes.

Never commit, push, merge, rebase, force-push, rewrite history, or publish automatically.

Preserve working local user configuration.

If another process is modifying the same files, pause conflicting modifications rather than overwriting them.

---

## 15. Final deliverables

Produce:

1. Harmonized public source files.
2. Corrected and standardized public documentation.
3. A concise, accurate root README.
4. Appropriate root-level licensing and attribution documents.
5. Git exclusions for private/generated resources.
6. Relocated private development materials.
7. Consolidated latest public audit reports.
8. `docs/reports/corrective-preventive-actions.md`.
9. Final publication readiness assessment.

### Final response format

Provide:

**Repository baseline**

- Current branch and working-tree state.
- Number of files reviewed and classified.

**Implemented corrections**

- File paths and concise change descriptions.
- Confirmation of intended behavioral preservation.

**Private/public separation**

- Public files retained.
- Files relocated privately.
- Disposable files removed.
- Remaining exposure risks.

**Documentation**

- Authoritative document ownership.
- Duplications removed.
- Diagrams and links verified.

**Legal**

- License status.
- Attribution status.
- Outstanding owner decisions.

**Validation**

- Commands executed.
- Results.
- Limitations.

**CAPA**

- Findings by severity.
- Resolved and unresolved counts.
- Remaining blockers.

**Final status**

- READY FOR PUBLICATION
- READY WITH NON-BLOCKING ISSUES
- BLOCKED

Use only evidence-supported status statements.

**Stop before Git commit, push, or publication.**
