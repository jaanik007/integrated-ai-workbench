# TASK-009: IntelliJ Tool Window Workflow and MCP Tool Execution

**Status:** BLOCKED  
**Dependencies:** TASK-008  
**Milestone:** B — Base Plugin MVP

## Objective

Connect the existing AI Workbench Tool Window to the operational agent execution coordinator and enable
permission-controlled use of IntelliJ IDEA's existing MCP tools by Workbench-managed local models.

Provide an IntelliJ-native interface for selecting local models and agents, preparing bounded requests, executing agent
workflows, approving consequential tool operations, cancelling execution, and inspecting validated results.

The implementation must support an actual end-to-end tool execution cycle:

User request → Local LLM → Tool proposal → Permission validation → IntelliJ MCP tool → Tool result → Local LLM
continuation → Validated response.

Reuse existing IntelliJ capabilities and established Workbench services. Do not build a second implementation of IDE
tools.

The Tool Window must remain responsive throughout model loading, inference, tool execution, and user approval.

## Scope

### Model and agent selection

The Tool Window must allow users to:

- Select a registered model profile.
- Select a loaded runtime agent.
- View relevant model and agent information.
- Identify missing or incompatible configurations.
- Understand why execution is unavailable.
- Review applicable runtime capability restrictions.
- Identify whether MCP tool access is available for the selected agent.
- Understand which tool capabilities are permitted, restricted, or unavailable.

Selection must use existing registry, loader, compatibility, and permission services.

Do not implement duplicate model registration, runtime compatibility, or permission logic in presentation components.

### Request preparation

Provide a practical interface for submitting model requests.

Support:

- User-entered prompt text.
- Explicit selection of approved project context.
- Display of applicable request and context limits.
- Available agent or skill selection where supported.
- Validation feedback before submission.
- Visibility of available and permitted IDE tool capabilities.
- Explicit activation of tool-enabled execution where required by policy.

Do not automatically transmit the entire project as grounding context.

Context selection must be explicit and subject to backend approval.

The UI may display safe source references and omission reasons, but must not reveal excluded or unauthorized file
contents.

Tool availability must not imply permission to access arbitrary project data.

### Execution workflow

Connect the UI to the execution coordinator established in TASK-008.

Support:

- Request submission.
- Visible execution progress or state.
- Cancellation of an active request.
- Successful result presentation.
- Validation failures.
- Runtime errors.
- Timeout and cancellation outcomes.
- Tool discovery and tool-call execution states.
- User approval or rejection of consequential operations.
- Continuation of model inference after a completed tool call.

Use the existing typed execution contracts.

Do not create a second model invocation path inside UI components.

Do not bypass the agent coordinator when executing tools.

### IntelliJ MCP integration

Implement an MCP client integration for accessing existing IntelliJ IDEA tools.

**Selected transport: STDIO.**

The Workbench acts as an MCP client. IntelliJ IDEA provides the existing MCP Server and IDE tools.

The Workbench must use the configured IntelliJ MCP STDIO entry point rather than implementing duplicate IDE tools or
creating a separate MCP server.

Before implementation:

1. Inspect the installed IntelliJ Platform version and available MCP Server capabilities.
2. Verify the configured STDIO transport and connection mechanism.
3. Inspect actual exposed-tool configuration and client access restrictions.
4. Verify whether an MCP client running within the same IDE process can establish a supported connection through the
   configured STDIO entry point.
5. Inspect relevant authentication, authorization, session, and lifecycle requirements.
6. Verify the target IntelliJ instance and project associated with the MCP connection.
7. Review the installed `mcp-builder/SKILL.md` when available and applicable.

The `mcp-builder` skill provides development guidance; it is not itself a runtime MCP implementation.

#### MCP client requirements

The integration must:

- Connect using the configured STDIO transport.
- Launch and manage the STDIO MCP client-side process where required.
- Establish and manage the MCP client session.
- Discover exposed tools and their input schemas.
- Respect server capability and exposure restrictions.
- Detect unavailable or disconnected servers.
- Handle connection failures and reconnection safely.
- Release sessions and associated resources during disposal.
- Avoid conflicts with IntelliJ Platform threading and lifecycle rules.
- Verify that the connection targets the intended IntelliJ project.
- Avoid launching additional full IDE instances unintentionally.

The STDIO entry point may act as a bridge to an existing IntelliJ MCP Server. The implementation must verify the actual
process behavior and connection lifecycle rather than assuming either a separate server or a direct in-process
connection.

