package com.datecalc.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

private const val PREFS = "datecalc_prefs"
private const val KEY_ASKED = "battery_opt_asked"

/** true when the system still throttles our background work (and so the daily widget refresh). */
fun isBatteryOptimized(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return !pm.isIgnoringBatteryOptimizations(context.packageName)
}

/**
 * Asks once, on first launch, to stop battery-optimizing the app. Without it the system may
 * force-stop us, which drops the pending midnight alarm and freezes the widget's day counter.
 */
@Composable
fun BatteryOptimizationPrompt() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var show by remember {
        mutableStateOf(!prefs.getBoolean(KEY_ASKED, false) && isBatteryOptimized(context))
    }

    if (!show) return

    fun dismiss() {
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        show = false
    }

    AlertDialog(
        onDismissRequest = { dismiss() },
        title = { Text("Разрешить работу в фоне") },
        text = {
            Text(
                "Приложению нужно разрешение на работу в фоне: раз в сутки оно пересчитывает " +
                    "даты в виджете. На расход заряда это почти не влияет — менее 0,01 % в день."
            )
        },
        confirmButton = {
            TextButton(onClick = {
                openBatterySettings(context)
                dismiss()
            }) { Text("Разрешить") }
        },
        dismissButton = {
            TextButton(onClick = { dismiss() }) { Text("Позже") }
        }
    )
}

private fun openBatterySettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val direct = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(direct)
        return
    } catch (_: Exception) {
    }
    // Some vendor ROMs hide the direct dialog - fall back to the system-wide list.
    try {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}
