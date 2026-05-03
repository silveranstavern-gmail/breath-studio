package com.ponderingsilver.breathstudio.domain.model

enum class BreathAction(val label: String) {
    Inhale("Inhale"),
    HoldIn("Hold In"),
    Exhale("Exhale"),
    HoldOut("Hold Out"),
    Rest("Rest"),
}

enum class BreathRoute(val label: String) {
    Both("Both"),
    Left("Left"),
    Right("Right"),
}

enum class BreathingVisualMode(val label: String) {
    Circle("Circle"),
    SquareTracer("Square"),
}

sealed interface SessionRunTarget {
    data class Timed(val durationMinutes: Int) : SessionRunTarget
    data class PracticeCycles(val cycles: Int) : SessionRunTarget
}

sealed interface PracticeStageTarget {
    data class Rounds(val count: Int) : PracticeStageTarget
    data class DurationSeconds(val seconds: Int) : PracticeStageTarget
}

data class PracticeStep(
    val action: BreathAction,
    val durationSeconds: Int,
    val label: String = action.label,
    val route: BreathRoute = BreathRoute.Both,
) {
    val safeDurationSeconds: Int = durationSeconds.coerceAtLeast(MinimumStepDurationSeconds)
    val safeLabel: String = label.trim().ifBlank { action.label }
    val durationMillis: Long = safeDurationSeconds * 1_000L

    companion object {
        const val MinimumStepDurationSeconds = 1
    }
}

data class PracticeCycle(
    val steps: List<PracticeStep>,
) {
    init {
        require(steps.isNotEmpty()) { "A practice cycle must contain at least one step." }
    }

    val durationSeconds: Int = steps.sumOf { it.safeDurationSeconds }
}

data class PracticeStage(
    val title: String,
    val cycle: PracticeCycle,
    val target: PracticeStageTarget = PracticeStageTarget.Rounds(1),
) {
    val safeTitle: String = title.trim().ifBlank { "Practice stage" }
    val rounds: Int
        get() = when (target) {
            is PracticeStageTarget.Rounds -> target.count.coerceAtLeast(1)
            is PracticeStageTarget.DurationSeconds -> {
                val targetSeconds = target.seconds.coerceAtLeast(cycle.durationSeconds)
                ceilDiv(targetSeconds, cycle.durationSeconds.coerceAtLeast(1)).coerceAtLeast(1)
            }
        }

    companion object {
        private fun ceilDiv(value: Int, divisor: Int): Int = (value + divisor - 1) / divisor
    }
}

data class BreathPractice(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val category: String,
    val stages: List<PracticeStage>,
    val preferredVisualMode: BreathingVisualMode,
    val defaultDurationMinutes: Int = 5,
) {
    init {
        require(stages.isNotEmpty()) { "A practice must contain at least one stage." }
    }

    val safeId: String = id.trim().ifBlank { "practice" }
    val safeTitle: String = title.trim().ifBlank { "Untitled Practice" }
    val safeSubtitle: String = subtitle.trim().ifBlank { "Guided breathing" }
    val safeDescription: String = description.trim().ifBlank { "Follow each cue at a comfortable pace." }
    val safeCategory: String = category.trim().ifBlank { "Practice" }
    val safeDefaultDurationMinutes: Int = defaultDurationMinutes.coerceAtLeast(1)

    val cycleDurationSeconds: Int = stages.sumOf { stage ->
        stage.cycle.durationSeconds * stage.rounds
    }

    val cadenceLabel: String = stages.first().cycle.steps.joinToString(" / ") { step ->
        "${step.safeLabel} ${step.safeDurationSeconds}s"
    }
}

data class ExecutableBreathStep(
    val action: BreathAction,
    val label: String,
    val durationMillis: Long,
    val route: BreathRoute,
    val stageIndex: Int,
    val stageTitle: String,
    val roundInStage: Int,
    val stepIndexInCycle: Int,
)

data class ExecutableSessionPlan(
    val practice: BreathPractice,
    val visualMode: BreathingVisualMode,
    val runTarget: SessionRunTarget,
    val totalDurationMillis: Long,
    val steps: List<ExecutableBreathStep>,
) {
    init {
        require(steps.isNotEmpty()) { "An executable session plan must contain at least one step." }
    }
}

enum class SessionStatus {
    Running,
    Paused,
    Complete,
}

data class PlayerSessionState(
    val plan: ExecutableSessionPlan,
    val status: SessionStatus = SessionStatus.Running,
    val elapsedSessionMillis: Long = 0L,
    val currentStepIndex: Int = 0,
    val remainingStepMillis: Long = plan.steps.first().durationMillis,
) {
    val currentStep: ExecutableBreathStep
        get() = plan.steps[currentStepIndex]

    val currentAction: BreathAction
        get() = currentStep.action

    val currentLabel: String
        get() = currentStep.label

    val currentStepDurationMillis: Long
        get() = currentStep.durationMillis

    val currentStepProgress: Float
        get() = when {
            currentStepDurationMillis <= 0L -> 1f
            status == SessionStatus.Complete -> 1f
            else -> (1f - (remainingStepMillis.toFloat() / currentStepDurationMillis.toFloat())).coerceIn(0f, 1f)
        }

    val sessionProgress: Float
        get() = when {
            plan.totalDurationMillis <= 0L -> 1f
            else -> (elapsedSessionMillis.toFloat() / plan.totalDurationMillis.toFloat()).coerceIn(0f, 1f)
        }

    val remainingSessionMillis: Long
        get() = (plan.totalDurationMillis - elapsedSessionMillis).coerceAtLeast(0L)

    val currentCycleProgress: Float
        get() {
            val cycleSteps = currentStageCycleSteps()
            val currentCycleIndex = currentStep.stepIndexInCycle.coerceIn(0, cycleSteps.lastIndex)
            val cycleDuration = cycleSteps.sumOf { it.durationMillis }.toFloat()
            if (cycleDuration <= 0f) return 1f

            val completedMillis = cycleSteps.take(currentCycleIndex).sumOf { it.durationMillis }.toFloat()
            val inStepMillis = currentStepDurationMillis - remainingStepMillis
            return ((completedMillis + inStepMillis) / cycleDuration).coerceIn(0f, 1f)
        }

    private fun currentStageCycleSteps(): List<ExecutableBreathStep> {
        val stageId = currentStep.stageIndex to currentStep.roundInStage
        return plan.steps.filter { it.stageIndex == stageId.first && it.roundInStage == stageId.second }
    }
}
