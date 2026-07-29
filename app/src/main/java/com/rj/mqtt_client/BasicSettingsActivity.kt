package com.rj.mqtt_client

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import com.rj.mqtt_client.databinding.ActivityBasicSettingsBinding

class BasicSettingsActivity : BaseActivity() {
    private lateinit var binding: ActivityBasicSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBasicSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentLang = SettingsManager.getLanguage()
        binding.radioGroupLanguage.check(
            if (currentLang == "en") R.id.radioEnglish else R.id.radioChinese
        )

        binding.btnSaveLanguage.setOnClickListener {
            val selectedLang = if (binding.radioGroupLanguage.checkedRadioButtonId == R.id.radioEnglish) "en" else "zh"
            SettingsManager.setLanguage(selectedLang)
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()

            // 彻底清空任务栈并重启应用，确保语言全局生效
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
            // 强制终结进程，避免旧实例残留
            Process.killProcess(Process.myPid())
        }
    }
}