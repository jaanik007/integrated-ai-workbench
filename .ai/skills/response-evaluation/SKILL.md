# Response Evaluation

**Skill ID:** `response-evaluation`  
**Status:** PLANNED RUNTIME DEFINITION

## Purpose

Evaluate AI-generated responses using deterministic validation and optional local LLM-as-a-Judge assessment.

The workflow distinguishes enforceable correctness and security checks from model-based semantic quality judgments.

Deterministic validation remains authoritative.

## Inputs

- Existing candidate response.
- Applicable execution and validation metadata.
- Deterministic evaluation rules.
- Optional explicit evaluation rubric.
- Optional compatible local judge profile.
- Applicable evaluation limits and policies.

## Workflow

The Workbench evaluation components must:

1. Verify the candidate response and applicable execution status.
2. Run deterministic security, policy, and structured-output checks.
3. Record validation results and supporting evidence.
4. Stop evaluation when a mandatory deterministic check fails.
5. When explicitly requested, validate the rubric and compatible local judge configuration.
6. Send only sanitized, bounded evaluation inputs to the selected judge agent.
7. Execute judge inference through the Workbench-managed local runtime.
8. Validate the judge's structured response against the evaluation schema.
9. Return a typed `EvaluationResult` with safe provenance.

The evaluation coordinator performs these operations. The Markdown skill does not independently invoke models or execute
tools.

## Deterministic Evaluation

Applicable checks include:

- Required output structure.
- Schema and field validation.
- Execution outcome.
- Output limits.
- Security-policy compliance.
- Unauthorized tool requests.
- Explicit task-specific assertions.

A failed mandatory deterministic check cannot become a passing result through judge scoring.

## LLM-as-a-Judge

The judge may assess rubric-defined semantic properties such as:

- Relevance.
- Completeness.
- Clarity.
- Consistency with explicitly supplied context.
- Other configured assessment criteria.

The judge must use only the supplied rubric and authorized evaluation context.

Judge scores are model-generated assessments, not independent proof of factual correctness.

The judge cannot change security decisions, grant permissions, repair failed schemas, or execute remediation.

## Result Handling

Return a typed `EvaluationResult` containing applicable:

- Deterministic validation status.
- Rubric identifier and version.
- Criterion scores.
- Overall score when valid.
- Concise evaluation evidence.
- Judge model-profile reference.
- Safe execution provenance.

If the judge is unavailable, cancelled, times out, or returns malformed output, record the judge assessment as
`UNKNOWN`.

Preserve existing deterministic validation results.

A judge failure must not fabricate a passing score or conceal a previously established failure.

## Security

- Use only approved and sanitized candidate content.
- Do not expose hidden reasoning or private system instructions.
- Do not provide the judge with unrestricted project context.
- Do not grant shell, filesystem, network, Git, or IDE tools.
- Do not permit recursive or unbounded judge execution.
- Enforce timeouts, input limits, and output limits through application components.

A valid evaluation result requires successful validation of the applicable assessment contract.