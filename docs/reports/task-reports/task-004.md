# TASK-004 — Managed Runtime Lifecycle

**Status:** VERIFIED  
**Date:** 2026-10-10  
**Execution:** COMPLETED

**Implemented:** Workbench-owned llama-server lifecycle, hash-gated configuration, provider-neutral state manager, fake adapter, and opt-in real smoke test.  
**Files:** Runtime adapters, focused tests, and `LlamaServerRealSmokeTest`.  
**Supporting skills:** skill-iwb-implement.

**Compilation:** `./gradlew.bat compileKotlin` — PASS (2026-10-10).  
**Tests:** `./gradlew.bat test --tests "*LifecycleRuntimeManagerTest" --tests "*LlamaServerAdapterTest"` — PASS (4 tests, 2026-10-10).  
**Gradle checks:** `./gradlew.bat check` — PASS (2026-10-10).  
**Runtime/UI:** PASS. Official b11429 Windows x64 CPU `llama-server.exe`; build 11429, commit `d81235049`, SHA-256 `7E236243C9B670C6A49410C03186FD575C57A0EFE3FD8A21D8727503D571D5B6`; CPU DLL dependencies present. The readable 2,497,281,120-byte `GGUF` model was used unchanged. Workbench started a CPU process on an available `127.0.0.1` port, passed readiness and `/health`, reached `MODEL_LOADED`, shut down, lost health access, and left no process. Original ZIP unavailable for separate hash validation.

**Acceptance:** 15/15 mandatory criteria verified.  
**Open issues:** None for TASK-004; original ZIP archive hash was not separately available.  
**Next:** TASK-005 is ready for real local-inference verification.
