# TASK-006: Runtime Markdown Agent and Skill Loader

**Status:** VERIFIED  
**Dependencies:** TASK-001  
**Milestone:** B — Base Plugin MVP

## Objective

Implement a validated runtime loader for Markdown-defined agents, skills, and reusable prompt instructions.

The Workbench must load its curated `.ai/` configuration into typed contracts without recompiling the plugin or treating
Markdown as executable code.

Development automation under `.codex/skills/` must remain completely separate from Workbench runtime instructions.

## Scope

### Runtime configuration sources

Load approved definitions from:

- `.ai/agents/`
- `.ai/skills/`
- Applicable curated prompt resources under `.ai/prompts/`

Support the existing default, coding, and judge agents, together with their referenced runtime skills.

Inspect the actual repository structure and existing Markdown conventions before introducing parsing rules.

Do not create a second, incompatible definition format.

### Agent definitions

Convert validated agent configuration into existing `AgentDefinition` and related shared contracts.

Preserve applicable information such as:

- Stable agent identifier and descriptive metadata.
- System or role instructions.
- Allowed runtime skills.
- Required model capabilities.
- Grounding policy references.
- Expected output format or schema.
- Declared tool permissions.
- Applicable execution restrictions.

Definitions may declare requested capabilities but must not authorize them independently.

The effective permissions must still be determined by the Workbench's programmatically enforced policies.

### Skill definitions

Load reusable Markdown workflows as declarative instructions.

Each skill must have an identifiable configuration and a validated relationship to an agent or supported execution
context.

The loader must distinguish instructions, examples, configuration metadata, and references to other approved resources
where the existing format supports them.

Skill content must not be interpreted as an operating-system command, executable script, or independent authorization to
access files or tools.

A reference to a missing or unauthorized skill must produce a deterministic validation failure.

### Validation and path safety

Implement validation for:

- Missing or malformed required metadata.
- Missing required instructions or sections.
- Blank or duplicate identifiers.
- Undeclared or unresolved skill references.
- Invalid agent-to-skill relationships.
- Unsupported configuration values.
- References outside approved runtime roots.
- Directory traversal and symbolic-link escape.
- Incorrect or unexpected resource types.

Use canonical filesystem-aware path checks.

Do not load `.codex/skills/`, unrelated project Markdown, or arbitrary files referenced inside instructions.

Markdown content is untrusted configuration data until validated against the application schema and execution policy.

### Runtime loading behavior

Configuration must be loaded from Markdown resources at runtime, not hard-coded into Kotlin classes.

Support explicit reloading of changed definitions without rebuilding the plugin.

A failed reload must not partially replace a previously validated configuration with inconsistent data.

Define predictable behavior for invalid configuration, missing resources, and duplicate identifiers.

Retain only the validated configuration and source information required for execution.

Do not retain unrelated filesystem contents or sensitive diagnostic data.

### Configuration provenance

Provide safe metadata identifying the configuration used for an execution, where applicable.

Relevant fields may include:

- Agent identifier.
- Skill identifiers.
- Configuration version or content fingerprint, when supported.
- Validation status.
- Safe source reference.

Avoid unnecessary absolute-path exposure or retention of sensitive configuration content in logs.

## Affected Files

Primary scope:

- `base/backend/**/agent/`
- Existing `PromptLoader.kt`
- Related shared agent and skill contracts
- Curated `.ai/` definitions, only if necessary for schema consistency
- Focused loader and validation tests

Preserve existing source-root and package conventions.

## Implementation Boundaries

- Do not execute Markdown content.
- Do not grant tool access through instruction text.
- Do not interpret agent instructions as changes to application security policy.
- Do not load Codex development skills as runtime configuration.
- Do not implement model inference or agent execution.
- Do not implement unrestricted dynamic plugin or script loading.
- Do not introduce cloud services or external AI applications.
- Do not modify unrelated architecture or remote Git branches.

Reuse existing Markdown parsing capabilities where suitable. New dependencies require a demonstrated need.

## Acceptance Criteria

TASK-006 is complete when:

1. The existing curated runtime agents and their referenced skills load successfully.
2. Loaded definitions map to typed shared contracts.
3. Agent instructions, skill bindings, capability requirements, output expectations, and declared policies are
   preserved.
4. Missing required metadata and malformed configurations fail predictably.
5. Duplicate identifiers and invalid skill references are rejected.
6. Traversal and symbolic-link escapes cannot extend the approved loading boundary.
7. `.codex/skills/` is excluded from runtime loading.
8. Markdown cannot independently grant permissions or trigger execution.
9. Changes to approved Markdown configuration can be loaded without recompilation.
10. Invalid reloads do not produce inconsistent partial configuration.
11. Focused tests cover valid definitions, malformed files, missing references, and safe reloading.
12. Required Gradle checks pass.

## Verification

Use repository fixtures and temporary malformed Markdown files.

Run focused loader tests, followed by:

```powershell
.\gradlew.bat check
```

A language model or inference runtime is not required for TASK-006 verification.

Record the actual tests executed, outcomes, and remaining limitations.

## Completion Record

**Implementation:** Validated runtime Markdown loading for curated agents and skills, safe fingerprints, and transactional reload are implemented.  
**Verification:** `./gradlew.bat test --tests "com.jaanik.integratedaiworkbench.backend.agent.RuntimeMarkdownLoaderTest"` (3 passed); `./gradlew.bat check` (PASS).  
**Final status:** VERIFIED

After implementation, record the affected files, supported definition format, verification results, outstanding issues,
and final status.

Set the task to `VERIFIED` only after all mandatory acceptance criteria are satisfied.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-006`.

The implementation skill must inspect the existing `.ai/` definitions and contracts before making changes, preserve the
separation between runtime configuration and development automation, and report verified results only.
