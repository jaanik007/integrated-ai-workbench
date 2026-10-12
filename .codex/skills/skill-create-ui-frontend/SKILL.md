---
name: skill-create-ui-frontend
description: Build and maintain the native IntelliJ IDEA configuration UI for Integrated AI Workbench, including workspace controls, local model cards, persistent model presets, inference provider settings, and feature toggles. Preserve existing backend contracts and working integrations.
---

# Skill — Create Integrated AI Workbench Frontend

## 1. Objective

Build a usable, professional, IntelliJ-native frontend for Integrated AI Workbench.

**Priority: Establish the complete UI framework before extending application capabilities.**

The plugin already has a minimal Tool Window, but it does not provide a sufficiently usable workspace configuration
interface.

The initial frontend must have one clearly defined responsibility:

**Allow users to discover, configure, enable, disable, and persist the settings of the Workbench's available
capabilities.**

Use the provided LM Studio screenshots as functional and visual references, not as a requirement to reproduce LM
Studio's entire application.

Favor simple, maintainable IntelliJ Platform components and existing services.

## 2. Existing project constraints

Project: `integrated-ai-workbench`

Technology:

- Kotlin 2.3.20.
- Java 21.
- IntelliJ Platform Gradle Plugin 2.19.0.
- IntelliJ IDEA 2026.1.3 development target.
- IntelliJ IDEA 2026.2.1 main installation.
- Native Swing/JB UI components.
- Existing frontend, shared, and backend source roots.

Respect:

- `build.gradle.kts`
- `plugin.xml`
- `AiWorkbenchToolWindowFactory.kt`
- `AiWorkbenchPanel.kt`
- `AiWorkbenchService`
- Existing model/profile contracts.
- Existing agent and skill definitions.
- Existing llama.cpp runtime.
- Existing ToolPermissionGuard and MCP implementation.

Do not introduce Vue, React, Electron, JavaFX, a browser server, or an external UI framework.

Do not recreate backend logic in frontend components.

Do not replace or disable the working llama.cpp adapter.

## 3. Entry points

Expose the Workbench through three discoverable entry points.

### A. IntelliJ Tool Window

`View → Tool Windows → AI Workbench`

Reuse the existing Tool Window ID where possible.

This is the primary compact dashboard.

It must open without requiring a model, runtime, ACP agent, or MCP server to be configured.

### B. IntelliJ Settings

`Settings → Tools → Integrated AI Workbench`

Implement a native Settings Configurable using IntelliJ's supported Settings API and Kotlin UI DSL.

Use standard Apply, OK, Cancel, modified-state, and reset semantics.

Changes must not be persisted merely because a user moves a slider before clicking Apply.

### C. IntelliJ Tools menu

`Tools → Integrated AI Workbench`

Add an IntelliJ Action that opens or activates the existing Tool Window.

Ensure the action can be found through Search Everywhere / Find Action.

Do not create duplicate Workbench windows or duplicate application services.

Optional toolbar or tool-window-header actions may be added only when they improve usability and follow IntelliJ UI
conventions.

## 4. UI architecture

Separate:

1. Presentation components.
2. Presentation state and validation.
3. Persistent settings.
4. Application/backend services.
5. Runtime provider adapters.

The UI consumes capabilities and state exposed by services.

The backend remains authoritative for:

- Model loading and inference.
- MCP tool discovery and execution.
- Security and permissions.
- Runtime lifecycle.
- Agent execution.
- Input/output validation.
- Cancellation.

Do not put these responsibilities into Swing listeners.

### Preferred dashboard sections

- **Overview** — effective configuration and component status.
- **Models** — registered local models and model cards.
- **Inference Providers** — llama.cpp and LM Studio/llmster.
- **Agents & Skills** — availability and enablement.
- **MCP Tools** — connection status and permitted integrations.
- **Workspace** — paths, defaults, and UI preferences.

Use compact navigation, tabs, or expandable sections appropriate to narrow Tool Windows.

Do not create unnecessary screens or duplicate configuration forms.

Preserve the previous execution panel if removing it would eliminate working functionality. It may become a secondary
view; do not redesign chat or execution workflows as part of this skill.

## 5. Model directory and discovery

Provide a configurable default local model directory.

Support:

- Selecting an existing directory.
- Remembering the selected directory.
- Discovering supported model files.
- Refreshing the model list manually.
- Registering an individual GGUF file.
- Avoiding duplicate registration.
- Showing missing or inaccessible files clearly.

Perform filesystem scanning outside the Event Dispatch Thread.

