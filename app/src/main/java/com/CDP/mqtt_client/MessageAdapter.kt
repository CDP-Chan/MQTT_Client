package com.CDP.mqtt_client

import android.view.LayoutInflater
import android.view.ViewGroup
import android.graphics.Color
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.CDP.mqtt_client.databinding.ItemMessageBinding

class MessageAdapter : ListAdapter<MessageItem, MessageAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = getItem(position)
        holder.binding.tvTime.text = msg.timestamp
        holder.binding.tvContent.text = msg.content
        val textColor = parseColorSafely(SettingsManager.getTextColor())
        val commentColor = parseColorSafely(SettingsManager.getCommentColor())
        holder.binding.tvTime.setTextColor(commentColor)
        holder.binding.tvContent.setTextColor(textColor)
    }

    private fun parseColorSafely(color: String): Int {
        return try {
            Color.parseColor(color)
        } catch (e: Exception) {
            Color.parseColor("#39FF14")
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MessageItem>() {
        override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
            return oldItem == newItem
        }
    }
}
