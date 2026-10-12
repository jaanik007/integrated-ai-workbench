```markdown
# AI Agent Instructions

This file contains the global operating rules for AI assistants working in this repository.

Project-specific instructions, architecture rules, implementation guidance, and subsystem context are maintained in
`README.md` files throughout the project tree.

These instructions are model-agnostic and apply to any AI assistant, agent, coding model, IDE assistant, CLI agent, or
automated development tool used with this repository.

---

## Language

- Respond to the user in Estonian by default unless another language is requested.
- Keep technical terminology precise and concise.
- Code, identifiers, configuration keys, API names, file names, commands, and established technical terms shall remain in
  English.
- When guiding the user through interactive setup or configuration, prefer one actionable step at a time and wait for
  confirmation before continuing when appropriate.

---

## README.md files are the project instruction system

Before modifying or implementing anything in the project:

1. Discover the `README.md` files in the repository.
2. Read the root `README.md` first.
3. Identify the part of the project affected by the task.
4. Read the `README.md` files that apply to that part of the project.
5. Follow references from those files when additional context is required.

Typical scopes may include:

```text
README.md
frontend/README.md
backend/README.md
docs/README.md
src/.../README.md
```

The root `README.md` describes project-wide context, architecture, conventions, and rules.

A `README.md` deeper in the directory tree provides more specific context for that directory and its descendants.

For example:

```text
README.md
└── general project context and rules

backend/README.md
└── backend architecture and implementation rules

frontend/README.md
└── frontend architecture and implementation rules

