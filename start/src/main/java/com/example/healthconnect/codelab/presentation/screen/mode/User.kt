package com.example.healthconnect.codelab.presentation.screen.mode

import com.google.firebase.Timestamp
import com.google.firebase.ktx.Firebase

data class User(
    val userId: String = "",
    val nickname: String = "",
    val totalSteps: Int? = 0
)