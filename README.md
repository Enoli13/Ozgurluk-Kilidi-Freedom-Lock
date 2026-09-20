# 🗽 Özgürlük Kilidi (Freedom Lock)

[🇹🇷 Türkçe](#-türkçe) · [🇬🇧 English](#-english)

---

# 🇹🇷 Türkçe

Özgürlük Kilidi, dijital detoks ve odaklanma için geliştirilmiş bir Android uygulamasıdır. Belirlediğin saat aralığında telefonda yalnızca önceden seçtiğin **en fazla 6 uygulama** kullanılabilir. Başka bir uygulamayı açmaya çalışırsan uygulama seni kilit ekranına geri gönderir.

## ✨ Özellikler

- **Serbest uygulamalar:** Telefondaki uygulamalar otomatik taranır, kilit sırasında açık kalacak en fazla 6 tanesini listeden seçersin. Kilit ekranında bu uygulamalar bir ikon ızgarası (grid) olarak görünür ve tüm özellikleriyle kullanılabilir (klavye, dosya/fotoğraf seçici, izin pencereleri dahil).
- **Akıllı zamanlayıcı:** Başlangıç ve bitiş saatini, ayrıca haftanın hangi günlerinde çalışacağını seçersin. Gün seçmezsen kilit her gün çalışır. Gece yarısını aşan aralıklar (örneğin 22:00 – 02:00) desteklenir.
- **Geri sayım:** Kilit sırasında bitişe kalan süre görünür. Süre dolunca kilit ekranı kapanır ve engelleme kendiliğinden durur.
- **Motivasyon kartı:** Kilit ekranında rastgele bir özlü söz gösterilir (yalnızca Türkçe).
- **Güvenlik şifresi:** Zamanlayıcıyı süre dolmadan durdurmak için şifre girmen gerekir.
- **Kalıcı ayarlar:** Saatler, günler ve seçilen uygulamalar telefon yeniden başlasa bile kaybolmaz.
- **Ayarlar kilitlenir:** Zamanlayıcı kuruluyken saat, gün ve uygulama seçimi değiştirilemez.
- **Gelen aramalar engellenmez:** Arama ekranı ve çalan alarm kilit yüzünden engellenmez.

## ⚙️ Nasıl çalışır?

Uygulama bir **Erişilebilirlik Servisi** kullanır. Servis, ekrana hangi uygulamanın penceresinin geldiğini (yalnızca paket adını) izler. Kilit saatlerinde seçilmemiş bir uygulama açılırsa Özgürlük Kilidi ekranını öne getirir.

> **Not:** Uygulama Geri, Ana Ekran ve Son Uygulamalar tuşlarını devre dışı bırakmaz. Bu tuşlarla başka bir uygulamaya geçersen kilit ekranına geri döndürülürsün.

## 🔒 Gizlilik

- Servis yalnızca açılan uygulamanın **paket adını** görür. Ekran içeriğini, yazdıklarını veya kişisel verileri okumaz.
- Uygulama **internet izni istemez**. Hiçbir veri telefondan dışarı çıkmaz.
- Tüm ayarlar yalnızca telefonda saklanır.

## 📋 Gereksinimler

- Android 10 (API 29) veya üzeri
- Derlemek için: [Android Studio](https://developer.android.com/studio)

## 🚀 Kurulum

### 1. Derle

Projeyi klonla ve Android Studio ile aç:

```bash
git clone https://github.com/Enoli13/Ozgurluk-Kilidi-Freedom-Lock.git
```

Ardından **Build → Build Bundle(s) / APK(s) → Build APK(s)** ile `app-debug.apk` dosyasını oluştur ya da terminalde şunu çalıştır:

```bash
./gradlew assembleDebug
```

APK `app/build/outputs/apk/debug/` klasöründe oluşur. Dosyayı telefonuna aktarıp kur.

### 2. Şifreni belirle (derlemeden önce)

Kilidi erken durdurmak için kullanılan şifre `MainActivity.kt` dosyasında `UNLOCK_PASSWORD` sabitinde tutulur:

```
app/src/main/java/com/ozgurluk/kilidi/MainActivity.kt
```

Derlemeden önce bunu kendi şifrenle değiştir. Repo herkese açıksa kaynak koddaki şifreyi herkes görebilir, başka yerlerde kullandığın bir şifreyi buraya yazma.

### 3. İzinleri ver

Uygulamayı açınca iki izin gerekir:

1. **Üstte Gösterim İzni:** Kilit ekranını diğer uygulamaların önüne getirmek için.
2. **Erişilebilirlik İzni:** Hangi uygulamanın açıldığını fark etmek için. Ayarlarda "Özgürlük Kilidi" servisini aç.

> **Android 13+ notu:** APK'yı Play Store dışından kurduysan Erişilebilirlik anahtarı gri görünebilir. Bu durumda **Ayarlar → Uygulamalar → Özgürlük Kilidi → sağ üstteki ⋮ menüsü → Kısıtlı ayarlara izin ver** seçeneğini kullan, sonra izni tekrar dene.
>
> Google Play Protect, Erişilebilirlik ve üstte gösterim izni isteyen uygulamalar için kurulum sırasında uyarı gösterebilir.

### 4. Kullan

1. **Detoks Dışı Uygulamaları Seç** düğmesiyle en fazla 6 uygulama seç ve kaydet.
2. Başlangıç ve bitiş saatini, istersen günleri seç.
3. **ZAMANLAYICIYI AKTİF ET** düğmesine bas.

Zamanlayıcı kuruluyken uygulamayı açarsan bir sonraki kilide kalan süreyi görürsün. Kilit saati gelince seçilmemiş bir uygulamayı açtığın anda kilit ekranı devreye girer.

## 🛠️ Sorun giderme

**Bir uygulama içinde kilit ekranına atılıyorum (örneğin başka bir cihazda farklı bir dosya seçici):**
Bazı üreticiler kendi sistem pencerelerini kullanır. Android Studio'da Logcat'i `OzgurlukKilidi` etiketiyle filtrele. Kilit ekranına atıldığın anda şuna benzer bir satır çıkar:

```
Engellendi: paket.adi (sinif.adi)
```

Buradaki paket adını `AppBlockerService.kt` dosyasındaki `ALWAYS_ALLOWED` listesine ekleyip yeniden derle.

## ⚠️ Sınırlar

Özgürlük Kilidi bir **öz disiplin aracıdır**, güvenlik duvarı değildir. Uygulamayı kaldırmak, Erişilebilirlik servisini kapatmak ya da telefonu yeniden başlatıp güvenli moda almak kilidi aşar. Amaç seni engellemek değil, dikkatini dağıtan uygulamaya uzanma anında bir duraklama yaratmaktır.

## 📁 Proje yapısı

| Dosya | Görevi |
|---|---|
| `MainActivity.kt` | Ayar ve kilit ekranı, grid, geri sayım, şifre penceresi |
| `AppBlockerService.kt` | Erişilebilirlik servisi; izinsiz uygulamayı fark edip kilit ekranını öne getirir |
| `LockSettings.kt` | Kalıcı ayarlar ve zaman penceresi hesapları |

---

# 🇬🇧 English

Freedom Lock is an Android app for digital detox and focus. During the time window you set, only the **up to 6 apps** you pre-selected can be used. If you try to open anything else, the app sends you back to the lock screen.

## ✨ Features

- **Allowed apps:** The app scans your installed apps and lets you pick up to 6 to stay available during the lock. They appear as an icon grid on the lock screen and work with all their features (keyboard, file/photo pickers and permission dialogs included).
- **Smart scheduler:** Choose a start time, an end time, and the days of the week it should run. If no day is selected, it runs every day. Windows that cross midnight (e.g. 22:00 – 02:00) are supported.
- **Countdown:** The remaining time is shown during the lock. When it hits zero, the lock screen closes and blocking stops automatically.
- **Motivation card:** A random quote is shown on the lock screen (Turkish only).
- **Security password:** A password is required to stop the timer before it ends.
- **Persistent settings:** Times, days and selected apps survive a phone restart.
- **Settings locked while armed:** You can't change times, days or apps while the timer is set.
- **Calls are never blocked:** The call screen and ringing alarms are not blocked by the lock.

## ⚙️ How it works

The app uses an **Accessibility Service**. It watches which app's window comes to the screen (package name only). If a non-allowed app opens during lock hours, Freedom Lock brings its own screen to the front.

> **Note:** The app does not disable the Back, Home or Recents buttons. If you use them to switch to another app, you are sent back to the lock screen.

## 🔒 Privacy

- The service only sees the **package name** of the app that opened. It does not read screen content, what you type, or personal data.
- The app **does not request internet permission**. No data leaves your phone.
- All settings are stored on the device only.

## 📋 Requirements

- Android 10 (API 29) or newer
- To build: [Android Studio](https://developer.android.com/studio)

## 🚀 Installation

### 1. Build

Clone the project and open it in Android Studio:

```bash
git clone https://github.com/Enoli13/Ozgurluk-Kilidi-Freedom-Lock.git
```

Then use **Build → Build Bundle(s) / APK(s) → Build APK(s)** to create `app-debug.apk`, or run:

```bash
./gradlew assembleDebug
```

The APK is created in `app/build/outputs/apk/debug/`. Copy it to your phone and install it.

### 2. Set your password (before building)

The password used to stop the lock early is kept in the `UNLOCK_PASSWORD` constant in `MainActivity.kt`:

```
app/src/main/java/com/ozgurluk/kilidi/MainActivity.kt
```

Change it to your own password before building. If the repository is public, anyone can read the password in the source, so don't use a password you use elsewhere.

### 3. Grant permissions

When you open the app, two permissions are required:

1. **Display over other apps:** so the lock screen can appear on top of other apps.
2. **Accessibility:** to detect which app was opened. Turn on the "Özgürlük Kilidi" service in settings.

> **Android 13+ note:** If you installed the APK from outside the Play Store, the Accessibility switch may be greyed out. In that case go to **Settings → Apps → Özgürlük Kilidi → ⋮ menu (top right) → Allow restricted settings**, then try the permission again.
>
> Google Play Protect may show a warning during installation for apps that request Accessibility and overlay permissions.

### 4. Use it

1. Press **Select Whitelist Apps** and choose up to 6 apps, then save.
2. Choose the start time, the end time and, if you like, the days.
3. Press **ACTIVATE TIMER**.

While the timer is armed, opening the app shows the time left until the next lock. Once lock time arrives, the lock screen kicks in the moment you open a non-selected app.

## 🛠️ Troubleshooting

**I get sent back to the lock screen inside an allowed app (e.g. a different file picker on another device):**
Some manufacturers use their own system windows. In Android Studio, filter Logcat by the tag `OzgurlukKilidi`. When you're kicked to the lock screen, a line like this appears:

```
Engellendi: package.name (class.name)
```

Add that package name to the `ALWAYS_ALLOWED` list in `AppBlockerService.kt` and rebuild.

## ⚠️ Limits

Freedom Lock is a **self-discipline tool**, not a security barrier. Uninstalling the app, turning off the Accessibility service, or rebooting into safe mode bypasses the lock. The goal isn't to stop you by force, but to create a pause at the moment you reach for a distracting app.

## 📁 Project structure

| File | Purpose |
|---|---|
| `MainActivity.kt` | Settings and lock screen, grid, countdown, password dialog |
| `AppBlockerService.kt` | Accessibility service; detects non-allowed apps and brings the lock screen to the front |
| `LockSettings.kt` | Persistent settings and time-window calculations |
