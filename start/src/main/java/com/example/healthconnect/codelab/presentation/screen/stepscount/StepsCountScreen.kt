package com.example.healthconnect.codelab.presentation.screen.stepscount


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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp


@Composable
fun StepsCountScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    lastStartTime: ZonedDateTime,
    totalStepsFromDayBefore: Long,
    hasStarted: Boolean,
    stepsBeforeStart: Long,
    recordsList: List<StepsRecord>,
    uiState: StepsCountViewModel.UiState,
    onStartClick: (ZonedDateTime) -> Unit = {},
    onCheckClick: (ZonedDateTime) -> Unit = {},
    onStopClick: (Boolean) -> Unit = {},
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {},
) {
    val isStarted = remember { mutableStateOf(hasStarted) }
    val startTime = rememberSaveable { mutableStateOf<ZonedDateTime?>(lastStartTime) }
    val stepsUntilNow = rememberSaveable{ mutableStateOf(stepsBeforeStart) }
    val totalSteps = rememberSaveable{ mutableStateOf(0L) }
    val isChecked = rememberSaveable { mutableStateOf(false) }
    // Remember the last error ID, such that it is possible to avoid re-launching the error
    // notification for the same error when the screen is recomposed, or configuration changes etc.
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    LaunchedEffect(uiState) {
        // If the initial data load has not taken place, attempt to load the data.
        if (uiState is StepsCountViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }

        // The [ExerciseSessionViewModel.UiState] provides details of whether the last action was a
        // success or resulted in an error. Where an error occurred, for example in reading and
        // writing to Health Connect, the user is notified, and where the error is one that can be
        // recovered from, an attempt to do so is made.
        if (uiState is StepsCountViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }

    val currentHasStarted = remember { mutableStateOf(hasStarted) }

    LaunchedEffect(hasStarted) {
        currentHasStarted.value = hasStarted
//        isStarted.value = hasStarted
    }

    LaunchedEffect(stepsBeforeStart) {
        stepsUntilNow.value = stepsBeforeStart
//        isStarted.value = hasStarted
    }

    if (uiState != StepsCountViewModel.UiState.Uninitialized) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
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
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(4.dp),
                        onClick = {
                            startTime.value = ZonedDateTime.now()
                            Log.d("TAG", "new start time" + startTime.value.toString())
                            onStartClick(startTime.value!!)
                            Log.d("TAG", "new last start time$lastStartTime")
                            isStarted.value = true // set the isStarted variable to true
                        },
                        enabled = !currentHasStarted.value // disable the button if isStarted is true
                    ) {
                        Text(stringResource(id = R.string.start_button_label))
                    }
                }
//                Log.d("TAG", isStarted.value.toString())
                if (currentHasStarted.value) {
                    item {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(4.dp),
                            onClick = {
                                onCheckClick(startTime.value!!)
                                isChecked.value = true
                            },
                            enabled = true
                        ) {
                            Text(stringResource(id = R.string.check_step_counts_button_label))
                        }
                    }
                }

                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(4.dp),
                        onClick = {
                            isStarted.value = false // set the isStarted variable to true
                            onStopClick(false)
                        },
                    ) {
                        Text(stringResource(id = R.string.stop_button_label))
                    }
                }
                Log.d("TAG", isStarted.value.toString())
                if(currentHasStarted.value){
                    item { Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(4.dp)) }

                    item{
                        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy - HH:mm:ss");
                        Text(text = "The start time is")
                        Text(text = formatter.format(lastStartTime))
                    }
                }
                if (isChecked.value && currentHasStarted.value) {
                    totalSteps.value = totalStepsFromDayBefore - stepsBeforeStart

                    item { Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(4.dp)) }

                    item{

                        Text(text = "You have walked",
                            style = TextStyle(
                                fontSize = 24.sp,))
                        Text(text = "${totalSteps.value}",
                            style = TextStyle(
                                color = Color.Red,
                                fontSize = 24.sp,))
                        Text(text = "steps",
                            style = TextStyle(
                                fontSize = 24.sp,))
                    }

                    if (totalSteps.value > 10) {
                        item {
                            Text(text = "Success!!",
                                style = TextStyle(
                                    color = Color.Green,
                                    fontSize = 24.sp,))
                        }
                    }
                }

            }

        }
    }
}

@Preview
@Composable
fun StepsCountScreenPreview() {
    HealthConnectTheme {
        val runningStartTime = ZonedDateTime.now()
        val runningEndTime = runningStartTime.plusMinutes(30)
        val walkingStartTime = ZonedDateTime.now().minusMinutes(120)
        val walkingEndTime = walkingStartTime.plusMinutes(30)
        StepsCountScreen(
            permissions = setOf(),
            permissionsGranted = true,
            lastStartTime = ZonedDateTime.now(),
            totalStepsFromDayBefore = 10000,
            stepsBeforeStart = 1000,
            hasStarted = false,
            recordsList = listOf(StepsRecord(
                    count = 1000,
                    startTime = runningStartTime.toInstant(),
                    startZoneOffset = runningStartTime.offset,
                    endTime = runningEndTime.toInstant(),
                    endZoneOffset = runningEndTime.offset,
                    metadata = Metadata(UUID.randomUUID().toString())
                ),
            ),
            uiState = StepsCountViewModel.UiState.Done
        )
    }
}

@Composable
fun UpdateDataStore(
    saveStatus: (Boolean) -> Unit,
    currentStatus: Boolean) {

    val status = remember { mutableStateOf(currentStatus ?: false)}

    Button(onClick = {
        status.value = true
        saveStatus.invoke(status.value)
    },
        enabled = !status.value
        ) {
        Text(text="Start")
    }

    if (currentStatus) {
        Column(modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "IN PROGRESS")
            Button(onClick = {
                status.value = false
                saveStatus.invoke(status.value)
            }, enabled = status.value
            ) {
                Text(text="STOP")
            }
        }
    }

}
