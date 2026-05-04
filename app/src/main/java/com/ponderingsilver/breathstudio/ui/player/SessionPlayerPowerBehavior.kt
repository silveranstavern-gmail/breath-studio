package com.ponderingsilver.breathstudio.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@SuppressLint("WakelockTimeout")
@Composable
fun SessionPlayerPowerBehavior(active: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current.applicationContext
    val wakeLock = remember(context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "${context.packageName}:SessionPlayback",
        ).apply {
            setReferenceCounted(false)
        }
    }

    DisposableEffect(view, active) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = active || previousKeepScreenOn

        if (active && !wakeLock.isHeld) {
            wakeLock.acquire()
        }

        onDispose {
            view.keepScreenOn = previousKeepScreenOn
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
