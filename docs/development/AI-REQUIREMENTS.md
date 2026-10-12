# AI Engineering Principles and Capabilities

**Status:** PARTIAL — Milestone B implementation in progress

This document defines the AI engineering principles, implementation methods, architectural choices, and functional
capabilities of Integrated AI Workbench.

It connects the product architecture to concrete implementation tasks and verification criteria.

The purpose is not to prescribe one AI framework or inference technology. The Workbench must support practical AI
workflows through explicit configuration, well-defined execution boundaries, and verifiable behavior.

The root [README.md](../../README.md) defines the product objectives. The [architecture](../architecture/README.md)
establishes component responsibilities, and the [implementation backlog](tasks/README.md) defines the incremental
development work.

The [Development Roadmap](ROADMAP.md) defines the implementation sequence and milestone boundaries.

## 1. Architectural principles

### Local-first execution

Local inference is a primary Workbench capability.

The Workbench must be able to execute compatible user-supplied language models without requiring independently operated
AI applications or cloud services.

The Workbench manages the selected local runtime through its established execution interfaces.

Local-first means:

- User ownership of local model files.
- Explicit selection and configuration of models.
- Workbench-managed runtime lifecycle.
- Hardware-aware compatibility assessment.
- Local processing without mandatory cloud connectivity.
- Controlled project-data access.
- No implicit transmission of project content to remote providers.
- No silent fallback from local inference to cloud inference.

Offline operation must be possible when the necessary model files, compatible runtime resources, and required local
tools are available.

Local execution does not automatically guarantee security, correctness, or model quality. Those properties require
separate controls.

### Provider-neutral architecture

The core architecture must remain independent of any specific inference provider or engine.

`LocalInferenceAdapter`, `LocalRuntimeManager`, model profiles, and shared execution contracts define the boundaries
between application functionality and model execution.

The architecture may support different inference implementations, but the Base Plugin MVP requires only one properly
integrated and verified local backend.

Runtime-specific features must be exposed through declared capabilities rather than assumed to be universally available.

Tool integration must remain separate from the inference provider. A change of model or inference backend must not
require replacing the entire agent or tool architecture.

### Explicit capability negotiation

Model execution settings must be checked against the selected runtime.

The Workbench must distinguish:

- Requested capabilities.
- Declared runtime capabilities.
- Verified runtime compatibility.
- Effective execution configuration.
- Unsupported or unknown capabilities.

Unsupported explicit requirements must not be silently ignored.

The Workbench must not report uncertain compatibility as verified compatibility.

The same principle applies to tool use. Tool availability, runtime tool-calling support, and agent authorization must be
checked independently.

### Controlled extensibility

Future capabilities should extend established interfaces instead of requiring replacement of the core application
architecture.

Potential extensions must remain optional unless explicitly incorporated into the product requirements.

Avoid speculative abstractions that are not necessary for the current implementation.

The Base Plugin should reuse existing IntelliJ IDEA capabilities before introducing custom implementations of equivalent
tools.

## 2. AI methods and design choices

Integrated AI Workbench combines different AI methods according to their purpose.

The following table distinguishes the Base Plugin implementation direction from possible alternatives.

| Area                  | Base Plugin approach                                     | Alternative or extension                                    |
|-----------------------|----------------------------------------------------------|-------------------------------------------------------------|
| Inference location    | Workbench-managed local execution                        | Explicitly configured remote or cloud inference             |
| Inference integration | Provider-neutral adapter with a verified local backend   | Additional native libraries or managed processes            |
| Model management      | User-supplied local models and validated profiles        | Additional model sources and distribution workflows         |
| Hardware utilization  | Capability detection and compatible runtime selection    | Additional accelerator backends and optimization strategies |
| Prompting             | Structured prompt composition with configurable examples | Advanced prompt optimization and adaptive prompting         |
| Project knowledge     | Explicit, bounded project grounding                      | Retrieval-augmented generation with indexing and embeddings |
| Agent configuration   | Runtime-loaded Markdown agents and skills                | More sophisticated workflow and multi-agent orchestration   |
| Tool access           | Existing IntelliJ IDEA tools exposed through MCP         | Custom-built tools and integrations                         |
| Output handling       | Typed responses and deterministic structured validation  | Additional native constrained-generation formats            |
| Quality assurance     | Deterministic checks and optional local LLM-as-a-Judge   | Extended evaluation suites and human-feedback workflows     |
| User interaction      | IntelliJ-native Tool Window                              | Additional IDE surfaces and interaction modes               |

