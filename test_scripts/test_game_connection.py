"""
TEST SUITE: CONEXION FRONTEND-MOVIL → BACKEND (PARTIDAS Y WEBSOCKET)
====================================================================
Verifica que los endpoints de partidas y la conexion WebSocket usados
por el frontend movil (GamesRepository.kt, GameWebSocket.kt) funcionan
correctamente, simulando las mismas peticiones que hace la app Android.

Ambitos cubiertos:
  1. Creacion de sala publica (POST /api/games/create)
  2. Listado de lobbies publicos (GET /api/games/public)
  3. Union a sala y bloqueo de sala llena
  4. Estado del lobby (GET /api/games/{id}/state) — polling del frontend
  5. Ready y leave (POST /api/games/{id}/ready, /leave)
  6. WebSocket de partida: player_assignment, game_state_update
  7. Movimientos por WebSocket y sincronizacion
  8. Chat en partida por WebSocket
  9. Rendicion por WebSocket
 10. Invitaciones privadas entre amigos
"""

import asyncio
import websockets
import requests
import json
import uuid
import sys

BASE_URL = "http://localhost:8081"
WS_URL   = "ws://localhost:8081"

# ─────────────────────────────────────────────
#  UTILIDADES
# ─────────────────────────────────────────────

def step(n, msg):
    print(f"\n[PASO {n}] {msg}")

def ok(msg):
    print(f"         ✓ OK: {msg}")

def debug(msg):
    print(f"         · DEBUG: {msg}")

def create_and_login(username, password="password123"):
    email = f"{username}@test.com"
    requests.post(f"{BASE_URL}/api/auth/register", json={
        "username": username, "email": email, "password": password
    })
    return requests.post(f"{BASE_URL}/api/auth/login", data={
        "username": username, "password": password
    }).json()["access_token"]

def delete_user(token, username):
    res = requests.delete(f"{BASE_URL}/api/users/me",
                          headers={"Authorization": f"Bearer {token}"})
    if res.status_code == 200:
        print(f"   [Limpieza] '{username}' eliminado.")
    else:
        print(f"   [Limpieza] ATENCION — No se pudo eliminar '{username}': {res.text}")

def get_user_id(token):
    return requests.get(f"{BASE_URL}/api/users/me",
                        headers={"Authorization": f"Bearer {token}"}).json()["id"]

async def safe_recv(ws, label="WS", timeout=5.0):
    """Recibe mensaje del WS, auto-responde a waiting_for_player e ignora room_sync."""
    try:
        while True:
            raw = await asyncio.wait_for(ws.recv(), timeout=timeout)
            data = json.loads(raw)
            tipo = data.get("type")

            if tipo == "waiting_for_player":
                await ws.send(json.dumps({"action": "set_ready", "ready": True}))
                continue
            if tipo == "room_sync":
                continue

            debug(f"[{label}] tipo='{tipo}' | payload={str(data.get('payload', ''))[:120]}")
            return data
    except asyncio.TimeoutError:
        debug(f"[{label}] TIMEOUT ({timeout}s)")
        return None
    except Exception as e:
        debug(f"[{label}] Error: {e}")
        return None

async def wait_for_game_update(ws, timeout=4.0):
    for _ in range(10):
        msg = await safe_recv(ws, timeout=timeout)
        if msg and msg.get("type") == "game_state_update":
            return msg
    return None

async def wait_for_chat_msg(ws, label, expected_sender, timeout=6.0):
    deadline = asyncio.get_event_loop().time() + timeout
    while asyncio.get_event_loop().time() < deadline:
        remaining = deadline - asyncio.get_event_loop().time()
        raw = await safe_recv(ws, label=label, timeout=min(remaining, 2.0))
        if raw is None:
            break
        if raw.get("type") == "chat_message":
            sender = raw.get("payload", {}).get("sender")
            if sender == expected_sender:
                return raw
    return None


# ─────────────────────────────────────────────
#  BLOQUE 1: LOBBY HTTP (crear, listar, unir)
# ─────────────────────────────────────────────

