package com.omarea.vtools

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.support.v7.widget.SearchView
import android.view.View
import com.omarea.ui.DesktopAppsAdapter
import kotlinx.android.synthetic.main.activity_home.*
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)

        //透明状态栏/导航栏
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        // 使用壁纸高斯模糊作为窗口背景
        try {
            val wallPaper = WallpaperManager.getInstance(this).getDrawable();
            this.getWindow().setBackgroundDrawable(wallPaper);
            val source: Bitmap = rsBlur((wallPaper as BitmapDrawable).bitmap, 20)
            /*
            for (i in 0..5) {
                source = rsBlur(source, 20)
            }
            */
            this.getWindow().setBackgroundDrawable(BitmapDrawable(resources, source))
        } catch (ex: Exception) {

        }
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        loadApps()
        val adapter = DesktopAppsAdapter(apps!!, this, desktop_search.query.toString())
        desktop_apps.setAdapter(adapter)
        desktop_apps.setNestedScrollingEnabled(true)
        desktop_apps.setOnItemClickListener { parent, view, position, id ->
            val app = adapter.getItem(position) as ResolveInfo
            val pkg = app.activityInfo.packageName
            val cls = app.activityInfo.name
            val componet = ComponentName(pkg, cls)
            val intent = Intent()
            intent.component = componet
            startActivity(intent)
            //overridePendingTransition()
        }
        desktop_search.setOnQueryTextListener(object : SearchView.OnQueryTextListener, android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextChange(p0: String?): Boolean {
                try {
                    (desktop_apps.adapter as DesktopAppsAdapter).setKeywords(p0!!)
                } catch (ex: java.lang.Exception) {}
                return true
            }

            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }
        })
    }

    private var timer: Timer? = null
    private var myHandler = Handler()
    private var timerFormater = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
    private var dateFormater = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    override fun onResume() {
        super.onResume()

        stopTimer()
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                updateClock()
            }
        }, 0, 1000)
    }

    private fun stopTimer() {
        if (this.timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    private fun updateClock () {
        myHandler.post {
            desktop_time.text = timerFormater.format(Date(System.currentTimeMillis()))
            desktop_date.text = dateFormater.format(Date(System.currentTimeMillis()))
        }
    }

    private fun rsBlur(source: Bitmap, radius: Int): Bitmap {
        val inputBmp = source
        val renderScript = RenderScript.create(this);

        // Allocate memory for Renderscript to work with
        //(2)
        val input = Allocation.createFromBitmap(renderScript, inputBmp);
        val output = Allocation.createTyped(renderScript, input.getType());
        //(3)
        // Load up an instance of the specific script that we want to use.
        val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        //(4)
        scriptIntrinsicBlur.setInput(input);
        //(5)
        // Set the blur radius
        scriptIntrinsicBlur.setRadius(radius.toFloat());
        //(6)
        // Start the ScriptIntrinisicBlur
        scriptIntrinsicBlur.forEach(output);
        //(7)
        // Copy the output to the blurred bitmap
        output.copyTo(inputBmp);
        //(8)
        renderScript.destroy();

        return inputBmp;
    }

    private var apps: List<ResolveInfo>? = null

    private fun loadApps() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        apps = packageManager.queryIntentActivities(mainIntent, 0)
    }
}
