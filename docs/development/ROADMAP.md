# Integrated AI Workbench — Development Roadmap

**Project status:** Milestone A complete; Milestone B in progress.

This roadmap defines the implementation sequence for Integrated AI Workbench. Development prioritizes working,
independently verifiable capabilities over disconnected components or placeholder implementations.

Detailed specifications, dependencies, and acceptance criteria are maintained
in [Implementation Tasks](tasks/README.md).

TASK-001 through TASK-011 share the [skill-iwb-implement workflow](../../.codex/skills/skill-iwb-implement/SKILL.md).

The workflow supports:

- `$skill-iwb-implement TASK-XXX` — execute one selected task.
- `$skill-iwb-implement ALL` — execute or resume dependency-ready tasks, preserving verified work and reporting
  remaining blockers.

Keep task-specific requirements in the individual specifications. Execution reports are stored under
`_local/reports/task-reports/task-XXX.md` and excluded from Git.

The planning workflow maintains these shared references instead of generating individual implementation skills.

## Milestone A — Plugin Bootstrap

**Status: COMPLETE**

Established and verified:

- Kotlin and Gradle project configuration.
- IntelliJ Platform plugin descriptor and registration.
- IntelliJ-native Tool Window startup.
- Logical frontend, shared, and backend boundaries.
- Initial contracts for models, runtimes, agents, grounding, prompting, security, and evaluation.
- Successful plugin build and Development Instance launch.

**Outcome:** A working IntelliJ plugin foundation without operational AI inference.

## Milestone B — Base Plugin MVP

**Status: IN PROGRESS**

The objective is a functional, local-first AI Workbench that manages local model execution and provides configurable,
secure, tool-enabled AI workflows within IntelliJ IDEA.

The Base Plugin includes controlled access to existing IntelliJ IDE capabilities. Authorized agent tool execution is
part of the core workflow, not an optional external integration.

### Phase B1 — Execution Contracts and Model Preparation

**Tasks:** TASK-001–TASK-003

- Define shared execution contracts and model profiles.
- Register user-supplied models using validated local paths.
- Detect hardware capabilities and determine runtime compatibility.
- Configure supported generation, sampling, and reasoning parameters.

**Outcome:** The Workbench can validate model configurations and determine local execution requirements.

### Phase B2 — Managed Local Inference

**Tasks:** TASK-004–TASK-005

- Select and integrate a compatible inference backend.
- Implement Workbench-managed runtime lifecycle and resource handling.
- Support model loading, inference, cancellation, error handling, and shutdown.
- Verify actual inference using a user-supplied local model.

**Outcome:** Local model inference works without LM Studio, Ollama, cloud APIs, or independently operated AI
applications.

### Phase B3 — Agent Workflows and Security

**Tasks:** TASK-006–TASK-008

- Load agents, skills, and prompt templates dynamically from Markdown.
- Support controlled project grounding.
- Preserve system, task, context, and user prompt boundaries.
- Support zero-shot, one-shot, and few-shot prompting.
- Enforce input, output, file-access, and tool-permission policies.
- Validate structured model responses.
- Establish typed agent execution contracts and authorization boundaries for subsequent tool execution.

**Outcome:** Local inference operates through reusable agent workflows with controlled context, validated output, and
programmatically enforced security boundaries.

Tool authorization established in this phase does not by itself constitute actual tool execution. Operational tool
dispatch is integrated in Phase B4.

### Phase B4 — User Experience, IDE Tools, and Evaluation

**Tasks:** TASK-009–TASK-010

#### TASK-009 — Tool Window and MCP Tool Execution

- Connect AI execution workflows to the IntelliJ Tool Window.
- Provide model selection, prompt input, execution feedback, cancellation, and result presentation.
- Integrate with IntelliJ IDEA's existing MCP Server through a supported Workbench MCP client.
- Discover available IDE tools and their input schemas.
- Expose only permitted tools to the selected local agent.
- Validate structured model-proposed tool calls before execution.
- Execute authorized tool calls through the MCP client.
- Return validated tool results to the local model for bounded continuation.
- Require explicit, operation-specific user approval for consequential actions.
- Enforce tool-call limits, timeouts, cancellation, and safe cleanup.
- Display tool execution activity, approval requests, failures, and results.
- Verify an actual read-only IntelliJ MCP tool call and subsequent local-model response.

Reuse IntelliJ's existing IDE tools rather than implementing equivalent capabilities independently.

