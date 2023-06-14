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
package com.example.healthconnect.codelab.presentation.navigation

import android.bluetooth.BluetoothSocket
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.healthconnect.codelab.data.HealthConnectManager
import com.example.healthconnect.codelab.presentation.screen.WelcomeScreen
import com.example.healthconnect.codelab.presentation.screen.changes.DifferentialChangesScreen
import com.example.healthconnect.codelab.presentation.screen.changes.DifferentialChangesViewModel
import com.example.healthconnect.codelab.presentation.screen.changes.DifferentialChangesViewModelFactory
import com.example.healthconnect.codelab.presentation.screen.privacypolicy.PrivacyPolicyScreen
import com.example.healthconnect.codelab.presentation.screen.stepscount.StepsCountScreen
import com.example.healthconnect.codelab.presentation.screen.stepscount.StepsCountViewModel
import com.example.healthconnect.codelab.presentation.screen.stepscount.StepsCountViewModelFactory
import com.example.healthconnect.codelab.presentation.screen.login.LoginScreen
import com.example.healthconnect.codelab.presentation.screen.mode.ModeWithFriends
import com.example.healthconnect.codelab.presentation.screen.mode.ModeScreen
import com.example.healthconnect.codelab.presentation.screen.mode.RoomScreen
import com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends.Leaderboard
import com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends.ProgressScreen
import com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends.ProgressScreenViewModel
import com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends.ProgressScreenViewModelFactory
import com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends.SetUpRoom
import com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends.StepsCountScreenWithFriends
import com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends.StepsCountWithFriendsViewModel
import com.example.healthconnect.codelab.presentation.screen.stepscountwithfriends.StepsCountWithFriendsViewModelFactory
import com.example.healthconnect.codelab.showExceptionSnackbar

/**
 * Provides the navigation in the app.
 */