def run_lobby_http_test():
    print("\n" + "="*60)
    print("  BLOQUE 1: LOBBY HTTP — CREAR, LISTAR Y UNIRSE")
    print("="*60)

    u1 = f"host_{uuid.uuid4().hex[:4]}"
    u2 = f"guest_{uuid.uuid4().hex[:4]}"
    u3 = f"extra_{uuid.uuid4().hex[:4]}"
    t1, t2, t3 = None, None, None

    try:
        step(1, "Creando tres usuarios...")
        t1 = create_and_login(u1)
        t2 = create_and_login(u2)
        t3 = create_and_login(u3)
        ok("Tres usuarios listos")

        # --- Crear sala ---
        step(2, f"'{u1}' crea sala publica 1v1 (simula GamesRepository.createLobby)...")
        res_create = requests.post(
            f"{BASE_URL}/api/games/create",
            headers={"Authorization": f"Bearer {t1}"},
            json={"mode": "1vs1"}
        )
        assert res_create.status_code == 200, f"HTTP {res_create.status_code}: {res_create.text}"
        game_id = res_create.json()["game_id"]
        assert game_id is not None, "No se devolvio game_id"
        debug(f"game_id creado: '{game_id}'")
        ok(f"Sala creada: {game_id}")

        # --- Listar lobbies ---
        step(3, "Listando lobbies publicos (simula GamesRepository.getPublicLobbies)...")
        res_pub = requests.get(f"{BASE_URL}/api/games/public")
        assert res_pub.status_code == 200, f"HTTP {res_pub.status_code}: {res_pub.text}"
        lobbies = res_pub.json().get("lobbies", [])
        sala = next((l for l in lobbies if l["game_id"] == game_id), None)
        assert sala is not None, f"Sala '{game_id}' no visible en lobbies: {[l['game_id'] for l in lobbies]}"
        # Verificar campos que el frontend espera (PublicLobby)
        for campo in ["game_id", "mode"]:
            assert campo in sala, f"Falta '{campo}' en PublicLobby: {sala}"
        debug(f"Sala en lobby: {sala}")
        ok("Sala visible en el listado publico con campos correctos")

        # --- Unirse ---
        step(4, f"'{u2}' se une a la sala (simula GamesRepository.joinLobby)...")
        res_join = requests.post(
            f"{BASE_URL}/api/games/join/{game_id}",
            headers={"Authorization": f"Bearer {t2}"}
        )
        assert res_join.status_code == 200, f"HTTP {res_join.status_code}: {res_join.text}"
        ok(f"'{u2}' unido correctamente")

        # --- Sala llena ---
        step(5, f"'{u3}' intenta unirse a sala llena (debe rechazar)...")
        res_full = requests.post(
            f"{BASE_URL}/api/games/join/{game_id}",
            headers={"Authorization": f"Bearer {t3}"}
        )
        assert res_full.status_code == 400, \
            f"Se permitio entrar a sala llena: HTTP {res_full.status_code}"
        ok(f"Sala llena bloqueada: HTTP {res_full.status_code}")

        # --- Sala inexistente ---
        step(6, "Intentando unirse a sala inexistente...")
        res_fake = requests.post(
            f"{BASE_URL}/api/games/join/sala-fantasma-xyz",
            headers={"Authorization": f"Bearer {t3}"}
        )
        assert res_fake.status_code in (400, 404, 500), \
            f"Sala fantasma aceptada: HTTP {res_fake.status_code}"
        ok(f"Sala inexistente rechazada: HTTP {res_fake.status_code}")

        # --- Estado del lobby (polling) ---
        step(7, f"Consultando estado del lobby (simula polling cada 2s del frontend)...")
        res_state = requests.get(
            f"{BASE_URL}/api/games/{game_id}/state",
            headers={"Authorization": f"Bearer {t1}"}
        )
        assert res_state.status_code == 200, f"HTTP {res_state.status_code}: {res_state.text}"
        state = res_state.json()
        debug(f"Estado del lobby: {state}")
        # LobbyStateResponse espera: players, status
        assert "players" in state, f"Falta 'players' en LobbyStateResponse: {state}"
        ok("Estado del lobby obtenido correctamente")

        print("\n  ✔ BLOQUE 1 PASADO: Lobby HTTP OK")
        return True

    except AssertionError as e:
        print(f"\n  ✘ BLOQUE 1 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 1]")
        for t, u in [(t1, u1), (t2, u2), (t3, u3)]:
            if t: delete_user(t, u)