The Workbench must not treat model-generated tool requests as self-authorizing instructions.

#### TASK-010 — Evaluation and Judge

- Implement deterministic response validation and evaluation.
- Integrate LLM-as-a-Judge with configurable evaluation rubrics.
- Maintain deterministic validation and security failure precedence over judge assessments.
- Preserve bounded, explicitly configured local judge execution.
- Verify evaluation outcomes against the applicable task requirements.

**Outcome:** Users can execute, inspect, and evaluate local AI workflows directly within IntelliJ IDEA, including
permission-controlled IDE tool use.

### Phase B5 — MVP Integration and Verification

**Task:** TASK-011

- Verify the complete execution pipeline using a real local model.
- Validate model configuration, grounding, prompt composition, and Markdown instruction loading.
- Verify structured output and security enforcement.
- Validate deterministic evaluation and LLM-as-a-Judge.
- Verify real IntelliJ MCP tool discovery and authorized tool execution.
- Verify a complete model-driven tool cycle: tool proposal, permission validation, IDE execution, validated tool result,
  and local-model continuation.
- Confirm that unauthorized or malformed tool requests cannot execute.
- Verify required user approval for consequential operations.
- Exercise relevant error, timeout, cancellation, disconnection, and recovery scenarios.
- Validate integrated Tool Window behavior and resource cleanup.
- Record reproducible verification evidence.

Real local inference, real IDE tool execution, and applicable real local judge verification must not be replaced by
fake-adapter results.

**Outcome:** An operational Base Plugin MVP with validated local inference, agent workflows, controlled IDE tool
execution, and evaluation.

## Milestone C — Optional Extensions

**Status: FUTURE**

Potential extensions include:

- Optional remote and cloud inference providers.
- Additional local inference backends and model formats.
- Advanced retrieval-augmented generation.
- Additional external MCP servers and specialized tool integrations.
- Advanced tool workflows and extended automation capabilities.
- Multi-agent orchestration.
- Model fine-tuning and advanced evaluation.
- Extended IntelliJ Remote Development support.
- Distribution and marketplace preparation.

These extensions are not prerequisites for Milestone B.

The core capability to use authorized IntelliJ IDE tools through the supported MCP integration belongs to Milestone B.
Additional tool providers, external MCP servers, and advanced automation remain optional extensions.

## Development Principles

- **Local-first:** Local inference is a native Workbench capability.
- **Provider-neutral:** Core interfaces remain independent of specific inference engines.
- **Tool-enabled agents:** Agents may discover and execute explicitly authorized tools through controlled interfaces.
- **IDE integration:** Prefer existing IntelliJ Platform capabilities and supported MCP interfaces over duplicate IDE
  tooling.
- **Permission control:** Model-generated tool requests never grant execution authority.
- **User approval:** Consequential tool operations require explicit authorization.
- **Incremental delivery:** Every implementation task produces verifiable progress.
- **Security by enforcement:** Security boundaries are enforced programmatically.
- **Evidence-based development:** Completion requires implemented functionality verified against documented acceptance
  criteria.
- **Controlled scope:** Dependencies and architectural changes must be justified.

## MVP Completion Criteria

Milestone B is complete when Integrated AI Workbench can execute a real user-supplied local model through a
Workbench-managed runtime, with fully functional model configuration, agent execution, grounding, prompting, structured
output, security, controlled IDE tool execution, and evaluation integrated into IntelliJ IDEA.

Mandatory integrated verification must establish:

- Actual local inference with a compatible user-supplied model.
- Runtime agent and skill loading.
- Programmatically enforced grounding, security, and authorization boundaries.
- Real IntelliJ MCP tool execution followed by local-model continuation.
- Prevention of unauthorized tool execution.
- Correct user approval behavior for consequential operations.
- Applicable real local LLM-as-a-Judge execution.
- Responsive Tool Window operation, cancellation, and cleanup.
- Successful mandatory automated and integration checks.

Mock adapters remain useful for deterministic testing but cannot replace verification against real local models and real
IDE tool execution.

Completion requires all mandatory tasks to be `VERIFIED` with recorded evidence.

## Related Documentation

- [Development Guide](README.md)
- [Implementation Tasks](tasks/README.md)
- [AI Requirements](AI-REQUIREMENTS.md)
- [Base Plugin Architecture](../architecture/README.md)
- [Security Architecture](../security/README.md)
- [Managed Local Inference Decision](../architecture/ADR-001-managed-local-runtime.md)