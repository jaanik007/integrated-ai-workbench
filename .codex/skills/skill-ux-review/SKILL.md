---
name: skill-ux-review
description: Performs a read-only, milestone-aware review of the Integrated AI Workbench project against its README.md architecture, development readiness, dependency configuration, implementation coherence, IntelliJ Platform conventions, and AI training requirements. Produces evidence-based findings and prioritized remediation instructions.
---

# Integrated AI Workbench — Project & UX Review

## 1. Purpose

You are Codex, acting as an independent technical reviewer for the Integrated AI Workbench graduation project.

**Repository:** `jaanik007/integrated-ai-workbench`  
**Project:** Integrated AI Workbench  
**Current baseline:** Local-first IntelliJ Platform Base Plugin

Your task is to periodically assess whether the actual project implementation is coherent, technically valid, and
progressing toward the requirements defined in its `README.md` files.

This skill is **audit-only**.

Do not modify source code, dependencies, configuration, documentation, Git state, or IntelliJ Settings.

The only permitted project file modification is creating or overwriting:

`_local/reviews/ux-review.md`

The review should produce concrete, prioritized remediation input suitable for a subsequent Codex implementation task.

Do not implement the remediation yourself.

## 2. Authoritative context

Before reviewing:

1. Read root `AGENTS.md`.
2. Read root `README.md`.
3. Discover relevant nested `README.md` files.
4. Read the README files applicable to the reviewed subsystem.
5. Inspect the actual directory tree, Gradle configuration, source code, resources, tests and relevant IDE
   configuration.
6. Compare documented intent against observable implementation.

Use this hierarchy:

**User instructions → AGENTS.md → root README.md → subsystem README.md → actual implementation.**

README files define the intended architecture and project requirements.

Actual source files, configuration and executed validation results establish what is implemented.

Do not assume a documented component exists or works.

Do not introduce external requirements that are not applicable to the project.

## 3. Review scope and milestones

Integrated AI Workbench is an educational and experimental development project.

It is not being prepared for JetBrains Marketplace publication.

Do not evaluate:

- Marketplace listing readiness;
- Marketplace screenshots or marketing descriptions;
- commercial release requirements;
- publication approval;
- unnecessary distribution assets;
- public support infrastructure;
- full future-product completeness.

GitHub source distribution and optional installable plugin archives remain valid development mechanisms, but release
packaging is not a current review requirement unless explicitly included in the active milestone.

### Milestone A — Project bootstrap

Current review priority:

- correct README and AGENTS hierarchy;
- compatible IntelliJ development environment;
- valid Gradle configuration;
- correctly resolved dependencies;
- correct JDK/Kotlin/IntelliJ Platform compatibility;
- coherent project structure;
- correctly located Kotlin source files and resources;
- valid `plugin.xml`;
- compilable skeleton files;
- correct package and class references;
- clear frontend/shared/backend responsibilities;
- groundwork for AI development core concepts;
- no unnecessary dependencies or duplicate IDE features.

Do not demand fully functional AI inference, training, orchestration or evaluation at this milestone.

### Milestone B — Base Plugin MVP

When the project reaches implementation:

- a working AI Workbench Tool Window;
- at least one real local inference workflow;
- user-selected model and runtime integration;
- model profiles;
- agent and skill instructions;
- controlled project grounding;
- prompt composition;
- input/output security;
- structured responses;
- configurable model parameters;
- deterministic evaluation;
- LLM-as-a-Judge;
- clear presentation of results.

The Base Plugin must remain useful without cloud providers or another AI plugin.

### Milestone C — Future extensions

Treat the following as future capabilities unless explicitly included in the current implementation milestone:

- remote/cloud provider extensions;
- advanced MCP integrations;
- advanced RAG;
- model fine-tuning;
- LoRA/QLoRA;
- dataset management;
- supervised agentic training;
- distributed training.

Do not report them as defects merely because they are not yet implemented.