# ─────────────────────────────────────────────
#  BLOQUE 2: READY Y LEAVE
# ─────────────────────────────────────────────

def run_ready_leave_test():
    print("\n" + "="*60)
    print("  BLOQUE 2: READY Y LEAVE EN LOBBY")
    print("="*60)

    u1 = f"ready_a_{uuid.uuid4().hex[:4]}"
    u2 = f"ready_b_{uuid.uuid4().hex[:4]}"
    t1, t2 = None, None

    try:
        step(1, "Creando usuarios y sala...")
        t1 = create_and_login(u1)
        t2 = create_and_login(u2)
        h1 = {"Authorization": f"Bearer {t1}"}
        h2 = {"Authorization": f"Bearer {t2}"}

        res = requests.post(f"{BASE_URL}/api/games/create", headers=h1, json={"mode": "1vs1"})
        game_id = res.json()["game_id"]
        requests.post(f"{BASE_URL}/api/games/join/{game_id}", headers=h2)
        ok(f"Sala '{game_id}' con 2 jugadores")

        # --- Ready ---
        step(2, f"'{u1}' marca ready (simula GamesRepository.setReady)...")
        res_ready = requests.post(f"{BASE_URL}/api/games/{game_id}/ready", headers=h1)
        assert res_ready.status_code == 200, f"HTTP {res_ready.status_code}: {res_ready.text}"
        ok("Ready marcado correctamente")

        # --- Leave ---
        step(3, f"'{u2}' abandona la sala antes de empezar (simula GamesRepository.leaveLobby)...")
        res_leave = requests.post(f"{BASE_URL}/api/games/{game_id}/leave", headers=h2)
        assert res_leave.status_code == 200, f"HTTP {res_leave.status_code}: {res_leave.text}"
        ok(f"'{u2}' abandono la sala: {res_leave.json()}")

        print("\n  ✔ BLOQUE 2 PASADO: Ready y Leave OK")
        return True

    except AssertionError as e:
        print(f"\n  ✘ BLOQUE 2 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 2]")
        if t1: delete_user(t1, u1)
        if t2: delete_user(t2, u2)


# ─────────────────────────────────────────────
#  BLOQUE 3: WEBSOCKET DE PARTIDA 1v1
# ─────────────────────────────────────────────

