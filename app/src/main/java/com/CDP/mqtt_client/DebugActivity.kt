package com.CDP.mqtt_client

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
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
        val clearButton = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.log_clear_title)
            setTextColor(Color.parseColor("#39FF14"))
            setOnClickListener {
                showThemedDialog(
                    title = getString(R.string.log_clear_title),
                    message = getString(R.string.log_clear_msg),
                    positiveText = getString(R.string.ok),
                    negativeText = getString(R.string.cancel),
                    onPositive = {
                        File(filesDir, "debug_log.txt").delete()
                        textView.text = loadLog()
                    }
                )
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
