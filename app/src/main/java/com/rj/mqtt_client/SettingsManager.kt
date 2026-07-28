package com.rj.mqtt_client

import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object SettingsManager {
    private val baseDir = File(Environment.getExternalStorageDirectory(), "Mqtt")
    private val settingsFile = File(baseDir, "settings.json")
    private val topicsFile = File(baseDir, "topics.json")
    private val gson = Gson()

    fun init() {
        try {
            if (!baseDir.exists()) baseDir.mkdirs()
            if (!settingsFile.exists()) {
                val defaultSettings = mapOf(
                    "first_setting" to true,
                    "language" to "zh",
                    "heartbeat_enabled" to true,
                    "send_message" to true
                )
                writeSettingsFile(defaultSettings)
            }
            if (!topicsFile.exists()) {
                Log.d("SettingsManager", "topics.json not found, creating empty file")
                topicsFile.writeText(gson.toJson(emptyList<TopicConfig>()))
            } else {
                Log.d("SettingsManager", "topics.json exists, size: ${topicsFile.length()} bytes")
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "File init failed: ${e.message}")
        }
    }

    private fun readSettingsFile(): MutableMap<String, Any> {
        if (!settingsFile.exists()) return mutableMapOf()
        return try {
            val json = settingsFile.readText()
            val type = object : TypeToken<MutableMap<String, Any>>() {}.type
            gson.fromJson<MutableMap<String, Any>>(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            Log.e("SettingsManager", "Read settings failed", e)
            mutableMapOf()
        }
    }

    private fun writeSettingsFile(map: Map<String, Any>) {
        try {
            settingsFile.writeText(gson.toJson(map))
        } catch (e: Exception) {
            Log.e("SettingsManager", "Write settings failed", e)
        }
    }

    private fun updateSettingsFile(key: String, value: Any) {
        val map = readSettingsFile()
        map[key] = value
        writeSettingsFile(map)
    }

    fun getFirstSetting(): Boolean = (readSettingsFile()["first_setting"] as? Boolean) ?: true
    fun setFirstSetting(value: Boolean) = updateSettingsFile("first_setting", value)

    fun getLanguage(): String = (readSettingsFile()["language"] as? String) ?: "zh"
    fun setLanguage(lang: String) = updateSettingsFile("language", lang)

    fun isHeartbeatEnabled(): Boolean = (readSettingsFile()["heartbeat_enabled"] as? Boolean) ?: true
    fun setHeartbeatEnabled(enabled: Boolean) = updateSettingsFile("heartbeat_enabled", enabled)

    fun isSendMessageEnabled(): Boolean = (readSettingsFile()["send_message"] as? Boolean) ?: true
    fun setSendMessageEnabled(enabled: Boolean) = updateSettingsFile("send_message", enabled)

    fun saveTopics(topics: List<TopicConfig>) {
        try {
            topicsFile.writeText(gson.toJson(topics))
            Log.d("SettingsManager", "Topics saved, count: ${topics.size}")
        } catch (e: Exception) {
            Log.e("SettingsManager", "Save topics failed", e)
        }
    }

    fun loadTopics(): List<TopicConfig> {
        if (!topicsFile.exists()) {
            Log.w("SettingsManager", "topics.json does not exist")
            return emptyList()
        }
        return try {
            val json = topicsFile.readText()
            if (json.isBlank()) {
                Log.w("SettingsManager", "topics.json is empty")
                return emptyList()
            }
            val type = object : TypeToken<List<TopicConfig>>() {}.type
            val list: List<TopicConfig>? = gson.fromJson(json, type)
            Log.d("SettingsManager", "Loaded ${list?.size ?: 0} topics")
            list ?: emptyList()
        } catch (e: Exception) {
            Log.e("SettingsManager", "Load topics failed: ${e.message}")
            emptyList()
        }
    }

    fun getMsgFilePath(): File = File(baseDir, "local_MSG.json").also {
        if (!it.exists()) {
            try { it.createNewFile() } catch (e: Exception) {
                //
            }
        }
    }

    fun resetToDefaults() {
        val defaultSettings = mapOf(
            "first_setting" to true,
            "language" to "zh",
            "heartbeat_enabled" to true,
            "send_message" to true
        )
        writeSettingsFile(defaultSettings)
        try {
            if (topicsFile.exists()) topicsFile.delete()
            topicsFile.writeText(gson.toJson(emptyList<TopicConfig>()))
        } catch (e: Exception) {
            Log.e("SettingsManager", "Reset topics failed", e)
        }
    }
}

data class TopicConfig(
    val topic: String,
    val url: String,
    val name: String,
    val username: String = "",
    val password: String = "",
    val notifyLockScreen: Boolean = false
)