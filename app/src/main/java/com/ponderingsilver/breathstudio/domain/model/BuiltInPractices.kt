package com.ponderingsilver.breathstudio.domain.model

object BuiltInPractices {
    val BoxBreathing = BreathPractice(
        id = "box",
        title = "Box Breathing",
        subtitle = "Steady focus",
        description = "Find your center with equal parts breath and pause. A steady anchor for focus.",
        category = "Reset",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Stage cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 4, label = "Hold In", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 4, label = "Exhale", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 4, label = "Hold Out", sound = StepSound.Hold),
                    ),
                ),
            ),
        ),
    )

    val FourSevenEight = BreathPractice(
        id = "four-seven-eight",
        title = "4-7-8",
        subtitle = "Long release",
        description = "A rhythmic descent into stillness. Quiet the nervous system with a long, controlled release.",
        category = "Calming",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Stage cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 7, label = "Hold In", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 8, label = "Exhale", sound = StepSound.Exhale),
                    ),
                ),
            ),
        ),
    )

    val ExtendedExhale = BreathPractice(
        id = "extended-exhale",
        title = "Extended Exhale",
        subtitle = "Gentle reset",
        description = "Gently downshift. A soft practice focusing on the soothing power of a long exhale.",
        category = "Reset",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Stage cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 2, label = "Hold In", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 6, label = "Exhale", sound = StepSound.Exhale),
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
        category = "Calming",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Settle",
                target = PracticeStageTarget.Rounds(3),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 6, label = "Exhale", sound = StepSound.Exhale),
                    ),
                ),
            ),
            PracticeStage(
                title = "Lengthen",
                target = PracticeStageTarget.Rounds(3),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 8, label = "Exhale", sound = StepSound.Exhale),
                    ),
                ),
            ),
            PracticeStage(
                title = "Soften",
                target = PracticeStageTarget.Rounds(3),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Inhale", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 10, label = "Exhale", sound = StepSound.Exhale),
                    ),
                ),
            ),
        ),
        defaultDurationMinutes = 6,
    )

    val NadiShodhanaL3 = BreathPractice(
        id = "nadi-shodhana-l3",
        title = "Nadi Shodhana L3",
        subtitle = "Alternate nostril balance",
        description = "A full alternate-nostril cycle with equal inhale, inner hold, exhale, and outer hold. Keep the count smooth and unforced.",
        category = "Reset",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Full round",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 5, label = "Left Nostril Inhale", colorHex = "#6FCF97", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 5, label = "Block and Hold In", colorHex = "#E2C27B", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 5, label = "Right Nostril Exhale", colorHex = "#6BA8FF", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 5, label = "Block and Hold Out", colorHex = "#BFC9D4", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 5, label = "Right Nostril Inhale", colorHex = "#6BA8FF", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 5, label = "Block and Hold In", colorHex = "#E2C27B", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 5, label = "Left Nostril Exhale", colorHex = "#6FCF97", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 5, label = "Block and Hold Out", colorHex = "#BFC9D4", sound = StepSound.Hold),
                    ),
                ),
            ),
        ),
        defaultDurationMinutes = 20,
    )

    val NadiShodhanaL4 = BreathPractice(
        id = "nadi-shodhana-l4",
        title = "Nadi Shodhana L4",
        subtitle = "Alternate nostril retention ladder",
        description = "A longer-hold alternate nostril cycle with steady 5 second inhales and exhales, then 10 second inner and outer retentions.",
        category = "Reset",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Full round",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 5, label = "Left Nostril Inhale", colorHex = "#6FCF97", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 10, label = "Block and Hold In", colorHex = "#E2C27B", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 5, label = "Right Nostril Exhale", colorHex = "#6BA8FF", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 10, label = "Block and Hold Out", colorHex = "#BFC9D4", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 5, label = "Right Nostril Inhale", colorHex = "#6BA8FF", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 10, label = "Block and Hold In", colorHex = "#E2C27B", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 5, label = "Left Nostril Exhale", colorHex = "#6FCF97", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 10, label = "Block and Hold Out", colorHex = "#BFC9D4", sound = StepSound.Hold),
                    ),
                ),
            ),
        ),
        defaultDurationMinutes = 20,
    )

    val NadiShodhanaL5 = BreathPractice(
        id = "nadi-shodhana-l5",
        title = "Nadi Shodhana L5",
        subtitle = "Extended alternate nostril holds",
        description = "A deeper alternate nostril round with longer retentions and a fuller exhale. Only use this level if the breath stays smooth and controlled.",
        category = "Calming",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Full round",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 5, label = "Left Nostril Inhale", colorHex = "#6FCF97", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 15, label = "Block and Hold In", colorHex = "#E2C27B", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 10, label = "Right Nostril Exhale", colorHex = "#6BA8FF", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 15, label = "Block and Hold Out", colorHex = "#BFC9D4", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 5, label = "Right Nostril Inhale", colorHex = "#6BA8FF", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 15, label = "Block and Hold In", colorHex = "#E2C27B", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 10, label = "Left Nostril Exhale", colorHex = "#6FCF97", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 15, label = "Block and Hold Out", colorHex = "#BFC9D4", sound = StepSound.Hold),
                    ),
                ),
            ),
        ),
        defaultDurationMinutes = 20,
    )

    val LeftNostrilMoon = BreathPractice(
        id = "left-nostril-moon",
        title = "Left Nostril / Moon",
        subtitle = "Cooling nasal downshift",
        description = "A single-side nasal practice that stays simple while still giving users a nostril-specific template to modify.",
        category = "Calming",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Cooling cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 5, label = "Left Nostril Inhale", colorHex = "#6FCF97", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 2, label = "Block and Hold In", colorHex = "#E2C27B", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 7, label = "Left Nostril Exhale", colorHex = "#6FCF97", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 2, label = "Block and Hold Out", colorHex = "#BFC9D4", sound = StepSound.Hold),
                    ),
                ),
            ),
        ),
        defaultDurationMinutes = 12,
    )

    val RightNostrilSun = BreathPractice(
        id = "right-nostril-sun",
        title = "Right Nostril / Sun",
        subtitle = "Brightening nasal focus",
        description = "A one-sided activating nasal pattern that works as a reusable template for energizing focus sessions.",
        category = "Energizing",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Activating cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 5, label = "Right Nostril Inhale", colorHex = "#6BA8FF", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 2, label = "Block and Hold In", colorHex = "#E2C27B", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 5, label = "Right Nostril Exhale", colorHex = "#6BA8FF", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 2, label = "Block and Hold Out", colorHex = "#BFC9D4", sound = StepSound.Hold),
                    ),
                ),
            ),
        ),
        defaultDurationMinutes = 12,
    )

    val Bhramari = BreathPractice(
        id = "bhramari",
        title = "Bhramari",
        subtitle = "Humming exhale meditation",
        description = "A calming humming-breath template with a long resonant exhale and a gentle pause between rounds.",
        category = "Calming",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Humming cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Nasal Inhale", colorHex = "#7ED9C8", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 8, label = "Humming Exhale", colorHex = "#E6B86A", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 2, label = "Quiet Pause", colorHex = "#BFC9D4", sound = StepSound.Other2),
                    ),
                ),
            ),
        ),
        defaultDurationMinutes = 10,
    )

    val Ujjayi = BreathPractice(
        id = "ujjayi",
        title = "Ujjayi",
        subtitle = "Ocean-breath concentration",
        description = "A steady ocean-breath rhythm with matched inhale and exhale, useful as a strong base template for meditative pacing.",
        category = "Reset",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Ocean cycle",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 6, label = "Ocean Inhale", colorHex = "#7ED9C8", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 6, label = "Ocean Exhale", colorHex = "#9BC1FF", sound = StepSound.Exhale),
                    ),
                ),
            ),
        ),
        defaultDurationMinutes = 12,
    )

    val PowerBreathRetention = BreathPractice(
        id = "power-breath-retention",
        title = "Power Breath + Retention",
        subtitle = "Staged energize and reset template",
        description = "A multi-stage power-breath routine with rhythmic active breathing, a long exhale hold, and a short recovery hold. This is intentionally structured as a more advanced template to tweak.",
        category = "Energizing",
        preferredVisualMode = BreathingVisualMode.Glow,
        stages = listOf(
            PracticeStage(
                title = "Build",
                target = PracticeStageTarget.Rounds(15),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 2, label = "Power Inhale", colorHex = "#7ED9C8", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 2, label = "Power Exhale", colorHex = "#9BC1FF", sound = StepSound.Exhale),
                    ),
                ),
            ),
            PracticeStage(
                title = "Hold Out",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Release Exhale", colorHex = "#9BC1FF", sound = StepSound.Exhale),
                        PracticeStep(durationSeconds = 30, label = "Hold Out", colorHex = "#C7D7D4", sound = StepSound.Hold),
                    ),
                ),
            ),
            PracticeStage(
                title = "Recovery",
                target = PracticeStageTarget.Rounds(1),
                cycle = PracticeCycle(
                    steps = listOf(
                        PracticeStep(durationSeconds = 4, label = "Recovery Inhale", colorHex = "#7ED9C8", sound = StepSound.Inhale),
                        PracticeStep(durationSeconds = 15, label = "Hold In", colorHex = "#E7C98C", sound = StepSound.Hold),
                        PracticeStep(durationSeconds = 6, label = "Ease Out", colorHex = "#9BC1FF", sound = StepSound.Exhale),
                    ),
                ),
            ),
        ),
        defaultDurationMinutes = 15,
    )

    val all: List<BreathPractice> = listOf(
        FourSevenEight,
        ExtendedExhale,
        NadiShodhanaL3,
        NadiShodhanaL4,
        NadiShodhanaL5,
        LeftNostrilMoon,
        RightNostrilSun,
        Bhramari,
        Ujjayi,
        PowerBreathRetention,
    )
}
