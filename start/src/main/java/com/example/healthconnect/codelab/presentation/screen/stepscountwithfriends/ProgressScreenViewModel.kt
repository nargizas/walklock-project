package com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket

import android.os.RemoteException
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringArrayResource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthconnect.codelab.data.HealthConnectManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ProgressScreenViewModel(private val healthConnectManager: HealthConnectManager, private val dataStore: DataStore<Preferences>, private val bluetoothSocket: BluetoothSocket) :
    ViewModel() {

    fun sendCommand(command: String) {
        try {
            bluetoothSocket.outputStream.write(command.toByteArray())
            Log.d("HEALTHCONNECT", "sent command $command")
        } catch (e: Exception) {
            // Handle write error
            Log.d("HEALTHCONNECT", "couldn't send command")
            return
        }
    }

    val permissions = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    var startTime = mutableStateOf<ZonedDateTime?>(ZonedDateTime.now())
        private set

    var deadline = mutableStateOf<ZonedDateTime?>(ZonedDateTime.now())
        private set

    var stepsBeforeStart: MutableState<Long?> = mutableStateOf(0L)
        private set

    var stepGoal: MutableState<Long?> = mutableStateOf(0L)
        private set

    var permissionsGranted = mutableStateOf(false)
        private set

    var totalSteps: MutableState<Long?> = mutableStateOf(0L)
        private set

    var mode: MutableState<String> = mutableStateOf("")
        private set

    var roomId: MutableState<String> = mutableStateOf("")
        private set

    var totalStepsFromDayBefore: MutableState<Long?> = mutableStateOf(0L)
        private set

    var recordsList: MutableState<List<StepsRecord>> = mutableStateOf(listOf())
        private set

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()



    fun ZonedDateTime.toIsoString(): String {
        return format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }

    fun initialLoad() {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                getUserData()
            }
        }
    }

    fun getUserData(){
        viewModelScope.launch {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val usersCollection = FirebaseFirestore.getInstance().collection("users")
            var userId = currentUser!!.uid;

// Perform the query to retrieve the document
            usersCollection.document(userId).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val userData = documentSnapshot.data
//                    Log.d("HEALTHCONNECT", "$userData")
                        val steps = userData?.get("stepsBeforeStart") as? Long
                        stepsBeforeStart.value = steps
                        Log.d("HEALTHCONNECT", "steps before start: $steps")


                        val startTimeTimestamp = userData?.get("startTime") as? Timestamp
                        startTime.value =
                            startTimeTimestamp!!.toDate().toInstant().
                            atZone(ZoneOffset.UTC);
                        Log.d("HEALTHCONNECT", "start time ${startTime.value.toString()}")

                        val deadlineTimestamp = userData?.get("deadline") as? Timestamp
                        deadline.value =
                            deadlineTimestamp!!.toDate().toInstant().
                            atZone(ZoneOffset.UTC);
                        Log.d("HEALTHCONNECT", "deadline ${deadline.value.toString()}")

                        val stepCountGoal = userData?.get("stepGoal") as? Long
                        stepGoal.value = stepCountGoal
                        Log.d("HEALTHCONNECT", "stepGoal ${stepGoal.value.toString()}")

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
            tryWithPermissionsCheck {

                getStepsFromDayBefore2(startTime.value!!)
            }
        }
    }

    fun getTotalStepsFromDayBefore(startTime: ZonedDateTime) {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                Log.d("HEALTHCONNECT", "start time ${startTime.toString()}")
                getStepsFromDayBefore(startTime)

            }
        }
    }




fun updateSoloUser(totalSteps: Int){
    val usersCollection = FirebaseFirestore.getInstance().collection("users")
    val userId = FirebaseAuth.getInstance().currentUser!!.uid

    usersCollection.document(userId)
        .update("totalSteps", totalSteps)
        .addOnSuccessListener {
            // Array updated successfully
        }
        .addOnFailureListener { exception ->
            // Handle the update failure
            Log.d(
                "Firestore",
                "Failed to update user document: ${exception.message}"
            )
        }
    }
