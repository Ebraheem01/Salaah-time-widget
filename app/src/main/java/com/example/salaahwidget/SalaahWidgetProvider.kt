package com.example.salaahwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.ZonedDateTime

/**
 * Home screen widget showing today's prayer times for Cape Town.
 *
 * The widget refreshes when:
 *  - the system calls onUpdate (initial placement, periodic backup refresh),
 *  - the midnight alarm fires (see [AlarmScheduler]),
 *  - the widget is tapped or the app is opened,
 *  - the device reboots or the clock changes (see [BootReceiver]).
 */
class SalaahWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
        AlarmScheduler.scheduleNext(context)
    }

    override fun onEnabled(context: Context) {
        refreshAll(context)
        AlarmScheduler.scheduleNext(context)
    }

    override fun onDisabled(context: Context) {
        AlarmScheduler.cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH, AlarmScheduler.ACTION_MIDNIGHT_REFRESH -> {
                refreshAll(context)
                AlarmScheduler.scheduleNext(context)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.salaahwidget.action.REFRESH"

        /** Must stay in the same order as the rows in R.layout.widget_salaah. */
        private val timeViewIds = intArrayOf(
            R.id.widget_fajr_time,
            R.id.widget_sunrise_time,
            R.id.widget_dhuhr_time,
            R.id.widget_asr_time,
            R.id.widget_maghrib_time,
            R.id.widget_isha_time,
        )

        /** Rebuilds every placed widget. Safe to call from anywhere. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, SalaahWidgetProvider::class.java)
            )
            ids.forEach { id ->
                manager.updateAppWidget(id, buildViews(context))
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_salaah)
            val now = ZonedDateTime.now(PrayerTimesFactory.zone)
            val entries = PrayerTimesFactory.prayerTimesFor(now)

            views.setTextViewText(R.id.widget_date, PrayerTimesFactory.formatDate(now))
            entries.forEachIndexed { index, entry ->
                views.setTextViewText(timeViewIds[index], PrayerTimesFactory.formatTime(entry.time))
            }

            // Highlight the next prayer in amber.
            val nowMillis = System.currentTimeMillis()
            val nextIndex = entries.indexOfFirst { it.isPrayer && it.time.time > nowMillis }
            if (nextIndex >= 0) {
                views.setTextColor(timeViewIds[nextIndex], context.getColor(R.color.accent))
            }

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)

            return views
        }
    }
}
