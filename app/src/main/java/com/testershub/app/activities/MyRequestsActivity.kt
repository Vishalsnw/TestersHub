package com.testershub.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.testershub.app.adapters.RequestAdapter
import com.testershub.app.databinding.ActivityMyRequestsBinding
import com.testershub.app.models.TestingRequest

class MyRequestsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyRequestsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: RequestAdapter
    private val requestList = mutableListOf<TestingRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadMyRequests()
    }

    private fun setupRecyclerView() {
        adapter = RequestAdapter(requestList) { request ->
            // Handle click
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadMyRequests() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("testingRequests")
            .whereEqualTo("createdBy", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    requestList.clear()
                    for (doc in snapshot.documents) {
                        val request = doc.toObject(TestingRequest::class.java)
                        if (request != null) requestList.add(request)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}
