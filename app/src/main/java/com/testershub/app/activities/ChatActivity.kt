package com.testershub.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.testershub.app.databinding.ActivityChatBinding
import com.testershub.app.models.ChatMessage
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FieldValue

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var chatId: String? = null

    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra("CHAT_ID")
        
        adapter = ChatAdapter(messageList)
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etMessage.setText("")
            }
        }
        
        loadMessages()
    }

    private fun sendMessage(text: String) {
        val message = hashMapOf(
            "senderId" to auth.currentUser?.uid,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )
        chatId?.let {
            db.collection("chats").document(it).collection("messages").add(message)
        }
    }

    private fun loadMessages() {
        chatId?.let {
            db.collection("chats").document(it).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        messageList.clear()
                        for (doc in snapshot.documents) {
                            val msg = doc.toObject(ChatMessage::class.java)
                            if (msg != null) messageList.add(msg)
                        }
                        adapter.notifyDataSetChanged()
                        binding.rvChat.scrollToPosition(messageList.size - 1)
                    }
                }
        }
    }
}