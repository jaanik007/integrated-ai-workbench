---
name: skill-readme-settings-and-structure
description: Audits and configures IntelliJ IDEA for the Integrated AI Workbench project, resolves Gradle dependencies, and creates the planned IntelliJ Platform plugin structure and compilable skeleton files using the repository README.md hierarchy as authoritative project context.
---

# Integrated AI Workbench — Project Bootstrap

## 1. Role and objective

You are Codex, the implementation agent responsible for preparing the Integrated AI Workbench development environment
and bootstrapping its IntelliJ Platform plugin project.

**Repository:** `jaanik007/integrated-ai-workbench`  
**Plugin name:** Integrated AI Workbench  
**Plugin ID:** `integrated-ai-workbench`  
**Current version:** `0.1.0-SNAPSHOT`

Your task is to:

1. Read and understand the actual project requirements.
2. Audit and configure applicable IntelliJ IDEA Settings.
3. Install and synchronize required project dependencies.
4. Create the planned project structure and compilable skeleton files.
5. Validate the resulting development environment and project bootstrap.
6. If sufficient execution capacity remains, create a compact report at
   `_local/reports/settings-and-structure-report.md`.

**Execute the work. Do not merely generate an implementation plan.**

The scope is project bootstrap, not completion of the entire AI Workbench product.

## 2. Authoritative project context

Before changing anything, inspect the actual local repository.

Read in this order:

1. Root `AGENTS.md` — repository-wide agent instructions.
2. Root `README.md` — primary project specification.
3. Relevant nested `README.md` files — subsystem-specific instructions.
4. Existing `.codex/skills/` definitions, where relevant.
5. Existing source files, Gradle configuration, `.gitignore`, `.idea` configuration and project structure.

Discover nested README files dynamically. Do not assume they already exist.

Apply the hierarchy:

**Direct user instructions → AGENTS.md → root README.md → applicable subsystem README.md → existing implementation
conventions.**

A specific README may refine a broader rule without contradicting higher-priority instructions.

Treat repository documentation as the intended design, but verify the filesystem and development environment before
assuming any implementation already exists.

Do not overwrite the existing root README or AGENTS.md merely to recreate them.

When newly created subsystems require architectural explanations, create short subsystem README files.

## 3. Product and architecture boundaries

Use the current README as the primary specification.

The Integrated AI Workbench Base Plugin must be:

- IntelliJ-native;
- local-first and capable of offline operation;
- independent of any particular language model;
- functional without mandatory cloud services;
- independent of other AI plugins;
- extensible through optional capability plugins;
- secure by design;
- compatible with IntelliJ Platform architectural requirements.

**Local inference is a native Base Plugin capability. Remote inference is optional.**

The plugin integrates user-supplied language models, rather than distributing model binaries.

Distinguish these responsibilities:

- Model — user-selected inference resource.
- Runtime — software executing the model.
- Model Profile — model configuration and inference parameters.
- Agent — configured worker combining a model, instructions, tools and policies.
- Skill — repeatable task-specific workflow.
- Advisor/Orchestrator — optional coordination of multiple agents.
- Grounding — controlled selection of relevant project context.
- Evaluation — deterministic validation and optional model-based assessment.

Do not duplicate existing IntelliJ features such as the code editor, terminal, Git client, debugger, Gradle integration,
test runner, project explorer or run configurations.

Prefer IntelliJ Platform APIs and narrowly scoped integrations.

### Base Plugin scope

The planned Base Plugin architecture includes:

- AI Workbench Tool Window;
- Model Registry and Model Profiles;
- local inference Runtime Manager;
- Agent Registry and agent-to-model assignment;
- Markdown AGENT/SKILL/Prompt loading;
- Context/Grounding Service;
- Prompt Composer;
- model generation parameters;
- input and output security guards;
- structured output contracts and validation;
- controlled tool permissions;
- deterministic evaluation;
- LLM-as-a-Judge abstraction;
- optional Advisor/Orchestrator;
- reproducible run metadata.

### Deferred capabilities

Treat the following as separate future capability extensions unless the README explicitly changes their priority:

- cloud provider implementations;
- MCP ecosystem integrations;
- advanced RAG;
- dataset management;
- SFT, LoRA and QLoRA;
- training orchestration;
- supervisor-guided agentic training;
- distributed or from-scratch training.

Do not install dependencies or create extensive implementations for deferred capabilities during this bootstrap.

Document extension points instead.

## 4. Execution policy

Work autonomously within the permissions actually granted for this execution.

The user intends to authorize relevant IntelliJ IDEA configuration changes.