The Base Plugin uses existing IntelliJ IDEA MCP tools through a Workbench MCP client.

**STDIO is the selected MCP transport for Milestone B.**

Custom-built tools may be introduced when existing capabilities are insufficient or when a specific operation cannot
reasonably be provided through available IntelliJ tools.

These are architectural options, not a requirement to implement every alternative in Milestone B.

The managed local runtime approach is defined further in [ADR-001](../architecture/ADR-001-managed-local-runtime.md).

## 3. Core AI capabilities

The following requirements describe functional capabilities expected from the Base Plugin.

Each requirement is connected to implementation tasks and verification targets.

### Implementation status

- **IMPLEMENTED** — Required functionality exists and has been verified against its applicable acceptance criteria.
- **PARTIAL** — Contracts, configuration, or incomplete implementations exist, or further integrated verification
  remains necessary.
- **PLANNED** — Required functionality has not yet been implemented.
- **EXPERIMENTAL** — Functionality exists for investigation but has not satisfied its acceptance criteria.

The existence of a source file or successful compilation is not sufficient to establish functional completion.

Task verification and capability verification are related but distinct. A completed implementation task does not
automatically verify every dependent end-to-end capability.

### Capability mapping

| ID    | Capability                                     | Current implementation foundation                                             | Implementation tasks                   | Verification target                                                                  | Status      |
|-------|------------------------------------------------|-------------------------------------------------------------------------------|----------------------------------------|--------------------------------------------------------------------------------------|-------------|
| AI-01 | Model profiles and generation configuration    | Verified model contracts and registry implementation                          | TASK-001, TASK-002                     | Validated profiles and supported sampling parameters                                 | IMPLEMENTED |
| AI-02 | Reasoning and thinking-level configuration     | Runtime capability contracts and parameter handling                           | TASK-001, TASK-003, TASK-005, TASK-008 | Supported settings propagate correctly; unsupported settings are rejected            | PARTIAL     |
| AI-03 | Managed local inference and lifecycle          | Verified managed runtime and real local inference                             | TASK-002–TASK-005                      | Real inference, lifecycle, cancellation, and recovery                                | IMPLEMENTED |
| AI-04 | Hardware-aware compatibility                   | Hardware detection and runtime compatibility implementation                   | TASK-003, TASK-004                     | Deterministic hardware and runtime compatibility decisions                           | IMPLEMENTED |
| AI-05 | Markdown-defined agents and skills             | Runtime agent and skill loading                                               | TASK-006, TASK-008                     | Validated runtime loading and applied instructions                                   | IMPLEMENTED |
| AI-06 | Controlled grounding and prompt roles          | Grounding, sanitization, and prompt composition                               | TASK-007, TASK-008                     | Approved file context and preserved instruction boundaries                           | IMPLEMENTED |
| AI-07 | Input, output, and tool security               | Verified execution guards; operational tool authorization integration pending | TASK-007–TASK-009, TASK-011            | Enforced policies, approved tool execution, and rejection of unauthorized operations | PARTIAL     |
| AI-08 | Zero-shot, one-shot, and few-shot prompting    | Markdown instructions and prompt composition                                  | TASK-006, TASK-007, TASK-011           | Bounded examples and comparable execution configurations                             | PARTIAL     |
| AI-09 | Structured output and validation               | Verified typed agent execution and structured response validation             | TASK-008                               | Schema validation and typed failure outcomes                                         | IMPLEMENTED |
| AI-10 | IntelliJ-native interaction and tool execution | Operational bootstrap Tool Window; MCP execution workflow pending             | TASK-009, TASK-011                     | Model interaction, real IDE tool execution, continuation, and cancellation           | PARTIAL     |
| AI-11 | Deterministic evaluation                       | Evaluation contracts and ongoing implementation                               | TASK-010, TASK-011                     | Repeatable validation and explicit evaluation results                                | PARTIAL     |
| AI-12 | LLM-as-a-Judge                                 | Judge contracts and runtime agent definitions                                 | TASK-010, TASK-011                     | Bounded local judge inference with deterministic guard precedence                    | PARTIAL     |

