---
name: skill-iwb-implement
description: Implement or resume individual or all TASK-001 through TASK-011 for the Integrated AI Workbench Kotlin IntelliJ Platform plugin. Respect dependencies, preserve verified work, compile, test, verify acceptance criteria, and maintain the central task backlog and local execution reports. Supports TASK-XXX and ALL modes.
---

# Integrated AI Workbench — Task Implementation

## Purpose

Implement the Integrated AI Workbench Base Plugin MVP through its existing TASK-001 to TASK-011 specifications.

The project uses a clear separation of responsibilities:

- Product and architecture documentation defines the stable framework.
- `docs/development/tasks/README.md` is the central operational development backlog.
- Individual `TASK-XXX` specifications define implementation requirements and acceptance criteria.
- This skill defines the implementation and verification procedure.
- `_local/reports/task-reports/` stores execution evidence and resumable progress.

Support dependency-aware execution without compromising verification requirements.

Preserve the established architecture and previously verified functionality.

Do not generate unnecessary documentation, duplicate task specifications, or reorganize the project.

## 1. Invocation Modes

### Individual task

`$skill-iwb-implement TASK-XXX`

Execute or resume only the selected task.

Use the corresponding specification, existing implementation, dependency state, and available verification evidence.

Do not automatically execute unrelated tasks in this mode.

### Complete backlog

`$skill-iwb-implement ALL`

Process TASK-001 through TASK-011 automatically, respecting their dependencies.

Continue with the next eligible task after completing the current task.

If a task is blocked, continue with another independent eligible task where safe.

Stop when:

- All tasks are VERIFIED.
- No remaining task can safely progress.
- Further progress requires user authorization or unavailable resources.
- The execution environment cannot safely continue.

Do not enter endless retry loops.

### Resume

`$skill-iwb-implement ALL`

The same invocation resumes interrupted or partially completed backlog execution.

Reinspect the repository, current backlog, task specifications, and existing verification evidence.

Preserve completed work and do not repeat already verified implementation unnecessarily.

A prior BLOCKED status must be reconsidered when its dependencies or resources have changed.

### Invalid invocation

If neither ALL nor a valid TASK-001 through TASK-011 identifier is supplied, request clarification.

Invocation does not grant unrestricted filesystem, terminal, Git, network, or tool permissions.

## 2. Project Context

The target is the existing Integrated AI Workbench IntelliJ Platform plugin.

Development environment:

- Windows workstation.
- IntelliJ IDEA Ultimate.
- Kotlin and JDK 21.
- Existing Gradle Wrapper.
- Single Gradle module.
- Source roots: `base/frontend`, `base/shared`, `base/backend`.
- Workbench-managed local inference.
- Runtime agents and skills defined under `.ai/`.
- IntelliJ IDEA MCP integration through the selected STDIO transport.

This is not a Python, Spring Boot, or web frontend project.

### Required orientation

Read:

1. Root `AGENTS.md` and `README.md`.
2. `docs/development/tasks/README.md`.
3. The selected TASK specification.
4. Relevant existing Kotlin code and tests.
5. Only the architecture and security documents needed for the task.

Stable reference documents include:

- `docs/architecture/README.md`
- `docs/architecture/ADR-001-managed-local-runtime.md`
- `docs/security/README.md`
- `docs/development/README.md`
- `docs/development/ROADMAP.md`
- `docs/development/AI-REQUIREMENTS.md`

These documents provide constraints and architectural context. They are not routine task-status files.

Inspect applicable `.ai/` configuration when working with models, agents, skills, grounding, prompting, security, tool
permissions, or evaluation.

Runtime resources under `.ai/` are separate from developer automation under `.codex/skills/`.

After initial orientation, read only task-relevant documentation and source files.

Do not repeatedly scan the entire repository or inspect every installed development skill.

## 3. Documentation Authority and Change Control

### Central operational document

`docs/development/tasks/README.md` is the single central entry point for ongoing development management.

It tracks:

- Active development scope.
- Task identifiers and priorities.
- Dependencies.
- Current task statuses.
- Newly eligible work.
- Blockers and next actions.
- Approved changes affecting implementation order.

Use its current contents together with individual TASK specifications and actual verification reports.

Do not maintain competing development-status summaries across multiple documentation files.

