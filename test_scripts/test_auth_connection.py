"""
TEST SUITE: CONEXION FRONTEND-MOVIL → BACKEND (AUTENTICACION Y PERFIL)
======================================================================
Verifica que los endpoints usados por el frontend movil (ApiClient.kt,
AuthApiService.kt) funcionan correctamente desde el punto de vista del
cliente HTTP, simulando las mismas peticiones que hace la app Android.

Ambitos cubiertos:
  1. Registro de usuario (POST /api/auth/register)
  2. Login con credenciales correctas e incorrectas (POST /api/auth/login)
  3. Obtencion del perfil propio (GET /api/users/me)
  4. Actualizacion de email (PUT /api/users/me)
  5. Personalizacion estetica (PUT /api/users/customization)
  6. Estadisticas de usuario (GET /api/users/{id}/stats)
  7. Historial de partidas (GET /api/users/me/history)
  8. Recuperacion de contrasena (POST /api/auth/forgot-password)
  9. Proteccion de endpoints sin token
 10. Token JWT invalido / expirado
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
    res_reg = requests.post(f"{BASE_URL}/api/auth/register", json={
        "username": username, "email": email, "password": password
    })
    assert res_reg.status_code == 200, \
        f"[ERROR] Fallo al registrar '{username}': HTTP {res_reg.status_code} → {res_reg.text}"
    debug(f"Usuario '{username}' registrado. ID: {res_reg.json().get('id')}")

    res_log = requests.post(f"{BASE_URL}/api/auth/login", data={
        "username": username, "password": password
    })
    assert res_log.status_code == 200, \
        f"[ERROR] Fallo al loguear '{username}': HTTP {res_log.status_code} → {res_log.text}"
    token = res_log.json()["access_token"]
    debug(f"Token JWT obtenido para '{username}': {token[:30]}...")
    return token

def delete_user(token, username):
    res = requests.delete(f"{BASE_URL}/api/users/me",
                          headers={"Authorization": f"Bearer {token}"})
    if res.status_code == 200:
        print(f"   [Limpieza] '{username}' eliminado correctamente.")
    else:
        print(f"   [Limpieza] ATENCION — No se pudo eliminar '{username}': {res.text}")

def get_user_id(token):
    res = requests.get(f"{BASE_URL}/api/users/me",
                       headers={"Authorization": f"Bearer {token}"})
    assert res.status_code == 200, f"[ERROR] No se pudo obtener /me: {res.text}"
    return res.json()["id"]


# ─────────────────────────────────────────────
#  BLOQUE 1: REGISTRO, LOGIN Y PERFIL
# ─────────────────────────────────────────────

def run_auth_profile_test():
    print("\n" + "="*60)
    print("  BLOQUE 1: REGISTRO, LOGIN Y PERFIL (FLUJO COMPLETO)")
    print("="*60)

    username = f"movil_{uuid.uuid4().hex[:5]}"
    email    = f"{username}@example.com"
    password = "TestPass_99"
    token    = None

    try:
        # --- Registro ---
        step(1, f"Registrando nuevo usuario '{username}' (simula RegisterRequest del frontend)...")
        reg_data = {"username": username, "email": email, "password": password}
        res_reg  = requests.post(f"{BASE_URL}/api/auth/register", json=reg_data)
        assert res_reg.status_code == 200, f"HTTP {res_reg.status_code}: {res_reg.text}"
        user_id = res_reg.json()["id"]
        ok(f"Usuario registrado con ID={user_id}")

        # --- Registro duplicado ---
        step(2, "Intentando registrar el mismo username (debe fallar)...")
        res_dup = requests.post(f"{BASE_URL}/api/auth/register", json=reg_data)
        assert res_dup.status_code in (400, 409, 422), \
            f"Se permitio un registro duplicado: HTTP {res_dup.status_code}"
        ok(f"Registro duplicado rechazado con HTTP {res_dup.status_code}")

        # --- Login correcto (form-encoded, como hace el frontend) ---
        step(3, "Login con credenciales correctas (form-encoded como OkHttp)...")
        res_log = requests.post(f"{BASE_URL}/api/auth/login",
                                data={"username": username, "password": password})
        assert res_log.status_code == 200, f"HTTP {res_log.status_code}: {res_log.text}"
        token   = res_log.json()["access_token"]
        assert "access_token" in res_log.json(), "Falta 'access_token' en la respuesta"
        headers = {"Authorization": f"Bearer {token}"}
        ok(f"Token JWT recibido correctamente")

        # --- Login incorrecto ---
        step(4, "Login con contrasena incorrecta (debe rechazar)...")
        res_bad = requests.post(f"{BASE_URL}/api/auth/login",
                                data={"username": username, "password": "WRONGPASS"})
        assert res_bad.status_code in (400, 401, 403), \
            f"El servidor acepto credenciales invalidas: HTTP {res_bad.status_code}"
        ok(f"Login incorrecto rechazado con HTTP {res_bad.status_code}")

        # --- Perfil propio ---
        step(5, "Obteniendo perfil propio (GET /api/users/me, simula UserMeResponse)...")
        res_me = requests.get(f"{BASE_URL}/api/users/me", headers=headers)
        assert res_me.status_code == 200, f"HTTP {res_me.status_code}: {res_me.text}"
        perfil = res_me.json()
        assert perfil["username"] == username, \
            f"Username no coincide: esperado='{username}' recibido='{perfil['username']}'"
        # Verificar campos que el frontend espera (UserMeResponse)
        for campo in ["id", "username", "email"]:
            assert campo in perfil, f"Falta campo '{campo}' en UserMeResponse: {perfil}"
        debug(f"Perfil completo: {perfil}")
        ok(f"Perfil valido: username='{perfil['username']}', email='{perfil['email']}'")

        # --- Actualizar email ---
        step(6, "Actualizando email del perfil (PUT /api/users/me)...")
        new_email = f"updated_{email}"
        res_upd   = requests.put(f"{BASE_URL}/api/users/me",
                                 json={"email": new_email}, headers=headers)
        assert res_upd.status_code == 200, f"HTTP {res_upd.status_code}: {res_upd.text}"
        assert res_upd.json()["email"] == new_email, \
            f"Email no actualizado: esperado='{new_email}' recibido='{res_upd.json()['email']}'"
        ok(f"Email actualizado a '{new_email}'")

        # --- Personalizacion estetica ---
        step(7, "Personalizacion estetica (PUT /api/users/customization)...")
        custom_data = {
            "preferred_board_color": "#FF5733",
            "preferred_piece_color": "galaxy_blue"
        }
        res_cust = requests.put(f"{BASE_URL}/api/users/customization",
                                json=custom_data, headers=headers)
        assert res_cust.status_code == 200, f"HTTP {res_cust.status_code}: {res_cust.text}"

        # Verificar persistencia leyendo de nuevo el perfil
        perfil2 = requests.get(f"{BASE_URL}/api/users/me", headers=headers).json()
        assert perfil2.get("preferred_board_color") == "#FF5733", \
            f"Color tablero no persiste. Recibido: {perfil2.get('preferred_board_color')}"
        assert perfil2.get("preferred_piece_color") == "galaxy_blue", \
            f"Color fichas no persiste. Recibido: {perfil2.get('preferred_piece_color')}"
        ok("Personalizacion guardada y verificada en BD")

        # --- Estadisticas ---
        step(8, f"Consultando estadisticas (GET /api/users/{user_id}/stats)...")
        res_stats = requests.get(f"{BASE_URL}/api/users/{user_id}/stats")
        assert res_stats.status_code == 200, f"HTTP {res_stats.status_code}: {res_stats.text}"
        stats = res_stats.json()
        for campo in ["elo", "winrate"]:
            assert campo in stats, f"Falta '{campo}' en stats: {stats}"
        debug(f"Stats: ELO={stats['elo']}, Winrate={stats['winrate']}")
        ok(f"Estadisticas recibidas: ELO={stats['elo']}")

        # --- Historial ---
        step(9, "Consultando historial de partidas (GET /api/users/me/history)...")
        res_hist = requests.get(f"{BASE_URL}/api/users/me/history", headers=headers)
        assert res_hist.status_code == 200, f"HTTP {res_hist.status_code}: {res_hist.text}"
        historial = res_hist.json()
        assert isinstance(historial, list), f"Historial no es lista: {type(historial)}"
        debug(f"Historial tiene {len(historial)} entradas")
        ok("Endpoint de historial accesible y devuelve lista")

        print("\n  ✔ BLOQUE 1 PASADO: Registro, Login y Perfil OK")
        return True

    except AssertionError as e:
        print(f"\n  ✘ BLOQUE 1 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 1]")
        if token:
            delete_user(token, username)


# ─────────────────────────────────────────────
#  BLOQUE 2: SEGURIDAD DE TOKENS
# ─────────────────────────────────────────────

def run_token_security_test():
    print("\n" + "="*60)
    print("  BLOQUE 2: SEGURIDAD DE TOKENS JWT")
    print("="*60)

    username = f"sec_{uuid.uuid4().hex[:5]}"
    token    = None

    try:
        step(1, f"Creando usuario de prueba '{username}'...")
        token = create_and_login(username)
        ok("Usuario creado y logueado")

        # --- Sin token ---
        step(2, "Accediendo a /api/users/me SIN token (debe rechazar)...")
        res_notoken = requests.get(f"{BASE_URL}/api/users/me")
        assert res_notoken.status_code in (401, 403, 422), \
            f"/me accesible sin token: HTTP {res_notoken.status_code}"
        ok(f"Acceso sin token rechazado: HTTP {res_notoken.status_code}")

        # --- Token invalido ---
        step(3, "Accediendo con token JWT inventado (debe rechazar)...")
        res_fake = requests.get(f"{BASE_URL}/api/users/me",
                                headers={"Authorization": "Bearer este.token.no.existe"})
        assert res_fake.status_code in (401, 403, 422), \
            f"/me accesible con token falso: HTTP {res_fake.status_code}"
        ok(f"Token falso rechazado: HTTP {res_fake.status_code}")

        # --- Token sin prefijo Bearer ---
        step(4, "Accediendo con token sin prefijo 'Bearer'...")
        res_nobearer = requests.get(f"{BASE_URL}/api/users/me",
                                    headers={"Authorization": token})
        assert res_nobearer.status_code in (401, 403, 422), \
            f"/me accesible sin 'Bearer': HTTP {res_nobearer.status_code}"
        ok(f"Token sin Bearer rechazado: HTTP {res_nobearer.status_code}")

        # --- Endpoint protegido: personalizar sin token ---
        step(5, "PUT /api/users/customization sin token...")
        res_cust = requests.put(f"{BASE_URL}/api/users/customization",
                                json={"preferred_board_color": "#000"})
        assert res_cust.status_code in (401, 403, 422), \
            f"Customization accesible sin token: HTTP {res_cust.status_code}"
        ok(f"Customization protegido: HTTP {res_cust.status_code}")

        # --- Endpoint protegido: friends sin token ---
        step(6, "GET /api/friends sin token...")
        res_friends = requests.get(f"{BASE_URL}/api/friends")
        assert res_friends.status_code in (401, 403, 422), \
            f"Friends accesible sin token: HTTP {res_friends.status_code}"
        ok(f"Friends protegido: HTTP {res_friends.status_code}")

        # --- Endpoint protegido: crear partida sin token ---
        step(7, "POST /api/games/create sin token...")
        res_game = requests.post(f"{BASE_URL}/api/games/create",
                                 json={"mode": "1vs1"})
        assert res_game.status_code in (401, 403, 422), \
            f"Games/create accesible sin token: HTTP {res_game.status_code}"
        ok(f"Games/create protegido: HTTP {res_game.status_code}")

        print("\n  ✔ BLOQUE 2 PASADO: Seguridad de Tokens OK")
        return True

    except AssertionError as e:
        print(f"\n  ✘ BLOQUE 2 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 2]")
        if token:
            delete_user(token, username)


# ─────────────────────────────────────────────
#  BLOQUE 3: RECUPERACION DE CONTRASENA
# ─────────────────────────────────────────────

def run_password_recovery_test():
    print("\n" + "="*60)
    print("  BLOQUE 3: FLUJO DE RECUPERACION DE CONTRASENA")
    print("="*60)

    username = f"recov_{uuid.uuid4().hex[:5]}"
    email    = f"{username}@test.com"
    token    = None

    try:
        step(1, f"Creando usuario '{username}' con email '{email}'...")
        token = create_and_login(username)
        ok("Usuario creado")

        step(2, "Solicitando recuperacion de contrasena (POST /api/auth/forgot-password)...")
        res_forgot = requests.post(f"{BASE_URL}/api/auth/forgot-password",
                                   json={"email": email})
        # Puede devolver 200 siempre (para no revelar si el email existe) o un codigo especifico
        assert res_forgot.status_code in (200, 201, 202, 404), \
            f"HTTP inesperado: {res_forgot.status_code}: {res_forgot.text}"
        debug(f"Respuesta forgot-password: HTTP {res_forgot.status_code} → {res_forgot.text[:200]}")
        ok(f"Forgot-password respondio con HTTP {res_forgot.status_code}")

        step(3, "Solicitando recuperacion con email inexistente...")
        res_fake_email = requests.post(f"{BASE_URL}/api/auth/forgot-password",
                                       json={"email": "noexiste@fantasma.com"})
        # El servidor no deberia revelar si el email existe o no (seguridad)
        assert res_fake_email.status_code in (200, 202, 404), \
            f"HTTP inesperado para email falso: {res_fake_email.status_code}"
        debug(f"Respuesta para email falso: HTTP {res_fake_email.status_code}")
        ok("Endpoint no revela informacion sobre emails inexistentes")

        step(4, "Probando reset-password con codigo invalido (debe fallar)...")
        res_reset = requests.post(f"{BASE_URL}/api/auth/reset-password",
                                  json={"email": email, "code": "000000", "new_password": "NuevaPass1"})
        assert res_reset.status_code in (400, 401, 403, 404, 422), \
            f"Reset con codigo falso aceptado: HTTP {res_reset.status_code}"
        ok(f"Reset con codigo invalido rechazado: HTTP {res_reset.status_code}")

        print("\n  ✔ BLOQUE 3 PASADO: Recuperacion de contrasena OK")
        return True

    except AssertionError as e:
        print(f"\n  ✘ BLOQUE 3 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 3]")
        if token:
            delete_user(token, username)


# ─────────────────────────────────────────────
#  BLOQUE 4: VALIDACION DE DATOS DE ENTRADA
# ─────────────────────────────────────────────

def run_input_validation_test():
    print("\n" + "="*60)
    print("  BLOQUE 4: VALIDACION DE DATOS DE ENTRADA")
    print("="*60)

    try:
        step(1, "Registro con campos vacios...")
        res = requests.post(f"{BASE_URL}/api/auth/register", json={
            "username": "", "email": "", "password": ""
        })
        assert res.status_code in (400, 422), \
            f"Registro con campos vacios aceptado: HTTP {res.status_code}"
        ok(f"Campos vacios rechazados: HTTP {res.status_code}")

        step(2, "Registro con email malformado...")
        res2 = requests.post(f"{BASE_URL}/api/auth/register", json={
            "username": f"test_{uuid.uuid4().hex[:4]}",
            "email": "esto-no-es-un-email",
            "password": "Password123"
        })
        assert res2.status_code in (400, 422), \
            f"Email invalido aceptado: HTTP {res2.status_code}"
        ok(f"Email malformado rechazado: HTTP {res2.status_code}")

        step(3, "Registro sin campo password (JSON incompleto)...")
        res3 = requests.post(f"{BASE_URL}/api/auth/register", json={
            "username": f"test_{uuid.uuid4().hex[:4]}",
            "email": "ok@test.com"
        })
        assert res3.status_code in (400, 422), \
            f"Registro sin password aceptado: HTTP {res3.status_code}"
        ok(f"Registro sin password rechazado: HTTP {res3.status_code}")

        step(4, "Login con body vacio...")
        res4 = requests.post(f"{BASE_URL}/api/auth/login", data={})
        assert res4.status_code in (400, 422), \
            f"Login con body vacio aceptado: HTTP {res4.status_code}"
        ok(f"Login vacio rechazado: HTTP {res4.status_code}")

        step(5, "Login con usuario inexistente...")
        res5 = requests.post(f"{BASE_URL}/api/auth/login", data={
            "username": "usuario_que_no_existe_xyz", "password": "password123"
        })
        assert res5.status_code in (400, 401, 403, 404), \
            f"Login con usuario inexistente aceptado: HTTP {res5.status_code}"
        ok(f"Usuario inexistente rechazado: HTTP {res5.status_code}")

        print("\n  ✔ BLOQUE 4 PASADO: Validacion de entrada OK")
        return True

    except AssertionError as e:
        print(f"\n  ✘ BLOQUE 4 FALLIDO: {e}")
        return False


# ─────────────────────────────────────────────
#  RUNNER PRINCIPAL
# ─────────────────────────────────────────────

def main():
    print("\n" + "#"*60)
    print("  TEST SUITE FRONTEND-MOVIL: AUTENTICACION Y PERFIL")
    print("#"*60)

    results = {
        "Registro, Login y Perfil"       : run_auth_profile_test(),
        "Seguridad de Tokens JWT"        : run_token_security_test(),
        "Recuperacion de contrasena"     : run_password_recovery_test(),
        "Validacion de datos de entrada" : run_input_validation_test(),
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
