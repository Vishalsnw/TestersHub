package com.testershub.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.testershub.app.adapters.RequestAdapter
import com.testershub.app.databinding.ActivityMainBinding
import com.testershub.app.models.TestingRequest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: RequestAdapter
    private val requestList = mutableListOf<TestingRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupBottomNavigation()
        listenForRequests()
    }

    private fun setupRecyclerView() {
        adapter = RequestAdapter(requestList) { request ->
            // Handle click - Navigate to details
            Toast.makeText(this, "Clicked: ${request.appName}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun listenForRequests() {
        db.collection("testingRequests")
            .whereEqualTo("status", "IN_PROGRESS")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading requests", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    requestList.clear()
                    for (doc in snapshot.documents) {
                        val request = doc.toObject(TestingRequest::class.java)
                        if (request != null) {
                            requestList.add(request)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                com.testershub.app.R.id.nav_home -> true
                com.testershub.app.R.id.nav_create -> {
                    // Start CreateRequestActivity
                    true
                }
                else -> false
            }
        }
    }
}
