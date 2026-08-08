package com.CDP.mqtt_client

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class AddTopicActivity : BaseActivity() {
    private var pendingSave = false

    private val requestWritePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                performSave()
            } else {
                pendingSave = false
                Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_topic)

        findViewById<View>(android.R.id.content).setOnClickListener { hideKeyboard() }

        val etTopic = findViewById<EditText>(R.id.etTopic)
        val etUrl = findViewById<EditText>(R.id.etUrl)
        findViewById<EditText>(R.id.etName).setText(R.string.default_topic_name)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val topic = etTopic.text.toString().trim()
            val url = etUrl.text.toString().trim()
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

            // 没有写入权限时先申请；用户不同意则拒绝添加主题
            if (!hasStorageWritePermission()) {
                requestStoragePermission()
                return@setOnClickListener
            }

            performSave()
        }
    }

    private fun performSave() {
        val etTopic = findViewById<EditText>(R.id.etTopic)
        val etUrl = findViewById<EditText>(R.id.etUrl)
        val etName = findViewById<EditText>(R.id.etName)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val cbNotify = findViewById<CheckBox>(R.id.cbNotify)

        val topic = etTopic.text.toString().trim()
        val url = etUrl.text.toString().trim()
        val name = etName.text.toString().trim().ifEmpty { getString(R.string.default_topic_name) }
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // 检查重复
        val existingTopics = SettingsManager.loadTopics()
        val duplicateTopic = existingTopics.any { it.topic == topic }
        val duplicateName = existingTopics.any { it.name == name }
        if (duplicateTopic || duplicateName) {
            Toast.makeText(this, R.string.duplicate_topic, Toast.LENGTH_SHORT).show()
            return
        }

        val newConfig = TopicConfig(topic, url, name, username, password, cbNotify.isChecked)
        val topics = existingTopics.toMutableList()
        topics.add(newConfig)
        SettingsManager.saveTopics(topics)
        Toast.makeText(this, R.string.add_success, Toast.LENGTH_SHORT).show()
        // 通知服务重新加载主题，避免新增主题后需要手动点“连接”
        startService(Intent(this, MqttMonitorService::class.java).apply {
            action = MqttMonitorService.ACTION_RELOAD_TOPICS
        })
        finish()
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingSave = true
            val dialog = showThemedDialog(
                title = getString(R.string.permission_storage_title),
                message = getString(R.string.storage_permission_msg),
                positiveText = getString(R.string.go_settings),
                negativeText = getString(R.string.cancel),
                onPositive = {
                    try {
                        startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    } catch (e: Exception) {
                        Toast.makeText(this, R.string.goto_settings_failed, Toast.LENGTH_SHORT).show()
                    }
                },
                onNegative = {
                    pendingSave = false
                    Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show()
                }
            )
            dialog.setOnCancelListener { pendingSave = false }
        } else {
            requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun onResume() {
        super.onResume()
        if (pendingSave && hasStorageWritePermission()) {
            pendingSave = false
            performSave()
        } else if (pendingSave) {
            pendingSave = false
            Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("pendingSave", pendingSave)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        pendingSave = savedInstanceState.getBoolean("pendingSave", false)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }
}
