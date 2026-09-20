package com.ozgurluk.kilidi

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Calendar

/**
 * Tüm kilit ayarları burada kalıcı olarak (SharedPreferences) saklanır.
 * Eskiden bunlar `companion object` içindeki static değişkenlerdi; uygulama süreci
 * Android tarafından sonlandırıldığında (çok sık olur) kilit ve saatler kayboluyordu.
 */
object LockSettings {

    const val MAX_APPS = 6
    const val NOT_SET = -1

    private const val PREFS_NAME = "OzgurlukKilidiPrefs"
    private const val KEY_ACTIVE = "scheduler_active"
    private const val KEY_START = "start_minutes"
    private const val KEY_END = "end_minutes"
    private const val KEY_DAYS = "selected_days"
    private const val KEY_APPS = "whitelist_apps"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- Ayarlar ----------------------------------------------------------------------------

    fun isActive(context: Context): Boolean = prefs(context).getBoolean(KEY_ACTIVE, false)

    fun setActive(context: Context, active: Boolean) =
        prefs(context).edit { putBoolean(KEY_ACTIVE, active) }

    /** Gece yarısından itibaren dakika (0..1439) ya da [NOT_SET]. */
    fun startMinutes(context: Context): Int = prefs(context).getInt(KEY_START, NOT_SET)

    fun endMinutes(context: Context): Int = prefs(context).getInt(KEY_END, NOT_SET)

    fun setStartMinutes(context: Context, minutes: Int) =
        prefs(context).edit { putInt(KEY_START, minutes) }

    fun setEndMinutes(context: Context, minutes: Int) =
        prefs(context).edit { putInt(KEY_END, minutes) }

    /** Calendar.MONDAY vb. değerleri. Boşsa "her gün" anlamına gelir. */
    fun days(context: Context): Set<Int> =
        prefs(context).getStringSet(KEY_DAYS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

    fun setDays(context: Context, days: Set<Int>) =
        prefs(context).edit { putStringSet(KEY_DAYS, days.map { it.toString() }.toSet()) }

    fun allowedApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_APPS, emptySet())?.toSet() ?: emptySet()

    fun setAllowedApps(context: Context, apps: Set<String>) =
        prefs(context).edit { putStringSet(KEY_APPS, apps.toSet()) }

    // ---- Zaman hesapları --------------------------------------------------------------------

    /** Şu an kilit penceresinin (başlangıç–bitiş, seçili günler) içinde miyiz? */
    fun isInLockWindow(context: Context, now: Calendar = Calendar.getInstance()): Boolean {
        val start = startMinutes(context)
        val end = endMinutes(context)
        if (start == NOT_SET || end == NOT_SET || start == end) return false

        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = now.get(Calendar.DAY_OF_WEEK)

        // Kilidin hangi güne "ait" olduğu: gece yarısını aşan pencerelerde (örn. 22:00–02:00)
        // gece yarısından sonraki kısım bir önceki günün oturumudur.
        val ownerDay: Int = if (start < end) {
            if (nowMinutes in start until end) today else return false
        } else {
            when {
                nowMinutes >= start -> today
                nowMinutes < end -> (today + 5) % 7 + 1 // dünün gün numarası
                else -> return false
            }
        }

        val days = days(context)
        return days.isEmpty() || ownerDay in days
    }

    /** Kilit penceresi içindeyken bitişe kalan saniye. */
    fun secondsUntilEnd(context: Context, now: Calendar = Calendar.getInstance()): Long {
        val end = endMinutes(context)
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, end / 60)
            set(Calendar.MINUTE, end % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return (target.timeInMillis - now.timeInMillis) / 1000
    }

    /** Kilit penceresi dışındayken bir sonraki başlangıca kalan saniye (yoksa null). */
    fun secondsUntilNextStart(context: Context, now: Calendar = Calendar.getInstance()): Long? {
        val start = startMinutes(context)
        if (start == NOT_SET) return null
        val days = days(context)

        for (offset in 0..7) {
            val candidate = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, start / 60)
                set(Calendar.MINUTE, start % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayMatches = days.isEmpty() || candidate.get(Calendar.DAY_OF_WEEK) in days
            if (candidate.after(now) && dayMatches) {
                return (candidate.timeInMillis - now.timeInMillis) / 1000
            }
        }
        return null
    }
}
