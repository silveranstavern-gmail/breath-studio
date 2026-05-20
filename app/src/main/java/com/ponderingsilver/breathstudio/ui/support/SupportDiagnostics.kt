package com.ponderingsilver.breathstudio.ui.support

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SupportDiagnosticEntry(
    val timestampMillis: Long,
    val title: String,
    val detail: String,
) {
    val timestampLabel: String
        get() = SupportTimestampFormatter.format(Date(timestampMillis))
}

object SupportDiagnostics {
    private val _entries = MutableStateFlow<List<SupportDiagnosticEntry>>(emptyList())
    val entries: StateFlow<List<SupportDiagnosticEntry>> = _entries.asStateFlow()

    fun log(
        title: String,
        detail: String,
    ) {
        val entry = SupportDiagnosticEntry(
            timestampMillis = System.currentTimeMillis(),
            title = title,
            detail = detail,
        )
        _entries.value = (_entries.value + entry).takeLast(MaxEntries)
    }

    fun clear() {
        _entries.value = emptyList()
        log("Diagnostics", "Log cleared.")
    }

    fun environmentSnapshot(context: Context): List<Pair<String, String>> {
        val packageManager = context.packageManager
        val versionInfo = appVersion(context)
        return listOf(
            "App Id" to context.packageName,
            "Version" to versionInfo,
            "SDK" to "${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE ?: "unknown"})",
            "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Installer" to installerPackageName(context),
            "Play Store" to packageState(packageManager, PlayStorePackage),
            "Play Services" to packageState(packageManager, PlayServicesPackage),
        )
    }

    private fun appVersion(context: Context): String {
        return runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName ?: "unknown"} (${packageInfo.longVersionCode})"
        }.getOrElse {
            "unknown"
        }
    }

    private fun installerPackageName(context: Context): String {
        return runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        }.getOrNull().orEmpty().ifBlank { "unknown" }
    }

    private fun packageState(
        packageManager: PackageManager,
        packageName: String,
    ): String {
        return runCatching {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "unknown"
            "installed ($versionName)"
        }.getOrElse {
            "not installed"
        }
    }

    private const val MaxEntries = 250
    private const val PlayStorePackage = "com.android.vending"
    private const val PlayServicesPackage = "com.google.android.gms"
}

private val SupportTimestampFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
