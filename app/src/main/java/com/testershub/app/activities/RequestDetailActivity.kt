package com.testershub.app.activities

import android.os.Bundle
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
import android.content.Intent
import android.net.Uri

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

    private fun loadRequestDetails(id: String) {
        db.collection("testingRequests").document(id)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    val request = snapshot.toObject(TestingRequest::class.java)
                    request?.let { updateUI(it) }
                }
            }
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

    private fun updateUI(request: TestingRequest) {
        binding.tvAppName.text = request.appName
        binding.tvPackageName.text = request.packageName
        binding.tvInstructions.text = request.instructions
        binding.progressBar.max = request.testersRequired
        binding.progressBar.progress = request.joinedCount
        binding.tvProgressText.text = "${request.joinedCount} / ${request.testersRequired} testers"

        binding.tvPackageName.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(request.testingLink))
            startActivity(intent)
        }

        checkIfJoined(request.requestId)
    }

    private fun checkIfJoined(id: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("testingRequests").document(id)
            .collection("supporters").document(userId)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val isVerified = doc.getBoolean("verified") ?: false
                    binding.btnJoin.visibility = android.view.View.GONE
                    if (isVerified) {
                        binding.tvJoinedStatus.text = "Verified Tester ✓"
                        binding.tvJoinedStatus.setTextColor(android.graphics.Color.GREEN)
                    } else {
                        binding.tvJoinedStatus.text = "Joined (Pending Verification)"
                        binding.tvJoinedStatus.setTextColor(android.graphics.Color.GRAY)
                    }
                    binding.tvJoinedStatus.visibility = android.view.View.VISIBLE
                } else {
                    binding.btnJoin.visibility = android.view.View.VISIBLE
                    binding.tvJoinedStatus.visibility = android.view.View.GONE
                }
            }
    }

    private fun joinTesting() {
        val userId = auth.currentUser?.uid ?: return
        val rId = requestId ?: return

        // Disable button immediately to prevent double clicks
        binding.btnJoin.isEnabled = false

        db.runTransaction { transaction ->
            val requestRef = db.collection("testingRequests").document(rId)
            val supporterRef = requestRef.collection("supporters").document(userId)
            val userRef = db.collection("users").document(userId)

            val snapshot = transaction.get(supporterRef)
            if (snapshot.exists()) {
                throw Exception("Already joined")
            }

            val supporter = hashMapOf(
                "userId" to userId,
                "timestamp" to FieldValue.serverTimestamp(),
                "verified" to false
            )

            transaction.set(supporterRef, supporter)
            transaction.update(requestRef, "joinedCount", FieldValue.increment(1))
            transaction.update(userRef, "helpedCount", FieldValue.increment(1))
        }.addOnSuccessListener {
            Toast.makeText(this, "Joined successfully! Please download and keep the app for 14 days.", Toast.LENGTH_LONG).show()
            createNotification(rId)
        }.addOnFailureListener { e ->
            binding.btnJoin.isEnabled = true
            if (e.message == "Already joined") {
                Toast.makeText(this, "You have already joined this testing.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createNotification(rId: String) {
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
