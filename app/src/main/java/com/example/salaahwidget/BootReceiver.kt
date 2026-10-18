package com.example.salaahwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the midnight alarm after reboots, app updates, clock or timezone
 * changes, and after the user grants the exact-alarm permission.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_EXACT_ALARM_PERMISSION_CHANGED -> {
                SalaahWidgetProvider.refreshAll(context)
                AlarmScheduler.scheduleNext(context)
            }
        }
    }

    private companion object {
        const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
