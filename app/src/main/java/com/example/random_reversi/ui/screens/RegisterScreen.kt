package com.example.random_reversi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.random_reversi.R
import com.example.random_reversi.data.AuthRepository
import com.example.random_reversi.data.AuthResult
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
    if (!isOpen) return

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(360.dp) // Maintain similar size reference box as login
                .height(600.dp), // Registration has more fields, box needs to be taller
            contentAlignment = Alignment.Center
        ) {
            // Fondo Registro PNG
            Image(
                painter = painterResource(id = R.drawable.positregistro),
                contentDescription = "Fondo Crear Cuenta",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 10.dp)
            )

            // Contenido Interno
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight(0.85f)
                    .padding(top = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Título invisible para desplazar la colisión del marco principal
                Text(
                    text = "Crear Cuenta".uppercase(),
                    color = Color.Transparent,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )

                // Error Ocular
                errorMessage?.let {
                    Text(
                        text = it,
                        color = SecondaryColor,
                        fontSize = 13.sp
                    )
                }

                // Inputs
                AuthTextInput(
                    label = "Nombre de usuario",
                    value = username,
                    placeholder = "Tu nombre de usuario",
                    onValueChange = { username = it },
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.padding(start = 20.dp, end = 24.dp)
                )

                AuthTextInput(
                    label = "Correo electrónico",
                    value = email,
                    placeholder = "usuario@ejemplo.com",
                    onValueChange = { email = it },
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.padding(start = 20.dp, end = 24.dp)
                )

                AuthTextInput(
                    label = "Contraseña",
                    value = password,
                    placeholder = "••••••••",
                    onValueChange = {
                        password = it
                        passwordError = false
                    },
                    isPassword = true,
                    modifier = Modifier.padding(start = 20.dp, end = 24.dp)
                )

                AuthTextInput(
                    label = "Confirmar contraseña",
                    value = confirmPassword,
                    placeholder = "••••••••",
                    onValueChange = {
                        confirmPassword = it
                        passwordError = false
                    },
                    isPassword = true,
                    modifier = Modifier.padding(start = 20.dp, end = 24.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Botón Gráfico "Registrarse" o botonantaregistro.png
                Image(
                    painter = painterResource(id = R.drawable.botonregistro),
                    contentDescription = "Registrarse",
                    modifier = Modifier
                        .fillMaxWidth(1.2f)
                        .height(85.dp)
                        .padding(bottom = 32.dp)
                        .clickable(enabled = !isLoading) {
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
                    contentScale = ContentScale.Fit
                )
            }

            // Botón X (Cerrar) en la esquina superior derecha
            Image(
                painter = painterResource(id = R.drawable.x),
                contentDescription = "Cerrar",
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-58).dp, y = 125.dp)
                    .clickable { onClose() },
                contentScale = ContentScale.Fit
            )
        }
    }
}
