"""
TEST SUITE: CONEXION FRONTEND-MOVIL → BACKEND (RANKING Y ESTADISTICAS)
======================================================================
Verifica que los endpoints de ranking y estadisticas usados por el
frontend movil (RankingRepository.kt, AuthApiService.kt) funcionan
correctamente.

Ambitos cubiertos:
  1. Leaderboard global Top 50 (GET /api/ranking/)
  2. Estructura de RankingEntry (username, elo, avatar_url)
  3. Orden descendente por ELO
  4. Stats de usuario propio (GET /api/users/me/stats)
  5. Stats de otro usuario (GET /api/users/{id}/stats)
  6. Head-to-head (GET /api/users/{id}/h2h)
  7. Historial de partidas propio y ajeno
"""

import requests
import uuid
import sys

BASE_URL = "http://localhost:8081"

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


# ─────────────────────────────────────────────
#  BLOQUE 1: LEADERBOARD GLOBAL
# ─────────────────────────────────────────────

def run_ranking_test():
    print("\n" + "="*60)
    print("  BLOQUE 1: LEADERBOARD GLOBAL (TOP 50)")
    print("="*60)

    username = f"rank_{uuid.uuid4().hex[:4]}"
    token = None

    try:
        step(1, f"Creando usuario de referencia '{username}'...")
        token = create_and_login(username)
        ok("Usuario creado")

        # --- Obtener ranking ---
        step(2, "Consultando ranking global (simula RankingRepository.getRanking)...")
        res = requests.get(f"{BASE_URL}/api/ranking/")
        assert res.status_code == 200, f"HTTP {res.status_code}: {res.text}"
        data = res.json()
        assert "ranking" in data, f"Falta clave 'ranking': {list(data.keys())}"
        ranking = data["ranking"]
        assert isinstance(ranking, list), f"'ranking' no es lista: {type(ranking)}"
        assert len(ranking) > 0, "El ranking esta vacio"
        debug(f"Total jugadores en ranking: {len(ranking)}")
        ok(f"Ranking recibido con {len(ranking)} jugadores")

        # --- Validar estructura RankingEntry ---
        step(3, "Validando estructura de cada RankingEntry...")
        for i, player in enumerate(ranking[:5]):
            for campo in ["username", "elo"]:
                assert campo in player, f"Entrada #{i} sin '{campo}': {player}"
        debug(f"Campos disponibles: {list(ranking[0].keys())}")
        ok("Estructura RankingEntry validada (username + elo)")

        # --- Orden descendente ---
        step(4, "Verificando orden descendente por ELO...")
        elos = [p["elo"] for p in ranking]
        assert elos == sorted(elos, reverse=True), \
            f"Ranking NO ordenado DESC: {elos[:10]}..."
        ok("Orden descendente confirmado")

        # --- Max 50 ---
        step(5, "Verificando limite de 50 entradas...")
        assert len(ranking) <= 50, f"Ranking excede 50 entradas: {len(ranking)}"
        ok(f"Ranking tiene {len(ranking)} entradas (max 50)")

        # --- Display top 5 ---
        step(6, "Top 5 para verificacion visual...")
        print()
        print("         ┌─────┬──────────────────┬──────────┐")
        print("         │ Pos │ Username         │ ELO      │")
        print("         ├─────┼──────────────────┼──────────┤")
        for i, p in enumerate(ranking[:5], 1):
            print(f"         │ #{i:<3} │ {p['username']:<16} │ {str(p['elo']):<8} │")
        print("         └─────┴──────────────────┴──────────┘")

        print("\n  ✔ BLOQUE 1 PASADO: Leaderboard global OK")
        return True

    except AssertionError as e:
        print(f"\n  ✘ BLOQUE 1 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 1]")
        if token: delete_user(token, username)


# ─────────────────────────────────────────────
#  BLOQUE 2: ESTADISTICAS DE USUARIO
# ─────────────────────────────────────────────

