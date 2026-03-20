package com.example.random_reversi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.random_reversi.data.AuthRepository
import com.example.random_reversi.data.AuthResult
import com.example.random_reversi.ui.components.AppModal
import com.example.random_reversi.ui.components.AuthButton
import com.example.random_reversi.ui.components.AuthFormContainer
import com.example.random_reversi.ui.components.AuthTextInput
import com.example.random_reversi.ui.theme.SecondaryColor
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    isOpen: Boolean,
    onClose: () -> Unit,
    onNavigate: (screen: String) -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AppModal(
        isOpen = isOpen,
        onClose = onClose,
        maxWidth = 400.dp,
        showCloseButton = true
    ) {
        AuthFormContainer(
            title = "Crear Cuenta",
            subtitle = "Únete a Random Reversi"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AuthTextInput(
                    label = "Nombre de usuario",
                    value = username,
                    placeholder = "Tu nombre de usuario",
                    onValueChange = { username = it }
                )

                AuthTextInput(
                    label = "Correo electrónico",
                    value = email,
                    placeholder = "tu@email.com",
                    onValueChange = { email = it },
                    keyboardType = KeyboardType.Email
                )

                AuthTextInput(
                    label = "Contraseña",
                    value = password,
                    placeholder = "••••••••",
                    onValueChange = {
                        password = it
                        passwordError = false
                    },
                    isPassword = true
                )

                AuthTextInput(
                    label = "Confirmar contraseña",
                    value = confirmPassword,
                    placeholder = "••••••••",
                    onValueChange = {
                        confirmPassword = it
                        passwordError = false
                    },
                    isPassword = true
                )

                AuthButton(
                    text = "Registrarse",
                    onClick = {
                        if (username.isNotEmpty() && email.isNotEmpty() && 
                            password.isNotEmpty() && password == confirmPassword && !isLoading) {
                            passwordError = false
                            errorMessage = null
                            isLoading = true

                            coroutineScope.launch {
                                when (val result = AuthRepository.register(
                                    username = username,
                                    email = email,
                                    password = password
                                )) {
                                    is AuthResult.Success -> {
                                        onRegisterSuccess()
                                        onNavigate("home")
                                        onClose()
                                    }
                                    is AuthResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                                isLoading = false
                            }
                        } else if (password != confirmPassword) {
                            passwordError = true
                            errorMessage = "Las contraseñas no coinciden"
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.padding(top = 8.dp)
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = SecondaryColor
                    )
                }
            }
        }
    }
}