async def run_websocket_game_test():
    print("\n" + "="*60)
    print("  BLOQUE 3: WEBSOCKET DE PARTIDA 1v1 (FLUJO COMPLETO)")
    print("="*60)

    u1 = f"ws_p1_{uuid.uuid4().hex[:4]}"
    u2 = f"ws_p2_{uuid.uuid4().hex[:4]}"
    t1, t2 = None, None

    try:
        step(1, "Creando sala y uniendo jugadores...")
        t1 = create_and_login(u1)
        t2 = create_and_login(u2)

        res = requests.post(f"{BASE_URL}/api/games/create",
                            headers={"Authorization": f"Bearer {t1}"}, json={"mode": "1vs1"})
        game_id = res.json()["game_id"]
        requests.post(f"{BASE_URL}/api/games/join/{game_id}",
                      headers={"Authorization": f"Bearer {t2}"})
        ok(f"Sala '{game_id}' lista")

        # --- Conectar WebSockets (simula GameWebSocket.connect) ---
        step(2, "Conectando WebSockets (simula GameWebSocket.kt)...")
        async with websockets.connect(f"{WS_URL}/ws/play/{game_id}?token={t1}") as ws1, \
                   websockets.connect(f"{WS_URL}/ws/play/{game_id}?token={t2}") as ws2:

            # --- player_assignment ---
            step(3, "Verificando player_assignment para ambos jugadores...")
            asig1 = await safe_recv(ws1, label=f"asig-{u1}")
            asig2 = await safe_recv(ws2, label=f"asig-{u2}")

            assert asig1 and asig1.get("type") == "player_assignment", \
                f"'{u1}' no recibio player_assignment: {asig1}"
            assert asig2 and asig2.get("type") == "player_assignment", \
                f"'{u2}' no recibio player_assignment: {asig2}"

            color1 = asig1.get("payload", {}).get("color")
            color2 = asig2.get("payload", {}).get("color")
            assert color1 in ("black", "white"), f"Color invalido: {color1}"
            assert color2 in ("black", "white"), f"Color invalido: {color2}"
            assert color1 != color2, f"Ambos tienen el mismo color: {color1}"
            debug(f"Colores: {u1}={color1}, {u2}={color2}")
            ok(f"Asignacion correcta: {u1}={color1}, {u2}={color2}")

            # --- game_state_update inicial ---
            step(4, "Esperando tablero inicial (game_state_update)...")
            tablero1 = await wait_for_game_update(ws1)
            tablero2 = await wait_for_game_update(ws2)

            assert tablero1 is not None, f"'{u1}' no recibio tablero inicial"
            assert tablero2 is not None, f"'{u2}' no recibio tablero inicial"

            payload1 = tablero1["payload"]
            payload2 = tablero2["payload"]
            # Verificar campos que GameWebSocket.kt parsea
            for campo in ["board", "current_player", "scores"]:
                assert campo in payload1, f"Falta '{campo}' en game_state_update: {list(payload1.keys())}"
            assert payload1["board"] == payload2["board"], "Tableros iniciales no coinciden"
            debug(f"Turno inicial: {payload1['current_player']}")
            ok("Tablero inicial sincronizado en ambos clientes")

            # --- Movimiento (simula GameWebSocket sendMove) ---
            step(5, "Enviando movimiento (simula action=make_move)...")
            mov = {"action": "make_move", "row": 2, "col": 3}
            # Determinar quien es negras y enviar con ese WS
            ws_black = ws1 if color1 == "black" else ws2
            ws_white = ws2 if color1 == "black" else ws1
            await ws_black.send(json.dumps(mov))
            debug(f"Movimiento enviado por negras: {mov}")

            res_black = await safe_recv(ws_black, label="post-mov-black", timeout=5.0)
            res_white = await safe_recv(ws_white, label="post-mov-white", timeout=5.0)

            assert res_black is not None, "Negras no recibio confirmacion"
            assert res_white is not None, "Blancas no recibio actualizacion"
            assert res_black["payload"]["board"] == res_white["payload"]["board"], \
                "Tableros desincronizados tras movimiento"
            ok("Movimiento confirmado y sincronizado en ambos clientes")

            # --- Chat en partida (simula action=chat) ---
            step(6, "Enviando chat en partida (simula action=chat)...")
            chat_msg = "Buena jugada!"
            await ws1.send(json.dumps({"action": "chat", "message": chat_msg}))

            recibido = await wait_for_chat_msg(ws2, label=u2, expected_sender=u1)
            assert recibido is not None, f"'{u2}' no recibio el chat"
            assert recibido["payload"]["message"] == chat_msg, \
                f"Mensaje incorrecto: {recibido['payload']['message']}"
            ok("Chat en partida funciona correctamente")

            # --- Rendicion (simula action=surrender) ---
            step(7, "Enviando rendicion (simula action=surrender)...")
            await ws_black.send(json.dumps({"action": "surrender"}))

            game_over = False
            for _ in range(10):
                estado = await safe_recv(ws_white, label="endgame", timeout=3.0)
                if estado and estado.get("type") == "game_state_update":
                    if estado["payload"].get("game_over"):
                        game_over = True
                        winner = estado["payload"].get("winner")
                        debug(f"game_over=True, winner='{winner}'")
                        break
                if estado is None:
                    break

            assert game_over, "No se recibio game_over tras rendicion"
            ok(f"Rendicion procesada. Ganador: '{winner}'")

        print("\n  ✔ BLOQUE 3 PASADO: WebSocket de partida 1v1 OK")
        return True

    except (AssertionError, Exception) as e:
        print(f"\n  ✘ BLOQUE 3 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 3]")
        if t1: delete_user(t1, u1)
        if t2: delete_user(t2, u2)


