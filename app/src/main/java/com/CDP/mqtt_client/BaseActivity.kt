package com.CDP.mqtt_client

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
        val commentColor = parseColorSafely(SettingsManager.getCommentColor())

        val rootView = findViewById<View>(android.R.id.content)
        if (rootView != null) {
            traverseAndApply(rootView, borderColor, textColor, commentColor)
        }
    }

    private fun traverseAndApply(view: View, borderColor: Int, textColor: Int, commentColor: Int) {
        when (view) {
            is MaterialButton -> {
                view.strokeColor = ColorStateList.valueOf(borderColor)
                view.setTextColor(textColor)
            }
            is SwitchCompat -> {
                view.trackTintList = ColorStateList.valueOf(borderColor)
                view.thumbTintList = ColorStateList.valueOf(borderColor)
            }
            is CompoundButton -> {
                view.buttonTintList = ColorStateList.valueOf(borderColor)
            }
            is ImageButton -> {
                view.imageTintList = ColorStateList.valueOf(borderColor)
            }
            is SeekBar -> {
                view.progressTintList = ColorStateList.valueOf(borderColor)
                view.thumbTintList = ColorStateList.valueOf(borderColor)
            }
            is EditText -> {
                view.setTextColor(textColor)
                if (view.tag != "fixed") {
                    view.setHintTextColor(commentColor)
                }
            }
            is TextView -> {
                when (view.tag) {
                    "comment" -> view.setTextColor(commentColor)
                    "fixed" -> Unit // 保持 XML 中锁定的颜色（如帮助文档的纯白正文）
                    else -> view.setTextColor(textColor)
                }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                traverseAndApply(view.getChildAt(i), borderColor, textColor, commentColor)
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

    /** 统一风格的确认弹窗：标题、正文与按钮文字全部跟随用户自定义的文字颜色。 */
    protected fun showThemedDialog(
        title: String?,
        message: String?,
        positiveText: String? = null,
        negativeText: String? = null,
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null
    ): AlertDialog {
        val builder = AlertDialog.Builder(this)
        if (title != null) builder.setTitle(title)
        if (message != null) builder.setMessage(message)
        if (positiveText != null) {
            builder.setPositiveButton(positiveText) { _, _ -> onPositive?.invoke() }
        }
        if (negativeText != null) {
            builder.setNegativeButton(negativeText) { _, _ -> onNegative?.invoke() }
        }
        val dialog = builder.create()
        dialog.show()

        val textColor = parseColorSafely(SettingsManager.getTextColor())
        dialog.findViewById<TextView>(com.google.android.material.R.id.alertTitle)?.setTextColor(textColor)
        dialog.findViewById<TextView>(android.R.id.title)?.setTextColor(textColor)
        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(textColor)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(textColor)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(textColor)
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(textColor)
        return dialog
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
