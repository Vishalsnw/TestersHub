package com.testershub.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.testershub.app.databinding.ItemTestingRequestBinding
import com.testershub.app.models.TestingRequest

class TestingRequestAdapter(
    private var requests: List<TestingRequest>,
    private val onItemClick: (TestingRequest) -> Unit,
    private val onJoinClick: (TestingRequest) -> Unit
) : RecyclerView.Adapter<TestingRequestAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTestingRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTestingRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.binding.apply {
            tvAppName.text = request.appName
            tvProgress.text = "${request.joinedCount} / ${request.testersRequired}"
            progressBar.max = request.testersRequired
            progressBar.progress = request.joinedCount
            
            btnViewDetails.setOnClickListener { onItemClick(request) }
            btnJoin.setOnClickListener { onJoinClick(request) }
        }
    }

    override fun getItemCount() = requests.size

    fun updateData(newRequests: List<TestingRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
}
