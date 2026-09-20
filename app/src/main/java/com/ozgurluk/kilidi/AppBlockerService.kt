package com.ozgurluk.kilidi

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.telecom.TelecomManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * Kilit penceresi içindeyken, izin verilmeyen bir uygulamanın penceresi açılırsa
 * kullanıcıyı [MainActivity] (grid ekranı) üzerine geri gönderir.
 *
 * Tüm ayarlar [LockSettings] üzerinden (kalıcı) okunur; süreç öldürülse ya da telefon yeniden
 * başlasa bile kilit ayarları kaybolmaz.
 */
class AppBlockerService : AccessibilityService() {

    private var lastBounceAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (pkg == packageName) return // kendi ekranımız
        if (!LockSettings.isActive(this) || !LockSettings.isInLockWindow(this)) return
        if (pkg in LockSettings.allowedApps(this)) return // gridde seçilen serbest uygulamalar
        if (isHelperWindow(pkg, event.className?.toString())) return // klavye, izin diyalogları vb.

        Log.d(TAG, "Engellendi: $pkg (${event.className})")
        bringLockScreenToFront()
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
        startActivity(lockIntent)
    }

    override fun onInterrupt() {}

    private companion object {
        const val TAG = "OzgurlukKilidi"
        const val BOUNCE_DEBOUNCE_MS = 400L

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