docs/README.md
└── documentation structure and conventions
```

For a task affecting a specific file, apply the relevant instructions from the repository root down to the nearest
applicable `README.md`.

More specific instructions take precedence over broader instructions when they intentionally refine them.

Direct user instructions take precedence over repository documentation.

If instructions conflict and the intended precedence is unclear, stop and ask instead of guessing.

---

## Purpose of README.md files

`README.md` files should remain understandable to humans, but they also serve as the primary structured context source
for AI models working in the repository.

Use them to understand:

- project purpose;
- architecture;
- directory responsibilities;
- subsystem boundaries;
- implementation conventions;
- dependencies;
- data flow;
- API conventions;
- security requirements;
- model and AI workflow rules;
- testing requirements;
- documentation rules;
- current limitations;
- planned functionality;
- task-specific constraints.

Do not treat a README as merely descriptive documentation when it contains implementation or architecture rules.

When starting work in a project area, prefer reading the applicable README context before reconstructing the
architecture from source code alone.

---

## Work from the actual repository state

Always inspect the real project before making changes.

Do not assume that:

- a documented file exists;
- a documented dependency is installed;
- an implementation matches its documentation;
- a command is available;
- a particular IDE integration is active;
- a specific AI model is configured;
- another AI tool or plugin is installed;
- a local or remote service is running.

Repository documentation describes project intent, architecture, and conventions.

Use the actual runtime, tools, files, APIs, services, and capabilities available to the current environment to perform
the work.

Tool-specific examples in documentation are informative unless explicitly stated otherwise.

Do not invent missing files, APIs, dependencies, configuration values, runtime capabilities, or successful command
results.

---

## Scope discipline

Before editing a file:

1. determine which project area owns it;
2. determine which README instructions apply;
3. inspect related existing implementation;
4. identify the smallest correct change;
5. avoid unrelated modifications.

Do not perform broad refactoring merely because another design would also be possible.

Preserve established project conventions unless the task explicitly requires changing them.

Prefer extending existing abstractions over creating parallel duplicate implementations.

When adding a new major project area, create or update an appropriate `README.md` so future developers and AI models can
understand that area without reconstructing its architecture from source code.

---

## Documentation consistency

When implementation changes make an applicable `README.md` materially incorrect, update the documentation as part of the
same task unless the user explicitly excludes documentation changes.

Documentation must accurately distinguish where relevant:

```text
IMPLEMENTED
PARTIAL
PLANNED
EXPERIMENTAL
TODO
```

Do not claim planned or incomplete functionality as implemented.

Do not describe a command, build, test, integration, or runtime check as successful unless it was actually executed
successfully.

---

## Security

Treat credentials, tokens, API keys, passwords, private keys, environment variables, personal data, configuration
secrets, and external service credentials as sensitive.

Never:

- hard-code secrets;
- expose secrets in generated documentation;
- include secrets in logs or examples;
- commit secrets;
- copy secrets into prompts unnecessarily;
- send project data to an external service unless the task and project rules permit it.

Prefer least-privilege and read-only operations where possible.

Apply project-defined security controls programmatically where available.

Relevant controls may include:

```text
input length limits
output validation
context limits
allowed paths
file type filters
secret masking
tool allowlists
timeouts
cloud/local data policies
structured output validation
```

Do not rely on prompt instructions alone for security when programmatic enforcement is possible.

---

## Git and repository safety

Normal local Git operations may be used when they are relevant to the requested task.

This includes operations such as:

```text
git status
git diff
git add
git commit
git switch
git checkout
git branch
git fetch
git pull
git merge
git rebase
git stash
git restore
```

Use Git carefully and preserve unrelated user work.

Do not discard, overwrite, reset, or clean local changes unless the user explicitly requests it or the operation is
clearly required by the requested workflow.

Before potentially destructive local Git operations, inspect the working tree and preserve unrelated work.

### Remote master protection

Treat the remote `master` branch as protected.

Do not perform any operation that changes remote `master` unless the user explicitly requests that specific action.

This includes, but is not limited to:

```text
git push origin master
git push --force origin master
merging a feature branch into master and pushing it
rewriting remote master history
resetting remote master
deleting remote master
recreating remote master
force-updating remote master
```

Do not merge a feature branch into `master` and do not push the resulting `master` state to the remote unless the user
has explicitly requested it.

The primary protected boundary is:

```text
origin/master
```

Local development is not prohibited merely because it changes repository state.

Local branches and feature branches may be created, modified, merged, rebased, committed, or deleted when required by
the requested task.

Remote feature-branch operations may also be performed when they are part of the requested workflow, but never assume
permission to modify remote `master`.

Before modifying remote `master`:

1. verify the current branch;
2. verify the working tree state;
3. verify the intended commits;
4. verify that unrelated changes are not included;
5. confirm that the user explicitly requested the remote `master` change;
6. avoid force-push or history rewriting unless specifically requested.

Never discard unrelated local user work.

---

## Tool independence

Do not assume that instructions written for another AI model, IDE assistant, CLI agent, or plugin describe the current
runtime.

Use the tools actually available to the current agent.

Repository documentation may mention systems such as:

```text
Codex
Claude
IntelliJ AI Assistant
local LLMs
cloud LLMs
MCP
terminal tools
IDE integrations
training runtimes
external services
```

These references describe possible workflows, architecture, or project context.

They do not imply that the current agent has those tools available.

Translate the intended workflow to the current environment without changing the underlying project rules.

Do not require installation of another AI plugin merely because documentation refers to one.

---

## Existing capability reuse

Prefer integration with existing project, IDE, framework, or runtime capabilities over duplicate implementation.

Before creating a new capability:

1. inspect whether the project already contains an equivalent implementation;
2. inspect whether the IDE or framework already provides the capability;
3. reuse or integrate when appropriate;
4. create a new implementation only when it provides genuinely missing project functionality.

Do not duplicate existing editor, Git, terminal, build, test, debugger, project navigation, or other standard IDE
functionality unnecessarily.

---

## Implementation behaviour

Before implementing:

1. understand the user task;
2. inspect the relevant repository state;
3. load the applicable README context;
4. inspect related existing implementation;
5. identify existing abstractions that should be reused;
6. determine the smallest correct change;
7. implement within the established architecture;
8. validate using available project tooling;
9. update affected documentation when necessary.

Prefer clear, focused classes and modules.

Avoid:

- duplicate business logic;
- unnecessary abstractions;
- giant service classes;
- giant UI classes;
- provider-specific logic inside generic layers;
- hidden mutable global state;
- hard-coded environment-specific assumptions.

Do not create empty architectural placeholders and describe them as implemented functionality.

---

## AI and model-related work

When working on AI-related parts of the project, keep distinct concerns separated where the project architecture
supports them.

Typical concerns may include:

```text
model provider
model profile
model routing
grounding/context
prompt loading
AGENT instructions
SKILL instructions
security guards
structured output
tool permissions
evaluation
LLM-as-a-judge
training orchestration
run history
```

Do not hard-code provider-specific behaviour into generic architecture unless explicitly required.

Do not send arbitrary project context to a model without applying the project's grounding and security rules.

Do not expose or attempt to reconstruct hidden private model chain-of-thought.

When reasoning output is required, prefer a concise structured reasoning summary or decision explanation.

---

## Prompt and instruction files

Project AI instructions may be stored in Markdown files such as:

```text
AGENT.md
SKILL.md
PROMPT.md
README.md
```

Treat their responsibilities separately.

Typical intent:

```text
AGENTS.md
→ global repository entry point for AI behaviour

