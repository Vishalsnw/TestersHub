package com.testershub.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.testershub.app.databinding.ItemSupporterBinding
import com.testershub.app.models.Supporter
import java.text.SimpleDateFormat
import java.util.*

class SupporterAdapter(private val supporters: List<Supporter>) :
    RecyclerView.Adapter<SupporterAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSupporterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSupporterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val supporter = supporters[position]
        holder.binding.tvUserId.text = "Tester: ${supporter.userId.take(8)}..."
        val date = supporter.timestamp?.toDate()
        if (date != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.binding.tvTimestamp.text = sdf.format(date)
        }
    }

    override fun getItemCount() = supporters.size
}
