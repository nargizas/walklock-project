package com.example.healthconnect.codelab.presentation.screen.mode


import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthconnect.codelab.presentation.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RoomScreen(
    navController: NavController,
    roomId: String
) {
    Log.d("HEALTHCONNECT", "RoomScreen opened")
    val roomRef = FirebaseFirestore.getInstance().collection("rooms").document(roomId)
    val roomState = remember { mutableStateOf<Room?>(null) }
    DisposableEffect(roomRef) {
        val listenerRegistration = roomRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e("RoomScreen", "Error retrieving room: ${exception.message}")
                // Handle error
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val room = snapshot.toObject(Room::class.java)
                roomState.value = room
            }
        }

        onDispose {
            listenerRegistration.remove()
        }
    }


    val room = roomState.value
    Log.d("HEALTHCONNECT", "got the room")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (room != null) {
            Text(
                text = "Room ID: ${room.roomId}",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Users:",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                room.users.forEach { user ->
                    Card(
                        backgroundColor = Color(0xFFE6EEFA),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "User Icon",
                                tint = Color.Blue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = user.nickname,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(start = 8.dp)
                            )

                            if (user.userId == room.hostId) {
                                Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "host",
                                        color = Color.Gray,
                                        fontStyle = FontStyle.Italic,
                                    )

                            }
                        }
                    }
                }
            }

            if (room.hostId == FirebaseAuth.getInstance().currentUser?.uid) {
                Button(
                    onClick = { navController.navigate("${Screen.StepsCounterWithFriends.route}/$roomId") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(text = "Set the Goal")
                }
            }
        }
    }
}