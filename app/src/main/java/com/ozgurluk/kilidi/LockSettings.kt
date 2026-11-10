package com.ozgurluk.kilidi

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Calendar

/**
 * Tüm kilit ayarları burada kalıcı olarak (SharedPreferences) saklanır.
 * Eskiden bunlar `companion object` içindeki static değişkenlerdi; uygulama süreci
 * Android tarafından sonlandırıldığında (çok sık olur) kilit ve saatler kayboluyordu.
 */
object LockSettings {

    const val MAX_APPS = 6
    const val NOT_SET = -1
    const val MIN_PASSWORD_LENGTH = 4

    /** Art arda bu kadar hatalı şifre girilince bekleme süresi başlar. */
    const val MAX_FAILED_ATTEMPTS = 3
    private const val LOCKOUT_MS = 30_000L

    private const val PREFS_NAME = "OzgurlukKilidiPrefs"
    private const val KEY_ACTIVE = "scheduler_active"
    private const val KEY_START = "start_minutes"
    private const val KEY_END = "end_minutes"
    private const val KEY_DAYS = "selected_days"
    private const val KEY_APPS = "whitelist_apps"
    private const val KEY_PW_HASH = "password_hash"
    private const val KEY_PW_SALT = "password_salt"
    private const val KEY_FAILED = "failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "lockout_until"

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

    // ---- Şifre ------------------------------------------------------------------------------
    // Şifre artık kaynak kodda değil, cihazda tuzlu SHA-256 özeti olarak saklanır.

    fun hasPassword(context: Context): Boolean =
        prefs(context).getString(KEY_PW_HASH, null) != null

    fun setPassword(context: Context, password: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs(context).edit {
            putString(KEY_PW_SALT, encode(salt))
            putString(KEY_PW_HASH, encode(hash(password, salt)))
            putInt(KEY_FAILED, 0)
            putLong(KEY_LOCKOUT_UNTIL, 0L)
        }
    }

    fun checkPassword(context: Context, password: String): Boolean {
        val p = prefs(context)
        val storedHash = p.getString(KEY_PW_HASH, null) ?: return false
        val salt = p.getString(KEY_PW_SALT, null)?.let { decode(it) } ?: return false
        return MessageDigest.isEqual(hash(password, salt), decode(storedHash))
    }

    /** Hatalı denemeden sonra kalan bekleme süresi (ms). 0 ise şifre girilebilir. */
    fun lockoutRemainingMs(context: Context): Long =
        (prefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    /** Hatalı denemeyi kaydeder; sınır aşıldıysa bekleme süresini başlatır. Kalan hakkı döndürür. */
    fun registerFailedAttempt(context: Context): Int {
        val p = prefs(context)
        val failed = p.getInt(KEY_FAILED, 0) + 1
        if (failed >= MAX_FAILED_ATTEMPTS) {
            p.edit {
                putInt(KEY_FAILED, 0)
                putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_MS)
            }
            return 0
        }
        p.edit { putInt(KEY_FAILED, failed) }
        return MAX_FAILED_ATTEMPTS - failed
    }

    fun resetFailedAttempts(context: Context) =
        prefs(context).edit { putInt(KEY_FAILED, 0); putLong(KEY_LOCKOUT_UNTIL, 0L) }

    private fun hash(password: String, salt: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").run {
            update(salt)
            digest(password.toByteArray(Charsets.UTF_8))
        }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

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

    /** Bir kilit oturumunun toplam uzunluğu (saniye). */
    fun windowLengthSeconds(context: Context): Long {
        val start = startMinutes(context)
        val end = endMinutes(context)
        if (start == NOT_SET || end == NOT_SET || start == end) return 0L
        return ((end - start + 1440) % 1440) * 60L
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