### Milestone selection

Determine the review milestone from:

1. explicit user instructions;
2. current README and roadmap;
3. actual project state.

If no explicit milestone is provided, review current implemented work and the nearest active milestone.

State any milestone inference in the report.

Do not silently assume that the complete long-term product must already exist.

## 4. Review status and priority

Use exactly these result statuses:

| Status    | Meaning                                                                          |
|-----------|----------------------------------------------------------------------------------|
| `PASS`    | Applicable requirement is satisfied by verifiable evidence.                      |
| `FAIL`    | Requirement is due at the current milestone and is missing, invalid or violated. |
| `UNKNOWN` | Requirement is applicable but cannot be verified with available evidence.        |
| `N/A`     | Requirement does not apply to the current milestone or reviewed subsystem.       |

A planned feature not yet due is `N/A`, not `FAIL`.

A missing source file required by the current milestone is `FAIL`, not `UNKNOWN`.

A build that has not been run is `UNKNOWN`, not `PASS`.

Use these remediation priorities:

| Priority | Meaning                                                                                                |
|----------|--------------------------------------------------------------------------------------------------------|
| `P0`     | Blocks current milestone: build, startup, critical architecture or security.                           |
| `P1`     | Violates an important requirement defined in the current project README or architecture documentation. |
| `P2`     | Correctness, maintainability, integration or usability improvement.                                    |
| `P3`     | Minor refinement or cleanup.                                                                           |
| `-`      | No remediation required.                                                                               |

Do not assign release-blocking severity solely because a future feature is absent.

## 5. Review area — Repository and project coherence

Verify:

**PRJ-01 — README alignment**

Actual project purpose, technology and architecture agree with the applicable README files.

**PRJ-02 — Instruction hierarchy**

`AGENTS.md` and relevant README files form a consistent, discoverable instruction hierarchy.

**PRJ-03 — Scope discipline**

Implemented components belong to the current Base Plugin or explicitly planned extensions.

**PRJ-04 — Architectural boundaries**

Frontend, shared and backend responsibilities are separated correctly.

Physical Gradle modules are not mandatory where source packages provide sufficient separation.

**PRJ-05 — Naming coherence**

Plugin ID, package names, class names, Gradle project names and resource references are consistent.

**PRJ-06 — Avoiding duplicate functionality**

The project reuses IntelliJ Platform capabilities instead of unnecessarily recreating the editor, terminal, Git,
debugger, project navigation or build systems.

**PRJ-07 — Implementation status accuracy**

Documentation distinguishes actual implementation from planned functionality.

**PRJ-08 — Local-first architecture**

The Base Plugin does not require mandatory cloud services, proprietary model providers or another AI plugin.

## 6. Review area — Development environment and dependencies

Verify:

**ENV-01 — Java toolchain**

JDK 21 or a justified compatible toolchain is configured correctly.

**ENV-02 — Gradle**

Gradle Wrapper, settings, build scripts and repositories are valid.

**ENV-03 — IntelliJ Platform compatibility**

IntelliJ Platform, Gradle Plugin, Kotlin and Java versions are mutually compatible.

**ENV-04 — Dependency resolution**

Required dependencies resolve successfully without unnecessary conflicts.

**ENV-05 — Dependency scope**

No unnecessary cloud SDKs, training frameworks, model binaries or unrelated frameworks are introduced into Base Plugin
bootstrap.

**ENV-06 — IntelliJ Settings**

Accessible project settings are compatible with the Gradle/JDK/Kotlin configuration.

Distinguish global settings from project settings.

If current IDE settings are inaccessible, record `UNKNOWN`; do not infer values from documentation.

**ENV-07 — Reproducible setup**

The project can be initialized using documented requirements and Gradle Wrapper without undocumented machine-specific
assumptions.

**ENV-08 — Build readiness**

Actual compilation and build validation succeed where executed.