def run_user_stats_test():
    print("\n" + "="*60)
    print("  BLOQUE 2: ESTADISTICAS DE USUARIO")
    print("="*60)

    u1 = f"stats_a_{uuid.uuid4().hex[:4]}"
    u2 = f"stats_b_{uuid.uuid4().hex[:4]}"
    t1, t2 = None, None

    try:
        step(1, "Creando dos usuarios...")
        t1 = create_and_login(u1)
        t2 = create_and_login(u2)
        id1 = get_user_id(t1)
        id2 = get_user_id(t2)
        ok(f"Usuarios: {u1} (id={id1}), {u2} (id={id2})")

        # --- Stats propias via /me/stats ---
        step(2, "Consultando mis stats (simula GamesRepository.getMyStats)...")
        res_my = requests.get(f"{BASE_URL}/api/users/me/stats",
                              headers={"Authorization": f"Bearer {t1}"})
        assert res_my.status_code == 200, f"HTTP {res_my.status_code}: {res_my.text}"
        my_stats = res_my.json()
        # UserStatsResponse campos esperados por el frontend
        for campo in ["elo", "winrate"]:
            assert campo in my_stats, f"Falta '{campo}' en mis stats: {my_stats}"
        debug(f"Mis stats: {my_stats}")
        ok(f"Stats propias: ELO={my_stats['elo']}, Winrate={my_stats['winrate']}")

        # --- Stats de otro usuario ---
        step(3, f"Consultando stats de otro usuario (GET /api/users/{id2}/stats)...")
        res_other = requests.get(f"{BASE_URL}/api/users/{id2}/stats")
        assert res_other.status_code == 200, f"HTTP {res_other.status_code}: {res_other.text}"
        other_stats = res_other.json()
        assert "elo" in other_stats, f"Falta 'elo' en stats de {u2}: {other_stats}"
        ok(f"Stats de '{u2}': ELO={other_stats['elo']}")

        # --- Head-to-head ---
        step(4, f"Consultando head-to-head (GET /api/users/{id2}/h2h)...")
        res_h2h = requests.get(f"{BASE_URL}/api/users/{id2}/h2h",
                               headers={"Authorization": f"Bearer {t1}"})
        assert res_h2h.status_code == 200, f"HTTP {res_h2h.status_code}: {res_h2h.text}"
        h2h = res_h2h.json()
        debug(f"Head-to-head: {h2h}")
        ok("Endpoint h2h accesible")

        # --- Historial propio ---
        step(5, "Consultando mi historial (simula GamesRepository.getMyHistory)...")
        res_hist = requests.get(f"{BASE_URL}/api/users/me/history",
                                headers={"Authorization": f"Bearer {t1}"})
        assert res_hist.status_code == 200, f"HTTP {res_hist.status_code}: {res_hist.text}"
        historial = res_hist.json()
        assert isinstance(historial, list), f"Historial no es lista: {type(historial)}"
        ok(f"Historial accesible ({len(historial)} entradas)")

        # --- Historial de otro ---
        step(6, f"Consultando historial de otro (GET /api/users/{id2}/history)...")
        res_hist2 = requests.get(f"{BASE_URL}/api/users/{id2}/history",
                                 headers={"Authorization": f"Bearer {t1}"})
        assert res_hist2.status_code == 200, f"HTTP {res_hist2.status_code}: {res_hist2.text}"
        ok("Historial ajeno accesible")

        print("\n  ✔ BLOQUE 2 PASADO: Estadisticas de usuario OK")
        return True

    except AssertionError as e:
        print(f"\n  ✘ BLOQUE 2 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 2]")
        if t1: delete_user(t1, u1)
        if t2: delete_user(t2, u2)


# ─────────────────────────────────────────────
#  BLOQUE 3: COHERENCIA TRAS PARTIDA
# ─────────────────────────────────────────────

