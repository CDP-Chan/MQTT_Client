package com.rj.mqtt_client

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rj.mqtt_client.databinding.ItemMessageBinding

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    private var items = listOf<MessageItem>()

    inner class ViewHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = items[position]
        holder.binding.tvContent.text = msg.content
        holder.binding.tvTime.text = msg.timestamp
    }

    override fun getItemCount() = items.size

    fun setMessages(newList: List<MessageItem>) {
        items = newList
        notifyDataSetChanged()
    }
}