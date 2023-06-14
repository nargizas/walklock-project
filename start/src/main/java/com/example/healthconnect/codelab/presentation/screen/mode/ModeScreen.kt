package com.example.healthconnect.codelab.presentation.screen.mode

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.healthconnect.codelab.presentation.navigation.Screen

@Composable
fun ModeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Let's take a walk!",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = { navController.navigate(Screen.StepsCounter.route) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "I want to walk by myself")
        }

        Button(
            onClick = { navController.navigate(Screen.ModeWithFriends.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "I want to walk with my friends")
        }
    }
}

@Composable
@Preview
fun PreviewWalkPage() {
    val navController = rememberNavController()
    ModeScreen(navController)
}