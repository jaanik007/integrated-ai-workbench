# TASK-005: Local Inference Vertical Slice

**Status:** VERIFIED  
**Dependencies:** TASK-004  
**Milestone:** B — Base Plugin MVP

## Objective

Implement and verify the first operational local language-model inference workflow in Integrated AI Workbench.

Connect the selected Workbench-managed inference backend to the existing model registry, compatibility matcher, runtime
manager, and shared execution contracts.

A compatible user-supplied local model must be able to receive a validated `ModelRequest` and produce a typed
`ModelResponse` through a runtime controlled by the Workbench.

The implementation must support bounded execution, cancellation, timeouts, error handling, and safe execution metadata.

This task establishes real model inference before introducing agent orchestration, project grounding, or additional
user-interface functionality.

## Scope

### 1. End-to-end local inference path

Implement the following execution sequence:

```text
ModelRequest
    ↓
Model Profile Resolution
    ↓
Registered Local Model Validation
    ↓
Hardware and Runtime Compatibility Check
    ↓
Workbench-Managed Runtime
    ↓
Model Loading
    ↓
Inference Execution
    ↓
Typed ModelResponse
    ↓
Resource Cleanup
```

The complete sequence must operate through existing provider-neutral interfaces.

Reuse the contracts and components established in TASK-001 through TASK-004.

Do not introduce a parallel model registry, runtime manager, or request-processing architecture.

### 2. Model and profile selection

Before inference:

- Resolve the requested model profile.
- Validate the associated registered model reference.
- Revalidate the local model path before loading.
- Verify the configured local-only execution policy.
- Evaluate compatibility using the existing matcher.
- Verify that the selected runtime is available and operational.
- Reject known unsupported configurations.
- Handle unknown compatibility information according to the explicit runtime-selection policy.

Do not silently substitute another model, runtime, or remote service.

A model registered successfully in TASK-002 is not automatically considered compatible with the selected inference
backend.

### 3. Runtime adapter integration

Implement the actual inference operations through `LocalInferenceAdapter` and `LocalRuntimeManager`.

The selected adapter must support the applicable lifecycle:

- Runtime readiness verification.
- Model loading or equivalent initialization.
- Validated inference request submission.
- Generation execution.
- Response retrieval.
- Cancellation and timeout handling.
- Model unloading where supported.
- Runtime resource cleanup.

Follow the runtime architecture selected and verified in TASK-004.

Avoid duplicating process management or native-resource ownership already implemented by the runtime manager.

For a process-based backend, communicate through its documented local interface.

For an embedded native backend, use its verified native integration mechanism.

The adapter must not assume capabilities that the selected backend has not established.

### 4. Generation configuration

Apply the validated execution settings defined in TASK-001.

Relevant settings include:

- Maximum generated tokens.
- Temperature.
- Top-p and other supported sampling parameters.
- Execution timeout.
- Structured-output requirements.
- Thinking-level preference, where supported.

Validate configuration against the selected runtime's declared capabilities before execution.

Explicitly requested unsupported features must result in a predictable compatibility or validation failure rather than
being silently ignored.

Backend defaults may be used only where the shared contract explicitly permits them.

Distinguish requested parameters from effective parameters in execution metadata where applicable.

Thinking-level configuration must use supported runtime controls.

The implementation must not request, expose, or persist hidden model chain-of-thought.

### 5. Structured-output handling

Support the structured-output expectations defined by the shared execution contracts.

Distinguish:

- Runtime-native constrained generation.
- Ordinary model generation followed by Workbench-side validation.

Do not present post-generation validation as native constrained-generation support.

If the profile explicitly requires a native structured-generation capability that the runtime does not provide, reject
the configuration predictably.

This task must propagate the relevant output requirements and preserve the resulting response information.

The complete agent-level structured-output validation pipeline belongs to TASK-008.

### 6. Execution limits and cancellation

Inference execution must be bounded and controllable.

Required behavior includes:

- Validation of input and generation limits.
- Configured execution timeouts.
- Cancellation propagation to the selected runtime.
- Recognition of interrupted or terminated execution.
- Bounded waiting during cancellation and cleanup.
- Safe handling of incomplete responses.
- Prevention of stale or cancelled responses being reported as successful.
- Consistent runtime state after completion or failure.

