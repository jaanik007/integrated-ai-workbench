---
name: skill-integrated-ai-workbench
description: Creates a content in project integrated-ai-workbench for IntelliJ IDEA plugin namely Integrated AI Workbench. The plugin must provide one IDE-native environment for local and cloud LLMs, grounding, AGENT/SKILL prompt workflows, secure tool use, structured output, evaluation, LLM-as-a-judge, and extensible model training orchestration without duplicating IntelliJ IDEA built-in capabilities or requiring another AI plugin.
---

Integrated AI Workbench — new project implementation skill

# 1. Role

You are the implementation agent responsible for creating a new IntelliJ IDEA plugin project.

Naming convention:

- User-facing Plugin Marketplace Name / UI Title:** Integrated AI Workbench (Natural regular naming)
- Technical Plugin ID / Gradle Artifact:** `integrated-ai-workbench` (Kebab-case)
- Class Prefix:** `AiWorkbench...` (PascalCase)

Build the project as a clean, extensible IntelliJ Platform plugin from scratch.
---

# 2. Primary project objective

Create an IntelliJ IDEA plugin that provides a unified AI workbench inside the IDE.

The long-term system must support:

- local language models;
- cloud language models;
- configurable model routing;
- project-aware grounding;
- AGENT.md workflows;
- SKILL.md workflows;
- prompt files loaded from Markdown;
- one-shot and few-shot prompting;
- structured model output;
- security guards;
- model generation parameters;
- reasoning/thinking configuration;
- controlled tool usage;
- evaluation;
- deterministic checks;
- LLM-as-a-judge;
- dataset management;
- supervised fine-tuning;
- LoRA / QLoRA;
- checkpoints;
- training orchestration;
- optional from-scratch training;
- supervised training guidance by a stronger model.

The plugin must become a useful general IntelliJ IDEA extension, not a project-specific one-off implementation.

---

# 3. Core product principle

The plugin must NOT unnecessarily reimplement capabilities already provided by IntelliJ IDEA or existing system tools.

Do not recreate:

- code editor;
- Project tool window;
- terminal;
- Git client;
- debugger;
- test runner;
- Gradle;
- Maven;
- run configurations;
- file system browser;
- Docker;
- compiler;
- standard IDE inspections.

Use IntelliJ Platform APIs and external runtimes instead.

Example:

```text
IntelliJ UI
    ↓
AI Workbench plugin
    ↓
TrainingRuntime abstraction
    ↓
Python / PyTorch / CUDA / Transformers / external process
```

The plugin owns:

- orchestration;
- configuration;
- AI workflow state;
- grounding;
- prompt composition;
- routing;
- validation;
- security;
- result presentation;
- evaluation;
- reproducibility.

The underlying model runtime or training framework may remain external.

---

# 4. Product value

The plugin exists because current AI development workflows are fragmented across multiple applications.

Typical current workflow:

```text
IntelliJ IDEA
+ separate AI plugin
+ LM Studio
+ llama.cpp
+ terminal
+ Python scripts
+ training tools
+ cloud console
+ prompt files
+ evaluation scripts
+ model manager
```

Desired workflow:

```text
IntelliJ IDEA
        ↓
Integrated AI Workbench
        ↓
local / cloud / training / evaluation runtimes
```

The developer should be able to stay inside IntelliJ IDEA for the practical workflow.

---

# 5. Local-first but provider-independent

The architecture must not be hard-coded to specific single model provider.

Use a provider abstraction.

Conceptually:

```text
ModelProvider
├── LocalOpenAiCompatibleProvider
├── LlamaCppProvider
├── CloudModelProvider
├── GoogleGenAiProvider
├── OpenAiCompatibleCloudProvider
└── OpenAiCompatibleLocalFirstIntegration
```

Do not require users to install another IntelliJ AI plugin.

External model servers may be used when appropriate, but they must be accessed through this plugin's own provider
integration and ability to integrate any local model right inside IDE to open project as part of it.

A developer must be able to configure:

```text
local model
cloud model
teacher model
judge model
embedding model
training target model
local model integration to open project (models folder inside project file system)
```

independently.

