package com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import com.example.healthconnect.codelab.R
import com.example.healthconnect.codelab.presentation.theme.HealthConnectTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import android.util.Log


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

import android.app.TimePickerDialog
import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit


@Composable
fun StepsCountScreenWithFriends(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    uiState: StepsCountWithFriendsViewModel.UiState,
    stepsBeforeStart: Long,
    onStartClick: (ZonedDateTime) -> Unit = {},
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {},
    navController: NavController,
    roomId: String
) {

    val stepsUntilNow = rememberSaveable{ mutableStateOf(stepsBeforeStart) }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var goalStepCount by remember { mutableStateOf("") }

    val context = LocalContext.current
    val time = remember { mutableStateOf("--:--") }
    val date = remember { mutableStateOf("--/--/--") }

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
            selectedDate = LocalDate.of(year, month+1, day)
        }, ZonedDateTime.now().year, ZonedDateTime.now().monthValue, ZonedDateTime.now().dayOfMonth
    )

    // Remember the last error ID, such that it is possible to avoid re-launching the error
    // notification for the same error when the screen is recomposed, or configuration changes etc.
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    LaunchedEffect(uiState) {
        // If the initial data load has not taken place, attempt to load the data.
        if (uiState is StepsCountWithFriendsViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }

        // The [ExerciseSessionViewModel.UiState] provides details of whether the last action was a
        // success or resulted in an error. Where an error occurred, for example in reading and
        // writing to Health Connect, the user is notified, and where the error is one that can be
        // recovered from, an attempt to do so is made.
        if (uiState is StepsCountWithFriendsViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }


    LaunchedEffect(stepsBeforeStart) {
        stepsUntilNow.value = stepsBeforeStart
    }

    fun updateRoom(deadlineTimestamp: ZonedDateTime, goalSteps: Int, startTime: ZonedDateTime) {

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
            }
    }

    fun updateUser(deadlineTimestamp: ZonedDateTime, goalSteps: Int, startTime: ZonedDateTime) {
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
                "stepGoal" to goalSteps
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

    if (uiState != StepsCountWithFriendsViewModel.UiState.Uninitialized) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!permissionsGranted) {
                item {
                    Button(
                        onClick = {
                            onPermissionsLaunch(permissions)
                        }
                    ) {
                        Text(text = stringResource(R.string.permissions_button_label))
                    }
                }
            } else {
                item {
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
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            ),
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
                                val deadline = LocalDateTime.of(selectedDate, selectedTime).minus(9, ChronoUnit.HOURS)
                                val deadlineTimestamp = deadline.atZone(ZoneOffset.UTC)

                                Log.d("HEALTHCONNECT", "$deadlineTimestamp")
                                val startTime = ZonedDateTime.now()
                                val goalSteps = goalStepCount.toIntOrNull()

                                if (deadline != null && goalSteps != null) {
                                    updateRoom(deadlineTimestamp, goalSteps, startTime)
                                    updateUser(deadlineTimestamp, goalSteps, startTime)
                                }

                                onStartClick(startTime)
                            }
                        ) {
                            Text("START")
                        }

                    }
                }
            }

        }
    }
}

//@Preview
//@Composable
//fun StepsCountScreenPreview() {
//    HealthConnectTheme {
//        val runningStartTime = ZonedDateTime.now()
//        val runningEndTime = runningStartTime.plusMinutes(30)
//        val walkingStartTime = ZonedDateTime.now().minusMinutes(120)
//        val walkingEndTime = walkingStartTime.plusMinutes(30)
//        StepsCountScreenWithFriends(
//            permissions = setOf(),
//            permissionsGranted = true,
//            lastStartTime = ZonedDateTime.now(),
//            stepsBeforeStart = 1000,
//            hasStarted = false,
//            uiState = StepsCountWithFriendsViewModel.UiState.Done
//        )
//    }
//}

