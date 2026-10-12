# TASK-011: MVP Integration and Verification

**Status:** BLOCKED  
**Dependencies:** TASK-009, TASK-010  
**Milestone:** B — Base Plugin MVP

## Objective

Verify the integrated Integrated AI Workbench Base Plugin MVP against its documented functional, security, runtime, and
reliability requirements.

The completed workflow must operate with a real user-supplied local language model through a Workbench-managed inference
runtime and be accessible through the IntelliJ Tool Window.

Verification must distinguish automated fake-adapter testing from actual local-model operation.

The purpose of this task is functional acceptance and reproducibility, not preparation for a separate presentation.

## Scope

### Integration readiness

Confirm that TASK-001 through TASK-010 have satisfied their mandatory acceptance criteria.

Verify that the integrated implementation preserves:

- Shared execution contracts.
- Validated model registration.
- Hardware and runtime compatibility assessment.
- Managed runtime lifecycle.
- Actual local inference.
- Runtime Markdown loading.
- Controlled project grounding.
- Prompt composition.
- Security policy enforcement.
- Agent execution.
- Structured output.
- IntelliJ-native interaction.
- Deterministic evaluation.
- LLM-as-a-Judge.

Do not treat the existence of a class or a successful compilation as evidence that its functionality is operational.

### Automated integration verification

Implement or complete focused integration tests using deterministic fixtures and fake adapters where appropriate.

Tests must cover relevant interactions across:

- Model profiles and execution configuration.
- Registered model paths.
- Hardware and runtime matching.
- Runtime lifecycle and cancellation.
- Inference request and response contracts.
- Agent and skill loading.
- Controlled grounding and prompt roles.
- Input, secret, output, and tool-security policies.
- Structured-response validation.
- Deterministic evaluation.
- Judge-result validation and precedence.

Automated tests must not require a particular GPU, cloud service, or downloaded model.

Verify failure behavior, not only successful execution.

### Real local-model verification

Successful inference using a real local model is mandatory.

Use a compatible user-supplied local model and the backend selected in TASK-004.

Verify:

- Model registration and selection.
- Local model-path validation.
- Hardware and runtime compatibility handling.
- Workbench-managed runtime startup.
- Model initialization or loading.
- Submission of a real inference request.
- Actual model-generated response.
- Typed response and metadata.
- Runtime cancellation or timeout behavior where supported.
- Safe shutdown and resource cleanup.

The Workbench must not require LM Studio, Ollama, a cloud provider, or another independently operated AI application.

A fake adapter cannot satisfy this requirement.

If a compatible model or runtime is unavailable, record the limitation and leave the task incomplete.

### Integrated agent workflow

Verify actual execution through the configured runtime agent system.

The integration must establish that:

- A validated runtime agent can be selected.
- Applicable Markdown instructions are loaded.
- A permitted runtime skill is applied.
- Controlled grounding reaches prompt composition.
- System, task, context, and user roles remain distinguishable.
- Supported model configuration reaches the runtime.
- Model output returns through the existing coordinator.
- Applicable output validation is enforced.
- Safe execution provenance is retained.

The workflow must use the same functional backend rather than a separate test-only execution path.

### Prompting capabilities

Verify the implemented prompt configuration options.

Include applicable checks for:

- Zero-shot execution.
- One-shot or few-shot execution.
- Runtime-loaded examples.
- Bounded example and context sizes.
- Supported sampling parameters.
- Supported thinking-level settings or explicit rejection of unsupported values.

Zero-shot and example-guided execution should be comparable under consistent model and runtime settings.

This is a functional configuration and verification requirement, not a requirement to establish that one prompting
approach always produces better answers.

### Structured output and security

Verify that the integrated workflow:

- Enforces approved filesystem boundaries.
- Excludes unauthorized or unsupported files.
- Applies relevant input and context limits.
- Protects sensitive information according to defined policies.
- Rejects unauthorized tool operations.
- Validates required structured output.
- Distinguishes malformed responses from successful results.
- Does not execute commands or modify files based on model-generated instructions.
- Does not fall back to remote inference when local execution fails.

Security checks must be implemented and verified programmatically.

Do not rely on agent instructions as the sole protection mechanism.

### Deterministic evaluation and local judge

Verify deterministic checks and LLM-as-a-Judge integration.

Required results include:

- Repeatable deterministic validation.
- Structured judge assessment using an explicit rubric.
- Actual judge inference through a compatible Workbench-managed local model.
- Correct handling of judge errors or unavailable states.
- Deterministic failures retaining precedence over judge scores.
- Safe evaluation provenance.

The judge may reuse the same local model used for ordinary agent execution.

Fake judge fixtures remain useful for automated tests but do not replace verification of real judge inference.

### IntelliJ Development Instance

Verify the operational workflow in the IntelliJ Development Instance.

Confirm that:

- The plugin loads successfully.
- The AI Workbench Tool Window opens.
- Registered model profiles are available for selection.
- Loaded agents can be selected.
- Approved project context can be selected.
- Users can submit requests.
- Active execution does not freeze IntelliJ.
- Execution state is visible.
- Results and errors are displayed safely.
- Cancellation behaves consistently.
- The Tool Window can present a validated result from actual local inference.