---

# 6. Model profiles

Create a model profile concept.

A profile represents a model plus inference configuration.

Example conceptual configuration:

```yaml
name: local-coder
provider: local-openai-compatible
model: qwen-coder

generation:
  temperature: 0.2
  top_p: 0.9
  max_tokens: 4096
  timeout_seconds: 60

reasoning:
  level: medium

security:
  cloud_allowed: false

prompt:
  agent: .ai/agents/java-backend/AGENT.md
  skill: .ai/skills/controller-review/SKILL.md
```

The implementation must be extensible enough to support provider-specific parameters.

Do not assume that all providers support the same parameters.

Make difference of local model intended to be integrated in project functionality and is part of future application
runtime and second use case where local model is used as integrated tool inside IntelliJ IDEA.

---

# 7. Model router

Design a model-routing layer.

The system must eventually support rules such as:

```text
coding
→ local-coder

architecture-review
→ cloud-supervisor

dataset-generation
→ teacher-model

evaluation
→ judge-model

training
→ local-target-model

sensitive-data
→ local-only

integrated-runtime-model
→ local-project-model
```

Routing priorities:

```text
security policy
    ↓
explicit user choice
    ↓
workflow configuration
    ↓
router default
```

A model or agent must never override a security restriction.

Any new project application specific integrated local model instruction files like SYSTEM.md, AGENT.md, TOOLS.md,
SKILL.md have subfolders inside models folder in project tree used only by local models located in this models folder
and as project context or task for generation for other models used with IDE for any project completion related tasks.

---

# 8. IntelliJ-native architecture

The project must use IntelliJ Platform architecture appropriately.

Prefer project-level services for project-specific AI state.

Conceptual architecture:

```text
IntelliJ IDEA
│
├── existing IDE capabilities
│
└── AI Workbench Plugin
    │
    ├── UI
    │   └── AI Workbench Tool Window
    │
    ├── Model Registry
    ├── Model Router
    ├── Provider Layer
    ├── Context / Grounding Engine
    ├── Prompt Engine
    ├── Security Layer
    ├── Agent Engine
    ├── Tool Engine
    ├── Structured Output Layer
    ├── Evaluation Engine
    ├── LLM Judge
    ├── Run History
    └── Training Workbench
```

Keep clear package boundaries.

Do not place all logic inside the Tool Window class. Create git ignored folder in project tree when this plugin is
applied to project and include there all this project specific variable folders for AI model instruction files and
CONFIGURATION.toml, SETTINGS.json and other this kind of files as if applicable.

---

# 9. Recommended package structure

Use a clear namespace appropriate for the project.

Conceptual structure:

```text
src/main/kotlin/.../ai-workbench/
├── action/
├── agent/
├── config/
├── context/
├── evaluation/
├── model/
│   ├── provider/
│   ├── profile/
│   └── routing/
├── prompt/
├── security/
├── service/
├── structured/
├── tool/
├── training/
├── ui/
│   └── toolwindow/
└── util/
```

Resources:

```text
src/main/resources/
├── META-INF/
│   └── plugin.xml
└── prompts/
    └── defaults/
```

Project-level AI configuration:

```text
.ai/
├── agents/
│   └── default/
│       └── AGENT.md
├── skills/
│   └── default/
│       └── SKILL.md
├── prompts/
├── profiles/
└── evaluation/
```

Any provider specific API KEY's should be located together with other settings per specific provider but never exposed
as characters but hidden *******.

---

# 10. Graduation-project AI requirements

The implementation and documentation must explicitly demonstrate all required AI concepts.

Mandatory:

- Grounding
- Chain of Thought / reasoning capability
- System and user roles/prompts
- Basic security
- input sanitation
- output sanitation
- input length caps
- context limits
- one-shot or few-shot prompting
- examples inside prompts
- structured output
- generation parameters
- thinking/reasoning level
- sampling configuration

Bonus requirements that must also be implemented if feasible:

- LLM-as-a-judge
- prompts loaded from Markdown files

For this project both bonus items are considered part of the intended design.

---

# 11. Grounding

Grounding must be a first-class subsystem.

