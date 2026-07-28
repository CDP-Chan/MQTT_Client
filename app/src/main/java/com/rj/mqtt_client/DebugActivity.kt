package com.rj.mqtt_client

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.io.File

class DebugActivity : BaseActivity() {
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(20, 20, 20, 20)
        }
        val clearButton = Button(this).apply {
            text = getString(R.string.log_clear_title)
            setTextColor(Color.parseColor("#39FF14"))
            setOnClickListener {
                AlertDialog.Builder(this@DebugActivity)
                    .setTitle(R.string.log_clear_title)
                    .setMessage(R.string.log_clear_msg)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        File(filesDir, "debug_log.txt").delete()
                        textView.text = loadLog()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
        textView = TextView(this).apply {
            setTextColor(Color.parseColor("#39FF14"))
            text = loadLog()
        }
        val scrollView = ScrollView(this).apply { addView(textView) }
        rootLayout.addView(clearButton)
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(rootLayout)
    }

    private fun loadLog(): String {
        val file = File(filesDir, "debug_log.txt")
        return if (file.exists()) file.readText() else getString(R.string.no_log)
    }
}