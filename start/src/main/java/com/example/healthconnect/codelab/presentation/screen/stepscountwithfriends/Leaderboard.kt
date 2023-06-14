package com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends



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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthconnect.codelab.presentation.navigation.Screen
import com.example.healthconnect.codelab.presentation.screen.mode.Room
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.ZoneOffset

@Composable
fun Leaderboard(
    navController: NavController,
) {
    Log.d("HEALTHCONNECT", "Leaderboard opened")
    val currentUser = FirebaseAuth.getInstance().currentUser
    val usersCollection = FirebaseFirestore.getInstance().collection("users")
    var userId = currentUser!!.uid;

// Perform the query to retrieve the document
    var mode = rememberSaveable { mutableStateOf("")}
    var roomId = rememberSaveable { mutableStateOf("")}

    usersCollection.document(userId).get()
        .addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val userData = documentSnapshot.data

                val modeData = userData?.get("mode") as? String
                mode.value = modeData!!
                Log.d("HEALTHCONNECT", "stepGoal ${mode.value}")
                if (mode.value == "group"){
                    val roomData = userData!!.get("roomId") as? String
                    roomId.value = roomData!!
                    Log.d("HEALTHCONNECT", "stepGoal ${roomId.value}")
                }
            } else {
                println("User document does not exist")
            }
        }
        .addOnFailureListener { exception ->
            println("Error getting user document: $exception")
        }

    if ( roomId.value != ""){
        val roomRef = FirebaseFirestore.getInstance().collection("rooms").document(roomId.value)
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
                    if ( roomId.value != "") {


                        room.users.sortedBy { it.totalSteps }.forEach { user ->
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

                                    Spacer(Modifier.weight(1f))

                                    Text(
                                        text = "${user.totalSteps}",
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
                        onClick = { navController.navigate(Screen.WelcomeScreen.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(text = "Back")
                    }
                }
            }
        }


    }





}