Create a `ContextService` or equivalent abstraction.

Potential grounding sources:

```text
active editor file
selected code
selected project files
README files
project documentation
API specification
database schema
Git diff
compiler output
test output
AGENT.md
SKILL.md
other explicitly approved files
```

Never automatically send the entire project to a model.

Grounding must support controlled selection.

Required controls:

```text
allowed project roots
maximum number of files
maximum file size
maximum context characters/tokens
allowed file types
binary-file exclusion
secret filtering
cloud/local policy
```

The UI must eventually make visible which context was used.

---

# 12. Prompt architecture

Keep system instructions, task instructions, project context and user input conceptually separate.

Preferred composition:

```text
SYSTEM
→ AGENT.md

TASK / WORKFLOW INSTRUCTIONS
→ SKILL.md

GROUNDING CONTEXT
→ ContextService output

USER
→ actual user task
```

Where the model API supports native roles, preserve them instead of flattening everything into one string.

---

# 13. Markdown prompt files

Prompt files must be external to code.

Required concepts:

```text
AGENT.md
SKILL.md
PROMPT.md
```

The plugin must load prompt content from Markdown at runtime.

Changing a prompt must not require recompiling the plugin.

Recommended project layout:

```text
.ai/
├── agents/
│   └── java-backend/
│       └── AGENT.md
├── skills/
│   └── controller-review/
│       └── SKILL.md
└── prompts/
    └── evaluator.md
```

---

# 14. AGENT.md semantics

`AGENT.md` defines the durable role and constraints of an agent.

It may define:

- identity;
- responsibility;
- communication behaviour;
- allowed tools;
- forbidden actions;
- security constraints;
- project scope;
- high-level operating rules.

Do not make application security dependent only on text in AGENT.md.

Programmatic enforcement must exist separately.

---

# 15. SKILL.md semantics

`SKILL.md` defines a repeatable task-specific workflow.

It may define:

- task goal;
- prerequisites;
- accepted input;
- workflow steps;
- expected output;
- validation rules;
- tool sequence;
- one-shot examples;
- few-shot examples;
- evaluation rubric.

Example conceptual hierarchy:

```text
AGENT
Java Backend Agent

SKILL
Spring Controller Review
```

One agent may support multiple skills.

---

# 16. One-shot and few-shot prompting

Implement support for prompt examples.

At least one graduation-project demonstration must compare:

```text
zero-shot
vs
one-shot or few-shot
```

A skill may contain:

```markdown
## Examples

### Example 1

Input:
Create GET /api/courses.

Expected output:
Controller → Service → Repository → DTO.

### Example 2

Input:
Create POST /api/location.

Expected output:
Controller → Service → Repository → Mapper → DTO.
```

Examples belong in the prompt/skill definition, not hard-coded into Kotlin business logic.

---

# 17. Reasoning / thinking

Support provider reasoning or thinking configuration when available.

Conceptual levels:

```text
none
medium
high
```

Do NOT attempt to expose a model's hidden private chain-of-thought if this is not a subject of open project specific
task.

Do NOT design UI that dumps hidden internal reasoning.

Instead expose a concise model-generated reasoning summary if useful.

Example structured response:

```json
{
  "result": "...",
  "reasoningSummary": "The controller delegates business logic to the service and keeps request validation at the boundary.",
  "confidence": 0.91
}
```

The graduation project may document this as controlled use of model reasoning without exposing private internal
reasoning traces.

---

# 18. Generation and sampling parameters

Model profiles must support applicable inference parameters such as:

```text
temperature
top_p
max_tokens
timeout
seed
reasoning/thinking level
```

Provider capability differences must be handled cleanly.

Unsupported parameters must not cause invalid requests.

The UI should eventually show editable parameters per model profile.

---

# 19. Structured output

Structured model output is mandatory for internal workflows.

Avoid parsing arbitrary prose when a machine-readable result is expected.

Example result:

```json
{
  "status": "SUCCESS",
  "answer": "...",
  "reasoningSummary": "...",
  "usedContext": [
    "ChatbotService.java",
    "AGENT.md"
  ]
}
```

Example evaluation:

```json
{
  "passed": true,
  "score": 0.94,
  "criteria": {
    "correctness": 0.95,
    "security": 1.0,
    "instructionFollowing": 0.88
  },
  "feedback": "..."
}
```

Create typed internal DTO/model classes.

Validate structured output before passing it to later workflow stages.

---

# 20. Security architecture

Security must be implemented programmatically.

Target flow:

```text
User Input
    ↓
InputGuard
    ↓
ContextBuilder
    ↓
PromptBuilder
    ↓
ModelProvider
    ↓
StructuredOutputValidator
    ↓
ToolPermissionGuard
    ↓
Tool execution
```

Required security controls:

```text
maximum input length
maximum context size
maximum file count
maximum individual file size
allowed project roots
file type filtering
binary exclusion
secret detection / masking
output sanitation
structured schema validation
tool allowlist
tool timeout
cloud/local data policy
```

Secrets such as:

```text
API keys
passwords
tokens
private keys
.env values
```

must not be silently sent to a cloud provider.

Do not log secrets.

---

# 21. Tool architecture

Do not give models unrestricted operating-system access.

Expose specific tools through controlled adapters.

Conceptual tools:

```text
ReadFileTool
ProjectSearchTool
GitDiffTool
CompileTool
TestTool
RunConfigurationTool
```

Potential future tools:

```text
TerminalTool
DatabaseSchemaTool
HttpRequestTool
McpTool
```

Each tool should define:

```text
name
description
input schema
permission requirement
timeout
structured result
```

High-impact operations must require explicit permission.

Read-only operations should be preferred by default.

---

# 22. MCP compatibility

The architecture should allow MCP integrations later.

Do not build the initial system around MCP exclusively.

Treat MCP as one possible external tool/provider integration mechanism.

Conceptually:

```text
ToolRegistry
├── NativeIntelliJTool
├── InternalTool
└── McpToolAdapter
```

The plugin must remain functional without requiring an MCP plugin from another vendor.

---

# 23. Evaluation engine

Evaluation must support both deterministic and model-based checks.

Evaluation order:

```text
Model Output
    ↓
Deterministic checks
    ├── structured schema
    ├── compile
    ├── tests
    ├── required files
    ├── forbidden content
    └── security rules
    ↓
LLM Judge
```

A deterministic failure must not be overruled by an LLM judge.

Example:

```text
compile = FAIL
→ overall technical result cannot be PASS
```

---

# 24. LLM-as-a-judge

Implement an extensible judge abstraction.

Conceptually:

```text
JudgeService
    ↓
JudgeModelProfile
    ↓
EvaluationRubric
    ↓
Structured JudgeResult
```

Example:

```json
{
  "score": 0.91,
  "passed": true,
  "criteria": {
    "correctness": 0.95,
    "instructionFollowing": 0.90,
    "security": 1.0,
    "maintainability": 0.80
  },
  "feedback": "The result is correct but duplicates project logic."
}
```

The judge prompt should be loadable from Markdown.

---

# 25. Training Workbench

The architecture must reserve a real subsystem for model training.

The training layer should eventually support:

```text
dataset management
dataset validation
train/validation split
supervised fine-tuning
LoRA
QLoRA
evaluation
checkpoint management
model export
```

Future capability:

```text
from-scratch training
```

Do NOT attempt to rewrite PyTorch or Transformers inside the plugin.

Use a runtime adapter.

Conceptually:

```text
TrainingService
    ↓
TrainingRuntime
    ├── LocalPythonRuntime
    ├── DockerTrainingRuntime
    └── RemoteTrainingRuntime
```

---

# 26. Supervised training live guidance

The architecture must allow a stronger model to supervise training of a smaller target model.

Concept:

```text
Teacher / Supervisor Model
        ↓
AGENT + SKILL
        ↓
Task / example generation
        ↓
Target lightweight model
        ↓
Model output
        ↓
Deterministic evaluation
        ↓
LLM Judge
        ↓
Supervisor feedback
        ↓
Dataset improvement
        ↓
Next training iteration
```

Potential specialized tasks:

