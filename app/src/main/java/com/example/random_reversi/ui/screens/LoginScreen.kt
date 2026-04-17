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
fun LoginScreen(
    isOpen: Boolean,
    onClose: () -> Unit,
    onNavigate: (screen: String) -> Unit
) {
    if (!isOpen) return

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Permite salir de los márgenes nativos
    ) {
        // Envoltorio para permitir que el robot "asome" por el marco del post it
        Box(
            modifier = Modifier
                .width(360.dp)
                .height(460.dp),
            contentAlignment = Alignment.Center
        ) {
            // Imagen del Post-It (Fondo visual)
            Image(
                painter = painterResource(id = R.drawable.positiniciarsesion),
                contentDescription = "Fondo Iniciar Sesión",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 10.dp) // Espacio para que el robot superpuesto quede más libre
            )

            // Contenido Interno
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight(0.75f)
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Título dibujado sobre el post it (invisible para mantener la posición del resto)
                Text(
                    text = "Iniciar Sesión".uppercase(),
                    color = Color.Transparent,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )

                // Error
                errorMessage?.let {
                    Text(
                        text = it,
                        color = SecondaryColor,
                        fontSize = 13.sp
                    )
                }

                // Inputs
                AuthTextInput(
                    label = "Usuario o Correo",
                    value = email,
                    placeholder = "Tu Usuario o Correo",
                    onValueChange = { email = it },
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.padding(start = 20.dp, end = 24.dp)
                )

                AuthTextInput(
                    label = "Contraseña",
                    value = password,
                    placeholder = "••••••••",
                    onValueChange = { password = it },
                    isPassword = true,
                    modifier = Modifier.padding(start = 20.dp, end = 24.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Boton Entrar gráfico nativo
                Image(
                    painter = painterResource(id = R.drawable.botonentrar),
                    contentDescription = "Entrar",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(55.dp)
                        .padding(bottom = 8.dp)
                        .clickable(enabled = !isLoading) {
                            if (email.isNotEmpty() && password.isNotEmpty()) {
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
                    contentScale = ContentScale.Fit
                )
            }

        }
    }
}
