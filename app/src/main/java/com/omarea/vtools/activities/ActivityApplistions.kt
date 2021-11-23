package com.omarea.vtools.activities

import android.content.Intent
import android.os.*
import android.provider.Settings
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.omarea.ui.SearchTextWatcher
import com.omarea.ui.TabIconHelper2
import com.omarea.vtools.R
import com.omarea.vtools.fragments.FragmentAppBackup
import com.omarea.vtools.fragments.FragmentAppSystem
import com.omarea.vtools.fragments.FragmentAppUser
import kotlinx.android.synthetic.main.activity_applictions.*

class ActivityApplistions : ActivityBase() {
    private var myHandler: Handler = UpdateHandler {
        reloadList()
    }

    private val fragmentAppUser = FragmentAppUser(myHandler)
    private val fragmentAppSystem = FragmentAppSystem(myHandler)
    private val fragmentAppBackup = FragmentAppBackup(myHandler)

    class UpdateHandler(private var updateList: Runnable) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 2) {
                updateList.run()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_retrieve, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_retrieve) {
            val intent = Intent(context, ActivityAppRetrieve::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applictions)

        setBackArrow()

        TabIconHelper2(tab_list, tab_content, this, supportFragmentManager).run {
            newTabSpec("Installed", ContextCompat.getDrawable(context, R.drawable.tab_app)!!, fragmentAppUser)
            newTabSpec("System", ContextCompat.getDrawable(context, R.drawable.tab_security)!!, fragmentAppSystem)
            newTabSpec("Backups", ContextCompat.getDrawable(context, R.drawable.tab_package)!!, fragmentAppBackup)
            tab_content.adapter = this.adapter
        }

        apps_search_box.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchApp(apps_search_box.text)
            }
            true
        }
        var lastInput = 0L
        apps_search_box.addTextChangedListener(SearchTextWatcher {
            val current = System.currentTimeMillis()
            lastInput = current
            myHandler.postDelayed({
                if (lastInput == current) {
                    searchApp(apps_search_box.text)
                }
            }, 500)
        })

        // 储存管理权限
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(this, "无法申请存储管理权限~", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        title = getString(R.string.menu_applictions)
    }

    private fun searchApp(text: Editable) {
        text.toString().run {
            fragmentAppUser.searchText = this
            fragmentAppSystem.searchText = this
            fragmentAppBackup.searchText = this
        }
    }

    private fun reloadList() {
        try {
            fragmentAppUser.reloadList()
        } catch (ex: Exception) {}
        try {
            fragmentAppSystem.reloadList()
        } catch (ex: Exception) {}
        try {
            fragmentAppBackup.reloadList()
        } catch (ex: Exception) {}
    }
}