### Individual task specifications

`docs/development/tasks/TASK-XXX-*.md` defines:

- Objective.
- Functional requirements.
- Implementation boundaries.
- Dependencies.
- Acceptance criteria.
- Required verification.
- Completion evidence.

Treat accepted task requirements and acceptance criteria as stable during implementation.

Do not silently add, remove, or weaken acceptance criteria.

Task specifications may receive concise completion-status and verification updates after actual work.

A change to task scope or acceptance criteria requires explicit justification and applicable approval.

### Stable project documentation

The following are architectural and product references, not routine implementation logs:

- Root `README.md`.
- `docs/architecture/`.
- `docs/security/`.
- `docs/development/README.md`.
- `docs/development/ROADMAP.md`.
- `docs/development/AI-REQUIREMENTS.md`.

**Read these documents; do not automatically rewrite them after implementing a task.**

Modify a stable document only when:

1. A genuine product requirement or architectural boundary changes.
2. The change is necessary and directly relevant.
3. Its scope is explicitly authorized.
4. The modification cannot be represented adequately in the central backlog or applicable TASK specification.

Do not update stable documents merely because a TASK changes from READY to VERIFIED.

Do not propagate routine status changes into AI capability tables, ROADMAP sections, architecture descriptions, or root
README summaries.

Accepted ADRs remain historical decision records. A substantial new or superseding architectural decision should receive
an appropriately authorized ADR rather than silently rewriting an accepted decision.

### Local execution evidence

`_local/reports/task-reports/` contains the latest actual implementation and verification results.

Reports are local execution records, not architectural specifications.

They must remain excluded from Git.

### Change requests outside current tasks

If requested functionality does not fit an existing TASK:

1. Identify the scope mismatch.
2. Check the central backlog.
3. Explain which requirement or task would need updating.
4. Obtain applicable approval before expanding implementation scope.
5. Do not create additional tasks or rewrite architecture documentation automatically.

## 4. Repository Safety

Before modifying files, inspect:

`git status --short`

Preserve existing user changes and the current Git branch.

Do not:

- Commit, push, merge, rebase, reset, or delete Git branches.
- Rewrite unrelated source code.
- Overwrite uncommitted user work.
- Download language models.
- Install unapproved software.
- Introduce mandatory cloud services.
- Require independently operated AI applications.
- Add unverified runtime dependencies.
- Expose credentials, secrets, or sensitive model data.
- Create unnecessary documentation or temporary artifacts.
- Modify the remote repository.
- Change global IntelliJ IDEA settings without authorization.

Use the existing architecture and the smallest coherent implementation.

Consequential architectural decisions, licensing changes, runtime acquisition, or new third-party dependencies require
documented justification and explicit approval where applicable.

If approval is unavailable, record the blocker and continue with independent eligible work in ALL mode.

## 5. Task Selection and Dependencies

### Initialization

At the beginning of execution:

1. Read the central backlog.
2. Identify the selected TASK or all 11 TASK specifications.
3. Inspect declared dependencies.
4. Inspect current statuses and verification evidence.
5. Identify partially implemented work.
6. Inspect Git working-tree changes.
7. Determine which tasks are eligible.
8. Locate existing local task reports.

Use actual repository evidence rather than assuming a prior report is current.

### Dependency rules

A task is eligible when its mandatory prerequisites are VERIFIED with supporting evidence.

The dependency graph is maintained in `docs/development/tasks/README.md`.

Do not begin a task whose mandatory dependencies remain unverified.

Prefer ascending task numbers among eligible tasks unless the central backlog specifies a different approved priority.

### Verified tasks

If a task is already VERIFIED:

- Check whether its existing evidence remains applicable.
- Preserve verified functionality.
- Do not reimplement it unnecessarily.
- Avoid reopening its scope merely because later tasks extend the overall system.

A later integration task may extend an existing contract without invalidating the earlier task's completed work.

### Incomplete tasks

For partially completed work:

- Inspect existing files and tests.
- Determine which acceptance criteria remain unsatisfied.
- Continue from the current implementation.
- Avoid rewriting functioning components.
- Execute missing verification.
- Record remaining blockers accurately.

### Blocked tasks

If a task cannot proceed:

- Identify the exact dependency, resource, approval, or technical limitation.
- Record BLOCKED or IN PROGRESS as appropriate.
- Update its local report.
- Do not bypass the blocker.
- Continue with another independent eligible task in ALL mode.

Do not mark a task VERIFIED merely because source files exist or compilation succeeds.

## 6. Continuous ALL-Mode Execution

For each eligible task:

1. Inspect its existing implementation.
2. Determine missing acceptance criteria.
3. Use applicable development skills.
4. Implement required changes.
5. Compile.
6. Create or update focused tests.
7. Execute mandatory verification.
8. Review every acceptance criterion.
9. Update the TASK completion record.
10. Update the central backlog status.
11. Write the local task report.
12. Reevaluate dependent tasks.
13. Continue to the next eligible task.

Do not request confirmation between ordinary dependency-ready tasks.

Stop for approvals required by repository policy or implementation boundaries.

### End condition

Stop ALL mode when:

- All 11 tasks are VERIFIED.
- No further eligible task can safely progress.
- Required resources or approvals are unavailable.
- The execution environment cannot safely continue.

Before stopping, preserve accurate progress and report outstanding work.

Do not conceal incomplete tasks or repeat failed operations indefinitely.

## 7. Specialized Development Skills

Use relevant installed Codex skills when they materially improve implementation or verification.

Potential supporting skills include:

- `project-context-ingestion` — unfamiliar code boundaries.
- `codebase-audit` — existing contracts and implementation consistency.
- `test-suite-builder` — focused automated tests.
- `gradle-kotlin-dsl-doctor` — Gradle and Kotlin diagnostics.
- `debugging-code` — concrete implementation failures.
- `stacktrace-log-triage` — JVM and runtime errors.
- `ij-debugger` — IntelliJ Platform debugging.
- `security-threat-model` — security-sensitive functionality.
- `jackson-kotlin-serialization-specialist` — Jackson integration when applicable.
- `mcp-builder` — MCP implementation guidance.
- `review-agent` — independent code review.
- `update-docs` — authorized documentation maintenance.

Verify skill availability in the current environment.

Read the instructions of selected supporting skills before using them.

Do not assume an installed skill provides a runtime implementation or automatically exposes IDE tools.

Do not inspect all installed skills indiscriminately.

Do not use a skill outside its actual technology scope.

No additional skill installation is authorized by this workflow.

Record only supporting skills actually used.

## 8. Implementation Rules

For each selected task:

- Inspect established contracts and services.
- Reuse existing implementations.
- Make minimal coherent source changes.
- Preserve backward compatibility where required.
- Maintain provider-neutral inference boundaries.
- Enforce security programmatically.
- Handle relevant failures, cancellation, and cleanup.
- Avoid duplicate components and speculative abstractions.
- Keep code, comments, identifiers, and project documentation in English.

Do not implement unrelated future capabilities prematurely.

A minimal supporting change is acceptable only when necessary for the selected task and consistent with established
contracts.

Do not present placeholders, mocked integrations, or simulated outputs as completed product functionality.

### Existing IDE tools

Prefer IntelliJ IDEA's existing exposed tools over custom implementations of equivalent IDE operations.

The Workbench uses an MCP client to access authorized IDE tools.

For TASK-009 and applicable integrated verification, use the selected STDIO transport described in the task
specification.

Do not implement a second MCP Server or switch transports without a verified technical reason and applicable approval.

Machine-specific executable paths, project paths, and ports must remain configuration data rather than hardcoded
application defaults.

### Runtime tool authorization

For functionality involving agent tool execution:

- Respect actual MCP tool exposure.
- Respect runtime agent permissions.
- Validate tool schemas and arguments.
- Enforce Workbench security policy.
- Require operation-specific approval for consequential operations.
- Bound tool execution and model continuation.
- Handle timeouts, cancellation, disconnection, and cleanup.
- Treat tool results as untrusted data.

Do not interpret arbitrary free-form model text as an executable tool command.

The model must not grant itself permissions.

## 9. Compile and Test

Compile after implementing a coherent change.

Default Windows command:

`.\gradlew.bat compileKotlin`

If compilation fails, investigate and correct task-scoped problems.

Create or update focused automated tests for mandatory acceptance criteria.

Cover applicable:

- Successful behavior.
- Invalid inputs.
- Boundary values.
- Compatibility failures.
- Security rejection.
- Authorization failures.
- Cancellation and timeouts.
- Cleanup and recovery.
- Contract compatibility.

Run focused tests first.

Then run:

`.\gradlew.bat check`

Execute any additional verification required by the selected task, including plugin checks, Development Instance
inspection, local-model execution, and real MCP integration.

Deterministic fixtures and fake adapters are permitted where appropriate.

They do not replace mandatory real integration verification.

### Failure handling

When compilation or verification fails:

1. Identify the concrete failure.
2. Correct task-scoped problems.
3. Repeat the affected check.
4. Record unresolved failures if safe correction is unavailable.

Avoid repeated broad rewrites and unbounded debugging cycles.

In ALL mode, an unresolved task remains incomplete. Independent eligible tasks may continue.

Do not report unexecuted checks as passing.

## 10. Real Integration Requirements

### Managed local inference

TASK-004 establishes the Workbench-managed runtime.

TASK-005 requires successful inference using a real compatible user-supplied local model.

Use the established managed runtime rather than an independently operated AI application.

Do not download model binaries or introduce mandatory cloud services to bypass local verification requirements.

### IntelliJ MCP tool execution

TASK-009 requires real operational tool execution through the Workbench agent pipeline.

The selected MCP transport is STDIO.

Verify:

1. The configured IntelliJ IDEA MCP Server is available.
2. The STDIO connection targets the intended IDE instance and project.
3. An actual enabled IDE tool and its schema can be discovered.
4. The selected runtime agent is permitted to use the tool.
5. A real local model produces a valid structured tool-call proposal.
6. Workbench validates and authorizes the proposed operation.
7. The actual IDE tool executes through MCP.
8. The real tool result is validated and returned to the model.
9. The local model continues inference and produces a validated final response.
10. Unauthorized requests, approval requirements, cancellation, and cleanup behave correctly.

Do not assume that the main IntelliJ IDEA instance and the Gradle Development Instance share the same MCP port, project,
or server.

Do not modify IntelliJ MCP exposure settings merely to force a passing test.

Do not substitute a fake MCP response for a mandatory real tool-call verification.

### Evaluation

TASK-010 implements deterministic evaluation and local LLM-as-a-Judge integration.

Deterministic security failures must take precedence over judge assessments.

Do not fabricate judge scores or treat missing judge output as successful verification.

### Integrated MVP

TASK-011 requires integrated verification of:

- Managed local inference.
- Runtime agents and skills.
- Grounding and prompt composition.
- Structured output and security.
- Real IDE MCP tool execution.
- Model continuation after tool results.
- Deterministic evaluation.
- Applicable real local judge execution.
- UI operation, cancellation, failures, and cleanup.

A successful build or isolated component test does not establish MVP completion.

### Missing resources

If a required model, runtime, MCP environment, or verification resource is unavailable:

- Record the exact limitation.
- Leave the affected task incomplete.
- Do not fabricate evidence.
- Continue eligible independent work in ALL mode.

## 11. Acceptance and Status

Evaluate every mandatory acceptance criterion independently.

Use the existing task statuses:

- READY — prerequisites satisfied; implementation can begin.
- BLOCKED — missing prerequisites or resources prevent progress.
- IN PROGRESS — implementation or required verification remains incomplete.
- VERIFIED — all mandatory acceptance criteria satisfied with recorded evidence.

Only mark a task VERIFIED when required checks have succeeded.

Compilation alone is insufficient.

An existing implementation must have adequate verification evidence before serving as a VERIFIED dependency.

In ALL mode, update newly eligible dependent tasks to READY when applicable.

Synchronize the operational status between:

1. The selected TASK completion record.
2. `docs/development/tasks/README.md`.
3. The corresponding local task report.

Do not update unrelated documentation merely to repeat the status.

Preserve the task's accepted requirements and acceptance criteria.

## 12. Local Task Reports

Report directory:

`_local/reports/task-reports/`

Use one report per task:

`task-001.md` through `task-011.md`

The latest execution replaces the corresponding report with its consolidated current state.

### ALL-mode report coverage

At the beginning of ALL mode, inspect existing evidence for all 11 tasks.

Initialize missing reports using only available factual information.

During execution, update the report after each processed task.

Before stopping, ensure reports distinguish:

