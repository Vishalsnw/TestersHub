package com.testershub.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.testershub.app.R
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
        
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupBottomNavigation()
        listenForRequests()
        
        // Enable Firestore offline persistence
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
    }

    private fun setupRecyclerView() {
        adapter = RequestAdapter(requestList) { request ->
            val intent = Intent(this, RequestDetailActivity::class.java)
            intent.putExtra("REQUEST_ID", request.requestId)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun listenForRequests() {
        db.collection("testingRequests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    requestList.clear()
                    for (doc in snapshot.documents) {
                        try {
                            val request = doc.toObject(TestingRequest::class.java)
                            if (request != null) requestList.add(request)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> true
                R.id.nav_create -> {
                    startActivity(Intent(this, CreateRequestActivity::class.java))
                    true
                }
                R.id.nav_my_requests -> {
                    startActivity(Intent(this, MyRequestsActivity::class.java))
                    true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        Unit
    }
}
