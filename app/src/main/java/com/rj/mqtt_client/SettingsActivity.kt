package com.rj.mqtt_client

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import java.util.Locale

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupLanguage)
        val btnSaveLanguage = findViewById<Button>(R.id.btnSaveLanguage)
        val btnFactoryReset = findViewById<Button>(R.id.btnFactoryReset)
        val switchHeartbeat = findViewById<SwitchCompat>(R.id.switchHeartbeat)
        val switchSendMessage = findViewById<SwitchCompat>(R.id.switchSendMessage)

        val currentLang = SettingsManager.getLanguage()
        radioGroup.check(if (currentLang == "en") R.id.radioEnglish else R.id.radioChinese)

        switchHeartbeat.isChecked = SettingsManager.isHeartbeatEnabled()
        switchSendMessage.isChecked = SettingsManager.isSendMessageEnabled()

        btnSaveLanguage.setOnClickListener {
            val selectedLang = if (radioGroup.checkedRadioButtonId == R.id.radioEnglish) "en" else "zh"
            SettingsManager.setLanguage(selectedLang)

            // 立即应用新语言到当前 Activity，确保通知文字正确
            val locale = if (selectedLang == "zh") Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)

            // 只有发送消息开关打开时才发送语言切换通知
            if (SettingsManager.isSendMessageEnabled()) {
                val langDisplay = if (selectedLang == "zh") "中文" else "English"
                val title = getString(R.string.language_changed_title)
                val content = getString(R.string.language_changed_content, langDisplay)

                val intent = Intent(this, MqttMonitorService::class.java).apply {
                    action = MqttMonitorService.ACTION_LOCAL_NOTIFY
                    putExtra("title", title)
                    putExtra("content", content)
                }
                startService(intent)
            }

            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                restartApplication()
            }, 100)
        }

        switchHeartbeat.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setHeartbeatEnabled(isChecked)
            startService(Intent(this, MqttMonitorService::class.java).apply {
                action = MqttMonitorService.ACTION_UPDATE_HEARTBEAT
            })
        }

        switchSendMessage.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setSendMessageEnabled(isChecked)
            startService(Intent(this, MqttMonitorService::class.java).apply {
                action = MqttMonitorService.ACTION_UPDATE_KEEP_ALIVE
            })
        }

        btnFactoryReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.factory_reset_title)
                .setMessage(R.string.factory_reset_msg)
                .setPositiveButton(R.string.ok) { _, _ ->
                    performFactoryReset()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun performFactoryReset() {
        SettingsManager.resetToDefaults()
        stopService(Intent(this, MqttMonitorService::class.java))
        restartApplication()
    }

    private fun restartApplication() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
        Process.killProcess(Process.myPid())
    }
}