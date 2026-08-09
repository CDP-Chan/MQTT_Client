package com.CDP.mqtt_client

import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object SettingsManager {
    // 所有数据统一保存在应用内部 data，不依赖外部存储（外部存储部分机型不支持读写）
    private val baseDir: File by lazy { File(MyApplication.instance.filesDir, "Mqtt") }
    private val settingsFile: File by lazy { File(baseDir, "settings.json") }
    private val topicsFile: File by lazy { File(baseDir, "topics.json") }
    private val gson = Gson()

    private val defaultSettings = mapOf(
        "first_setting" to true,
        "language" to "zh",
        "heartbeat_enabled" to true,
        "send_message" to true,
        "font_size" to "standard",
        "border_color" to "#39FF14",
        "text_color" to "#39FF14",
        "comment_color" to "#FFFFFF"
    )

    fun init() {
        try {
            if (!baseDir.exists()) baseDir.mkdirs()
            migrateFromExternalStorage()
            if (!settingsFile.exists()) {
                writeSettingsFile(defaultSettings)
            }
            if (!topicsFile.exists()) {
                topicsFile.writeText(gson.toJson(emptyList<TopicConfig>()))
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "File init failed: ${e.message}")
        }
    }

    /**
     * 将旧版本存放在外部公共目录 /Mqtt 的数据一次性复制到内部 data。
     * 仅复制、不删除旧文件，保证除“恢复出厂设置”外任何情况下都不会删除数据。
     */
    private fun migrateFromExternalStorage() {
        try {
            val externalBase = File(Environment.getExternalStorageDirectory(), "Mqtt")
            if (!externalBase.exists()) return
            var migrated = false
            for (name in listOf("settings.json", "topics.json", "local_MSG.json")) {
                val src = File(externalBase, name)
                val dst = File(baseDir, name)
                if (src.exists() && !dst.exists()) {
                    src.copyTo(dst, overwrite = false)
                    migrated = true
                }
            }
            if (migrated) {
                Log.i("SettingsManager", "Migrated data from external storage to internal data")
            }
        } catch (e: Exception) {
            Log.w("SettingsManager", "External storage migration skipped: ${e.message}")
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

    fun getFontSize(): String = (readSettingsFile()["font_size"] as? String) ?: "standard"

    fun setFontSize(size: String) = updateSettingsFile("font_size", size)

    fun getBorderColor(): String = (readSettingsFile()["border_color"] as? String) ?: "#39FF14"

    fun setBorderColor(color: String) = updateSettingsFile("border_color", color)

    fun getTextColor(): String = (readSettingsFile()["text_color"] as? String) ?: "#39FF14"

    fun setTextColor(color: String) = updateSettingsFile("text_color", color)

    fun getCommentColor(): String = (readSettingsFile()["comment_color"] as? String) ?: "#FFFFFF"

    fun setCommentColor(color: String) = updateSettingsFile("comment_color", color)

    fun saveTopics(topics: List<TopicConfig>) {
        try {
            topicsFile.writeText(gson.toJson(topics))
        } catch (e: Exception) {
            Log.e("SettingsManager", "Save topics failed", e)
        }
    }

    fun loadTopics(): List<TopicConfig> {
        if (!topicsFile.exists()) return emptyList()
        return try {
            val json = topicsFile.readText()
            val type = object : TypeToken<List<TopicConfig>>() {}.type
            val list: List<TopicConfig>? = gson.fromJson(json, type)
            list ?: emptyList()
        } catch (e: Exception) {
            Log.e("SettingsManager", "Load topics failed", e)
            emptyList()
        }
    }

    fun getMsgFilePath(): File = File(baseDir, "local_MSG.json").also {
        if (!it.exists()) {
            try {
                it.createNewFile()
            } catch (e: Exception) {
                //
            }
        }
    }

    /** 仅“恢复出厂设置”允许删除/重置数据，其他任何入口不得调用。 */
    fun resetToDefaults() {
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