# ─────────────────────────────────────────────
#  BLOQUE 4: INVITACIONES PRIVADAS
# ─────────────────────────────────────────────

async def run_invite_test():
    print("\n" + "="*60)
    print("  BLOQUE 4: INVITACIONES PRIVADAS (DUELOS)")
    print("="*60)

    u1 = f"inv_a_{uuid.uuid4().hex[:4]}"
    u2 = f"inv_b_{uuid.uuid4().hex[:4]}"
    t1, t2 = None, None

    try:
        step(1, "Creando usuarios y haciendolos amigos...")
        t1 = create_and_login(u1)
        t2 = create_and_login(u2)
        h1 = {"Authorization": f"Bearer {t1}"}
        h2 = {"Authorization": f"Bearer {t2}"}

        # Hacer amigos
        requests.post(f"{BASE_URL}/api/friends/request", json={"username": u2}, headers=h1)
        panel = requests.get(f"{BASE_URL}/api/friends", headers=h2).json()
        pet = next((r for r in panel.get("requests", [])
                    if (r.get("name") or r.get("username") or r.get("sender_name")) == u1), None)
        assert pet is not None, "No se encontro peticion de amistad"
        requests.post(f"{BASE_URL}/api/friends/{pet['id']}/accept", headers=h2)
        ok("Usuarios son amigos")

        u2_id = get_user_id(t2)

        # --- Conectar WS notificaciones ---
        step(2, "Conectando WS de notificaciones de ambos...")
        async with websockets.connect(f"{WS_URL}/ws/notifications?token={t1}") as notif1, \
                   websockets.connect(f"{WS_URL}/ws/notifications?token={t2}") as notif2:

            # --- Enviar invitacion ---
            step(3, f"'{u1}' invita a '{u2}' a jugar (simula GamesRepository.inviteFriends)...")
            res_inv = requests.post(f"{BASE_URL}/api/games/invite",
                                    json={"friend_ids": [u2_id], "mode": "1vs1"}, headers=h1)
            assert res_inv.status_code == 200, f"HTTP {res_inv.status_code}: {res_inv.text}"
            game_id = res_inv.json().get("game_id")
            assert game_id is not None, f"No se devolvio game_id: {res_inv.json()}"
            ok(f"Invitacion enviada. game_id={game_id}")

            # --- Notificacion al invitado ---
            step(4, f"Esperando notificacion de duelo en WS de '{u2}'...")
            aviso = await safe_recv(notif2, label=u2, timeout=5.0)
            assert aviso is not None, f"'{u2}' no recibio notificacion"
            assert aviso.get("type") == "duel_invite", \
                f"Tipo incorrecto: '{aviso.get('type')}' (esperado 'duel_invite')"
            ok(f"'{u2}' recibio 'duel_invite' correctamente")

            # --- Aceptar ---
            step(5, f"'{u2}' acepta el duelo (simula GamesRepository.acceptGameInvite)...")
            res_acc = requests.post(f"{BASE_URL}/api/games/{game_id}/accept", headers=h2)
            assert res_acc.status_code == 200, f"HTTP {res_acc.status_code}: {res_acc.text}"
            ok("Duelo aceptado")

            # --- Confirmacion al invitador ---
            step(6, "Esperando confirmacion de aceptacion en WS del invitador...")
            confirma = await safe_recv(notif1, label=u1, timeout=5.0)
            assert confirma is not None, f"'{u1}' no recibio confirmacion"
            assert confirma.get("type") == "invite_response", \
                f"Tipo incorrecto: '{confirma.get('type')}'"
            ok("Invitador notificado de la aceptacion")

        print("\n  ✔ BLOQUE 4 PASADO: Invitaciones privadas OK")
        return True

    except (AssertionError, Exception) as e:
        print(f"\n  ✘ BLOQUE 4 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 4]")
        if t1: delete_user(t1, u1)
        if t2: delete_user(t2, u2)


