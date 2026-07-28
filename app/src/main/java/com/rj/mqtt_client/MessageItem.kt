package com.rj.mqtt_client

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MessageItem(
    val id: Long = System.currentTimeMillis(),
    val topic: String,
    val content: String,
    val timestamp: String = SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(Date())
)