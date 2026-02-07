package com.testershub.app.models

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profilePhoto: String = "",
    val helpedCount: Int = 0,
    val requestedCount: Int = 0
)

data class TestingRequest(
    val requestId: String = "",
    val appName: String = "",
    val packageName: String = "",
    val testingLink: String = "",
    val testersRequired: Int = 0,
    val joinedCount: Int = 0,
    val deadline: Timestamp? = null,
    val instructions: String = "",
    val createdBy: String = "",
    val status: String = "IN_PROGRESS",
    val createdAt: Timestamp? = null
)

data class Supporter(
    val userId: String = "",
    val joinedAt: Timestamp? = null,
    val verified: Boolean = false
)

data class Notification(
    val message: String = "",
    val requestId: String = "",
    val timestamp: Timestamp? = null,
    val read: Boolean = false
)