Do not recursively scan unrelated disks.

Do not copy or relocate model files automatically.

Do not download models without explicit user action.

### Model catalog

Display registered models as selectable cards or compact list entries.

Each model receives a stable identity and independent configuration.

Do not hardcode model identities from screenshots.

Support more than one registered model and preserve the selected default model.

## 6. Model cards

Every card has two distinct categories of information.

### A. Static model information

Read-only metadata:

- Model identifier.
- Display name.
- File path or provider-specific model key.
- Model format.
- File size.
- Architecture.
- Parameter count.
- Quantization.
- Publisher, if known.
- Maximum supported context length, if known.
- Tool-use capability, if verifiable.
- Vision capability, if verifiable.
- Reasoning capability, if verifiable.

Prefer authoritative runtime/API metadata.

For LM Studio, inspect `GET /api/v1/models`.

For standalone GGUF files, use existing metadata services or a properly validated reader only where necessary.

Never infer unsupported metadata solely from a filename.

Show **Unknown** when reliable metadata is unavailable.

### B. Editable model configuration

Each model card must support an independent, persistent preset.

Organize editable parameters into expandable groups.

#### General

- Model display name or user-defined alias.
- Default model selection.
- Preferred inference provider.
- Optional system prompt.
- Optional stop strings.

#### Load & Offload

Where supported:

- Context length.
- GPU offload / GPU layers.
- CPU thread count.
- Evaluation batch size.
- Physical batch size.
- Maximum concurrency.
- Flash Attention.
- KV cache GPU offload.
- Keep model loaded.
- Relevant memory and caching options.

#### Inference

Where supported:

- Temperature.
- Maximum output tokens.
- Top-K.
- Top-P.
- Min-P.
- Repeat penalty.
- Presence penalty.
- Random seed.
- Context overflow handling.

#### Reasoning

Where supported:

- Enable/disable thinking.
- Reasoning budget.
- Supported reasoning mode.

#### Structured Output

Where supported:

- Structured-output enablement.
- Expected output format.
- Schema selection or validated schema reference.

#### Advanced

Only include meaningful settings supported by the selected provider.

Examples may include speculative decoding, prompt templates, or provider-specific execution options.

Avoid implementing every LM Studio screenshot option merely to fill the UI.

### Capability-aware behavior

Every parameter must have:

- Unique setting identifier.
- Appropriate data type.
- Valid range or allowed values.
- Default value.
- Provider compatibility.
- Model compatibility where relevant.
- Persistence behavior.
- Application/reload requirement, if applicable.

Unsupported options must be hidden or disabled with an explanation.

Differentiate:

- Configurable settings.
- Runtime-discovered facts.
- Currently effective settings.
- Pending changes.

Never display a saved preference as proof that the runtime applied it successfully.

## 7. Persistent configuration

Use IntelliJ Platform-supported persistent settings services.

Prefer `PersistentStateComponent` or its recommended Kotlin implementations.

Separate:

**Application-level settings**

- Default model directory.
- Provider preferences.
- General UI options.
- Local server endpoint preferences.
- Registered model catalog, where appropriate.

**Project-level settings**

- Project-specific enabled capabilities.
- Default model/agent for the project.
- Project-specific workspace choices.
- Approved context preferences where appropriate.

Use a consistent, versionable settings schema.

Requirements:

- Settings survive IDE restart.
- Model presets survive restart.
- The original model file is never modified by saving a preset.
- Invalid configurations are rejected without destroying the last valid saved state.
- Unsaved edits can be reverted.
- Defaults can be restored.
- Migration preserves existing registered profiles.
- Machine-specific paths are not committed to Git.
- Tokens are stored using IntelliJ PasswordSafe, not ordinary XML settings.

Use a single authoritative persistent configuration service for each settings scope. Do not create competing JSON/XML
configuration sources.

## 8. Inference providers

Support the following separate provider types.

### Provider A — existing managed llama.cpp

Preserve the existing Workbench-managed llama.cpp implementation.

Expose its current configuration and status where supported.

Do not change executable provenance, SHA-256 checks, process ownership, dynamic port selection, or runtime security
policies without a separate justified task.

### Provider B — LM Studio / llmster

Add LM Studio as an optional local inference provider.

Prefer the existing HTTP REST interfaces over embedding a new SDK dependency.

Reference:

- https://lmstudio.ai/docs/cli/daemon/daemon-up
- https://lmstudio.ai/docs/developer/rest
- https://lmstudio.ai/docs/developer/openai-compat

