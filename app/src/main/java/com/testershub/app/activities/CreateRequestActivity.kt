package com.testershub.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.testershub.app.databinding.ActivityCreateRequestBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.Date
import android.app.DatePickerDialog
import java.util.Calendar

import com.google.firebase.firestore.FieldValue

class CreateRequestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateRequestBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedDeadline: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectDeadline.setOnClickListener {
            showDatePicker()
        }

        binding.btnSubmit.setOnClickListener {
            submitRequest()
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDeadline = calendar.time
            binding.btnSelectDeadline.text = "Deadline: $dayOfMonth/${month + 1}/$year"
            Unit
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun submitRequest() {
        val appName = binding.etAppName.text.toString()
        val packageName = binding.etPackageName.text.toString()
        val testingLink = binding.etTestingLink.text.toString()
        val instructions = binding.etInstructions.text.toString()
        val testersRequired = binding.etTestersRequired.text.toString().toIntOrNull() ?: 0

        if (appName.isEmpty()) {
            binding.etAppName.error = "App Name is required"
            return
        }
        if (packageName.isEmpty() || !packageName.contains(".")) {
            binding.etPackageName.error = "Valid Package Name is required"
            return
        }
        if (testersRequired <= 0 || testersRequired > 100) {
            binding.etTestersRequired.error = "Enter a number between 1 and 100"
            return
        }
        if (selectedDeadline == null) {
            Toast.makeText(this, "Please select a deadline", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val requestId = db.collection("testingRequests").document().id

        val request = hashMapOf(
            "requestId" to requestId,
            "appName" to appName,
            "packageName" to packageName,
            "testingLink" to testingLink,
            "instructions" to instructions,
            "testersRequired" to testersRequired,
            "joinedCount" to 0,
            "deadline" to Timestamp(selectedDeadline!!),
            "createdBy" to userId,
            "status" to "IN_PROGRESS",
            "createdAt" to Timestamp.now()
        )

        val batch = db.batch()
        batch.set(db.collection("testingRequests").document(requestId), request)
        batch.update(db.collection("users").document(userId), "requestedCount", FieldValue.increment(1))

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Request created successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to create request: ${e.message}", Toast.LENGTH_LONG).show()
            binding.btnSubmit.isEnabled = true
        }
    }
}
