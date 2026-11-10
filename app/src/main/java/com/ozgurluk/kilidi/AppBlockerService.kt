package com.ozgurluk.kilidi

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.telecom.TelecomManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

/**
 * Kilit penceresi içindeyken, izin verilmeyen bir uygulamanın penceresi açılırsa
 * kullanıcıyı [MainActivity] (grid ekranı) üzerine geri gönderir.
 *
 * Tüm ayarlar [LockSettings] üzerinden (kalıcı) okunur; süreç öldürülse ya da telefon yeniden
 * başlasa bile kilit ayarları kaybolmaz.
 */
class AppBlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastBounceAt = 0L

    /** Ekranda en son öne gelen "gerçek" uygulama (klavye, bildirim paneli vb. hariç). */
    private var foregroundPkg: String? = null

    /**
     * Pencere olayları yalnızca uygulama değişince gelir. Kilit saati başladığında yasaklı bir
     * uygulama zaten açıksa hiçbir olay gelmez; bu denetleyici o durumu da yakalar.
     */
    private val watchdog = object : Runnable {
        override fun run() {
            foregroundPkg?.let { pkg ->
                if (isUserLooking() && shouldBlock(pkg)) {
                    Log.d(TAG, "Engellendi (zaten açıktı): $pkg")
                    bringLockScreenToFront()
                }
            }
            handler.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.removeCallbacks(watchdog)
        handler.postDelayed(watchdog, WATCHDOG_INTERVAL_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val className = event.className?.toString()

        // Klavye, izin diyalogları, bildirim paneli gibi pencereler altta açık olan uygulamanın
        // "parçası" sayılır: ne engellenir ne de ön plandaki uygulama bilgisini değiştirir.
        if (isHelperWindow(pkg, className)) return

        foregroundPkg = pkg
        if (shouldBlock(pkg)) {
            Log.d(TAG, "Engellendi: $pkg ($className)")
            if (pkg in TAMPER_PACKAGES && !LockSettings.isInLockWindow(this)) {
                Toast.makeText(this, "Kilit kapatılamaz günlerde Ayarlar açılamaz.", Toast.LENGTH_SHORT).show()
            }
            bringLockScreenToFront()
        }
    }

    private fun shouldBlock(pkg: String): Boolean {
        if (pkg == packageName) return false // kendi ekranımız
        if (!LockSettings.isActive(this)) return false
        // Takvimle kapatılamaz günlerde kilit saatleri dışında da Ayarlar ve uygulama kaldırma
        // ekranı açılamaz; aksi halde Erişilebilirlik servisi kapatılarak kilit aşılabilirdi.
        if (pkg in TAMPER_PACKAGES && LockSettings.isCommittedNow(this)) return true
        if (!LockSettings.isInLockWindow(this)) return false
        return pkg !in LockSettings.allowedApps(this) // seçilen serbest uygulamalar
    }

    /** Ekran kapalıyken ya da telefon kilitliyken kilit ekranını açmaya çalışma. */
    private fun isUserLooking(): Boolean {
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive ?: true
        val keyguardLocked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked ?: false
        return interactive && !keyguardLocked
    }

    /**
     * Serbest bir uygulamanın "parçası" sayılan pencereler. Bunlar yüzünden kilit ekranına
     * geri atılmamalı; aksi halde örneğin WhatsApp'ta klavye açılınca grid ekranı geri gelir.
     */
    private fun isHelperWindow(pkg: String, className: String?): Boolean {
        if (pkg in ALWAYS_ALLOWED) return true
        // Klavye (IME) penceresi: sınıf adı ya da yüklü klavye paketlerinden biri
        if (className != null && className.startsWith("android.inputmethodservice.")) return true
        if (pkg in inputMethodPackages()) return true
        // Gelen/giden arama ekranı
        return pkg == defaultDialerPackage()
    }

    private fun inputMethodPackages(): Set<String> = try {
        getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.map { it.packageName }
            ?.toSet()
            ?: emptySet()
    } catch (e: Exception) {
        emptySet()
    }

    private fun defaultDialerPackage(): String? = try {
        getSystemService(TelecomManager::class.java)?.defaultDialerPackage
    } catch (e: Exception) {
        null
    }

    private fun bringLockScreenToFront() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBounceAt < BOUNCE_DEBOUNCE_MS) return
        lastBounceAt = now

        val lockIntent = Intent(this, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        try {
            startActivity(lockIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Kilit ekranı açılamadı", e)
        }
    }

    override fun onInterrupt() {}

    private companion object {
        const val TAG = "OzgurlukKilidi"
        const val BOUNCE_DEBOUNCE_MS = 400L
        const val WATCHDOG_INTERVAL_MS = 2_000L

        /** Kilidi kapatmaya / uygulamayı kaldırmaya yarayan ekranlar. */
        val TAMPER_PACKAGES = setOf(
            "com.android.settings",
            "com.miui.securitycenter",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
        )

        /**
         * Kilit sırasında da çalışması gereken sistem yardımcıları.
         *
         * Bir ekranda hâlâ grid'e geri atılıyorsan: `adb logcat -s OzgurlukKilidi` çalıştır,
         * "Engellendi: <paket>" satırındaki paket adını buraya ekle.
         */
        val ALWAYS_ALLOWED = setOf(
            // Bildirim çubuğu, hızlı ayarlar, ses/güç menüsü, ekran görüntüsü
            "com.android.systemui",
            // Sistem diyalogları ve paylaşım menüsü
            "android",
            "com.android.intentresolver",
            // İzin pencereleri (kamera, mikrofon, konum, bildirim...)
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.lbe.security.miui",
            // Dosya / medya seçiciler (WhatsApp'ta belge, fotoğraf eklemek için)
            "com.google.android.documentsui",
            "com.android.documentsui",
            "com.android.providers.media.module",
            "com.google.android.providers.media.module",
            "com.sec.android.app.myfiles",
            // Google Play hizmetleri diyalogları (konum doğruluğu, hesap seçici vb.)
            "com.google.android.gms",
            // Arama ekranı: gelen aramalar kilit yüzünden engellenmemeli
            "com.android.incallui",
            "com.android.server.telecom",
            "com.android.phone",
            "com.google.android.dialer",
            "com.android.dialer",
            "com.samsung.android.incallui",
            "com.samsung.android.dialer",
            // Çalan alarm ekranı
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.sec.android.app.clockpackage"
        )
    }
}
