---
name: skill-integrated-ai-workbench
description: Generates and maintains README-aligned runtime AGENT.md and SKILL.md instructions, a dependency-ordered implementation backlog, and executable Codex task skills for the Integrated AI Workbench Base Plugin. Prioritizes native managed local LLM inference, complete MVP workflows, security, and demonstrable AI course requirements.
---

# Integrated AI Workbench — Agent & Implementation Planner

## 1. Role and objective

You are Codex, acting as the AI architecture and implementation-planning agent for the Integrated AI Workbench project.

Repository: `jaanik007/integrated-ai-workbench`

Your responsibility is to transform the documented product architecture and existing project skeleton into:

1. Complete, meaningful runtime `AGENT.md` files.
2. Complete, executable runtime `SKILL.md` files.
3. A dependency-ordered implementation backlog.
4. One task-specific Codex implementation skill for each appropriate development task.
5. Explicit acceptance criteria connecting implementation to the project's AI course requirements.

This skill prepares the project for incremental coding.

**Do not implement the complete application during this planning task.**

Generate the actual instruction and planning files. Do not merely describe what should be created.

## 2. Project authority and discovery

Before generating files:

1. Read root `AGENTS.md`.
2. Read root `README.md`.
3. Discover and read relevant subsystem `README.md` files.
4. Inspect the actual Gradle project and directory tree.
5. Inspect existing Kotlin skeletons, resources and plugin descriptor.
6. Inspect existing `.ai/` and `.codex/skills/` structures.
7. Inspect existing development documentation and implementation plans.
8. Read the most recent `_local/reviews/ux-review.md`, if present.

The project README hierarchy defines product intent, architecture and subsystem responsibilities.

Actual code and configuration establish the implementation state.

Do not invent missing capabilities or assume that skeleton files contain working functionality.

Do not change the root README merely to fit a preferred architecture.

If documentation and code disagree, record the discrepancy and plan the smallest appropriate correction.

## 3. Existing skills and responsibility boundaries

Recognize the following development skills:

### skill-readme-settings-and-structure

Responsible for:

- IntelliJ IDEA configuration;
- development environment;
- Gradle and dependencies;
- project directories;
- initial Kotlin skeletons;
- project bootstrap validation.

Do not duplicate these responsibilities.

### skill-ux-review

Responsible for:

- independent implementation review;
- README conformance;
- dependency and structure coherence;
- IntelliJ UX;
- security requirements;
- course-concept coverage;
- prioritized remediation findings.

Use its existing report as optional planning input.

Do not overwrite this skill or its reports.

### skill-integrated-ai-workbench

This skill is responsible for:

- generating runtime agent definitions;
- generating runtime workflow skills;
- mapping README requirements to actual implementation tasks;
- sequencing tasks according to dependencies;
- generating implementation-ready Codex skills;
- defining acceptance and verification criteria.

Do not recreate the two existing skills under new names.

Keep this planning skill separate from implementation and review activities.

## 4. Architectural principles

Preserve the architecture defined in README.md.

The project is an IntelliJ Platform plugin with:

- Kotlin;
- Gradle;
- JDK 21 baseline;
- IntelliJ-native UI;
- local-first model execution;
- user-supplied language models;
- agent, skill and model-profile abstractions;
- controlled grounding;
- prompt composition;
- input and output security;
- structured model responses;
- deterministic evaluation;
- LLM-as-a-Judge;
- optional multi-agent orchestration.

The Base Plugin must work without mandatory cloud services, another AI plugin or another independently operated AI
application.

Cloud providers, advanced RAG, MCP extensions and model training are future extension capabilities.

Do not generate current MVP implementation tasks for deferred features unless explicitly requested.

### Architecture boundaries

Respect the README's conceptual boundaries:

- `base/frontend/` — IntelliJ UI and user interaction.
- `base/shared/` — shared models, contracts and typed data.
- `base/backend/` — project services, model/runtime management, security and evaluation.

These may be implemented as packages rather than separate physical Gradle modules.

Avoid unnecessary module proliferation.

Use the actual repository layout when determining precise paths.

## 5. Native local LLM inference — critical requirement

The Base Plugin must provide a genuinely integrated local inference capability.

This means a developer can select an available local model and execute inference through Integrated AI Workbench without
depending on LM Studio, Ollama or another separately operated AI application.

A local inference engine may be implemented through an appropriate native library or a plugin-managed runtime process.

