package com.rj.mqtt_client

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.rj.mqtt_client.databinding.ActivitySystemSettingsBinding

class SystemSettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySystemSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySystemSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchHeartbeat.isChecked = SettingsManager.isHeartbeatEnabled()
        binding.switchSendMessage.isChecked = SettingsManager.isSendMessageEnabled()

        binding.switchHeartbeat.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setHeartbeatEnabled(isChecked)
            startService(Intent(this, MqttMonitorService::class.java).apply { action = MqttMonitorService.ACTION_UPDATE_HEARTBEAT })
        }

        binding.switchSendMessage.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setSendMessageEnabled(isChecked)
            startService(Intent(this, MqttMonitorService::class.java).apply { action = MqttMonitorService.ACTION_UPDATE_KEEP_ALIVE })
        }

        binding.btnFactoryReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.factory_reset_title)
                .setMessage(R.string.factory_reset_msg)
                .setPositiveButton(R.string.ok) { _, _ ->
                    SettingsManager.resetToDefaults()
                    stopService(Intent(this, MqttMonitorService::class.java))
                    val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}