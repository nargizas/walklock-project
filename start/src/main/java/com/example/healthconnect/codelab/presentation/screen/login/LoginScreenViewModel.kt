package com.example.healthconnect.codelab.presentation.screen.login

import androidx.compose.runtime.mutableStateOf
import com.example.healthconnect.codelab.presentation.screen.LoginUiState

class LoginScreenViewModel {
    var uiState = mutableStateOf(LoginUiState())
        private set
}