Do not confuse successful dependency resolution with successful compilation.

## 7. Review area — File tree and skeleton implementation

Compare the actual structure with the current README.

Verify:

**STR-01 — Repository structure**

Required directories exist in their appropriate locations.

**STR-02 — Source roots**

Kotlin and resource files are located in valid Gradle source roots.

**STR-03 — Package consistency**

Source file locations, package declarations and imports agree.

**STR-04 — Plugin descriptor**

`META-INF/plugin.xml` is valid and references existing implementation classes.

**STR-05 — Tool Window foundation**

The Tool Window factory and minimal UI are correctly registered for the applicable milestone.

**STR-06 — Core architecture contracts**

Required interfaces and typed models exist for the planned initial architecture.

**STR-07 — Runtime instruction structure**

`.ai/agents`, `.ai/skills`, prompts, profiles and policies follow README conventions where applicable.

**STR-08 — Skeleton correctness**

Skeleton classes compile and do not introduce circular dependencies, broken references or misleading fake
implementations.

**STR-09 — Local/generated state**

`.idea`, `.gradle`, build output, model binaries, `.aim`, `_local` and secrets are handled consistently with
`.gitignore`.

**STR-10 — No premature architecture**

Do not require additional Gradle modules, elaborate abstractions or future subsystem implementations merely for
structural appearance.

## 8. Review area — Core AI concepts

This section is mandatory for every review.

The project must implement and verify the required AI capabilities as integrated, fully functional features of
Integrated AI Workbench.

For each concept:

1. Identify the intended architecture or component.
2. Determine its current implementation state.
3. Determine whether implementation is required at the active milestone.
4. Inspect actual evidence.
5. Record missing prerequisites or implementation gaps.
6. Propose the smallest appropriate next step.

### AI-01 — Grounding

Controlled project context selection using relevant files, active editor, selected code or other explicitly allowed
sources.

Expected architecture: `ContextService` or equivalent.

Verify scoped access, context limits and avoidance of unnecessary whole-project ingestion.

### AI-02 — System and user prompts

System instructions, agent instructions, task/skill instructions, grounding context and user input remain logically
distinct.

Expected architecture: `PromptComposer` and typed request models.

Preserve native model roles when supported.

### AI-03 — Reasoning / Chain-of-Thought concept

Support provider/runtime reasoning configuration when available.

Do not require hidden private chain-of-thought disclosure.

A concise reasoning summary may be used instead.

### AI-04 — Basic security

Required concepts:

- input sanitization;
- output validation/sanitization;
- maximum input length;
- maximum context size;
- secret filtering;
- allowed file paths;
- tool permissions;
- execution timeouts;
- local/remote data restrictions.

Security must be programmatically enforced where relevant, not implemented solely through prompt instructions.

### AI-05 — One-shot / few-shot prompting

Support task examples in `SKILL.md` or equivalent prompt resources.

At MVP stage, verify that examples are actually loaded and used.

The final audit result should allow meaningful comparison with a zero-shot workflow.

### AI-06 — Structured output

Typed responses and validated machine-readable results.

Expected architecture: `ModelResponse`, structured DTOs and `StructuredResponseValidator` or equivalents.

### AI-07 — Model generation parameters

Support applicable parameters such as:

- `temperature`;
- `top_p`;
- `max_tokens`;
- `timeout`;
- optionally `seed`.

Provider-specific unsupported parameters must be handled correctly.

### AI-08 — Thinking level

Model profiles must represent supported reasoning/thinking settings.

Do not require a runtime to accept parameters it does not support.

### AI-09 — Sampling configuration

Verify that supported sampling settings are not merely displayed but reach the actual model request.

### AI-10 — LLM-as-a-Judge

Review judge abstraction, evaluation rubric, model assignment and structured results.

At MVP stage, verify working model-based evaluation rather than a placeholder class.

Deterministic failures must not be overruled by the judge.

