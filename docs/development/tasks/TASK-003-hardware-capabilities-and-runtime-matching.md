# TASK-003: Hardware Capabilities and Runtime Matching

**Status:** VERIFIED  
**Dependencies:** TASK-001  
**Milestone:** B — Base Plugin MVP

## Objective

Implement reliable hardware capability detection and deterministic compatibility matching for local model execution.

The Workbench must identify available system resources, compare model requirements with declared inference-runtime
capabilities, and explain whether a selected model profile can be executed locally.

Compatibility checks must occur before model loading whenever sufficient information is available.

The implementation must distinguish verified compatibility, known incompatibility, and insufficient information.

## Scope

### Hardware capability detection

Extend the existing `HardwareDetector` and related contracts to produce a typed hardware capability snapshot.

Collect safely available information, including:

- Operating system and version.
- JVM and native operating-system architecture.
- Available CPU processor count.
- Total physical system memory, when accessible.
- Estimated available physical memory, when accessible.
- JVM memory limits, represented separately from physical RAM.
- Optional GPU or other accelerator information, when reliably available.
- Accelerator memory capacity and availability, when reliably available.

Unavailable information must be represented explicitly as unknown rather than inferred or replaced with invented values.

Hardware detection must not install drivers, modify the operating system, or require elevated permissions.

Native hardware detection may be introduced in a later task if the selected inference backend requires it.

### Memory representation

Represent memory resources with clear types and units.

Distinguish:

- Physical system RAM.
- Estimated available system RAM.
- JVM heap limits.
- GPU dedicated memory (VRAM), when known.
- Model memory requirements, when available.
- Runtime overhead and configurable memory reserves.

JVM heap capacity must not be used as a substitute for total system RAM or native inference-process memory.

Do not assume that CPU RAM and GPU VRAM can be combined into a single usable memory pool.

Do not infer model memory requirements solely from file size.

When reliable memory requirements or available-memory estimates are missing, the compatibility result must reflect that
uncertainty.

### Accelerator capabilities

Accelerator information may include:

- Device type and vendor.
- Device identifier or descriptive name.
- Reported dedicated memory.
- Available compute backend, where verifiable.
- Relevant runtime compatibility information.

Unknown or unavailable accelerator details must not prevent CPU-only compatibility assessment when a candidate runtime
explicitly supports CPU inference and sufficient CPU-side requirements are known.

The presence of a GPU does not establish support for CUDA, Vulkan, Metal, or another acceleration backend.

Acceleration support must be declared by the selected inference runtime and checked against the detected environment.

### Runtime capability contracts

Use the provider-neutral runtime capability contracts established or extended by TASK-001.

Runtime capability descriptors must support applicable information such as:

- Supported operating systems and architectures.
- Supported model formats.
- Supported execution devices or accelerator backends.
- Required hardware resources.
- Structured-output capabilities.
- Supported thinking-level settings.
- Relevant generation and execution limits.

Use existing fields and types wherever they are sufficient.

A runtime capability descriptor is a declaration to be validated during runtime integration, not proof that a runtime
binary is installed or operational.

TASK-004 is responsible for selecting and verifying the actual inference backend.

### Compatibility matching

Implement a deterministic matcher that evaluates:

- Model format and runtime-format support.
- Operating-system and architecture compatibility.
- Local-only execution policy.
- Model and runtime memory requirements.
- Available hardware and declared accelerator support.
- Structured-output requirements and available validation strategies.
- Requested thinking level and declared runtime support.
- Other required execution capabilities represented by the shared contracts.

Compatibility matching must operate on supplied capability snapshots and runtime descriptors.

It must not depend on UI state, network access, or a running language model.

The matcher must preserve provider neutrality and avoid hard-coded assumptions about specific inference engines.

### Compatibility results

Return a typed result with one of three states:

| Result         | Meaning                                                                                                |
|----------------|--------------------------------------------------------------------------------------------------------|
| `COMPATIBLE`   | All mandatory requirements evaluated by the matcher are satisfied by known capabilities.               |
| `INCOMPATIBLE` | One or more mandatory requirements are known to be unsupported or insufficient.                        |
| `UNKNOWN`      | No known incompatibility exists, but available information is insufficient to establish compatibility. |

Each result must include machine-readable reason codes and concise, actionable explanations.

Relevant results should identify:

- The evaluated model or profile.
- The candidate runtime capability descriptor.
- Compatibility state.
- Satisfied requirements, where useful.
- Failed requirements.
- Missing or uncertain capability information.
- Applicable execution limitations.

A known mandatory incompatibility takes precedence over unknown optional information.

Do not silently convert `UNKNOWN` into `COMPATIBLE`.

A compatibility result is a pre-execution assessment, not a guarantee that model loading or inference will succeed.

