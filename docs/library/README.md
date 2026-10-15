# Integrated AI Workbench — Capabilities implementation

This bundle replaces the previous **session-only** `../../../../Downloads/SettingsPanelCapabilitiesTab.kt` and adds `WorkbenchLibrary.kt` to the same Kotlin package:

`com.jaanik.integratedaiworkbench.frontend.tabs`

## Gradle prerequisite

The persistent, lazily initialized SQLite catalog needs a packaged SQLite JDBC implementation. Add to the existing module's `dependencies` (adapt to Groovy/Kotlin DSL):

```kotlin
implementation("org.xerial:sqlite-jdbc:3.49.1.0")
```

Check plugin packaging and classloader resolution in the actual IntelliJ Gradle run configuration. Do not rely on a SQLite JDBC jar merely being present in the IDE distribution. SQLite is embedded; no separate database server is needed.

## Data layout

- Global metadata: `<IDE config>/integrated-ai-workbench/library.sqlite`
- Global immutable documents: `<IDE config>/integrated-ai-workbench/documents/<uuid>.md`
- Project immutable documents: `<project>/.ai-workbench/library/<uuid>.md` with `.properties` metadata sidecars.
- Project roles: `<project>/.ai-workbench/roles/<uuid>.properties`, mirrored from the SQLite catalog.
- Existing project `AGENTS.md` files: imported as independent snapshots; originals are not overwritten.

Database creation happens upon **Sources** first access, first role creation, or first document write. Opening the Studio and listing empty roles does not create the database.

## Features implemented

- Persistent role creation, editing, copying, deletion, and document assignments.
- Role name, purpose, responsibilities/scope, global/project storage association.
- Dynamic categories (no compulsory SKILL/TOOL/AGENT/SPEC types).
- New documents, disk imports, AGENTS.md import, read-only viewing, copy-and-modify, guarded delete.
- Immutable document content with SHA-256 integrity verification.
- SQLite activity events for changes and source scan operations (not a full observability engine).
- Opt-in scans of known local skills directories; manual export adapter status for IntelliJ integrations.
- Existing `context.workbench.agents(defaultModelProfileId)` integration preserved.
- No policy toggles, fake permissions, or duplicate configuration.

## Not implemented / not claimed

- The rest of the plugin's Workflow authorization, Overview Security or Observability modules.
- Automatic export from private JetBrains AI Assistant/MCP/Database settings. Public adapters are required.
- Automatic activation of imported tools or execution of skills.
- Automatic project file `AGENTS.md` overwrite, runtime agent registration, remote synchronization or Git commits.
- Full Gradle compile verification, since other project sources and build files have not been supplied.

## Acceptance checks in the actual plugin repository

1. Add the two files to the existing `frontend/tabs` package. Remove the old `../../../../Downloads/SettingsPanelCapabilitiesTab.kt` first.
2. Add the SQLite JDBC dependency to the relevant Gradle module.
3. Run `./gradlew compileKotlin` (or the project's actual Kotlin compile task) and `./gradlew test`.
4. Open Capabilities: existing roles should show if the DB exists; on fresh install the DB should not exist yet.
5. Open Sources: SQLite file should be initialized automatically. Run Scan; unavailable locations must remain NOT_FOUND.
6. Create a global role and a project role; restart IDE and confirm persistence.
7. Create a document and assign it to a role; restart IDE and confirm association.
8. Copy and change the document; ensure source document SHA-256/content is unchanged.
9. Attempt deleting an assigned document: operation must fail safely.
10. Check that role assignment alone never grants tool or terminal access.

## Known production hardening before public release

- Database migration/versioning for future schema versions and startup recovery/backups.
- Input file encoding checks and protection against import races (symlink swap and huge-file races).
- UI task cancellation/disposal; progress display and project closing during background tasks.
- Reconciliation/re-indexing of project metadata and documents when repositories are moved or cloned (the project files are retained, but cross-machine import is not yet automatic).
- Resource-level permissions for multiple projects and shared global roles.
- Build verification against the exact IntelliJ Platform SDK and Kotlin plugin versions.
