package com.ponderingsilver.breathstudio.domain.model

sealed interface AuthoredBlockTarget {
    data class Repetitions(
        val count: Int,
        val fromTargetDurationMillis: Long? = null,
    ) : AuthoredBlockTarget
    data class DurationMillis(val durationMillis: Long) : AuthoredBlockTarget
}

data class AuthoredPracticeStep(
    val durationMillis: Long,
    val label: String = "Step",
    val colorHex: String = defaultColorHexForLabel(label),
    val sound: StepSound = StepSound.Default,
) {
    val safeDurationMillis: Long = durationMillis.coerceAtLeast(MinimumDurationMillis)
    val safeLabel: String = label.trim().ifBlank { "Step" }
    val safeColorHex: String = normalizeColorHexOrDefault(colorHex, safeLabel)
    val resolvedSound: StepSound = resolveStepSound(sound, safeLabel)

    companion object {
        const val MinimumDurationMillis: Long = 100L
    }
}

data class AuthoredPracticeCycle(
    val steps: List<AuthoredPracticeStep>,
) {
    init {
        require(steps.isNotEmpty()) { "An authored practice cycle must contain at least one step." }
    }

    val durationMillis: Long = steps.sumOf { it.safeDurationMillis }
}

sealed interface AuthoredPracticeBlock {
    val title: String

    data class RepeatingCycle(
        override val title: String,
        val cycle: AuthoredPracticeCycle,
        val target: AuthoredBlockTarget,
    ) : AuthoredPracticeBlock {
        val safeTitle: String = title.trim().ifBlank { "Practice block" }
    }
}

data class AuthoredPracticeDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val category: String,
    val blocks: List<AuthoredPracticeBlock>,
    val preferredVisualMode: BreathingVisualMode,
) {
    init {
        require(blocks.isNotEmpty()) { "An authored practice definition must contain at least one block." }
    }

    val safeId: String = id.trim().ifBlank { "practice" }
    val safeTitle: String = title.trim().ifBlank { "Untitled Practice" }
    val safeSubtitle: String = subtitle.trim().ifBlank { "Guided breathing" }
    val safeDescription: String = description.trim().ifBlank { "Follow each cue at a comfortable pace." }
    val safeCategory: String = category.trim().ifBlank { "Practice" }
}

fun AuthoredPracticeDefinition.toDomainPracticeOrNull(): BreathPractice? = runCatching {
    BreathPractice(
        id = safeId,
        title = safeTitle,
        subtitle = safeSubtitle,
        description = safeDescription,
        category = safeCategory,
        stages = blocks.mapNotNull { block -> block.toPracticeStageOrNull() }.ifEmpty {
            listOf(
                PracticeStage(
                    title = "Practice block",
                    cycle = PracticeCycle(
                        steps = listOf(
                            PracticeStep(
                                durationSeconds = 1,
                                label = "Rest",
                            ),
                        ),
                    ),
                ),
            )
        },
        preferredVisualMode = preferredVisualMode,
    )
}.getOrNull()

fun BreathPractice.toAuthoredPracticeDefinition(): AuthoredPracticeDefinition = AuthoredPracticeDefinition(
    id = safeId,
    title = safeTitle,
    subtitle = safeSubtitle,
    description = safeDescription,
    category = safeCategory,
    blocks = stages.map { stage ->
        AuthoredPracticeBlock.RepeatingCycle(
            title = stage.safeTitle,
            cycle = AuthoredPracticeCycle(
                steps = stage.cycle.steps.map { step ->
                    AuthoredPracticeStep(
                        durationMillis = step.durationMillis,
                        label = step.safeLabel,
                        colorHex = step.safeColorHex,
                        sound = step.resolvedSound,
                    )
                },
            ),
            target = when (val target = stage.target) {
                is PracticeStageTarget.Rounds -> AuthoredBlockTarget.Repetitions(target.count.coerceAtLeast(1))
                is PracticeStageTarget.DurationSeconds -> AuthoredBlockTarget.DurationMillis(
                    target.seconds.coerceAtLeast(1) * 1_000L,
                )
            },
        )
    },
    preferredVisualMode = preferredVisualMode,
)

private fun AuthoredPracticeBlock.toPracticeStageOrNull(): PracticeStage? {
    return when (this) {
        is AuthoredPracticeBlock.RepeatingCycle -> {
            val domainSteps = cycle.steps.map { it.toPracticeStep() }
            runCatching {
                PracticeStage(
                    title = safeTitle,
                    cycle = PracticeCycle(domainSteps),
                    target = when (val blockTarget = target) {
                        is AuthoredBlockTarget.Repetitions -> PracticeStageTarget.Rounds(blockTarget.count.coerceAtLeast(1))
                        is AuthoredBlockTarget.DurationMillis -> PracticeStageTarget.DurationSeconds(
                            ceilDiv(blockTarget.durationMillis.coerceAtLeast(1L), 1_000L)
                                .coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt(),
                        )
                    },
                )
            }.getOrNull()
        }
    }
}

private fun AuthoredPracticeStep.toPracticeStep(): PracticeStep = PracticeStep(
    durationSeconds = ceilDiv(safeDurationMillis, 1_000L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt(),
    label = safeLabel,
    colorHex = safeColorHex,
    sound = sound,
)

private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1L) / divisor
