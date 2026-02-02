package com.testershub.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.testershub.app.adapters.NotificationAdapter
import com.testershub.app.databinding.ActivityNotificationsBinding
import com.testershub.app.models.Notification

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationList = mutableListOf<Notification>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notificationList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("notifications").document(userId)
            .collection("items")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    notificationList.clear()
                    for (doc in snapshot.documents) {
                        val notification = doc.toObject(Notification::class.java)
                        if (notification != null) notificationList.add(notification)
                    }
                    adapter.notifyDataSetChanged()
                    markAllAsRead(userId, snapshot.documents)
                }
            }
    }

    private fun markAllAsRead(userId: String, docs: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val batch = db.batch()
        for (doc in docs) {
            if (doc.getBoolean("read") == false) {
                batch.update(doc.reference, "read", true)
            }
        }
        batch.commit()
    }
}
