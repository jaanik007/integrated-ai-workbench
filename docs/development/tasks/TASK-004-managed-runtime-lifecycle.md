# TASK-004: Managed Local Runtime Decision and Lifecycle

**Status:** VERIFIED  
**Dependencies:** TASK-002, TASK-003  
**Milestone:** B — Base Plugin MVP

## Objective

Select a technically suitable local inference backend and implement the Workbench-managed runtime lifecycle.

Integrated AI Workbench must be able to select, initialize, monitor, and stop a compatible local inference runtime
without requiring LM Studio, Ollama, a cloud API, or another independently operated AI application.

The implementation must preserve provider-neutral interfaces and provide a reliable execution foundation for TASK-005.

## Scope

### 1. Inference backend selection

Evaluate candidate inference backends before introducing runtime dependencies or binaries.

Potential approaches include:

- An embedded native inference library.
- A Workbench-managed local inference process.
- Another technically justified local execution mechanism.

A managed llama.cpp-based implementation is a candidate, not a predetermined selection.

Evaluate candidates against:

- Windows, Linux, and macOS compatibility, as applicable to the supported plugin platforms.
- Native architecture and JDK integration requirements.
- CPU inference availability.
- GPU acceleration support and its installation requirements.
- Supported model formats, including relevant GGUF compatibility.
- Runtime lifecycle and process-control interfaces.
- Model loading and unloading capabilities.
- Request cancellation and execution timeouts.
- Structured generation capabilities.
- Generation and reasoning parameter support.
- Licensing and redistribution conditions.
- Binary provenance and integrity verification.
- Offline operation.
- Packaging, installation, and maintenance complexity.

Document verified capabilities, limitations, and rejected alternatives.

Do not claim accelerator, reasoning, or structured-output support without evidence from the selected runtime.

### 2. Architecture decision record

Update:

`docs/architecture/ADR-001-managed-local-runtime.md`

The updated ADR must identify:

- The selected inference backend and execution approach.
- The reasons for selection.
- Evaluated alternatives.
- Verified platform and model-format support.
- Relevant licensing and binary-distribution constraints.
- Runtime acquisition or installation requirements.
- Security and lifecycle limitations.
- Known unsupported capabilities.
- Consequences for TASK-005 and subsequent integration.

Change the ADR status from `PROPOSED` to `ACCEPTED` only when sufficient evidence supports the selected implementation
approach.

If no candidate satisfies the mandatory requirements, retain the proposed status and document the unresolved decision.

### 3. Runtime management

Extend the existing `LocalRuntimeManager`, `LocalInferenceAdapter`, and related contracts.

The Workbench must control:

- Runtime selection using model requirements and compatibility results.
- Runtime location and configuration validation.
- Initialization and startup.
- Readiness and health checks.
- Model-loading lifecycle coordination.
- Model unloading where supported.
- Cancellation of supported lifecycle operations.
- Graceful shutdown.
- Forced termination when graceful shutdown fails.
- Failure detection and recovery.
- Resource cleanup.

Reuse the shared contracts established in TASK-001 and the compatibility results from TASK-003.

Do not duplicate runtime or model-state abstractions unnecessarily.

### 4. Runtime state management

Represent runtime lifecycle states explicitly through typed contracts.

The state model must distinguish relevant conditions such as:

- Not started.
- Starting.
- Ready.
- Loading a model.
- Model loaded.
- Stopping.
- Stopped.
- Failed.

Use the minimum state set required by the selected runtime architecture.

State transitions must be deterministic and reject invalid operations predictably.

Applicable operations should be idempotent, including repeated shutdown requests.

Cancellation must not leave the manager in an inconsistent state.

A failed startup or model-loading operation must transition into a recoverable or safely terminated state.

The runtime manager must not report readiness before the selected backend has passed its required initialization checks.

### 5. Managed process or native library lifecycle

For a process-based implementation:

- Start the runtime from a validated executable path.
- Use structured process arguments rather than shell-command concatenation.
- Validate configured model paths and runtime options.
- Restrict any local service endpoint to an appropriate local interface.
- Detect unexpected process termination.
- Enforce startup and shutdown timeouts.
- Terminate owned child processes where necessary.
- Release process handles and temporary resources.
- Prevent orphaned runtime processes after normal shutdown or recoverable failures.

For a native-library implementation:

- Validate library loading and platform compatibility.
- Define initialization and disposal behavior.
- Release native handles and model resources.
- Handle initialization failures and unsupported native configurations safely.

The selected implementation must clearly define ownership of every managed runtime resource.

### 6. Runtime compatibility and selection

Runtime selection must use the model registry and compatibility information established in TASK-002 and TASK-003.

Selection must account for:

- Registered model reference.
- Model format.
- Operating system and architecture.
- Available execution devices.
- Memory requirements and known limits.
- Runtime-supported execution parameters.
- Structured-output requirements.
- Requested thinking level.
- Local-only execution policy.

Known incompatible configurations must be rejected.

An `UNKNOWN` compatibility result must not be silently treated as confirmed compatibility. Any permitted operation under
uncertainty must be explicitly controlled and its limitations reported.

Do not silently select a remote provider or independently operated AI application as a fallback.

### 7. Security and configuration

The runtime manager must enforce appropriate execution boundaries.

Required controls include:

- Validated runtime executable or native-library locations.
- Validated user-selected model paths.
- Safe argument construction.
- Controlled process environment.
- Explicit local endpoint configuration where applicable.
- Bounded startup and shutdown operations.
- Predictable failure handling.
- No unrestricted command execution.
- No credentials or model contents in diagnostic logs.
- No unnecessary exposure of local inference endpoints to external networks.

Runtime binaries and model files must remain outside source control.

Persist only the configuration necessary to identify and manage the selected runtime.