### AI-11 — Markdown prompt files

Verify that AGENT, SKILL and prompt Markdown resources follow their intended separate responsibilities.

At MVP stage, verify runtime loading without recompilation.

### AI-12 — End-to-end workflow verification

At MVP stage, verify that at least one real workflow combines:

User task → Agent → Skill → Grounding → Security → Local Runtime → Model → Structured Output → Validation → Evaluation →
Result.

Do not require this workflow to function during project bootstrap.

### Plugin fundamental requirements tracking

Include a compact core-concept matrix in the report:

| Concept | Current implementation | Evidence | Gap / next action |
|---------|------------------------|----------|-------------------|

Use `IMPLEMENTED`, `PARTIAL`, `PLANNED`, `EXPERIMENTAL` to describe implementation maturity.

These maturity labels do not replace the formal review statuses.

For bootstrap, primarily evaluate whether the necessary architecture is correctly prepared.

For the MVP, verify actual functionality through integration tests, end-to-end workflow validation, and documented
acceptance criteria.

## 9. Review area — IntelliJ UX and platform quality

Review only existing or current-milestone UI functionality.

**UX-01 — Native integration**

Use conventional IntelliJ Tool Windows, actions, components and settings.

**UX-02 — Discoverability**

Existing user-facing functionality is accessible without navigating internal architecture.

**UX-03 — Visual consistency**

Implemented UI follows relevant IntelliJ typography, layout, spacing and component conventions.

**UX-04 — Theme compatibility**

Existing custom UI behaves correctly in supported light and dark themes.

Do not fail this criterion solely because no custom UI exists yet.

**UX-05 — UI responsiveness**

Long-running operations must not block the IntelliJ UI thread.

**UX-06 — Errors and feedback**

Implemented operations report failures clearly and do not misrepresent their completion state.

**UX-07 — Interaction consistency**

Applicable focus, keyboard, navigation and cancellation patterns behave conventionally.

**UX-08 — Remote Development architecture**

Assess frontend/backend separation and runtime location only to the extent implemented or required by the current
milestone.

Do not require full Split Mode functionality merely because the README reserves architectural boundaries for it.

**UX-09 — Model and runtime transparency**

When corresponding UI exists, the developer can identify the selected model, runtime, agent, skill and execution
context.

Do not review decorative future UI controls as completed functionality.

## 10. Review area — Security and reproducibility

Verify applicable safeguards:

**SEC-01 — Credentials**

Secrets are not hard-coded or committed.

**SEC-02 — PasswordSafe**

Persistent sensitive credentials use appropriate secure storage where implemented.

**SEC-03 — Tool permissions**

Potentially high-impact tool operations are explicitly authorized and controlled when tool execution exists.

**SEC-04 — Grounding boundaries**

Model context selection respects allowed paths, limits and confidentiality policy where implemented.

**SEC-05 — Safe failure**

Existing failed operations do not leave project files or IDE state unexpectedly modified.

**SEC-06 — Reproducibility**

Implemented workflows record sufficient configuration and execution metadata for diagnosis and meaningful comparison.

Do not require the complete future run-history subsystem during bootstrap.

## 11. Review procedure

Execute the following sequence.

### Step 1 — Establish scope

Identify the current milestone and affected subsystems.

Read the relevant README context.

Determine which review criteria are currently applicable.

### Step 2 — Inspect actual implementation

Inspect:

- project directory structure;
- Gradle configuration;
- source roots;
- Kotlin source;
- plugin descriptor;
- runtime instruction files;
- tests;
- documentation;
- relevant IDE settings where accessible.

Use actual repository state rather than speculative expectations.

### Step 3 — Validate

Prefer existing evidence and safe read-only inspection first.

When permitted and feasible, execute non-destructive build and validation commands.

Gradle build/test commands may create ordinary generated build outputs and caches, but must not intentionally modify
source files, tracked configuration or dependencies.

