package com.ozgurluk.kilidi

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Calendar
import java.util.Locale

@SuppressLint("SetTextI18n")
@Suppress("SpellCheckingInspection")
class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvStatusDetail: TextView
    private lateinit var tvCountdownLabel: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvLockEnds: TextView
    private lateinit var tvQuote: TextView
    private lateinit var tvSelectedApps: TextView
    private lateinit var tvNoApps: TextView
    private lateinit var progressLock: LinearProgressIndicator
    private lateinit var layoutSettings: View
    private lateinit var layoutLockContainer: View
    private lateinit var gvAllowedApps: GridView
    private lateinit var btnOverlay: MaterialButton
    private lateinit var btnAccessibility: MaterialButton
    private lateinit var btnSelectApps: MaterialButton
    private lateinit var btnStartTime: MaterialButton
    private lateinit var btnEndTime: MaterialButton
    private lateinit var btnLock: MaterialButton
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var dayButtons: Map<Int, ToggleButton>

    private val handler = Handler(Looper.getMainLooper())

    /** Şu an kilit paneli (grid + geri sayım) ekranda mı? */
    private var lockUiShown = false
    private var gridApps: List<LauncherApp> = emptyList()

    /** Kilit sırasında Geri tuşu uygulamayı kapatıp ana ekran ↔ kilit arasında titremesin. */
    private val lockBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            toast("Odak modu aktif. Serbest uygulamalardan birini kullanabilirsin.")
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, 1000L - System.currentTimeMillis() % 1000L)
        }
    }

    private val quotesLibrary = arrayOf(
        "Benim Soyumun Asaleti Benimle Başlayacak\n— Napolyon",
        "Zorluklar, başarının değerini artıran süslerdir.\n— Molière",
        "Yarınlar yorgun ve bezgin kimselere değil, rahatını terk edebilen gayretli insanlara aittir.\n— Cicero",
        "Aklın gücü, çalışmakla gelişir.\n— Leonardo da Vinci",
        "Gelecek, bugünden ona hazırlananlara aittir.\n— Malcolm X",
        "En büyük zaferimiz hiç düşmemek değil, her düştüğümüzde kalkabilmektir.\n— Konfüçyüs",
        "Bin millik yolculuk tek bir adımla başlar.\n— Lao Tzu",
        "Biz ne yaptığımızı tekrar tekrar yapan varlıklarız. Mükemmellik bir eylem değil, alışkanlıktır.\n— Will Durant",
        "Hayatta en hakiki mürşit ilimdir.\n— Mustafa Kemal Atatürk",
        "Dikkatini nereye verirsen, hayatın oraya akar."
    )

    private companion object {
        /** Erken durdurma ekranları: (başlık, mesaj). Her biri şifreyi yeniden ister. */
        val STOP_STEPS = listOf(
            "Emin misin?" to "Odak modunu erken bitirmek üzeresin. Devam etmek için şifreni gir.",
            "Gerçekten emin misin?" to "Biraz daha dayanabilirsin. Yine de bitirmek istiyorsan şifreni tekrar gir.",
            "Ya düşündüm de, son kararın mı?" to "Bu süreyi kendin belirlemiştin. Hâlâ bitirmek istiyorsan şifreni bir kez daha gir.",
            "Tamam, bak bu son ekran." to "Şifreyi son kez girersen kilit kapanacak ve uygulama kapanacak."
        )
    }

    private val dayShortNames = mapOf(
        Calendar.MONDAY to "Pzt", Calendar.TUESDAY to "Sal", Calendar.WEDNESDAY to "Çar",
        Calendar.THURSDAY to "Per", Calendar.FRIDAY to "Cum", Calendar.SATURDAY to "Cmt",
        Calendar.SUNDAY to "Paz"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // targetSdk 35+ uygulamalar sistem çubuklarının altına çizilir; içeriği çubukların dışına al.
        val rootLayout = findViewById<View>(R.id.rootLayout)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvStatus = findViewById(R.id.tvStatus)
        tvStatusDetail = findViewById(R.id.tvStatusDetail)
        tvCountdownLabel = findViewById(R.id.tvCountdownLabel)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvLockEnds = findViewById(R.id.tvLockEnds)
        tvQuote = findViewById(R.id.tvQuote)
        tvSelectedApps = findViewById(R.id.tvSelectedApps)
        tvNoApps = findViewById(R.id.tvNoApps)
        progressLock = findViewById(R.id.progressLock)
        layoutSettings = findViewById(R.id.layoutSettings)
        layoutLockContainer = findViewById(R.id.layoutLockContainer)
        gvAllowedApps = findViewById(R.id.gvAllowedApps)
        btnOverlay = findViewById(R.id.btnOverlayPermission)
        btnAccessibility = findViewById(R.id.btnAccessibilityPermission)
        btnSelectApps = findViewById(R.id.btnSelectApps)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        btnLock = findViewById(R.id.btnLock)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        val btnUnlock = findViewById<MaterialButton>(R.id.btnUnlock)

        onBackPressedDispatcher.addCallback(this, lockBackCallback)

        dayButtons = mapOf(
            Calendar.MONDAY to findViewById<ToggleButton>(R.id.btnMon),
            Calendar.TUESDAY to findViewById<ToggleButton>(R.id.btnTue),
            Calendar.WEDNESDAY to findViewById<ToggleButton>(R.id.btnWed),
            Calendar.THURSDAY to findViewById<ToggleButton>(R.id.btnThu),
            Calendar.FRIDAY to findViewById<ToggleButton>(R.id.btnFri),
            Calendar.SATURDAY to findViewById<ToggleButton>(R.id.btnSat),
            Calendar.SUNDAY to findViewById<ToggleButton>(R.id.btnSun)
        )

        // Kayıtlı günleri geri yükle, sonra değişiklikleri anında kaydet
        val savedDays = LockSettings.days(this)
        for ((day, toggle) in dayButtons) {
            toggle.isChecked = day in savedDays
            toggle.setOnCheckedChangeListener { _, _ -> saveDays() }
        }

        btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()))
            } else {
                toast("Üstte gösterim izni zaten verilmiş.")
            }
        }

        btnAccessibility.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.accessibility_disclosure_title)
                .setMessage(R.string.accessibility_disclosure_body)
                .setPositiveButton("Devam") { _, _ ->
                    openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        btnSelectApps.setOnClickListener { showAppSelectionDialog() }

        btnStartTime.setOnClickListener {
            pickTime(LockSettings.startMinutes(this)) { minutes ->
                LockSettings.setStartMinutes(this, minutes)
                render()
            }
        }

        btnEndTime.setOnClickListener {
            pickTime(LockSettings.endMinutes(this)) { minutes ->
                LockSettings.setEndMinutes(this, minutes)
                render()
            }
        }

        btnLock.setOnClickListener {
            if (LockSettings.isActive(this)) askPasswordToStop() else activateTimer()
        }

        btnChangePassword.setOnClickListener { changePassword() }
        btnUnlock.setOnClickListener { askPasswordToStop() }

        tvQuote.setOnClickListener { tvQuote.text = nextQuote() }

        gvAllowedApps.setOnItemClickListener { _, _, position, _ ->
            gridApps.getOrNull(position)?.let { launchApp(it) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Kilit ekranı yeniden öne getirildi: yeni bir özlü söz seçilsin.
        lockUiShown = false
    }

    override fun onResume() {
        super.onResume()
        render()
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, 1000L - System.currentTimeMillis() % 1000L)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    // ---- Ekran durumu ---------------------------------------------------------------------------

    /** Kayıtlı ayarlara göre doğru paneli (ayarlar / kilit) gösterir. */
    private fun render() {
        val armed = LockSettings.isActive(this)
        val locking = armed && LockSettings.isInLockWindow(this)

        if (locking) {
            if (!lockUiShown) tvQuote.text = nextQuote()
            layoutSettings.visibility = View.GONE
            layoutLockContainer.visibility = View.VISIBLE
            tvLockEnds.text = "Kilit ${formatClock(LockSettings.endMinutes(this))} saatinde kalkacak"
            showAllowedApps()
        } else {
            layoutLockContainer.visibility = View.GONE
            layoutSettings.visibility = View.VISIBLE
            bindSettings(armed)
        }
        lockUiShown = locking
        lockBackCallback.isEnabled = locking
        tick()
    }

    private fun bindSettings(armed: Boolean) {
        btnStartTime.text = "Başlangıç\n${formatClock(LockSettings.startMinutes(this))}"
        btnEndTime.text = "Bitiş\n${formatClock(LockSettings.endMinutes(this))}"

        // İzin durumları
        val overlayOk = Settings.canDrawOverlays(this)
        val accessibilityOk = isAccessibilityServiceEnabled(this, AppBlockerService::class.java)
        bindPermissionButton(btnOverlay, overlayOk, "Üstte gösterim izni")
        bindPermissionButton(btnAccessibility, accessibilityOk, "Erişilebilirlik servisi")

        // Seçili uygulamalar
        val allowed = queryLauncherApps(LockSettings.allowedApps(this))
        tvSelectedApps.text = if (allowed.isEmpty()) {
            "Henüz uygulama seçilmedi. Kilit sırasında yalnızca arama ve alarm açık kalır."
        } else {
            "${allowed.size}/${LockSettings.MAX_APPS} seçili: " + allowed.joinToString(", ") { it.label }
        }

        // Zamanlayıcı kuruluyken ayarlar değiştirilemez (kilidi atlatmayı zorlaştırır).
        val editable = !armed
        btnSelectApps.isEnabled = editable
        btnStartTime.isEnabled = editable
        btnEndTime.isEnabled = editable
        dayButtons.values.forEach {
            it.isEnabled = editable
            it.alpha = if (editable) 1f else 0.45f
        }
        btnChangePassword.visibility = if (armed) View.GONE else View.VISIBLE
        btnChangePassword.text =
            if (LockSettings.hasPassword(this)) "Kilit şifresini değiştir" else "Kilit şifresini belirle"

        if (armed) {
            btnLock.text = "ZAMANLAYICIYI DURDUR"
            btnLock.backgroundTintList = ColorStateList.valueOf(color(R.color.danger))
            btnLock.setTextColor(color(R.color.on_danger))
        } else {
            tvStatus.text = "Zamanlayıcı Beklemede"
            tvStatus.setTextColor(color(R.color.accent))
            tvStatusDetail.text = scheduleSummary()
            btnLock.text = "ZAMANLAYICIYI AKTİF ET"
            btnLock.backgroundTintList = ColorStateList.valueOf(color(R.color.accent))
            btnLock.setTextColor(color(R.color.on_accent))
        }
    }

    private fun bindPermissionButton(button: MaterialButton, granted: Boolean, label: String) {
        val c = color(if (granted) R.color.accent else R.color.warning)
        button.text = if (granted) "✓  $label verildi" else "!  $label ver"
        button.setTextColor(c)
        button.strokeColor = ColorStateList.valueOf(c)
    }

    /** Saniyede bir çalışır: geri sayımı günceller, süre bitince kilit ekranını kapatır. */
    private fun tick() {
        val armed = LockSettings.isActive(this)
        val locking = armed && LockSettings.isInLockWindow(this)

        when {
            // Süre doldu (00:00:00): grid/kilit ekranını kapat, engelleme zaten kendiliğinden durur.
            lockUiShown && !locking -> if (armed) endLockSession() else render()
            // Kilit penceresi ekran açıkken başladı.
            !lockUiShown && locking -> render()
            locking -> {
                val remaining = LockSettings.secondsUntilEnd(this)
                tvCountdown.text = formatHms(remaining)
                val total = LockSettings.windowLengthSeconds(this).coerceAtLeast(1L)
                val done = ((total - remaining).coerceIn(0L, total) * 1000L / total).toInt()
                progressLock.setProgressCompat(done, false)
            }
            armed -> {
                if (!isAccessibilityServiceEnabled(this, AppBlockerService::class.java)) {
                    tvStatus.text = "Kilit çalışmıyor!"
                    tvStatus.setTextColor(color(R.color.danger))
                    tvStatusDetail.text =
                        "Erişilebilirlik servisi kapalı. Kilidin çalışması için 1. adımdan tekrar aç."
                    return
                }
                tvStatus.text = "Zamanlayıcı Aktif"
                tvStatus.setTextColor(color(R.color.warning))
                val seconds = LockSettings.secondsUntilNextStart(this)
                tvStatusDetail.text = if (seconds != null) {
                    "Kilidin başlamasına ${formatHms(seconds)}\n${scheduleSummary()}"
                } else {
                    scheduleSummary()
                }
            }
        }
    }

    private fun endLockSession() {
        lockUiShown = false
        lockBackCallback.isEnabled = false
        toast("Süre doldu, kilit kaldırıldı. Tebrikler!", long = true)
        if (!isFinishing) finishAndRemoveTask()
    }

    // ---- Zamanlayıcıyı aç / kapat ---------------------------------------------------------------

    private fun activateTimer() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Önce 1. adımdaki \"Üstte gösterim\" iznini ver.")
            return
        }
        if (!isAccessibilityServiceEnabled(this, AppBlockerService::class.java)) {
            toast("Önce 1. adımdaki Erişilebilirlik servisini aç.")
            return
        }
        val start = LockSettings.startMinutes(this)
        val end = LockSettings.endMinutes(this)
        if (start == LockSettings.NOT_SET || end == LockSettings.NOT_SET) {
            toast("Başlangıç ve bitiş saatini seç.")
            return
        }
        if (start == end) {
            toast("Başlangıç ve bitiş saati aynı olamaz.")
            return
        }

        saveDays()
        if (!LockSettings.hasPassword(this)) {
            toast("Önce kilidi erken durdurmak için bir şifre belirle.", long = true)
            askNewPassword { confirmActivation() }
        } else {
            confirmActivation()
        }
    }

    private fun confirmActivation() {
        val appCount = LockSettings.allowedApps(this).size
        MaterialAlertDialogBuilder(this)
            .setTitle("Zamanlayıcı kurulsun mu?")
            .setMessage(
                "${scheduleSummary()}\n" +
                    "Serbest uygulama: $appCount\n\n" +
                    "Zamanlayıcı kuruluyken saat, gün ve uygulama seçimi değiştirilemez. " +
                    "Erken durdurmak için şifren gerekecek."
            )
            .setPositiveButton("Aktif et") { _, _ ->
                LockSettings.setActive(this, true)
                render()
                toast("Zamanlayıcı kuruldu. Bol odaklı çalışmalar!")
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun askPasswordToStop() {
        // Eski sürümden şifresiz yükseltildiyse kullanıcıyı kilitli bırakma.
        if (!LockSettings.hasPassword(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Zamanlayıcı durdurulsun mu?")
                .setMessage("Henüz bir kilit şifresi belirlenmemiş.")
                .setPositiveButton("Durdur") { _, _ -> stopTimer() }
                .setNegativeButton("İptal", null)
                .show()
            return
        }

        val waitMs = LockSettings.lockoutRemainingMs(this)
        if (waitMs > 0) {
            toast("Çok fazla hatalı deneme. ${(waitMs + 999) / 1000} sn sonra tekrar dene.", long = true)
            return
        }

        askStopStep(0)
    }

    /**
     * Erken durdurma 4 aşamalıdır; her aşamada şifre yeniden istenir. Herhangi bir aşamada
     * şifre yanlışsa ya da "Vazgeç" denirse süreç iptal olur ve baştan başlamak gerekir.
     */
    private fun askStopStep(step: Int) {
        if (step >= STOP_STEPS.size) {
            LockSettings.resetFailedAttempts(this)
            stopTimer(closeApp = true)
            return
        }
        val (title, message) = STOP_STEPS[step]
        val input = passwordField("Kilit şifresi")
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage("$message\n\n(${step + 1}/${STOP_STEPS.size})")
            .setView(wrap(input))
            .setPositiveButton(if (step == STOP_STEPS.lastIndex) "Kilidi kapat" else "Devam") { _, _ ->
                if (LockSettings.checkPassword(this, input.text.toString())) {
                    askStopStep(step + 1)
                } else {
                    val left = LockSettings.registerFailedAttempt(this)
                    if (left == 0) {
                        toast("Hatalı şifre! 30 saniye beklemen gerekiyor.", long = true)
                    } else {
                        toast("Hatalı şifre! Baştan başlaman gerekiyor. Kalan deneme: $left", long = true)
                    }
                }
            }
            .setNegativeButton("Vazgeç") { _, _ ->
                toast("Harika karar! Odaklanmaya devam.")
            }
            .setCancelable(false)
            .show()
    }

    private fun stopTimer(closeApp: Boolean = false) {
        LockSettings.setActive(this, false)
        render()
        toast("Odak modu kapatıldı.")
        if (closeApp && !isFinishing) finishAndRemoveTask()
    }

    // ---- Şifre ----------------------------------------------------------------------------------

    private fun changePassword() {
        if (LockSettings.isActive(this)) return
        if (!LockSettings.hasPassword(this)) {
            askNewPassword { render() }
            return
        }
        val waitMs = LockSettings.lockoutRemainingMs(this)
        if (waitMs > 0) {
            toast("Çok fazla hatalı deneme. ${(waitMs + 999) / 1000} sn sonra tekrar dene.", long = true)
            return
        }
        val current = passwordField("Mevcut şifre")
        MaterialAlertDialogBuilder(this)
            .setTitle("Şifreyi değiştir")
            .setView(wrap(current))
            .setPositiveButton("Devam") { _, _ ->
                if (LockSettings.checkPassword(this, current.text.toString())) {
                    LockSettings.resetFailedAttempts(this)
                    askNewPassword { render() }
                } else {
                    LockSettings.registerFailedAttempt(this)
                    toast("Mevcut şifre hatalı.")
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    /** Yeni şifre ister; geçerli ve iki alan eşleşiyorsa kaydedip [onSet] çağırır. */
    private fun askNewPassword(onSet: () -> Unit) {
        val first = passwordField("Yeni şifre (en az ${LockSettings.MIN_PASSWORD_LENGTH} karakter)")
        val second = passwordField("Yeni şifre (tekrar)")
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Kilit şifresi belirle")
            .setMessage("Zamanlayıcıyı süre dolmadan durdurmak için bu şifre gerekecek. Unutmayacağın ama anında da yazamayacağın bir şey seç.")
            .setView(wrap(first, second))
            .setPositiveButton("Kaydet", null)
            .setNegativeButton("İptal", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val pw = first.text.toString()
                when {
                    pw.length < LockSettings.MIN_PASSWORD_LENGTH ->
                        first.error = "En az ${LockSettings.MIN_PASSWORD_LENGTH} karakter olmalı"
                    pw != second.text.toString() ->
                        second.error = "Şifreler eşleşmiyor"
                    else -> {
                        LockSettings.setPassword(this, pw)
                        toast("Şifre kaydedildi.")
                        dialog.dismiss()
                        onSet()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun passwordField(hintText: String) = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        hint = hintText
        setSingleLine(true)
    }

    private fun wrap(vararg views: View): View {
        val pad = (20 * resources.displayMetrics.density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            views.forEach { addView(it) }
        }
    }

    // ---- Diğer ayarlar --------------------------------------------------------------------------

    private fun saveDays() {
        LockSettings.setDays(this, dayButtons.filterValues { it.isChecked }.keys)
    }

    private fun pickTime(currentMinutes: Int, onPicked: (Int) -> Unit) {
        val now = Calendar.getInstance()
        val hour = if (currentMinutes == LockSettings.NOT_SET) now.get(Calendar.HOUR_OF_DAY) else currentMinutes / 60
        val minute = if (currentMinutes == LockSettings.NOT_SET) now.get(Calendar.MINUTE) else currentMinutes % 60
        TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) }, hour, minute, true).show()
    }

    private fun scheduleSummary(): String {
        val start = LockSettings.startMinutes(this)
        val end = LockSettings.endMinutes(this)
        if (start == LockSettings.NOT_SET || end == LockSettings.NOT_SET) {
            return "Başlangıç ve bitiş saatini seçerek başla."
        }
        val selected = LockSettings.days(this)
        val dayText = when {
            selected.isEmpty() || selected.size == 7 -> "Her gün"
            else -> dayButtons.keys.filter { it in selected }.joinToString(", ") { dayShortNames.getValue(it) }
        }
        val nextDay = if (end <= start) " (ertesi gün)" else ""
        return "$dayText · ${formatClock(start)} – ${formatClock(end)}$nextDay"
    }

    private fun nextQuote(): String {
        val current = tvQuote.text?.toString()
        return quotesLibrary.filter { it != current }.random()
    }

    // ---- Serbest uygulamalar (grid) -------------------------------------------------------------

    private class LauncherApp(
        val packageName: String,
        val label: String,
        private val info: ResolveInfo,
        private val pm: PackageManager
    ) {
        val icon: Drawable by lazy { info.loadIcon(pm) }
    }

    @Suppress("DEPRECATION")
    private fun queryLauncherApps(onlyPackages: Set<String>? = null): List<LauncherApp> {
        if (onlyPackages != null && onlyPackages.isEmpty()) return emptyList()
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            pm.queryIntentActivities(intent, 0)
        }
        return infos
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName } // kendini seçtirme
            .filter { onlyPackages == null || it.activityInfo.packageName in onlyPackages }
            .map { LauncherApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), it, pm) }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun showAllowedApps() {
        gridApps = queryLauncherApps(LockSettings.allowedApps(this))
        gvAllowedApps.adapter = AppGridAdapter(this, gridApps)
        val empty = gridApps.isEmpty()
        gvAllowedApps.visibility = if (empty) View.GONE else View.VISIBLE
        tvNoApps.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun launchApp(app: LauncherApp) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent == null) {
            toast("${app.label} açılamadı.")
            return
        }
        try {
            startActivity(launchIntent)
        } catch (e: ActivityNotFoundException) {
            toast("${app.label} açılamadı.")
        }
    }

    private class AppGridAdapter(context: Context, private val apps: List<LauncherApp>) :
        ArrayAdapter<LauncherApp>(context, 0, apps) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.grid_item_app, parent, false)
            val app = apps[position]
            view.findViewById<ImageView>(R.id.ivAppIcon).setImageDrawable(app.icon)
            view.findViewById<TextView>(R.id.tvAppName).text = app.label
            return view
        }
    }

    private fun showAppSelectionDialog() {
        val apps = queryLauncherApps()
        val names = apps.map { it.label }.toTypedArray()

        // Artık yüklü olmayan / ikonu olmayan paketler listede görünmez, sayıma da girmesin.
        val alreadyAllowed = LockSettings.allowedApps(this)
        val selected = apps.map { it.packageName }.filter { it in alreadyAllowed }.toMutableSet()
        val checked = BooleanArray(apps.size) { apps[it].packageName in selected }

        MaterialAlertDialogBuilder(this)
            .setTitle("Uygulama seç (en fazla ${LockSettings.MAX_APPS})")
            .setMultiChoiceItems(names, checked) { dialog, which, isChecked ->
                val pkg = apps[which].packageName
                if (!isChecked) {
                    selected.remove(pkg)
                } else if (selected.size >= LockSettings.MAX_APPS) {
                    checked[which] = false
                    (dialog as AlertDialog).listView.setItemChecked(which, false)
                    toast("En fazla ${LockSettings.MAX_APPS} uygulama seçebilirsin!")
                } else {
                    selected.add(pkg)
                }
            }
            .setPositiveButton("Kaydet") { _, _ ->
                LockSettings.setAllowedApps(this, selected)
                toast("${selected.size} uygulama kaydedildi.")
                render()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // ---- Yardımcılar ----------------------------------------------------------------------------

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast("Ayarlar ekranı açılamadı; telefon ayarlarından elle aç.")
        }
    }

    private fun color(resId: Int): Int = ContextCompat.getColor(this, resId)

    private fun toast(message: String, long: Boolean = false) {
        Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    private fun formatClock(minutes: Int): String =
        if (minutes == LockSettings.NOT_SET) "--:--"
        else String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

    private fun formatHms(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0L)
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expected = ComponentName(context, service)
        val setting = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(setting) }
        while (splitter.hasNext()) {
            if (ComponentName.unflattenFromString(splitter.next()) == expected) return true
        }
        return false
    }
}