These statuses reflect the documented implementation progress and must be reconciled with actual task verification
reports before release.

At the current planning checkpoint:

- TASK-001–TASK-008 have been reported as verified.
- TASK-009 is ready for implementation and real MCP verification.
- TASK-010 has ongoing implementation and verification work.
- TASK-011 depends on the required preceding integrations.

Requirements are complete only when the corresponding functionality has been verified against documented acceptance
criteria.

## 4. Prompt engineering

The Workbench must support structured prompt construction rather than relying on uncontrolled concatenation of
instruction text.

### Prompt roles

Preserve distinct origins and purposes for:

- System-level application instructions.
- Agent and task instructions.
- Approved grounding context.
- User input.
- Explicit examples.
- Output-format requirements.
- Available tool definitions, when tool use is enabled.
- Tool results returned during authorized execution.

Provider-specific serialization may vary, but the application must preserve the intended distinction between trusted
configuration and untrusted content.

Tool output is not a system instruction and must not be promoted to a higher-trust prompt role.

### Prompting strategies

Support the following strategies:

| Strategy                    | Description                                                   |
|-----------------------------|---------------------------------------------------------------|
| Zero-shot                   | Execute a request without task-specific examples              |
| One-shot                    | Include one relevant example                                  |
| Few-shot                    | Include multiple bounded examples                             |
| Grounded prompting          | Include explicitly approved contextual information            |
| Structured-output prompting | Specify the required response format or schema                |
| Tool-assisted prompting     | Provide permitted tool definitions and validated tool results |

Examples, grounding sources, and tool results must remain subject to input, context, and security limits.

The Workbench must support comparable zero-shot and example-guided executions using consistent model settings and
evaluation criteria.

No prompting strategy is assumed to outperform another in all situations.

### Generation configuration

Expose generation parameters only when they are applicable to the selected runtime.

Relevant controls may include:

- Output token limits.
- Temperature.
- Top-p.
- Top-k, when supported.
- Seed or reproducibility controls, when supported.
- Execution timeouts.
- Reasoning or thinking-level preferences, when supported.

Distinguish validated configuration from effective runtime behavior.

Unsupported explicit settings must produce a predictable response instead of being silently discarded.

Tool-calling features must likewise be checked against the selected runtime and model.

## 5. Reasoning and model behavior

Reasoning configuration is separate from prompt content and output evaluation.

The Workbench may expose provider-neutral thinking-level preferences where the selected model and runtime support them.

Required principles:

- Reasoning settings must be validated against declared capabilities.
- Runtime-specific mappings belong inside the appropriate adapter.
- An unsupported thinking setting must not be presented as active.
- A concise user-facing reasoning summary may be supported as ordinary model output.
- Hidden model chain-of-thought must not be requested, extracted, or stored.

Model-generated explanations must not be treated as reliable records of internal model computation.

Inference behavior and output quality are evaluated through observable results.

A model-generated tool request is a proposed action, not evidence that the operation is valid, necessary, or authorized.

The Workbench must make execution decisions through programmatically enforced rules rather than accepting the model's
assessment of its own permissions.

## 6. Grounding and knowledge access

Grounding provides relevant external context to model execution.

For the Base Plugin, the primary context source is explicitly approved project content.

The implementation must support:

- Authorized project roots and files.
- Canonical path validation.
- Context selection and size limits.
- Binary and unsupported-file exclusion.
- Secret filtering.
- Clear source provenance.
- Separation of grounding content from trusted instructions.

Grounding is not equivalent to unrestricted access to the project filesystem.

### Grounding and IDE tools

IntelliJ IDEA tools may provide additional project information during an authorized agent workflow.

Tool-based project access must respect applicable project, resource, and permission boundaries.

