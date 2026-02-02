package com.testershub.app.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.testershub.app.databinding.ActivityRequestDetailBinding
import com.testershub.app.models.TestingRequest

import androidx.recyclerview.widget.LinearLayoutManager
import com.testershub.app.adapters.SupporterAdapter
import com.testershub.app.models.Supporter

class RequestDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRequestDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var requestId: String? = null
    private val supporterList = mutableListOf<Supporter>()
    private lateinit var supporterAdapter: SupporterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSupportersRecyclerView()

        requestId = intent.getStringExtra("REQUEST_ID")
        requestId?.let { 
            loadRequestDetails(it)
            loadSupporters(it)
        }

        binding.btnJoin.setOnClickListener {
            joinTesting()
        }
    }

    private fun setupSupportersRecyclerView() {
        supporterAdapter = SupporterAdapter(supporterList)
        binding.rvSupporters.layoutManager = LinearLayoutManager(this)
        binding.rvSupporters.adapter = supporterAdapter
    }

    private fun loadSupporters(id: String) {
        db.collection("testingRequests").document(id)
            .collection("supporters")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    supporterList.clear()
                    for (doc in snapshot.documents) {
                        val supporter = doc.toObject(Supporter::class.java)
                        if (supporter != null) supporterList.add(supporter)
                    }
                    supporterAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun loadRequestDetails(id: String) {
        db.collection("testingRequests").document(id)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    val request = snapshot.toObject(TestingRequest::class.java)
                    request?.let { updateUI(it) }
                }
            }
    }

    private fun updateUI(request: TestingRequest) {
        binding.tvAppName.text = request.appName
        binding.tvPackageName.text = request.packageName
        binding.tvInstructions.text = request.instructions
        binding.progressBar.max = request.testersRequired
        binding.progressBar.progress = request.joinedCount
        binding.tvProgressText.text = "${request.joinedCount} / ${request.testersRequired} testers"

        checkIfJoined(request.requestId)
    }

    private fun checkIfJoined(id: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("testingRequests").document(id)
            .collection("supporters").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.btnJoin.visibility = View.GONE
                    binding.tvJoinedStatus.visibility = View.VISIBLE
                }
            }
    }

    private fun joinTesting() {
        val userId = auth.currentUser?.uid ?: return
        val rId = requestId ?: return

        val supporter = hashMapOf(
            "userId" to userId,
            "timestamp" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        val requestRef = db.collection("testingRequests").document(rId)
        val supporterRef = requestRef.collection("supporters").document(userId)
        val userRef = db.collection("users").document(userId)

        batch.set(supporterRef, supporter)
        batch.update(requestRef, "joinedCount", FieldValue.increment(1))
        batch.update(userRef, "helpedCount", FieldValue.increment(1))

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Joined successfully!", Toast.LENGTH_SHORT).show()
            createNotification(rId, userId)
        }
    }

    private fun createNotification(rId: String, testerId: String) {
        db.collection("testingRequests").document(rId).get().addOnSuccessListener { snapshot ->
            val ownerId = snapshot.getString("createdBy") ?: return@addOnSuccessListener
            val appName = snapshot.getString("appName") ?: "your app"
            
            val notification = hashMapOf(
                "message" to "Someone joined testing for $appName",
                "requestId" to rId,
                "timestamp" to FieldValue.serverTimestamp(),
                "read" to false
            )
            
            db.collection("notifications").document(ownerId)
                .collection("items").add(notification)
        }
    }
}
