/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.healthconnect.codelab.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID

/**
 * The entry point into the sample.
 */
class MainActivity : ComponentActivity() {
  val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "values")
  private val DEVICE_ADDRESS = "98:DA:60:08:0B:72" // Replace with your HC-06 Bluetooth module address

  private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID for Serial Port Profile (SPP)

  private lateinit var bluetoothSocket: BluetoothSocket

  val REQUEST_BLUETOOTH_PERMISSION = 1


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val healthConnectManager = (application as BaseApplication).healthConnectManager
    Log.d("HEALTHCONNECT", "maybe here")
    val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.getAdapter()
    if (bluetoothAdapter == null) {
      Toast.makeText(applicationContext, "this device doesn't support Bluetooth", Toast.LENGTH_LONG)
      return
    }
    val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS)
    Log.d("HEALTHCONNECT", "or here")
    try {
      if (ActivityCompat.checkSelfPermission(
          this,
          Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        ActivityCompat.requestPermissions(
          this,
          arrayOf(Manifest.permission.BLUETOOTH),
          REQUEST_BLUETOOTH_PERMISSION
        )
        Log.d("HEALTHCONNECT", "are you here")
      }
       device?.let {
         bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
         bluetoothSocket?.connect()
         Log.d("HEALTHCONNECT", "socket connected")
       }
    } catch (e: Exception) {
      // Handle connection error
      Log.d("HEALTHCONNECT", "some error")
      return
    }

    // ...

    setContent {
      bluetoothSocket?.let { socket ->
        HealthConnectApp(
          healthConnectManager = healthConnectManager,
          dataStore = dataStore,
          bluetoothSocket = socket
        )
      }
    }
  }
}