README.md
→ project and subsystem context

AGENT.md
→ runtime agent role, boundaries, and behavioural rules

SKILL.md
→ repeatable task-specific workflow

PROMPT.md
→ reusable prompt template
```

Do not merge these responsibilities unnecessarily.

Runtime prompts should remain external to application code where the project architecture requires them to be
configurable.

---

## README hierarchy as AI context

When a task belongs to a specific project subsection, load only the context that is relevant to that subsection in
addition to the root context.

For example:

```text
Task:
modify frontend model configuration UI

Context:
AGENTS.md
→ README.md
→ frontend/README.md
→ relevant frontend files
```

Another example:

```text
Task:
modify training runtime

Context:
AGENTS.md
→ README.md
→ training/README.md
→ relevant training files
```

Do not automatically load unrelated subsystem documentation unless cross-subsystem behaviour requires it.

The objective is relevant grounding, not maximum context volume.

---

## Validation

Use the project's actual validation mechanisms where applicable.

Examples:

```text
build
compile
tests
lint
type checking
IDE plugin validation
runIde
runtime startup
API validation
structured output validation
integration checks
```

Do not report success unless the check was actually performed successfully.

If validation cannot be performed, state exactly what was not verified.

When a validation command produces warnings but succeeds, distinguish warnings from failures.

---

## Error handling

When something fails:

1. inspect the actual error;
2. identify the first relevant root cause;
3. avoid speculative broad changes;
4. correct the smallest confirmed cause;
5. rerun the relevant validation.

Do not replace a working architecture based only on an unverified hypothesis.

Prefer evidence from:

```text
compiler output
runtime logs
test failures
IDE diagnostics
API responses
Git diff
actual configuration
```

over assumptions.

---

## User interaction

When the user is performing setup or configuration interactively:

- provide precise commands or UI steps;
- avoid unnecessary branching;
- prefer one step at a time when confirmation affects the next action;
- do not repeat already completed steps;
- use the user's actual current project state;
- explain only as much as necessary for the immediate action unless broader explanation is requested.

When the user requests a complete file, return a complete copy-paste-ready file rather than fragmented edits unless
there is a specific reason not to.

---

## Guiding principle

Use `AGENTS.md` only as the stable repository-wide entry point for AI behaviour.

Use the project's `README.md` hierarchy as the authoritative source of project and subsystem context.

The intended workflow is:

```text
User task
    ↓
AGENTS.md
    ↓
root README.md
    ↓
relevant subsystem README.md
    ↓
relevant implementation files
    ↓
task execution
    ↓
validation
    ↓
documentation update if required
```

The goal is that any capable AI model can enter the repository, discover the correct context, understand the scope of
the requested task, and work consistently without depending on instructions written for one specific AI product or
model.

```