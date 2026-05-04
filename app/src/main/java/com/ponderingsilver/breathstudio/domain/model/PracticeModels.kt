package com.ponderingsilver.breathstudio.domain.model

enum class BreathingVisualMode(val label: String) {
    Glow("Glow"),
}

enum class StepSound(val label: String) {
    Default("Default"),
    Inhale("Inhale"),
    Exhale("Exhale"),
    Hold("Hold"),
    Other1("Other 1"),
    Other2("Other 2"),
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
    val durationSeconds: Int,
    val label: String = "Step",
    val colorHex: String = defaultColorHexForLabel(label),
    val sound: StepSound = StepSound.Default,
) {
    val safeDurationSeconds: Int = durationSeconds.coerceAtLeast(MinimumStepDurationSeconds)
    val safeLabel: String = label.trim().ifBlank { "Step" }
    val safeColorHex: String = normalizeColorHexOrDefault(colorHex, safeLabel)
    val resolvedSound: StepSound = resolveStepSound(sound, safeLabel)
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
    val label: String,
    val durationMillis: Long,
    val colorHex: String,
    val sound: StepSound,
    val stageIndex: Int,
    val stageTitle: String,
    val roundInStage: Int,
    val stepIndexInCycle: Int,
    val sessionCycleIndex: Int = 0,
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
    Preparing,
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
    val orientationRemainingMillis: Long = 0L,
) {
    val currentStep: ExecutableBreathStep
        get() = plan.steps[currentStepIndex]

    val currentLabel: String
        get() = currentStep.label

    val currentStepDurationMillis: Long
        get() = currentStep.durationMillis

    val currentStepProgress: Float
        get() = when {
            status == SessionStatus.Preparing -> 0f
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

    val currentStageProgress: Float
        get() {
            val stageSteps = currentStageSteps()
            val stageDuration = stageSteps.sumOf { it.durationMillis }.toFloat()
            if (stageDuration <= 0f) return 1f

            val stageStartIndex = plan.steps.indexOf(stageSteps.first())
            if (stageStartIndex < 0) return 0f

            val completedMillisBeforeStage = plan.steps.take(stageStartIndex).sumOf { it.durationMillis }.toFloat()
            val elapsedInStage = (elapsedSessionMillis.toFloat() - completedMillisBeforeStage).coerceIn(0f, stageDuration)
            return (elapsedInStage / stageDuration).coerceIn(0f, 1f)
        }

    private fun currentStageSteps(): List<ExecutableBreathStep> {
        return plan.steps.filter { step ->
            step.stageIndex == currentStep.stageIndex &&
                step.sessionCycleIndex == currentStep.sessionCycleIndex
        }
    }

    private fun currentStageCycleSteps(): List<ExecutableBreathStep> {
        return plan.steps.filter { step ->
            step.stageIndex == currentStep.stageIndex &&
                step.roundInStage == currentStep.roundInStage &&
                step.sessionCycleIndex == currentStep.sessionCycleIndex
        }
    }
}

private val LabelColorPresets: Map<String, String> = mapOf(
    "inhale" to "#7ED9C8",
    "hold in" to "#E7C98C",
    "hold" to "#E7C98C",
    "exhale" to "#9BC1FF",
    "hold out" to "#C7D7D4",
    "rest" to "#E8E1D1",
)

fun defaultColorHexForLabel(label: String): String {
    val key = label.trim().lowercase()
    return LabelColorPresets[key] ?: "#7ED9C8"
}

fun normalizeColorHexOrNull(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    val withoutHash = if (trimmed.startsWith("#")) trimmed.drop(1) else trimmed
    if (withoutHash.length != 6) return null
    if (!withoutHash.all { char -> char.isDigit() || char.lowercaseChar() in 'a'..'f' }) return null
    return "#${withoutHash.uppercase()}"
}

fun normalizeColorHexOrDefault(value: String?, label: String): String {
    return normalizeColorHexOrNull(value) ?: defaultColorHexForLabel(label)
}

fun resolveStepSound(sound: StepSound, label: String): StepSound {
    return sound
}

fun parseStepSound(value: String?): StepSound {
    val normalized = value?.trim().orEmpty()
    return StepSound.entries.firstOrNull { sound ->
        sound.name.equals(normalized, ignoreCase = true) ||
            sound.label.equals(normalized, ignoreCase = true)
    } ?: StepSound.Default
}
