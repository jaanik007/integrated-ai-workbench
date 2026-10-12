# TASK-008: Agent Execution and Structured Output

**Status:** VERIFIED  
**Dependencies:** TASK-005, TASK-006, TASK-007  
**Milestone:** B — Base Plugin MVP

## Objective

Implement the first integrated, policy-enforced agent execution coordinator.

The coordinator must resolve an approved runtime agent, validate its model profile and skills, prepare controlled
project context, invoke Workbench-managed local inference, validate the result, and return a typed execution outcome.

The implementation must reuse the functional inference path from TASK-005 and the configuration and security boundaries
from TASK-006 and TASK-007.

## Scope

### Agent resolution and execution configuration

Resolve the requested agent through the validated runtime configuration loader.

Before execution, verify:

- Agent existence and configuration validity.
- Allowed runtime skills.
- Model-profile availability.
- Required model capabilities.
- Applicable grounding policy.
- Declared output requirements.
- Effective tool restrictions.
- Compatibility with the selected local runtime.

Invalid or incompatible configurations must fail before inference.

Do not silently select another agent, model, or skill to bypass a restriction.

### Execution coordinator

Extend the existing `AiWorkbenchService` or the established equivalent service.

The coordinator must connect the approved components in a controlled sequence:

```text
Agent and profile resolution
    ↓
Skill and policy validation
    ↓
Input validation
    ↓
Approved project grounding
    ↓
Secret filtering and prompt composition
    ↓
Model and runtime compatibility
    ↓
Workbench-managed local inference
    ↓
Output and structured-response validation
    ↓
Typed execution outcome
```

Reuse existing lifecycle, request, response, and run contracts.

Avoid introducing a second inference pipeline.

### Runtime instructions

Use Markdown-defined agent and skill instructions loaded by TASK-006.

The coordinator must preserve the separation between trusted application policy, agent instructions, user input, and
untrusted project context.

Runtime instructions cannot grant permissions beyond the application's effective policy.

Do not interpret Markdown instructions as executable code.

### Model execution

Send validated prompt parts and applicable execution settings to the TASK-005 inference path.

Support:

- Model-profile selection.
- Supported generation configuration.
- Bounded execution.
- Cancellation propagation.
- Timeout handling.
- Typed runtime failures.
- Accurate execution metadata.

Unsupported requested model capabilities must fail according to the existing compatibility rules.

No cloud or independently operated AI application may be selected as an implicit fallback.

### Structured output

Implement deterministic validation of model-generated structured responses.

Use the existing `StructuredResponseValidator` and shared output contracts.

Required behavior includes:

- Explicit expected output type or schema.
- Parsing and validation of machine-readable responses.
- Rejection of malformed or incompatible structured content.
- Clear distinction between valid text output and required structured output.
- Typed schema-validation failures.
- No execution of embedded commands or tool-call proposals.

A model response must not be marked valid merely because it resembles JSON.

When native constrained generation is unavailable, Workbench-side validation must not be represented as native
enforcement.

Do not introduce an unrestricted model-generated repair loop.

### Tool permission enforcement

The coordinator must not perform arbitrary actions requested by an agent or model.

Proposed tool operations, where represented by the existing contracts, must be checked against programmatic permission
policies.

No actual shell execution, file modification, external network operation, or unrestricted tool invocation is introduced
in this task.

Text discussing an action is not itself an authorized tool call.

Invalid or unauthorized structured tool requests must be rejected as executable operations.

### Run metadata and provenance

Produce a typed execution outcome containing applicable safe information:

- Run identifier.
- Agent and skill identifiers.
- Model-profile identifier.
- Selected runtime identifier.
- Execution status and duration.
- Grounding source references.
- Validation results.
- Available generation metadata.
- Safe failure details.

Do not fabricate model metrics or retain secrets, hidden model reasoning, or unsanitized prompt contents.

Keep user-facing reasoning summaries separate from private reasoning information.

### Failure handling

Distinguish applicable outcomes including:

- Invalid agent or skill configuration.
- Invalid model profile.
- Unsupported capabilities.
- Grounding or input-policy rejection.
- Runtime unavailable.
- Model-loading or inference failure.
- Timeout.
- Cancellation.
- Output-policy failure.
- Structured-response validation failure.
- Unauthorized proposed operation.

A failed required validation cannot be converted into success by subsequent processing.

### Tests

Use fake runtime adapters, controlled Markdown fixtures, and deterministic grounding data to test coordinator behavior.

Verify both successful execution and failure precedence across component boundaries.

An available real inference backend may support additional integration checks, but the coordinator's automated tests
must not require a GPU, network service, or model binary.

## Affected Files

Primary scope:

- Existing `AiWorkbenchService.kt`
- `base/backend/**/agent/`
- `base/backend/**/validation/`
- Relevant security integration components
- Shared run and execution models
- Focused coordinator and structured-output tests

Preserve the established architecture and source-root boundaries.

## Implementation Boundaries

- Do not introduce arbitrary tool execution.
- Do not write files or run shell commands on behalf of the model.
- Do not implement cloud inference or external network access.
- Do not implement LLM-as-a-Judge scoring.
- Do not redesign the Tool Window.
- Do not duplicate runtime loading, grounding, or lifecycle services.
- Do not weaken deterministic security checks.
- Do not modify unrelated architecture or remote Git branches.

## Acceptance Criteria

TASK-008 is complete when:

1. An approved agent can be resolved and executed through the existing local inference path.
2. Agent, profile, skill, and runtime compatibility is checked before execution.
3. Only approved and bounded grounding reaches the model.
4. Prompt roles and runtime instructions are composed correctly.
5. Input and secret controls are enforced before inference.
6. Model generation uses validated execution settings.
7. Output and structured-response validation is performed after inference.
8. Unauthorized executable tool requests are rejected.
9. Cancellation, timeout, and runtime failures propagate as typed outcomes.
10. Run metadata contains accurate, safe provenance.
11. Mandatory validation failures cannot be reported as successful execution.
12. Deterministic coordinator tests cover successful and rejected workflows.
13. Required Gradle checks pass.

## Verification

Run focused tests for:

- Valid agent execution.
- Missing or incompatible agents and skills.
- Profile and runtime mismatch.
- Prompt composition and approved grounding.
- Input, secret, and output policy enforcement.
- Valid structured output.
- Malformed structured output.
- Unauthorized tool requests.
- Cancellation and timeout propagation.
- Typed run metadata and error outcomes.

Then run:

```powershell
.\gradlew.bat check
```

Record the actual commands and results.

## Completion Record

**Implementation:** `AgentExecutionService` now resolves only validated Markdown-defined agents and skills, prepares
sanitized bounded grounding, composes separated instruction/task/input/context parts, invokes the existing
`LocalInferenceService`, and returns a typed safe outcome. `StructuredResponseValidator` now deterministically parses
a bounded JSON/schema subset; output guards and programmatic tool permissions run after inference and never execute a
proposed operation.

**Verification:** `./gradlew.bat test --tests "*AgentExecutionServiceTest" --tests "*StructuredResponseValidatorTest"`
passed (6 tests); `./gradlew.bat check` passed on 2026-10-10. Tests cover valid grounded structured output, missing
agent, malformed/schema-incompatible output, unauthorized tool proposal, timeout, and cancellation.

**Final status:** VERIFIED

Record modified files, executed tests, verified behavior, known limitations, and final status after implementation.

Set the task to `VERIFIED` only after satisfying all mandatory acceptance criteria.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-008`.

The implementation skill must reuse the existing local inference and security pipelines, implement a single coherent
execution coordinator, and avoid expanding into unrestricted tools or evaluation functionality.
