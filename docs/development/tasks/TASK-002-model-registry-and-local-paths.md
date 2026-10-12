# TASK-002: Model Registry and Local Model-Path Validation

**Status:** VERIFIED  
**Dependencies:** TASK-001  
**Milestone:** B — Base Plugin MVP

## Objective

Implement a validated registry for user-supplied local language models and their associated model profiles.

Replace the bootstrap-only in-memory registry behavior with reliable model registration, discovery, metadata management,
and configuration persistence.

The registry must preserve user ownership of model files, enforce explicit file-access boundaries, and provide validated
model references for subsequent hardware compatibility and runtime selection.

Model registration does not imply that a model is compatible with an available inference runtime.

## Scope

### Model registry

Extend the existing `ModelRegistry` and related components to support:

- Registration of user-selected local model files.
- Explicit discovery within approved directories.
- Retrieval and enumeration of registered models.
- Updates to model metadata and references.
- Removal of registry entries without deleting model files.
- Stable, unique model identifiers.
- Relationships between model entries and their profiles.
- Predictable validation and error reporting.

Reuse the existing shared contracts established in TASK-001.

Avoid duplicate registry implementations and unnecessary abstractions.

### Local model-path validation

All model references must resolve to explicitly approved local filesystem locations.

Support approval through:

- A user-selected individual model file.
- A user-selected directory containing model files.

Required validation includes:

- Absolute, normalized filesystem paths.
- Canonical path resolution for existing files.
- File existence and readability.
- Regular-file validation.
- Approved-root or explicitly approved-file membership.
- Path traversal prevention.
- Symbolic-link target validation.
- Rejection of unauthorized or unsupported file types.

Use filesystem-aware path operations rather than string-prefix comparisons.

A symbolic link must not bypass the approved access boundary.

Paths must be revalidated before model loading, since filesystem contents and permissions may change after registration.

Do not automatically approve arbitrary parent directories or expand access beyond the user's selection.

### Model discovery

Discovery must be explicitly initiated and restricted to approved locations.

It must:

- Avoid scanning entire drives or unrelated project directories.
- Apply bounded traversal and configurable discovery limits.
- Exclude irrelevant, unsupported, or unsafe file types.
- Avoid reading or loading model contents during discovery.
- Return safe metadata and identifiable validation failures.
- Handle inaccessible files and directories predictably.

Discovery must not run automatically during routine Tool Window rendering.

### Model metadata

Registered models should expose the metadata required for subsequent processing, including:

- Stable model identifier.
- Display name.
- Validated local file reference.
- File size where available.
- Candidate model format.
- Associated model profiles.
- Registration and validation status where applicable.

File extensions and lightweight filesystem metadata may identify candidate formats but do not establish runtime
compatibility.

Actual model-format support, hardware compatibility, and runtime requirements are evaluated in TASK-003 and TASK-004.

Do not introduce binary parsing solely to populate the registry.

### Configuration persistence

Persist registered model references and profile associations through an appropriate IntelliJ Platform configuration
mechanism.

The implementation must:

- Define whether registry state is application-scoped or project-scoped.
- Preserve registered entries across the relevant IDE lifecycle.
- Restore configuration using validated, typed contracts.
- Handle missing or relocated model files.
- Reject invalid or inconsistent persisted entries predictably.
- Avoid storing model binary contents or credentials.
- Keep user-specific filesystem configuration outside version-controlled project files by default.

Select the simplest persistence mechanism compatible with the existing plugin architecture.

### Registry consistency

Enforce deterministic rules for:

- Duplicate model identifiers.
- Duplicate or conflicting registrations.
- References to nonexistent model entries.
- Profile-to-model association consistency.
- Updates that invalidate existing relationships.
- Removal of models referenced by active profiles.

Deletion of a registry entry must not delete the corresponding user-owned model file.

The implementation must explicitly define whether removal of an associated model is rejected or requires controlled
removal of its profile associations.

## Affected Files

Primary scope:

- `base/backend/**/model/`
- `base/shared/**/model/`
- Existing configuration persistence boundaries.
- Focused backend and shared tests in the corresponding test source roots.

Inspect existing classes and package declarations before modifying files.

Preserve the single Gradle module and established source-root boundaries.

## Implementation Boundaries

- Do not download, bundle, copy, modify, or delete model binaries.
- Do not scan arbitrary drives or unrelated directories.
- Do not introduce a model inference engine or runtime process.
- Do not add LM Studio, Ollama, cloud providers, or external AI applications.
- Do not implement hardware detection or runtime compatibility matching.
- Do not parse model binaries during ordinary registry operations.
- Do not access unapproved filesystem locations.
- Do not persist credentials or sensitive runtime arguments.
- Do not expose model contents or sensitive filesystem details in logs.
- Do not add unnecessary UI functionality.
- Do not modify unrelated architecture or remote Git branches.

Additional dependencies require a documented technical justification.

## Acceptance Criteria

TASK-002 is complete when:

1. Models can be registered, retrieved, updated, listed, and removed through validated registry operations.
2. Model identifiers are unique and profile-to-model relationships remain consistent.
3. Registered model references resolve to approved local files only.
4. Missing, unreadable, unsupported, or unauthorized files are rejected predictably.
5. Path traversal and symbolic-link escapes cannot bypass approved filesystem boundaries.
6. Discovery operates only within explicitly approved locations and respects defined limits.
7. Registry operations expose safe metadata without loading model contents.
8. Registry configuration survives the relevant IDE lifecycle and handles invalid persisted references safely.
9. Removing a registry entry never deletes the user-owned model file.
10. Invalid registry operations produce actionable errors without falling back to cloud services or external inference
    applications.
11. Tests cover valid registrations, conflicting identifiers, invalid paths, symbolic links, persistence, and profile
    associations.
12. Existing bootstrap functionality remains compatible and the required Gradle checks pass.

Compilation alone is insufficient for task completion.

## Verification

Run focused model-registry and filesystem-validation tests, followed by:

```powershell
.\gradlew.bat check
```

Use temporary directories and lightweight fixture files.

A real language-model binary is not required for registry unit tests.

Verification must cover:

- Successful registration and lookup.
- Duplicate and conflicting identifiers.
- Profile association validation.
- Missing and inaccessible files.
- Unsupported file types.
- Relative paths and traversal attempts.
- Symbolic links within and outside approved locations.
- Approved-file and approved-directory boundaries.
- Bounded discovery behavior.
- Persistence and restoration.
- Missing model files after restoration.
- Removal without filesystem deletion.
- Compatibility with TASK-001 contracts.

Record only commands actually executed and their verified results.

Where a platform-specific filesystem test cannot run, report the limitation rather than treating it as passed.

## Completion Record

**Implementation:** Validated registry operations, canonical local-path enforcement, bounded discovery, profile associations, and validated persisted-state restoration are implemented.  
**Verification:** `./gradlew.bat test --tests "com.jaanik.integratedaiworkbench.backend.model.ModelRegistryTest"` (6 passed); `./gradlew.bat check` (PASS).  
**Final status:** VERIFIED

After implementation, record:

- Modified and added files.
- Registry and persistence decisions.
- Tests executed and their outcomes.
- Acceptance criteria satisfied.
- Remaining limitations.
- Final task status.

Mark TASK-002 `VERIFIED` only when its acceptance criteria and required verification checks are satisfied.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-002`.

The corresponding Codex skill must inspect the existing registry and shared contracts before implementation, preserve
established architecture, and report only verified results.
