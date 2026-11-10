package com.ozgurluk.kilidi

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
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
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@SuppressLint("SetTextI18n")
@Suppress("SpellCheckingInspection")
class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvStatusDetail: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvLockEnds: TextView
    private lateinit var tvLockCommit: TextView
    private lateinit var tvQuote: TextView
    private lateinit var tvSelectedApps: TextView
    private lateinit var tvNoApps: TextView
    private lateinit var tvCommitInfo: TextView
    private lateinit var progressLock: LinearProgressIndicator
    private lateinit var layoutSettings: View
    private lateinit var layoutLockContainer: View
    private lateinit var layoutAllowedApps: LinearLayout
    private lateinit var btnOverlay: MaterialButton
    private lateinit var btnAccessibility: MaterialButton
    private lateinit var btnSelectApps: MaterialButton
    private lateinit var btnStartTime: MaterialButton
    private lateinit var btnEndTime: MaterialButton
    private lateinit var btnLock: MaterialButton
    private lateinit var btnUnlock: MaterialButton
    private lateinit var groupCommitMode: MaterialButtonToggleGroup
    private lateinit var btnPickUntil: MaterialButton
    private lateinit var btnAddDate: MaterialButton
    private lateinit var chipDates: ChipGroup
    private lateinit var dayButtons: Map<Int, ToggleButton>

    private val handler = Handler(Looper.getMainLooper())

    /** Şu an kilit paneli (uygulama ızgarası + geri sayım) ekranda mı? */
    private var lockUiShown = false

    /** Ayarlar ekrana basılırken takvim düğmelerinin dinleyicisi tetiklenmesin. */
    private var bindingCommitMode = false

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

    private val dayShortNames = mapOf(
        Calendar.MONDAY to "Pzt", Calendar.TUESDAY to "Sal", Calendar.WEDNESDAY to "Çar",
        Calendar.THURSDAY to "Per", Calendar.FRIDAY to "Cum", Calendar.SATURDAY to "Cmt",
        Calendar.SUNDAY to "Paz"
    )

    private val dateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr"))
    private val shortDateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("tr"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        LockSettings.clearLegacyPasswordData(this)

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
        tvCountdown = findViewById(R.id.tvCountdown)
        tvLockEnds = findViewById(R.id.tvLockEnds)
        tvLockCommit = findViewById(R.id.tvLockCommit)
        tvQuote = findViewById(R.id.tvQuote)
        tvSelectedApps = findViewById(R.id.tvSelectedApps)
        tvNoApps = findViewById(R.id.tvNoApps)
        tvCommitInfo = findViewById(R.id.tvCommitInfo)
        progressLock = findViewById(R.id.progressLock)
        layoutSettings = findViewById(R.id.layoutSettings)
        layoutLockContainer = findViewById(R.id.layoutLockContainer)
        layoutAllowedApps = findViewById(R.id.layoutAllowedApps)
        btnOverlay = findViewById(R.id.btnOverlayPermission)
        btnAccessibility = findViewById(R.id.btnAccessibilityPermission)
        btnSelectApps = findViewById(R.id.btnSelectApps)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        btnLock = findViewById(R.id.btnLock)
        btnUnlock = findViewById(R.id.btnUnlock)
        groupCommitMode = findViewById(R.id.groupCommitMode)
        btnPickUntil = findViewById(R.id.btnPickUntil)
        btnAddDate = findViewById(R.id.btnAddDate)
        chipDates = findViewById(R.id.chipDates)

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
            toggle.setOnCheckedChangeListener { _, _ ->
                saveDays()
                tvStatusDetail.text = scheduleSummary()
            }
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

        groupCommitMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || bindingCommitMode) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.btnModeUntil -> LockSettings.COMMIT_UNTIL
                R.id.btnModeDates -> LockSettings.COMMIT_DATES
                else -> LockSettings.COMMIT_NONE
            }
            LockSettings.setCommitMode(this, mode)
            render()
        }

        btnPickUntil.setOnClickListener {
            pickDate(LockSettings.commitUntil(this)) { date ->
                LockSettings.setCommitUntil(this, date)
                render()
            }
        }

        btnAddDate.setOnClickListener {
            pickDate(null) { date ->
                LockSettings.setCommitDates(this, LockSettings.commitDates(this) + date)
                render()
            }
        }

        btnLock.setOnClickListener {
            if (LockSettings.isActive(this)) askToStop() else activateTimer()
        }

        btnUnlock.setOnClickListener { askToStop() }

        tvQuote.setOnClickListener { tvQuote.text = nextQuote() }
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
            val committed = LockSettings.isCommittedNow(this)
            btnUnlock.visibility = if (committed) View.GONE else View.VISIBLE
            tvLockCommit.visibility = if (committed) View.VISIBLE else View.GONE
            tvLockCommit.text = commitLockText()
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
            "${allowed.size} uygulama serbest: " + allowed.joinToString(", ") { it.label }
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
        bindCommitment(editable)

        if (armed) {
            val committed = LockSettings.isCommittedNow(this)
            btnLock.text = if (committed) "KİLİTLİ · KAPATILAMAZ" else "ZAMANLAYICIYI DURDUR"
            btnLock.isEnabled = !committed
            btnLock.backgroundTintList = ColorStateList.valueOf(
                color(if (committed) R.color.surface_high else R.color.danger)
            )
            btnLock.setTextColor(color(if (committed) R.color.text_secondary else R.color.on_danger))
        } else {
            tvStatus.text = "Zamanlayıcı Beklemede"
            tvStatus.setTextColor(color(R.color.accent))
            tvStatusDetail.text = scheduleSummary()
            btnLock.text = "ZAMANLAYICIYI AKTİF ET"
            btnLock.isEnabled = true
            btnLock.backgroundTintList = ColorStateList.valueOf(color(R.color.accent))
            btnLock.setTextColor(color(R.color.on_accent))
        }
    }

    /** Takvim kartını (Yok / Şu güne kadar / Şu günlerde) doldurur. */
    private fun bindCommitment(editable: Boolean) {
        val mode = LockSettings.commitMode(this)
        bindingCommitMode = true
        groupCommitMode.check(
            when (mode) {
                LockSettings.COMMIT_UNTIL -> R.id.btnModeUntil
                LockSettings.COMMIT_DATES -> R.id.btnModeDates
                else -> R.id.btnModeNone
            }
        )
        bindingCommitMode = false
        groupCommitMode.isEnabled = editable
        for (i in 0 until groupCommitMode.childCount) groupCommitMode.getChildAt(i).isEnabled = editable

        btnPickUntil.visibility = if (mode == LockSettings.COMMIT_UNTIL) View.VISIBLE else View.GONE
        btnPickUntil.isEnabled = editable
        btnPickUntil.text = LockSettings.commitUntil(this)
            ?.let { "Bitiş tarihi: ${it.format(dateFormat)} (dahil)" }
            ?: "Tarih seç"

        btnAddDate.visibility = if (mode == LockSettings.COMMIT_DATES) View.VISIBLE else View.GONE
        btnAddDate.isEnabled = editable
        chipDates.visibility = if (mode == LockSettings.COMMIT_DATES) View.VISIBLE else View.GONE
        chipDates.removeAllViews()
        if (mode == LockSettings.COMMIT_DATES) {
            LockSettings.commitDates(this).sorted().forEach { date ->
                chipDates.addView(Chip(this).apply {
                    text = date.format(shortDateFormat)
                    isCloseIconVisible = editable
                    isClickable = false
                    setOnCloseIconClickListener {
                        LockSettings.setCommitDates(this@MainActivity, LockSettings.commitDates(this@MainActivity) - date)
                        render()
                    }
                })
            }
        }

        val info = commitSummary()
        tvCommitInfo.visibility = if (info != null) View.VISIBLE else View.GONE
        tvCommitInfo.text = info ?: ""
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
            // Süre doldu (00:00:00): kilit ekranını kapat, engelleme zaten kendiliğinden durur.
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
                val lines = mutableListOf<String>()
                if (seconds != null) lines += "Kilidin başlamasına ${formatHms(seconds)}"
                lines += scheduleSummary()
                if (LockSettings.isCommittedNow(this)) lines += commitLockText()
                tvStatusDetail.text = lines.joinToString("\n")
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
        if (LockSettings.days(this).isEmpty()) {
            toast("Kilidin çalışacağı en az bir gün seç.")
            return
        }

        val today = LocalDate.now()
        when (LockSettings.commitMode(this)) {
            LockSettings.COMMIT_UNTIL -> {
                val until = LockSettings.commitUntil(this)
                if (until == null) {
                    toast("Takvimde \"Şu güne kadar\" için bir tarih seç.")
                    return
                }
                if (until.isBefore(today)) {
                    toast("Seçtiğin tarih geçmişte kaldı, yeni bir tarih seç.")
                    return
                }
            }
            LockSettings.COMMIT_DATES -> {
                val dates = LockSettings.commitDates(this)
                if (dates.none { !it.isBefore(today) }) {
                    toast("Takvimde \"Şu günlerde\" için bugün ya da sonrası en az bir gün ekle.")
                    return
                }
                // Geçmişte kalan günleri temizle
                LockSettings.setCommitDates(this, dates.filter { !it.isBefore(today) }.toSet())
            }
        }
        confirmActivation()
    }

    private fun confirmActivation() {
        val appCount = LockSettings.allowedApps(this).size
        val commit = commitSummary()
        val message = buildString {
            append(scheduleSummary())
            append("\nSerbest uygulama: $appCount")
            if (commit != null) append("\n\n⚠ $commit")
            append("\n\nZamanlayıcı kuruluyken saat, gün, takvim ve uygulama seçimi değiştirilemez.")
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Zamanlayıcı kurulsun mu?")
            .setMessage(message)
            .setPositiveButton("Aktif et") { _, _ ->
                LockSettings.setActive(this, true)
                render()
                toast("Zamanlayıcı kuruldu. Bol odaklı çalışmalar!")
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun askToStop() {
        if (LockSettings.isCommittedNow(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Kapatılamaz")
                .setMessage("${commitLockText()}\n\nBu kararı kendin verdin, sonuna kadar götür!")
                .setPositiveButton("Tamam", null)
                .show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Zamanlayıcı durdurulsun mu?")
            .setMessage("Odak modunu kapatırsan seçtiğin saatlerde uygulamalar artık engellenmeyecek.")
            .setPositiveButton("Durdur") { _, _ -> stopTimer() }
            .setNegativeButton("Vazgeç") { _, _ -> toast("Harika karar! Odaklanmaya devam.") }
            .show()
    }

    private fun stopTimer() {
        LockSettings.setActive(this, false)
        render()
        toast("Odak modu kapatıldı.")
    }

    // ---- Takvim ---------------------------------------------------------------------------------

    /** Kurulumda gösterilecek takvim özeti; takvim yoksa null. */
    private fun commitSummary(): String? = when (LockSettings.commitMode(this)) {
        LockSettings.COMMIT_UNTIL -> LockSettings.commitUntil(this)?.let {
            "${it.format(dateFormat)} günü bitene kadar zamanlayıcı hiçbir şekilde kapatılamaz."
        } ?: "Bir bitiş tarihi seç."
        LockSettings.COMMIT_DATES -> {
            val dates = LockSettings.commitDates(this).filter { !it.isBefore(LocalDate.now()) }.sorted()
            if (dates.isEmpty()) "Kapatılamayacak günleri ekle."
            else "Şu günlerde zamanlayıcı hiçbir şekilde kapatılamaz: " +
                dates.joinToString(", ") { it.format(shortDateFormat) }
        }
        else -> null
    }

    /** Bugün geçerli olan kapatma yasağının açıklaması. */
    private fun commitLockText(): String = when (LockSettings.commitMode(this)) {
        LockSettings.COMMIT_UNTIL -> LockSettings.commitUntil(this)
            ?.let { "Bu kilit ${it.format(dateFormat)} günü bitene kadar kapatılamaz." } ?: ""
        LockSettings.COMMIT_DATES -> "Bugün kilit kapatılamayan günlerden biri."
        else -> ""
    }

    private fun pickDate(initial: LocalDate?, onPicked: (LocalDate) -> Unit) {
        val today = LocalDate.now()
        val start = initial?.takeIf { !it.isBefore(today) } ?: today
        val dialog = DatePickerDialog(
            this,
            { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            start.year, start.monthValue - 1, start.dayOfMonth
        )
        dialog.datePicker.minDate =
            today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dialog.show()
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
        val selected = LockSettings.days(this)
        if (start == LockSettings.NOT_SET || end == LockSettings.NOT_SET) {
            return "Başlamak için saat aralığını ve günleri seç."
        }
        val dayText = when {
            selected.isEmpty() -> "Gün seçilmedi"
            selected.size == 7 -> "Her gün"
            else -> dayButtons.keys.filter { it in selected }.joinToString(", ") { dayShortNames.getValue(it) }
        }
        val nextDay = if (end <= start) " (ertesi gün)" else ""
        return "$dayText · ${formatClock(start)} – ${formatClock(end)}$nextDay"
    }

    private fun nextQuote(): String {
        val current = tvQuote.text?.toString()
        return quotesLibrary.filter { it != current }.random()
    }

    // ---- Serbest uygulamalar --------------------------------------------------------------------

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

    /** Serbest uygulamaları 3 sütunlu satırlar halinde dizer (sayı sınırı yok). */
    private fun showAllowedApps() {
        val apps = queryLauncherApps(LockSettings.allowedApps(this))
        layoutAllowedApps.removeAllViews()
        tvNoApps.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        layoutAllowedApps.visibility = if (apps.isEmpty()) View.GONE else View.VISIBLE

        val inflater = LayoutInflater.from(this)
        val gap = (6 * resources.displayMetrics.density).toInt()
        apps.chunked(GRID_COLUMNS).forEach { rowApps ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (i in 0 until GRID_COLUMNS) {
                val app = rowApps.getOrNull(i)
                val cell: View = if (app == null) {
                    View(this) // son satırı hizalamak için boş hücre
                } else {
                    inflater.inflate(R.layout.grid_item_app, row, false).apply {
                        findViewById<ImageView>(R.id.ivAppIcon).setImageDrawable(app.icon)
                        findViewById<TextView>(R.id.tvAppName).text = app.label
                        contentDescription = app.label
                        setOnClickListener { launchApp(app) }
                    }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(gap, gap, gap, gap)
                })
            }
            layoutAllowedApps.addView(row)
        }
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

    private fun showAppSelectionDialog() {
        // Sosyal medya uygulamaları (WhatsApp hariç) ve Ayarlar listede hiç görünmez.
        val apps = queryLauncherApps().filter { LockSettings.isWhitelistable(it.packageName) }
        val names = apps.map { it.label }.toTypedArray()

        val alreadyAllowed = LockSettings.allowedApps(this)
        val selected = apps.map { it.packageName }.filter { it in alreadyAllowed }.toMutableSet()
        val checked = BooleanArray(apps.size) { apps[it].packageName in selected }

        MaterialAlertDialogBuilder(this)
            .setTitle("Serbest uygulamaları seç")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                val pkg = apps[which].packageName
                if (isChecked) selected.add(pkg) else selected.remove(pkg)
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

    private companion object {
        const val GRID_COLUMNS = 3
    }
}
