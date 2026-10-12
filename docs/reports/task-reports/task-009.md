# TASK-009 — Tool Window Workflow

**Status:** BLOCKED **Date:** 2026-10-11 **Execution:** BLOCKED

**Implemented:** Fail-closed Workbench MCP STDIO client, discovery, target verification, policy dispatch, model continuation, and Tool Window coordinator wiring. **Files:** scoped TASK-009 backend, frontend, runtime definitions, and tests.
**Supporting skills:** skill-iwb-implement.

**Compilation:** PASS — `compileKotlin` completed with the existing user Gradle cache. **Focused tests:** PASS — 13 agent, MCP, Markdown, and llama.cpp adapter tests completed with no failures. **Gradle checks:** PASS — `check` completed successfully after the inference-adapter correction. **Runtime/UI:** `runIde` previously loaded the plugin, but no Tool Window was observable. On 2026-10-11 the available computer-use bridge returned `apps: []` and `browsers: []`; its Windows app launcher is unavailable, so the installed IntelliJ 2026.2.1 plugin cannot be physically operated from this session.

**Acceptance:** not verified. **MCP accessibility:** OpenCode independently connected through the configured IntelliJ STDIO transport, discovered tools, and executed `get_project_modules` for `integrated-ai-workbench`; this is server-only evidence. **Workbench integration:** PASS — the opt-in real test completed a Workbench-managed local model → authorized IntelliJ `get_project_modules` → model continuation → validated response cycle in 327.772 seconds. **Open issue:** a user-visible Development Instance Tool Window interaction with a registered local profile and MCP target remains required before TASK-009 can become `VERIFIED`; attach its Windows desktop to this session to enable the check.
