package com.CDP.mqtt_client

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class TopicSettingsActivity : BaseActivity() {
    private var pendingSave = false
    private var originalTopic: String? = null

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
        setContentView(R.layout.activity_topic_settings)

        // 点击空白处隐藏键盘（已在 XML 中设置 clickable 等属性）
        findViewById<android.view.View>(android.R.id.content).setOnClickListener { hideKeyboard() }

        findViewById<EditText>(R.id.etName).setText(intent.getStringExtra("name"))
        findViewById<EditText>(R.id.etUrl).setText(intent.getStringExtra("url"))
        val etUsername = findViewById<EditText>(R.id.etUsername).apply {
            setText(intent.getStringExtra("username") ?: "")
        }
        val etPassword = findViewById<EditText>(R.id.etPassword).apply {
            setText(intent.getStringExtra("password") ?: "")
        }
        findViewById<CheckBox>(R.id.cbNotify).isChecked = intent.getBooleanExtra("notify", false)
        originalTopic = intent.getStringExtra("topic") ?: return
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.username_password_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 没有写入权限时先申请；用户不同意则拒绝保存
            if (!hasStorageWritePermission()) {
                requestStoragePermission()
                return@setOnClickListener
            }

            performSave()
        }
    }

    private fun performSave() {
        val etName = findViewById<EditText>(R.id.etName)
        val etUrl = findViewById<EditText>(R.id.etUrl)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val cbNotify = findViewById<CheckBox>(R.id.cbNotify)

        val name = etName.text.toString().trim()
        val url = etUrl.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val original = originalTopic ?: return

        val topics = SettingsManager.loadTopics().toMutableList()
        val index = topics.indexOfFirst { it.topic == original }
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