The daemon and API server are separate lifecycle concerns.

Relevant CLI commands:

- `lms daemon status`
- `lms daemon up`
- `lms server status`
- `lms server start`

Default local API endpoint:

`http://127.0.0.1:1234`

Make the endpoint configurable.

Never assume the default port is free or that LM Studio is installed.

A successful daemon startup does not imply that the HTTP server is listening or a model is loaded.

Provide observable states:

- Not installed.
- Not configured.
- Disconnected.
- Connecting.
- Connected.
- Model loading.
- Ready.
- Error.

Distinguish known daemon status from verified API status.

Initially prefer connecting to an already-running provider. Do not take ownership of a user-started daemon or terminate
it during plugin disposal.

If lifecycle control is added, make it explicit and avoid stopping processes not owned by the plugin.

### LM Studio model management

Use the supported LM Studio REST API for:

- Model discovery.
- Model metadata.
- Loaded-instance inspection.
- Supported model load/unload operations.

Do not reimplement LM Studio's model engine.

Use feature detection and validated API schemas.

Support Just-In-Time loading only where available and enabled; do not invent unavailable configuration endpoints.

### Tool use

LM Studio supports OpenAI-compatible tool-call responses at:

`POST /v1/chat/completions`

When using tool definitions, preserve the structured `tool_calls` representation and tool-call identifiers.

Model-produced tool calls are proposals, not permission grants.

All IDE tool execution must continue through the Workbench's existing authorization, argument validation, and execution
pipeline.

Do not bypass ToolPermissionGuard by letting an external runtime execute unrestricted IntelliJ tools.

Do not automatically enable LM Studio remote MCP execution as a shortcut around Workbench policies.

Actual tool-calling integration beyond configuration and capability display is a separate task unless existing adapters
already support it.

## 9. Feature toggles

Expose one consistent method for enabling/disabling available Workbench capabilities.

Possible entries, only when present in the actual project:

- Local inference.
- Managed llama.cpp.
- LM Studio connection.
- Runtime agents.
- Runtime skills.
- Project grounding.
- Permitted read-only IntelliJ MCP tools.
- Structured output.
- Evaluation / local judge.
- Advanced diagnostics.

Distinguish:

**Available** — implementation exists.

**Supported** — selected runtime/model supports the feature.

**Enabled** — user permits the feature.

**Ready** — required configuration and dependencies are valid.

**Active** — feature is currently executing.

A toggle must not claim to disable a feature unless the corresponding backend execution path actually respects that
setting.

Unsupported or unimplemented capabilities must not appear as working switches.

Keep consequential tool operations denied unless the existing policy and real approval mechanism explicitly authorize
them.

## 10. User interaction standards

Use IntelliJ-native components:

- JB panels.
- JB labels.
- JB combo boxes.
- JB checkboxes.
- JB scroll panes.
- Suitable spinners/sliders.
- Kotlin UI DSL for Settings.
- Standard validation and status presentation.

Keep the appearance consistent with IntelliJ light and dark themes.

Support narrow Tool Windows without horizontal overflow.

Avoid unnecessarily tall forms.

Use tooltips for technical settings rather than lengthy visible paragraphs.

Use sensible grouping and disclosure controls.

Always show:

- Current selection.
- Pending changes.
- Save/apply result.
- Validation errors.
- Connection failure where relevant.
- Whether a setting requires model reload or provider restart.

Never block the EDT with:

- File scanning.
- HTTP requests.
- Model loading.
- Process startup.
- Metadata parsing.
- Model inference.

Dispose listeners and pending UI tasks correctly.

Prevent stale background callbacks from updating closed or outdated views.

## 11. File responsibilities

Inspect existing source files before creating new ones.

Prefer the following responsibilities and create files only where justified.

| File or component                  | Purpose                                           |
|------------------------------------|---------------------------------------------------|
| `AiWorkbenchToolWindowFactory.kt`  | Registers and creates the Tool Window content     |
| `AiWorkbenchPanel.kt`              | Main configuration dashboard and navigation       |
| `WorkbenchSettingsConfigurable.kt` | IntelliJ Settings entry and Apply/Reset lifecycle |
| `WorkbenchSettingsService.kt`      | Persistent configuration and preferences          |
| `ModelCatalogPanel.kt`             | Local model list, discovery, and selection        |
| `ModelCardPanel.kt`                | Metadata and editable settings for one model      |
| `ModelPresetService.kt`            | Validates and persists per-model presets          |
| `InferenceProviderPanel.kt`        | Provider configuration and connection status      |
| `WorkbenchFeaturePanel.kt`         | Capability-aware feature toggles                  |
| `OpenWorkbenchAction.kt`           | Opens Workbench through the Tools menu            |
| `plugin.xml`                       | Registers Tool Window, Settings, and menu action  |

