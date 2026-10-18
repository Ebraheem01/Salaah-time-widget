package com.example.salaahwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.os.Bundle
import android.os.SystemClock
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
 *  - midnight or a prayer-boundary alarm fires (see [AlarmScheduler]),
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
            appWidgetManager.updateAppWidget(id, buildViews(context, appWidgetManager, id))
        }
        AlarmScheduler.scheduleNext(context)
        AlarmScheduler.scheduleNextPrayer(context)
    }

    override fun onEnabled(context: Context) {
        refreshAll(context)
        AlarmScheduler.scheduleNext(context)
        AlarmScheduler.scheduleNextPrayer(context)
    }

    override fun onDisabled(context: Context) {
        AlarmScheduler.cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH, AlarmScheduler.ACTION_MIDNIGHT_REFRESH,
            AlarmScheduler.ACTION_PRAYER_REFRESH -> {
                refreshAll(context)
                AlarmScheduler.scheduleNext(context)
                AlarmScheduler.scheduleNextPrayer(context)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetManager, appWidgetId))
    }

    companion object {
        const val ACTION_REFRESH = "com.example.salaahwidget.action.REFRESH"

        /** Must stay in the same order as the rows in both widget layouts. */
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
                manager.updateAppWidget(id, buildViews(context, manager, id))
            }
            if (ids.isNotEmpty()) AlarmScheduler.scheduleNextPrayer(context)
        }

        private fun buildViews(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
        ): RemoteViews {
            val options = manager.getAppWidgetOptions(appWidgetId)
            val isCompact =
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) < 270 ||
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) < 410
            val layout = if (isCompact) R.layout.widget_compact else R.layout.widget_premium
            val views = RemoteViews(context.packageName, layout)
            val now = ZonedDateTime.now(PrayerTimesFactory.zone)
            val entries = PrayerTimesFactory.prayerTimesFor(now)

            views.setTextViewText(R.id.widget_date, PrayerTimesFactory.formatDate(now))
            entries.forEachIndexed { index, entry ->
                views.setTextViewText(timeViewIds[index], PrayerTimesFactory.formatTime(entry.time))
            }

            val next = PrayerTimesFactory.nextPrayer(now)
            val nextName = if (next.isTomorrow) {
                context.getString(R.string.tomorrow_prayer, next.entry.name)
            } else next.entry.name
            views.setTextViewText(R.id.widget_next_name, nextName)
            views.setTextViewText(R.id.widget_next_time, PrayerTimesFactory.formatTime(next.entry.time))
            val remainingMillis = (next.entry.time.time - System.currentTimeMillis()).coerceAtLeast(1000L)
            views.setChronometer(
                R.id.widget_countdown,
                SystemClock.elapsedRealtime() + remainingMillis,
                context.getString(R.string.widget_countdown_format),
                true,
            )
            views.setChronometerCountDown(R.id.widget_countdown, true)

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            val openSettings = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_menu, openSettings)

            return views
        }
    }
}
