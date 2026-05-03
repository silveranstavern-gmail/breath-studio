package com.ponderingsilver.breathstudio.domain.model

object BuiltInPractices {
    val BoxBreathing = BreathPractice(
        id = "box",
        title = "Box Breathing",
        subtitle = "Steady focus",
        description = "A four-sided breath for balance and concentration, best paired with the square guide.",
        category = "Focus",
        preferredVisualMode = BreathingVisualMode.SquareTracer,
        stages = listOf(
            PracticeStage(
                title = "Main cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(BreathAction.Inhale, 4),
                        PracticeStep(BreathAction.HoldIn, 4),
                        PracticeStep(BreathAction.Exhale, 4),
                        PracticeStep(BreathAction.HoldOut, 4),
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
        preferredVisualMode = BreathingVisualMode.Circle,
        stages = listOf(
            PracticeStage(
                title = "Main cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(BreathAction.Inhale, 4),
                        PracticeStep(BreathAction.HoldIn, 7),
                        PracticeStep(BreathAction.Exhale, 8),
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
        preferredVisualMode = BreathingVisualMode.Circle,
        stages = listOf(
            PracticeStage(
                title = "Main cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(BreathAction.Inhale, 4),
                        PracticeStep(BreathAction.HoldIn, 2),
                        PracticeStep(BreathAction.Exhale, 6),
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
        preferredVisualMode = BreathingVisualMode.Circle,
        stages = listOf(
            PracticeStage(
                title = "Settle",
                target = PracticeStageTarget.Rounds(3),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(BreathAction.Inhale, 4),
                        PracticeStep(BreathAction.Exhale, 6),
                    ),
                ),
            ),
            PracticeStage(
                title = "Lengthen",
                target = PracticeStageTarget.Rounds(3),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(BreathAction.Inhale, 4),
                        PracticeStep(BreathAction.Exhale, 8),
                    ),
                ),
            ),
            PracticeStage(
                title = "Soften",
                target = PracticeStageTarget.Rounds(3),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(BreathAction.Inhale, 4),
                        PracticeStep(BreathAction.Exhale, 10),
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