Do not assume that tools exposed only to ACP agents are automatically available to the Workbench.

Do not silently alter global IntelliJ MCP exposure settings.

Do not bypass authentication or authorization requirements.

Do not create another MCP server unless the existing server is technically unsuitable and an alternative architecture
has been explicitly justified and approved.

Do not switch to HTTP transport merely for convenience. A transport change requires a verified technical reason and
explicit architectural approval.

### Tool discovery and registry

Reuse IntelliJ MCP tool definitions rather than reimplementing existing IDE operations.

Implement the minimum Workbench-side discovery and dispatch contracts required to:

- Identify tools by stable names.
- Obtain their descriptions and input schemas.
- Filter tools according to Workbench agent permissions.
- Identify read-only and consequential operations.
- Validate arguments before execution.
- Dispatch approved calls through the MCP client.
- Handle typed tool results and failures.
- Maintain safe execution provenance.

Tool classifications and permissions must be enforced by programmatic policy.

Unknown or unclassified operations must not receive automatic execution permission.

Do not present unavailable, prohibited, or unsupported tools as executable capabilities.

Existing `ToolPermissionGuard` functionality must be reused and extended only where necessary.

### Runtime agent and skill permissions

Inspect the existing runtime definitions before enabling MCP execution:

- `.ai/agents/default/AGENT.md`
- `.ai/agents/coding/AGENT.md`
- `.ai/agents/judge/AGENT.md`
- `.ai/skills/default/SKILL.md`
- Other applicable runtime agent and skill definitions.

Verify that runtime instructions and declared permissions are compatible with authorized tool use.

Tool availability depends on all applicable controls:

1. The MCP Server exposes the tool.
2. The tool is enabled in IntelliJ IDEA MCP settings.
3. The selected agent is permitted to use the tool.
4. Workbench security policy permits the operation.
5. Any required operation-specific approval has been granted.

A tool enabled in IntelliJ settings is not automatically authorized for every agent.

Existing blanket prohibitions must be reviewed where they prevent the intended authorized MCP workflow.

Update only the runtime definitions necessary for TASK-009 functionality.

Preserve the separation between development instructions in `.codex/skills/` and Workbench runtime instructions in
`.ai/`.

The Judge Agent must remain isolated from operational IDE tool execution unless a separate, explicitly approved
requirement changes that boundary.

Markdown instructions may restrict behavior but must not replace programmatic authorization.

### Model-driven tool invocation

Extend the existing TASK-008 agent execution pipeline to support actual authorized tool execution.

The local model must be able to:

1. Receive definitions of permitted tools.
2. Produce a structured tool-call proposal.
3. Supply arguments conforming to the relevant schema.
4. Receive the validated tool result.
5. Continue inference using that result.
6. Produce a final response governed by existing output policies.

The Workbench, not the model, decides whether a proposed operation may execute.

#### Protocol compatibility

Inspect the existing llama.cpp runtime and inference adapter before selecting a tool-calling protocol.

Prefer verified native structured tool calling when supported by the chosen runtime and model.

If native tool calling is unavailable, a constrained structured-output mechanism may be used only when its schema and
dispatch boundaries are explicitly validated.

Never execute tool commands inferred from unrestricted free-form model text.

Do not treat model-generated JSON as authorized merely because it parses successfully.

Preserve existing provider-neutral inference contracts wherever possible.

The implementation must not require a cloud model or LM Studio application.

### Tool execution loop

The agent coordinator must support bounded tool execution and model continuation.

Requirements:

- Maximum permitted tool calls per request.
- Maximum model/tool continuation steps.
- Execution deadlines.
- Cancellation propagation.
- Per-tool argument validation.
- Permission validation before every execution.
- Detection of repeated or invalid tool requests.
- Handling of unavailable or disconnected tools.
- Bounded tool-result sizes.
- Typed error outcomes.
- Safe cleanup after completion, cancellation, or failure.

Do not implement unrestricted autonomous execution.

Tool results are untrusted external data and must be processed through applicable input, sanitization, and output
boundaries before reuse.

Tool output must never override system policy, agent permissions, or user approvals.

### Tool permissions and approvals

Apply the existing Workbench security architecture to all MCP operations.

#### Read-only tools

Read-only IDE tools may execute automatically only when:

- The tool is exposed by the connected MCP Server.
- The tool is enabled in IntelliJ IDEA settings.
- The tool is explicitly permitted by the selected agent and Workbench policy.
- The operation remains within approved project and context boundaries.
- Its arguments pass schema and security validation.
- No additional user authorization is required by the IDE or MCP Server.