Enabling an IDE tool does not automatically authorize access to all project files or arbitrary system resources.

Tool results may contribute to the model's working context only after applicable validation, size limits, and
sanitization.

The Workbench should retain safe provenance identifying the tool and relevant project resource without exposing secrets
or unauthorized content.

### Grounding versus RAG

Direct project grounding and retrieval-augmented generation are related but distinct approaches.

The MVP uses bounded context selection and controlled prompt composition.

Advanced retrieval systems may later introduce:

- Document indexing.
- Embeddings.
- Semantic search.
- Reranking.
- Incremental context updates.
- Structure-aware retrieval.

These are possible extensions, not mandatory prerequisites for the first operational local inference workflow.

The architecture should permit future retrieval components without requiring them now.

## 7. Agents, skills, and tools

The Workbench separates model execution from agent configuration and tool authorization.

Existing IntelliJ IDEA tools are managed through IntelliJ IDEA MCP settings, where exposed tools can be enabled or
disabled individually.

Only tools available through the configured MCP connection and permitted by the selected agent may be used during a
project workflow.

Tool availability and agent authorization are separate conditions.

### Model

A model provides the underlying inference capability.

Its behavior depends on the model, runtime, generation configuration, prompt, and available context.

The local model may propose a tool call, but it does not directly control the MCP connection or determine its own
permissions.

### Agent

An agent defines a configured execution role using instructions, model requirements, permitted skills, tools, and
applicable policies.

An agent does not automatically receive filesystem, process, or network permissions.

Agent permissions must be enforced by Workbench application logic.

A selected agent may use an enabled tool only if the applicable security policy permits that operation.

### Skill

A runtime skill defines a reusable workflow or instruction set loaded from Markdown.

Skills may contain task guidance, examples, and output expectations.

Markdown content must not be interpreted as executable code.

Loading or selecting a skill does not grant additional tool permissions.

### Tool

A tool provides a capability that an agent can use.

The Workbench uses existing tools available through IntelliJ IDEA or custom-built tools when needed.

Existing IntelliJ IDEA tools are exposed through the IDE's MCP Server. Their availability is managed in IntelliJ IDEA
MCP settings.

Agents can use only available tools within their assigned permissions.

Tool execution must follow programmatically enforced security restrictions. Model-generated instructions alone do not
authorize unrestricted operating-system command execution.

### MCP integration

The Workbench acts as an MCP client and reuses IntelliJ IDEA's existing MCP Server.

**STDIO is the selected transport for the Base Plugin MVP.**

The MCP client must:

- Establish a supported MCP session.
- Discover available tools and their input schemas.
- Respect IntelliJ MCP exposure settings.
- Validate tool-call arguments.
- Apply Workbench agent permissions.
- Execute authorized operations.
- Return validated tool results to the local model.
- Handle failures, timeouts, cancellation, and resource cleanup.

The configured STDIO entry point may bridge communication to an existing IntelliJ MCP Server.

The implementation must verify the effective connection and target project rather than assuming that all running
IntelliJ instances share the same MCP Server or project.

Machine-specific executable paths, project paths, and ports belong in appropriate configuration, not in portable
application source code.

The detailed integration requirements are defined in [TASK-009](tasks/TASK-009-tool-window-workflow.md).

### Tool permissions

Tool execution requires all applicable authorization conditions:

1. The connected MCP Server exposes the requested tool.
2. The tool is enabled and accessible under IntelliJ IDEA MCP settings.
3. The selected runtime agent is permitted to use the tool.
4. The Workbench security policy permits the requested operation.
5. Tool arguments satisfy schema and resource-access restrictions.
6. Any required user approval has been granted.

Read-only operations may execute automatically when all applicable policy conditions are satisfied.

Operations with potential side effects require explicit, operation-specific approval.

These include project file modifications, terminal or process execution, Git write operations, configuration changes,
and destructive IDE actions.

An IDE setting that permits execution without confirmation does not override stricter Workbench approval requirements.

Unknown or unclassified operations must not receive automatic execution permission.

### Tool execution lifecycle

A tool-enabled agent workflow must support:

1. Presenting permitted tool definitions to the selected local model.
2. Receiving a structured tool-call proposal.
3. Validating the tool name, arguments, and permissions.
4. Obtaining any required user approval.
5. Executing the authorized operation through the MCP client.
6. Validating and limiting the returned tool result.
7. Supplying that result to the local model.
8. Continuing inference until a validated final response or execution limit is reached.

Tool execution must be bounded by call counts, continuation limits, timeouts, and cancellation.

Tool results must not override system instructions, security policies, or agent permissions.

### Development versus runtime instructions

Maintain strict separation between:

- `.codex/skills/` — development-agent workflows used to build and review the project.
- `.ai/agents/` — runtime agents used by Integrated AI Workbench.
- `.ai/skills/` — reusable runtime AI workflows.

A Codex development skill is not automatically a Workbench runtime capability.

Runtime agents must explicitly permit applicable tools.

The Judge Agent remains separate from operational IDE tool execution unless a future approved requirement changes that
boundary.

## 8. Security and trust boundaries

AI output and external context must be treated as untrusted unless validated.

The Workbench must enforce security through application code, not through prompt instructions alone.

Required controls include:

- Input validation.
- Output validation.
- Controlled filesystem access.
- Secret protection.
- Resource and execution limits.
- Structured-response validation.
- Tool-permission enforcement.
- MCP tool-call argument validation.
- Operation-specific user approval where required.
- Runtime isolation appropriate to the selected backend.
- Safe error reporting.
- Explicit local and remote execution policies.

Prompt-injection defenses must protect enforceable application boundaries.

Prompt formatting alone cannot guarantee that malicious instructions will not influence generated text.

Local-first operation reduces dependence on external providers but does not remove the need for these protections.

### MCP trust boundary

The IntelliJ MCP Server exposes operations that may access or modify IDE state.

The Workbench must not treat MCP tool discovery as blanket permission to execute all discovered operations.

Before every tool call, the application must verify tool identity, argument validity, authorization, and applicable user
approval.

Read-only and consequential operations must be distinguished through programmatic policy.

Unknown operations must not be automatically classified as safe.

Tool results must be handled as untrusted external data.

MCP connection failures, malformed results, and authorization failures must produce explicit, typed outcomes.

The Workbench must not bypass IDE security settings or silently change global MCP exposure configuration.

Detailed responsibilities are defined in [Security Architecture](../security/README.md).

## 9. Structured output

Model-generated text must be validated before it is used as structured application data.

The Workbench must distinguish:

- Free-form text generation.
- Requested structured output.
- Runtime-native constrained generation.
- Post-generation schema validation.
- Structured tool-call proposals.
- Final user-facing responses.

A syntactically valid JSON document is not necessarily valid against the requested schema.

Invalid structured results must produce explicit validation failures.

### Tool-call structure

A tool-call proposal must identify the requested tool and provide arguments matching its declared schema.

Prefer native structured tool calling when the selected model and runtime demonstrably support it.

When native tool calling is unavailable, a validated constrained structured-output mechanism may be used.

Tool requests must never be inferred and executed from unrestricted free-form model text.

A valid structured tool-call proposal is not sufficient authorization for execution.

The Workbench must perform permission and resource validation before dispatch.

### Model continuation

Successful tool execution must produce a validated result that can be supplied to the local model.

The model may then continue the original request using that result.

The final response remains subject to existing structured-output, sanitization, and security policies.

Tool execution failures must have explicit outcomes rather than fabricated tool results or model responses.

## 10. Evaluation and quality control

Evaluation combines deterministic checks with optional model-based assessment.

### Deterministic validation

Deterministic checks assess properties that can be verified directly, including:

- Schema correctness.
- Required output fields.
- Explicit constraints.
- Security-policy compliance.
- Tool-permission compliance.
- Tool-call argument validity.
- Relevant execution status.

Equivalent inputs and rule configurations must produce reproducible deterministic results.

Tool authorization failures must not be overridden by model-generated explanations or evaluation scores.

### LLM-as-a-Judge

A configured local model may evaluate semantic quality using an explicit rubric.

Judge execution must be:

- Bounded.
- Associated with an approved model profile.
- Based on a defined rubric.
- Structured and validated.
- Separate from deterministic security enforcement.
- Recorded with safe evaluation provenance.

Judge assessments are model-generated judgments, not independently established facts.

A failed mandatory deterministic check cannot be overridden by a positive judge score.

Unavailable or malformed judge assessments must have explicit outcomes rather than fabricated scores.

### Evaluation and tool execution

Evaluation must distinguish between:

- A correctly authorized and executed tool call.
- A rejected or unauthorized tool proposal.
- An unavailable or failed tool.
- An incomplete model/tool execution cycle.
- A completed, validated final response.

A successful MCP tool call alone does not establish successful task completion.

When tool use is necessary, completion requires the model to receive the actual tool result and produce an appropriate
validated response.

## 11. Integrated execution workflow

The intended Base Plugin execution path is:

1. **User request**
    - Receive the user prompt and selected workflow.

2. **Agent and model-profile selection**
    - Load the applicable agent, skills, model profile, and policies.

3. **Configuration and permission validation**
    - Check model compatibility, available tools, and applicable authorization rules.

4. **Approved project grounding**
    - Select permitted project context and enforce resource limits.

5. **Input validation and secret filtering**
    - Sanitize request inputs and applicable grounding content.

6. **Prompt composition and generation settings**
    - Preserve instruction roles, output requirements, and permitted tool definitions.

7. **Runtime capability and compatibility checks**
    - Verify the selected model and inference backend support the required execution behavior.

8. **Workbench-managed local inference**
    - Execute the request using the configured local model.

9. **Typed model response**
    - Receive and validate the model output.

10. **Conditional MCP tool execution**
    - Determine whether the model produced a valid structured tool-call proposal.
    - Verify tool exposure, agent permissions, schema conformity, and security policy.
    - Obtain user approval when required.
    - Execute an authorized tool through the configured STDIO MCP client.
    - Validate, sanitize, and limit the returned result.

11. **Bounded model continuation**
    - Return the authorized tool result to the local model.
    - Continue the original request.
    - Repeat the controlled tool cycle only within configured limits.
    - Terminate with a validated response or explicit failure outcome.

12. **Output and structured-data validation**
    - Validate final response structure and applicable output requirements.

13. **Deterministic evaluation**
    - Apply mandatory validation and security checks.

14. **Optional local LLM-as-a-Judge**
    - Evaluate semantic quality where configured and permitted.

15. **Validated result and safe execution provenance**
    - Present the result, relevant tool activity, and applicable evaluation information.

Tool execution is conditional. Requests that do not require tools must remain executable without an MCP tool call.

Each component must preserve the responsibilities established by the architecture.

The final execution path must not depend on a fake inference adapter.

When tool use is required, real MCP execution and actual local-model continuation must be demonstrated before the
integrated workflow is considered verified.

## 12. Verification and implementation evidence

Verification is part of engineering completion, not a separate product feature.

**TASK-005** must establish working inference through a real local model.

**TASK-008** must integrate agent execution, approved grounding, prompt composition, security, and structured output.

**TASK-009** must provide functional interaction through the IntelliJ Tool Window and establish real STDIO MCP tool
execution with local-model continuation.

**TASK-010** must implement deterministic evaluation and local judge integration.

**TASK-011** must verify the integrated Base Plugin using actual local inference, real IDE tool execution, and
applicable judge execution.

### Required evidence

Relevant evidence includes:

- Unit and integration test results.
- Runtime compatibility checks.
- Successful real-model inference.
- Security rejection tests.
- Validated structured responses.
- IntelliJ Development Instance observations.
- Actual MCP connection and tool discovery.
- Real IDE tool execution.
- Validated tool-result propagation to the local model.
- Final model responses informed by actual tool results.
- Operation-specific approval and rejection behavior.
- Actual local judge results.
- Verified failure, cancellation, and recovery behavior.

Mock adapters remain appropriate for deterministic testing but do not replace verification using a real local model.

Similarly, mocked MCP tool responses do not replace a required real IntelliJ IDE tool-call test.

### MCP integration verification

The mandatory integration check must establish:

1. A working MCP client session using the selected STDIO transport.
2. Correct identification of the intended IntelliJ instance and project.
3. Discovery of at least one available read-only IDE tool.
4. A valid structured tool-call proposal generated by the local model.
5. Programmatic validation and authorization of the proposal.
6. Successful execution of the real IDE tool.
7. Receipt and validation of the actual tool result.
8. Continuation of inference using the tool result.
9. A final validated response.
10. Rejection of unauthorized or malformed tool requests.
11. Enforcement of required user approval for consequential operations.
12. Correct cancellation, timeout, and cleanup behavior.

The detailed acceptance criteria remain in TASK-009 and TASK-011.

Only executed checks may be recorded as verified.

## 13. AI-assisted development practices

Development automation and runtime AI functionality are separate concerns.

AI-assisted development may support:

- Architecture and implementation planning.
- Code generation and refactoring.
- Contract and dependency analysis.
- Documentation maintenance.
- Test generation.
- Code review.
- Security review.
- Verification reporting.

Generated output must be checked against existing source code, architectural constraints, and acceptance criteria.

Development-agent instructions must not override repository policies or silently expand task scope.

Use focused implementation tasks and recorded verification results instead of treating generated code as inherently
correct.

### Shared implementation workflow

TASK-001 through TASK-011 use the shared implementation skill:

`.codex/skills/skill-iwb-implement/SKILL.md`

Supported invocation modes are:

- `$skill-iwb-implement TASK-XXX` — execute a selected task.
- `$skill-iwb-implement ALL` — execute or resume dependency-ready tasks according to the shared workflow.

Previously verified implementation work must be preserved.

Task reports belong under `_local/reports/task-reports/` and must not be committed as project source.

Implementation status must reflect actual verification evidence.

## 14. Future architecture options

The following capabilities remain potential extensions rather than requirements of Milestone B.

### Additional inference modes

- Additional Workbench-managed local inference backends.
- Optional remote or cloud providers.
- Explicitly configured hybrid execution.
- Additional accelerator and model-format support.

Any future remote execution mode must require explicit configuration and appropriate data-handling controls.

### Advanced knowledge retrieval

- Embedding-based retrieval.
- Structure-aware document indexing.
- Semantic search and reranking.
- Persistent retrieval stores.

### Extended agent capabilities

- Multi-agent coordination.
- Additional workflow orchestration.
- Additional external MCP server integrations.
- Custom-built specialized tools and tool providers.
- Advanced agent scheduling and automation.

The ability to use authorized IntelliJ IDEA tools through the configured MCP connection is part of Milestone B, not a
future extension.

### Advanced model capabilities

- Fine-tuning.
- Specialized evaluation suites.
- Model optimization and quantization workflows.
- Additional modalities and inference features.

These options must not delay the Base Plugin MVP or become implicit dependencies.

## 15. Scope and maintenance

Milestone B remains focused on a functional, secure, locally managed AI Workbench.

It must not require:

- Cloud APIs.
- Independently operated local AI applications.
- Bundled or automatically downloaded language-model binaries.
- Unnecessary external frameworks.
- Advanced RAG infrastructure.
- Model training infrastructure.
- JetBrains Marketplace publication.
- Duplicate implementations of existing IntelliJ IDE tools.
- Additional external MCP servers for the core IDE tool workflow.

The Base Plugin MVP must provide:

- Workbench-managed local inference.
- Model and runtime configuration.
- Runtime-loaded agents and skills.
- Controlled project grounding.
- Structured prompts and responses.
- Programmatically enforced security and permissions.
- Authorized use of existing IntelliJ IDEA tools through MCP.
- A bounded tool execution and model continuation workflow.
- IntelliJ-native user interaction.
- Deterministic evaluation and applicable local judge integration.

Maintain stable requirement identifiers and alignment with
the [implementation backlog](tasks/README.md), [development roadmap](ROADMAP.md), [architecture](../architecture/README.md),
and [security architecture](../security/README.md).

Update capability status only after the applicable acceptance criteria have been verified.

Future architectural options may be evaluated independently, but they must not be represented as already implemented.