```text
Spring controller generation
REST API design
SQL generation
code review
documentation generation
project-specific coding conventions
```

The target is not necessarily to produce a general-purpose LLM.

The target may be a lightweight model specialized for a narrow, repeatable agentic task.

---

# 27. Agentic training objective

Support the conceptual goal:

```text
same AGENT
+
same SKILL
+
same grounding rules
+
same tool set
+
same evaluation
→
repeatable correct behaviour
```

A useful research question for the graduation project is:

> Can a lightweight local model become reliable enough for a narrow agentic software-engineering task when guided by a
> stronger teacher model, grounded project context, skill instructions, few-shot examples and repeated evaluation?

The implementation should make this measurable.

---

# 28. Reproducibility

Every important run should have a reproducible run record.

Conceptual `RunRecord` / `TrainingRecipe`:

```text
run id
timestamp
project
base model
model version/hash
provider
model profile
temperature
top_p
max_tokens
reasoning level
dataset version
AGENT version/hash
SKILL version/hash
prompt version/hash
grounding sources
tool configuration
random seed
judge model
evaluation result
training parameters
checkpoint
```

Do not depend only on human memory or terminal logs.

---

# 29. AI Workbench Tool Window

Create one primary IntelliJ Tool Window.

Working name:

```text
AI Workbench
```

Long-term sections:

```text
AI Workbench
├── Chat
├── Context
├── Models
├── Agents
├── Skills
├── Tools
├── Training
├── Evaluation
└── Runs
```

Do not implement all advanced sections as fake fully working UI during the first step.

Build real functionality incrementally.

---

# 30. Initial MVP

The first working version must be intentionally small but architecturally correct.

Required MVP:

```text
IntelliJ Tool Window
        ↓
Model Profile
        ↓
Provider abstraction
        ↓
AGENT.md
        ↓
SKILL.md
        ↓
User prompt
        ↓
Grounded project context
        ↓
Input/security validation
        ↓
Model request
        ↓
Structured response
        ↓
Output validation
        ↓
Result displayed in Tool Window
```

The MVP should support at least one provider end-to-end.

Prefer an OpenAI-compatible HTTP provider abstraction first if it allows easy testing with multiple local runtimes.

Do not bind the whole architecture to that provider.

---

# 31. Graduation-project demonstration workflow

The finished graduation-project MVP should demonstrate a complete flow:

```text
IntelliJ project
      ↓
selected grounding
      ↓
AGENT.md
      ↓
SKILL.md with few-shot example
      ↓
Model Profile
      ↓
local or cloud model
      ↓
configured thinking/sampling
      ↓
structured output
      ↓
security validation
      ↓
deterministic evaluation
      ↓
LLM-as-a-judge
      ↓
stored run result
```

The demonstration should visibly prove the assessment criteria instead of only describing them in README.

---

# 32. Initial files to create

Create an appropriate IntelliJ Platform Gradle plugin project.

At minimum establish files conceptually equivalent to:

```text
build.gradle.kts
settings.gradle.kts
gradle.properties

src/main/kotlin/.../ai-workbench/
├── ui/toolwindow/
│   ├── AiWorkbenchToolWindowFactory.kt
│   └── AiWorkbenchPanel.kt
│
├── model/
│   ├── ModelProvider.kt
│   ├── ModelRequest.kt
│   ├── ModelResponse.kt
│   └── profile/
│       └── ModelProfile.kt
│
├── service/
│   └── AiWorkbenchService.kt
│
├── context/
│   ├── ContextService.kt
│   └── GroundingContext.kt
│
├── prompt/
│   ├── PromptLoader.kt
│   └── PromptComposer.kt
│
├── security/
│   ├── InputGuard.kt
│   ├── SecretSanitizer.kt
│   └── OutputGuard.kt
│
├── structured/
│   └── StructuredResponseValidator.kt
│
├── evaluation/
│   ├── EvaluationService.kt
│   └── JudgeService.kt
│
└── training/
    ├── TrainingService.kt
    └── TrainingRuntime.kt

src/main/resources/
└── META-INF/
    └── plugin.xml

.ai/
├── agents/
│   └── default/
│       └── AGENT.md
├── skills/
│   └── default/
│       └── SKILL.md
└── prompts/
    └── judge.md

docs/
├── ARCHITECTURE.md
├── SECURITY.md
├── AI-REQUIREMENTS.md
└── ROADMAP.md

README.md
```

