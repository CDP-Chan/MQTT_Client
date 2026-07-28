package com.rj.mqtt_client

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast

class TopicSettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic_settings)

        // 点击空白处隐藏键盘（已在 XML 中设置 clickable 等属性）
        findViewById<android.view.View>(android.R.id.content).setOnClickListener { hideKeyboard() }

        val etName = findViewById<EditText>(R.id.etName).apply {
            setText(intent.getStringExtra("name"))
        }
        val etUrl = findViewById<EditText>(R.id.etUrl).apply {
            setText(intent.getStringExtra("url"))
        }
        val etUsername = findViewById<EditText>(R.id.etUsername).apply {
            setText(intent.getStringExtra("username") ?: "")
        }
        val etPassword = findViewById<EditText>(R.id.etPassword).apply {
            setText(intent.getStringExtra("password") ?: "")
        }
        val cbNotify = findViewById<CheckBox>(R.id.cbNotify).apply {
            isChecked = intent.getBooleanExtra("notify", false)
        }
        val originalTopic = intent.getStringExtra("topic") ?: return
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val url = etUrl.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.username_password_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val topics = SettingsManager.loadTopics().toMutableList()
            val index = topics.indexOfFirst { it.topic == originalTopic }
            if (index >= 0) {
                topics[index] = topics[index].copy(
                    name = name,
                    url = url,
                    username = username,
                    password = password,
                    notifyLockScreen = cbNotify.isChecked
                )
                SettingsManager.saveTopics(topics)
                Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()

                // 重启MQTT服务以应用新配置
                stopService(Intent(this, MqttMonitorService::class.java))
                startService(Intent(this, MqttMonitorService::class.java))

                finish()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }
}