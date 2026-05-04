package com.ponderingsilver.breathstudio.domain.model

object BuiltInPractices {
    val BoxBreathing = BreathPractice(
        id = "box",
        title = "Box Breathing",
        subtitle = "Steady focus",
        description = "A four-sided breath for balance and concentration, guided by a soft visual glow.",
        category = "Focus",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Main cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale"),
                        PracticeStep(durationSeconds = 4, label = "Hold In"),
                        PracticeStep(durationSeconds = 4, label = "Exhale"),
                        PracticeStep(durationSeconds = 4, label = "Hold Out"),
                    ),
                ),
            ),
        ),
    )

    val FourSevenEight = BreathPractice(
        id = "four-seven-eight",
        title = "4-7-8",
        subtitle = "Long release",
        description = "A classic calming ratio with a longer hold and a fuller exhale.",
        category = "Calm",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Main cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale"),
                        PracticeStep(durationSeconds = 7, label = "Hold In"),
                        PracticeStep(durationSeconds = 8, label = "Exhale"),
                    ),
                ),
            ),
        ),
    )

    val ExtendedExhale = BreathPractice(
        id = "extended-exhale",
        title = "Extended Exhale",
        subtitle = "Gentle reset",
        description = "A softer downshift with a longer exhale and a lighter internal pause.",
        category = "Reset",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Main cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale"),
                        PracticeStep(durationSeconds = 2, label = "Hold In"),
                        PracticeStep(durationSeconds = 6, label = "Exhale"),
                    ),
                ),
            ),
        ),
    )

    val ExhaleLadder = BreathPractice(
        id = "exhale-ladder",
        title = "Exhale Ladder",
        subtitle = "Stage-based downshift",
        description = "A progressive practice that lengthens the exhale over three stages without changing the inhale.",
        category = "Sleep",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Settle",
                target = PracticeStageTarget.Rounds(3),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale"),
                        PracticeStep(durationSeconds = 6, label = "Exhale"),
                    ),
                ),
            ),
            PracticeStage(
                title = "Lengthen",
                target = PracticeStageTarget.Rounds(3),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale"),
                        PracticeStep(durationSeconds = 8, label = "Exhale"),
                    ),
                ),
            ),
            PracticeStage(
                title = "Soften",
                target = PracticeStageTarget.Rounds(3),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale"),
                        PracticeStep(durationSeconds = 10, label = "Exhale"),
                    ),
                ),
            ),
        ),
        defaultDurationMinutes = 6,
    )

    val all: List<BreathPractice> = listOf(
        BoxBreathing,
        FourSevenEight,
        ExtendedExhale,
        ExhaleLadder,
    )
}