Use that authorization only for confirmed, necessary and proportionate changes.

Before modifying any environment or project configuration:

1. Read its current value.
2. Determine the project requirement.
3. Determine whether the current value already satisfies the requirement.
4. Identify the smallest safe modification.
5. Apply it using an available supported mechanism.
6. Verify the resulting state.

Do not change settings that already satisfy the requirements.

Do not globally disable unrelated IDE capabilities.

Do not assume access to IDE menus, operating-system settings, external tools, APIs or configuration files merely because
this skill requests such access.

If a required operation is inaccessible, record the limitation and continue with independent tasks.

Preserve unrelated user configurations and work.

## 5. Phase 1 — Development environment and IntelliJ Settings

**Priority: CRITICAL**

### 5.1 Inspect the development environment

Discover actual versions and availability of:

- IntelliJ IDEA and IntelliJ Platform;
- Java Development Kit;
- Gradle and Gradle Wrapper;
- Kotlin compiler/toolchain;
- IntelliJ Platform Gradle Plugin;
- Git;
- operating system and architecture;
- relevant IntelliJ Platform SDK components.

Use JDK 21 as the planned project baseline unless current IntelliJ Platform compatibility requirements demand an
explicitly justified adjustment.

Do not select Gradle, Kotlin, IntelliJ Platform SDK or IntelliJ Platform Gradle Plugin versions arbitrarily.

Resolve compatible versions using installed tooling, existing repository configuration and official compatibility
documentation when accessible.

Prefer stable compatible versions over experimental releases.

### 5.2 Audit IntelliJ IDEA Settings

Inspect only configuration categories that materially affect this project.

Relevant categories may include:

**Build, Execution, Deployment**

- Gradle JVM and toolchain compatibility.
- Gradle project import and synchronization.
- Gradle build/test delegation.
- Gradle Kotlin DSL recognition.
- JVM and compiler configuration.

**Languages & Frameworks**

- Kotlin support.
- Java SDK and language-level compatibility.
- Applicable IntelliJ Platform development integration.

**Editor**

- Kotlin formatting and code style.
- File encodings, especially UTF-8.
- Relevant inspections and diagnostics.
- Project-specific formatting consistency.

**Project Structure**

- Project SDK.
- Modules and content roots.
- Source and resource directories.
- Excluded generated directories.
- Dependencies and library resolution.

**Tools and integrations**

- Available IDE automation interfaces.
- Existing Codex/IDE integration.
- Relevant MCP capabilities, if present.
- Required Gradle and build-tool integration.

**Version Control**

- Confirm the correct Git repository root.
- Check for settings that may interfere with project operations.
- Preserve existing remote configuration.

**Plugins**

- Assume required IDE plugins are already installed.
- Verify required capabilities where access exists.
- Inspect relevant configuration and conflicts.
- Do not reinstall, replace or add plugins unless an actual missing capability makes it necessary and the user
  authorizes the installation.

### 5.3 Apply applicable changes

Prefer this order:

1. Official IntelliJ IDE Settings or available IDE automation API.
2. Supported project-level settings and Gradle configuration.
3. Documented IntelliJ configuration files that can be modified safely.

Distinguish:

- global IDE settings;
- current-project settings;
- Gradle build configuration;
- user-local runtime configuration.

Prefer project-level settings when they can meet the requirement without affecting other projects.

Do not edit global IDE configuration merely to impose a project-specific preference.

Before editing XML or other IDE configuration directly, verify the schema, intended ownership and compatibility with the
active IDE version.

Do not modify opaque IDE state or internal configuration databases.

For any setting that requires user interface access, use the actual authorized GUI automation capability if available.

**Editing a configuration file is not equivalent to changing a GUI setting unless the IDE recognizes and applies that
change.**

If GUI access or an appropriate API is unavailable, do not claim the setting was applied.

Record the exact setting, recommended value and limitation.

### 5.4 Settings completion criteria

This phase is complete when:

- relevant current settings have been inspected where accessible;
- necessary authorized modifications have been applied;
- modifications have been verified where possible;
- inaccessible or deferred settings have been identified;
- unrelated IDE configuration remains unchanged.

Continue to project bootstrap even when a nonblocking IDE setting cannot be changed.

## 6. Phase 2 — Dependencies and Gradle initialization

**Priority: CRITICAL**

Use the project requirements in README.md to determine the required dependency set.

### 6.1 Gradle project

Inspect the existing Gradle setup.

If missing, initialize a valid IntelliJ Platform plugin project with:

