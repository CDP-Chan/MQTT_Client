package com.rj.mqtt_client

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast

class AddTopicActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_topic)

        findViewById<View>(android.R.id.content).setOnClickListener { hideKeyboard() }

        val etTopic = findViewById<EditText>(R.id.etTopic)
        val etUrl = findViewById<EditText>(R.id.etUrl)
        val etName = findViewById<EditText>(R.id.etName).apply {
            setText(R.string.default_topic_name)
        }
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val cbNotify = findViewById<CheckBox>(R.id.cbNotify)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val topic = etTopic.text.toString().trim()
            val url = etUrl.text.toString().trim()
            val name = etName.text.toString().trim().ifEmpty { getString(R.string.default_topic_name) }
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (topic.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, R.string.field_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.username_password_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 检查重复
            val existingTopics = SettingsManager.loadTopics()
            val duplicateTopic = existingTopics.any { it.topic == topic }
            val duplicateName = existingTopics.any { it.name == name }
            if (duplicateTopic || duplicateName) {
                Toast.makeText(this, R.string.duplicate_topic, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newConfig = TopicConfig(topic, url, name, username, password, cbNotify.isChecked)
            val topics = existingTopics.toMutableList()
            topics.add(newConfig)
            SettingsManager.saveTopics(topics)
            Toast.makeText(this, R.string.add_success, Toast.LENGTH_SHORT).show()

            sendBroadcast(Intent("com.rj.mqtt_client.RELOAD_TOPICS"))
            finish()
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