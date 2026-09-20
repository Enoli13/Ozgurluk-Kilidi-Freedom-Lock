package com.ozgurluk.kilidi

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
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
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar
import java.util.Locale

@SuppressLint("SetTextI18n")
@Suppress("SpellCheckingInspection")
class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvCountdownLabel: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvQuote: TextView
    private lateinit var layoutSettings: View
    private lateinit var layoutLockContainer: View
    private lateinit var gvAllowedApps: GridView
    private lateinit var btnSelectApps: Button
    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var btnLock: Button
    private lateinit var dayButtons: Map<Int, ToggleButton>

    private val handler = Handler(Looper.getMainLooper())

    /** Şu an kilit paneli (grid + geri sayım) ekranda mı? */
    private var lockUiShown = false
    private var gridApps: List<LauncherApp> = emptyList()

    private val ticker = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, 1000L - System.currentTimeMillis() % 1000L)
        }
    }

    private val quotesLibrary = arrayOf(
        "Benim Soyumun Asaleti Benimle Başlayacak\n-Napolyon-",
        "Zorluklar, başarının değerini artıran süslerdir.\n-Moliere-",
        "Yarınlar yorgun ve bezgin kimselere değil, rahatını terk edebilen gayretli insanlara aittir.\n-Cicero-",
        "Aklın gücü, çalışmakla gelişir.\n-Leonardo da Vinci-",
        "Gelecek, bugünden ona hazırlananlara aittir.\n-Malcolm X-",
        "En büyük zaferimiz hiç düşmemek değil, her düştüğümüzde kalkabilmektir.\n-Konfüçyüs-"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // targetSdk 35+ uygulamalar sistem çubuklarının altına çizilir; içeriği çubukların dışına al.
        val rootLayout = findViewById<View>(R.id.rootLayout)
        val padLeft = rootLayout.paddingLeft
        val padTop = rootLayout.paddingTop
        val padRight = rootLayout.paddingRight
        val padBottom = rootLayout.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(padLeft + bars.left, padTop + bars.top, padRight + bars.right, padBottom + bars.bottom)
            insets
        }

        tvStatus = findViewById(R.id.tvStatus)
        tvCountdownLabel = findViewById(R.id.tvCountdownLabel)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvQuote = findViewById(R.id.tvQuote)
        layoutSettings = findViewById(R.id.layoutSettings)
        layoutLockContainer = findViewById(R.id.layoutLockContainer)
        gvAllowedApps = findViewById(R.id.gvAllowedApps)
        btnSelectApps = findViewById(R.id.btnSelectApps)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        btnLock = findViewById(R.id.btnLock)

        val btnOverlay = findViewById<Button>(R.id.btnOverlayPermission)
        val btnAccessibility = findViewById<Button>(R.id.btnAccessibilityPermission)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)

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
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()))
            } else {
                toast("Üstte gösterim izni verilmiş.")
            }
        }

        btnAccessibility.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.accessibility_disclosure_title)
                .setMessage(R.string.accessibility_disclosure_body)
                .setPositiveButton("Devam") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        btnSelectApps.setOnClickListener { showAppSelectionDialog() }

        btnStartTime.setOnClickListener {
            pickTime(LockSettings.startMinutes(this)) { minutes ->
                LockSettings.setStartMinutes(this, minutes)
                btnStartTime.text = "Başlangıç: ${formatClock(minutes)}"
            }
        }

        btnEndTime.setOnClickListener {
            pickTime(LockSettings.endMinutes(this)) { minutes ->
                LockSettings.setEndMinutes(this, minutes)
                btnEndTime.text = "Bitiş: ${formatClock(minutes)}"
            }
        }

        btnLock.setOnClickListener {
            if (LockSettings.isActive(this)) askPasswordToStop() else activateTimer()
        }

        btnUnlock.setOnClickListener { askPasswordToStop() }

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
            if (!lockUiShown) tvQuote.text = quotesLibrary.random()
            layoutSettings.visibility = View.GONE
            layoutLockContainer.visibility = View.VISIBLE
            tvCountdownLabel.text = "Kilidin bitmesine kalan süre"
            showAllowedApps()
        } else {
            layoutLockContainer.visibility = View.GONE
            layoutSettings.visibility = View.VISIBLE
            bindSettings(armed)
        }
        lockUiShown = locking
        tick()
    }

    private fun bindSettings(armed: Boolean) {
        btnStartTime.text = "Başlangıç: ${formatClock(LockSettings.startMinutes(this))}"
        btnEndTime.text = "Bitiş: ${formatClock(LockSettings.endMinutes(this))}"

        // Zamanlayıcı kuruluyken ayarlar değiştirilemez (kilidi atlatmayı zorlaştırır).
        val editable = !armed
        btnSelectApps.isEnabled = editable
        btnStartTime.isEnabled = editable
        btnEndTime.isEnabled = editable
        dayButtons.values.forEach { it.isEnabled = editable }

        if (armed) {
            tvStatus.setTextColor(COLOR_ARMED)
            btnLock.text = "ZAMANLAYICIYI DURDUR"
        } else {
            tvStatus.text = "Zamanlayıcı Beklemede"
            tvStatus.setTextColor(COLOR_IDLE)
            btnLock.text = "ZAMANLAYICIYI AKTİF ET"
        }
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
            locking -> tvCountdown.text = formatHms(LockSettings.secondsUntilEnd(this))
            armed -> {
                val seconds = LockSettings.secondsUntilNextStart(this)
                tvStatus.text = if (seconds != null) {
                    "Zamanlayıcı Aktif\nKilit başlamasına: ${formatHms(seconds)}"
                } else {
                    "Zamanlayıcı Aktif"
                }
            }
        }
    }

    private fun endLockSession() {
        lockUiShown = false
        toast("Süre doldu, kilit kaldırıldı.", long = true)
        if (!isFinishing) finishAndRemoveTask()
    }

    // ---- Zamanlayıcıyı aç / kapat ---------------------------------------------------------------

    private fun activateTimer() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Önce 1. adımdaki \"Üstte Gösterim\" iznini ver.")
            return
        }
        if (!isAccessibilityServiceEnabled(this, AppBlockerService::class.java)) {
            toast("Önce 2. adımdaki Erişilebilirlik iznini aç.")
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
        LockSettings.setActive(this, true)
        render()
    }

    private fun askPasswordToStop() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Güvenlik Şifresini Girin"
        }

        AlertDialog.Builder(this)
            .setTitle("Sistem Koruması")
            .setMessage("Odaklanma modunu erken sonlandırmak için yetkili şifreyi giriniz:")
            .setView(input)
            .setPositiveButton("Doğrula") { _, _ ->
                if (input.text.toString() == UNLOCK_PASSWORD) {
                    LockSettings.setActive(this, false)
                    render()
                    toast("Odaklanma Modu Kapatıldı.")
                } else {
                    toast("Hatalı Şifre! Odaklanma Modu Devam Ediyor.", long = true)
                }
            }
            .setNegativeButton("İptal", null)
            .setCancelable(false)
            .show()
    }

    private fun saveDays() {
        LockSettings.setDays(this, dayButtons.filterValues { it.isChecked }.keys)
    }

    private fun pickTime(currentMinutes: Int, onPicked: (Int) -> Unit) {
        val now = Calendar.getInstance()
        val hour = if (currentMinutes == LockSettings.NOT_SET) now.get(Calendar.HOUR_OF_DAY) else currentMinutes / 60
        val minute = if (currentMinutes == LockSettings.NOT_SET) now.get(Calendar.MINUTE) else currentMinutes % 60
        TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) }, hour, minute, true).show()
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
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            pm.queryIntentActivities(intent, 0)
        }
        return infos
            .distinctBy { it.activityInfo.packageName }
            .filter { onlyPackages == null || it.activityInfo.packageName in onlyPackages }
            .map { LauncherApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), it, pm) }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun showAllowedApps() {
        gridApps = queryLauncherApps(LockSettings.allowedApps(this))
        gvAllowedApps.adapter = AppGridAdapter(this, gridApps)
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

        AlertDialog.Builder(this)
            .setTitle("Uygulama Seç (Maksimum ${LockSettings.MAX_APPS})")
            .setMultiChoiceItems(names, checked) { dialog, which, isChecked ->
                val pkg = apps[which].packageName
                if (!isChecked) {
                    selected.remove(pkg)
                } else if (selected.size >= LockSettings.MAX_APPS) {
                    checked[which] = false
                    (dialog as AlertDialog).listView.setItemChecked(which, false)
                    toast("En fazla ${LockSettings.MAX_APPS} uygulama seçebilirsiniz!")
                } else {
                    selected.add(pkg)
                }
            }
            .setPositiveButton("Kaydet") { _, _ ->
                LockSettings.setAllowedApps(this, selected)
                toast("${selected.size} Uygulama Kaydedildi.")
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // ---- Yardımcılar ----------------------------------------------------------------------------

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
        const val UNLOCK_PASSWORD = "EnoliBilge4317"
        val COLOR_IDLE: Int = Color.parseColor("#4CAF50")
        val COLOR_ARMED: Int = Color.parseColor("#FF9800")
    }
}