#### Consequential tools

Operations with potential side effects require explicit, operation-specific user approval.

Examples include:

- Project file modifications.
- Terminal or process execution.
- Git write operations.
- Configuration changes.
- Destructive IDE actions.

The approval request must identify the proposed tool, relevant arguments, affected resources, and expected operation.

Approval of one operation must not authorize unrelated future operations.

The model must never grant itself permissions.

Missing, denied, expired, or cancelled approval must prevent execution and produce a typed outcome.

Permission enforcement must remain in Kotlin backend logic, not solely in UI components or Markdown agent instructions.

Respect additional restrictions imposed by the IDE MCP Server.

An IDE setting that permits execution without confirmation must not override stricter Workbench authorization
requirements.

### Execution state

Represent relevant UI states explicitly, including:

- No valid configuration.
- Ready.
- Incompatible configuration.
- Starting or loading.
- Running.
- Tool discovery.
- Tool execution.
- Awaiting user approval.
- Tool execution denied or failed.
- Cancelling.
- Cancelled.
- Failed.
- Completed with validated output.

Use the minimum practical state set required by established backend contracts.

Execution-state transitions must remain coherent.

Do not report a cancelled, rejected, failed, or unauthorized operation as successful.

Prevent conflicting duplicate submissions and stale tool-call completions.

### IntelliJ threading and lifecycle

Use IntelliJ Platform-supported background execution mechanisms.

Requirements include:

- Do not block the Event Dispatch Thread.
- Keep model, MCP, filesystem, and other long-running operations outside UI rendering.
- Update presentation state through appropriate IntelliJ mechanisms.
- Respect Tool Window and project disposal.
- Propagate cancellation through inference and MCP requests.
- Avoid retained callbacks, sessions, or listeners after disposal.
- Prevent stale asynchronous results from replacing newer state.
- Do not open synchronous approval dialogs that block background execution indefinitely.
- Prevent orphaned Workbench-owned runtime or STDIO processes.

Follow compatibility requirements of the configured IntelliJ Platform target.

### Tool Window presentation

Provide a minimal but functional IntelliJ-native UI.

Display:

- Selected model and runtime agent.
- Prompt input.
- Approved project context selection.
- Tool availability and permission status.
- Active execution state.
- Tool-call activity.
- Tool name and safe argument summary.
- Approval requests for consequential operations.
- Tool success, failure, or rejection.
- Generated response.
- Safe context and tool provenance.
- Validation outcomes.
- Available timing information.
- Actionable error messages.

Do not expose:

- Credentials.
- Unsanitized diagnostic data.
- Hidden model reasoning.
- Unauthorized project contents.
- Sensitive runtime arguments.
- Tool results outside approved disclosure boundaries.

Evaluation results may be incorporated by TASK-010 and TASK-011 without restructuring the basic execution workflow.

### UI scope

Prefer a minimal, operational Tool Window over an elaborate interface.

Reuse IntelliJ-native components and interaction patterns.

Do not duplicate IntelliJ's terminal, editor, Git interface, file navigation, or settings screens.

Do not introduce a separate MCP management application.

Additional settings screens or advanced controls require justification by current functionality.

## Affected Files

Primary scope:

- `base/frontend/`
- Existing Tool Window factory and presentation components.
- View models or presentation controllers.
- Applicable UI resources and tests.

Required backend integration may include:

- Existing `AgentExecutionService` and related execution contracts.
- Existing `ToolPermissionGuard`.
- MCP STDIO client connection and session management.
- Tool discovery and validated dispatch.
- Bounded model/tool continuation.
- Typed execution results and provenance.
- Focused MCP and agent execution tests.

Runtime configuration may include:

- Applicable `.ai/agents/` definitions.
- Applicable `.ai/skills/` definitions.
- Project-local MCP client configuration, if required.

Reuse existing services, interfaces, and state management.

Do not create a parallel agent orchestration system.

Changes to backend code must remain limited to the MCP and UI execution integration required by this task.

## Implementation Boundaries

- Preserve TASK-008's verified agent execution behavior and existing tests.
- Do not place security enforcement exclusively in UI components.
- Do not execute inference or MCP calls on the Event Dispatch Thread.
- Do not implement additional inference engines.
- Do not introduce mandatory cloud providers or external AI applications.
- Do not duplicate IDE tools already exposed by IntelliJ.
- Do not create an unnecessary second MCP server.
- Do not bypass MCP Server configuration or permissions.
- Do not implement unrestricted autonomous tool execution.
- Do not execute tool requests extracted from arbitrary free-form model text.
- Do not silently approve consequential operations.
- Do not store secrets in UI state or diagnostics.
- Do not display hidden reasoning traces.
- Do not implement the evaluation engine assigned to TASK-010.
- Do not implement unrelated IDE features.
- Do not modify remote Git branches.

