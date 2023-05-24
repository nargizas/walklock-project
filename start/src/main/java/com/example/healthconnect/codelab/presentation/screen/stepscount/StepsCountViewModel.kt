package com.example.healthconnect.codelab.presentation.screen.stepscount

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.RemoteException
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.units.Mass
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthconnect.codelab.data.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class StepsCountViewModel(private val healthConnectManager: HealthConnectManager, private val dataStore: DataStore<Preferences>, private val bluetoothSocket: BluetoothSocket) :
    ViewModel() {

    val LAST_START_TIME_KEY = stringPreferencesKey("last_start_time")
    val HAS_STARTED = booleanPreferencesKey("has_started")
    val STEPS_BEFORE_START = longPreferencesKey("steps_before_start")


    fun sendCommand(command: String) {
        try {
            bluetoothSocket.outputStream.write(command.toByteArray())
            Log.d("TAG", "sent command $command")
        } catch (e: Exception) {
            // Handle write error
            Log.d("TAG", "couldn't send command")

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

    var recordsList: MutableState<List<StepsRecord>> = mutableStateOf(listOf())
        private set

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

    val lastStartTimeString: Flow<String?> = dataStore.data
        .map { preferences ->
            // On the first run of the app, we will use LinearLayoutManager by default
            preferences[LAST_START_TIME_KEY]
        }

    fun ZonedDateTime.toIsoString(): String {
        return format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }

    fun initialLoad() {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                getStatusFun()
                getLastStartTime()
                getStepsBeforeStart()
            }
        }
    }



    fun getTotalStepsFromDayBefore(startTime: ZonedDateTime) {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                getStepsFromDayBefore(startTime)
            }
        }
    }

    fun readSteps(startTime: ZonedDateTime) {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                readStepsRecords(startTime)
            }
        }
    }

    private suspend fun readStepsRecords(startTime: ZonedDateTime) {
        val startOfTheDay = startTime.truncatedTo(ChronoUnit.DAYS).toInstant()
        val now = Instant.now()
        recordsList.value = healthConnectManager.readStepsCounts(startOfTheDay, now)
    }

    private suspend fun getSteps(startTime: ZonedDateTime) {
        val now = Instant.now()
        totalSteps.value = healthConnectManager.getTotalSteps(startTime.toInstant(), now)
        Log.d("TAG", totalSteps.value.toString())
    }

    private suspend fun getStepsFromDayBefore(startTime: ZonedDateTime) {
//        val startOfTheDay = startTime.minusDays(1)
        val startOfTheDay = startTime.truncatedTo(ChronoUnit.DAYS)
        val now = Instant.now()
        totalStepsFromDayBefore.value = healthConnectManager.getTotalSteps(startOfTheDay.toInstant(), now)
        Log.d("TAG", "got the value" + totalStepsFromDayBefore.value.toString())
    }

    private suspend fun getLastStartTime(){
        val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
        val stringDate = dataStore.data
            .catch {
                if (it is IOException) {
                    it.printStackTrace()
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                // On the first run of the app, we will use LinearLayoutManager by default
                preferences[LAST_START_TIME_KEY]
            }
        Log.d("TAG", stringDate.first().toString())
        stringDate.first()?.let{lastStartTime.value = ZonedDateTime.parse(it, formatter)}
    }

    fun getLastStartTimeFun(){
        viewModelScope.launch {
            getLastStartTime()
        }
    }
    fun setLastStartTime(lastStartTime: ZonedDateTime){
        viewModelScope.launch {
            val isoString = lastStartTime.toIsoString()
            Log.d("TAG", "update$isoString")
            dataStore.edit { preferences ->
                preferences[LAST_START_TIME_KEY] = isoString
            }
            getLastStartTime()
        }
    }

    private suspend fun getStatus(){
        val statusFlow = dataStore.data
            .catch {
                if (it is IOException) {
                    it.printStackTrace()
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                // On the first run of the app, we will use LinearLayoutManager by default
                preferences[HAS_STARTED]
            }
        Log.d("TAG", "the status is " + statusFlow.first().toString())
        statusFlow.first()?.let{hasStarted.value = it}
    }

    fun getStatusFun(){
        viewModelScope.launch {
            getStatus()
        }
    }

    fun setStatus(hasStarted: Boolean){
        viewModelScope.launch {
            Log.d("TAG", "update $hasStarted")
            dataStore.edit { preferences ->
                preferences[HAS_STARTED] = hasStarted
            }
            getStatus()
        }
    }

    private suspend fun getStepsBeforeStart(){
        val steps = dataStore.data
            .catch {
                if (it is IOException) {
                    it.printStackTrace()
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                // On the first run of the app, we will use LinearLayoutManager by default
                preferences[STEPS_BEFORE_START]
            }
        Log.d("TAG", "the status is " + steps.first().toString())
        steps.first()?.let{stepsBeforeStart.value = it}
    }

    fun getStepsBeforeStartFun(){
        viewModelScope.launch {
            getStepsBeforeStart()
        }
    }

    fun setStepsBeforeStart(steps: Long){
        viewModelScope.launch {
            Log.d("TAG", "update $steps")
            dataStore.edit { preferences ->
                preferences[STEPS_BEFORE_START] = steps
            }
            getStepsBeforeStart()
        }
    }

    fun storeStepsBeforeStart(startTime: ZonedDateTime){
        viewModelScope.launch {
            getStepsFromDayBefore(startTime)
            val steps: Long = if (totalStepsFromDayBefore.value != null) totalStepsFromDayBefore.value!! else 0
            setStepsBeforeStart(steps)
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

class StepsCountViewModelFactory(
    private val healthConnectManager: HealthConnectManager, private val dataStore: DataStore<Preferences>, private val bluetoothSocket: BluetoothSocket
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StepsCountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StepsCountViewModel(
                healthConnectManager = healthConnectManager,
                dataStore = dataStore,
                bluetoothSocket = bluetoothSocket
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
