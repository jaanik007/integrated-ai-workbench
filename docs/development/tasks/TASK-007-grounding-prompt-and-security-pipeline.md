# TASK-007: Grounding, Prompt, and Security Pipeline

**Status:** VERIFIED  
**Dependencies:** TASK-001, TASK-006  
**Milestone:** B — Base Plugin MVP

## Objective

Connect the existing context, prompting, and security components into a deterministic pipeline that produces validated,
bounded prompt content for an approved agent request.

The pipeline must separate trusted application instructions from user input and project grounding, enforce file-access
restrictions, and prevent agent instructions from overriding application permissions.

Model inference and agent execution remain outside this task.

## Scope

### Controlled project grounding

Use the existing context services and shared contracts to collect explicitly approved project content.

Required controls include:

- Approved project roots.
- Canonical path containment.
- Path traversal prevention.
- Symbolic-link target validation.
- Regular-file and supported-text-file checks.
- Binary-file exclusion.
- Maximum file count.
- Per-file size limits.
- Overall context size limits.
- Deterministic handling of unreadable or missing files.

Do not recursively ingest the entire project by default.

Exclude sensitive or irrelevant content through configured policies, including appropriate treatment of version-control
metadata, generated files, binary artifacts, and known credential locations.

Filesystem access must be authorized by application policy rather than inferred from model instructions.

Grounding must be bounded before content is passed to a model.

### Grounding provenance

Return typed information about the selected context and its validation status.

Preserve applicable metadata such as:

- Approved source reference.
- Included or omitted status.
- Omission reason.
- Relevant file or context limits.
- Safe content provenance.

Avoid exposing unnecessary absolute paths, secrets, or excluded file contents.

A source listed as included must correspond to content actually collected and made available for prompt composition.

### Prompt composition

Extend the existing prompt components to construct ordered, typed prompt parts.

Preserve distinct categories for:

- System instructions.
- Agent or task instructions.
- User-provided input.
- Approved grounding context.
- Explicit one-shot or few-shot examples.

Do not merge all content into an unstructured string before the application has applied the relevant policies.

The eventual inference adapter may serialize these parts according to supported runtime message roles, but the shared
representation must preserve their origins and intended precedence.

Project files, retrieved context, and user-supplied examples are untrusted content. Instructions embedded in such
material must not acquire system-level authority.

### Example-based prompting

Support zero-shot, one-shot, and few-shot prompt composition.

Examples must be:

- Explicitly configured or approved.
- Associated with the applicable agent or skill.
- Bounded by count and context size.
- Represented separately from the current user request.
- Subject to applicable input and secret-handling policies.

Use consistent formatting so example-guided and zero-shot requests can be compared without changing unrelated execution
configuration.

Do not require an inference runtime to validate prompt assembly.

### Input security

Integrate the existing `InputGuard` with the prompt preparation process.

Validate applicable input properties, including:

- Required input presence.
- Input size limits.
- Supported content types.
- Invalid or disallowed request configurations.
- Applicable agent and context restrictions.

Reject invalid requests before model execution.

### Secret protection

Integrate `SecretSanitizer` into the context and prompt preparation boundary.

Prevent known sensitive values from entering assembled model input unless explicitly permitted by a higher-level
application policy.

Sanitization must be applied to the actual text sent forward, not merely to diagnostic previews.

Define deterministic handling for sensitive content that cannot be safely processed.

Secret detection is a protective control, not a guarantee that every possible credential pattern can be recognized.

Do not log unfiltered prompts or context during rejection handling.

### Output and tool security boundaries

Provide or extend the reusable output and permission checks required by downstream execution.

`OutputGuard` must support applicable post-generation validation and sanitization.

`ToolPermissionGuard` must support deterministic evaluation of proposed tool operations against explicit policy.

TASK-007 does not execute model-generated tool requests.

The eventual coordinator in TASK-008 is responsible for invoking the appropriate post-generation guards at execution
time.

Do not report an output guard as operational in a complete model workflow until that integration has been verified.

### Prompt-injection boundaries

Treat instructions found in project files, Markdown examples, and retrieved content as untrusted data.

The implementation must prevent such content from directly:

- Changing application policy.
- Expanding approved filesystem roots.
- Granting tool permissions.
- Executing commands.
- Overriding deterministic validation.

Prompt formatting alone cannot guarantee that a language model will ignore malicious text.

The enforceable security boundary must remain in Kotlin services and policy checks.

### Failure handling

Return typed, actionable failures for:

- Unauthorized paths.
- Invalid context references.
- Unsupported files.
- Context limit violations.
- Invalid prompt composition.
- Sensitive-content policy rejection.
- Unauthorized requested operations.

Failures must not silently broaden project access or drop mandatory security checks.

## Affected Files

Primary scope:

- `base/backend/**/context/`
- `base/backend/**/prompt/`
- `base/backend/**/security/`
- Relevant shared grounding and policy contracts
- Focused tests for grounding, prompting, and security

Reuse existing components wherever they meet the requirements.

## Implementation Boundaries

- Do not invoke a language model.
- Do not implement agent orchestration.
- Do not execute tools or shell commands.
- Do not grant authority through Markdown or prompt content.
- Do not weaken path checks to accommodate invalid resources.
- Do not add cloud inference or independently operated AI applications.
- Do not duplicate the runtime loader from TASK-006.
- Do not implement the coordinator assigned to TASK-008.
- Do not modify remote Git branches.

## Acceptance Criteria

TASK-007 is complete when:

1. Grounding operates only on explicitly approved files and roots.
2. Canonical validation prevents traversal and supported symbolic-link escapes.
3. Binary files and unsupported content are excluded safely.
4. File-count, file-size, and overall context limits are enforced.
5. Grounding results contain accurate, safe provenance and omission information.
6. System, task, context, user, and example content retain distinct typed origins.
7. Zero-shot, one-shot, and few-shot composition is supported with explicit limits.
8. Input and secret controls operate on the actual content passed downstream.
9. Output and tool-permission guards provide deterministic checks for later coordinator integration.
10. Untrusted content cannot directly change application permissions or filesystem boundaries.
11. Failures are typed, actionable, and fail closed where policy requires.
12. Tests cover valid and malicious input, file boundaries, sanitization, example limits, and permission rejection.
13. Required Gradle checks pass.

## Verification

Run focused tests using controlled filesystem and prompt fixtures.

Required scenarios include:

- Approved and rejected context files.
- Traversal and symbolic-link escapes.
- Binary and oversized content.
- Context budget exhaustion.
- Secret masking and rejection.
- Separation of prompt roles.
- Zero-shot and bounded example-guided prompting.
- Prompt-injection instructions embedded in project content.
- Unauthorized tool requests.
- Output-guard validation behavior.

Run:

```powershell
.\gradlew.bat check
```

Prompt-injection tests verify deterministic application boundaries; they do not prove that a model cannot be
semantically influenced by malicious text.

Record only completed checks and observed outcomes.

## Completion Record

**Implementation:** Bounded approved-root grounding, sanitization, provenance, typed prompt preparation, example limits, and downstream guard boundaries are implemented.  
**Verification:** focused grounding and prompt preparation tests (4 passed); `./gradlew.bat check` (PASS).  
**Final status:** VERIFIED

Record modified files, enforced policies, completed tests, unresolved limitations, and final status after
implementation.

Set the task to `VERIFIED` only when its acceptance criteria are satisfied.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-007`.

The implementation skill must preserve programmatic security enforcement and distinguish reusable guard implementation
from later integration into actual model execution.
