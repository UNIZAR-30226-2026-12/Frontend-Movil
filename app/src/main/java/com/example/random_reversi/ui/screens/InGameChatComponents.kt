package com.example.random_reversi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.ui.theme.BgColor
import com.example.random_reversi.ui.theme.PrimaryColor
import com.example.random_reversi.ui.theme.TextMutedColor

@Composable
fun InGameChatButton(
    unreadCount: Int,
    onClick: () -> Unit
) {
    Box {
        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFA78BFA),
                containerColor = Color(0xFF7C3AED).copy(alpha = 0.16f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.45f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Chat", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(Color(0xFFEF4444), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun InGameChatOverlay(
    messages: List<Pair<String, String>>,
    myUsername: String,
    onClose: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Image(
                painter = painterResource(id = com.example.random_reversi.R.drawable.chatmovil),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
                Spacer(modifier = Modifier.height(75.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chat de partida", color = Color(0xFF2C1B0C), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay mensajes todavía.", color = TextMutedColor, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { (sender, message) ->
                                val mine = sender == myUsername
                                Column(
                                    horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (!mine) {
                                        Text(sender, color = TextMutedColor, fontSize = 11.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                brush = if (mine) {
                                                    Brush.verticalGradient(listOf(Color(0xFFE8B56B), Color(0xFFD9984D)))
                                                } else {
                                                    Brush.verticalGradient(listOf(Color(0xFFFCF1DB), Color(0xFFFCF1DB)))
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (mine) Color(0xFF865525).copy(alpha = 0.28f)
                                                else Color(0xFF785228).copy(alpha = 0.2f),
                                                RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Text(
                                            text = message,
                                            color = if (mine) Color(0xFF2B1707) else Color(0xFF2C1B0C),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(Color(0xFFFFF7E9).copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        textStyle = TextStyle(color = Color(0xFF2C1B0C), fontSize = 14.sp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val clean = text.trim()
                                if (clean.isNotBlank()) {
                                    onSend(clean)
                                    text = ""
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (text.isEmpty()) {
                                    Text("Escribe un mensaje...", color = Color(0xFF2C1B0C).copy(alpha = 0.5f), fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        }
                    )

                    Button(
                        onClick = {
                            val clean = text.trim()
                            if (clean.isNotBlank()) {
                                onSend(clean)
                                text = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9984D)),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text("Enviar")
                    }
                }
            }
            
            // Botón X (Cerrar)
            Image(
                painter = painterResource(id = com.example.random_reversi.R.drawable.x),
                contentDescription = "Cerrar",
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = 50.dp)
                    .clickable { onClose() },
                contentScale = ContentScale.Fit
            )
        }
    }
}
