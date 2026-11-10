# 🗽 Özgürlük Kilidi (Freedom Lock) — Android uygulama kilidi ve dijital detoks

> Özgürlük Kilidi uygulaması: telefon bağımlılığına karşı belirlediğin saatlerde yalnızca seçtiğin uygulamaları açık bırakan ücretsiz Android app blocker.

🌐 **Proje sayfası:** [https://enoli13.github.io/ozgurluk-kilidi/](https://enoli13.github.io/ozgurluk-kilidi/) · ⬇ [Son sürümü indir](https://github.com/Enoli13/Ozgurluk-Kilidi-Freedom-Lock/releases/latest)


[🇹🇷 Türkçe](#-türkçe) · [🇬🇧 English](#-english)

---

# 🇹🇷 Türkçe

Özgürlük Kilidi, dijital detoks ve odaklanma için geliştirilmiş bir Android uygulamasıdır. Belirlediğin saat aralığında telefonda yalnızca önceden seçtiğin **serbest uygulamalar** kullanılabilir. Başka bir uygulamayı açmaya çalışırsan uygulama seni kilit ekranına geri gönderir.

## ✨ Özellikler

- **Serbest uygulamalar:** Telefondaki uygulamalar otomatik taranır, kilit sırasında açık kalacakları listeden seçersin; kaç tane olacağına sen karar verirsin. Sosyal medya uygulamaları (WhatsApp hariç) ve Ayarlar serbest listeye eklenemez. Kilit ekranında bu uygulamalar bir ikon ızgarası (grid) olarak görünür ve tüm özellikleriyle kullanılabilir (klavye, dosya/fotoğraf seçici, izin pencereleri dahil).
- **Akıllı zamanlayıcı:** Başlangıç ve bitiş saatini, ayrıca haftanın hangi günlerinde çalışacağını seçersin. Saat aralığı ve en az bir gün seçmek zorunludur. Gece yarısını aşan aralıklar (örneğin 22:00 – 02:00) desteklenir.
- **Geri sayım:** Kilit sırasında bitişe kalan süre ve ilerleme çubuğu görünür. Süre dolunca kilit ekranı kapanır ve engelleme kendiliğinden durur.
- **Motivasyon kartı:** Kilit ekranında rastgele bir özlü söz gösterilir; dokununca değişir (yalnızca Türkçe).
- **Takvim (isteğe bağlı):** "Şu güne kadar" seçeneğinde takvimden bir tarih seçersin ve o gün bitene kadar zamanlayıcı hiçbir şekilde kapatılamaz. "Şu günlerde" seçeneğinde tarihleri tek tek eklersin ve o günlerde zamanlayıcı kapatılamaz. Bu günlerde Ayarlar ve uygulama kaldırma ekranı da açılamaz. Takvim seçilmezse zamanlayıcı bir onayla durdurulabilir.
- **Anında devreye girer:** Kilit saati başladığında yasaklı bir uygulama zaten açıksa birkaç saniye içinde kilit ekranına geçilir.
- **Durum takibi:** İzinlerin verilip verilmediği ve Erişilebilirlik servisi kapanırsa kilidin çalışmadığı ana ekranda gösterilir.
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

### 2. Takvim (isteğe bağlı)

Zamanlayıcıyı kapatılamaz yapmak için **4 · Takvim** kartından seçim yap:

- **Yok:** Zamanlayıcı bir onayla durdurulabilir.
- **Şu güne kadar:** Seçtiğin tarih (o gün dahil) bitene kadar zamanlayıcı kapatılamaz, ayarlar değiştirilemez.
- **Şu günlerde:** Eklediğin her tarihte zamanlayıcı kapatılamaz.

Kapatılamaz günlerde kilit saatleri dışında da Ayarlar ve uygulama kaldırma ekranı açılmaz; böylece Erişilebilirlik servisi kapatılarak ya da uygulama silinerek kilit aşılamaz.

### 3. İzinleri ver

Uygulamayı açınca iki izin gerekir:

1. **Üstte Gösterim İzni:** Kilit ekranını diğer uygulamaların önüne getirmek için.
2. **Erişilebilirlik İzni:** Hangi uygulamanın açıldığını fark etmek için. Ayarlarda "Özgürlük Kilidi" servisini aç.

> **Android 13+ notu:** APK'yı Play Store dışından kurduysan Erişilebilirlik anahtarı gri görünebilir. Bu durumda **Ayarlar → Uygulamalar → Özgürlük Kilidi → sağ üstteki ⋮ menüsü → Kısıtlı ayarlara izin ver** seçeneğini kullan, sonra izni tekrar dene.
>
> Google Play Protect, Erişilebilirlik ve üstte gösterim izni isteyen uygulamalar için kurulum sırasında uyarı gösterebilir.

### 4. Kullan

1. **Uygulamaları seç** düğmesiyle serbest kalacak uygulamaları seç ve kaydet.
2. Başlangıç ve bitiş saatini ve en az bir günü seç (zorunlu). İstersen takvimden kapatılamaz günleri belirle.
3. **ZAMANLAYICIYI AKTİF ET** düğmesine bas ve özeti onayla.

Zamanlayıcı kuruluyken uygulamayı açarsan bir sonraki kilide kalan süreyi görürsün. Kilit saati gelince seçilmemiş bir uygulamayı açtığın anda kilit ekranı devreye girer.

## 🛠️ Sorun giderme

**Bir uygulama içinde kilit ekranına atılıyorum (örneğin başka bir cihazda farklı bir dosya seçici):**
Bazı üreticiler kendi sistem pencerelerini kullanır. Android Studio'da Logcat'i `OzgurlukKilidi` etiketiyle filtrele. Kilit ekranına atıldığın anda şuna benzer bir satır çıkar:

```
Engellendi: paket.adi (sinif.adi)
```

Buradaki paket adını `AppBlockerService.kt` dosyasındaki `ALWAYS_ALLOWED` listesine ekleyip yeniden derle.

## ⚠️ Sınırlar

Özgürlük Kilidi bir **öz disiplin aracıdır**, güvenlik duvarı değildir. Takvim seçilmemişse uygulamayı kaldırmak ya da Erişilebilirlik servisini kapatmak kilidi aşar. Takvimli günlerde bile telefonu güvenli modda başlatmak ya da telefonun tarihini elle değiştirmek kilidi aşabilir. Amaç seni engellemek değil, dikkatini dağıtan uygulamaya uzanma anında bir duraklama yaratmaktır.

## 📁 Proje yapısı

| Dosya | Görevi |
|---|---|
| `MainActivity.kt` | Ayar ve kilit ekranı, uygulama ızgarası, geri sayım, takvim |
| `AppBlockerService.kt` | Erişilebilirlik servisi; izinsiz uygulamayı fark edip kilit ekranını öne getirir |
| `LockSettings.kt` | Kalıcı ayarlar, takvim ve zaman penceresi hesapları |

---

# 🇬🇧 English

Freedom Lock is an Android app for digital detox and focus. During the time window you set, only the **allowed apps** you pre-selected can be used. If you try to open anything else, the app sends you back to the lock screen.

## ✨ Features

- **Allowed apps:** The app scans your installed apps and lets you pick which ones stay available during the lock; you decide how many. Social media apps (except WhatsApp) and Settings can't be allowed. They appear as an icon grid on the lock screen and work with all their features (keyboard, file/photo pickers and permission dialogs included).
- **Smart scheduler:** Choose a start time, an end time, and the days of the week it should run. A time range and at least one day are required. Windows that cross midnight (e.g. 22:00 – 02:00) are supported.
- **Countdown:** The remaining time and a progress bar are shown during the lock. When it hits zero, the lock screen closes and blocking stops automatically.
- **Motivation card:** A random quote is shown on the lock screen; tap it for another one (Turkish only).
- **Calendar (optional):** With "Şu güne kadar" (until a date) you pick a date and the timer can't be turned off until that day ends. With "Şu günlerde" (on these days) you add dates one by one and the timer can't be turned off on those days. Settings and the uninstall screen are blocked on those days too. Without a calendar, the timer can be stopped after a confirmation.
- **Kicks in immediately:** If a blocked app is already open when the lock starts, you're moved to the lock screen within a few seconds.
- **Status at a glance:** The home screen shows which permissions are granted and warns you if the Accessibility service was turned off.
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

### 2. Calendar (optional)

Use the **4 · Takvim** card to make the timer impossible to turn off:

- **Yok (none):** The timer can be stopped after a confirmation.
- **Şu güne kadar (until):** Until the chosen date ends (inclusive), the timer can't be stopped and settings can't be changed.
- **Şu günlerde (on these days):** On every date you add, the timer can't be stopped.

On those days, Settings and the uninstall screen are blocked even outside lock hours, so the lock can't be bypassed by turning off the Accessibility service or uninstalling the app.

### 3. Grant permissions

When you open the app, two permissions are required:

1. **Display over other apps:** so the lock screen can appear on top of other apps.
2. **Accessibility:** to detect which app was opened. Turn on the "Özgürlük Kilidi" service in settings.

> **Android 13+ note:** If you installed the APK from outside the Play Store, the Accessibility switch may be greyed out. In that case go to **Settings → Apps → Özgürlük Kilidi → ⋮ menu (top right) → Allow restricted settings**, then try the permission again.
>
> Google Play Protect may show a warning during installation for apps that request Accessibility and overlay permissions.

### 4. Use it

1. Press **Uygulamaları seç** (Select apps), choose the apps that stay available, then save.
2. Choose the start time, the end time and, if you like, the days.
3. Press **ZAMANLAYICIYI AKTİF ET** (Activate timer) and confirm the summary.

While the timer is armed, opening the app shows the time left until the next lock. Once lock time arrives, the lock screen kicks in the moment you open a non-selected app.

## 🛠️ Troubleshooting

**I get sent back to the lock screen inside an allowed app (e.g. a different file picker on another device):**
Some manufacturers use their own system windows. In Android Studio, filter Logcat by the tag `OzgurlukKilidi`. When you're kicked to the lock screen, a line like this appears:

```
Engellendi: package.name (class.name)
```

Add that package name to the `ALWAYS_ALLOWED` list in `AppBlockerService.kt` and rebuild.

## ⚠️ Limits

Freedom Lock is a **self-discipline tool**, not a security barrier. Without a calendar, uninstalling the app or turning off the Accessibility service bypasses the lock. Even on calendar days, booting into safe mode or changing the phone's date manually can bypass it. The goal isn't to stop you by force, but to create a pause at the moment you reach for a distracting app.

## 📁 Project structure

| File | Purpose |
|---|---|
| `MainActivity.kt` | Settings and lock screen, app grid, countdown, calendar |
| `AppBlockerService.kt` | Accessibility service; detects non-allowed apps and brings the lock screen to the front |
| `LockSettings.kt` | Persistent settings, calendar and time-window calculations |
