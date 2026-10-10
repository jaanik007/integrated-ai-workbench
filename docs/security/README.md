# Security architecture

Status: PARTIAL

Implemented bootstrap safeguards are `InputGuard`, `SecretSanitizer`, `OutputGuard`, `ToolPermissionGuard` and
`StructuredResponseValidator`. They are independently callable skeleton components and are not yet connected to a
model-execution flow.

PLANNED enforcement includes allowed-path traversal, binary-file exclusion, configured grounding limits, persistent
secret storage through PasswordSafe, runtime timeouts, policy loading and execution provenance.
