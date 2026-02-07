package com.testershub.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.testershub.app.databinding.ItemTestingRequestBinding
import com.testershub.app.models.TestingRequest

class RequestAdapter(private val requests: List<TestingRequest>, private val onItemClick: (TestingRequest) -> Unit) :
    RecyclerView.Adapter<RequestAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTestingRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTestingRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.binding.tvAppName.text = request.appName
        holder.binding.tvTestersCount.text = "${request.joinedCount} of ${request.testersRequired} testers joined"
        
        holder.binding.root.setOnClickListener { onItemClick(request) }
    }

    override fun getItemCount() = requests.size
}
