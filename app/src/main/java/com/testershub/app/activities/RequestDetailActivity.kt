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

class RequestDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRequestDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var requestId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestId = intent.getStringExtra("REQUEST_ID")
        requestId?.let { loadRequestDetails(it) }

        binding.btnJoin.setOnClickListener {
            joinTesting()
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
        }
    }
}