Use a separate development environment and avoid modifying unrelated user project files.

### Error and recovery verification

Verify representative failure paths, including:

- Missing or relocated local model.
- Unsupported model or runtime configuration.
- Insufficient or unknown hardware capability.
- Invalid agent or skill definition.
- Unauthorized grounding path.
- Rejected input.
- Invalid structured output.
- Inference timeout.
- User cancellation.
- Runtime termination.
- Unavailable or malformed judge result.

Errors must remain typed, actionable, and safe for display.

Where automated coverage is impractical for a platform-specific scenario, document the limitation explicitly.

### Documentation and verification records

Update applicable project documentation to reflect the implemented system.

Relevant documents include:

- Root README and architecture documentation where behavior has changed.
- Runtime architecture decision record.
- Security architecture.
- Development roadmap.
- `docs/development/AI-REQUIREMENTS.md`
- Relevant TASK-001 through TASK-011 completion records.

Documentation must distinguish:

- Verified functionality.
- Implemented but unverified functionality.
- Unsupported capabilities.
- Outstanding limitations.
- Future work.

Remove claims that are not supported by actual execution evidence.

Do not introduce separate presentation or showcase requirements.

## Affected Files

Primary scope:

- Focused automated integration tests.
- Existing integration points requiring minimal corrections.
- Development Instance verification instructions.
- Applicable product and development documentation.
- Task completion and verification records.

Avoid unrelated refactoring or new product functionality.

## Implementation Boundaries

- Do not introduce cloud inference or mandatory external AI applications.
- Do not download or bundle user model files.
- Do not introduce new inference backends without a justified blocking issue.
- Do not add JetBrains Marketplace publication work.
- Do not expand into advanced RAG, training, or unrelated integrations.
- Do not weaken security or acceptance criteria merely to make tests pass.
- Do not call fake inference evidence of actual model execution.
- Do not require a separate presentation or demonstration.
- Do not modify remote Git branches without explicit authorization.

## Acceptance Criteria

TASK-011 is complete when:

1. All prerequisite implementation tasks satisfy their mandatory acceptance criteria.
2. Relevant automated integration tests pass.
3. The plugin builds and passes applicable IntelliJ Platform verification checks.
4. The IntelliJ Development Instance loads the plugin successfully.
5. A compatible user-supplied local model executes real inference through a Workbench-managed runtime.
6. The Tool Window supports functional model and agent interaction.
7. Runtime Markdown instructions are loaded and applied.
8. Approved grounding and prompt composition operate correctly.
9. Zero-shot and example-guided prompting are supported and verified.
10. Supported generation settings reach the runtime, and unsupported settings are handled explicitly.
11. Structured output and required security policies are enforced.
12. Relevant errors, cancellation, and failure states are handled safely.
13. Deterministic evaluation produces reliable results.
14. A real local judge can evaluate an applicable result using a defined rubric.
15. Deterministic validation failures cannot be overridden by the judge.
16. Execution and evaluation metadata are accurate and appropriately sanitized.
17. Verified functionality and known limitations are documented.
18. No mandatory cloud service or independently operated AI application is required.

A successful fake-adapter run alone does not satisfy MVP completion.

## Verification

### Automated checks

Run focused integration tests and the applicable Gradle checks:

```powershell
.\gradlew.bat check
.\gradlew.bat verifyPluginStructure
.\gradlew.bat build
```

Run additional supported plugin verification tasks only when configured and applicable.

Record the exact commands and results.

### Development Instance verification

Start the plugin Development Instance:

```powershell
.\gradlew.bat runIde
```

Verify the functional Tool Window workflow, including configuration, execution, cancellation, error handling, and result
presentation.

### Real-model verification

Execute a bounded request through the actual Workbench runtime and Tool Window using a compatible local model.

Verify at least one successful response, appropriate execution metadata, and controlled resource cleanup.

Also verify the applicable judge workflow using actual local inference.

Record the relevant environment, model format, inference backend, procedure, outcome, and limitations without committing
model binaries, credentials, or sensitive paths.

### Completion decision

Milestone B may be marked complete only when mandatory functionality has been implemented and verified.

If a required check cannot be performed, record it as unverified rather than passed.

Unresolved mandatory acceptance criteria prevent final `VERIFIED` status.

## Completion Record

**Implementation:** NOT STARTED  
**Automated verification:** NOT RUN  
**Development Instance verification:** NOT RUN  
**Real-model verification:** NOT RUN  
**Real-judge verification:** NOT RUN  
**Final status:** BLOCKED

After verification, record:

- Applicable completed tasks.
- Executed commands and actual outcomes.
- Real-model and judge verification results.
- Development Instance observations.
- Functional and security acceptance results.
- Unsupported or unverified scenarios.
- Final task and milestone status.

Mark TASK-011 `VERIFIED` only when all mandatory acceptance criteria have been satisfied.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-011`.

The implementation skill must verify the complete operational MVP, preserve actual execution evidence, and avoid
treating simulated workflows as proof of real local-model functionality.
