package com.example.salaahwidget

import android.app.Activity
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Shows today's times, the next prayer, and the exact-alarm permission state.
 * Opening the app also refreshes the widget and re-arms the midnight alarm.
 */
class MainActivity : Activity() {

    companion object {
        const val EXTRA_OPEN_SETTINGS = "com.example.salaahwidget.extra.OPEN_SETTINGS"
    }

    private val timeRowIds = mapOf(
        "Fajr" to R.id.main_fajr_time,
        "Sunrise" to R.id.main_sunrise_time,
        "Dhuhr" to R.id.main_dhuhr_time,
        "Asr" to R.id.main_asr_time,
        "Maghrib" to R.id.main_maghrib_time,
        "Isha" to R.id.main_isha_time,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.button_exact_alarm).setOnClickListener {
            requestExactAlarmAccess()
        }
    }

    override fun onResume() {
        super.onResume()
        showToday()
        SalaahWidgetProvider.refreshAll(this)
        AlarmScheduler.scheduleNext(this)
        updateExactAlarmUi()
        if (intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)) {
            findViewById<View>(R.id.main_status).post {
                findViewById<View>(R.id.main_scroll).scrollTo(0, findViewById<View>(R.id.main_status).top)
            }
            intent.removeExtra(EXTRA_OPEN_SETTINGS)
        }
    }

    private fun showToday() {
        val now = ZonedDateTime.now(PrayerTimesFactory.zone)
        val entries = PrayerTimesFactory.prayerTimesFor(now)

        findViewById<TextView>(R.id.main_date).text = PrayerTimesFactory.formatLongDate(now)
        entries.forEach { entry ->
            timeRowIds[entry.name]?.let { id ->
                findViewById<TextView>(id).text = PrayerTimesFactory.formatTime(entry.time)
            }
        }

        val next = PrayerTimesFactory.nextPrayer(now)
        val nextLabel: String = if (!next.isTomorrow) {
            val minutes = Duration.between(now, next.entry.time.toInstant()).toMinutes()
            getString(
                R.string.next_prayer,
                next.entry.name,
                PrayerTimesFactory.formatTime(next.entry.time),
                humanizeMinutes(minutes),
            )
        } else {
            getString(
                R.string.next_prayer_tomorrow,
                next.entry.name,
                PrayerTimesFactory.formatTime(next.entry.time),
            )
        }
        findViewById<TextView>(R.id.main_next).text = nextLabel
    }

    private fun humanizeMinutes(totalMinutes: Long): String = when {
        totalMinutes < 1 -> getString(R.string.duration_under_minute)
        totalMinutes < 60 -> getString(R.string.duration_minutes, totalMinutes)
        else -> getString(
            R.string.duration_hours_minutes,
            totalMinutes / 60,
            totalMinutes % 60,
        )
    }

    private fun updateExactAlarmUi() {
        val status = findViewById<TextView>(R.id.main_status)
        val button = findViewById<Button>(R.id.button_exact_alarm)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            val allowed = alarmManager.canScheduleExactAlarms()
            status.text = getString(
                if (allowed) R.string.exact_alarm_granted else R.string.exact_alarm_denied
            )
            button.visibility = if (allowed) View.GONE else View.VISIBLE
        } else {
            status.text = getString(R.string.exact_alarm_not_needed)
            button.visibility = View.GONE
        }
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName"),
                )
            )
        }
    }
}