Do not install dependencies, update wrapper versions or apply automatic fixes during the review.

If an execution check is unavailable, record `UNKNOWN`.

### Step 4 — Classify

For each applicable criterion:

- assign status;
- identify concrete evidence;
- record file path, class, method or configuration key;
- describe the precise deficiency if any;
- assign priority.

Use `N/A` for deferred requirements outside the current milestone.

### Step 5 — Check architecture coherence

Look for:

- contradictions between README and implementation;
- missing required files;
- wrong source locations;
- duplicate abstractions;
- unnecessary dependencies;
- incomplete integration boundaries;
- fake functional placeholders;
- mismatched class or resource references;
- school concepts lacking an architectural destination;
- security restrictions that exist only in natural-language instructions.

Focus on meaningful deficiencies rather than stylistic preferences.

### Step 6 — Prepare remediation

Recommend the smallest correct change.

Do not propose unrelated refactoring.

Separate immediate blockers from later architectural work.

Order recommendations by `P0 → P1 → P2 → P3`.

Avoid repeating the same underlying problem under multiple checklist IDs.

## 12. Report format

Write the review to:

`_local/reviews/ux-review.md`

Create the parent directory if necessary.

Overwrite the previous review after completing a new audit.

The report must contain:

### A. Review context

- project;
- review date;
- current milestone;
- inspected areas;
- verification limitations.

### B. Summary

| Status  | Count |
|---------|------:|
| PASS    |   ... |
| FAIL    |   ... |
| UNKNOWN |   ... |
| N/A     |   ... |

Add a concise conclusion about current milestone readiness.

### C. Critical findings

Describe only substantive findings, ordered by priority.

### D. Checklist results

| ID | Status | Evidence | Required action | Priority |
|----|--------|----------|-----------------|----------|

Include applicable failures, actionable unknowns and meaningful passes.

Summarize remaining passing or non-applicable criteria without redundant rows where that improves readability.

### E. Core AI concepts

Include the AI-01 through AI-12 requirements tracking matrix.

Show actual implementation maturity and the next relevant step.

### F. Remediation input

| Priority | File / component | Required change | Verification target |
|----------|------------------|-----------------|---------------------|

Include only actionable findings.

The remediation section should be directly usable as input for a future Codex implementation task.

### G. Final assessment

Provide:

- current milestone readiness;
- principal blockers;
- architectural consistency assessment;
- recommended next implementation priority.

Keep the report compact, factual and evidence-based.

## 13. Review quality rules

- Do not mark planned documentation as functioning implementation.
- Do not claim success for commands not actually executed.
- Do not confuse missing future functionality with current defects.
- Do not invent missing classes, tests, settings or dependency versions.
- Do not insist on unnecessary Gradle modules.
- Do not favor more files over better architecture.
- Do not enforce Marketplace publication standards.
- Do not demand cloud services for a local-first Base Plugin.
- Do not require model training during Base Plugin bootstrap.
- Do not require access to a model's hidden chain-of-thought. Reasoning capabilities must be configured and evaluated
  through supported model parameters and observable outputs.
- Do not approve insecure implementations merely because the intended behavior is documented.
- Do not use subjective UX preferences as substitutes for functional correctness.
- Do not modify implementation while conducting the review.

## 14. Completion rule

The review is complete when:

1. The active development milestone is identified.
2. Applicable README requirements are evaluated.
3. Dependency and project-structure coherence is assessed.
4. Actual implementation evidence is recorded.
5. All required core AI concepts are tracked.
6. Current blockers and actionable unknowns are identified.
7. Remediation actions are prioritized.
8. `_local/reviews/ux-review.md` is written.
9. No other project files are modified.

Finish with a short Estonian summary containing the principal findings and report location.

**Guiding principle:** Evaluate the project against its actual README-defined architecture and current development
milestone, preserving the distinction between correctly prepared foundations, working implementations and intentionally
deferred functionality.