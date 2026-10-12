# ADR-001: Managed Local Inference Runtime Boundary

**Status:** ACCEPTED  
**Implementation:** VERIFIED — TASK-004 and TASK-005  
**Milestone:** B — Base Plugin MVP

## Context

Integrated AI Workbench must support user-supplied local language models without requiring LM Studio, Ollama, cloud
APIs, or another independently operated AI application.

Local inference is a native Workbench capability. The Base Plugin owns the execution lifecycle while preserving
compatibility with different inference backends.

The architecture provides:

- `ModelProfile` for model configuration and generation parameters.
- Runtime capability and compatibility contracts.
- `LocalRuntimeManager` for managed runtime lifecycle and resource control.
- `LocalInferenceAdapter` for provider-neutral inference execution.
- A configured llama.cpp `llama-server.exe` adapter for the initial implementation.

The Base Plugin does not bundle or automatically download inference binaries or language models.

Model files and runtime executables remain user-supplied resources stored outside the plugin's source code.

The architecture must preserve the ability to introduce compatible inference backends without changing the core
application, agent, or user-interface contracts.

## Decision

Keep the Workbench core provider-neutral and implement local inference behind `LocalInferenceAdapter` and
`LocalRuntimeManager`.

The Workbench is responsible for:

- Runtime selection and configuration.
- Model and runtime compatibility checks.
- Runtime initialization and startup.
- Model loading and execution.
- Runtime health monitoring.
- Request timeouts and cancellation.
- Controlled failure handling and recovery.
- Process termination and resource cleanup.

### Selected Runtime

