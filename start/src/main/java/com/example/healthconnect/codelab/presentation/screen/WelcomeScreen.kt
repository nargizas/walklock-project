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
package com.example.healthconnect.codelab.presentation.screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.healthconnect.codelab.R
import com.example.healthconnect.codelab.data.HealthConnectAvailability
import com.example.healthconnect.codelab.presentation.component.InstalledMessage
import com.example.healthconnect.codelab.presentation.component.NotInstalledMessage
import com.example.healthconnect.codelab.presentation.component.NotSupportedMessage
import com.example.healthconnect.codelab.presentation.screen.mode.Room
import com.example.healthconnect.codelab.presentation.theme.HealthConnectTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Welcome screen shown when the app is first launched.
 */
@Composable
fun WelcomeScreen(
    healthConnectAvailability: HealthConnectAvailability,
    onResumeAvailabilityCheck: () -> Unit,
    onStartClick: (Boolean) -> Unit = {},
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    navController: NavController
) {
  val currentOnAvailabilityCheck by rememberUpdatedState(onResumeAvailabilityCheck)
    var isLoggedIn by remember { mutableStateOf(false) }
  // Add a listener to re-check whether Health Connect has been installed each time the Welcome
  // screen is resumed: This ensures that if the user has been redirected to the Play store and
  // followed the onboarding flow, then when the app is resumed, instead of showing the message
  // to ask the user to install Health Connect, the app recognises that Health Connect is now
  // available and shows the appropriate welcome.
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        currentOnAvailabilityCheck()
      }
    }

    // Add the observer to the lifecycle
    lifecycleOwner.lifecycle.addObserver(observer)

    // When the effect leaves the Composition, remove the observer
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }
    var navigateTo by remember {
        mutableStateOf(0)
    }
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser != null) {
        isLoggedIn = true
    } else {
        isLoggedIn = false
    }


    if (isLoggedIn) {
        val usersCollection = Firebase.firestore.collection("users")
        val userId = currentUser!!.uid

        // Assuming you have initialized Firestore and have a reference to your collection
        suspend fun fetchUserData(usersCollection: CollectionReference, userId: String): DocumentSnapshot? =
            withContext(Dispatchers.IO) {
                try {
                    val docRef = usersCollection.document(userId)
                    val document = docRef.get().await()
                    document
                } catch (e: Exception) {
                    null
                }
            }

        val documentSnapshot = remember { mutableStateOf<DocumentSnapshot?>(null) }
        LaunchedEffect(Unit) {
            val fetchedDocument = fetchUserData(usersCollection, userId)
            documentSnapshot.value = fetchedDocument

            // Access the document data or handle null case
            fetchedDocument?.let { document ->
                val hasStarted = document.data?.get("hasStarted")
                Log.d("HEALTHCONNECT", "DocumentSnapshot data: $hasStarted")
                if (hasStarted == true){
                    navigateTo = 1
                } else {
                    navigateTo = 0
                }

            } ?: Log.d("HEALTHCONNECT", "No such document")
        }
    } else {
        navigateTo = 0
    }

  Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Image(
      modifier = Modifier.fillMaxWidth(0.5f),
      painter = painterResource(id = R.drawable.logo),
      contentDescription = stringResource(id = R.string.health_connect_logo)
    )
    Spacer(modifier = Modifier.height(32.dp))
    Text(
      text = stringResource(id = R.string.welcome_message),
      color = MaterialTheme.colors.onBackground
    )
    Spacer(modifier = Modifier.height(32.dp))
          when (healthConnectAvailability) {
              HealthConnectAvailability.INSTALLED -> InstalledMessage(navController, isLoggedIn, navigateTo)
              HealthConnectAvailability.NOT_INSTALLED -> NotInstalledMessage()
              HealthConnectAvailability.NOT_SUPPORTED -> NotSupportedMessage()
          }
  }
}

@Preview
@Composable
fun InstalledMessagePreview() {
    val navController = rememberNavController()
  HealthConnectTheme {
    WelcomeScreen(
        navController = navController,
      healthConnectAvailability = HealthConnectAvailability.INSTALLED,
      onResumeAvailabilityCheck = {},
    )
  }
}

@Preview
@Composable
fun NotInstalledMessagePreview() {
    val navController = rememberNavController()
  HealthConnectTheme {
    WelcomeScreen(
        navController = navController,
      healthConnectAvailability = HealthConnectAvailability.NOT_INSTALLED,
      onResumeAvailabilityCheck = {}
    )
  }
}

@Preview
@Composable
fun NotSupportedMessagePreview() {
    val navController = rememberNavController()
  HealthConnectTheme {
    WelcomeScreen(
        navController = navController,
      healthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,
      onResumeAvailabilityCheck = {}
    )
  }
}