- `settings.gradle.kts`;
- `build.gradle.kts` or an appropriate root/module build arrangement;
- `gradle.properties`;
- Gradle Wrapper scripts and wrapper properties;
- the required IntelliJ Platform Gradle Plugin;
- Kotlin support;
- JDK toolchain configuration;
- IntelliJ Platform plugin descriptor integration.

The base implementation should initially use the smallest practical Gradle module structure.

The README's `frontend`, `shared` and `backend` boundaries may be represented by source packages instead of separate
Gradle modules where that avoids premature complexity.

Choose a structure compatible with future IntelliJ Remote Development / Split Mode requirements, without claiming Split
Mode support has already been implemented.

### 6.2 Dependency selection

Install only dependencies required for the planned Base Plugin skeleton and immediately necessary interfaces.

Potential dependency categories:

- IntelliJ Platform APIs;
- Kotlin standard runtime support;
- IntelliJ Platform testing support;
- JSON serialization/validation where necessary;
- HTTP client facilities where immediately required;
- compatible testing frameworks.

Prefer platform-provided APIs and libraries where appropriate.

Do not introduce unnecessary framework dependencies.

Do not add:

- mandatory cloud AI SDKs;
- model binary downloads;
- PyTorch or Transformers;
- training framework dependencies;
- separate AI plugins;
- external databases;
- unrelated web application frameworks.

No language model is a build dependency.

### 6.3 Resolve dependencies

After creating or correcting Gradle configuration:

1. Generate or restore the Gradle Wrapper.
2. Resolve compatible toolchains.
3. Refresh the Gradle project.
4. Download required dependencies using Gradle.
5. Verify successful dependency resolution.
6. Check for unsupported or conflicting dependencies.

Avoid mixing incompatible Kotlin and IntelliJ Platform dependency versions.

Do not hard-code machine-specific SDK paths into version-controlled configuration.

Use supported local configuration mechanisms where necessary.

## 7. Phase 3 — Planned project structure

**Priority: CRITICAL**

Create the project directories from the architecture specified in README.md.

Use the following as a responsibility map, adapting source layout to the actual Gradle and IntelliJ Platform
conventions.

### 7.1 Repository-level structure

- `base/` — Base Plugin implementation.
- `.ai/` — runtime AI instructions, profiles, prompts and policies.
- `.codex/skills/` — Codex development workflows.
- `docs/architecture/` — architecture and component relationships.
- `docs/security/` — security architecture.
- `docs/development/` — development notes and bootstrap conventions.
- `_local/reports/` — machine-local generated reports.
- `.aim/` — machine-local AI Workbench runtime state, where required.

Preserve `.gitignore` rules.

Do not commit machine-local state, model binaries, credentials, reports or generated build artifacts.

### 7.2 Base Plugin implementation boundaries

Establish the equivalent of:

**Frontend**

- Tool Window factory.
- Workbench panel.
- UI state and view models.
- IDE action integration.

**Shared**

- typed requests and responses;
- Model Profile;
- Agent Definition;
- configuration contracts;
- structured output contracts;
- shared result and evaluation models.

**Backend**

- Model Registry;
- Runtime Manager interface;
- Model Provider abstraction;
- Agent Registry;
- Context Service;
- Prompt Loader;
- Prompt Composer;
- input and output guards;
- Secret Sanitizer;
- Tool Permission Guard;
- Evaluation Service;
- Judge Service;
- optional orchestration abstraction;
- run metadata contracts.

Use cohesive Kotlin packages and conventional `src/main/kotlin` and `src/main/resources` source roots.

The existing architecture determines the actual package names and module structure.

Avoid invalid Kotlin package identifiers and excessive nested directories.

### 7.3 Runtime AI instruction files

Create a minimal, consistent structure under `.ai/`:

- `agents/default/AGENT.md`
- `skills/default/SKILL.md`
- `prompts/`
- `profiles/`
- `policies/`

The default agent and skill should define a safe workflow verification and must not imply capabilities that have not
been implemented.

Keep runtime agent definitions separate from Codex development instructions.

Use the README conventions for additional files.

### 7.4 Local models

Support the README's concept of model files that may reside:

- inside a project;
- in an existing local directory;
- in a configured external model directory;
- through a supported link or reference.

Do not download or relocate model binaries during bootstrap.

Do not assume a specific installed model.

Actual model registration and inference functionality belongs to subsequent implementation work.

## 8. Phase 4 — Skeleton files

**Priority: CRITICAL**

Create valid source files at the appropriate architectural boundaries.

A skeleton file must either:

- provide a minimal functioning implementation;
- define a valid interface or data type; or
- explicitly represent unfinished functionality without pretending it works.

