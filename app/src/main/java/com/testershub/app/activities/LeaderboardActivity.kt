package com.testershub.app.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.testershub.app.R
import com.testershub.app.databinding.ActivityLeaderboardBinding
import com.testershub.app.models.User

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLeaderboardBinding
    private val db = FirebaseFirestore.getInstance()
    private val leaderboardList = mutableListOf<User>()
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadLeaderboard()
    }

    private fun setupRecyclerView() {
        adapter = LeaderboardAdapter(leaderboardList)
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        binding.rvLeaderboard.adapter = adapter
    }

    private fun loadLeaderboard() {
        db.collection("users")
            .orderBy("helpedCount", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                leaderboardList.clear()
                for (doc in snapshot.documents) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) leaderboardList.add(user)
                }
                adapter.notifyDataSetChanged()
            }
    }

    inner class LeaderboardAdapter(private val users: List<User>) :
        RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvRank: TextView = view.findViewById(R.id.tvRank)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvPoints: TextView = view.findViewById(R.id.tvPoints)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leaderboard, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.tvRank.text = "${position + 1}"
            holder.tvName.text = user.name
            holder.tvPoints.text = "${user.helpedCount} Apps Helped"
        }

        override fun getItemCount() = users.size
    }
}