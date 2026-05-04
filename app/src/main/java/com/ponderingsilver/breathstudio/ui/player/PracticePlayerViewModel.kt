package com.ponderingsilver.breathstudio.ui.player

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ponderingsilver.breathstudio.SessionConfig
import com.ponderingsilver.breathstudio.domain.model.ExecutableBreathStep
import com.ponderingsilver.breathstudio.domain.model.PlayerSessionState
import com.ponderingsilver.breathstudio.domain.model.SessionStatus
import com.ponderingsilver.breathstudio.domain.session.advanceSession
import com.ponderingsilver.breathstudio.domain.session.buildExecutableSessionPlan
import com.ponderingsilver.breathstudio.domain.session.newPlayerSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerCueEvent {
    data class StepStarted(val stepIndex: Int, val step: ExecutableBreathStep) : PlayerCueEvent
    data object StepStopped : PlayerCueEvent
    data object SessionCompleted : PlayerCueEvent
}

class PracticePlayerViewModel : ViewModel() {
    private val _sessionState = MutableStateFlow<PlayerSessionState?>(null)
    val sessionState: StateFlow<PlayerSessionState?> = _sessionState.asStateFlow()

    private val _cueEvents = MutableSharedFlow<PlayerCueEvent>(
        extraBufferCapacity = CueEventBufferCapacity,
    )
    val cueEvents: SharedFlow<PlayerCueEvent> = _cueEvents.asSharedFlow()

    private var tickerJob: Job? = null
    private var activeConfigKey: SessionConfigKey? = null

    fun start(config: SessionConfig) {
        val configKey = SessionConfigKey.from(config)
        if (activeConfigKey == configKey && _sessionState.value != null) {
            return
        }

        activeConfigKey = configKey
        val plan = if (config.authoredDefinition != null) {
            buildExecutableSessionPlan(
                practice = config.practice,
                authoredDefinition = config.authoredDefinition,
                durationMinutes = config.durationMinutes,
                visualMode = config.visualMode,
            )
        } else {
            buildExecutableSessionPlan(
                practice = config.practice,
                durationMinutes = config.durationMinutes,
                visualMode = config.visualMode,
            )
        }
        _sessionState.value = newPlayerSession(plan).withOrientationDelay()
        startTicker()
    }

    fun pauseOrResume() {
        val current = _sessionState.value ?: return
        val nextState = when (current.status) {
            SessionStatus.Preparing -> {
                val started = current.copy(
                    status = SessionStatus.Running,
                    orientationRemainingMillis = 0L,
                )
                emitCue(PlayerCueEvent.StepStarted(stepIndex = started.currentStepIndex, step = started.currentStep))
                started
            }
            SessionStatus.Running -> {
                emitCue(PlayerCueEvent.StepStopped)
                current.copy(status = SessionStatus.Paused)
            }
            SessionStatus.Paused -> {
                val resumed = current.copy(status = SessionStatus.Running)
                emitCue(
                    PlayerCueEvent.StepStarted(
                        stepIndex = resumed.currentStepIndex,
                        step = resumed.currentStep.copy(durationMillis = resumed.remainingStepMillis),
                    ),
                )
                resumed
            }
            SessionStatus.Complete -> {
                newPlayerSession(current.plan).withOrientationDelay()
            }
        }
        _sessionState.value = nextState
        if (_sessionState.value?.status.isClockActive()) {
            startTicker()
        } else {
            stopTicker()
        }
    }

    fun restart() {
        val current = _sessionState.value ?: return
        _sessionState.value = newPlayerSession(current.plan).withOrientationDelay()
        startTicker()
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = viewModelScope.launch {
            var lastTickMillis = SystemClock.elapsedRealtime()
            while (_sessionState.value?.status.isClockActive()) {
                delay(TickIntervalMillis)
                val nowMillis = SystemClock.elapsedRealtime()
                val deltaMillis = (nowMillis - lastTickMillis)
                    .coerceIn(0L, MaximumTickDeltaMillis)
                lastTickMillis = nowMillis

                val current = _sessionState.value ?: break
                if (current.status == SessionStatus.Preparing) {
                    val remainingOrientationMillis = (current.orientationRemainingMillis - deltaMillis).coerceAtLeast(0L)
                    if (remainingOrientationMillis == 0L) {
                        val started = current.copy(
                            status = SessionStatus.Running,
                            orientationRemainingMillis = 0L,
                        )
                        _sessionState.value = started
                        emitCue(PlayerCueEvent.StepStarted(stepIndex = started.currentStepIndex, step = started.currentStep))
                    } else {
                        _sessionState.value = current.copy(orientationRemainingMillis = remainingOrientationMillis)
                    }
                    continue
                }

                val advanced = advanceSession(current, deltaMillis)
                if (advanced.currentStepIndex != current.currentStepIndex && advanced.status != SessionStatus.Complete) {
                    emitCue(
                        PlayerCueEvent.StepStarted(
                            stepIndex = advanced.currentStepIndex,
                            step = advanced.currentStep,
                        ),
                    )
                }
                if (advanced.status == SessionStatus.Complete && current.status != SessionStatus.Complete) {
                    emitCue(PlayerCueEvent.SessionCompleted)
                }
                _sessionState.value = advanced
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun emitCue(event: PlayerCueEvent) {
        _cueEvents.tryEmit(event)
    }

    private fun PlayerSessionState.withOrientationDelay(): PlayerSessionState = copy(
        status = SessionStatus.Preparing,
        orientationRemainingMillis = OrientationDelayMillis,
    )

    private fun SessionStatus?.isClockActive(): Boolean {
        return this == SessionStatus.Preparing || this == SessionStatus.Running
    }

    private data class SessionConfigKey(
        val practiceId: String,
        val practiceHash: Int,
        val durationMinutes: Int,
        val visualModeName: String,
        val authoredDefinitionHash: Int?,
    ) {
        companion object {
            fun from(config: SessionConfig): SessionConfigKey = SessionConfigKey(
                practiceId = config.practice.safeId,
                practiceHash = config.practice.hashCode(),
                durationMinutes = config.durationMinutes.coerceAtLeast(1),
                visualModeName = config.visualMode.name,
                authoredDefinitionHash = config.authoredDefinition?.hashCode(),
            )
        }
    }

    private companion object {
        const val TickIntervalMillis = 50L
        const val MaximumTickDeltaMillis = 1_000L
        const val OrientationDelayMillis = 3_000L
        const val CueEventBufferCapacity = 8
    }
}