Cancellation must not leave the Workbench waiting indefinitely.

Where the selected backend cannot interrupt generation directly, document the limitation and implement the safest
supported termination or recovery mechanism.

Timeouts must be enforced by the Workbench rather than relying exclusively on model-generated behavior.

### 7. Typed responses and execution metadata

Return results through the existing `ModelResponse` and shared execution contracts.

Responses must distinguish applicable outcomes such as:

- Successful generation.
- Invalid or incompatible configuration.
- Runtime unavailable.
- Model-loading failure.
- Inference failure.
- Cancellation.
- Timeout.

Reuse established status and error types where available.

Response metadata should contain relevant safe information, including:

- Model and profile identifiers.
- Selected runtime identifier.
- Execution status.
- Requested and effective configuration, where available.
- Execution duration.
- Token usage, only when reliably reported or explicitly identified as estimated.
- Safe error information.
- Appropriate execution provenance.

Do not fabricate token counts, runtime metrics, or supported capabilities.

Never include hidden reasoning, credentials, sensitive runtime arguments, or model binary contents in response metadata.

A concise user-facing reasoning summary may be included only when explicitly supported and returned as ordinary model
output.

### 8. Error handling and recovery

Handle failures predictably at each execution boundary.

Required scenarios include:

- Missing registered model.
- Invalid or relocated model path.
- Unsupported model format.
- Insufficient or incompatible runtime capabilities.
- Runtime startup failure.
- Model-loading failure.
- Request timeout.
- User cancellation.
- Unexpected runtime termination.
- Malformed or incomplete backend response.
- Resource cleanup failure.

Provide actionable error information without exposing sensitive implementation details.

Ensure that failures do not silently trigger cloud inference or another independently operated application.

After failure, the runtime manager must either remain in a valid reusable state or transition safely to a stopped or
failed state.

### 9. Deterministic adapter testing

Use the fake adapter established in TASK-004 for controlled integration tests.

Tests must verify:

- Valid profile and model resolution.
- Compatible runtime selection.
- Request propagation.
- Supported generation-parameter mapping.
- Rejection of unsupported explicit parameters.
- Typed successful responses.
- Typed failures.
- Cancellation propagation.
- Timeout handling.
- Runtime failure recovery.
- Cleanup after execution.

Fake-adapter tests must not require a real model binary, GPU, or network service.

The fake adapter must exercise the same relevant execution contracts as the production adapter.

### 10. Real local-model verification

A successful real-model inference is a mandatory acceptance requirement for TASK-005.

Use a compatible, explicitly supplied local model and the inference backend selected in TASK-004.

The verification must establish that:

- The model is resolved through the Workbench registry.
- Compatibility is evaluated before loading.
- The Workbench controls the runtime lifecycle.
- The selected runtime loads or initializes the model successfully.
- A real inference request reaches the selected backend.
- The model generates an actual response.
- The result is returned through `ModelResponse`.
- Execution metadata accurately identifies the outcome.
- Runtime resources can be released safely.

Record the actual environment, supported model format, backend, execution parameters, verification procedure, and
observed outcome.

Do not commit the model binary, runtime executable, or sensitive local configuration.

A lightweight local model is sufficient if it is compatible with the selected runtime and exercises the actual inference
path.

If suitable runtime resources or model files are unavailable, record the blocker and retain `IN PROGRESS` status.

Fake-adapter success must not be substituted for real inference verification.

## Affected Files

Primary scope:

- Existing `LocalInferenceAdapter.kt`.
- Existing `LocalRuntimeManager.kt`.
- Selected backend-specific inference adapter implementation.
- Existing backend runtime integration components.
- Relevant shared model request, response, and execution contracts.
- Existing model registry and compatibility interfaces where integration requires changes.
- Focused adapter and integration tests.

Inspect the established package structure before creating or modifying files.

Reuse the architecture and runtime selection established in TASK-004.

Preserve the single Gradle module and existing source-root boundaries.

## Implementation Boundaries

