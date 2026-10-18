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
    private const val PRAYER_REQUEST_CODE = 1002
    const val ACTION_PRAYER_REFRESH = "com.example.salaahwidget.action.PRAYER_REFRESH"

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
        alarmManager.cancel(prayerPendingIntent(context))
    }

    /** Refresh the featured prayer when the current one starts. */
    fun scheduleNextPrayer(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = PrayerTimesFactory.nextPrayer().entry.time.time + 1000L
        val pendingIntent = prayerPendingIntent(context)
        val exactAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exactAllowed) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun prayerPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SalaahWidgetProvider::class.java).setAction(ACTION_PRAYER_REFRESH)
        return PendingIntent.getBroadcast(
            context, PRAYER_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
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
