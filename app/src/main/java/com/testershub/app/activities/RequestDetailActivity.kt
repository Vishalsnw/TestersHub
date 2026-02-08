package com.testershub.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
        
        // Add click listener for verification (only if owner)
        supporterAdapter.onItemClick = { supporter ->
            db.collection("testingRequests").document(requestId!!).get().addOnSuccessListener { snapshot ->
                if (snapshot.getString("createdBy") == auth.currentUser?.uid) {
                    showVerificationDialog(supporter)
                }
            }
        }
    }

    private fun showVerificationDialog(supporter: Supporter) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Verify Tester")
            .setMessage("Mark this tester as verified?")
            .setPositiveButton("Verify") { _, _ ->
                db.collection("testingRequests").document(requestId!!)
                    .collection("supporters").document(supporter.userId)
                    .update("verified", true)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tester verified", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            .orderBy("joinedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
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
        val currentUserId = auth.currentUser?.uid
        val isOwner = request.createdBy == currentUserId
        
        binding.tvAppName.text = request.appName
        binding.tvPackageName.text = if (isOwner) "${request.packageName} (Owner)" else request.packageName
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
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data ?: return
            uploadProofAndJoin(imageUri)
        } else {
            binding.btnJoin.isEnabled = true
        }
    }

    private fun uploadProofAndJoin(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val rId = requestId ?: return
        
        Toast.makeText(this, "Uploading proof...", Toast.LENGTH_SHORT).show()
        
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            .child("proofs/$rId/$userId.jpg")
            
        storageRef.putFile(imageUri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                completeJoinTransaction(downloadUri.toString())
            }
        }.addOnFailureListener {
            binding.btnJoin.isEnabled = true
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun completeJoinTransaction(proofUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        val rId = requestId ?: return

        db.runTransaction { transaction ->
            val requestRef = db.collection("testingRequests").document(rId)
            val supporterRef = requestRef.collection("supporters").document(userId)
            val userRef = db.collection("users").document(userId)

            val requestSnapshot = transaction.get(requestRef)
            val supporterSnapshot = transaction.get(supporterRef)
            val userSnapshot = transaction.get(userRef)

            if (supporterSnapshot.exists()) {
                throw Exception("Already joined")
            }

            val supporter = hashMapOf(
                "userId" to userId,
                "userName" to (auth.currentUser?.displayName ?: "Anonymous"),
                "joinedAt" to FieldValue.serverTimestamp(),
                "verified" to false,
                "proofUrl" to proofUrl
            )

            transaction.set(supporterRef, supporter)
            transaction.update(requestRef, "joinedCount", FieldValue.increment(1))
            
            if (userSnapshot.exists()) {
                transaction.update(userRef, "helpedCount", FieldValue.increment(1))
            } else {
                val newUser = hashMapOf(
                    "userId" to userId,
                    "helpedCount" to 1,
                    "requestedCount" to 0
                )
                transaction.set(userRef, newUser)
            }
        }.addOnSuccessListener {
            Toast.makeText(this, "Joined successfully!", Toast.LENGTH_LONG).show()
            createNotification(rId)
        }.addOnFailureListener { e ->
            binding.btnJoin.isEnabled = true
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
