package com.rj.mqtt_client

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.rj.mqtt_client.databinding.ActivityTopicDetailBinding

class TopicDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityTopicDetailBinding
    private val adapter = MessageAdapter()
    private val handler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null
    private var topicName = ""
    private var topicConfig: TopicConfig? = null
    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        topicName = intent.getStringExtra("topic") ?: ""
        binding.tvTopicName.text = intent.getStringExtra("name") ?: topicName
        topicConfig = SettingsManager.loadTopics().find { it.topic == topicName }

        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMessages.adapter = adapter

        // 显示加载文字
        binding.tvLoading.visibility = View.VISIBLE

        binding.btnSettings.setOnClickListener {
            topicConfig?.let { config ->
                startActivity(Intent(this, TopicSettingsActivity::class.java).apply {
                    putExtra("topic", config.topic)
                    putExtra("url", config.url)
                    putExtra("name", config.name)
                    putExtra("username", config.username)
                    putExtra("password", config.password)
                    putExtra("notify", config.notifyLockScreen)
                })
            }
        }

        binding.btnDeleteAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_all_messages_title)
                .setMessage(R.string.delete_all_messages_msg)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val allMessages = MessageStorage.loadMessages().toMutableList()
                    allMessages.removeAll { it.topic == topicName }
                    MessageStorage.saveMessages(allMessages)
                    adapter.setMessages(emptyList())
                    Toast.makeText(this, R.string.delete_success, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.btnSendMessage.setOnClickListener {
            val message = binding.editMessage.text.toString().trim()
            if (message.isEmpty()) {
                Toast.makeText(this, R.string.msg_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, MqttMonitorService::class.java).apply {
                action = MqttMonitorService.ACTION_SEND_MESSAGE
                putExtra("topic", topicName)
                putExtra("payload", message)
            }
            startService(intent)
            binding.editMessage.text.clear()
            Toast.makeText(this, R.string.send_success, Toast.LENGTH_SHORT).show()
        }

        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        refreshRunnable = object : Runnable {
            override fun run() {
                val messages = MessageStorage.getMessagesForTopic(topicName)
                adapter.setMessages(messages)
                if (isLoading) {
                    binding.tvLoading.visibility = View.GONE
                    isLoading = false
                }
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(refreshRunnable!!, 500) // 稍延迟以显示加载文字
    }

    override fun onDestroy() {
        refreshRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}