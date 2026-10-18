# Salaah Cape Town — Android home screen widget

A home-screen widget showing today's prayer times for Cape Town. Times are
computed entirely on-device with the [Adhan](https://github.com/batoulapps/adhan-java)
library — the app has no internet permission at all.

- Method: **Muslim World League** (Fajr 18°, Isha 17°)
- Asr: **Standard / Shafi'i**
- Timezone: `Africa/Johannesburg` (SAST) — correct even if the phone is set to another zone
- Rows: Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha, plus a featured next-prayer card
- Two responsive layouts: a compact timetable and a larger layout with mosque artwork and Qur'an quote
- The next-prayer countdown uses Android's host-side Chronometer; the app does not wake every minute
- Refreshes at prayer boundaries and just after midnight

## Build

Requirements: JDK 17 and Android SDK 35 (Android Studio Meerkat/Ladybug or newer
recommended).

- **Android Studio:** open the `salaah-widget` folder, let it sync, press Run.
- **CLI:** `./gradlew assembleDebug`
  → APK at `app/build/outputs/apk/debug/app-debug.apk`

> This scaffold was generated without a JDK or Android SDK available, so it has
> not been compiled here. The first sync downloads Gradle 8.9, AGP 8.7.3 and the
> Kotlin 2.0.21 plugin. If `gradlew` is missing (for example after copying the
> sources without binaries), run `gradle wrapper` once or let Android Studio
> regenerate it.

### Build in the cloud (GitHub Actions)

Push this folder to a GitHub repository and the included workflow
(`.github/workflows/build.yml`) builds the debug APK automatically:

1. Create an **empty** repository on GitHub (do not add a README or license).
2. From this folder:

   ```sh
   git init
   git config user.name "Your Name"
   git config user.email "you@example.com"
   git add .
   git commit -m "Salaah Cape Town widget"
   git branch -M main
   git remote add origin https://github.com/<your-username>/salaah-widget.git
   git push -u origin main
   ```

   When git asks for a password, use a personal access token with `repo` and
   `workflow` scopes (not your account password).

3. Watch the run in the repository's **Actions** tab (first build ~2 minutes).
   When it finishes, the APK is attached as `app-debug.apk` to a new entry in
   the **Releases** section — download it straight to your phone and tap it to
   install (allow "install unknown apps" for your browser when asked).

## Install and add the widget

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then long-press your home screen → **Widgets** → **Salaah Cape Town** → drag
it out. The compact layout needs about 220 × 315 dp; enlarge it to around
240 × 400 dp to show the mosque artwork and quote. Launcher grid sizes vary.

## How the midnight refresh works

Android widgets cannot run code on their own, so the app arms a single alarm:

1. `AlarmScheduler` sets one alarm for the next Cape Town midnight and one
   for the next prayer boundary.
2. When it fires, `SalaahWidgetProvider` recomputes the times, repaints every
   placed widget, and arms the following midnight.
3. `BootReceiver` re-arms the alarm after reboots, app updates, clock/timezone
   changes, and right after the exact-alarm permission is granted.
4. `updatePeriodMillis` (6 hours) in `res/xml/salaah_widget_info.xml` is a
   belt-and-braces backup refresh.

Notes:

- **Android 12+** requires the "Alarms & reminders" special access
  (`SCHEDULE_EXACT_ALARM`). The app shows a button to grant it. If the user
  denies it, the app falls back to an inexact wake-up around midnight, which may
  fire a few minutes late while the phone is idle. Since prayer times change by
  roughly a minute per day, a small delay is harmless.
- Opening the app refreshes the widget immediately, which is also the easiest
  way to force an update while testing.

## Customize

| What | Where |
| --- | --- |
| Coordinates, calculation method, madhab | `PrayerTimesFactory.kt` |
| 12-hour clock | change `"HH:mm"` to `"h:mm a"` in `PrayerTimesFactory.kt` |
| Widget and app colours | `res/values/colors.xml` |
| Widget size / preview | `res/xml/salaah_widget_info.xml` |
| Package name | `applicationId` + `namespace` in `app/build.gradle.kts`, the `com.example.salaahwidget.*` action strings, and the Kotlin package folder |

Calculation methods available in the Adhan library: `MUSLIM_WORLD_LEAGUE`,
`MOON_SIGHTING_COMMITTEE`, `EGYPTIAN`, `KARACHI`, `UMM_AL_QURA`, `DUBAI`,
`QATAR`, `KUWAIT`, `SINGAPORE`, `NORTH_AMERICA`, `OTHER`.

To fine-tune towards a local timetable, apply per-prayer minute offsets:

```kotlin
private val parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.apply {
    madhab = Madhab.SHAFI
    adjustments.fajr = 2   // for example
}
```

## Accuracy note

These are astronomical calculations, not local observations, so they can differ
by a few minutes from your masjid's timetable (such as the MJC timetable).
Please verify against the timetable you follow, especially the Fajr and Isha
boundaries, and use `adjustments` if you need to match it exactly.

## Project layout

```
salaah-widget/
├── settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew, gradle/
└── app/
    ├── build.gradle.kts                     # module config, Adhan dependency
    └── src/main/
        ├── AndroidManifest.xml              # permissions, receivers, widget
        ├── java/com/example/salaahwidget/
        │   ├── PrayerTimesFactory.kt        # coordinates, method, formatting
        │   ├── AlarmScheduler.kt            # midnight alarm arm/cancel
        │   ├── SalaahWidgetProvider.kt      # widget rendering + updates
        │   ├── BootReceiver.kt              # re-arm after boot/time changes
        │   └── MainActivity.kt              # today's times + permission UI
        └── res/
            ├── layout/widget_compact.xml    # smaller widget layout
            ├── layout/widget_premium.xml    # larger artwork layout
            ├── layout/widget_salaah.xml     # legacy layout, no longer selected
            ├── layout/activity_main.xml     # app screen
            ├── xml/salaah_widget_info.xml   # widget metadata (size, refresh)
            ├── drawable/, mipmap-anydpi-v26/, values/
```

## Test tips

- Tap the widget (or just open the app) to force an immediate refresh.
- To exercise the midnight path in a minute, temporarily change
  `nextMidnightMillis()` in `PrayerTimesFactory.kt` from `plusDays(1)` to
  `plusMinutes(1)`, then reinstall.
