package com.example.healthconnect.codelab.presentation.screen.mode

import com.google.firebase.Timestamp
import com.google.firebase.ktx.Firebase

data class UserInfo(
    val userId: String = "",
    val email: String = "",
    val hasStarted: Boolean = false,
    val mode: String? = "",
    val roomId: String? = "",
    val startTime: Timestamp? = null,
    val deadline: Timestamp? = null,
    val stepGoal: Int? = 0,
    val stepsBeforeStart: Int? = 0,
    val totalSteps: Int? = 0
)