## Acceptance Criteria

TASK-009 is complete when:

### Tool Window workflow

1. Registered model profiles and loaded agents can be selected.
2. Invalid or incompatible selections are explained.
3. Users can enter a prompt and explicitly select approved project context.
4. Request and grounding limits are respected.
5. Requests execute through the existing TASK-008 coordinator.
6. Execution is non-blocking and the Tool Window remains responsive.
7. Users can cancel active execution.
8. Typed execution states are presented accurately.
9. Successful validated results are displayed.
10. Failure, timeout, cancellation, and policy-rejection states are distinguishable.
11. Safe context provenance and omission information are available where applicable.
12. UI disposal and asynchronous updates are handled safely.
13. Presentation tests cover representative success and failure states.
14. A Development Instance check confirms the integrated Tool Window workflow.
15. Required Gradle checks pass.

### MCP integration and tool execution

16. Workbench establishes a real, supported STDIO MCP client connection to IntelliJ's existing MCP Server.
17. Actual exposed IDE tools and their input schemas are discovered successfully.
18. Tool availability and permissions are filtered through IntelliJ exposure settings and established Workbench security
    policy.
19. The selected local model produces a validated structured proposal for a permitted IDE tool.
20. At least one real read-only IntelliJ MCP tool executes successfully through the Workbench agent pipeline.
21. The actual tool result is returned to the local model, which continues inference and produces a final validated
    response.
22. Invalid tool names, malformed arguments, unauthorized operations, and unavailable tools are rejected without
    execution.
23. Consequential operations cannot execute without explicit, operation-specific user approval; approval, denial, and
    cancellation paths are verified.
24. Tool execution obeys bounded call counts, continuation limits, timeouts, and cancellation.
25. MCP disconnection, runtime failure, and project disposal do not leave uncontrolled requests or orphaned resources.
26. Untrusted tool results cannot bypass grounding, sanitization, prompt, or output security policies.
27. The Tool Window displays tool execution activity and applicable approval interactions.
28. Automated tests and a real IDE MCP integration test confirm the required functionality.

All mandatory criteria must be verified before the task is marked `VERIFIED`.

A fake MCP server or mocked tool response may support deterministic testing but cannot replace the required real
IntelliJ MCP tool-call verification.

A successful IDE tool call alone is insufficient: the local model must receive the result and continue the original
request.

## Verification

### Deterministic tests

Use fake backend and MCP components for focused, deterministic tests.

Verify:

- No registered profile.
- No valid agent.
- Compatible and incompatible configurations.
- Valid request submission.
- Explicit context selection.
- Running and cancellation states.
- Validated response presentation.
- Runtime and policy failures.
- No duplicate or stale result presentation.
- Safe disposal during active work.
- MCP connection and discovery failures.
- Tool schema validation.
- Read-only permission filtering.
- Unauthorized tool rejection.
- Consequential operation approval and denial.
- Invalid or malformed model tool proposals.
- Tool execution timeout and cancellation.
- Maximum tool-call and continuation limits.
- Untrusted or oversized tool results.
- Model continuation after a successful tool result.

Preserve and rerun applicable TASK-008 tests when modifying the agent execution coordinator.

### Real MCP integration

Verify with an actual IntelliJ IDEA instance and its supported MCP Server.

The following STDIO client configuration was prepared specifically for Integrated AI Workbench:

```json
{
  "type": "stdio",
  "env": {
    "IJ_MCP_SERVER_PROJECT_PATH": "C:/Users/Jaanik Virve/plugins/integrated-ai-workbench",
    "IJ_MCP_SERVER_PORT": "64342"
  },
  "command": "C:\\Program Files\\JetBrains\\IntelliJ IDEA 2026.2.1\\bin\\idea64.exe",
  "args": [
    "stdioMcpServer"
  ]
}
```

This is the current workstation-specific reference configuration, not a portable default.

The Workbench implementation must obtain effective connection settings from supported configuration rather than embed
workstation-specific values in Kotlin source code.

Verify that:

- The configured IntelliJ installation exists.
- The selected IDE instance is running and its MCP Server is enabled.
- The configured port belongs to the intended IntelliJ MCP Server.
- The project path identifies the intended opened project.
- The STDIO command establishes a working MCP session.
- The client can discover tools permitted by the current exposure settings.

