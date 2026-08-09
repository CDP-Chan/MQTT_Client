package com.CDP.mqtt_client

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.CDP.mqtt_client.databinding.ItemTopicCardBinding

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
        // 卡片边框、删除图标与文字全部使用用户自定义颜色
        val borderColor = parseColorSafely(SettingsManager.getBorderColor())
        val textColor = parseColorSafely(SettingsManager.getTextColor())
        val commentColor = parseColorSafely(SettingsManager.getCommentColor())
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            setStroke(dpToPx(holder.itemView, 2), borderColor)
            cornerRadius = dpToPx(holder.itemView, 4).toFloat()
        }
        holder.binding.root.background = background
        holder.binding.btnDelete.imageTintList = ColorStateList.valueOf(borderColor)
        holder.binding.tvName.setTextColor(textColor)
        holder.binding.tvUrl.setTextColor(commentColor)
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(config) }
        holder.binding.root.setOnClickListener { onItemClick(config) }
    }

    override fun getItemCount() = items.size

    private fun parseColorSafely(color: String): Int {
        return try {
            Color.parseColor(color)
        } catch (e: Exception) {
            Color.parseColor("#39FF14")
        }
    }

    private fun dpToPx(view: android.view.View, dp: Int): Int {
        return (dp * view.resources.displayMetrics.density).toInt()
    }

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