Do not create empty classes merely to increase the file count.

### 8.1 Essential initial files

Establish equivalent Kotlin source files for:

**Plugin UI**

- `AiWorkbenchToolWindowFactory.kt`
- `AiWorkbenchPanel.kt`

**Model contracts**

- `ModelProvider.kt`
- `ModelProfile.kt`
- `ModelRequest.kt`
- `ModelResponse.kt`

**Core orchestration**

- `AiWorkbenchService.kt`
- `AgentDefinition.kt`
- `AgentRegistry.kt`

**Runtime**

- `LocalRuntimeManager.kt` or an equivalent interface.
- `RuntimeCapabilities.kt` or equivalent typed contract.

**Grounding and prompting**

- `ContextService.kt`
- `GroundingContext.kt`
- `PromptLoader.kt`
- `PromptComposer.kt`

**Security**

- `InputGuard.kt`
- `SecretSanitizer.kt`
- `OutputGuard.kt`
- `ToolPermissionGuard.kt`

**Structured output**

- `StructuredResponseValidator.kt`

**Evaluation**

- `EvaluationService.kt`
- `JudgeService.kt`

Only create additional classes when required by compile-time dependencies or a real architectural need.

Place all files in the correct source roots and Kotlin packages.

### 8.2 Plugin descriptor

Create:

`META-INF/plugin.xml`

Ensure:

- a valid plugin ID and name;
- appropriate plugin metadata;
- compatible IntelliJ Platform declarations;
- correct Kotlin implementation class references;
- a valid Tool Window registration;
- no non-existent required classes or dependencies.

Use the current IntelliJ Platform conventions rather than obsolete examples.

### 8.3 Minimal UI

The initial Tool Window must register successfully.

It may display a minimal informative development state.

Do not create nonfunctional buttons or fake controls that suggest models, tools or evaluation are operational.

Real model execution is not required in this bootstrap task.

### 8.4 Implementation principles

Use:

- small focused types;
- explicit responsibility boundaries;
- dependency inversion where justified;
- project-level services where appropriate;
- IntelliJ Platform lifecycle conventions;
- correct UI thread handling;
- asynchronous/background execution for blocking work;
- immutable structured contracts where practical.

Avoid:

- giant Tool Window classes;
- giant services;
- provider-specific logic in generic UI;
- hard-coded local filesystem paths;
- fake model responses represented as real inference;
- security implemented solely through Markdown prompts;
- duplicate implementations of existing IDE capabilities.

### 8.5 Security foundation

Define safe initial boundaries for:

- maximum input size;
- maximum grounding context size;
- allowed project paths;
- binary-file exclusion;
- secret filtering;
- model request timeouts;
- structured output validation;
- tool permission checking;
- local/remote data policy.

Do not claim complete security enforcement unless the relevant controls actually execute.

Use IntelliJ PasswordSafe for persistent secret storage when applicable.

Never place credentials in README, AGENT, SKILL, plugin.xml or Git-tracked configuration.

## 9. Phase 5 — Documentation

**Priority: HIGH**

Keep the existing root README.md as the architectural reference.

Create or update only documentation required to explain newly established project areas.

Recommended documentation:

- `docs/architecture/README.md`
- `docs/security/README.md`
- `docs/development/README.md`
- `docs/development/ROADMAP.md`
- `docs/development/AI-REQUIREMENTS.md`

The architecture documentation should describe the actual frontend/shared/backend responsibility boundaries and
dependencies.

The security documentation should distinguish implemented safeguards from planned mechanisms.

The AI requirements document should map the project requirements to planned code locations:

- grounding;
- system and user prompt roles;
- reasoning configuration;
- security and sanitation;
- few-shot prompting;
- structured output;
- model generation parameters;
- sampling controls;
- LLM-as-a-Judge;
- runtime-loaded Markdown prompts.

Use accurate status indicators:

- `IMPLEMENTED`
- `PARTIAL`
- `PLANNED`
- `EXPERIMENTAL`

Do not repeat the entire root README in every subsystem.

Do not describe future training or cloud integrations as implemented Base Plugin functionality.

## 10. Phase 6 — Validation

**Priority: HIGH**

Validate the bootstrap with the available project tooling.

Check:

1. Gradle project imports correctly.
2. Required dependencies resolve.
3. Kotlin and Java toolchains are compatible.
4. Source roots are recognized.
5. `plugin.xml` references existing classes.
6. Project compiles.
7. Gradle build executes.
8. Existing tests execute when present.
9. `runIde` can be launched when supported.
10. The development plugin loads into an IntelliJ sandbox when verifiable.
11. No credentials or model binaries have been introduced into tracked files.
12. Existing user changes remain intact.

