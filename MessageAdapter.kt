package com.whatsappvault.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.whatsappvault.R
import com.whatsappvault.database.MessageEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val onClick: (MessageEntity) -> Unit
) : ListAdapter<MessageEntity, MessageAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MessageEntity>() {
            override fun areItemsTheSame(a: MessageEntity, b: MessageEntity) = a.id == b.id
            override fun areContentsTheSame(a: MessageEntity, b: MessageEntity) = a == b
        }
        private val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderText: TextView = itemView.findViewById(R.id.tvSender)
        val messageText: TextView = itemView.findViewById(R.id.tvMessage)
        val timeText: TextView = itemView.findViewById(R.id.tvTime)
        val deletedBadge: TextView = itemView.findViewById(R.id.tvDeletedBadge)
        val groupBadge: TextView = itemView.findViewById(R.id.tvGroupBadge)
        val thumbnail: ImageView = itemView.findViewById(R.id.imgThumbnail)
        val imageIcon: ImageView = itemView.findViewById(R.id.imgImageIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = getItem(position)

        holder.senderText.text = msg.sender
        holder.messageText.text = when {
            msg.imageCopyPath != null && msg.message.isBlank() -> "[Image]"
            msg.imageCopyPath != null -> msg.message
            else -> msg.message
        }
        holder.timeText.text = sdf.format(Date(msg.timestamp))

        // Deleted badge
        holder.deletedBadge.visibility = if (msg.isDeleted) View.VISIBLE else View.GONE

        // Group badge
        holder.groupBadge.visibility = if (msg.isGroup) View.VISIBLE else View.GONE
        holder.groupBadge.text = msg.groupName ?: "Group"

        // Image thumbnail
        if (msg.imageCopyPath != null) {
            val file = File(msg.imageCopyPath)
            if (file.exists()) {
                holder.thumbnail.visibility = View.VISIBLE
                holder.imageIcon.visibility = View.GONE
                Glide.with(holder.itemView.context)
                    .load(file)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.thumbnail)
            } else {
                holder.thumbnail.visibility = View.GONE
                holder.imageIcon.visibility = View.VISIBLE
            }
        } else {
            holder.thumbnail.visibility = View.GONE
            holder.imageIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(msg) }
    }
}
