package com.rj.mqtt_client

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup

class WelcomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 如果不是首次设置，直接跳转到主页（语言已在 BaseActivity 中应用）
        if (!SettingsManager.getFirstSetting()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_welcome)
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupLanguage)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)
        radioGroup.check(R.id.radioChinese)

        btnConfirm.setOnClickListener {
            val selectedLang = if (radioGroup.checkedRadioButtonId == R.id.radioEnglish) "en" else "zh"
            SettingsManager.setLanguage(selectedLang)
            SettingsManager.setFirstSetting(false)

            // 直接进入主页，无需手动刷新语言（BaseActivity 的 attachBaseContext 会处理）
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}