Do not confuse native Workbench integration with a requirement to implement transformer inference mathematics from
scratch.

### Required architectural separation

Maintain distinct responsibilities for:

**Model Registry**

- discovers or registers user-supplied models;
- stores model metadata and locations;
- supports project-local and external model paths;
- does not store model binaries in Git.

**Hardware Capability Detection**

- discovers available CPU and memory resources;
- detects GPUs, available memory and supported backends where possible;
- records capability and compatibility information;
- does not assume specific hardware exists.

**Runtime Manager**

- selects a compatible execution backend;
- manages runtime installation or discovery;
- controls model loading and unloading;
- starts and stops runtime processes where applicable;
- manages lifecycle, health checks, resources, timeouts and failures.

**Local Inference Adapter**

- translates typed model requests into runtime-specific operations;
- supports supported sampling and reasoning parameters;
- returns structured inference results;
- handles failures and cancellation.

**Model Profile**

- links a model to compatible runtime configuration;
- defines generation parameters;
- stores non-secret configuration;
- supports independent agent assignments.

**Agent**

- applies role, policy, allowed tools and skill instructions;
- refers to a configured model profile;
- does not own the inference engine.

### Runtime selection

Before selecting a runtime implementation:

1. Inspect project requirements.
2. Identify candidate native or plugin-managed local inference backends.
3. Evaluate supported model formats and platforms.
4. Examine CPU/GPU support, licensing and integration constraints.
5. Evaluate startup, cancellation, memory management and failure recovery.
6. Choose the smallest viable implementation for the first working inference flow.
7. Document the decision and rejected alternatives.

A llama.cpp-based managed runtime is a valid candidate, but do not assume it is already selected or implemented.

An HTTP interface to a plugin-managed local process is acceptable when properly controlled.

The plugin must own that process's lifecycle.

Do not make users manually start a separate model server as a prerequisite for normal Base Plugin operation.

### Runtime security

Where applicable, require:

- verified runtime binaries;
- supported runtime versions;
- controlled installation locations;
- explicit consent before downloads;
- license compliance;
- restricted local process access;
- loopback-only network binding where applicable;
- safe temporary files;
- process cleanup;
- model-path validation;
- execution timeout and cancellation.

Do not bundle or automatically download large model binaries.

Offline inference must be possible after the required local runtime and user-supplied model resources are available.

## 6. Runtime agent generation

Generate meaningful agent definitions under:

`.ai/agents/<agent-name>/AGENT.md`

Inspect existing files before creating or changing them.

Do not overwrite useful existing instructions unnecessarily.

### Initial agent roles

Derive the minimal required roles from README.md and the current milestone.

Potential roles include:

**Default / General Advisor**

- receives user tasks;
- applies grounding and security restrictions;
- selects allowed skills;
- executes through an assigned model profile;
- presents validated results.

**Coding Agent**

- processes source-code-related tasks;
- uses explicitly permitted project context and tools;
- produces structured implementation guidance or authorized changes.

**Judge Agent**

- evaluates model output using defined criteria;
- produces structured evaluation results;
- does not override deterministic failures.

Specialized agents should be generated only when their responsibilities and integration contracts are justified.

A single model may serve multiple agents through different profiles or role configurations.

Do not require one model per agent.

### Every AGENT.md must define

1. Agent name and responsibility.
2. Intended operating context.
3. Model-profile selection requirements.
4. Allowed skills.
5. Allowed tool capabilities.
6. Grounding scope and context restrictions.
7. Security and confidentiality rules.
8. Expected input.
9. Expected structured output.
10. Error and failure behavior.
11. Conditions for requesting user authorization.
12. Limitations and unsupported operations.

Do not hard-code a particular installed language model.

Do not claim an agent has tools or capabilities that have not been implemented.

Security-critical restrictions must also be represented in programmatically enforceable application architecture.

## 7. Runtime skill generation

Generate task-oriented runtime instructions under:

`.ai/skills/<skill-name>/SKILL.md`

Runtime skills are consumed by Integrated AI Workbench agents, not by Codex directly.

Create the minimum set needed to support the planned Base Plugin workflow.

Potential initial workflows:

- project-context-grounding;
- prompt-composition;
- structured-response;
- secure-tool-execution;
- response-evaluation.

Do not create a separate skill when the behavior properly belongs to an ordinary application service rather than an
agent workflow.

### Every runtime SKILL.md must define

