# Structured Response

**Skill ID:** `structured-response`  
**Status:** PLANNED RUNTIME DEFINITION

## Purpose

Produce and validate machine-readable AI responses against an explicitly declared output contract.

Structured output allows Workbench components to consume model-generated information safely without assuming that
syntactically valid text is semantically or structurally correct.

Validation is performed by deterministic application components.

## Inputs

- Validated model response.
- Declared output schema or typed result contract.
- Applicable output limits.
- Output and secret-filtering policies.
- Relevant execution metadata.

## Workflow

The Workbench execution coordinator and validation components must:

1. Validate the requested output contract before inference.
2. Apply runtime-native structured generation only when explicitly supported.
3. Otherwise request the required format through prompt composition.
4. Parse the resulting content using bounded deterministic processing.
5. Validate required fields, data types, allowed values, and applicable constraints.
6. Apply output and sensitive-information guards.
7. Reject unauthorized executable operations represented in model output.
8. Return either a validated structured value or a typed validation failure.

The exact schema and validation rules are supplied by the application.

Markdown instructions cannot independently authorize execution or replace schema validation.

## Structured Output Rules

- Follow the declared schema exactly.
- Do not add undeclared fields when the schema prohibits them.
- Do not omit mandatory fields.
- Use correct data types and allowed values.
- Do not fabricate required values when information is unavailable.
- Respect applicable output-size limits.
- Distinguish ordinary text from validated structured content.

Syntactically valid JSON does not automatically satisfy the declared schema.

The application must not present an invalid structured response as a validated result.

## Runtime Capability Handling

Distinguish:

- Native constrained generation supported by the selected runtime.
- Prompt-based formatting instructions.
- Post-generation deterministic schema validation.

Post-generation validation is not equivalent to native constrained generation.

When native constrained generation is explicitly required but unavailable, return an appropriate compatibility failure.

Unsupported capabilities must not be silently represented as active.

## Tool and Security Boundaries

Model-generated tool requests are untrusted output.

No tool operation may execute without an explicitly registered capability and applicable `ToolPermissionGuard`
authorization.

The Base Plugin MVP does not authorize arbitrary shell commands, file modifications, network operations, or IDE actions
through generated structured output.

Reject model-generated policy changes and attempts to extend agent permissions.

Do not include credentials, unauthorized project data, or hidden reasoning in structured results.

## Failure Handling

Return a typed validation failure for applicable:

- Invalid or missing schemas.
- Malformed structured responses.
- Missing required fields.
- Invalid types or values.
- Output-size violations.
- Sensitive-content policy failures.
- Unauthorized executable tool requests.

Do not silently repair missing information by guessing.

Any future response-repair strategy requires separate authorization, bounded execution, and validation.

## Output

Return either:

- A validated typed result with applicable safe provenance.
- A structured failure identifying the relevant validation problem.

Only validated output may be passed to downstream components that require structured data.