Adjust exact names when required by IntelliJ Platform conventions.

---

# 33. Initial UI behaviour

The initial Tool Window must contain at minimum:

```text
AI Workbench

Model: [selected model profile]

Agent: [selected AGENT.md]
Skill: [selected SKILL.md]

Context:
[selected grounding summary]

Prompt:
[text input]

[Run]

Result:
[structured/displayed response]
```

The implementation may begin simpler but should evolve toward this shape.

Do not create decorative controls that are not wired to real functionality.

---

# 34. Initial provider behaviour

Implement the provider API cleanly.

Example conceptual interface:

```kotlin
interface ModelProvider {
    fun id(): String

    suspend fun generate(
        request: ModelRequest
    ): ModelResponse
}
```

Actual implementation may differ according to IntelliJ/Kotlin architecture.

The provider must not directly access arbitrary project files.

Context must already have been prepared by the grounding layer.

---

# 35. Model request concept

A request should carry explicit structured data.

Conceptually:

```text
systemPrompt
skillPrompt
userPrompt
groundingContext
modelProfile
generationParameters
requestedOutputSchema
```

Avoid passing a single uncontrolled concatenated string throughout the entire application.

---

# 36. Security defaults

Default behaviour must be conservative.

Preferred defaults:

```text
file access → read-only
external tool execution → disabled unless explicitly enabled
cloud sharing → explicit configuration
secret files → excluded
binary files → excluded
huge files → excluded
project root escape → forbidden
tool commands → allowlisted
structured output validation → enabled
```

Never silently execute a model-provided shell command.

---

# 37. Configuration and secrets

Do not commit API keys.

Support secure configuration through appropriate mechanisms such as:

- environment variables;
- IntelliJ PasswordSafe for secret persistence;
- non-secret project settings;
- user-level settings.

Do not store secrets in:

```text
README
AGENT.md
SKILL.md
plugin.xml
Git-tracked properties files
```

---

# 38. Existing development workflow support

The design should support workflows common in full-stack software projects, including:

```text
Java
Spring Boot
REST APIs
Vue
JavaScript
Postgres
SQL
Swagger / OpenAPI
Git
Gradle
project documentation
local LLMs
cloud LLMs
AGENT.md
SKILL.md
```

Do not hard-code behavior specifically for these technologies.

Implement them through reusable grounding, agent, skill and tool abstractions.

---

# 39. Coding standards

Use clean architecture principles without unnecessary abstraction.

Requirements:

- small focused classes;
- dependency inversion where useful;
- interfaces at genuine extension boundaries;
- no giant god service;
- no giant Tool Window class;
- no provider-specific logic inside generic UI;
- no business logic inside Swing listeners;
- no secrets in logs;
- no hidden global mutable state;
- no duplicate implementations of the same policy.

Prefer constructor injection or IntelliJ service patterns where appropriate.

Use descriptive names.

Do not create abstraction merely to increase file count.

---

# 40. Documentation requirements

Create documentation while implementing.

`README.md` must explain:

```text
what the plugin does
why it exists
how to run the plugin
current MVP capabilities
supported providers
configuration
security model
project status
known limitations
roadmap
```

`docs/AI-REQUIREMENTS.md` must explicitly map graduation requirements:

```text
Grounding
Chain of Thought / reasoning
System + user roles
Basic security
One-shot / few-shot
Structured output
Parameters
Thinking level
Sampling
LLM-as-a-judge
Markdown prompts
```

For every item document:

```text
requirement
implementation
relevant class/file
demonstration method
current status
```

`docs/ARCHITECTURE.md` must explain component relationships.

`docs/SECURITY.md` must explain actual programmatic controls.

`docs/ROADMAP.md` must clearly distinguish:

```text
implemented
MVP
planned
future research
```

Do not claim unfinished functionality as implemented.

---

# 41. Testing requirements

