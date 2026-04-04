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
fun LoginScreen(
    isOpen: Boolean,
    onClose: () -> Unit,
    onNavigate: (screen: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
            title = "Iniciar Sesión",
            subtitle = "Bienvenido de vuelta a Random Reversi"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AuthTextInput(
                    label = "Usuario o Correo",
                    value = email,
                    placeholder = "Tu Usuario o Correo",
                    onValueChange = { email = it },
                    keyboardType = KeyboardType.Text
                )

                AuthTextInput(
                    label = "Contraseña",
                    value = password,
                    placeholder = "••••••••",
                    onValueChange = { password = it },
                    isPassword = true
                )

                AuthButton(
                    text = "Entrar",
                    onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty() && !isLoading) {
                            errorMessage = null
                            isLoading = true

                            coroutineScope.launch {
                                when (val result = AuthRepository.login(email = email, password = password)) {
                                    is AuthResult.Success -> {
                                        onNavigate("menu")
                                        onClose()
                                    }
                                    is AuthResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                                isLoading = false
                            }
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
