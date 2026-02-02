package com.testershub.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.testershub.app.databinding.ItemNotificationBinding
import com.testershub.app.models.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        holder.binding.tvMessage.text = notification.message
        
        val date = notification.timestamp?.toDate()
        if (date != null) {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            holder.binding.tvTime.text = sdf.format(date)
        }
        
        holder.binding.root.alpha = if (notification.read) 0.6f else 1.0f
    }

    override fun getItemCount() = notifications.size
}
