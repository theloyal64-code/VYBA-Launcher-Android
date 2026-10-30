package com.vyba.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.text.TextWatcher
import android.text.Editable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var home: LinearLayout
    private lateinit var drawer: LinearLayout
    private lateinit var appGrid: GridLayout
    private lateinit var homeGrid: GridLayout
    private lateinit var search: EditText
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private val prefs by lazy { getSharedPreferences("vyba", MODE_PRIVATE) }
    private val shortcuts = linkedSetOf<String>()
    private val hidden = linkedSetOf<String>()
    private var apps: List<ResolveInfo> = emptyList()
    private var open = false
    private var downX = 0f
    private var downY = 0f
    private var columns = 4
    private var iconSize = 52
    private var light = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        columns = prefs.getInt("columns", 4).coerceIn(3, 6)
        iconSize = prefs.getInt("iconSize", 52).coerceIn(40, 72)
        light = prefs.getBoolean("light", false)
        shortcuts.addAll(prefs.getStringSet("shortcuts", emptySet()) ?: emptySet())
        hidden.addAll(prefs.getStringSet("hidden", emptySet()) ?: emptySet())
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        buildInterface()
    }

    private val bgColor get() = if (light) 0xFFF1F3F7.toInt() else 0xFF10141D.toInt()
    private val panelColor get() = if (light) 0xFFFFFFFF.toInt() else 0xFF1C2431.toInt()
    private val textColor get() = if (light) 0xFF17202D.toInt() else Color.WHITE
    private val mutedColor get() = if (light) 0xFF667085.toInt() else 0xFFADB7C6.toInt()
    private fun buildInterface() {
        root = FrameLayout(this).apply { setBackgroundColor(bgColor) }
        try {
            val wallpaper = WallpaperManager.getInstance(this).drawable
            if (wallpaper != null) root.background = wallpaper
        } catch (_: Exception) {}
        root.addView(View(this).apply {
            setBackgroundColor(if (light) 0x55FFFFFF else 0x88070B12.toInt())
        }, FrameLayout.LayoutParams(-1, -1))
        home = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(54), dp(22), dp(24))
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(label("VYBA", 30f, true, textColor).apply { letterSpacing = .22f })
        brand.addView(label("YOUR PHONE. YOUR IDENTITY.", 10f, false, textColor))
        top.addView(brand)
        top.addView(actionButton("⚙", 48) { showSettings() })
        home.addView(top)
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(30), 0, dp(22))
        }
        clock = label("", 46f, true, textColor)
        date = label("", 13f, false, mutedColor)
        info.addView(clock)
        info.addView(date)
        home.addView(info)
        val section = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        section.addView(label("FAVOURITES", 11f, true, textColor).apply { letterSpacing = .12f },
            LinearLayout.LayoutParams(0, -2, 1f))
        section.addView(label("EDIT", 10f, true, mutedColor).apply {
            setPadding(dp(12), dp(8), dp(4), dp(8))
            setOnClickListener { showDrawer() }
        })
        home.addView(section)
        val scroll = ScrollView(this)
        homeGrid = GridLayout(this).apply { columnCount = columns }
        scroll.addView(homeGrid)
        home.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        bottom.addView(actionButton("+     ADD APPS", 58) { showDrawer() }, LinearLayout.LayoutParams(-1, dp(58)))
        bottom.addView(label("Swipe up for all apps  ↑", 12f, false, mutedColor).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(16), 0, dp(4))
        })
        home.addView(bottom)
        root.addView(home, FrameLayout.LayoutParams(-1, -1))

        drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(42), dp(18), dp(18))
            setBackgroundColor(bgColor)
        }
        val drawerHeader = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        drawerHeader.addView(label("ALL APPS", 22f, true, textColor), LinearLayout.LayoutParams(0, dp(48), 1f))
        drawerHeader.addView(actionButton("✕", 42) { hideDrawer() })
        drawer.addView(drawerHeader)
        search = EditText(this).apply {
            hint = "Search apps"; setSingleLine(true); textSize = 15f
            setTextColor(textColor); setHintTextColor(mutedColor)
            background = rounded(panelColor, 18)
            setPadding(dp(16), 0, dp(16), 0)
        }
        drawer.addView(search, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(12) })
        appGrid = GridLayout(this).apply { columnCount = columns }
        drawer.addView(ScrollView(this).apply { addView(appGrid) }, LinearLayout.LayoutParams(-1, 0, 1f))
        drawer.visibility = View.GONE
        root.addView(drawer, FrameLayout.LayoutParams(-1, -1))
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderApps(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        setContentView(root)
        updateClock()
        renderHome()
    }

    private fun updateClock() {
        clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        date.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        apps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName && it.activityInfo.packageName !in hidden }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
    }

    private fun showDrawer() {
        loadApps()
        open = true
        drawer.visibility = View.VISIBLE
        search.setText("")
        renderApps("")
    }
    private fun hideDrawer() { open = false; drawer.visibility = View.GONE }

    private fun renderApps(filter: String) {
        if (!::appGrid.isInitialized) return
        appGrid.removeAllViews()
        appGrid.columnCount = columns
        val filtered = apps.filter { it.loadLabel(packageManager).toString().contains(filter, true) }
        filtered.forEach { info ->
            val pkg = info.activityInfo.packageName
            val tile = appTile(info, true)
            tile.setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) startActivity(launch)
                else Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show()
            }
            tile.setOnLongClickListener {
                AlertDialog.Builder(this).setTitle(info.loadLabel(packageManager))
                    .setItems(arrayOf("Add to home screen", "Hide this app", "Cancel")) { _, which ->
                        when (which) {
                            0 -> { shortcuts.add(pkg); saveSets(); renderHome(); Toast.makeText(this, "Added to favourites", Toast.LENGTH_SHORT).show() }
                            1 -> { hidden.add(pkg); saveSets(); loadApps(); renderApps(search.text.toString()) }
                        }
                    }.show()
                true
            }
            appGrid.addView(tile, gridParams())
        }
        if (filtered.isEmpty()) {
            appGrid.addView(label("No apps found", 14f, false, mutedColor).apply { gravity = Gravity.CENTER; setPadding(0, dp(28), 0, 0) })
        }
    }

    private fun appTile(info: ResolveInfo, drawerTile: Boolean): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(10), dp(4), dp(10))
            addView(ImageView(this@MainActivity).apply {
                setImageDrawable(info.loadIcon(packageManager))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(iconSize), dp(iconSize)))
            addView(label(info.loadLabel(packageManager).toString(), 10.5f, false, textColor).apply {
                gravity = Gravity.CENTER; maxLines = 2
            }, LinearLayout.LayoutParams(-1, dp(34)))
        }
    }

    private fun renderHome() {
        if (!::homeGrid.isInitialized) return
        homeGrid.removeAllViews()
        homeGrid.columnCount = columns
        shortcuts.toList().forEach { pkg ->
            try {
                val info = packageManager.getApplicationInfo(pkg, 0)
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent == null) return@forEach
                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                    setPadding(dp(4), dp(12), dp(4), dp(12))
                    setOnClickListener { startActivity(intent) }
                    setOnLongClickListener {
                        AlertDialog.Builder(this@MainActivity).setItems(arrayOf("Remove from home", "Cancel")) { _, which ->
                            if (which == 0) { shortcuts.remove(pkg); saveSets(); renderHome() }
                        }.show()
                        true
                    }
                }
                tile.addView(ImageView(this).apply { setImageDrawable(packageManager.getApplicationIcon(info)) },
                    LinearLayout.LayoutParams(dp(iconSize), dp(iconSize)))
                tile.addView(label(packageManager.getApplicationLabel(info).toString(), 10f, false, textColor).apply {
                    gravity = Gravity.CENTER; maxLines = 1
                })
                homeGrid.addView(tile, gridParams())
            } catch (_: Exception) { shortcuts.remove(pkg) }
        }
        saveSets()
    }

    private fun gridParams() = GridLayout.LayoutParams(
        GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED)
    ).apply { width = 0 }

    private fun showSettings() {
        val options = arrayOf(
            "Appearance: ${if (light) "Light" else "Dark"}",
            "Home grid: $columns columns",
            "App icon size: $iconSize dp",
            "Manage hidden apps (${hidden.size})",
            "Reset launcher preferences"
        )
        AlertDialog.Builder(this).setTitle("VYBA Settings").setItems(options) { _, which ->
            when (which) {
                0 -> { light = !light; prefs.edit().putBoolean("light", light).apply(); rebuild() }
                1 -> chooseValue("Home grid columns", arrayOf("3", "4", "5", "6"), columns.toString()) {
                    columns = it.toInt(); prefs.edit().putInt("columns", columns).apply(); rebuild()
                }
                2 -> chooseValue("Icon size", arrayOf("40", "48", "52", "60", "68", "72"), iconSize.toString()) {
                    iconSize = it.toInt(); prefs.edit().putInt("iconSize", iconSize).apply(); rebuild()
                }
                3 -> manageHidden()
                4 -> AlertDialog.Builder(this).setMessage("Reset favourites, hidden apps and display preferences?")
                    .setNegativeButton("Cancel", null).setPositiveButton("Reset") { _, _ ->
                        prefs.edit().clear().apply(); shortcuts.clear(); hidden.clear()
                        columns = 4; iconSize = 52; light = false; rebuild()
                    }.show()
            }
        }.show()
    }

    private fun chooseValue(title: String, values: Array<String>, selected: String, done: (String) -> Unit) {
        val index = values.indexOf(selected).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(values, index) { dialog, which ->
            done(values[which]); dialog.dismiss()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun manageHidden() {
        if (hidden.isEmpty()) {
            AlertDialog.Builder(this).setMessage("No hidden apps. Long-press an app in All Apps to hide it.")
                .setPositiveButton("OK", null).show()
            return
        }
        val names = hidden.map { pkg ->
            try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }
            catch (_: Exception) { pkg }
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Tap an app to unhide").setItems(names) { _, which ->
            hidden.remove(hidden.elementAt(which)); saveSets(); manageHidden()
        }.setNegativeButton("Done", null).show()
    }

    private fun rebuild() {
        buildInterface()
    }
    private fun saveSets() {
        prefs.edit().putStringSet("shortcuts", shortcuts.toSet()).putStringSet("hidden", hidden.toSet()).apply()
    }
    private fun label(text: String, size: Float, bold: Boolean, color: Int) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun actionButton(text: String, height: Int, action: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 13f; gravity = Gravity.CENTER; setTextColor(textColor)
        background = rounded(panelColor, 18); setPadding(dp(14), 0, dp(14), 0)
        setOnClickListener { action() }
        minWidth = dp(height)
        if (height > 50) minHeight = dp(height)
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) { downX = event.x; downY = event.y }
        if (event.action == MotionEvent.ACTION_UP) {
            val dx = event.x - downX; val dy = event.y - downY
            if (abs(dy) > dp(70) && abs(dy) > abs(dx) * 1.2f) {
                if (dy < 0 && !open) { showDrawer(); return true }
                if (dy > 0 && open) { hideDrawer(); return true }
            }
        }
        return super.dispatchTouchEvent(event)
    }
    override fun onResume() { super.onResume(); if (::clock.isInitialized) updateClock() }
    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() { if (open) hideDrawer() else super.onBackPressed() }
}