### Structured output and reasoning support

Distinguish runtime-native structured generation from Workbench-side response validation.

When a profile requires native constrained generation, ordinary post-generation validation must not be treated as
equivalent support.

For thinking-level preferences:

- Evaluate the requested level against declared runtime capabilities.
- Preserve the distinction between unspecified/default and explicitly requested settings.
- Reject known unsupported requested levels.
- Return `UNKNOWN` when required runtime capability information is unavailable.
- Never require access to hidden model chain-of-thought.

Runtime-specific parameter mappings remain outside this task.

### Testability

Hardware collection and compatibility matching must be independently testable.

Use injectable hardware probes, immutable capability snapshots, and deterministic runtime descriptors.

Tests must not depend on the actual developer workstation's CPU, GPU, memory capacity, or installed inference software.

Use fixture data to represent different hardware and runtime configurations.

## Affected Files

Primary scope:

- Existing `HardwareDetector.kt` implementation.
- Existing shared `RuntimeCapabilities` and related contracts.
- Backend runtime matching and compatibility services.
- Relevant shared model-profile contracts from TASK-001.
- Focused backend and shared tests.

Inspect the current package structure before creating or modifying files.

Reuse existing contracts and preserve the single Gradle module with its established source-root boundaries.

## Implementation Boundaries

- Do not install drivers, runtimes, or model binaries.
- Do not introduce cloud services or remote hardware detection.
- Do not require LM Studio, Ollama, or independently operated AI applications.
- Do not benchmark hardware automatically.
- Do not load or execute language models.
- Do not implement runtime process lifecycle management.
- Do not modify model files or registry data.
- Do not assume accelerator support from GPU presence alone.
- Do not introduce unnecessary native libraries or dependencies.
- Do not create UI functionality.
- Do not modify unrelated architecture or remote Git branches.

Additional dependencies require verified necessity and compatibility.

## Acceptance Criteria

TASK-003 is complete when:

1. `HardwareDetector` produces a typed, testable hardware capability snapshot.
2. Operating-system, architecture, CPU, and memory information are represented safely and consistently.
3. Physical RAM, JVM heap, and GPU memory are distinguished.
4. Optional accelerator information supports explicit unknown values.
5. The compatibility matcher evaluates relevant model, profile, hardware, and runtime requirements.
6. Compatibility returns deterministic `COMPATIBLE`, `INCOMPATIBLE`, or `UNKNOWN` results.
7. Results include actionable reasons for failures and missing information.
8. Known incompatibilities are never silently treated as supported.
9. Unknown capabilities do not become unsupported guarantees or fabricated estimates.
10. CPU-only configurations can be evaluated without requiring GPU detection.
11. Structured-output requirements and requested thinking levels are evaluated against declared runtime capabilities.
12. Fixture-based tests verify matching behavior independently of actual local hardware.
13. Existing TASK-001 contracts and bootstrap functionality remain compatible.
14. Focused tests and required Gradle checks pass.

Compilation alone is insufficient for task completion.

## Verification

Run focused hardware-detection and runtime-matching tests, followed by:

```powershell
.\gradlew.bat check
```

Verification scenarios must include:

- Compatible CPU-only configuration.
- Insufficient physical memory.
- Unknown available memory.
- Missing GPU information.
- GPU detected but unsupported by the candidate runtime.
- Unsupported model format.
- Unknown or unverified model format.
- Unsupported operating-system architecture.
- Local-only policy incompatibility.
- Unsupported structured-output requirement.
- Unsupported thinking-level request.
- Incomplete runtime capability descriptor.
- Deterministic reason-code generation.

All compatibility tests must use controlled fixtures.

Tests must distinguish an established incompatibility from insufficient evidence.

Record commands, outcomes, and limitations only after execution.

## Completion Record

**Implementation:** Typed memory and accelerator snapshots plus deterministic provider-neutral runtime compatibility matching are implemented.  
**Verification:** `./gradlew.bat test --tests "com.jaanik.integratedaiworkbench.backend.runtime.RuntimeCompatibilityMatcherTest"` (6 passed); `./gradlew.bat cleanTest check --rerun-tasks` (PASS).  
**Final status:** VERIFIED

After implementation, record:

- Modified and added files.
- Hardware detection mechanisms used.
- Compatibility rules implemented.
- Tests executed and their results.
- Acceptance criteria satisfied.
- Known limitations and unsupported scenarios.
- Final task status.

Mark TASK-003 `VERIFIED` only after its acceptance criteria and required verification have been satisfied.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-003`.

The corresponding Codex skill must inspect existing hardware and runtime contracts before implementation, preserve
provider neutrality, use deterministic fixture-based tests, and report only verified results.
