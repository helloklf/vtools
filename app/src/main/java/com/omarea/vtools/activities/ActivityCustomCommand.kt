package com.omarea.vtools.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_custom_command.*
import java.net.URLEncoder
import java.nio.charset.Charset

class ActivityCustomCommand : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_command)

        setBackArrow()

        btn_run.setOnClickListener {
            runCommand()
        }

        btn_confirm.setOnClickListener {
            val title = command_title.text?.toString()
            val script = command_script.text?.toString()
            if (title.isNullOrEmpty()) {
                Toast.makeText(this, "先输入一个标题吧！", Toast.LENGTH_SHORT).show()
            } else if (script.isNullOrEmpty()) {
                Toast.makeText(this, "脚本内容不能为空哦！", Toast.LENGTH_SHORT).show()
            } else {
                saveCommand(title, script)
            }
        }
    }

    private fun runCommand() {
        Toast.makeText(this, "还未实现此功能！", Toast.LENGTH_SHORT).show()
    }

    private fun saveCommand(title: String, script: String) {
        val fileContent = script.toByteArray(Charset.defaultCharset()) // TODO: 做 /r/n 转义处理
        val fileName = "custom-command/" + URLEncoder.encode(title) + ".sh"

        if (FileWrite.writePrivateFile(fileContent, fileName, this)) {
            val fullPath = FileWrite.getPrivateFilePath(context, fileName)
            setResult(RESULT_OK, Intent().apply {
                putExtra("path", fullPath)
            })
            finishAffinity()
        } else {
            Toast.makeText(this, "保存失败！", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_custom_command)
    }

    override fun onPause() {
        super.onPause()
    }
}
