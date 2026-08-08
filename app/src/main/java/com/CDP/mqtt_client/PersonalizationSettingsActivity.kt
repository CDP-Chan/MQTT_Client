package com.CDP.mqtt_client

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import com.CDP.mqtt_client.databinding.ActivityPersonalizationSettingsBinding

class PersonalizationSettingsActivity : BaseActivity() {
    private lateinit var binding: ActivityPersonalizationSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalizationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 恢复字体大小
        val savedFontSize = SettingsManager.getFontSize()
        when (savedFontSize) {
            "small" -> binding.radioGroupFontSize.check(R.id.radioFontSmall)
            "large" -> binding.radioGroupFontSize.check(R.id.radioFontLarge)
            else -> binding.radioGroupFontSize.check(R.id.radioFontStandard)
        }

        // 恢复边框颜色（使用简短命名避免后续变量遮蔽）
        val borderColor = SettingsManager.getBorderColor()
        val (br, bg, bb) = parseRGB(borderColor)
        binding.seekBarBorderR.progress = br
        binding.seekBarBorderG.progress = bg
        binding.seekBarBorderB.progress = bb
        updateBorderPreviewAndValues(br, bg, bb)

        // 恢复文字颜色
        val textColor = SettingsManager.getTextColor()
        val (tr, tg, tb) = parseRGB(textColor)
        binding.seekBarTextR.progress = tr
        binding.seekBarTextG.progress = tg
        binding.seekBarTextB.progress = tb
        updateTextPreviewAndValues(tr, tg, tb)

        // 恢复注释颜色
        val commentColor = SettingsManager.getCommentColor()
        val (cr, cg, cb) = parseRGB(commentColor)
        binding.seekBarCommentR.progress = cr
        binding.seekBarCommentG.progress = cg
        binding.seekBarCommentB.progress = cb
        updateCommentPreviewAndValues(cr, cg, cb)

        // 边框颜色 SeekBar 监听
        val borderListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val r = binding.seekBarBorderR.progress
                val g = binding.seekBarBorderG.progress
                val b = binding.seekBarBorderB.progress
                updateBorderPreviewAndValues(r, g, b)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        binding.seekBarBorderR.setOnSeekBarChangeListener(borderListener)
        binding.seekBarBorderG.setOnSeekBarChangeListener(borderListener)
        binding.seekBarBorderB.setOnSeekBarChangeListener(borderListener)

        // 文字颜色 SeekBar 监听
        val textListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val r = binding.seekBarTextR.progress
                val g = binding.seekBarTextG.progress
                val b = binding.seekBarTextB.progress
                updateTextPreviewAndValues(r, g, b)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        binding.seekBarTextR.setOnSeekBarChangeListener(textListener)
        binding.seekBarTextG.setOnSeekBarChangeListener(textListener)
        binding.seekBarTextB.setOnSeekBarChangeListener(textListener)

        // 注释颜色 SeekBar 监听
        val commentListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val r = binding.seekBarCommentR.progress
                val g = binding.seekBarCommentG.progress
                val b = binding.seekBarCommentB.progress
                updateCommentPreviewAndValues(r, g, b)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        binding.seekBarCommentR.setOnSeekBarChangeListener(commentListener)
        binding.seekBarCommentG.setOnSeekBarChangeListener(commentListener)
        binding.seekBarCommentB.setOnSeekBarChangeListener(commentListener)

        // 保存按钮
        binding.btnSavePersonalization.setOnClickListener {
            val selectedFontSize = when (binding.radioGroupFontSize.checkedRadioButtonId) {
                R.id.radioFontSmall -> "small"
                R.id.radioFontLarge -> "large"
                else -> "standard"
            }

            val selBorderR = binding.seekBarBorderR.progress
            val selBorderG = binding.seekBarBorderG.progress
            val selBorderB = binding.seekBarBorderB.progress
            val borderColorStr = rgbToHex(selBorderR, selBorderG, selBorderB)

            val selTextR = binding.seekBarTextR.progress
            val selTextG = binding.seekBarTextG.progress
            val selTextB = binding.seekBarTextB.progress
            val textColorStr = rgbToHex(selTextR, selTextG, selTextB)

            val selCommentR = binding.seekBarCommentR.progress
            val selCommentG = binding.seekBarCommentG.progress
            val selCommentB = binding.seekBarCommentB.progress
            val commentColorStr = rgbToHex(selCommentR, selCommentG, selCommentB)

            SettingsManager.setFontSize(selectedFontSize)
            SettingsManager.setBorderColor(borderColorStr)
            SettingsManager.setTextColor(textColorStr)
            SettingsManager.setCommentColor(commentColorStr)
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()

            // 彻底重启应用
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun updateBorderPreviewAndValues(r: Int, g: Int, b: Int) {
        updatePreview(binding.previewBorderColor, r, g, b)
        binding.tvBorderRValue.text = r.toString()
        binding.tvBorderGValue.text = g.toString()
        binding.tvBorderBValue.text = b.toString()
    }

    private fun updateTextPreviewAndValues(r: Int, g: Int, b: Int) {
        updatePreview(binding.previewTextColor, r, g, b)
        binding.tvTextRValue.text = r.toString()
        binding.tvTextGValue.text = g.toString()
        binding.tvTextBValue.text = b.toString()
    }

    private fun updateCommentPreviewAndValues(r: Int, g: Int, b: Int) {
        updatePreview(binding.previewCommentColor, r, g, b)
        binding.tvCommentRValue.text = r.toString()
        binding.tvCommentGValue.text = g.toString()
        binding.tvCommentBValue.text = b.toString()
    }

    private fun parseRGB(hexColor: String): Triple<Int, Int, Int> {
        return try {
            val color = Color.parseColor(hexColor)
            Triple(Color.red(color), Color.green(color), Color.blue(color))
        } catch (e: Exception) {
            Triple(57, 255, 20) // 默认绿色
        }
    }

    private fun rgbToHex(r: Int, g: Int, b: Int): String {
        return String.format("#%02X%02X%02X", r, g, b)
    }

    private fun updatePreview(view: android.view.View, r: Int, g: Int, b: Int) {
        view.setBackgroundColor(Color.rgb(r, g, b))
    }
}
