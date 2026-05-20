package com.ponderingsilver.breathstudio.domain.session

import com.ponderingsilver.breathstudio.domain.model.AuthoredBlockTarget
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeBlock
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import com.ponderingsilver.breathstudio.domain.model.ExecutableBreathStep
import com.ponderingsilver.breathstudio.domain.model.ExecutableSessionPlan
import com.ponderingsilver.breathstudio.domain.model.PlayerSessionState
import com.ponderingsilver.breathstudio.domain.model.SessionRunTarget
import com.ponderingsilver.breathstudio.domain.model.SessionStatus
import com.ponderingsilver.breathstudio.domain.model.toAuthoredPracticeDefinition

fun buildExecutableSessionPlan(
    practice: BreathPractice,
    runTarget: SessionRunTarget,
    visualMode: BreathingVisualMode = practice.preferredVisualMode,
): ExecutableSessionPlan {
    return buildExecutableSessionPlan(
        practice = practice,
        authoredDefinition = practice.toAuthoredPracticeDefinition(),
        runTarget = runTarget,
        visualMode = visualMode,
    )
}

fun buildExecutableSessionPlan(
    practice: BreathPractice,
    authoredDefinition: AuthoredPracticeDefinition,
    runTarget: SessionRunTarget,
    visualMode: BreathingVisualMode = practice.preferredVisualMode,
): ExecutableSessionPlan {
    val templateSteps = buildTemplateSteps(authoredDefinition)
    val executableSteps = when (runTarget) {
        is SessionRunTarget.Timed -> buildTimedSteps(
            templateSteps = templateSteps,
            totalDurationMillis = runTarget.durationMinutes.coerceIn(1, MaximumSessionMinutes) * MillisPerMinute,
        )
        is SessionRunTarget.PracticeCycles -> buildCycleCountSteps(
            templateSteps = templateSteps,
            cycles = runTarget.cycles.coerceAtLeast(1),
        )
    }
    val totalDurationMillis = executableSteps.sumOf { it.durationMillis }

    return ExecutableSessionPlan(
        practice = practice,
        visualMode = visualMode,
        runTarget = runTarget,
        totalDurationMillis = totalDurationMillis,
        steps = executableSteps,
    )
}

internal fun buildTemplateSteps(definition: AuthoredPracticeDefinition): List<ExecutableBreathStep> {
    val steps = mutableListOf<ExecutableBreathStep>()

    definition.blocks.forEachIndexed { blockIndex, block ->
        when (block) {
            is AuthoredPracticeBlock.RepeatingCycle -> {
                val cycleSteps = block.cycle.steps
                val cycleDurationMillis = block.cycle.durationMillis.coerceAtLeast(1L)
                val rounds: Int = when (val target = block.target) {
                    is AuthoredBlockTarget.Repetitions -> target.count.coerceAtLeast(1)
                    is AuthoredBlockTarget.DurationMillis -> ceilDiv(
                        target.durationMillis.coerceAtLeast(cycleDurationMillis),
                        cycleDurationMillis,
                    ).coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                }

                repeat(rounds) { roundIndex ->
                    cycleSteps.forEachIndexed { stepIndex, step ->
                        steps += ExecutableBreathStep(
                            label = step.safeLabel,
                            durationMillis = step.safeDurationMillis,
                            colorHex = step.safeColorHex,
                            sound = step.resolvedSound,
                            stageIndex = blockIndex,
                            stageTitle = block.safeTitle,
                            roundInStage = roundIndex,
                            stepIndexInCycle = stepIndex,
                        )
                    }
                }
            }
        }
    }

    return steps
}

private fun buildTimedSteps(
    templateSteps: List<ExecutableBreathStep>,
    totalDurationMillis: Long,
): List<ExecutableBreathStep> {
    val executableSteps = mutableListOf<ExecutableBreathStep>()
    var elapsed = 0L
    var sessionCycleIndex = 0

    while (elapsed < totalDurationMillis) {
        templateSteps.forEach { template ->
            executableSteps += template.copy(
                sessionCycleIndex = sessionCycleIndex,
            )
            elapsed += template.durationMillis
        }
        sessionCycleIndex += 1
    }

    return executableSteps
}

private fun buildCycleCountSteps(
    templateSteps: List<ExecutableBreathStep>,
    cycles: Int,
): List<ExecutableBreathStep> {
    val maximumCyclesForTemplate = (MaximumGeneratedExecutableSteps / templateSteps.size.coerceAtLeast(1))
        .coerceAtLeast(1)
    val boundedCycles = cycles.coerceIn(1, maximumCyclesForTemplate)
    return buildList {
        repeat(boundedCycles) { sessionCycleIndex ->
            addAll(
                templateSteps.map { template ->
                    template.copy(sessionCycleIndex = sessionCycleIndex)
                },
            )
        }
    }
}

fun newPlayerSession(plan: ExecutableSessionPlan): PlayerSessionState {
    return PlayerSessionState(
        plan = plan,
        remainingStepMillis = plan.steps.first().durationMillis,
    )
}

fun advanceSession(
    state: PlayerSessionState,
    deltaMillis: Long,
): PlayerSessionState {
    if (state.status != SessionStatus.Running || deltaMillis <= 0L) {
        return state
    }

    val actualDelta = minOf(deltaMillis, state.remainingSessionMillis)
    var elapsed = state.elapsedSessionMillis + actualDelta
    var currentStepIndex = state.currentStepIndex
    var remainingStepMillis = state.remainingStepMillis - actualDelta

    while (remainingStepMillis <= 0L && elapsed < state.plan.totalDurationMillis) {
        val overflow = -remainingStepMillis
        if (currentStepIndex >= state.plan.steps.lastIndex) {
            break
        }
        currentStepIndex += 1
        remainingStepMillis = state.plan.steps[currentStepIndex].durationMillis - overflow
    }

    return if (elapsed >= state.plan.totalDurationMillis || currentStepIndex >= state.plan.steps.lastIndex && remainingStepMillis <= 0L) {
        state.copy(
            elapsedSessionMillis = state.plan.totalDurationMillis,
            currentStepIndex = state.plan.steps.lastIndex,
            remainingStepMillis = 0L,
            status = SessionStatus.Complete,
        )
    } else {
        state.copy(
            elapsedSessionMillis = elapsed,
            currentStepIndex = currentStepIndex,
            remainingStepMillis = remainingStepMillis.coerceAtLeast(0L),
        )
    }
}

private const val MillisPerMinute = 60_000L
private const val MaximumSessionMinutes = 240
private const val MaximumGeneratedExecutableSteps = 20_000

private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1L) / divisor