@Composable
fun HealthConnectNavigation(
  navController: NavHostController,
  healthConnectManager: HealthConnectManager,
  scaffoldState: ScaffoldState,
  dataStore: DataStore<Preferences>,
  bluetoothSocket: BluetoothSocket
) {
  val scope = rememberCoroutineScope()
  NavHost(navController = navController, startDestination = Screen.WelcomeScreen.route) {
    val availability by healthConnectManager.availability
    composable(Screen.WelcomeScreen.route) {
      WelcomeScreen(
        healthConnectAvailability = availability,
        onResumeAvailabilityCheck = {
          healthConnectManager.checkAvailability()
        },
        navController = navController,
      )
    }
    composable(
      route = Screen.PrivacyPolicy.route,
      deepLinks = listOf(
        navDeepLink {
          action = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
        }
      )
    ) {
      PrivacyPolicyScreen()
    }
    composable(Screen.StepsCounter.route) {
      val viewModel: StepsCountViewModel = viewModel(
        factory = StepsCountViewModelFactory(
          healthConnectManager = healthConnectManager,
          dataStore = dataStore,
          bluetoothSocket = bluetoothSocket,
        )
      )
      val permissionsGranted by viewModel.permissionsGranted
      val permissions = viewModel.permissions
      val onPermissionsResult = { viewModel.initialLoad() }
      val stepsBeforeStart by viewModel.stepsBeforeStart
      val permissionsLauncher =
        rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
          onPermissionsResult()
        }
      StepsCountScreen(
        permissionsGranted = permissionsGranted,
        permissions = permissions,
        uiState = viewModel.uiState,
        stepsBeforeStart = stepsBeforeStart!!,
        onStartClick = { startTime ->
          viewModel.storeStepsBeforeStart(startTime)
          viewModel.sendCommand("0")
        },
        onError = { exception ->
          showExceptionSnackbar(scaffoldState, scope, exception)
        },
        onPermissionsResult = {
          viewModel.initialLoad()
        },
        onPermissionsLaunch = { values ->
        permissionsLauncher.launch(values)
        },
        navController = navController
      )
    }
    composable(Screen.DifferentialChanges.route) {
      val viewModel: DifferentialChangesViewModel = viewModel(
        factory = DifferentialChangesViewModelFactory(
          healthConnectManager = healthConnectManager
        )
      )
      val changesToken by viewModel.changesToken
      val permissionsGranted by viewModel.permissionsGranted
      val permissions = viewModel.permissions
      val onPermissionsResult = {viewModel.initialLoad()}
      val permissionsLauncher =
        rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
          onPermissionsResult()}
      DifferentialChangesScreen(
        permissionsGranted = permissionsGranted,
        permissions = permissions,
        changesEnabled = changesToken != null,
        onChangesEnable = { enabled ->
          viewModel.enableOrDisableChanges(enabled)
        },
        changes = viewModel.changes,
        changesToken = changesToken,
        onGetChanges = {
          viewModel.getChanges()
        },
        uiState = viewModel.uiState,
        onError = { exception ->
          showExceptionSnackbar(scaffoldState, scope, exception)
        },
        onPermissionsResult = {
          viewModel.initialLoad()
        },
        onPermissionsLaunch = { values ->
          permissionsLauncher.launch(values)}
      )
    }
    composable(Screen.LoginScreen.route){
      LoginScreen(
        onSuccess = {
          navController.navigate(Screen.ModeScreen.route)
        }
      )
    }
    composable(Screen.ModeScreen.route){
      ModeScreen(
        navController = navController
      )
    }

    composable(Screen.ModeWithFriends.route){
      ModeWithFriends(
        navController = navController
      )
    }
    composable(
      "room_screen/{roomId}",
      arguments = listOf(navArgument("roomId") { type = NavType.StringType })
    ) { backStackEntry ->
      RoomScreen(navController, backStackEntry.arguments?.getString("roomId") ?: "")
    }
    composable(
      "set_up_room/{roomId}",
      arguments = listOf(navArgument("roomId") { type = NavType.StringType })
    ){ backStackEntry ->
      SetUpRoom(navController, backStackEntry.arguments?.getString("roomId") ?: "")
    }
    composable("${Screen.StepsCounterWithFriends.route}/{roomId}",
      arguments = listOf(navArgument("roomId") { type = NavType.StringType })
    ) { backStackEntry ->
      val viewModel: StepsCountWithFriendsViewModel = viewModel(
        factory = StepsCountWithFriendsViewModelFactory(
          healthConnectManager = healthConnectManager,
          dataStore = dataStore,
          bluetoothSocket = bluetoothSocket,
        )
      )
      val stepsBeforeStart by viewModel.stepsBeforeStart
      val permissionsGranted by viewModel.permissionsGranted
      val permissions = viewModel.permissions
      val onPermissionsResult = { viewModel.initialLoad() }
      val permissionsLauncher =
        rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
          onPermissionsResult()
        }
      StepsCountScreenWithFriends(
        permissionsGranted = permissionsGranted,
        permissions = permissions,
        uiState = viewModel.uiState,
        stepsBeforeStart = stepsBeforeStart!!,
        onStartClick = { startTime ->
          viewModel.storeStepsBeforeStart(startTime)
          viewModel.sendCommand("0")
          navController.navigate(Screen.ProgressScreen.route)
        },
        onError = { exception ->
          showExceptionSnackbar(scaffoldState, scope, exception)
        },
        onPermissionsResult = {
          viewModel.initialLoad()
        },
        onPermissionsLaunch = { values ->
        permissionsLauncher.launch(values)
        },
        navController = navController,
        roomId = backStackEntry.arguments?.getString("roomId") ?: ""
      )
    }
    composable(Screen.ProgressScreen.route) {
      val viewModel: ProgressScreenViewModel = viewModel(
        factory = ProgressScreenViewModelFactory(
          healthConnectManager = healthConnectManager,
          dataStore = dataStore,
          bluetoothSocket = bluetoothSocket,
        )
      )
      val permissionsGranted by viewModel.permissionsGranted
      val permissions = viewModel.permissions
      val onPermissionsResult = { viewModel.initialLoad() }
      val totalStepsFromDayBefore by viewModel.totalStepsFromDayBefore
      val stepsBeforeStart by viewModel.stepsBeforeStart
      val stepGoal by viewModel.stepGoal
      val startTime by viewModel.startTime
      val deadline by viewModel.deadline
      val mode by viewModel.mode
      val permissionsLauncher =
        rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
          onPermissionsResult()
        }
      ProgressScreen(
        permissionsGranted = permissionsGranted,
        permissions = permissions,
        uiState = viewModel.uiState,
        totalStepsFromDayBefore = totalStepsFromDayBefore!!,
        stepsBeforeStart = stepsBeforeStart!!,
        stepGoal = stepGoal!!,
        startTime = startTime!!,
        deadline = deadline!!,
        mode = mode!!,
        onError = { exception ->
          showExceptionSnackbar(scaffoldState, scope, exception)
        },
        onCheckClick = { startTime ->
          viewModel.getTotalStepsFromDayBefore(startTime)
        },
        onStopClick = { startTime ->
          viewModel.sendCommand("1")
          viewModel.getTotalStepsFromDayBefore(startTime)
          viewModel.endChallenge()
        },
        onHomeClick = {
          navController.navigate(Screen.WelcomeScreen.route)
        },
        onPermissionsResult = {
          viewModel.initialLoad()
        },
        onPermissionsLaunch =  { values ->
        permissionsLauncher.launch(values)
        },
        navController = navController
      )
    }
    composable(Screen.Leaderboard.route){
      Leaderboard(navController)
    }
  }
}