fun updateUser(totalSteps: Int) {
    val usersCollection = FirebaseFirestore.getInstance().collection("users")
    val roomsCollection = FirebaseFirestore.getInstance().collection("rooms")
    val userId = FirebaseAuth.getInstance().currentUser!!.uid

    usersCollection.document(userId)
        .update("totalSteps", totalSteps)
        .addOnSuccessListener {
            // Array updated successfully
        }
        .addOnFailureListener { exception ->
            // Handle the update failure
            Log.d(
                "Firestore",
                "Failed to update user document: ${exception.message}"
            )
        }

    roomsCollection.document(roomId.value)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val yourArray = document.get("users") as? List<HashMap<String, Any>>
                if (yourArray != null) {
                    // Find the entry with the given UID and update the totalSteps field
                    val updatedArray = yourArray.map { entry ->
                        if (entry["userId"] == userId) {
                            entry.toMutableMap().apply {
                                this["totalSteps"] = totalSteps
                            }
                        } else {
                            entry
                        }
                    }

                    // Update the array in Firestore
                    roomsCollection.document(roomId.value)
                        .update("users", updatedArray)
                        .addOnSuccessListener {
                            // Array updated successfully
                        }
                        .addOnFailureListener { exception ->
                            // Handle the update failure
                            Log.d(
                                "Firestore",
                                "Failed to update user document: ${exception.message}"
                            )
                        }
                }
            }
        }
        .addOnFailureListener { exception ->
            // Handle the retrieval failure
            Log.d(
                "Firestore",
                "Failed to update user document: ${exception.message}"
            )
        }
}

    private suspend fun getStepsFromDayBefore2(startTime: ZonedDateTime) {
//        val startOfTheDay = startTime.minusDays(1)
        Log.d("HEALTHCONNECT", "get steps: start time ${startTime.toString()}")
        val startOfTheDay = startTime.truncatedTo(ChronoUnit.DAYS)
        Log.d("HEALTHCONNECT", "get steps: start of the day ${startOfTheDay.toString()}")
        val now = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS).toInstant()
        Log.d("HEALTHCONNECT", "get steps: now ${now.toString()}")
        totalStepsFromDayBefore.value = healthConnectManager.getTotalSteps(startOfTheDay.toInstant(), now)
        Log.d("HEALTHCONNECT", "got the value" + totalStepsFromDayBefore.value.toString())
    }

    private suspend fun getStepsFromDayBefore(startTime: ZonedDateTime) {
//        val startOfTheDay = startTime.minusDays(1)
        Log.d("HEALTHCONNECT", "get steps: start time ${startTime.toString()}")
        val startOfTheDay = startTime.truncatedTo(ChronoUnit.DAYS).minus(9, ChronoUnit.HOURS)
        Log.d("HEALTHCONNECT", "get steps: start of the day ${startOfTheDay.toString()}")
        val now = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS).toInstant()
        Log.d("HEALTHCONNECT", "get steps: now ${now.toString()}")
        totalStepsFromDayBefore.value = healthConnectManager.getTotalSteps(startOfTheDay.toInstant(), now)
        Log.d("HEALTHCONNECT", "got the value" + totalStepsFromDayBefore.value.toString())

        if (mode.value == "group"){
            updateUser((totalStepsFromDayBefore.value!! - stepsBeforeStart.value!!).toInt())
        } else {
            updateSoloUser((totalStepsFromDayBefore.value!! - stepsBeforeStart.value!!).toInt())
        }

    }

    fun endChallenge(){
        val usersCollection = FirebaseFirestore.getInstance().collection("users")
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        usersCollection.document(userId)
            .update("hasStarted", false)
            .addOnSuccessListener {
                // Array updated successfully
            }
            .addOnFailureListener { exception ->
                // Handle the update failure
                Log.d(
                    "Firestore",
                    "Failed to update user document: ${exception.message}"
                )
            }
    }



    /**
     * Provides permission check and error handling for Health Connect suspend function calls.
     *
     * Permissions are checked prior to execution of [block], and if all permissions aren't granted
     * the [block] won't be executed, and [permissionsGranted] will be set to false, which will
     * result in the UI showing the permissions button.
     *
     * Where an error is caught, of the type Health Connect is known to throw, [uiState] is set to
     * [UiState.Error], which results in the snackbar being used to show the error message.
     */
    private suspend fun tryWithPermissionsCheck(block: suspend () -> Unit) {
        permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)
        uiState = try {
            if (permissionsGranted.value) {
                block()
            }
            UiState.Done
        } catch (remoteException: RemoteException) {
            UiState.Error(remoteException)
        } catch (securityException: SecurityException) {
            UiState.Error(securityException)
        } catch (ioException: IOException) {
            UiState.Error(ioException)
        } catch (illegalStateException: IllegalStateException) {
            UiState.Error(illegalStateException)
        }
    }

    sealed class UiState {
        object Uninitialized : UiState()
        object Done : UiState()

        // A random UUID is used in each Error object to allow errors to be uniquely identified,
        // and recomposition won't result in multiple snackbars.
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }
}

class ProgressScreenViewModelFactory(
    private val healthConnectManager: HealthConnectManager, private val dataStore: DataStore<Preferences>, private val bluetoothSocket: BluetoothSocket
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgressScreenViewModel(
                healthConnectManager = healthConnectManager,
                dataStore = dataStore,
                bluetoothSocket = bluetoothSocket
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}