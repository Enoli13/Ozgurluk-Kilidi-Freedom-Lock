package com.ozgurluk.kilidi

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Calendar

/**
 * Tüm kilit ayarları burada kalıcı olarak (SharedPreferences) saklanır.
 * Eskiden bunlar `companion object` içindeki static değişkenlerdi; uygulama süreci
 * Android tarafından sonlandırıldığında (çok sık olur) kilit ve saatler kayboluyordu.
 */
object LockSettings {

    const val NOT_SET = -1

    /** Takvim bağlılığı türleri. */
    const val COMMIT_NONE = 0
    const val COMMIT_UNTIL = 1 // "Şu güne kadar"
    const val COMMIT_DATES = 2 // "Şu günlerde"

    private const val PREFS_NAME = "OzgurlukKilidiPrefs"
    private const val KEY_ACTIVE = "scheduler_active"
    private const val KEY_START = "start_minutes"
    private const val KEY_END = "end_minutes"
    private const val KEY_DAYS = "selected_days"
    private const val KEY_APPS = "whitelist_apps"
    private const val KEY_COMMIT_MODE = "commit_mode"
    private const val KEY_COMMIT_UNTIL = "commit_until"
    private const val KEY_COMMIT_DATES = "commit_dates"

    /**
     * Serbest listeye eklenemeyen uygulamalar: sosyal medya (WhatsApp hariç) ve kilidi kapatmaya
     * yarayan sistem Ayarları.
     */
    val NOT_WHITELISTABLE = setOf(
        // Instagram / Threads
        "com.instagram.android", "com.instagram.lite", "com.instagram.barcelona",
        // Facebook / Messenger
        "com.facebook.katana", "com.facebook.lite", "com.facebook.orca", "com.facebook.mlite",
        // TikTok
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go",
        "com.ss.android.ugc.tiktok.lite",
        // X (Twitter)
        "com.twitter.android", "com.twitter.android.lite",
        // Snapchat
        "com.snapchat.android",
        // YouTube
        "com.google.android.youtube",
        // Reddit, Pinterest, Tumblr, LinkedIn
        "com.reddit.frontpage", "com.pinterest", "com.tumblr", "com.linkedin.android",
        // Telegram, Discord
        "org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram",
        "com.discord",
        // Twitch, Bluesky, Mastodon, BeReal, VK, Likee, Lemon8
        "tv.twitch.android.app", "xyz.blueskyweb.app", "org.joinmastodon.android",
        "com.bereal.ft", "com.vkontakte.android", "video.like", "com.bd.nproject",
        // Ayarlar: serbest olursa Erişilebilirlik servisi kapatılıp kilit aşılabilir
        "com.android.settings"
    )

    fun isWhitelistable(pkg: String): Boolean = pkg !in NOT_WHITELISTABLE

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

    /** Calendar.MONDAY vb. değerleri. Kurulum için en az bir gün seçilmesi zorunludur. */
    fun days(context: Context): Set<Int> =
        prefs(context).getStringSet(KEY_DAYS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

    fun setDays(context: Context, days: Set<Int>) =
        prefs(context).edit { putStringSet(KEY_DAYS, days.map { it.toString() }.toSet()) }

    /** Serbest uygulamalar. Eski sürümlerde seçilmiş sosyal medya uygulamaları elenir. */
    fun allowedApps(context: Context): Set<String> =
        (prefs(context).getStringSet(KEY_APPS, emptySet()) ?: emptySet())
            .filter { isWhitelistable(it) }
            .toSet()

    fun setAllowedApps(context: Context, apps: Set<String>) =
        prefs(context).edit { putStringSet(KEY_APPS, apps.filter { isWhitelistable(it) }.toSet()) }

    /** Eski sürümlerin şifre verilerini cihazdan siler. */
    fun clearLegacyPasswordData(context: Context) = prefs(context).edit {
        remove("password_hash")
        remove("password_salt")
        remove("failed_attempts")
        remove("lockout_until")
    }

    // ---- Takvim bağlılığı -------------------------------------------------------------------

    fun commitMode(context: Context): Int = prefs(context).getInt(KEY_COMMIT_MODE, COMMIT_NONE)

    fun setCommitMode(context: Context, mode: Int) =
        prefs(context).edit { putInt(KEY_COMMIT_MODE, mode) }

    /** "Şu güne kadar" tarihi (o gün dahil). */
    fun commitUntil(context: Context): LocalDate? =
        prefs(context).getString(KEY_COMMIT_UNTIL, null)?.let { parseDate(it) }

    fun setCommitUntil(context: Context, date: LocalDate?) =
        prefs(context).edit { putString(KEY_COMMIT_UNTIL, date?.toString()) }

    /** "Şu günlerde" seçilen tarihler. */
    fun commitDates(context: Context): Set<LocalDate> =
        (prefs(context).getStringSet(KEY_COMMIT_DATES, emptySet()) ?: emptySet())
            .mapNotNull { parseDate(it) }
            .toSet()

    fun setCommitDates(context: Context, dates: Set<LocalDate>) =
        prefs(context).edit { putStringSet(KEY_COMMIT_DATES, dates.map { it.toString() }.toSet()) }

    /**
     * Zamanlayıcı şu an kapatılamaz durumda mı? Kurulu zamanlayıcı, "şu güne kadar" seçildiyse
     * o tarih bitene kadar, "şu günlerde" seçildiyse o tarihlerin her birinde durdurulamaz.
     */
    fun isCommittedNow(context: Context, today: LocalDate = LocalDate.now()): Boolean =
        when (commitMode(context)) {
            COMMIT_UNTIL -> commitUntil(context)?.let { !today.isAfter(it) } ?: false
            COMMIT_DATES -> today in commitDates(context)
            else -> false
        }

    private fun parseDate(text: String): LocalDate? = try {
        LocalDate.parse(text)
    } catch (e: DateTimeParseException) {
        null
    }

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

        // Eski sürümlerden kalan "gün seçilmedi = her gün" kurulumları çalışmaya devam etsin.
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
