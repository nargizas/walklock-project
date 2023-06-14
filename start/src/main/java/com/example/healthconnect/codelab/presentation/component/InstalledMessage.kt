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
package com.example.healthconnect.codelab.presentation.component

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.healthconnect.codelab.R
import com.example.healthconnect.codelab.presentation.navigation.Screen
import com.example.healthconnect.codelab.presentation.theme.HealthConnectTheme

/**
 * Welcome text shown when the app first starts, where the Health Connect APK is already installed.
 */
@Composable
fun InstalledMessage(
  navController: NavController,
  isLoggedIn: Boolean,
  navigateTo: Int,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ){
    Text(
      text = stringResource(id = R.string.installed_welcome_message),
      textAlign = TextAlign.Justify
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .padding(4.dp),
      onClick = {
        if (isLoggedIn) {
          if (navigateTo == 1){
            Log.d("HealthConnect", "value is $navigateTo")
            Log.d("HealthConnect", "go to ProgressScreen")
            navController.navigate(Screen.ProgressScreen.route)
          } else {
            Log.d("HealthConnect", "value is $navigateTo")
            Log.d("HealthConnect", "go to ModeScreen")
            navController.navigate(Screen.ModeScreen.route)
          }
        } else {
          navController.navigate(Screen.LoginScreen.route)
        }
      },
    ) {
      Text(stringResource(id = R.string.start_button_label))
    }
  }

}

@Preview
@Composable
fun InstalledMessagePreview() {
  val navController = rememberNavController()
  HealthConnectTheme {
    InstalledMessage(navController = navController, isLoggedIn = true, navigateTo = 1)
  }
}
