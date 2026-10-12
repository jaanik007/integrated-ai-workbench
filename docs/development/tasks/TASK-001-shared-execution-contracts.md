# TASK-001: Shared Execution Contracts and Reasoning Control

**Status:** VERIFIED  
**Dependencies:** None  
**Milestone:** B — Base Plugin MVP

## Objective

Establish validated, provider-neutral execution contracts for Integrated AI Workbench.

Extend the existing bootstrap models to support reliable model configuration, request handling, response metadata, and
reasoning-level preferences without introducing runtime-specific dependencies.

These contracts form the foundation for model registration, runtime compatibility, local inference, agent execution, and
evaluation.

## Scope

### Model profiles

Extend `ModelProfile` and related shared models to represent:

- Stable model and profile identifiers.
- References to user-owned local models.
- Local-only execution policy.
- Supported generation and sampling parameters.
- Output token limits and execution timeout.
- Structured-output expectations.
- Configurable thinking level.

Use typed properties and predictable validation.

Preserve existing defaults and construction patterns wherever possible.

Profiles must contain configuration and references, not model binary contents or credentials.

### Thinking-level configuration

Introduce `ThinkingLevel` or an equivalent bounded, provider-neutral type.

The contract must:

- Distinguish an unspecified/default preference from an explicitly requested level.
- Define the meaning of each supported logical level.
- Allow runtime capability information to describe supported levels.
- Support explicit compatibility validation before execution.
- Reject unsupported requested levels predictably rather than silently ignoring them.
- Preserve the effective configuration in safe execution metadata where applicable.

Thinking level expresses an execution preference, not a guarantee that every inference backend supports identical
reasoning controls.

Runtime-specific mappings are outside this task and belong to subsequent runtime integration tasks.

The implementation must not request, expose, persist, or imply access to hidden chain-of-thought.

An optional concise reasoning summary may be represented only as an explicitly available, user-facing result field. It
must not be confused with private model reasoning.

### Execution contracts

Review and extend the existing shared contracts as required:

- `ModelRequest` — validated request data, model/profile selection, and applicable execution preferences.
- `ModelResponse` — generated content, response status, and safe execution metadata.
- Runtime capability contracts — supported execution options and compatibility information.
- Run metadata — typed identifiers, outcomes, timing information, and relevant execution settings.
- Agent-related shared contracts — compatibility with the updated execution models.

Use existing types where they already meet these requirements. Avoid duplicate contracts or unnecessary abstractions.

### Validation

Implement deterministic validation for applicable fields, including:

- Missing or blank required identifiers.
- Invalid or non-finite sampling values.
- Invalid token counts.
- Zero, negative, or otherwise invalid timeouts.
- Inconsistent structured-output configuration.
- Invalid combinations of execution settings.

Document and enforce provider-neutral validation boundaries.

Runtime-specific limits may impose stricter requirements during compatibility checking.

A valid profile must not fail construction merely because a runtime has not yet been selected. Unsupported
thinking-level requests must be rejected when checked against the selected runtime's capabilities.

## Affected Files

Primary scope:

- `base/shared/src/main/kotlin/**/shared/model/`
- `base/shared/src/main/kotlin/**/shared/agent/`
- `base/shared/src/main/kotlin/**/shared/run/`
- Existing shared runtime capability contracts, where applicable.
- Focused tests in the matching shared test source root.

Inspect the existing package structure before modifying files. Preserve established naming conventions and source-root
boundaries.

Do not create new Gradle modules.

## Implementation Boundaries

- Preserve provider-neutral contracts.
- Maintain compatibility with existing bootstrap callers.
- Do not introduce inference engines, provider SDKs, cloud integrations, or model binaries.
- Do not implement the model registry, hardware detector, runtime manager, or UI.
- Do not introduce credential storage or persistent configuration infrastructure.
- Do not implement runtime-specific thinking parameter mapping.
- Do not duplicate functionality already provided by existing shared models.
- Do not modify unrelated source files or project architecture.
- Do not modify remote Git branches.

Only introduce dependencies when necessary and justified.

## Acceptance Criteria

TASK-001 is complete when all the following conditions are satisfied:

1. `ModelProfile` supports validated execution settings and a typed thinking-level preference.
2. Thinking-level defaults and capability-compatibility rules are explicitly defined.
3. Unsupported thinking-level requests can be identified and rejected deterministically using declared runtime
   capabilities.
4. Invalid identifiers, sampling values, token limits, timeouts, and applicable configuration combinations fail
   predictably.
5. `ModelRequest`, `ModelResponse`, and related execution metadata remain typed and provider-neutral.
6. Safe execution metadata can describe the requested and applicable effective settings without exposing secrets or
   hidden model reasoning.
7. Existing bootstrap callers remain compatible or are updated with minimal, justified changes.
8. No runtime implementation, provider dependency, cloud service, or UI functionality is introduced.
9. Focused unit tests cover valid defaults, invalid configurations, thinking-level compatibility, and
   backward-compatible construction.
10. The project compiles and the required Gradle checks pass.

Passing compilation without the required validation tests is insufficient for task completion.

## Verification

Run focused tests for the modified shared contracts, followed by the project checks:

```powershell
.\gradlew.bat test
.\gradlew.bat check
```

Use narrower test filters where applicable to the actual test classes.

Verify:

- Existing contract construction and default values.
- Valid and invalid profile settings.
- Thinking-level compatibility and rejection behavior.
- Request and response metadata consistency.
- Absence of credentials and hidden reasoning content from shared execution data.
- Compatibility with existing bootstrap components.

Record actual commands, outcomes, and outstanding limitations after execution.

Do not report tests as passing before they have been run.

## Completion Record

**Implementation:** Validated provider-neutral execution contracts, typed thinking-level preferences, and safe execution metadata implemented in shared models.  
**Verification:** `./gradlew.bat test --tests "com.jaanik.integratedaiworkbench.shared.model.ExecutionContractsTest"` (4 passed); `./gradlew.bat check` (PASS).  
**Final status:** VERIFIED

Update this section after implementation with:

- Modified and added source files.
- Tests executed and their results.
- Acceptance criteria satisfied.
- Remaining issues or limitations.
- Final task status.

Mark the task `VERIFIED` only after the acceptance criteria are satisfied and the required verification has succeeded.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-001`.

The corresponding Codex skill must follow this specification, inspect existing contracts before making changes, and
report only completed work and verified results.
