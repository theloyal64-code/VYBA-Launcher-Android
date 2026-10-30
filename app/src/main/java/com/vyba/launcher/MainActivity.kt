package com.vyba.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.text.TextWatcher
import android.text.Editable
import kotlin.math.abs

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var home: LinearLayout
    private lateinit var drawer: LinearLayout
    private lateinit var appGrid: GridLayout
    private lateinit var search: EditText
    private val prefs by lazy { getSharedPreferences("vyba", MODE_PRIVATE) }
    private val shortcuts = linkedSetOf<String>()
    private var apps: List<ResolveInfo> = emptyList()
    private var open = false
    private var downX = 0f
    private var downY = 0f
    private var columns = 4
    private var iconSize = 52

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        columns = prefs.getInt("columns", 4)
        iconSize = prefs.getInt("iconSize", 52)
        shortcuts.addAll(prefs.getStringSet("shortcuts", emptySet()) ?: emptySet())
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        root = FrameLayout(this)
        try { root.background = WallpaperManager.getInstance(this).drawable }
        catch (_: Exception) { root.setBackgroundColor(Color.rgb(18,22,34)) }
        val shade = View(this).apply { setBackgroundColor(Color.BLACK); alpha=.20f }
        root.addView(shade, FrameLayout.LayoutParams(-1,-1))
        home = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER
            setPadding(dp(22),dp(56),dp(22),dp(28))
        }
        home.addView(TextView(this).apply {
            text="VYBA"; textSize=42f; letterSpacing=.22f; gravity=Gravity.CENTER
            setTextColor(Color.WHITE); typeface=android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(-1,dp(76)))
        home.addView(TextView(this).apply {
            text="YOUR PHONE. YOUR IDENTITY. YOUR RULES."; textSize=10f
            gravity=Gravity.CENTER; setTextColor(0xDDFFFFFF.toInt())
        })
        val gridScroll=ScrollView(this)
        val homeGrid=GridLayout(this).apply { columnCount=4 }
        gridScroll.addView(homeGrid)
        home.addView(gridScroll,LinearLayout.LayoutParams(-1,0,1f))
        home.addView(TextView(this).apply {
            text="+  ADD APPS"; textSize=13f; gravity=Gravity.CENTER
            setTextColor(Color.WHITE); background=bg(0x663A4658)
            setPadding(dp(22),dp(14),dp(22),dp(14))
            setOnClickListener { showDrawer() }
        })
        home.addView(TextView(this).apply {
            text="Swipe up to open all apps  ↑"; textSize=13f
            gravity=Gravity.CENTER; setTextColor(Color.WHITE); setPadding(0,dp(14),0,0)
        })
        root.addView(home,FrameLayout.LayoutParams(-1,-1))
        drawer=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(38),dp(18),dp(18))
            background=bg(0xF0182030.toInt())
        }
        drawer.addView(TextView(this).apply {
            text="ALL APPS"; textSize=20f; setTextColor(Color.WHITE)
        },LinearLayout.LayoutParams(-1,dp(44)))
        search=EditText(this).apply {
            hint="Search apps"; singleLine=true; textSize=15f
            setTextColor(Color.WHITE); setHintTextColor(0x99FFFFFF.toInt())
            background=bg(0x553A4658); setPadding(dp(14),0,dp(14),0)
        }
        drawer.addView(search)
        appGrid=GridLayout(this).apply { columnCount=columns }
        drawer.addView(ScrollView(this).apply { addView(appGrid) },LinearLayout.LayoutParams(-1,0,1f))
        root.addView(drawer,FrameLayout.LayoutParams(-1,-1))
        drawer.visibility=View.GONE
        search.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){ renderApps(s?.toString().orEmpty()) }
            override fun afterTextChanged(s:Editable?){}
        })
        renderHome(homeGrid)
        setContentView(root)
    }

    private fun showDrawer() {
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        apps=packageManager.queryIntentActivities(intent,0).filter { it.activityInfo.packageName!=packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        open=true; drawer.visibility=View.VISIBLE; search.setText(""); renderApps("")
    }
    private fun hideDrawer(){ open=false; drawer.visibility=View.GONE }
    private fun renderApps(filter:String) {
        if (!::appGrid.isInitialized) return
        appGrid.removeAllViews(); appGrid.columnCount=columns
        apps.filter { it.loadLabel(packageManager).toString().contains(filter,true) }.forEach { info ->
            val pkg=info.activityInfo.packageName
            val tile=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(dp(4),dp(10),dp(4),dp(10))
                setOnClickListener { packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) } }
                setOnLongClickListener {
                    shortcuts.add(pkg); prefs.edit().putStringSet("shortcuts",shortcuts.toSet()).apply()
                    Toast.makeText(this@MainActivity,"Added to home",Toast.LENGTH_SHORT).show(); true
                }
            }
            tile.addView(ImageView(this).apply { setImageDrawable(info.loadIcon(packageManager)); scaleType=ImageView.ScaleType.FIT_CENTER },
                LinearLayout.LayoutParams(dp(iconSize),dp(iconSize)))
            tile.addView(TextView(this).apply {
                text=info.loadLabel(packageManager); textSize=11f; gravity=Gravity.CENTER
                setTextColor(Color.WHITE); maxLines=2
            },LinearLayout.LayoutParams(-1,dp(36)))
            appGrid.addView(tile,GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED,1f),GridLayout.spec(GridLayout.UNDEFINED)).apply { width=0 })
        }
    }
    private fun renderHome(grid:GridLayout) {
        grid.removeAllViews()
        shortcuts.toList().forEach { pkg ->
            try {
                val ai=packageManager.getApplicationInfo(pkg,0)
                val tile=LinearLayout(this).apply {
                    orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(dp(6),dp(12),dp(6),dp(12))
                    setOnClickListener { packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) } }
                    setOnLongClickListener {
                        AlertDialog.Builder(this@MainActivity).setItems(arrayOf("Remove shortcut","Cancel")) { _,which ->
                            if(which==0){ shortcuts.remove(pkg); prefs.edit().putStringSet("shortcuts",shortcuts.toSet()).apply(); renderHome(grid) }
                        }.show(); true
                    }
                }
                tile.addView(ImageView(this).apply { setImageDrawable(packageManager.getApplicationIcon(ai)) },
                    LinearLayout.LayoutParams(dp(iconSize),dp(iconSize)))
                tile.addView(TextView(this).apply {
                    text=packageManager.getApplicationLabel(ai); textSize=10f; gravity=Gravity.CENTER
                    setTextColor(Color.WHITE); maxLines=1
                })
                grid.addView(tile,GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED,1f),GridLayout.spec(GridLayout.UNDEFINED)).apply { width=0 })
            } catch (_:Exception) { shortcuts.remove(pkg) }
        }
    }
    private fun bg(color:Int)=GradientDrawable().apply { setColor(color); cornerRadius=dp(18).toFloat() }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun dispatchTouchEvent(e:MotionEvent):Boolean {
        if(e.action==MotionEvent.ACTION_DOWN){downX=e.x;downY=e.y}
        if(e.action==MotionEvent.ACTION_UP){
            val dx=e.x-downX; val dy=e.y-downY
            if(abs(dy)>dp(70)&&abs(dy)>abs(dx)*1.2f){
                if(dy<0&&!open){showDrawer();return true}
                if(dy>0&&open){hideDrawer();return true}
            }
        }
        return super.dispatchTouchEvent(e)
    }
    @Deprecated("Deprecated in Android API")
    override fun onBackPressed(){ if(open) hideDrawer() else super.onBackPressed() }
}