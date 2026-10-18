package com.example.salaahwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules a single alarm at the next Cape Town midnight. When it fires, the
 * widget refreshes and schedules the following midnight. A one-shot alarm that
 * re-arms itself is preferred over a repeating alarm because exact repeating
 * alarms are not supported.
 */
object AlarmScheduler {

    const val ACTION_MIDNIGHT_REFRESH = "com.example.salaahwidget.action.MIDNIGHT_REFRESH"

    private const val REQUEST_CODE = 1001

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = midnightPendingIntent(context)
        val triggerAt = PrayerTimesFactory.nextMidnightMillis()

        val exactAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        if (exactAllowed) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        } else {
            // Exact alarms are not permitted, so refresh around midnight instead.
            // The system may deliver this a few minutes late while the phone is idle.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(midnightPendingIntent(context))
    }

    private fun midnightPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SalaahWidgetProvider::class.java)
            .setAction(ACTION_MIDNIGHT_REFRESH)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
