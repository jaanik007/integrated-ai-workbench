package com.jaanik.integratedaiworkbench.backend.evaluation
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import com.jaanik.integratedaiworkbench.shared.run.*
import com.jaanik.integratedaiworkbench.shared.model.ExecutionPolicy
import java.time.Instant
import kotlin.coroutines.startCoroutine
import kotlin.test.*
class EvaluationCoordinatorTest {
 private val rubric=EvaluationRubric("quality","1",listOf(EvaluationCriterion("clarity",1,5)))
 private fun outcome(status:AgentExecutionStatus=AgentExecutionStatus.SUCCESS,text:String="answer")=AgentExecutionOutcome(status,if(status==AgentExecutionStatus.SUCCESS) ModelResponse(text) else null,RunMetadata("r","agent","profile",Instant.now(),ExecutionPolicy.LOCAL_ONLY))
 @Test fun `deterministic failure blocks judge`(){ var called=false; val r=run { EvaluationCoordinator(JudgeExecutor { _,_->called=true; outcome() }).evaluate(outcome(AgentExecutionStatus.OUTPUT_REJECTED),rubric) }; assertEquals(false,r.passed);assertFalse(called) }
 @Test fun `valid and invalid judge results remain typed`(){ val good=run { EvaluationCoordinator(JudgeExecutor { _,_->outcome(text="{\"status\":\"PASS\",\"scores\":{\"clarity\":5}}") }).evaluate(outcome(),rubric) };assertTrue(good.passed); val bad=run { EvaluationCoordinator(JudgeExecutor { _,_->outcome(text="{\"status\":\"PASS\",\"scores\":{\"clarity\":9}}") }).evaluate(outcome(),rubric) };assertEquals(com.jaanik.integratedaiworkbench.shared.evaluation.EvaluationStatus.UNKNOWN,bad.status) }
 @Test fun `unavailable judge is unknown`(){ val r=run { EvaluationCoordinator().evaluate(outcome(),rubric) };assertEquals(com.jaanik.integratedaiworkbench.shared.evaluation.JudgeStatus.UNKNOWN,r.judgeStatus) }
 private fun <T> run(block:suspend()->T):T { var r:Result<T>?=null; block.startCoroutine(object:kotlin.coroutines.Continuation<T>{override val context=kotlin.coroutines.EmptyCoroutineContext;override fun resumeWith(result:Result<T>){r=result}});return r!!.getOrThrow() }
}
