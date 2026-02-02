package com.testershub.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.testershub.app.databinding.ActivityCreateRequestBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.Date

class CreateRequestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateRequestBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSubmit.setOnClickListener {
            submitRequest()
        }
    }

    private fun submitRequest() {
        val appName = binding.etAppName.text.toString()
        val packageName = binding.etPackageName.text.toString()
        val testersRequired = binding.etTestersRequired.text.toString().toIntOrNull() ?: 0

        if (appName.isEmpty() || packageName.isEmpty() || testersRequired <= 0) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val requestId = db.collection("testingRequests").document().id

        val request = hashMapOf(
            "requestId" to requestId,
            "appName" to appName,
            "packageName" to packageName,
            "testersRequired" to testersRequired,
            "joinedCount" to 0,
            "createdBy" to userId,
            "status" to "IN_PROGRESS",
            "createdAt" to Timestamp.now()
        )

        db.collection("testingRequests").document(requestId).set(request)
            .addOnSuccessListener {
                finish()
            }
    }
}
