# Conexión Frontend Móvil ↔ Backend — Resumen de Cambios

## Estado previo

Solo estaban conectados con el backend:
- Login / Registro / Recuperar contraseña
- Perfil de usuario (`GET /api/users/me`)
- Personalización de avatar y colores (`PUT /api/users/customization`, `POST /api/users/avatar`)

Todo lo demás (amigos, partidas online, sala de espera, tablero de juego, ranking) usaba **datos mock hardcodeados** sin conexión real.

---

## Cambios realizados

### 1. AuthApiService.kt — Todos los endpoints del backend

Se añadieron **todos** los endpoints que expone el backend:

| Categoría | Endpoints añadidos |
|---|---|
| **Stats** | `GET /api/users/me/stats`, `GET /api/users/{id}/stats`, `GET /api/users/{id}/h2h` |
| **Historial** | `GET /api/users/me/history`, `POST /api/users/me/history`, `GET /api/users/{id}/history` |
| **Amigos** | `GET /api/friends` (panel social), `POST /api/friends/request`, `POST /api/friends/{id}/accept`, `POST /api/friends/{id}/reject`, `DELETE /api/friends/{id}` |
| **Chat** | `GET /api/friends/{id}/chat`, `POST /api/friends/{id}/chat`, `POST /api/friends/{id}/chat/read` |
| **Lobby/Partidas** | `POST /api/games/create`, `GET /api/games/public`, `POST /api/games/join/{id}`, `POST /api/games/invite`, `POST /api/games/{id}/accept`, `POST /api/games/{id}/reject`, `GET /api/games/{id}/state`, `POST /api/games/{id}/ready`, `POST /api/games/{id}/leave` |
| **Ranking** | `GET /api/ranking/` |

Se crearon los **data classes** necesarios para request/response de cada endpoint.

### 2. Repositorios nuevos

| Archivo | Función |
|---|---|
| `FriendsRepository.kt` | Panel social, enviar/aceptar/rechazar solicitudes, eliminar amigos, chat |
| `GamesRepository.kt` | Crear lobby, listar públicos, unirse, invitar amigos, estado lobby, ready, abandonar, stats e historial |
| `RankingRepository.kt` | Obtener ranking global |

### 3. FriendsScreen.kt — Conectada al backend

**Antes:** Lista de amigos hardcodeada (MOCK_FRIENDS, MOCK_REQUESTS, etc.)

**Ahora:**
- Al entrar, llama a `GET /api/friends` para cargar amigos reales, solicitudes pendientes e invitaciones de juego
- **Añadir amigo:** envía `POST /api/friends/request` con el username
- **Aceptar/Rechazar solicitud:** llama a los endpoints correspondientes y recarga la lista
- **Eliminar amigo:** llama a `DELETE /api/friends/{id}`
- **Invitaciones de juego:** muestra las del backend y permite aceptar/rechazar
- Loading spinner mientras carga, mensajes de error reales

### 4. OnlineGameScreen.kt — Lobbies reales

**Antes:** Grid de partidas mock, ELO hardcodeado a 1500, historial fake.

**Ahora:**
- **ELO real** del usuario desde `GET /api/users/me`
- **Historial real** desde `GET /api/users/me/history` (últimas 5 partidas con V/D/E)
- **Lobbies públicos reales** desde `GET /api/games/public`
- **Crear partida:** `POST /api/games/create` → navega a waiting-room con el gameId real
- **Unirse a partida:** `POST /api/games/join/{id}` → navega a waiting-room
- Botón "Actualizar" recarga datos reales del backend
- Indicadores de carga y errores

### 5. WaitingRoomScreen.kt — Estado real del lobby

**Antes:** Simulaba que se unían oponentes tras 3 segundos.

**Ahora:**
- Recibe `gameId` del lobby real
- **Polling cada 2 segundos** a `GET /api/games/{id}/state` para ver jugadores, estados ready, etc.
- **Botón "Listo":** llama a `POST /api/games/{id}/ready`
- **Abandonar:** llama a `POST /api/games/{id}/leave`
- Cuando el backend cambia el estado a `"playing"`, navega automáticamente al tablero de juego
- Muestra avatares de otros jugadores si los tienen

### 6. GameWebSocket.kt — Comunicación en tiempo real (NUEVO)

Archivo completamente nuevo que gestiona la conexión WebSocket para partidas:

- **Conexión:** `ws://{host}/ws/play/{game_id}?token={jwt}`
- **Recibe:**
  - `player_assignment` → tu color asignado (black/white/piece_1-4)
  - `game_state_update` → tablero completo, turno actual, movimientos válidos, scores, game over
  - `chat_message` → mensajes del chat en partida
  - `room_sync` → sincronización de jugadores
