package com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends

import android.bluetooth.BluetoothSocket

import android.os.RemoteException
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
import java.time.format.DateTimeFormatter

class StepsCountWithFriendsViewModel(private val healthConnectManager: HealthConnectManager, private val dataStore: DataStore<Preferences>, private val bluetoothSocket: BluetoothSocket) :
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

    var lastStartTime = mutableStateOf<ZonedDateTime?>(ZonedDateTime.now())
        private set

    var hasStarted = mutableStateOf<Boolean>(false)
        private set

    var stepsBeforeStart: MutableState<Long?> = mutableStateOf(0L)
        private set

    var permissionsGranted = mutableStateOf(false)
        private set

    var totalSteps: MutableState<Long?> = mutableStateOf(0L)
        private set

    var totalStepsFromDayBefore: MutableState<Long?> = mutableStateOf(0L)
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
//                getStatusFun()
//                getLastStartTime()
//                getStepsBeforeStart()
            }
        }
    }

    fun storeStepsBeforeStart(startTime: ZonedDateTime){
        viewModelScope.launch {
            getStepsFromDayBefore(startTime)
            val steps: Long = if (totalStepsFromDayBefore.value != null) totalStepsFromDayBefore.value!! else 0
            setStepsBeforeStart(steps)
        }
    }

    private suspend fun getStepsFromDayBefore(startTime: ZonedDateTime) {
        val startOfTheDay = startTime.truncatedTo(ChronoUnit.DAYS)
        val now = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS).toInstant()
        totalStepsFromDayBefore.value = healthConnectManager.getTotalSteps(startOfTheDay.toInstant(), now)
        Log.d("HEALTHCONNECT", "got the value" + totalStepsFromDayBefore.value.toString())
    }

    fun setStepsBeforeStart(steps: Long){
        viewModelScope.launch {
            Log.d("HEALTHCONNECT", "update $steps")
            val usersCollection = FirebaseFirestore.getInstance().collection("users")
            val userId = FirebaseAuth.getInstance().currentUser!!.uid
// Update user document when room starts
            usersCollection.document(userId).update(
                mapOf(
                    "stepsBeforeStart" to steps
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
            stepsBeforeStart.value = steps
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

class StepsCountWithFriendsViewModelFactory(
    private val healthConnectManager: HealthConnectManager, private val dataStore: DataStore<Preferences>, private val bluetoothSocket: BluetoothSocket
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StepsCountWithFriendsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StepsCountWithFriendsViewModel(
                healthConnectManager = healthConnectManager,
                dataStore = dataStore,
                bluetoothSocket = bluetoothSocket
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
