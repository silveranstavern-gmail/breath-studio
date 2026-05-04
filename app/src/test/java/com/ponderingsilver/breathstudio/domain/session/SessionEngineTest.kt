package com.ponderingsilver.breathstudio.domain.session

import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import com.ponderingsilver.breathstudio.domain.model.PracticeCycle
import com.ponderingsilver.breathstudio.domain.model.PracticeStage
import com.ponderingsilver.breathstudio.domain.model.PracticeStageTarget
import com.ponderingsilver.breathstudio.domain.model.PracticeStep
import com.ponderingsilver.breathstudio.domain.model.SessionRunTarget
import com.ponderingsilver.breathstudio.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest {
    @Test
    fun timedPlanClampsInvalidDurationToOneMinute() {
        val plan = buildExecutableSessionPlan(
            practice = testPractice(),
            runTarget = SessionRunTarget.Timed(durationMinutes = -3),
        )

        assertEquals(60_000L, plan.totalDurationMillis)
        assertTrue(plan.steps.isNotEmpty())
        assertEquals(60_000L, plan.steps.sumOf { it.durationMillis })
    }

    @Test
    fun cycleCountPlanGeneratesExactPracticeRepetitions() {
        val plan = buildExecutableSessionPlan(
            practice = testPractice(),
            runTarget = SessionRunTarget.PracticeCycles(cycles = 3),
        )

        assertEquals(6, plan.steps.size)
        assertEquals(18_000L, plan.totalDurationMillis)
        assertEquals(listOf("Inhale", "Exhale", "Inhale", "Exhale", "Inhale", "Exhale"), plan.steps.map { it.label })
    }

    @Test
    fun invalidStepDurationIsClampedBeforeExecution() {
        val practice = testPractice(
            steps = listOf(
                PracticeStep(durationSeconds = 0, label = "Inhale"),
                PracticeStep(durationSeconds = -9, label = "Exhale"),
            ),
        )

        val plan = buildExecutableSessionPlan(
            practice = practice,
            runTarget = SessionRunTarget.PracticeCycles(cycles = 1),
        )

        assertEquals(2_000L, plan.totalDurationMillis)
        assertEquals(listOf(1_000L, 1_000L), plan.steps.map { it.durationMillis })
        assertEquals("Inhale", plan.steps.first().label)
    }

    @Test
    fun cycleCountPlanCapsOversizedGeneratedPlans() {
        val plan = buildExecutableSessionPlan(
            practice = testPractice(),
            runTarget = SessionRunTarget.PracticeCycles(cycles = Int.MAX_VALUE),
        )

        assertEquals(20_000, plan.steps.size)
        assertEquals(60_000_000L, plan.totalDurationMillis)
    }

    @Test
    fun advancingPastEndCompletesWithoutOvershooting() {
        val plan = buildExecutableSessionPlan(
            practice = testPractice(),
            runTarget = SessionRunTarget.PracticeCycles(cycles = 1),
        )

        val finalState = advanceSession(
            state = newPlayerSession(plan),
            deltaMillis = 90_000L,
        )

        assertEquals(SessionStatus.Complete, finalState.status)
        assertEquals(plan.totalDurationMillis, finalState.elapsedSessionMillis)
        assertEquals(0L, finalState.remainingStepMillis)
        assertEquals(plan.steps.lastIndex, finalState.currentStepIndex)
    }

    @Test
    fun stageProgressResetsWhenAdvancingToNextStage() {
        val practice = BreathPractice(
            id = "stages",
            title = "Stages",
            subtitle = "Stage progress",
            description = "Tracks stage progress.",
            category = "Test",
            preferredVisualMode = BreathingVisualMode.Glow,
            stages = listOf(
                PracticeStage(
                    title = "First",
                    target = PracticeStageTarget.Rounds(1),
                    cycle = PracticeCycle(listOf(PracticeStep(durationSeconds = 2, label = "Inhale"))),
                ),
                PracticeStage(
                    title = "Second",
                    target = PracticeStageTarget.Rounds(1),
                    cycle = PracticeCycle(listOf(PracticeStep(durationSeconds = 2, label = "Exhale"))),
                ),
            ),
        )
        val plan = buildExecutableSessionPlan(practice, SessionRunTarget.PracticeCycles(1))

        val first = advanceSession(newPlayerSession(plan), 1_000L)
        val second = advanceSession(first, 1_500L)

        assertEquals(0.5f, first.currentStageProgress, 0.001f)
        assertTrue(second.currentStageProgress < 0.5f)
    }

    private fun testPractice(
        steps: List<PracticeStep> = listOf(
            PracticeStep(durationSeconds = 2, label = "Inhale"),
            PracticeStep(durationSeconds = 4, label = "Exhale"),
        ),
    ): BreathPractice = BreathPractice(
        id = "test",
        title = "Test Practice",
        subtitle = "Testing",
        description = "A small deterministic practice for session-engine tests.",
        category = "Test",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Main",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(steps = steps),
            ),
        ),
    )
}