- name and purpose;
- compatible agent roles;
- prerequisite capabilities;
- inputs and outputs;
- workflow sequence;
- allowed tools;
- safety constraints;
- relevant configuration;
- completion conditions;
- failure handling;
- concrete examples where useful.

At least one applicable runtime skill must demonstrate one-shot or few-shot prompting.

Examples must be relevant to the project and must not contain secrets.

Distinguish:

- an instruction file that exists;
- a prompt loader that can read it;
- a functional agent that actually uses it.

Do not claim these three states are equivalent.

## 8. Codex task skills — separate responsibility

Development tasks require a separate class of skills.

Location:

`.codex/skills/skill-iwb-task-<task-slug>/SKILL.md`

These skills instruct Codex to implement application code.

They are not runtime skills loaded by the Integrated AI Workbench plugin.

### Rules

- One cohesive implementation task should correspond to one Codex task skill.
- A task may touch several files when they implement one complete responsibility.
- Do not create a separate task merely for every class or source file.
- Do not create duplicate tasks for already completed functionality.
- Keep task scope small enough for focused implementation and review.
- Reuse existing project conventions, source paths and abstractions.
- Prefer a working vertical slice over many disconnected placeholders.

Each generated task skill must be directly executable by Codex.

Do not generate vague instructions such as "implement the service according to best practices."

## 9. Implementation backlog

Create and maintain:

`docs/development/tasks/README.md`

This is the canonical development backlog.

For each planned task, create:

`docs/development/tasks/TASK-<number>-<slug>.md`

And its corresponding Codex execution skill:

`.codex/skills/skill-iwb-task-<slug>/SKILL.md`

Use stable IDs.

Do not renumber existing tasks merely because priorities change.

Before adding tasks, inspect any existing task plan and preserve completed work.

### Each task specification must contain

**Identity**

- stable task ID;
- descriptive title;
- priority;
- implementation status;
- affected subsystem.

**Reason**

- corresponding README requirement;
- intended user or architectural value;
- relevant AI course concept, if any.

**Dependencies**

- required preceding tasks;
- relevant existing interfaces and services;
- necessary development environment.

**Scope**

- files to inspect;
- files expected to be created or modified;
- relevant architecture boundaries;
- implementation responsibilities;
- explicitly excluded functionality.

**Contract**

- expected inputs and outputs;
- class or interface responsibilities;
- runtime or UI interaction;
- error behavior;
- applicable security requirements.

**Acceptance**

- observable behavior;
- compilation requirements;
- unit or integration validation;
- negative/failure-path behavior where relevant;
- definition of done.

**Execution**

- corresponding Codex skill name;
- how to invoke it in the available Codex environment.

### Task status

Use:

- `TODO`
- `READY`
- `BLOCKED`
- `IN_PROGRESS`
- `DONE`

Do not mark a task DONE because skeleton classes exist.

Mark DONE only when implementation and acceptance evidence support completion.

Identify blocked dependencies explicitly.

## 10. Initial task dependency planning

Use the following as a planning reference, not a fixed task list.

Generate tasks based on actual missing implementation and existing code.

### Foundation

- Confirm shared model, agent and runtime contracts.
- Complete typed configuration and request/response models.
- Establish service boundaries and dependency direction.
- Ensure the plugin boots with a minimal IntelliJ Tool Window.

### Local model preparation

- Model Registry and local model discovery.
- Model metadata and path validation.
- Hardware resource detection.
- Runtime capability matching.
- Managed local runtime selection and integration.
- Runtime lifecycle and model load/unload handling.
- Actual local model inference.

**A verified local inference path is an early architectural milestone.**

Do not defer this until after implementing every other subsystem.

### Agent execution

- Runtime Markdown prompt loading.
- Agent Registry and model-profile binding.
- Skill selection and prompt composition.
- Controlled project grounding.
- Input guards and secret sanitization.
- Structured model request and output validation.
- Controlled tool permission layer.

### User interaction

- Functional AI Workbench Tool Window.
- Model/profile selection.
- Agent and skill selection.
- Context selection and visibility.
- Prompt submission.
- Streaming or non-streaming response handling, according to actual runtime support.
- Error handling and cancellation.

### Evaluation

- Deterministic checks.
- Reproducible result metadata.
- Few-shot demonstration.
- Judge Agent and evaluation rubric.
- Structured LLM-as-a-Judge results.

### Completion

- One complete, real local inference workflow.
- Explicit demonstration of the required AI concepts.
- Verification against README.md.
- Independent review through `skill-ux-review`.

