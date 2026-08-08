package com.CDP.mqtt_client
import android.content.Intent
import android.os.Bundle
import com.CDP.mqtt_client.databinding.ActivitySettingsBinding

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBasicSettings.setOnClickListener { startActivity(Intent(this, BasicSettingsActivity::class.java)) }
        binding.btnPersonalizationSettings.setOnClickListener { startActivity(Intent(this, PersonalizationSettingsActivity::class.java)) }
        binding.btnSystemSettings.setOnClickListener { startActivity(Intent(this, SystemSettingsActivity::class.java)) }
    }
}