Create tests for logic that can be tested independently of the IDE UI.

Priority:

```text
PromptLoader
PromptComposer
InputGuard
SecretSanitizer
Context limits
Model profile validation
Structured output validation
Model router
Evaluation logic
```

Provider integration should be testable using a fake/mock provider.

Avoid tests that depend on a real paid cloud API by default.

---

# 42. Development workflow

Work incrementally.

Use this order unless repository state requires a justified change.

## Phase 1 — project bootstrap

1. Create IntelliJ Platform plugin project.
2. Configure Gradle.
3. Configure plugin metadata.
4. Confirm `build`.
5. Confirm `runIde`.
6. Create AI Workbench Tool Window.
7. Display a minimal working UI.

Acceptance:

```text
plugin builds
sandbox IntelliJ starts
AI Workbench Tool Window is visible
```

---

## Phase 2 — core abstractions

Create:

```text
ModelProvider
ModelProfile
ModelRequest
ModelResponse
AiWorkbenchService
```

Create a fake provider first if useful.

Acceptance:

```text
user enters prompt
request travels through service/provider abstraction
response is displayed
```

---

## Phase 3 — Markdown prompts

Implement:

```text
PromptLoader
AGENT.md
SKILL.md
PromptComposer
```

Acceptance:

```text
editing AGENT.md or SKILL.md changes the runtime prompt
without recompiling plugin code
```

---

## Phase 4 — grounding

Implement controlled project context.

Start with:

```text
active file
selected text
explicit selected files
```

Acceptance:

```text
UI shows which context is included
model request receives only approved context
```

---

## Phase 5 — security

Implement:

```text
input length cap
context cap
path validation
secret sanitation
output validation
```

Acceptance:

```text
oversized or forbidden input is rejected safely
secrets are not included in outbound cloud context
```

---

## Phase 6 — structured output

Introduce typed structured responses.

Acceptance:

```text
invalid model structure is rejected
valid structure is displayed
```

---

## Phase 7 — model parameters

Implement profile settings:

```text
temperature
top_p
max_tokens
timeout
reasoning level
```

Acceptance:

```text
request uses configured supported parameters
```

---

## Phase 8 — few-shot

Add examples to SKILL.md.

Acceptance:

```text
zero-shot and few-shot runs can be compared
```

---

## Phase 9 — evaluation

Implement deterministic checks and stored results.

Acceptance:

```text
run receives an evaluation result
```

---

## Phase 10 — LLM-as-a-judge

Implement judge provider/profile and rubric loaded from Markdown.

Acceptance:

```text
target response
→ deterministic checks
→ judge
→ structured JudgeResult
```

---

## Phase 11 — run history

Store reproducible run metadata.

Acceptance:

```text
previous run can be inspected
model/profile/prompt/context/evaluation metadata is visible
```

---

## Phase 12 — training foundation

Create:

```text
TrainingRuntime
TrainingService
TrainingRecipe
TrainingRun
```

Do not fake model training.

If real training is not implemented yet, clearly mark runtime operations as future work.

---

## Phase 13 — training MVP

When implementation reaches this phase, integrate a real lightweight training path.

Preferred first target:

```text
small open model
+
small controlled dataset
+
LoRA or QLoRA
+
before/after evaluation
```

The implementation must produce measurable before/after results.

---

# 43. Do not fake completeness

Never create empty UI tabs and describe them as completed features.

Use explicit statuses:

```text
IMPLEMENTED
PARTIAL
PLANNED
EXPERIMENTAL
```

The graduation project must be technically honest.

---

# 44. Do not overbuild the first iteration

The long-term vision is large.

Do not attempt to implement:

```text
all providers
all training frameworks
full from-scratch LLM training
complete MCP ecosystem
complex autonomous agent
distributed training
full model marketplace
```

before the basic architecture works.

The first priority is one complete vertical slice.

---

# 45. First vertical slice

The first serious milestone must prove:

```text
Tool Window
    ↓
Model Profile
    ↓
Markdown AGENT
    ↓
Markdown SKILL
    ↓
Grounding
    ↓
Input Security
    ↓
Model Provider
    ↓
Structured Output
    ↓
Output Validation
    ↓
Displayed Result
```

