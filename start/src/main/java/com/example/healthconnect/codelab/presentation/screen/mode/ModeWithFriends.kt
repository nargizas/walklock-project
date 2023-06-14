package com.example.healthconnect.codelab.presentation.screen.mode

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Random

@Composable
fun ModeWithFriends(navController: NavController) {
    val firebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser
    val roomRef = FirebaseFirestore.getInstance().collection("rooms")

    var roomCode by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Choose Your nickname",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Nickname") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        Text(
            text = "Enter the room",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = {
                if (nickname.isNotEmpty()){
                    createRoom(roomRef, currentUser, nickname, navController)
                } else {
                    error = "Enter nickname"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Create a Room")
        }

        Text(
            text = "or",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        OutlinedTextField(
            value = roomCode,
            onValueChange = { roomCode = it },
            label = { Text("Room ID") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )



        Button(
            onClick = { joinRoom(roomRef, currentUser, roomCode, nickname, navController) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Join a Room")
        }

        if (error.isNotEmpty()) {
            Text(
                text = error,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

private fun createRoom(
    roomRef: CollectionReference,
    currentUser: FirebaseUser?,
    hostNickname: String,
    navController: NavController
) {
    if (currentUser != null && hostNickname.isNotEmpty()) {
        val roomId = generateRoomId()
        val user = User(userId = currentUser.uid!!, nickname = hostNickname)
        val room = Room(roomId = roomId, hostId = currentUser.uid!!, hostNickname = hostNickname, users = listOf(user))

        roomRef.document(roomId)
            .set(room)
            .addOnSuccessListener {
                navigateToRoom(navController, roomId)
            }
            .addOnFailureListener { exception ->
                Log.e("CreateRoom", "Failed to create room: ${exception.message}")
                // Handle room creation failure
            }
    }
}

private fun joinRoom(
    roomRef: CollectionReference,
    currentUser: FirebaseUser?,
    roomCode: String,
    nickname: String,
    navController: NavController,
) {
    if (currentUser != null && nickname.isNotEmpty() && roomCode.isNotEmpty()) {
        val user = User(userId = currentUser.uid!!, nickname = nickname)

        roomRef.document(roomCode)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val room = document.toObject(Room::class.java)
                    if (room != null) {
                        val updatedUsers = room.users.toMutableList()
                        updatedUsers.add(user)
                        roomRef.document(roomCode)
                            .update("users", updatedUsers)
                            .addOnSuccessListener {
                                navigateToRoom(navController, roomCode)
                            }
                            .addOnFailureListener { exception ->
                                Log.d("JoinRoom", "Failed to join room: ${exception.message}")
                                // Handle room join failure
                            }
                    } else {
                        // Room object is null
                        Log.d("JoinRoom", "It's null")
                    }
                } else {
                    // Room document does not exist
                    Log.d("JoinRoom", "doesn't exist")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("JoinRoom", "Failed to retrieve room: ${exception.message}")
                // Handle room retrieval failure
            }
    }
}

private fun generateRoomId(): String {
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val random = Random()
    val roomId = StringBuilder()
    repeat(6) {
        val index = random.nextInt(characters.length)
        roomId.append(characters[index])
    }
    return roomId.toString()
}

private fun navigateToRoom(navController: NavController, roomId: String) {
    Log.d("HEALTHCONNECT", "success with room")
    navController.navigate("room_screen/$roomId")
    Log.d("HEALTHCONNECT", "navigated")
}