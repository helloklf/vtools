package com.omarea.vtools.activities

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.support.v4.app.FragmentActivity
import android.view.View
import com.omarea.vtools.R
import com.omarea.vtools.fragments.FragmentFreeze

class ActivityFreezeApps : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ThemeSwitch.switchTheme(this)

        setContentView(R.layout.activity_freeze_apps)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        //  得到当前界面的装饰视图
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = getWindow().getDecorView();
            //让应用主题内容占用系统状态栏的空间,注意:下面两个参数必须一起使用 stable 牢固的
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            //设置状态栏颜色为透明
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, FragmentFreeze.createPage())
                .commit()

        // 使用壁纸高斯模糊作为窗口背景
        // val wallPaper = WallpaperManager.getInstance(this).getDrawable();
        // this.getWindow().setBackgroundDrawable(wallPaper);

        // this.getWindow().setBackgroundDrawable(BitmapDrawable(resources, rsBlur((wallPaper as BitmapDrawable).bitmap, 25)))
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

    override fun onPause() {
        super.onPause()
        System.gc()
    }
}