- Verified tasks.
- Incomplete implementations.
- Blocked tasks.
- Tasks not attempted during the current run.

Do not invent test results.

### Report format

Keep each report compact, preferably within 20 lines.

Use:

**# TASK-XXX — Task title**

**Status:** READY / BLOCKED / IN PROGRESS / VERIFIED  
**Date:** YYYY-MM-DD  
**Execution:** COMPLETED / PARTIAL / BLOCKED / NOT ATTEMPTED

**Implemented:** Actual changes or none.  
**Files:** Main affected components.  
**Supporting skills:** Actually used skills or none.

**Compilation:** Command and actual outcome, or NOT RUN.  
**Tests:** Command and result, or NOT RUN.  
**Gradle checks:** Command and result, or NOT RUN.  
**Runtime/UI:** Actual verification or NOT RUN.

**Acceptance:** Verified criteria count / total criteria.  
**Open issues:** Remaining failures or blockers.  
**Next:** Single actionable next step.

### Report rules

- Create the report directory when necessary.
- Overwrite the corresponding report with its latest consolidated state.
- Preserve unresolved issues.
- Keep reports factual and concise.
- Do not paste verbose logs, secrets, hidden reasoning, or model contents.
- Do not fabricate acceptance counts.
- Keep `_local/` excluded from Git.

Verify the ignore rule using `git check-ignore`.

Check that `_local/` files are not already tracked using `git ls-files`.

Do not force-add reports or silently alter the Git index.

Reports contain execution details. Version-controlled TASK completion records contain concise acceptance evidence.

## 13. Documentation Updates

**Default rule: update operational records, not the static documentation framework.**

After implementing or verifying a task, update only:

1. The selected TASK completion record.
2. `docs/development/tasks/README.md`, when status, dependency eligibility, priority, or an approved operational change
   must be recorded.
3. `_local/reports/task-reports/task-XXX.md`.

Do not automatically modify:

- Root `README.md`.
- `docs/architecture/README.md`.
- Accepted ADRs.
- `docs/security/README.md`.
- `docs/development/README.md`.
- `docs/development/ROADMAP.md`.
- `docs/development/AI-REQUIREMENTS.md`.
- Unrelated TASK specifications.

These documents provide established product, architectural, and security requirements.

A task's implementation or status change does not by itself justify rewriting them.

If a genuine architectural or product-level change is necessary:

1. Identify the conflict with the existing framework.
2. Describe the smallest required amendment.
3. Obtain applicable approval.
4. Modify only the affected authoritative document.
5. Record the approved change in the central backlog where relevant.

Do not broadly rewrite READMEs, task specifications, or architecture descriptions.

Do not silently modify previously accepted decisions.

The project is an independent software product.

Do not introduce course, training-program, or presentation requirements.

## 14. Efficiency and Context Management

This workflow may involve multiple substantial implementation tasks.

Avoid unnecessary context consumption.

- Read relevant source files rather than entire unrelated directories.
- Do not repeat completed repository discovery without a reason.
- Inspect specialized skills only when needed.
- Do not generate large planning documents for already specified tasks.
- Reuse existing code, tests, and Gradle configuration.
- Prefer targeted diagnostics over broad investigations.
- Avoid repeated full builds during focused debugging.
- Complete every required final verification.
- Record progress before starting another task.
- Use local reports as persistent execution checkpoints.
- Keep the central backlog current without duplicating status across static documents.

If execution is interrupted, a later ALL invocation must inspect existing checkpoints and resume from the actual
implementation state.

Never reconstruct missing verification evidence from assumptions.

## 15. Final Response

### Individual task mode

Return a concise English summary containing:

- Task identifier and final status.
- Implemented functionality.
- Supporting skills actually used.
- Compilation and test results.
- Real integration verification where applicable.
- Missing verification or blockers.
- Local report path.

### ALL mode

Return a compact overall summary containing:

- VERIFIED tasks.
- IN PROGRESS tasks.
- BLOCKED tasks.
- Tasks not attempted.
- Main implementation results.
- Required verification still missing.
- Local report directory.
- Exact reason execution stopped.
- Next eligible task or required user action.

Do not repeat individual reports or complete task specifications.

Do not claim completion of unverified work.

Stop after the requested individual task or after ALL mode reaches its defined end condition.