# ─────────────────────────────────────────────
#  BLOQUE 5: PARTIDA VS IA
# ─────────────────────────────────────────────

async def run_vs_ai_test():
    print("\n" + "="*60)
    print("  BLOQUE 5: PARTIDA VS IA DESDE FRONTEND")
    print("="*60)

    u1 = f"ai_player_{uuid.uuid4().hex[:4]}"
    t1 = None

    try:
        step(1, f"Creando usuario '{u1}'...")
        t1 = create_and_login(u1)
        ok("Usuario listo")

        step(2, "Creando sala vs_ai...")
        res = requests.post(f"{BASE_URL}/api/games/create",
                            headers={"Authorization": f"Bearer {t1}"},
                            json={"mode": "vs_ai"})
        assert res.status_code == 200, f"HTTP {res.status_code}: {res.text}"
        game_id = res.json()["game_id"]
        ok(f"Sala IA creada: {game_id}")

        step(3, "Conectando WebSocket...")
        async with websockets.connect(f"{WS_URL}/ws/play/{game_id}?token={t1}") as ws:

            asig = await safe_recv(ws, label="asig-ia")
            assert asig and asig.get("type") == "player_assignment", \
                f"No se recibio player_assignment: {asig}"
            color = asig["payload"]["color"]
            debug(f"Color asignado: {color}")
            ok(f"Asignacion: {color}")

            tablero = await safe_recv(ws, label="tablero-ia")
            assert tablero and tablero.get("type") == "game_state_update", \
                f"No se recibio tablero: {tablero}"
            ok("Tablero inicial recibido")

            step(4, "Enviando movimiento del humano...")
            await ws.send(json.dumps({"action": "make_move", "row": 2, "col": 3}))

            confirmacion = await safe_recv(ws, label="post-human", timeout=5.0)
            assert confirmacion and confirmacion.get("type") == "game_state_update", \
                f"No se confirmo el movimiento: {confirmacion}"
            ok("Movimiento humano confirmado")

            step(5, "Esperando respuesta de la IA...")
            resp_ia = await safe_recv(ws, label="post-ia", timeout=8.0)
            assert resp_ia and resp_ia.get("type") == "game_state_update", \
                f"La IA no respondio: {resp_ia}"
            turno = resp_ia["payload"].get("current_player")
            assert turno == "black", f"Turno no volvio al humano: {turno}"
            ok(f"IA respondio. Turno devuelto al humano. last_move={resp_ia['payload'].get('last_move')}")

        print("\n  ✔ BLOQUE 5 PASADO: Partida vs IA OK")
        return True

    except (AssertionError, Exception) as e:
        print(f"\n  ✘ BLOQUE 5 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 5]")
        if t1: delete_user(t1, u1)


# ─────────────────────────────────────────────
#  RUNNER PRINCIPAL
# ─────────────────────────────────────────────

async def async_main():
    results = {}

    results["Lobby HTTP (crear, listar, unir)"]   = run_lobby_http_test()
    results["Ready y Leave"]                       = run_ready_leave_test()
    results["WebSocket partida 1v1"]               = await run_websocket_game_test()
    results["Invitaciones privadas (duelos)"]      = await run_invite_test()
    results["Partida vs IA"]                       = await run_vs_ai_test()

    print("\n" + "#"*60)
    print("  RESUMEN FINAL")
    print("#"*60)
    passed = 0
    for nombre, ok_val in results.items():
        estado = "✔ PASS" if ok_val else "✘ FAIL"
        print(f"  {estado}  →  {nombre}")
        if ok_val:
            passed += 1

    total = len(results)
    print(f"\n  Resultado: {passed}/{total} bloques pasados")
    print("#"*60 + "\n")

    if passed < total:
        sys.exit(1)
    else:
        sys.exit(0)


def main():
    print("\n" + "#"*60)
    print("  TEST SUITE FRONTEND-MOVIL: PARTIDAS Y WEBSOCKET")
    print("#"*60)
    asyncio.run(async_main())


if __name__ == "__main__":
    main()