Do not assume that the normal IntelliJ IDEA instance and the Gradle `runIde` Development Instance share the same MCP
Server, process, port, or project path.

If the Development Instance uses different MCP settings, verify and configure the correct connection for that instance
separately.

Do not silently substitute a different project or MCP server to obtain a successful test.

#### Required procedure

1. Confirm the MCP Server is enabled and exposes a suitable read-only IDE tool to the Workbench client.
2. Verify the effective STDIO configuration and target IntelliJ instance.
3. Establish the actual MCP client session.
4. Discover the tool and its schema.
5. Select a compatible local model and authorized agent.
6. Confirm that the agent's runtime instructions and permissions allow the selected tool.
7. Submit a user request requiring information from the currently opened project.
8. Verify that the model produces a structured tool-call proposal.
9. Verify that Workbench validates and dispatches the call through MCP.
10. Confirm that the real IDE tool returns project information.
11. Confirm that Workbench passes the tool result back to the local model.
12. Verify that the model generates a final response informed by the tool result.
13. Confirm that unauthorized operations remain blocked and consequential operations require approval.
14. Verify that cancellation and disposal release associated resources.

The real integration must use an actual IDE tool and a real local model.

Do not claim successful integration based only on mocked MCP traffic or a hard-coded final answer.

If the configured IntelliJ MCP Server cannot support the required client connection, record the exact technical
limitation, affected acceptance criteria, and proposed supported alternative.

Do not bypass IDE security settings merely to obtain a passing test.

### Gradle and Development Instance

Run:

```powershell
.\gradlew.bat check
```

Then:

```powershell
.\gradlew.bat runIde
```

Verify that the Tool Window opens, supports configuration, performs a controlled model execution, supports cancellation,
displays typed outcomes, and presents actual MCP tool activity.

Run focused integration verification using the existing Workbench-managed llama.cpp runtime and a compatible local
model.

Do not include local executable paths, private model locations, credentials, or generated runtime artifacts in committed
application source code.

Record only actual observations and test results.

The complete user-facing local-model and judge integration remains subject to TASK-011 verification.

## Completion Record

**Implementation:** IN PROGRESS; a fail-closed Workbench-side STDIO client, agent-scoped discovery/dispatch, bounded model continuation, and Tool Window coordinator wiring are present.  
**Automated verification:** PASS — `compileKotlin`, focused agent/MCP/runtime tests (13 tests), and Gradle `check` were run against the existing user Gradle cache.  
**Development Instance verification:** PARTIAL — `runIde` started the isolated IU 2026.1.3 Development Instance and loaded `Integrated AI Workbench`; its Tool Window could not be exercised in this automation session because the spawned isolated UI is unavailable to the active UI controller, and its sandbox has no registered local model profile. A user-visible Development Instance interaction with its own registered profile and configured MCP target remains required.  
**Real MCP accessibility:** VERIFIED separately from Workbench integration: OpenCode established the configured IntelliJ STDIO session, discovered tools, and executed read-only `get_project_modules` for `integrated-ai-workbench`. This independently verifies IntelliJ MCP server accessibility only.  
**Real Workbench MCP integration verification:** PASS — the opt-in `AgentMcpRealIntegrationTest` ran a Workbench-managed local Qwen3/llama.cpp request through the configured STDIO bridge, completed authorized `get_project_modules`, supplied the actual result to continuation inference, and received a validated final response (327.772 seconds).  
**Final status:** BLOCKED

Record:

- Modified source and test files.
- MCP STDIO transport and connection mechanism actually used.
- Effective IntelliJ instance and project identity.
- Discovered IDE tool capabilities.
- Runtime agent permission compatibility.
- Permission and approval behavior.
- Automated test results.
- Real model/tool-call integration results.
- Development Instance observations.
- Remaining limitations.
- Final acceptance status.

Set the task to `VERIFIED` only when all mandatory acceptance criteria are satisfied.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-009`.

The implementation skill must connect the existing Tool Window to the verified agent coordinator and operational
IntelliJ MCP tools using the configured STDIO connection.

Read the installed `mcp-builder` development skill when accessible and relevant, but do not assume that server-building
guidance automatically supplies a suitable MCP client implementation.

Preserve all previously verified functionality, reuse IntelliJ Platform capabilities, enforce tool permissions
programmatically, and avoid blocking UI operations.

Complete real tool execution verification before reporting TASK-009 as `VERIFIED`.