### Optional orchestration

Only implement an Advisor/Orchestrator when multiple effective agent workflows require coordination.

A single-agent application must not pay unnecessary architectural or runtime overhead for multi-agent orchestration.

## 11. Working vertical slice

Organize the development plan so it reaches one genuine execution path:

Developer enters prompt → Workbench Tool Window → selected Agent → applicable SKILL.md → controlled Grounding →
validated Prompt → Model Profile → managed Local Runtime → user-supplied Model → Structured Response → Output
Validation → Evaluation → displayed Result

The initial vertical slice may use a smaller subset provided that it executes real local inference.

Expand the workflow incrementally until the README-defined MVP is satisfied.

Do not use hard-coded text responses to claim that local inference works.

Mock providers may be used in unit tests, but not as proof of working inference.

## 12. AI course requirement mapping

Every course requirement must have:

1. A relevant README requirement.
2. An architectural owner.
3. A planned implementation task.
4. A concrete verification or demonstration method.
5. An actual implementation status.

Track at least:

| ID    | Concept               | Required demonstration                                                         |
|-------|-----------------------|--------------------------------------------------------------------------------|
| AI-01 | Grounding             | Selected project context reaches a model request under enforced limits.        |
| AI-02 | System/user roles     | Prompt roles remain correctly separated.                                       |
| AI-03 | Reasoning             | Supported thinking configuration or concise reasoning summary is demonstrated. |
| AI-04 | Security              | Input, output, context, secrets and tool access are controlled.                |
| AI-05 | Few-shot              | Markdown-based examples influence a controlled prompt workflow.                |
| AI-06 | Structured output     | Model output conforms to a validated typed contract.                           |
| AI-07 | Generation parameters | Supported parameters reach the local runtime.                                  |
| AI-08 | Thinking level        | Supported reasoning configuration is applied correctly.                        |
| AI-09 | Sampling              | Sampling parameters are configurable and respected.                            |
| AI-10 | LLM-as-a-Judge        | Model-based evaluation produces a structured assessment.                       |
| AI-11 | Markdown prompts      | Runtime-loaded AGENT.md and SKILL.md affect execution.                         |
| AI-12 | Integration           | One complete local model workflow executes end to end.                         |

Do not require exposure of a model's hidden private chain of thought.

Use reasoning summaries when appropriate.

Maintain the requirement mapping in the existing `docs/development/AI-REQUIREMENTS.md`, if available. Otherwise create
it.

Avoid introducing a duplicate requirement matrix.

## 13. Task generation quality

Every task must be:

- actionable;
- architecture-aligned;
- sufficiently bounded;
- dependency-aware;
- implementation-ready;
- testable;
- traceable to README.md.

Generated Codex task skills must specify the actual expected workflow.

Each task skill should include:

1. YAML frontmatter with a unique skill name and description.
2. Applicable README context.
3. Task ID and objective.
4. Prerequisite tasks.
5. Relevant existing files.
6. Required implementation changes.
7. Interfaces and data contracts.
8. Security constraints.
9. Verification commands.
10. Acceptance criteria.
11. Explicit non-goals.

Do not specify unsupported commands or invented APIs.

When exact class paths depend on bootstrap results, inspect and use actual paths.

Do not prescribe obsolete IntelliJ Platform APIs.

Use official platform conventions appropriate to the chosen development baseline.

## 14. Task implementation safety

Generated Codex implementation skills must instruct the coding agent to:

- inspect Git status before editing;
- read relevant README files;
- inspect related code;
- preserve unrelated changes;
- implement only the selected task;
- maintain existing architectural boundaries;
- validate compilation and relevant tests;
- inspect errors and fix confirmed causes;
- report actual results.

Do not authorize destructive Git operations.

Never modify remote `master`.

Do not automatically commit or push.

Do not hard-code machine-specific model paths, tokens or credentials.

Do not introduce mandatory cloud connections.

Do not install extra AI plugins as a shortcut.

Do not replace a managed local inference runtime with an external service merely because it is easier to demonstrate.

## 15. Planning and implementation separation

This skill generates planning and instruction artifacts.

It may create or update:

- runtime AGENT.md files;
- runtime SKILL.md files;
- relevant prompt templates;
- development task specifications;
- task-specific Codex skills;
- course requirement mappings;
- brief architecture decision records where necessary.

It must not:

- populate all Kotlin skeleton classes with production implementations;
- change IntelliJ Settings;
- change Gradle dependencies;
- implement runtime backends;
- launch autonomous multi-task coding sessions;
- claim task completion without code and validation evidence.

Those actions belong to subsequent task-specific Codex executions or the existing bootstrap skill.

## 16. Incremental maintenance

The skill must support repeated execution.

On subsequent runs:

1. Inspect the current README hierarchy.
2. Inspect completed and pending tasks.
3. Inspect existing agents and skills.
4. Inspect implementation changes.
5. Inspect review findings.
6. Identify missing or outdated planning artifacts.
7. Update only what is necessary.

Preserve stable task IDs.

Preserve valid task-specific implementation instructions.

Do not overwrite manually improved instructions with generic templates.

Do not duplicate already implemented tasks.

If the README scope changes, update the affected task dependencies and acceptance criteria.

If an implementation satisfies a requirement, verify the evidence before marking the task DONE.

If a task is obsolete, mark it superseded or N/A with a reason rather than silently deleting its history.

## 17. Output structure

Use the repository's existing conventions.

Expected planning artifacts may include:

```text
integrated-ai-workbench/
├── AGENTS.md
├── README.md
├── .ai/
│   ├── agents/
│   │   ├── default/
│   │   │   └── AGENT.md
│   │   └── judge/
│   │       └── AGENT.md
│   ├── skills/
│   │   ├── project-grounding/
│   │   │   └── SKILL.md
│   │   └── response-evaluation/
│   │       └── SKILL.md
│   ├── prompts/
│   ├── profiles/
│   └── policies/
├── .codex/
│   └── skills/
│       ├── skill-readme-settings-and-structure/
│       │   └── SKILL.md
│       ├── skill-ux-review/
│       │   └── SKILL.md
│       ├── skill-integrated-ai-workbench/
│       │   └── SKILL.md
│       ├── skill-iwb-task-model-registry/
│       │   └── SKILL.md
│       ├── skill-iwb-task-local-runtime/
│       │   └── SKILL.md
│       └── ...
└── docs/
    └── development/
        ├── AI-REQUIREMENTS.md
        └── tasks/
            ├── README.md
            ├── TASK-001-....md
            ├── TASK-002-....md
            └── ...
```

This is an illustrative structure, not a requirement to create every example file.

Generate only justified agents, skills and tasks.

Adapt names and paths to the actual repository.

Do not create parallel skill directories when a corresponding existing skill already exists elsewhere.

Keep the task backlog in tracked documentation.

Use `_local/` only for temporary reports and local artifacts.

## 18. Validation

After generating the planning artifacts:

1. Verify all generated files exist.
2. Verify agent instructions are complete and internally consistent.
3. Verify task skills have valid metadata and executable instructions.
4. Verify task IDs are unique.
5. Verify dependencies refer to existing tasks.
6. Detect circular task dependencies.
7. Verify each active README MVP requirement has an implementation task or verified implementation.
8. Verify each mandatory AI course concept has a plan and acceptance evidence target.
9. Verify local inference is treated as a Base Plugin responsibility.
10. Verify no future extension is incorrectly made a mandatory MVP dependency.
11. Verify existing bootstrap and review skills remain intact.
12. Check Git diff for unintended changes.

Do not claim application functionality has been validated merely because planning files are valid.

## 19. Completion criteria

This planning task is complete when:

- runtime agent definitions have meaningful contents;
- relevant runtime workflow skills have meaningful contents;
- README requirements are translated into cohesive implementation tasks;
- tasks have explicit dependency order;
- each actionable coding task has a corresponding Codex implementation skill;
- local inference has a technically credible implementation path;
- no separate AI application is required by the planned Base Plugin;
- AI course requirements are traceable to tasks and verification methods;
- future capabilities are correctly deferred;
- existing project instructions and skills are preserved.

## 20. Final response

Provide a concise Estonian summary:

- agents created or updated;
- runtime skills created or updated;
- number of development tasks;
- number of Codex task skills;
- earliest READY task;
- blocked tasks and their dependencies;
- major architectural decisions;
- remaining unknowns.

Do not reproduce the complete planning documents in the final response.

Do not claim any Kotlin implementation is complete unless verified by actual code and execution evidence.

**Guiding principle:** Transform README-defined intent into actionable, dependency-ordered implementation work. Build
meaningful agent and skill instructions, prioritize an integrated local inference engine, and progress toward a real,
testable Base Plugin MVP rather than a large collection of disconnected skeleton classes.