Use the existing `base/frontend`, `base/shared`, and `base/backend` source roots.

New services and shared contracts must be placed in their appropriate layers, not automatically in frontend.

Avoid creating several files containing identical settings or duplicated state.

Where an existing service already provides the required responsibility, extend it minimally instead of creating a second
implementation.

## 12. Execution plan

Implement incrementally.

### Phase 1 — Usable UI framework (mandatory first)

1. Inspect the current plugin registration and source structure.
2. Register the Settings entry.
3. Register the Tools menu action.
4. Preserve the existing Tool Window.
5. Replace the oversized prompt-first layout with a compact configuration dashboard.
6. Add persistent general workspace settings.
7. Add real capability toggles for implemented features.
8. Verify saving, reopening, and restoration after IDE restart.

The UI must be usable at the end of this phase, even without configured inference.

### Phase 2 — Model catalog and cards

1. Configure the default model directory.
2. Discover and register models.
3. Display model cards with real metadata.
4. Implement per-model presets.
5. Group settings by purpose.
6. Apply type/range/capability validation.
7. Implement Save, Reset, and pending-change behavior.
8. Confirm independent presets for multiple models.

### Phase 3 — Provider integration

1. Preserve the existing llama.cpp provider.
2. Detect an already-running LM Studio API server.
3. Fetch actual model metadata from `/api/v1/models`.
4. Show provider connection and loaded model status.
5. Implement an optional LM Studio adapter using supported API operations.
6. Handle authentication securely if required.
7. Keep provider selection and model presets persistent.
8. Verify no conflicts with existing MCP and agent execution.

Do not automatically install LM Studio or download new models.

### Phase 4 — Verification and cleanup

Verify:

- Plugin installs and loads.
- Tool Window opens.
- Settings entry opens.
- Tools menu action opens the same Workbench.
- Settings persist across IDE restart.
- Settings Apply/Cancel/Reset work.
- Two model presets remain independent.
- Unsupported settings are unavailable.
- UI stays responsive during background operations.
- Both IntelliJ light and dark themes work.
- Existing llama.cpp execution remains functional.
- Existing MCP/security tests continue passing.

Run:

`.\gradlew.bat compileKotlin check verifyPluginStructure buildPlugin`

If build instrumentation fails because of local JDK configuration, report that environment issue separately rather than
changing application code to conceal it.

## 13. Non-goals

Do not implement:

- A new chat application.
- Autonomous multi-agent orchestration.
- A new MCP security policy.
- Cloud-provider authentication.
- Model training or fine-tuning.
- A custom GGUF inference engine.
- A duplicate LM Studio implementation.
- Experimental settings with no backend support.
- Unrelated architecture refactoring.

This skill creates the configuration/control frontend. Advanced agent execution UX can be developed in a subsequent
dedicated skill.

## 14. Acceptance criteria

The task is complete when:

1. Workbench can be opened from Settings, Tool Windows, and Tools menu.
2. It is usable before any model is configured.
3. Users can enable/disable implemented capabilities.
4. Settings are validated and persist.
5. The local model directory is configurable.
6. Registered models have separate cards.
7. Model cards display reliable static metadata.
8. Supported editable settings have persistent presets.
9. Unsupported model/provider settings are not presented as functional.
10. Existing backend integrations remain intact.
11. All applicable automated tests pass.
12. Manual IDE interaction is verified or explicitly documented as unverified.

A visually attractive screen without working persistence does not satisfy acceptance.

## 15. Safety and reporting

Preserve existing user configuration, model files, runtime executables, and Git state.

Do not overwrite external LM Studio settings.

Do not alter global IntelliJ configuration unnecessarily.

Do not commit, push, merge, or publish.

Avoid parallel edits to files currently being modified by another Codex task.

Before implementation, identify any conflicts with ongoing work.

For every created or modified file, report its path and one-sentence purpose.

Final report:

- UI entry points added.
- Components created or modified.
- Settings persistence implementation.
- Model catalog capabilities.
- Supported provider integrations.
- Tests and manual verification.
- Remaining backend limitations.
- Explicitly deferred features.

**Deliver a working UI framework first, then expand capabilities without redesigning the framework again.**