package com.testershub.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.testershub.app.databinding.ActivityProfileBinding
import com.testershub.app.models.User
import com.bumptech.glide.Glide

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserProfile()
        
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            finish()
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    user?.let { updateUI(it) }
                }
            }
    }

    private fun updateUI(user: User) {
        binding.tvName.text = user.name
        binding.tvEmail.text = user.email
        binding.tvHelpedCount.text = "Helped: ${user.helpedCount}"
        binding.tvRequestedCount.text = "Requested: ${user.requestedCount}"
        
        Glide.with(this)
            .load(user.profilePhoto)
            .circleCrop()
            .into(binding.ivProfilePhoto)
    }
}
