package com.jaanik.integratedaiworkbench.backend.evaluation

import com.jaanik.integratedaiworkbench.backend.agent.AgentExecutionService
import com.jaanik.integratedaiworkbench.backend.agent.RuntimeMarkdownLoader
import com.jaanik.integratedaiworkbench.backend.context.FileSystemContextService
import com.jaanik.integratedaiworkbench.backend.model.InMemoryModelRegistry
import com.jaanik.integratedaiworkbench.backend.model.ModelPathValidator
import com.jaanik.integratedaiworkbench.backend.prompt.PromptPreparationService
import com.jaanik.integratedaiworkbench.backend.runtime.JvmHardwareDetector
import com.jaanik.integratedaiworkbench.backend.runtime.LifecycleRuntimeManager
import com.jaanik.integratedaiworkbench.backend.runtime.LlamaServerAdapter
import com.jaanik.integratedaiworkbench.backend.runtime.LlamaServerConfiguration
import com.jaanik.integratedaiworkbench.backend.runtime.LocalInferenceService
import com.jaanik.integratedaiworkbench.backend.runtime.RuntimeCompatibilityMatcher
import com.jaanik.integratedaiworkbench.backend.security.InputGuard
import com.jaanik.integratedaiworkbench.shared.evaluation.EvaluationStatus
import com.jaanik.integratedaiworkbench.shared.evaluation.JudgeStatus
import com.jaanik.integratedaiworkbench.shared.model.ExecutionPolicy
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus
import com.jaanik.integratedaiworkbench.shared.run.AgentExecutionOutcome
import com.jaanik.integratedaiworkbench.shared.run.AgentExecutionStatus
import com.jaanik.integratedaiworkbench.shared.run.RunMetadata
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Opt-in proof that the judge uses a real Workbench-managed local model and returns a validated assessment. */
class EvaluationRealJudgeIntegrationTest {
    @Test fun `evaluates a sanitized candidate through the managed local judge`() {
        if (setting("iwb.realJudge", "IWB_REAL_JUDGE") != "true") return
        val executable = Path.of(required("iwb.runtime.executable", "IWB_RUNTIME_EXECUTABLE"))
        val modelPath = Path.of(required("iwb.runtime.model", "IWB_RUNTIME_MODEL")).toRealPath()
        val validator = ModelPathValidator.fromSelections(listOf(modelPath), emptyList())
        val registry = InMemoryModelRegistry(validator)
        val model = registry.register(modelPath, modelId = "real-judge-model")
        val profile = ModelProfile(
            id = "real-judge-profile", displayName = "Real judge profile", providerId = "llama-cpp-server",
            modelReference = model.canonicalPath, timeout = Duration.ofSeconds(180), temperature = 0.0,
            topP = 1.0, maxOutputTokens = 256, structuredOutputSchema = JUDGE_SCHEMA,
        )
        registry.associate(profile, model.id)
        val inference = LocalInferenceService(
            registry, validator, RuntimeCompatibilityMatcher(), JvmHardwareDetector(),
            LifecycleRuntimeManager(LlamaServerAdapter(LlamaServerConfiguration(
                executablePath = executable, modelPath = modelPath, port = localPort(), startupTimeout = Duration.ofSeconds(120),
                releaseTag = required("iwb.runtime.releaseTag", "IWB_RUNTIME_RELEASE_TAG"),
                officialArtifactName = required("iwb.runtime.artifact", "IWB_RUNTIME_ARTIFACT"),
                expectedExecutableSha256 = required("iwb.runtime.executableSha256", "IWB_RUNTIME_EXECUTABLE_SHA256"),
            ))),
        )
        val root = Path.of(System.getProperty("user.dir")).toRealPath()
        val judge = AgentExecutionService(
            RuntimeMarkdownLoader(root.resolve(".ai")).also { it.reload(profile.id) }, registry,
            PromptPreparationService(InputGuard(), FileSystemContextService(setOf(root))), inference,
        )
        val candidate = AgentExecutionOutcome(
            status = AgentExecutionStatus.SUCCESS,
            response = ModelResponse("The user can register a local GGUF model and submit a bounded request."),
            metadata = RunMetadata("candidate-run", "coding", profile.id, Instant.now(), ExecutionPolicy.LOCAL_ONLY, outcome = ModelResponseStatus.SUCCESS),
        )

        val result = runSuspend {
            EvaluationCoordinator(AgentExecutionJudgeExecutor(judge)).evaluate(
                candidate,
                EvaluationRubric("poc-quality", "1", listOf(EvaluationCriterion("relevance", 0, 4), EvaluationCriterion("completeness", 0, 4))),
            )
        }

        assertNotEquals(EvaluationStatus.UNKNOWN, result.status)
        assertNotEquals(JudgeStatus.UNKNOWN, result.judgeStatus)
        assertTrue(result.provenance.containsKey("judgeAgentId"))
        assertTrue(result.provenance.containsKey("judgeProfileId"))
    }

    private fun localPort(): Int = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")).use { it.localPort }
    private fun required(property: String, environment: String): String = setting(property, environment)
        ?: error("'$property' or '$environment' is required for real judge integration.")
    private fun setting(property: String, environment: String): String? =
        (System.getProperty(property) ?: System.getenv(environment))?.takeIf(String::isNotBlank)
    private fun <T> runSuspend(block: suspend () -> T): T {
        val outcome = AtomicReference<Result<T>>()
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { outcome.set(result) }
        })
        return outcome.get().getOrThrow()
    }

    private companion object {
        const val JUDGE_SCHEMA = """{"type":"object","required":["status","scores"],"properties":{"status":{"type":"string"},"scores":{"type":"object"}},"additionalProperties":false}"""
    }
}
