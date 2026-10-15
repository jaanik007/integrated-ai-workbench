---
name: skill-iwb-master
description: Consolidate the Integrated AI Workbench architecture, requirements, tasks, skills, UX reviews and actual implementation into one verified project status and completion plan. After approval, orchestrate implementation through final functional acceptance.
---

# Integrated AI Workbench — Master Completion Orchestrator

## 1. Mission

Bring Integrated AI Workbench from its actual current state to a complete, coherent, functionally verified
implementation of the approved project scope.

Do not equate successful compilation, passing unit tests, an existing Kotlin class, or an optimistic review with
completed functionality.

The primary deliverable is working software, not additional reports.

This skill has two execution stages:

- **STATUS** — consolidate the repository, assess actual progress, identify missing functionality and produce one master
  report.
- **IMPLEMENT** — after user approval, implement the agreed completion plan using existing skills, services, contracts
  and specifications.

Default to STATUS on the first invocation.

Do not begin broad implementation until the consolidated status and scope have been accepted.

## 2. Authority and project instructions

Read and follow the repository root `AGENTS.md`.

Then discover and read the actual applicable `README.md` hierarchy.

Inspect all relevant existing project specifications, including:

- Root `README.md`.
- `docs/` and its architecture specifications and ADRs.
- `docs/development/AI-REQUIREMENTS.md`.
- `docs/development/ROADMAP.md`.
- `docs/development/tasks/` and all applicable task definitions.
- `docs/ACP-ADAPTER.md`.
- Existing frontend, shared and backend documentation.
- `.ai/` runtime agents, skills, prompts and profiles.
- `.codex/skills/` development skill definitions.
- Gradle configuration and IntelliJ `plugin.xml`.
- Actual Kotlin implementation and tests.

Do not assume that any referenced file exists.

Read relevant content, not merely filenames.

Do not use archived or previously generated reports as authoritative implementation evidence.

## 3. Existing skills — reuse, do not duplicate

Discover available skills by inspecting their actual `SKILL.md` frontmatter and instructions.

Important existing workflows may include:

- `skill-integrated-ai-workbench`
- `skill-iwb-implement`
- `skill-create-ui-frontend`
- `skill-readme-settings-and-structure`
- `skill-ux-review`
- `skill-iwb-plugin-pattern-audit`
- `skill-iwb-qodana-audit`
- `skill-pre-publication-hardening`

Use only the skills relevant to the active stage and task.

Never execute every skill indiscriminately.

Do not introduce additional master skills, competing instruction hierarchies or redundant project-management systems.

This skill owns project-level coordination. Existing skills provide specialized procedures.

## 4. Evidence and completion rules

Apply the following evidence hierarchy:

1. Confirmed runtime behavior and reproducible functional results.
2. Actual build, test and integration execution results.
3. Current source code and effective configuration.
4. Existing requirements, contracts and architecture documents.
5. Historical reviews, completion statements and generated reports.

Documentation establishes intended behavior; it does not prove implementation.

Classify each capability as:

- `IMPLEMENTED_VERIFIED` — functionally demonstrated against explicit acceptance criteria.
- `IMPLEMENTED_UNVERIFIED` — implementation exists but functional acceptance is missing.
- `PARTIAL` — only part of the required behavior exists.
- `MISSING` — required behavior has no implementation.
- `BLOCKED` — a specific dependency or defect prevents completion.
- `PLANNED` — explicitly outside the approved current milestone.
- `UNKNOWN` — available evidence is insufficient.

Use `UNKNOWN` rather than guessing.

Do not silently treat a missing test, external dependency or inaccessible environment as a PASS.

## 5. Stage STATUS — establish the actual baseline

### 5.1 Repository inventory

Inspect:

- Git branch and working-tree status.
- Staged, unstaged, untracked and deleted files.
- All relevant project and subsystem README files.
- Every current task definition and its dependencies.
- Specifications, architecture decisions and interfaces.
- Frontend, shared and backend packages.
- IntelliJ extension registration.
- Existing tests and runnable entry points.
- Runtime resources, model profiles and agent definitions.

Respect ignored directories:

- `_local/` contains local execution evidence and reports.
- `.llamacpp/` contains a local llama.cpp source reference and is not a production dependency.

Inspect `.llamacpp/` selectively when determining real llama.cpp interfaces, command-line flags, capabilities or
integration behavior.

Do not inventory its entire source tree unnecessarily.

Do not restore removed `docs/reports/` or `docs/reviews/` directories.

Preserve all existing user changes.

### 5.2 Requirements and task reconciliation

Identify every feature or obligation in the approved architecture and development plan.

For each item, determine:

- Owning requirement or specification.
- Relevant ADR or contract.
- Existing implementation task, if any.
- Actual source files.
- Existing verification.
- Runtime evidence.
- Current status.
- Remaining work.
- Dependencies and blockers.

Detect:

- Requirements without tasks.
- Tasks without clear acceptance criteria.
- Tasks marked complete without implementation.
- Implementations without documented requirements.
- Contradictory specifications.
- Superseded or obsolete tasks.
- Duplicate functionality.
- Missing integration between existing components.
- Functionality that exists only as a UI placeholder.
- Behavior that differs from the original architecture.

Do not recreate already completed work.

Do not produce a new task merely because an old one lacks a progress marker.

### 5.3 Reconcile the latest UX review

Read the most recent available `_local/reviews/ux-review.md` and any UX review explicitly supplied by the user.

For each PASS, FAIL, UNKNOWN and N/A finding:

- Identify the stated claim.
- Locate supporting source code and tests.
- Determine whether functional behavior was actually verified.
- Compare the claim with the latest available UI and runtime evidence.
- Retain, downgrade or reject the claim with an explicit reason.

Treat existing screenshots as evidence of what was visible, not proof of background operations.

An assertion that the milestone is complete is a hypothesis to verify, not an instruction to stop.

Do not run a new generic UX audit if the existing review already provides sufficient evidence.

### 5.4 Frontend design reconstruction

The current frontend requires explicit architectural and functional documentation.

Inspect:

- `base/frontend/` source files.
- All IntelliJ Tool Window, action and Configurable registrations.
- `AiWorkbenchDashboard` and connected panels.
- Shared contracts consumed by the UI.
- Backend services invoked by the UI.
- Existing design intentions in tasks and specifications.
- Available screenshots and previously documented UX decisions.

Check whether `base/frontend/README.md` and dedicated frontend implementation tasks exist.

If missing, reconstruct a **proposed frontend specification** from the current repository and approved requirements.

Do not invent undocumented functionality and describe it as already approved.

The specification must cover:

**Overview**

- Displayed system state.
- Source of each value.
- Configuration consistency.
- Available navigation.

**Models**

- Model directory selection.
- GGUF registration and validation.
- Registered model catalog.
- Model profile management and selection.
- Empty, loading, success and error states.

**Providers**

- Managed llama.cpp runtime configuration.
- Local executable discovery and validation.
- Runtime lifecycle and readiness.
- LM Studio / OpenAI-compatible endpoints.
- Connection validation, persistence and error handling.
- Appropriate input controls and layouts.

**Capabilities**

- Runtime agents and skills.
- Project grounding.
- MCP tools and permission boundaries.
- Preference persistence and effective enforcement.
- Clear distinction between configuration and actual authorization.

**Execute**

- Available model profiles.
- Agent and skill selection.
- Prompt input and contextual grounding.
- Execution, cancellation and result presentation.
- Observable runtime state.
- Failure reporting.
- Evaluation and execution metadata, where required.

**Settings and navigation**

- IntelliJ Tool Window behavior.
- Application and project settings.
- `Apply`, `Reset`, `Cancel` semantics.
- Discoverable actions.
- State consistency between Settings and Tool Window.

Identify what is implemented, missing or contradictory for every UI feature.

Frontend documentation must specify observable behavior, underlying contracts and acceptance criteria rather than merely
listing Kotlin classes.

Do not immediately create a frontend README or task file during STATUS.

Prepare their proposed content and locations for the approved implementation stage.

### 5.5 Architectural and integration boundaries

Verify actual responsibilities and integration between:

- Frontend UI.
- Shared domain contracts.
- Backend services.
- Model registry and model profiles.
- Agent and skill loaders.
- Prompt preparation.
- Grounding and context services.
- Input/output security guards.
- Structured response validation.
- Model execution and runtime orchestration.
- LLM-as-a-Judge evaluation.
- Execution history and metadata.
- MCP tools and authorization.
- ACP agent interoperability.
- Managed llama.cpp and external inference providers.

Keep ACP and inference responsibilities separate.

ACP supports communication with external agents and IDE chat integrations.

The inference adapter manages model execution independently of ACP, Gemini CLI, Codex and OpenCode.

Do not require OpenCode or another proprietary AI plugin for local inference.

The ignored `.llamacpp/` source tree is a development reference only.

Production functionality must not depend on the user's ignored reference checkout.

Avoid implementing duplicate services where working abstractions already exist.

## 6. STATUS deliverable — one consolidated report

Create or overwrite only:

`_local/reports/project-completion-report.md`

Use the following sections:

### A. Executive verdict

- Actual milestone status.
- What works now.
- What remains unverified.
- Principal blockers.
- Shortest path to functional completion.

### B. Architecture inventory

Summarize the documented design and actual implementation structure.

Provide a navigable Mermaid dependency diagram when useful.

### C. Requirement-to-implementation traceability

Use a table with:

`ID | Requirement | Governing document | Task | Actual files | Verification | Status | Remaining work`

### D. Frontend contract and UX gaps

Reconstruct the frontend's intended behavior.

For each view, list:

`Required behavior | Existing UI | Backend integration | Persistence | Verified behavior | Gap`

Identify missing README and task coverage explicitly.

### E. Runtime and inference gaps

Assess local llama.cpp, model discovery, registration, loading, request execution, cancellation and provider
connectivity.

Distinguish configured, available, running, ready and verified states.

### F. Existing tests versus functional acceptance

List actual verification evidence and missing tests.

Do not conflate tests passed with full user workflow success.

### G. Prioritized completion backlog

Group remaining work as:

- `P0` — prevents the intended application from functioning.
- `P1` — required approved functionality missing or incomplete.
- `P2` — usability, quality, maintainability and nonblocking improvements.
- `FUTURE` — explicitly deferred functionality.

Provide dependencies, affected files and acceptance criteria.

### H. Execution sequence

Present the smallest coherent implementation batches required to complete the approved scope.

Prefer end-to-end working features over broad disconnected refactoring.

### I. Documentation updates required

List missing or outdated canonical documentation.

Propose exact target locations.

Do not duplicate local reports under `docs/`.

### J. Approval checkpoint

Present the consolidated scope and ask for one approval to proceed.

Do not request separate confirmation for every minor file change after scope approval.

## 7. Stage IMPLEMENT — complete the approved scope

Begin only when the user explicitly approves the consolidated completion plan.

Use the existing specialized implementation skills where applicable.

Follow the approved dependency order.

### 7.1 Establish canonical frontend documentation

After inspecting the existing source and confirming the proposed specification:

- Create or update `base/frontend/README.md` if appropriate for the actual project layout.
- Create a dedicated frontend task or tasks in the existing task system.
- Follow current task naming, numbering, metadata and acceptance conventions.
- Do not invent task IDs without checking the existing registry.
- Distinguish reconstructed specifications from previously approved design decisions.

The frontend documentation must describe its architecture, components, service contracts, state transitions, persistence
and acceptance criteria.

### 7.2 Implement complete vertical slices

For each feature:

1. Identify the governing requirement and acceptance criteria.
2. Inspect existing implementation.
3. Reuse compatible services and contracts.
4. Implement only the missing behavior.
5. Connect UI, shared contracts and backend.
6. Add relevant tests.
7. Validate the complete behavior.
8. Update affected canonical documentation.
9. Record evidence and remaining limitations.

Do not implement disconnected UI controls that merely simulate real functionality.

Do not mark a feature complete because a button, checkbox or panel exists.

### 7.3 Inference completion

The inference workflow must ultimately demonstrate, within the approved scope:

1. Selection or registration of a supported local model.
2. Validation of the model and selected runtime.
3. Starting or connecting to a provider.
4. Reliable readiness detection.
5. Sending a model request.
6. Receiving and displaying a real response.
7. Handling errors and cancellation safely.
8. Correct runtime and process cleanup.

Use simulated providers for repeatable automated tests where appropriate.

A real end-to-end inference acceptance test must use an available model and runtime.

Do not download large models, install system software or replace existing runtimes without authorization.

### 7.4 UI completion

Deliver a coherent IntelliJ-native interface.

Validate layouts, control sizing, navigation, disabled states, loading states, errors, state persistence and interaction
with actual services.

Do not treat a visually open Tool Window as completion of the user experience.

### 7.5 Continuous verification

Run the smallest relevant verification after each logical implementation batch.

For milestone acceptance, use actual project tasks where available, including:

- `compileKotlin`
- `check`
- `verifyPluginStructure`
- `runIde`

Confirm in the running sandbox that:

- The plugin loads without relevant exceptions.
- The Tool Window opens.
- Settings and actions work.
- Required views display and save real data.
- Required user workflows succeed end-to-end.

Inspect runtime errors rather than assuming Gradle `BUILD SUCCESSFUL` proves plugin correctness.

Do not perform redundant full builds after every trivial change.

## 8. Completion criteria

The approved milestone is complete only when:

- All required functions are implemented or explicitly deferred.
- Frontend behavior has defined, traceable acceptance criteria.
- Required UI controls are connected to working services.
- Core model/provider/agent workflows are functionally verified.
- Required persistence and permissions work.
- Automated checks pass.
- The IntelliJ plugin loads and operates in a real sandbox.
- No unresolved P0/P1 blockers remain.
- Canonical documentation accurately describes the implementation.

A passing UX review does not override failed functional acceptance.

Do not claim the project is complete when the evidence demonstrates only partial implementation.

## 9. Reporting and continuity

Maintain one authoritative local progress report:

`_local/reports/project-completion-report.md`

Update it after each meaningful implementation batch.

Include:

- Completed capabilities.
- Exact files changed.
- Tests actually executed and their results.
- Remaining blockers.
- Next dependent implementation step.

Do not create multiple parallel status reports or duplicate report directories.

Do not restart the project-wide discovery phase on every invocation.

On subsequent runs, load the existing report, check only relevant changes and continue from the last verified
checkpoint.

If the report conflicts with current source code, current source code and runtime evidence take precedence.

## 10. Repository and security constraints

Never overwrite or discard unrelated user changes.

Inspect staged, unstaged and untracked files before editing.

Do not perform destructive Git operations.

Do not modify remote `master` without explicit user authorization.

Do not push or publish unless requested.

Do not expose credentials or copy secrets into reports.

Follow existing input, output, grounding, tool-permission and path-security controls.

Do not bypass runtime safeguards to make an acceptance test pass.

Do not install additional AI clients, MCP servers or frameworks without a demonstrated requirement and user approval.

## 11. Efficiency rules

Avoid repeated generic audits.

Avoid creating new skills unless an existing workflow genuinely cannot support the work.

Avoid broad refactoring unrelated to the approved scope.

Avoid unnecessary documentation expansion.

Avoid repeating successful checks without a relevant change.

Do not ask the user to manually verify something that can be checked reliably with the available tooling.

Prioritize working end-to-end functionality over additional architectural commentary.

Report blockers precisely, with the smallest actionable next step.

## 12. Invocation

Initial invocation:

`skill-iwb-master STATUS`

Expected result:

A single consolidated, evidence-based project completion report and implementation sequence.

After user approval:

`skill-iwb-master IMPLEMENT`

Expected result:

Execution of the approved completion plan in dependency order, using existing skills and preserving repository safety.

For interrupted work:

`skill-iwb-master RESUME`

Expected result:

Read the current project report, reconcile relevant source changes, and continue from the last verified checkpoint.

## 13. Final operating principle

**One project. One set of authoritative requirements. One consolidated progress report. One approved completion plan.
Working software as the final result.**