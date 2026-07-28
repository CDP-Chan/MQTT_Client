package com.rj.mqtt_client

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rj.mqtt_client.databinding.ItemTopicCardBinding

class TopicAdapter(
    private val onItemClick: (TopicConfig) -> Unit,
    private val onDeleteClick: (TopicConfig) -> Unit
) : RecyclerView.Adapter<TopicAdapter.ViewHolder>() {

    private var items = listOf<TopicConfig>()

    inner class ViewHolder(val binding: ItemTopicCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopicCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val config = items[position]
        holder.binding.tvName.text = config.name
        holder.binding.tvUrl.text = config.url
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(config) }
        holder.binding.root.setOnClickListener { onItemClick(config) }
    }

    override fun getItemCount() = items.size

    fun setItems(newList: List<TopicConfig>) {
        val diffResult = DiffUtil.calculateDiff(TopicDiffCallback(items, newList))
        items = newList
        diffResult.dispatchUpdatesTo(this)
    }

    private class TopicDiffCallback(
        private val oldList: List<TopicConfig>,
        private val newList: List<TopicConfig>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].topic == newList[newItemPosition].topic

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}