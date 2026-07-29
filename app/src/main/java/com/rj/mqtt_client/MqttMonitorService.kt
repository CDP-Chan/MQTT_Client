package com.rj.mqtt_client

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class MqttMonitorService : Service() {
    companion object {
        private const val TAG = "MqttService"
        const val CHANNEL_ID = "mqtt_notify"
        const val NOTIFICATION_ID = 101

        const val ACTION_SEND_MESSAGE = "SEND_MESSAGE"
        const val ACTION_RELOAD_TOPICS = "com.rj.mqtt_client.RELOAD_TOPICS"
        const val ACTION_UPDATE_HEARTBEAT = "UPDATE_HEARTBEAT"
        const val ACTION_LOCAL_NOTIFY = "LOCAL_NOTIFY"
        const val ACTION_UPDATE_KEEP_ALIVE = "UPDATE_KEEP_ALIVE"
        const val ACTION_CONNECTION_SUCCESS = "com.rj.mqtt_client.CONNECTION_SUCCESS"

        @Volatile var isAppInForeground = false
        @Volatile var isAnyConnected = false
    }

    private val clients = ConcurrentHashMap<String, MqttAsyncClient>()
    private var wakeLock: PowerManager.WakeLock? = null
    private var hasConnectedBefore = false
    private var heartbeatHandler: Handler? = null
    private var heartbeatRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        appendLog("Service started: ${formatNow()}")
        createNotificationChannel()
        acquireWakeLock()
        if (SettingsManager.isSendMessageEnabled()) {
            startForeground(NOTIFICATION_ID, createForegroundNotification(getLocalizedString(R.string.mqtt_connecting)))
        }
        scheduleHeartbeat()
        thread(name = "MqttInit") { connectAllTopics() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SEND_MESSAGE -> {
                val topic = intent.getStringExtra("topic")
                val payload = intent.getStringExtra("payload")
                if (topic != null && payload != null) sendMqttMessage(topic, payload)
            }
            ACTION_RELOAD_TOPICS -> {
                isAnyConnected = false
                clients.values.forEach { try { it.disconnect() } catch (_: Exception) {} }
                clients.clear()
                hasConnectedBefore = false
                thread(name = "MqttInit") { connectAllTopics() }
            }
            ACTION_UPDATE_HEARTBEAT -> scheduleHeartbeat()
            ACTION_LOCAL_NOTIFY -> {
                val title = intent.getStringExtra("title")
                val content = intent.getStringExtra("content")
                if (title != null && content != null) showLocalNotification(title, content)
            }
            ACTION_UPDATE_KEEP_ALIVE -> {
                if (SettingsManager.isSendMessageEnabled()) {
                    updateForegroundNotification(getLocalizedString(if (hasConnectedBefore) R.string.mqtt_connected else R.string.mqtt_connecting))
                }
                scheduleHeartbeat()
            }
        }
        return START_STICKY
    }

    private fun scheduleHeartbeat() {
        val previousRunnable = heartbeatRunnable
        previousRunnable?.let { heartbeatHandler?.removeCallbacks(it) }
        heartbeatRunnable = null
        if (!SettingsManager.isHeartbeatEnabled()) return

        val handler = heartbeatHandler ?: Handler(Looper.getMainLooper()).also { heartbeatHandler = it }
        heartbeatRunnable = object : Runnable {
            override fun run() {
                clients.values.forEach { client ->
                    if (client.isConnected) {
                        try {
                            client.publish("heartbeat", MqttMessage("ping".toByteArray()).apply { qos = 0 })
                        } catch (_: Exception) {}
                    }
                }
                handler.postDelayed(this, 5 * 60 * 1000)
            }
        }
        handler.postDelayed(heartbeatRunnable!!, 5 * 60 * 1000)
    }

    private fun sendMqttMessage(topic: String, payload: String) {
        val client = clients[topic] ?: return
        if (client.isConnected) {
            try {
                val message = MqttMessage(payload.toByteArray()).apply { qos = 1 }
                client.publish(topic, message)
            } catch (e: Exception) { Log.e(TAG, "Publish error", e) }
        }
    }

    private fun connectTopic(config: TopicConfig) {
        Log.d(TAG, "connectTopic: ${config.name} at ${config.url}")
        try {
            val clientId = "AndroidClient_${UUID.randomUUID()}"
            val client = MqttAsyncClient(config.url, clientId, MemoryPersistence())
            client.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d(TAG, "MQTT connected to ${config.url}")
                    try {
                        client.subscribe(config.topic, 1)
                    } catch (e: Exception) { Log.e(TAG, "Subscribe failed", e) }
                    clients[config.topic] = client
                    isAnyConnected = true
                    if (!hasConnectedBefore) {
                        hasConnectedBefore = true
                        // 发送广播通知连接成功
                        sendBroadcast(Intent(ACTION_CONNECTION_SUCCESS))
                        if (SettingsManager.isSendMessageEnabled()) {
                            updateForegroundNotification(getLocalizedString(R.string.mqtt_connected))
                        }
                    }
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (topic != null && message != null) {
                        val payload = message.toString()
                        MessageStorage.addMessage(MessageItem(topic = config.topic, content = "[${config.name}] $payload"))
                        if (SettingsManager.isSendMessageEnabled() && !isAppInForeground && config.notifyLockScreen) {
                            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            sendNotification(time, config.name, payload)
                        }
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "Connection lost: ${cause?.message}")
                    if (clients.values.none { it.isConnected }) {
                        isAnyConnected = false
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            val options = MqttConnectOptions().apply {
                keepAliveInterval = 60
                isAutomaticReconnect = true
                if (config.username.isNotEmpty()) {
                    userName = config.username
                    password = config.password.toCharArray()
                }
            }
            client.connect(options)
        } catch (e: Exception) { Log.e(TAG, "MQTT init failed for ${config.topic}", e) }
    }

    private fun connectAllTopics() {
        val topics = SettingsManager.loadTopics()
        if (topics.isEmpty()) {
            isAnyConnected = false
            if (SettingsManager.isSendMessageEnabled()) {
                updateForegroundNotification(getLocalizedString(R.string.mqtt_no_topics))
            }
            return
        }
        topics.forEach { connectTopic(it) }
    }

    private fun sendNotification(time: String, name: String, msg: String) {
        if (!SettingsManager.isSendMessageEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val prefix = getLocalizedString(R.string.notification_prefix)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$prefix - $name")
            .setContentText(msg)
            .setSubText(time)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        getSystemService(NotificationManager::class.java).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun showLocalNotification(title: String, content: String) {
        if (!SettingsManager.isSendMessageEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        getSystemService(NotificationManager::class.java).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG::WakeLock")
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getLocalizedString(R.string.mqtt_service_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Live status and system messages" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun updateForegroundNotification(status: String?) {
        if (!SettingsManager.isSendMessageEnabled()) return
        val notification = createForegroundNotification(status)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun createForegroundNotification(status: String?): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getLocalizedString(R.string.mqtt_service_title))
            .setContentText(status ?: "")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun getLocalizedString(resId: Int): String {
        val lang = SettingsManager.getLanguage()
        val locale = if (lang == "zh") Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
        val config = Configuration(resources.configuration).apply { setLocale(locale) }
        return createConfigurationContext(config).getString(resId)
    }

    private fun appendLog(message: String) {
        try {
            val file = File(Environment.getExternalStorageDirectory(), "Mqtt/debug_log.txt")
            file.appendText("$message\n")
        } catch (e: Exception) { Log.e(TAG, "Failed to write log", e) }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, this::class.java)
        val pi = PendingIntent.getService(
            applicationContext, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        (getSystemService(ALARM_SERVICE) as AlarmManager).set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pi)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        heartbeatHandler?.removeCallbacks(heartbeatRunnable ?: return)
        clients.values.forEach { try { it.disconnect() } catch (_: Exception) {} }
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun formatNow(): String =
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
}