A local runtime must not gain additional filesystem or tool permissions merely because an agent requests them.

### 8. Deterministic fake adapter

Retain or implement a fake runtime adapter for automated testing.

It must support controlled simulation of:

- Successful startup.
- Readiness.
- Model-loading transitions.
- Unsupported configurations.
- Startup failure.
- Cancellation.
- Unexpected runtime termination.
- Shutdown.
- Cleanup failure and recovery.

Fake-adapter tests must run without downloading or executing a real language model.

The fake adapter must use the same relevant lifecycle contracts as the selected real backend.

## Affected Files

Primary scope:

- `docs/architecture/ADR-001-managed-local-runtime.md`
- Existing backend runtime-management components.
- Existing `LocalRuntimeManager` and `LocalInferenceAdapter` contracts.
- Shared runtime-state and capability contracts where required.
- Focused lifecycle and compatibility tests.

Inspect the existing implementation before introducing new classes or dependencies.

Preserve the single Gradle module and established source-root boundaries.

## Implementation Boundaries

- Do not implement a new inference engine from scratch.
- Do not introduce cloud providers or mandatory external AI applications.
- Do not require LM Studio or Ollama.
- Do not bundle or download model binaries.
- Do not commit runtime executables, native libraries, or model files.
- Do not introduce an unverified dependency.
- Do not implement the complete model inference workflow assigned to TASK-005.
- Do not implement agent execution, grounding, evaluation, or Tool Window features.
- Do not introduce unsupported GPU or reasoning capabilities.
- Do not redesign existing shared contracts without a demonstrated need.
- Do not modify unrelated source files or remote Git branches.

Any new dependency must have documented compatibility, licensing, and security justification.

## Acceptance Criteria

TASK-004 is complete when:

1. A suitable inference backend has been selected using documented technical evidence.
2. The architecture decision record identifies the selected approach, evaluated alternatives, and known limitations.
3. The selected backend can be managed by Integrated AI Workbench without requiring an independently operated AI
   application.
4. Runtime selection respects known model, platform, hardware, and capability constraints.
5. Runtime lifecycle states and transitions are explicitly represented.
6. Initialization, readiness checks, shutdown, and cleanup are implemented for the selected approach.
7. Invalid operations and incompatible configurations fail predictably.
8. Cancellation and startup failures do not leave the runtime manager in an inconsistent state.
9. Unexpected runtime termination is detected and reported safely.
10. Runtime paths and launch parameters are validated.
11. Runtime resources are released during shutdown and recoverable failure handling.
12. Sensitive data and model contents are excluded from runtime logs.
13. A deterministic fake adapter supports lifecycle and failure-path testing.
14. Focused lifecycle tests and required Gradle checks pass.
15. Any real-runtime smoke-test limitations are explicitly recorded.

A passing fake-adapter test does not establish that a selected native backend is operational.

## Verification

Run focused lifecycle tests, followed by:

```powershell
.\gradlew.bat check
```

Required automated scenarios include:

- Successful runtime initialization.
- Repeated initialization requests.
- Valid runtime-state transitions.
- Invalid transition rejection.
- Known incompatible runtime selection.
- Startup timeout.
- Cancellation during initialization.
- Model-loading failure handling.
- Unexpected runtime termination.
- Graceful shutdown.
- Repeated shutdown.
- Forced termination and resource cleanup where applicable.
- Safe diagnostic output.

Tests must be deterministic and must not require actual model binaries.

### Local runtime smoke test

Once a real inference backend has been selected and integrated, perform an appropriate local smoke test using verified
runtime resources available on the development machine.

Verify the applicable subset of:

- Runtime executable or native library can be initialized.
- The Workbench can start or initialize the runtime.
- Readiness can be established.
- Runtime status is reported accurately.
- The Workbench can stop the runtime.
- Owned resources are released.

If the selected integration supports model loading during this stage, verify it using an explicitly supplied local model
without modifying the model file.

Record actual outcomes and any environment-specific limitations.

Full model inference, generated responses, and inference-level parameter propagation are acceptance targets for
TASK-005.

Do not substitute a fake-adapter result for a real-backend smoke test.

## Completion Record

**Implementation:** Workbench-managed llama.cpp `llama-server.exe` adapter, lifecycle state management, configured SHA-256 verification, deterministic fake adapter, and opt-in real-runtime smoke test are implemented.  
**Verification:** official `ggml-org/llama.cpp` b11429 Windows x64 CPU artifact selection was verified from its build output (`0.6.0-dev`, build `11429`, commit `d81235049`) and release record; executable SHA-256 `7E236243C9B670C6A49410C03186FD575C57A0EFE3FD8A21D8727503D571D5B6`; required CPU DLLs were present. The operator-supplied Qwen3 GGUF was readable with a `GGUF` header. Real Workbench-managed lifecycle smoke test passed: localhost startup, readiness, `/health`, model-loaded state, shutdown, endpoint loss, and no remaining `llama-server` process. Focused lifecycle and adapter tests (4 passed), `./gradlew.bat compileKotlin`, and `./gradlew.bat check` passed on 2026-10-10. The original ZIP was not present, so no separate archive-hash verification was possible.  
**Final status:** VERIFIED

After implementation, record:

- Selected inference backend.
- Architecture decision and supporting evidence.
- Added or modified source files.
- Runtime installation or acquisition requirements.
- Executed tests and outcomes.
- Local smoke-test result.
- Acceptance criteria satisfied.
- Known limitations.
- Final task status.

Mark TASK-004 `VERIFIED` only after its mandatory acceptance criteria and verification requirements are satisfied.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-004`.

The corresponding Codex skill must evaluate runtime candidates before implementation, preserve provider-neutral
contracts, and verify lifecycle behavior without expanding into TASK-005 functionality.