def run_post_game_stats_test():
    """
    Crea una partida, la termina por rendicion, y verifica que el ELO,
    historial y stats se actualizan correctamente — tal como el frontend
    los leeria tras la partida.
    """
    import asyncio
    import websockets
    import json

    WS_URL = "ws://localhost:8081"

    print("\n" + "="*60)
    print("  BLOQUE 3: COHERENCIA DE STATS TRAS PARTIDA")
    print("="*60)

    u1 = f"loser_{uuid.uuid4().hex[:4]}"
    u2 = f"winner_{uuid.uuid4().hex[:4]}"
    t1, t2 = None, None

    async def _run():
        nonlocal t1, t2

        step(1, "Creando usuarios y registrando ELO inicial...")
        t1 = create_and_login(u1)
        t2 = create_and_login(u2)
        id1 = get_user_id(t1)
        id2 = get_user_id(t2)

        elo_pre_1 = requests.get(f"{BASE_URL}/api/users/{id1}/stats").json().get("elo", 1000)
        elo_pre_2 = requests.get(f"{BASE_URL}/api/users/{id2}/stats").json().get("elo", 1000)
        debug(f"ELO antes: {u1}={elo_pre_1}, {u2}={elo_pre_2}")
        ok("ELOs iniciales registrados")

        step(2, "Creando sala, conectando y rindiendo...")
        res = requests.post(f"{BASE_URL}/api/games/create",
                            headers={"Authorization": f"Bearer {t1}"}, json={"mode": "1vs1"})
        game_id = res.json()["game_id"]
        requests.post(f"{BASE_URL}/api/games/join/{game_id}",
                      headers={"Authorization": f"Bearer {t2}"})

        async with websockets.connect(f"{WS_URL}/ws/play/{game_id}?token={t1}") as ws1, \
                   websockets.connect(f"{WS_URL}/ws/play/{game_id}?token={t2}") as ws2:
            # Consumir init
            for _ in range(3):
                await asyncio.wait_for(ws1.recv(), timeout=3.0)
                await asyncio.wait_for(ws2.recv(), timeout=3.0)

            await ws1.send(json.dumps({"action": "surrender"}))
            # Esperar game_over
            for _ in range(10):
                try:
                    raw = await asyncio.wait_for(ws2.recv(), timeout=3.0)
                    data = json.loads(raw)
                    if data.get("type") == "game_state_update" and data["payload"].get("game_over"):
                        break
                except:
                    break

        ok("Partida terminada por rendicion")

        step(3, "Esperando propagacion a BD...")
        await asyncio.sleep(1.5)

        step(4, "Verificando cambio de ELO...")
        elo_post_1 = requests.get(f"{BASE_URL}/api/users/{id1}/stats").json().get("elo", 1000)
        elo_post_2 = requests.get(f"{BASE_URL}/api/users/{id2}/stats").json().get("elo", 1000)
        debug(f"ELO despues: {u1}={elo_post_1} (antes={elo_pre_1}), {u2}={elo_post_2} (antes={elo_pre_2})")

        assert elo_post_1 < elo_pre_1, f"ELO del perdedor no bajo: {elo_post_1} >= {elo_pre_1}"
        assert elo_post_2 > elo_pre_2, f"ELO del ganador no subio: {elo_post_2} <= {elo_pre_2}"
        ok(f"ELO actualizado: {u1}={elo_post_1}(↓), {u2}={elo_post_2}(↑)")

        step(5, "Verificando historial del perdedor...")
        hist = requests.get(f"{BASE_URL}/api/users/me/history",
                            headers={"Authorization": f"Bearer {t1}"}).json()
        assert len(hist) > 0, "Historial vacio tras partida"
        debug(f"Historial: {hist[0]}")
        ok(f"Historial registrado ({len(hist)} entradas)")

        return True

    try:
        result = asyncio.run(_run())
        if result:
            print("\n  ✔ BLOQUE 3 PASADO: Coherencia post-partida OK")
        return result

    except (AssertionError, Exception) as e:
        print(f"\n  ✘ BLOQUE 3 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 3]")
        if t1: delete_user(t1, u1)
        if t2: delete_user(t2, u2)


# ─────────────────────────────────────────────
#  RUNNER PRINCIPAL
# ─────────────────────────────────────────────

def main():
    print("\n" + "#"*60)
    print("  TEST SUITE FRONTEND-MOVIL: RANKING Y ESTADISTICAS")
    print("#"*60)

    results = {
        "Leaderboard global (Top 50)"     : run_ranking_test(),
        "Estadisticas de usuario"          : run_user_stats_test(),
        "Coherencia de stats tras partida" : run_post_game_stats_test(),
    }

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


if __name__ == "__main__":
    main()
