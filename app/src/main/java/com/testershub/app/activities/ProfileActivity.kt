package com.testershub.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.testershub.app.R
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

        binding.btnBack.setOnClickListener { finish() }
        binding.optHelped.title.text = "Apps I Helped"
        binding.optHelped.icon.setImageResource(R.drawable.ic_home)

        binding.optLeaderboard.title.text = "Leaderboard"
        binding.optLeaderboard.icon.setImageResource(R.drawable.ic_leaderboard)
        binding.optLeaderboard.root.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }
        
        binding.optRequests.title.text = "My Testing Requests"
        binding.optRequests.icon.setImageResource(R.drawable.ic_list)

        binding.optMyProfile.title.text = "Account Settings"
        binding.optMyProfile.icon.setImageResource(R.drawable.ic_person)

        binding.optNotifications.title.text = "Notifications"
        binding.optNotifications.icon.setImageResource(R.drawable.ic_notifications)

        loadUserProfile()
        
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
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
        
        if (!user.profilePhoto.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.profilePhoto)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(binding.ivProfilePhoto)
        } else {
            binding.ivProfilePhoto.setImageResource(R.drawable.ic_person)
        }
    }
}