The initial inference backend is a Workbench-managed `llama-server.exe` subprocess from the
official [ggml-org/llama.cpp](https://github.com/ggml-org/llama.cpp) releases.

The initial implementation targets:

- Windows x64.
- GGUF language models.
- CPU inference.
- Compatible NVIDIA CUDA builds when explicitly configured.

The Workbench launches the runtime using a validated, configured absolute executable path.

The managed server must:

- Bind to `127.0.0.1`.
- Use a locally managed endpoint.
- Expose a verifiable health state.
- Support bounded inference requests.
- Respond predictably to failures and cancellation.
- Shut down without leaving orphaned Workbench-owned processes.

The runtime executable must not be selected from an untrusted implicit search path.

### Binary Verification and Configuration

Runtime binary verification is part of the managed execution boundary.

The Workbench validates the configured executable using applicable release metadata, artifact identity, and a
caller-provided SHA-256 fingerprint before startup.

The initial implementation uses the official llama.cpp release distribution.

The upstream project is MIT-licensed. Official release artifacts and available release attestations provide reference
information for operator verification.

A specific local artifact must still be checked before configuration.

The plugin must not hardcode or automatically download:

- Runtime binaries.
- Language-model files.
- Machine-specific executable paths.
- Release artifacts.
- Expected binary hashes.

Expected fingerprints and local paths belong to explicit configuration.

Runtime files and model binaries must remain outside source control.

### Runtime Ownership

The Workbench owns the lifecycle of every runtime process it starts.

For a managed subprocess, lifecycle ownership includes:

1. Validating the executable and configuration.
2. Starting the process with controlled arguments.
3. Monitoring initialization and readiness.
4. Performing model inference.
5. Handling timeouts, cancellation, and failures.
6. Performing graceful shutdown.
7. Terminating the process when graceful shutdown fails.
8. Releasing associated resources.

The implementation must avoid uncontrolled duplicate runtime processes.

For any future in-process inference implementation, equivalent lifecycle, isolation, and resource-management
requirements apply.

### Model Ownership

Language-model files remain user-owned.

The Workbench references models through validated local paths and registered model profiles.

Models are not bundled with the Base Plugin or committed to source control.

The managed runtime must operate within the selected model's compatible hardware and runtime requirements.

### Rejected Alternatives

**Embedded native inference library**

Not selected for the initial implementation because it introduces additional native-library loading, platform
compatibility, and plugin-distribution requirements.

It remains a possible future backend behind the existing inference interfaces.

**Independently managed inference servers**

Not selected for the Base Plugin MVP because they would require users to operate an external AI application
independently of the Workbench lifecycle.

**LM Studio and Ollama**

Not selected as mandatory inference dependencies because the Base Plugin must manage its own local runtime.

**Cloud inference providers**

Not selected for the initial local execution path because the MVP must work without mandatory cloud connectivity.

Optional external providers may be introduced later through explicitly configured interfaces and policies.

## Runtime Evaluation Criteria

The selected implementation was evaluated against:

- Platform and model-format compatibility.
- CPU/GPU support and hardware requirements.
- Runtime lifecycle and resource ownership.
- Execution timeouts and cancellation.
- Structured-output support and compatibility with Workbench-side validation.
- Security and executable verification.
- Licensing and packaging impact.
- Offline operation without independently operated AI applications.
- Provider-neutral shared contracts.
- Deterministic testing through replaceable adapters.

The selected implementation preserves the established `LocalInferenceAdapter` and `LocalRuntimeManager` boundaries.

A fake inference adapter remains useful for deterministic tests but cannot substitute for the required real-model
verification.

## Security Boundaries

Local inference execution must respect the Workbench's established security architecture.

Required controls include:

- Validated runtime executable paths.
- Binary identity and fingerprint verification.
- Controlled local server binding.
- Bounded request and response sizes.
- Execution timeouts.
- Cancellation and process cleanup.
- Safe error reporting.
- Protection of sensitive configuration values.
- Structured-output validation.
- Explicit local and remote execution policies.

Credentials and other sensitive values must not be exposed through logs or avoidable process arguments.

Local execution does not automatically establish trustworthy model output.

Model-generated responses remain untrusted until validated under the applicable application policies.

Structured output must be validated before it is used as application data or considered for tool execution.

### Separation from IDE Tool Execution

The local inference runtime and IDE tool execution are separate architectural responsibilities.

The managed llama.cpp runtime generates model responses and structured tool-call proposals when supported by the
selected model and execution protocol.

It does not directly grant access to IntelliJ IDEA tools or authorize operating-system operations.

The Workbench agent execution coordinator and security services are responsible for validating tool proposals and
enforcing permissions.

The IntelliJ MCP client integration belongs to TASK-009 and uses the existing IDE MCP Server.

Its selected STDIO transport does not change the managed local inference boundary defined by this decision.

## Consequences

This decision establishes the following architectural properties:

- Local inference remains a native Base Plugin capability.
- External AI applications and cloud providers are not mandatory.
- Runtime selection depends on model requirements and hardware compatibility.
- Local-only policies fail closed when no compatible managed runtime is available.
- The Tool Window and runtime agents depend on shared contracts rather than provider-specific implementations.
- Runtime processes, native resources, and model memory are released appropriately.
- Runtime binaries and model files remain outside source control.
- Executable verification and runtime lifecycle ownership are explicit responsibilities.
- Structured model outputs are validated before downstream processing.
- Additional inference backends can be introduced without replacing the Workbench core architecture.
- IDE tool access and authorization remain separate from inference runtime management.

## Implementation Status

### TASK-004 — Managed Local Runtime

**Status: VERIFIED**

`LlamaServerAdapter` implements the configured runtime process lifecycle and capability declaration.

Deterministic lifecycle and adapter tests passed.

The selected reference runtime is:

| Property        | Verified reference                 |
|-----------------|------------------------------------|
| Upstream        | `ggml-org/llama.cpp`               |
| Release         | `b11429`                           |
| Artifact        | `llama-b11429-bin-win-cpu-x64.zip` |
| Executable      | `llama-server.exe`                 |
| Reported build  | `11429`                            |
| Reported commit | `d81235049`                        |
| Platform        | Windows x86_64                     |
| Backend         | CPU                                |

The locally extracted executable reported the following SHA-256:

```text
7E236243C9B670C6A49410C03186FD575C57A0EFE3FD8A21D8727503D571D5B6
```

This fingerprint records the verified local reference artifact. It is not a hardcoded production requirement for other
compatible llama.cpp releases.

The CPU runtime DLL set was present.

A real Workbench-owned localhost lifecycle smoke test successfully:

1. Started the configured runtime executable.
2. Loaded a user-supplied GGUF model.
3. Reached the runtime `/health` endpoint.
4. Completed controlled shutdown.
5. Terminated without leaving a running `llama-server` process.

The original ZIP archive was not available for a separate archive-level SHA-256 check. Verification evidence therefore
applies to the extracted executable and observed runtime behavior, not to an independently verified ZIP hash.

### TASK-005 — Real Local Inference

**Status: VERIFIED**

Real bounded text generation was verified through the managed llama.cpp runtime using a user-supplied local GGUF model.

The verification established:

- Actual local inference rather than a fake adapter response.
- Invocation through the existing managed runtime.
- Bounded model generation.
- Typed execution results.
- Safe response provenance.
- Managed runtime shutdown.

The verified inference path does not require LM Studio, Ollama, Codex CLI, or a cloud AI provider.

### TASK-008 — Agent Execution Integration

**Status: VERIFIED**

TASK-008 verified that Markdown-configured runtime agents use the established managed inference path through the
Workbench agent execution coordinator.

Execution is subject to application-enforced:

- Agent and skill configuration.
- Controlled project grounding.
- Prompt construction.
- Input and output validation.
- Structured-response validation.
- Tool-permission checks.

TASK-008 validates and authorizes structured tool proposals but does not itself establish actual MCP tool execution.

### TASK-009 — IntelliJ MCP Tool Execution

**Status: NOT VERIFIED under this ADR**

TASK-009 integrates the local agent execution workflow with existing IntelliJ IDEA MCP tools through a Workbench MCP
client.

The selected MCP transport is STDIO.

Real IDE tool execution and subsequent model continuation must be verified independently under TASK-009 and TASK-011.

This integration does not replace or modify the inference runtime decision established by ADR-001.

## Verification and Maintenance

Implementation evidence is maintained in task reports under `_local/reports/task-reports/`, excluded from Git.

Future inference backend changes must preserve the existing architectural boundaries or be supported by an explicit new
architectural decision.

Changes to model execution transport, runtime packaging, binary verification, or lifecycle ownership must be evaluated
against this ADR.

The authoritative requirements and implementation references are:

- [Base Plugin Architecture](README.md)
- [AI Requirements](../development/AI-REQUIREMENTS.md)
- [Development Roadmap](../development/ROADMAP.md)
- [Implementation Tasks](../development/tasks/README.md)
- [Security Architecture](../security/README.md)

**Decision remains ACCEPTED.**