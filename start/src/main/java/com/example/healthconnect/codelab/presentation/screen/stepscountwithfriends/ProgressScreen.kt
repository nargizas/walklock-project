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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.healthconnect.codelab.presentation.navigation.Screen
import com.example.healthconnect.codelab.presentation.screen.stepscount.StepsCountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.Timer
import java.util.TimerTask

fun calculateRemainingTime(currentTime: ZonedDateTime, deadline: ZonedDateTime): Duration {
    return Duration.between(currentTime, deadline)
}

fun getCurrentStepCount(total: Long, beforeStart: Long): Long {
    // Replace with your implementation to fetch the current step count
    return total - beforeStart
}

@Composable
fun ProgressScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    stepsBeforeStart: Long,
    startTime: ZonedDateTime,
    deadline: ZonedDateTime,
    stepGoal: Long,
    totalStepsFromDayBefore: Long,
    mode: String,
    onCheckClick: (ZonedDateTime) -> Unit = {},
    onStopClick: (ZonedDateTime) -> Unit = {},
    onHomeClick: () -> Unit = {},
    uiState: ProgressScreenViewModel.UiState,
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {},
    navController: NavController
) {

    var currentTime = rememberSaveable { mutableStateOf(ZonedDateTime.now()) }
    var remainingTime = rememberSaveable { mutableStateOf<Duration>(calculateRemainingTime(currentTime.value, deadline)) }
    var startTimeUpdate = rememberSaveable{ mutableStateOf(startTime) }
    var deadlineUpdate = rememberSaveable{ mutableStateOf(deadline) }
    var modeUpdate = rememberSaveable{ mutableStateOf(mode) }
    var stepsUntilNow = rememberSaveable{ mutableStateOf(stepsBeforeStart) }
    val currentStepCount by remember { mutableStateOf(getCurrentStepCount(totalStepsFromDayBefore, stepsBeforeStart)) }

    val totalSteps = rememberSaveable{ mutableStateOf(0L) }

    // Remember the last error ID, such that it is possible to avoid re-launching the error
    // notification for the same error when the screen is recomposed, or configuration changes etc.
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    LaunchedEffect(uiState) {
        // If the initial data load has not taken place, attempt to load the data.
        if (uiState is ProgressScreenViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }

        // The [ExerciseSessionViewModel.UiState] provides details of whether the last action was a
        // success or resulted in an error. Where an error occurred, for example in reading and
        // writing to Health Connect, the user is notified, and where the error is one that can be
        // recovered from, an attempt to do so is made.
        if (uiState is ProgressScreenViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }


    LaunchedEffect(stepsBeforeStart) {
        stepsUntilNow.value = stepsBeforeStart
    }


    LaunchedEffect(startTime) {
        startTimeUpdate.value = startTime
    }

    LaunchedEffect(deadline) {
        deadlineUpdate.value = deadline
    }

    LaunchedEffect(mode) {
        modeUpdate.value = mode
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun formatRemainingTime(remainingTime: Duration): String {
        if (remainingTime.seconds > 0){
            val days = remainingTime.seconds / (3600 * 24)
            val hours = remainingTime.seconds / 3600 % 24
            val minutes = remainingTime.seconds % 3600 / 60
            val seconds = remainingTime.seconds % 60

            return String.format("%02d days, %02d hours, %02d min, %02d s", days, hours, minutes, seconds)
        }
        return "_ days, _ hours, _ min, _ s"

    }

    LaunchedEffect(remainingTime.value) {
        delay(1_000L)
        remainingTime.value = calculateRemainingTime(ZonedDateTime.now(), deadline)
        currentTime.value = ZonedDateTime.now()
    }

    // Invoke the getStepsFunction() every 5 seconds until the deadline is reached
    LaunchedEffect(currentTime) {
        // Start a coroutine to collect the steps every 5 seconds
        val stepsFlow = flow {
            while (ZonedDateTime.now() <= deadline) {
                // Emit a Unit value
                emit(Unit)
                // Delay for 5 seconds
                delay(5000L)
            }
        }

        // Collect the steps from the flow
        launch {
            stepsFlow.collect {
                // Invoke the getStepsFunction() from the ViewModel
                onCheckClick(startTimeUpdate.value)
            }
        }
    }


    if (uiState != ProgressScreenViewModel.UiState.Uninitialized) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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


                if (remainingTime.value.seconds > 0) {
                    item {

                            Text(text = "Time left:",
                                style = MaterialTheme.typography.h5)
                            Text(text = "${formatRemainingTime(remainingTime.value)}")

                    }
                } else if  (deadlineUpdate.value <= ZonedDateTime.now()){
                    item {
                        Text(text = "Time is up!",
                            style = MaterialTheme.typography.h5,
                            color = Color.Blue)
                    }
                }

                totalSteps.value = totalStepsFromDayBefore - stepsBeforeStart

                item { Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(4.dp)) }

                item{
                    Row(){
                        Text(text = "Steps goal: ${stepGoal}",
                            style = MaterialTheme.typography.h5
                        )
                    }
                    Row(){
                        Text(text = "Steps taken: ${totalSteps.value}",
                            style = MaterialTheme.typography.h5
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = {
                            onCheckClick(startTimeUpdate.value)
                        },
                        enabled = true
                    ) {
                        Text(text = "Check step count")
                    }
                }

                if (modeUpdate.value == "group"){
                    item {
                        if (deadlineUpdate.value <= ZonedDateTime.now()){
                            if (totalSteps.value >= stepGoal){
                                Column(modifier = Modifier, verticalArrangement = Arrangement.Bottom) {


                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                    )

                                    Button(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        onClick = {
                                            onStopClick(startTimeUpdate.value)
                                        },
                                        enabled = true
                                    ) {
                                        Text(text = "Open the box")
                                    }

                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                    )

                                    if (modeUpdate.value == "group") {

                                        Button(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            onClick = {
                                                navController.navigate(Screen.Leaderboard.route)
                                            },
                                            enabled = true
                                        ) {
                                            Text(text = "See leaderboard")
                                        }

                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                        )
                                    }


                                    Button(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        onClick = {
                                            onHomeClick()
                                        },
                                        enabled = true
                                    ) {
                                        Text(text = "Go to Main Page")
                                    }
                                }

                            } else {
                                Text(text = "Oh no :( Let's try more")
                            }

                        } else {
                            if (totalSteps.value >= stepGoal){
                                Column(modifier = Modifier, verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Spacer(modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp))
                                    Text(text = "You did it!",
                                        style = MaterialTheme.typography.h5)
                                    Text(text = "You still have time, let's walk more")
                                }

                            }
                        }

                    }
                } else if (modeUpdate.value == "solo"){
                    item {
                        if (deadlineUpdate.value <= ZonedDateTime.now()){
                            if (totalSteps.value >= stepGoal){

                                Spacer(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp))

                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    onClick = {
                                        onStopClick(startTimeUpdate.value)
                                    },
                                    enabled = true
                                ) {
                                    Text(text = "Open the box")
                                }


                                Spacer(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp))

                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    onClick = {
                                        onHomeClick()
                                    },
                                    enabled = true
                                ) {
                                    Text(text = "Go to Main Page")
                                }

                            } else {
                                Spacer(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp))

                                Text(text = "Oh no :( Let's try more")
                            }

                        } else {
                            if (totalSteps.value >= stepGoal){


                                Spacer(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp))

                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    onClick = {
                                        onStopClick(startTimeUpdate.value)
                                    },
                                    enabled = true
                                ) {
                                    Text(text = "Open the box")
                                }
                                Spacer(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp))

                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    onClick = {
                                        onHomeClick()
                                    },
                                    enabled = true
                                ) {
                                    Text(text = "Go to Main Page")
                                }
                            }
                        }
                    }
                }


            }

        }
    }
}

