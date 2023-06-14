package com.example.healthconnect.codelab.presentation.screen.login

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthconnect.codelab.presentation.screen.mode.UserInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun LoginScreen(
    onSuccess: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var success_message by remember { mutableStateOf("") }

    fun loginUser(email: String, password: String) {
        success_message = ""
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login successful, navigate to the main screen
                    onSuccess()
                    error = ""
                } else {
                    val exception = task.exception as? FirebaseAuthException
                    val errorMessage = exception?.message ?: "Unknown error occurred"
                    success_message = ""
                    error = "Wrong password or email."
                }
            }
    }

    fun signUpUser(email: String, password: String) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign up successful, navigate to the main screen
                    success_message = "You have successfully signed up"

                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val userRef = FirebaseFirestore.getInstance().collection("users")
                    val user = UserInfo(userId = currentUser!!.uid, email = currentUser.email!!)

                    userRef.document(currentUser.uid)
                        .set(user)
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener { exception ->
                            Log.e("CreateRoom", "Failed to create room: ${exception.message}")
                            // Handle room creation failure
                        }
                } else {
                    val exception = task.exception as? FirebaseAuthException
                    val errorMessage = exception?.message ?: "Unknown error occurred"
                    success_message = ""
                    error = "Error: $errorMessage"
                }
            }
    }

    fun validateFields(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                error = "Please enter your email."
                false
            }
            password.isEmpty() -> {
                error = "Please enter your password."
                false
            }
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(text = "Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        if (error.isNotEmpty()) {
            Text(
                text = error,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (success_message.isNotEmpty()) {
            Text(
                text = success_message,
                style = MaterialTheme.typography.body2,
                color = Color.Green,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = { if (validateFields(email, password)) loginUser(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Log in")
        }

        Button(
            onClick = { if (validateFields(email, password)) signUpUser(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Sign Up")
        }
    }
}



@Composable
@Preview
fun PreviewLoginScreen() {
    LoginScreen()
}