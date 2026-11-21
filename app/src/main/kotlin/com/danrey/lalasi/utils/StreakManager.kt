// StreakManager.kt

package com.danrey.lalasi.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate

/**
 * Listening streak, in the TikTok way: a day counts once you have really listened (at least
 * [MinListenMillisPerDay] of music that day), and the streak grows by one for every day in a row.
 * Miss a whole day and it starts over.
 *
 * Everything lives in the app's existing DataStore (no database migration). The UI reads it through
 * [rememberStreak], which follows [PreferencesCache], so the card on Home updates on its own.
 */
object StreakManager {
    /** one minute of music in a day is enough to keep the flame alive */
    const val MinListenMillisPerDay = 60_000L

    private val StreakCurrentKey = intPreferencesKey("streakCurrent")
    private val StreakBestKey = intPreferencesKey("streakBest")

    /** the last day (epoch day) that reached the minimum */
    private val StreakLastDayKey = longPreferencesKey("streakLastDay")

    /** the day [StreakTodayMsKey] belongs to */
    private val StreakTodayDayKey = longPreferencesKey("streakTodayDay")
    private val StreakTodayMsKey = longPreferencesKey("streakTodayMs")

    /** the recent days that counted, as epoch days joined by commas (the week row on the card) */
    private val StreakDaysKey = stringPreferencesKey("streakDays")

    /** a milestone reached but not celebrated yet; 0 = none */
    private val StreakPendingKey = intPreferencesKey("streakPendingMilestone")

    private const val KeepDays = 60

    /** 3, 10, 30, 100 and then every 100: 200, 300, 400 ... */
    fun isMilestone(days: Int): Boolean = days == 3 || days == 10 || days == 30 || (days >= 100 && days % 100 == 0)

    /** the next milestone after [days] */
    fun nextMilestone(days: Int): Int = when {
        days < 3 -> 3
        days < 10 -> 10
        days < 30 -> 30
        days < 100 -> 100
        else -> (days / 100 + 1) * 100
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    /**
     * Called whenever a song has been played for [playedMillis]. Cheap and safe from any thread;
     * a no-op for tiny values.
     */
    suspend fun recordListening(context: Context, playedMillis: Long) {
        if (playedMillis <= 0L) return
        val day = today()
        context.applicationContext.dataStore.edit { prefs ->
            val sameDay = prefs[StreakTodayDayKey] == day
            val total = (if (sameDay) prefs[StreakTodayMsKey] ?: 0L else 0L) + playedMillis
            prefs[StreakTodayDayKey] = day
            prefs[StreakTodayMsKey] = total

            val last = prefs[StreakLastDayKey] ?: Long.MIN_VALUE
            if (total < MinListenMillisPerDay || last == day) return@edit

            // today counts: one more day when yesterday counted too, otherwise a fresh start
            val next = if (last == day - 1) (prefs[StreakCurrentKey] ?: 0) + 1 else 1
            prefs[StreakCurrentKey] = next
            prefs[StreakBestKey] = maxOf(prefs[StreakBestKey] ?: 0, next)
            prefs[StreakLastDayKey] = day
            prefs[StreakDaysKey] = (parseDays(prefs[StreakDaysKey]) + day)
                .distinct()
                .sorted()
                .takeLast(KeepDays)
                .joinToString(",")
            if (isMilestone(next)) prefs[StreakPendingKey] = next
        }
    }

    /** the celebration has been shown */
    suspend fun clearPendingMilestone(context: Context) {
        context.applicationContext.dataStore.edit { it.remove(StreakPendingKey) }
    }

    private fun parseDays(raw: String?): List<Long> =
        raw.orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }

    /** what the UI needs, worked out from the stored values for [day] (so a missed day shows as 0 at once) */
    fun stateOf(prefs: Preferences, day: Long = today()): StreakState {
        val last = prefs[StreakLastDayKey] ?: Long.MIN_VALUE
        val stored = prefs[StreakCurrentKey] ?: 0
        val doneToday = last == day
        // alive while today or yesterday counted; after that the flame is out
        val alive = doneToday || last == day - 1
        val current = if (alive) stored else 0
        val days = parseDays(prefs[StreakDaysKey]).toHashSet()
        val todayMs = if (prefs[StreakTodayDayKey] == day) prefs[StreakTodayMsKey] ?: 0L else 0L
        return StreakState(
            current = current,
            best = maxOf(prefs[StreakBestKey] ?: 0, current),
            doneToday = doneToday,
            todayProgress = (todayMs.toFloat() / MinListenMillisPerDay).coerceIn(0f, 1f),
            listenedDays = days,
            today = day,
            pendingMilestone = prefs[StreakPendingKey] ?: 0,
        )
    }
}

class StreakState(
    val current: Int,
    val best: Int,
    val doneToday: Boolean,
    /** 0..1 of today's minute; a full bar means today is safe */
    val todayProgress: Float,
    val listenedDays: Set<Long>,
    val today: Long,
    val pendingMilestone: Int,
) {
    /** true when the flame is lit now, false when it is waiting for today's first minute (or gone) */
    val lit: Boolean get() = doneToday

    /** days until the next milestone */
    val daysToNext: Int get() = StreakManager.nextMilestone(current) - current
}

@Composable
fun rememberStreak(): androidx.compose.runtime.State<StreakState> {
    val context = androidx.compose.ui.platform.LocalContext.current
    PreferencesCache.current(context.applicationContext.dataStore)
    return remember {
        derivedStateOf { StreakManager.stateOf(PreferencesCache.snapshot.value) }
    }
}
