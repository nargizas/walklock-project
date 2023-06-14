package com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.grpc.Deadline
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Composable
fun SetUpRoom(
    navController: NavController,
    roomId: String
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var goalStepCount by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Set your goal",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val context = LocalContext.current
        val time = remember { mutableStateOf("") }
        val date = remember { mutableStateOf("") }
        val timePickerDialog = TimePickerDialog(
            context,
            {_, hour : Int, minute: Int ->
                time.value = "$hour:$minute"
                selectedTime = LocalTime.of(hour, minute)
            }, ZonedDateTime.now().hour, ZonedDateTime.now().minute, true
        )

        val datePickerDialog = DatePickerDialog(
            context,
            {_, year : Int, month : Int, day: Int ->
                date.value = "$year/$month/$day"
                selectedDate = LocalDate.of(year, month + 1, day)
            }, ZonedDateTime.now().year, ZonedDateTime.now().monthValue, ZonedDateTime.now().dayOfMonth
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            Text(
                text = "Set deadline",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row() {

                Button(modifier = Modifier.padding(8.dp),
                    onClick = {
                        datePickerDialog.show()
                    }) {
                    Text(text = "Choose Date")
                }
                Button(modifier = Modifier.padding(8.dp),
                    onClick = {
                        timePickerDialog.show()
                    }) {
                    Text(text = "Choose Time")
                }
            }

            // Selected date and time
            Text(
                text = "Selected Date: ${date.value}",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Selected Time: ${time.value}",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Set goal step count",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Goal step count text field
            OutlinedTextField(
                value = goalStepCount,
                onValueChange = { goalStepCount = it },
                label = { Text("Goal Step Count") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Display step goal
            Text(
                text = "Step Goal: $goalStepCount",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Start button
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val deadline = LocalDateTime.of(selectedDate, selectedTime)
                    val deadlineTimestamp = deadline.atZone(ZoneOffset.UTC)
                    Log.d("HEALTHCONNECT", "$deadlineTimestamp")
                    val startTime = ZonedDateTime.now()
                    val goalSteps = goalStepCount.toIntOrNull()

                    if (deadline != null && goalSteps != null) {
                        val roomRef =
                            FirebaseFirestore.getInstance().collection("rooms").document(roomId)
                        roomRef.update(
                            mapOf(
                                "deadline" to Timestamp(deadlineTimestamp.toEpochSecond(), 0),
                                "startTime" to Timestamp(
                                    startTime.toEpochSecond(),
                                    0
                                ),
                                "goalSteps" to goalSteps
                            )
                        ).addOnSuccessListener {

                        }.addOnFailureListener { exception ->
                            Log.e(
                                "SetDeadlineScreen",
                                "Failed to update room: ${exception.message}"
                            )
                            // Handle room update failure
                        }


                        val usersCollection = FirebaseFirestore.getInstance().collection("users")
                        val userId = FirebaseAuth.getInstance().currentUser!!.uid
// Update user document when room starts
                        usersCollection.document(userId).update(
                            mapOf(
                                "hasStarted" to true,
                                "mode" to "group",
                                "roomId" to roomId,
                                "deadline" to Timestamp(deadlineTimestamp.toEpochSecond(), 0),
                                "startTime" to Timestamp(
                                    startTime.toEpochSecond(),
                                    0
                                ),
                                "stepGoal" to goalStepCount
                            )
                        ).addOnSuccessListener {
                            // User document updated successfully
                        }.addOnFailureListener { exception ->
                            Log.e(
                                "Firestore",
                                "Failed to update user document: ${exception.message}"
                            )
                            // Handle user document update failure
                        }
                    }
                }
            ) {
                Text("START")
            }
        }
    }
}