- Do not implement a new inference engine from scratch.
- Do not introduce cloud-provider integrations or cloud fallback.
- Do not require LM Studio, Ollama, or independently operated AI applications.
- Do not automatically download, modify, or bundle model binaries.
- Do not claim general model-format support beyond the selected backend's verified capabilities.
- Do not implement agent orchestration or runtime skill execution.
- Do not implement project grounding or unrestricted filesystem access.
- Do not implement arbitrary tool execution.
- Do not introduce unnecessary Tool Window functionality.
- Do not implement the complete evaluation or LLM-as-a-Judge pipeline.
- Do not duplicate existing runtime lifecycle management.
- Do not modify unrelated architecture or remote Git branches.

All new runtime dependencies must remain consistent with the decision recorded in ADR-001.

## Acceptance Criteria

TASK-005 is complete when:

1. A validated `ModelRequest` can be executed through the selected real local inference adapter.
2. Model and profile references are resolved through the existing registry.
3. Compatibility is checked before model loading.
4. The Workbench controls the selected runtime without requiring an independently operated AI application.
5. A compatible user-supplied model can be loaded and used for actual text generation.
6. Successful execution returns a valid, typed `ModelResponse`.
7. Supported generation parameters are propagated correctly.
8. Explicitly requested unsupported capabilities are rejected predictably.
9. Structured-output requirements are handled according to verified runtime capabilities.
10. Execution timeouts and cancellation are enforced.
11. Failures produce typed, actionable results without unsafe fallback.
12. Runtime state and resources remain consistent after completion, cancellation, or failure.
13. Response metadata accurately reports available execution information without exposing sensitive data.
14. Deterministic adapter tests cover success and failure scenarios.
15. Focused integration tests and required Gradle checks pass.
16. At least one real local-model inference has been completed successfully through the Workbench-managed runtime.
17. The real-inference verification procedure and observed results are documented.

A successful build or fake-adapter test alone does not satisfy these acceptance criteria.

## Verification

### Automated tests

Run focused adapter and inference integration tests, followed by:

```powershell
.\gradlew.bat check
```

Use the existing test infrastructure and fake runtime adapter.

Automated tests must not depend on a particular developer workstation or installed language model.

### Real-model integration verification

Run a bounded inference request against an explicitly supplied compatible local model.

Confirm:

- Valid profile and model registration.
- Successful compatibility selection.
- Managed runtime initialization.
- Model loading.
- Successful generation.
- Typed response creation.
- Accurate execution outcome.
- Safe runtime shutdown or resource release.

Use a reproducible local integration-test procedure or an appropriately scoped development harness.

A production UI is not required for this task.

The actual generated text need not match a fixed expected sentence. Verification must establish that generation occurred
and returned a structurally valid response.

Record the real-model verification separately from fake-adapter unit tests.

Where supported, additionally exercise cancellation and timeout behavior against the real backend.

Do not claim a check passed unless it was actually executed.

## Completion Record

**Implementation:** `LocalInferenceService` resolves registered model profiles, revalidates local paths, checks compatibility, manages the selected runtime lifecycle, maps generation settings, returns typed responses with safe provenance, and releases the runtime after each request. `LlamaServerAdapter` maps bounded HTTP timeouts to `TIMED_OUT`; cancellation force-stops the owned runtime and prevents stale success responses.  
**Automated verification:** `./gradlew.bat compileTestKotlin --rerun-tasks`, focused inference/adapter/compatibility tests, and `./gradlew.bat check` (PASS, 2026-10-10).  
**Real-model verification:** PASS — official llama.cpp b11429 Windows x64 CPU runtime loaded the supplied Qwen3-4B GGUF through the registry and `LocalInferenceService`, sent a bounded localhost completion request, and returned a non-empty typed `ModelResponse` with safe metadata. JUnit recorded 1 executed test, 0 skipped/failures/errors, in 18.934 seconds; the managed process was stopped and no `llama-server` process remained.  
**Final status:** VERIFIED

After implementation, record:

- Selected inference backend.
- Modified and added source files.
- Supported model formats and generation parameters.
- Automated tests executed and their results.
- Real-model verification environment and procedure.
- Observed inference result and runtime behavior.
- Acceptance criteria satisfied.
- Unsupported capabilities or remaining limitations.
- Final task status.

Mark TASK-005 `VERIFIED` only when the mandatory acceptance criteria, automated verification, and real-model inference
verification have succeeded.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-005`.

The corresponding Codex skill must implement the first operational inference path using the backend selected in
TASK-004, preserve existing shared contracts, run deterministic tests, and verify actual inference using a compatible
user-supplied local model.
