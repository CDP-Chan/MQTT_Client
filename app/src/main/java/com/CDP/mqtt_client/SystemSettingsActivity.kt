package com.CDP.mqtt_client

import android.content.Intent
import android.os.Bundle
import com.CDP.mqtt_client.databinding.ActivitySystemSettingsBinding

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
            showThemedDialog(
                title = getString(R.string.factory_reset_title),
                message = getString(R.string.factory_reset_msg),
                positiveText = getString(R.string.ok),
                negativeText = getString(R.string.cancel),
                onPositive = {
                    SettingsManager.resetToDefaults()
                    stopService(Intent(this, MqttMonitorService::class.java))
                    val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}
