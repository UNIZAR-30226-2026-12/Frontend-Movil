package com.example.random_reversi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.random_reversi.ui.components.AppModal
import com.example.random_reversi.ui.components.AuthButton
import com.example.random_reversi.ui.components.AuthFormContainer
import com.example.random_reversi.ui.components.AuthTextInput

@Composable
fun LoginScreen(
    isOpen: Boolean,
    onClose: () -> Unit,
    onNavigate: (screen: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                    onValueChange = { password = it },
                    isPassword = true
                )

                AuthButton(
                    text = "Entrar",
                    onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            // TODO: Validar credenciales con API
                            onNavigate("menu")
                            onClose()
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
