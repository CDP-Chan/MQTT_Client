package com.rj.mqtt_client

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import java.util.Locale

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        val lang = SettingsManager.getLanguage()
        val locale = if (lang == "zh") Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
        val size = SettingsManager.getFontSize()
        val scale = when (size) {
            "small" -> 0.75f
            "large" -> 1.3f
            else -> 1.0f
        }
        val config = Configuration(newBase?.resources?.configuration)
        config.setLocale(locale)
        config.fontScale = scale
        val context = newBase?.createConfigurationContext(config)
        super.attachBaseContext(context ?: newBase)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        applyThemeColors()
    }

    private fun applyThemeColors() {
        val borderColor = parseColorSafely(SettingsManager.getBorderColor())
        val textColor = parseColorSafely(SettingsManager.getTextColor())

        val rootView = findViewById<View>(android.R.id.content)
        if (rootView != null) {
            traverseAndApply(rootView, borderColor, textColor)
        }
    }

    private fun traverseAndApply(view: View, borderColor: Int, textColor: Int) {
        when (view) {
            is MaterialButton -> {
                view.strokeColor = ColorStateList.valueOf(borderColor)
                if (view.currentTextColor != Color.WHITE) {
                    view.setTextColor(textColor)
                }
            }
            is SwitchCompat -> {
                view.trackTintList = ColorStateList.valueOf(borderColor)
                view.thumbTintList = ColorStateList.valueOf(borderColor)
            }
            is ImageButton -> {
                view.imageTintList = ColorStateList.valueOf(borderColor)
            }
            is TextView -> {
                if (view.currentTextColor != Color.WHITE && view.currentTextColor != -1) {
                    view.setTextColor(textColor)
                }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                traverseAndApply(view.getChildAt(i), borderColor, textColor)
            }
        }
    }

    protected fun parseColorSafely(color: String): Int {
        return try {
            Color.parseColor(color)
        } catch (e: Exception) {
            Color.parseColor("#39FF14")
        }
    }

    override fun onResume() {
        super.onResume()
        MqttMonitorService.isAppInForeground = true
    }

    override fun onPause() {
        super.onPause()
        MqttMonitorService.isAppInForeground = false
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is EditText) {
                val outRect = android.graphics.Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    view.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}