package com.example.random_reversi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.data.AuthRepository
import com.example.random_reversi.data.AuthResult
import com.example.random_reversi.ui.components.AppModal
import com.example.random_reversi.ui.components.AuthButton
import com.example.random_reversi.ui.components.AuthFormContainer
import com.example.random_reversi.ui.components.AuthTextInput
import com.example.random_reversi.ui.theme.PrimaryColor
import com.example.random_reversi.ui.theme.SecondaryColor
import com.example.random_reversi.ui.theme.TextColor
import com.example.random_reversi.ui.theme.TextMutedColor
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    isOpen: Boolean,
    onClose: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var emailSent by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Resetear estado al cerrar
    if (!isOpen) {
        LaunchedEffect(isOpen) {
            email = ""
            errorMessage = null
            emailSent = false
            isLoading = false
        }
    }

    AppModal(
        isOpen = isOpen,
        onClose = onClose,
        maxWidth = 400.dp,
        showCloseButton = true
    ) {
        if (!emailSent) {
            // Paso 1: Pedir correo electrónico
            AuthFormContainer(
                title = "Recuperar contraseña",
                subtitle = "Introduce tu correo electrónico y te enviaremos un código para restablecer tu contraseña"
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
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        keyboardType = KeyboardType.Email
                    )

                    AuthButton(
                        text = if (isLoading) "Enviando..." else "Enviar código",
                        onClick = {
                            if (email.isNotEmpty() && !isLoading) {
                                errorMessage = null
                                isLoading = true

                                coroutineScope.launch {
                                    when (val result = AuthRepository.forgotPassword(email)) {
                                        is AuthResult.Success -> {
                                            emailSent = true
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
                            color = SecondaryColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // Paso 2: Confirmación de envío
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "✉️",
                    fontSize = 48.sp
                )

                Text(
                    text = "Revise su correo electrónico",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Revise su correo electrónico para restablecer su contraseña",
                    fontSize = 14.sp,
                    color = TextMutedColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Text(
                    text = email,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryColor,
                    textAlign = TextAlign.Center
                )

                AuthButton(
                    text = "Entendido",
                    onClick = onClose,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