This vertical slice must compile and run before advanced features are added.

---

# 46. Definition of done for the initial project creation task

This skill's initial execution is complete only when:

- a valid IntelliJ Platform plugin project exists;
- Gradle wrapper exists;
- project builds;
- `runIde` works;
- sandbox IntelliJ opens;
- AI Workbench Tool Window appears;
- code has separated UI/service/provider/context/prompt/security concerns;
- `ModelProvider` abstraction exists;
- `ModelProfile` concept exists;
- Markdown prompt loading foundation exists;
- default `AGENT.md` exists;
- default `SKILL.md` exists;
- input length guarding exists;
- a structured response model exists;
- README exists;
- architecture documentation exists;
- AI graduation-requirements mapping exists;
- security documentation exists;
- roadmap exists;
- future training architecture is documented but not falsely presented as complete;
- code is ready for the next incremental task.

---

# 47. Required final verification

Before declaring the task complete:

1. inspect the complete project structure;
2. run Gradle build;
3. run tests;
4. verify plugin descriptor;
5. verify `runIde`;
6. verify Tool Window appears;
7. verify there are no committed secrets;
8. verify AGENT.md and SKILL.md are runtime-readable;
9. verify UI does not contain provider-specific architecture assumptions;
10. verify no existing IntelliJ capability was unnecessarily duplicated;
11. verify documentation accurately distinguishes implemented and planned features.

Report actual results.

Do not report commands as successful unless they were actually executed successfully.

---

# 48. Git safety

Do not:

- force push;
- reset user work;
- delete existing branches;
- modify remote master;
- merge branches;
- commit secrets;
- discard unrelated local changes.

Unless explicitly requested, leave Git mutations beyond ordinary local project file creation to the user.

---

# 49. Implementation decision rule

When choosing between:

```text
build a duplicate capability inside the plugin
```

and:

```text
integrate an existing IntelliJ or external runtime capability cleanly
```

prefer integration.

When choosing between:

```text
hard-code one provider
```

and:

```text
create a small provider abstraction
```

prefer the abstraction.

When choosing between:

```text
prompt-only safety
```

and:

```text
programmatically enforced safety
```

use programmatic enforcement.

When choosing between:

```text
free-form internal prose
```

and:

```text
typed structured communication
```

prefer structured communication.

When choosing between:

```text
large autonomous implementation
```

and:

```text
small verifiable vertical slice
```

build the verifiable vertical slice first.

---

# 50. Product vision

The project must evolve toward this user experience:

```text
Developer opens IntelliJ IDEA
        ↓
opens project
        ↓
opens AI Workbench
        ↓
selects:
    model
    agent
    skill
    context
    parameters
        ↓
runs task
        ↓
plugin safely grounds project context
        ↓
appropriate local/cloud model is used
        ↓
structured output is validated
        ↓
IDE-native tools may be invoked through controlled adapters
        ↓
result is evaluated
        ↓
optional LLM judge scores result
        ↓
run is stored reproducibly
```

For training:

```text
Developer selects target model
        ↓
selects dataset
        ↓
selects AGENT + SKILL
        ↓
selects teacher/supervisor model
        ↓
runs training workflow
        ↓
target model is evaluated
        ↓
teacher/judge feedback is collected
        ↓
dataset or configuration is improved
        ↓
next iteration
```

The long-term result should make IntelliJ IDEA a practical unified workbench for AI-assisted software development and
lightweight agentic model engineering.

---

# 51. Final guiding statement

Build the plugin around this principle:

> One IDE-native workbench should allow a developer to securely and reproducibly configure, ground, run, evaluate and
> train local or cloud-connected language models without fragmenting the development workflow across multiple unrelated
> applications.

Every architecture and implementation decision must support that principle. Every configuration made in this plugin
applicable to new or open project should overwrite any other active setting or configuration either in IntelliJ IDEA
global settings or project settings by deactivating or changing them in case they are parallel or in conflict with this
plugin by asking user to agree right after user has marked any change of settings or configurations applicable according
this rule.

```