# AI requirement mapping

| Requirement | Bootstrap location | Status |
| --- | --- | --- |
| Model profiles and generation parameters | `shared/model/ModelProfile.kt` | PARTIAL |
| Local runtime and inference adapter | `backend/runtime` | PARTIAL |
| Hardware detection | `backend/runtime/HardwareDetector.kt` | PARTIAL |
| Grounding and prompt roles | `backend/context`, `backend/prompt` | PARTIAL |
| Input/output sanitation and tool permission | `backend/security` | PARTIAL |
| Structured output validation | `backend/validation` | PARTIAL |
| Deterministic and judge evaluation | `backend/evaluation` | PLANNED |
| Runtime Markdown configuration | `.ai/` | PLANNED |