Run appropriate commands such as:

- Windows: `.\gradlew.bat build`
- Windows: `.\gradlew.bat test`
- Windows: `.\gradlew.bat runIde`
- Other systems: corresponding `./gradlew` commands.

Do not block indefinitely on an interactive development IDE process.

Do not claim the Tool Window is visually verified if only compilation succeeded.

When a validation step fails:

1. Inspect the actual error.
2. Identify its most specific supported cause.
3. Correct the smallest relevant issue.
4. Repeat the affected check.

Do not perform broad speculative dependency upgrades or architecture changes.

## 11. Phase 7 — Optional compact report

**Priority: LOW**

Only after the mandatory phases have been completed or cannot be advanced further, and sufficient execution capacity
remains, generate:

`_local/reports/settings-and-structure-report.md`

Create the parent directories if necessary.

Replace the previous report at this exact path, if one exists.

The report should be concise and factual.

Suggested structure:

### Environment

- IntelliJ IDEA version.
- JDK version.
- Gradle version.
- Kotlin version.
- IntelliJ Platform Gradle Plugin version.

### Settings

List only:

- settings changed, with previous and new values;
- necessary settings that could not be changed;
- relevant unresolved conflicts.

Do not list unchanged settings.

### Dependencies

- installed or resolved dependencies;
- missing or conflicting dependencies;
- synchronization status.

### Project structure

- created files and directories;
- existing structures preserved;
- deviations from the intended architecture.

### Validation

- commands actually executed;
- PASS/FAIL/BLOCKED results;
- meaningful errors.

### Remaining work

- unresolved bootstrap issues;
- next implementation milestone.

Never invent command results, IDE settings or successful operations.

If the report cannot be completed, prioritize valid source code and project structure rather than spending the remaining
execution budget on documentation.

## 12. Git and filesystem safety

Do not:

- force-push;
- modify remote `master`;
- change Git remotes;
- merge branches;
- reset existing work;
- delete branches;
- discard unrelated changes;
- commit secrets;
- upload project data to cloud services;
- automatically publish plugin releases;
- change licensing terms without authorization.

Use read-only Git inspection when useful.

Preserve all existing repository content unless modification is necessary for this task.

Follow the existing `.gitignore`, including project-local reports, IDE state, build artifacts and model binaries.

Do not commit or push changes automatically unless separately authorized.

## 13. Execution priorities

Apply this priority order when time, token budget or available tools constrain execution.

**P0 — Required**

- Read AGENTS.md and README.md hierarchy.
- Inspect actual repository state.
- Audit relevant IntelliJ Settings.
- Apply accessible, necessary, authorized settings.
- Initialize Gradle and resolve project dependencies.
- Create planned directories and Kotlin skeleton files.

**P1 — Required where feasible**

- Validate compilation.
- Validate plugin descriptor.
- Run Gradle build.
- Check development IDE integration.
- Correct confirmed bootstrap errors.

**P2 — Supporting**

- Create concise subsystem documentation.
- Synchronize README statements with actual implementation status.

**P3 — Optional**

- Generate `_local/reports/settings-and-structure-report.md`.

Never sacrifice a necessary P0 action to produce a more elaborate report.

## 14. Definition of done

The initial bootstrap is successful when:

- repository README instructions have been applied;
- relevant IntelliJ configuration has been audited;
- applicable authorized IDE settings have been changed where accessible;
- Gradle Wrapper and compatible project configuration exist;
- required dependencies have been resolved;
- the project has a valid IntelliJ Platform plugin descriptor;
- planned source packages and directories exist;
- necessary Kotlin skeletons compile;
- Base Plugin concerns have clear architectural boundaries;
- runtime AGENT/SKILL foundations exist;
- machine-local state and secrets remain excluded from version control;
- build results are reported accurately;
- unfinished product capabilities are clearly distinguished from working implementations.

The **completed Base Plugin MVP is a separate milestone**.

Do not implement advanced AI functionality merely to satisfy the bootstrap task.

## 15. Final response

Provide a compact Estonian completion summary identifying:

- IntelliJ Settings actually changed;
- settings that remained inaccessible or blocked;
- dependency resolution result;
- created project structure;
- Gradle build result;
- outstanding blockers;
- location of the optional report, if created.

Do not provide a long theoretical explanation or repeat the project specification.

**Execution principle:** Understand the repository first, configure only what is necessary, establish a valid
development environment, create the smallest architecturally correct project skeleton, and report only verified results.