- **Envía:**
  - `make_move` → mover ficha a (row, col)
  - `chat` → enviar mensaje
  - `set_ready` → marcar listo
  - `surrender` → rendirse
- Usa OkHttp WebSocket (ya incluido como dependencia)
- Expone StateFlows para que la UI se actualice reactivamente

### 7. GameBoard1v1Screen.kt — Partida real 1vs1

**Antes:** Tablero estático con piezas iniciales, sin lógica.

**Ahora:**
- Se conecta por WebSocket al iniciar
- **Tablero dinámico:** renderiza el estado del board que envía el servidor
- **Movimientos válidos:** muestra puntos verdes donde puedes colocar ficha (solo en tu turno)
- **Hacer movimiento:** click en celda válida → envía por WebSocket
- **Indicador de turno:** "TU TURNO" / "TURNO DEL RIVAL"
- **Scores en tiempo real** de ambos jugadores
- **Chat en partida:** panel colapsable para enviar/recibir mensajes
- **Rendirse:** diálogo de confirmación → envía surrender por WebSocket
- **Game Over:** diálogo con resultado (Victoria/Derrota/Empate) y puntuaciones
- **Estado de conexión:** indicador visual (verde/amarillo/rojo)

### 8. GameBoard1v1v1v1Screen.kt — Partida real 4 jugadores

**Antes:** Tablero estático mock.

**Ahora:**
- Misma integración WebSocket que 1v1
- Scores de los 4 jugadores en tiempo real
- Sistema de zoom por cuadrantes con movimientos válidos
- Indicador de turno muestra qué jugador toca
- Surrender y Game Over funcionales

### 9. RankingScreen.kt — Pantalla nueva

- Muestra el **Top 50 global** desde `GET /api/ranking/`
- Medallas dorado/plata/bronce para top 3
- Muestra username y ELO de cada jugador
- Loading spinner y manejo de errores

### 10. MainScreen.kt — Acceso al ranking

- Se reemplazó el botón ancho de "Reglas del juego" por una fila con dos botones:
  - **Ranking** (nuevo) → navega a pantalla de ranking
  - **Reglas** (existente) → navega a reglas

### 11. MainActivity.kt — Navegación actualizada

- Rutas ahora soportan parámetros: `waiting-room/{mode}/{gameId}`, `game-1vs1/{gameId}`, `game-1vs1vs1vs1/{gameId}`
- Se añadió ruta `ranking`
- Parsing de rutas mejorado con split por `/`

---

## Flujo completo de una partida online (ahora funcional)

1. **Menú** → "Jugar Online"
2. **OnlineGameScreen** → Ve lobbies públicos reales, tu ELO, historial
3. **"Crear Partida"** → Elige modo → `POST /api/games/create` → Navega a sala de espera con gameId
4. **WaitingRoomScreen** → Polling cada 2s al backend, ve otros jugadores uniéndose en tiempo real
5. **"Estoy Listo"** → `POST /api/games/{id}/ready` → Cuando todos listos, backend cambia a "playing"
6. **GameBoard** → WebSocket conectado, tablero real, movimientos en tiempo real, chat
7. **Fin** → Game Over dialog con resultado real

---

## Archivos modificados

| Archivo | Tipo de cambio |
|---|---|
| `data/remote/AuthApiService.kt` | Ampliado (todos los endpoints + modelos) |
| `data/FriendsRepository.kt` | **Nuevo** |
| `data/GamesRepository.kt` | **Nuevo** |
| `data/RankingRepository.kt` | **Nuevo** |
| `data/remote/GameWebSocket.kt` | **Nuevo** |
| `ui/screens/FriendsScreen.kt` | Reescrito (mock → backend real) |
| `ui/screens/OnlineGameScreen.kt` | Reescrito (mock → backend real) |
| `ui/screens/WaitingRoomScreen.kt` | Reescrito (mock → backend real + polling) |
| `ui/screens/GameBoard1v1Screen.kt` | Reescrito (UI estática → WebSocket real) |
| `ui/screens/GameBoard1v1v1v1Screen.kt` | Reescrito (UI estática → WebSocket real) |
| `ui/screens/RankingScreen.kt` | **Nuevo** |
| `ui/screens/MainScreen.kt` | Modificado (añadido botón Ranking) |
| `MainActivity.kt` | Reescrito (navegación con parámetros) |

---

## Notas técnicas

- **No se ha tocado ningún archivo del backend**
- La URL base sigue siendo `http://10.0.2.2:8081/` (emulador Android → Docker local)
- OkHttp WebSocket ya estaba como dependencia transitiva de Retrofit, no se necesitan nuevas dependencias
- El token JWT se envía tanto por header HTTP (para REST) como por query param (para WebSocket)
- El polling de la sala de espera es cada 2 segundos; podría optimizarse con WebSocket de notificaciones en el futuro
