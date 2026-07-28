package com.rj.mqtt_client

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rj.mqtt_client.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var topicAdapter: TopicAdapter

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestBatteryOptimization()
            } else {
                Toast.makeText(this, R.string.notify_reject, Toast.LENGTH_LONG).show()
                requestBatteryOptimization()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val startTime = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
        binding.tvStartTime.text = getString(R.string.start_time, startTime)

        binding.btnHelp.setOnClickListener { startActivity(Intent(this, HelpActivity::class.java)) }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        topicAdapter = TopicAdapter(
            onItemClick = { config ->
                val intent = Intent(this, TopicDetailActivity::class.java).apply {
                    putExtra("topic", config.topic)
                    putExtra("name", config.name)
                    putExtra("url", config.url)
                    putExtra("username", config.username)
                    putExtra("password", config.password)
                    putExtra("notify", config.notifyLockScreen)
                }
                startActivity(intent)
            },
            onDeleteClick = { config ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete_topic_title)
                    .setMessage(getString(R.string.delete_topic_msg, config.name))
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val topics = SettingsManager.loadTopics().toMutableList()
                        topics.removeAll { it.topic == config.topic }
                        SettingsManager.saveTopics(topics)
                        topicAdapter.setItems(topics)
                        Toast.makeText(this, R.string.delete_success, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )

        binding.recyclerViewTopics.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTopics.adapter = topicAdapter

        requestPermissions()

        binding.btnAddTopic.setOnClickListener { startActivity(Intent(this, AddTopicActivity::class.java)) }
        binding.btnDebug.setOnClickListener { startActivity(Intent(this, DebugActivity::class.java)) }

        binding.btnConnectMqtt.setOnClickListener {
            val message = if (MqttMonitorService.isAnyConnected) {
                getString(R.string.mqtt_connected_toast)
            } else {
                getString(R.string.mqtt_connecting_toast)
            }

            val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            snackbar.view.background = ResourcesCompat.getDrawable(resources, R.drawable.snackbar_bg, theme)
            val textView = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.setTextColor(Color.WHITE)
            snackbar.show()

            if (!MqttMonitorService.isAnyConnected) {
                val intent = Intent(this, MqttMonitorService::class.java).apply {
                    action = MqttMonitorService.ACTION_RELOAD_TOPICS
                }
                startService(intent)
            }
        }

        try {
            startService(Intent(this, MqttMonitorService::class.java))
            Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.service_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        requestBatteryOptimization()
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimization() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, R.string.battery_opt_fail, Toast.LENGTH_SHORT).show()
            }
        }
        checkAndRequestStoragePermission()
    }

    private fun checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.permission_storage_title)
                    .setMessage(R.string.permission_storage_msg)
                    .setPositiveButton(R.string.go_settings) { _, _ ->
                        try { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
                        catch (e: Exception) { Toast.makeText(this, R.string.goto_settings_failed, Toast.LENGTH_SHORT).show() }
                    }
                    .setNegativeButton(R.string.later, null)
                    .show()
            } else {
                loadTopicsAndSettings()
            }
        } else {
            loadTopicsAndSettings()
        }
    }

    private fun loadTopicsAndSettings() {
        try {
            topicAdapter.setItems(SettingsManager.loadTopics())
        } catch (e: Exception) {
            // 加载失败时静默忽略，用户可通过返回主界面重新触发加载
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) {
            loadTopicsAndSettings()
        }
    }
}