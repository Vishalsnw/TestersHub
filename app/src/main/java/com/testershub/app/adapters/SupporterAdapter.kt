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

    var onItemClick: ((Supporter) -> Unit)? = null

    class ViewHolder(val binding: ItemSupporterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSupporterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val supporter = supporters[position]
        holder.binding.tvUserId.text = "Tester: ${supporter.userId.take(8)}..."
        val date = supporter.joinedAt?.toDate()
        if (date != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.binding.tvTimestamp.text = sdf.format(date)
        }

        // Show proof if available
        if (!supporter.proofUrl.isNullOrEmpty()) {
            holder.binding.ivProof.visibility = android.view.View.VISIBLE
            com.bumptech.glide.Glide.with(holder.binding.root.context)
                .load(supporter.proofUrl)
                .into(holder.binding.ivProof)
            
            holder.binding.ivProof.setOnClickListener {
                // Open full image
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(supporter.proofUrl))
                holder.binding.root.context.startActivity(intent)
            }
        } else {
            holder.binding.ivProof.visibility = android.view.View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(supporter)
        }
    }

    override fun getItemCount() = supporters.size
}
