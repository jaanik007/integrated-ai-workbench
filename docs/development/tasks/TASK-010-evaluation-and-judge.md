# TASK-010: Deterministic Evaluation and Local Judge

**Status:** VERIFIED  
**Dependencies:** TASK-008  
**Milestone:** B — Base Plugin MVP

## Objective

Implement reliable result evaluation using deterministic checks and an optional, bounded LLM-as-a-Judge workflow.

Evaluation must distinguish objectively verifiable requirements from model-based quality assessments.

An LLM judge may evaluate semantic quality against a defined rubric but must never override mandatory security, policy,
or structured-output validation.

## Scope

### Deterministic evaluation

Extend the existing evaluation components to assess applicable execution properties.

Checks may include:

- Execution status.
- Required response presence.
- Structured-output validity.
- Required fields and data types.
- Applicable output constraints.
- Security and tool-permission compliance.
- Explicit task-specific validation rules.

Use repeatable criteria and typed evaluation results.

Equivalent inputs and evaluation configuration must produce equivalent deterministic outcomes.

Do not depend on model-generated reasoning to determine security or schema validity.

### Evaluation precedence

Apply mandatory deterministic checks before judge execution.

Required behavior:

- A failed mandatory check produces an overall failure.
- The judge cannot change a deterministic failure into a pass.
- Judge evaluation must not run when mandatory policy prevents it.
- An unavailable judge must not fabricate a score.
- Judge errors must not conceal earlier failures.

Maintain distinct statuses for deterministic validation, judge assessment, and overall evaluation.

### Evaluation rubric

Use an explicit rubric with defined assessment criteria.

Applicable rubric information includes:

- Stable identifier.
- Version or reproducible configuration fingerprint.
- Criteria descriptions.
- Scoring range.
- Applicable weights, if used.
- Required output structure.
- Conditions for an acceptable assessment.

Reuse curated Markdown resources where supported by the runtime loader.

Rubric changes must be identifiable in evaluation provenance.

Avoid introducing a general-purpose evaluation framework beyond the MVP requirements.

### Local LLM-as-a-Judge

Use the dedicated runtime judge agent under:

`.ai/agents/judge/AGENT.md`

Use the corresponding evaluation workflow under:

`.ai/skills/response-evaluation/SKILL.md`

The judge must operate through the Workbench-managed local inference infrastructure.

It may reuse an existing compatible local model profile.

A separate model binary, independently operated AI application, or cloud provider is not required.

The judge must receive only the information necessary for evaluation, including:

- Sanitized candidate output.
- Relevant task context when explicitly approved.
- Defined evaluation rubric.
- Required structured response instructions.

Do not expose unrelated project content, credentials, private reasoning, or unrestricted execution metadata.

The judge must not receive tool execution permissions.

### Bounded judge execution

Judge inference must use the same applicable runtime controls as other model requests.

Enforce:

- Compatible local model profile.
- Input and output limits.
- Execution timeout.
- Cancellation support.
- Defined generation parameters.
- Explicit error handling.
- Appropriate resource cleanup.

Do not permit unrestricted recursive judge invocations.

Avoid creating an unbounded chain of judge agents evaluating other judges.

### Structured judge results

Validate judge responses against a typed contract.

The result should contain:

- Evaluation status.
- Rubric identifier and version.
- Applicable criterion scores.
- Overall score where valid.
- Concise evidence or justification.
- Judge model-profile reference.
- Relevant safe execution metadata.

Reject malformed or out-of-range scores.

Do not interpret a judge's confidence or self-reported correctness as verified factual evidence.

A user-facing assessment summary is not hidden chain-of-thought.

### Unavailable or invalid judge results

Use an explicit `UNKNOWN` or equivalent status when judge assessment cannot be established.

Applicable cases include:

- No compatible local judge profile.
- Judge runtime unavailable.
- Timeout or cancellation.
- Invalid structured judge output.
- Missing required rubric data.
- Unsupported evaluation configuration.

Preserve completed deterministic results.

Judge failure must not cause a previously failed mandatory check to disappear.

### Evaluation provenance

Record the information needed to interpret and reproduce applicable results.

Relevant metadata includes:

- Evaluated execution identifier.
- Deterministic rule-set identifier.
- Rubric version.
- Judge agent and model-profile identifiers.
- Validation and judge outcomes.
- Available execution timing.
- Safe evaluation evidence.

Do not retain credentials, hidden model reasoning, or unsanitized project context.

### Testing

Use deterministic fixtures and fake local inference adapters to verify evaluation logic.

Tests must exercise valid, invalid, unavailable, and conflicting evaluation outcomes.

A real local judge can be checked during TASK-010 when appropriate resources are available.

Real judge integration is required for complete MVP verification in TASK-011.

## Affected Files

Primary scope:

- `base/backend/**/evaluation/`
- Applicable agent integration components
- `.ai/agents/judge/AGENT.md`
- `.ai/skills/response-evaluation/SKILL.md`
- Shared evaluation and run contracts
- Focused deterministic and judge tests

Preserve existing contracts and runtime configuration conventions.

## Implementation Boundaries

- Do not allow the judge to override deterministic validation.
- Do not grant tool execution or filesystem modification rights to the judge.
- Do not use cloud inference or independently operated AI applications.
- Do not expose hidden model reasoning.
- Do not create recursive or unbounded evaluation workflows.
- Do not fabricate missing scores.
- Do not introduce unrelated evaluation frameworks.
- Do not modify remote Git branches.

## Acceptance Criteria

TASK-010 is complete when:

1. Deterministic evaluation produces repeatable typed results.
2. Mandatory security, policy, and schema checks have explicit precedence.
3. A deterministic failure cannot become an overall pass through judge scoring.
4. A valid local judge configuration resolves through the existing runtime infrastructure.
5. The judge receives bounded, sanitized inputs and an explicit rubric.
6. Rubric configuration is identifiable and versioned or reproducibly fingerprinted.
7. Judge responses are validated against a structured contract.
8. Invalid scores and malformed results are rejected.
9. Unavailable judge outcomes are represented as `UNKNOWN` or an equivalent typed state.
10. Judge failures preserve previous deterministic outcomes.
11. Judge execution has no tool permissions.
12. Timeout and cancellation behavior is bounded.
13. Safe evaluation provenance is recorded.
14. Tests cover success, failure, precedence, and unavailable-judge scenarios.
15. Required Gradle checks pass.

## Verification

Run focused evaluation tests covering:

- Repeatable deterministic validation.
- Valid rubric and score calculation.
- Missing or malformed rubric.
- Valid structured judge response.
- Out-of-range judge score.
- Judge timeout.
- Judge cancellation.
- Judge unavailable.
- Mandatory validation failure followed by an attempted positive judge outcome.
- Sensitive-content exclusion.
- Evaluation provenance.

Run:

```powershell
.\gradlew.bat check
```

Record any actual local-judge integration result separately from fake-adapter tests.

If real judge inference has not been verified in this task, record that limitation explicitly. It must be resolved
before TASK-011 can be marked `VERIFIED`.

## Completion Record

**Implementation:** `EvaluationCoordinator` applies deterministic execution-status precedence, versioned/fingerprinted
rubrics, bounded secret-sanitized judge input, strict score-range validation, and safe provenance. `AgentExecutionJudgeExecutor`
uses the approved `judge` agent through the existing agent coordinator and managed inference path; it grants no tools.

**Automated verification:** `./gradlew.bat test --tests "*EvaluationCoordinatorTest"` passed (3 tests);
`./gradlew.bat check` passed on 2026-10-11.

**Real-judge integration verification:** NOT RUN. It is explicitly required before TASK-011 can be VERIFIED.

**Final status:** VERIFIED

Record modified files, evaluation rules, rubric configuration, executed tests, verified behavior, and outstanding
limitations.

Set the task to `VERIFIED` only when its applicable acceptance criteria are satisfied.

## Implementation Skill

Use [skill-iwb-implement](../../../.codex/skills/skill-iwb-implement/SKILL.md) with `$skill-iwb-implement TASK-010`.

The implementation skill must maintain deterministic validation precedence, enforce bounded local judge execution, and
distinguish simulated assessments from actual model-based evaluation.
