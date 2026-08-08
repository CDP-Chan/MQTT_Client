package com.CDP.mqtt_client

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MessageStorage {
    private val gson = Gson()
    private val msgFile = SettingsManager.getMsgFilePath()

    fun loadMessages(): MutableList<MessageItem> {
        if (!msgFile.exists()) return mutableListOf()
        val json = msgFile.readText()
        return gson.fromJson(json, object : TypeToken<MutableList<MessageItem>>() {}.type) ?: mutableListOf()
    }

    fun saveMessages(messages: List<MessageItem>) = msgFile.writeText(gson.toJson(messages))

    fun addMessage(msg: MessageItem) {
        val list = loadMessages()
        list.add(0, msg)
        saveMessages(list)
    }

    fun getMessagesForTopic(topic: String): List<MessageItem> = loadMessages().filter { it.topic == topic }
}