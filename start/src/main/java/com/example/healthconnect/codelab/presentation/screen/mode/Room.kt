package com.example.healthconnect.codelab.presentation.screen.mode

data class Room(
    val roomId: String = "",
    val hostId: String = "",
    val hostNickname: String = "",
    val users: List<User> = emptyList()
)