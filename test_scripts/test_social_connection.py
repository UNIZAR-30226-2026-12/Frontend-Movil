"""
TEST SUITE: CONEXION FRONTEND-MOVIL → BACKEND (SISTEMA SOCIAL)
===============================================================
Verifica que los endpoints de amigos y chat directo usados por el frontend
movil (FriendsRepository.kt, AuthApiService.kt) funcionan correctamente.

Ambitos cubiertos:
  1. Enviar peticion de amistad (POST /api/friends/request)
  2. Listar panel social: amigos, peticiones, invitaciones (GET /api/friends)
  3. Aceptar peticion (POST /api/friends/{id}/accept)
  4. Rechazar peticion (POST /api/friends/{id}/reject)
  5. Eliminar amigo (DELETE /api/friends/{id})
  6. Chat directo: enviar, leer, marcar como leido
  7. Peticiones duplicadas y auto-amistad (edge cases)
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
    res_log = requests.post(f"{BASE_URL}/api/auth/login", data={
        "username": username, "password": password
    })
    assert res_log.status_code == 200, \
        f"[ERROR] Fallo al loguear '{username}': HTTP {res_log.status_code} → {res_log.text}"
    return res_log.json()["access_token"]

def delete_user(token, username):
    res = requests.delete(f"{BASE_URL}/api/users/me",
                          headers={"Authorization": f"Bearer {token}"})
    if res.status_code == 200:
        print(f"   [Limpieza] '{username}' eliminado correctamente.")
    else:
        print(f"   [Limpieza] ATENCION — No se pudo eliminar '{username}': {res.text}")

def get_user_id(token):
    return requests.get(f"{BASE_URL}/api/users/me",
                        headers={"Authorization": f"Bearer {token}"}).json()["id"]


# ─────────────────────────────────────────────
#  BLOQUE 1: FLUJO COMPLETO DE AMISTAD
# ─────────────────────────────────────────────

def run_friendship_flow_test():
    print("\n" + "="*60)
    print("  BLOQUE 1: FLUJO COMPLETO DE AMISTAD")
    print("="*60)

    u1 = f"sender_{uuid.uuid4().hex[:4]}"
    u2 = f"receiver_{uuid.uuid4().hex[:4]}"
    t1, t2 = None, None

    try:
        step(1, f"Creando '{u1}' (envia peticion) y '{u2}' (recibe peticion)...")
        t1 = create_and_login(u1)
        t2 = create_and_login(u2)
        h1 = {"Authorization": f"Bearer {t1}"}
        h2 = {"Authorization": f"Bearer {t2}"}
        ok("Ambos usuarios creados")

        # --- Enviar peticion ---
        step(2, f"'{u1}' envia peticion de amistad a '{u2}'...")
        res_req = requests.post(f"{BASE_URL}/api/friends/request",
                                json={"username": u2}, headers=h1)
        assert res_req.status_code == 200, f"HTTP {res_req.status_code}: {res_req.text}"
        debug(f"Respuesta: {res_req.json()}")
        ok(f"Peticion enviada: {res_req.json().get('message')}")

        # --- Verificar panel social de u2 ---
        step(3, f"'{u2}' consulta su panel social (GET /api/friends) — simula SocialPanelResponse...")
        res_panel = requests.get(f"{BASE_URL}/api/friends", headers=h2)
        assert res_panel.status_code == 200, f"HTTP {res_panel.status_code}: {res_panel.text}"
        panel = res_panel.json()
        # El frontend espera: friends, requests, game_invitations
        for campo in ["friends", "requests"]:
            assert campo in panel, f"Falta '{campo}' en SocialPanelResponse: {list(panel.keys())}"
        debug(f"Panel social de '{u2}': {panel}")

        # Buscar la peticion de u1
        peticion = None
        for r in panel.get("requests", []):
            nombre = r.get("name") or r.get("username") or r.get("sender_name")
            if nombre == u1:
                peticion = r
                break
        assert peticion is not None, \
            f"Peticion de '{u1}' no encontrada en requests: {panel.get('requests')}"
        u1_id = peticion["id"]
        ok(f"Peticion de '{u1}' visible en el panel de '{u2}' (id={u1_id})")

        # --- Aceptar peticion ---
        step(4, f"'{u2}' acepta la peticion (POST /api/friends/{u1_id}/accept)...")
        res_acc = requests.post(f"{BASE_URL}/api/friends/{u1_id}/accept", headers=h2)
        assert res_acc.status_code == 200, f"HTTP {res_acc.status_code}: {res_acc.text}"
        ok(f"Amistad aceptada: {res_acc.json().get('message')}")

        # --- Verificar bidireccionalidad ---
        step(5, "Verificando que ambos se ven como amigos mutuamente...")
        lista_u1 = requests.get(f"{BASE_URL}/api/friends", headers=h1).json()
        lista_u2 = requests.get(f"{BASE_URL}/api/friends", headers=h2).json()

        en_u1 = any((f.get("name") or f.get("username")) == u2
                    for f in lista_u1.get("friends", []))
        en_u2 = any((f.get("name") or f.get("username")) == u1
                    for f in lista_u2.get("friends", []))
        assert en_u1, f"'{u2}' no aparece en la lista de amigos de '{u1}'"
        assert en_u2, f"'{u1}' no aparece en la lista de amigos de '{u2}'"
        ok("Relacion bidireccional confirmada")

        # --- Eliminar amigo ---
        step(6, f"'{u1}' elimina a '{u2}' (DELETE /api/friends/{get_user_id(t2)})...")
        u2_id = get_user_id(t2)
        res_del = requests.delete(f"{BASE_URL}/api/friends/{u2_id}", headers=h1)
        assert res_del.status_code == 200, f"HTTP {res_del.status_code}: {res_del.text}"
        ok(f"Amigo eliminado: {res_del.json().get('message')}")

        # --- Verificar eliminacion ---
        step(7, "Verificando que ya no son amigos...")
        final_u1 = requests.get(f"{BASE_URL}/api/friends", headers=h1).json()
        sigue = any((f.get("name") or f.get("username")) == u2
                    for f in final_u1.get("friends", []))
        assert not sigue, f"'{u2}' sigue en la lista de '{u1}' tras eliminarlo"
        ok("Eliminacion confirmada")

        print("\n  ✔ BLOQUE 1 PASADO: Flujo de amistad OK")
        return True

    except AssertionError as e:
        print(f"\n  ✘ BLOQUE 1 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 1]")
        if t1: delete_user(t1, u1)
        if t2: delete_user(t2, u2)


# ─────────────────────────────────────────────
#  BLOQUE 2: RECHAZAR PETICION Y EDGE CASES
# ─────────────────────────────────────────────

def run_reject_and_edge_cases_test():
    print("\n" + "="*60)
    print("  BLOQUE 2: RECHAZAR PETICION Y EDGE CASES")
    print("="*60)

    u1 = f"edge_a_{uuid.uuid4().hex[:4]}"
    u2 = f"edge_b_{uuid.uuid4().hex[:4]}"
    t1, t2 = None, None

    try:
        step(1, f"Creando usuarios '{u1}' y '{u2}'...")
        t1 = create_and_login(u1)
        t2 = create_and_login(u2)
        h1 = {"Authorization": f"Bearer {t1}"}
        h2 = {"Authorization": f"Bearer {t2}"}
        ok("Usuarios creados")

        # --- Peticion duplicada ---
        step(2, f"'{u1}' envia peticion a '{u2}'...")
        requests.post(f"{BASE_URL}/api/friends/request", json={"username": u2}, headers=h1)
        ok("Primera peticion enviada")

        step(3, "Intentando enviar peticion duplicada...")
        res_dup = requests.post(f"{BASE_URL}/api/friends/request",
                                json={"username": u2}, headers=h1)
        assert res_dup.status_code in (400, 409), \
            f"Peticion duplicada aceptada: HTTP {res_dup.status_code}"
        ok(f"Duplicada rechazada: HTTP {res_dup.status_code}")

        # --- Auto-amistad ---
        step(4, f"'{u1}' intenta enviarse peticion a si mismo...")
        res_self = requests.post(f"{BASE_URL}/api/friends/request",
                                 json={"username": u1}, headers=h1)
        assert res_self.status_code in (400, 409, 422), \
            f"Auto-amistad permitida: HTTP {res_self.status_code}"
        ok(f"Auto-amistad bloqueada: HTTP {res_self.status_code}")

        # --- Rechazar peticion ---
        step(5, f"'{u2}' rechaza la peticion de '{u1}'...")
        panel = requests.get(f"{BASE_URL}/api/friends", headers=h2).json()
        peticion = None
        for r in panel.get("requests", []):
            nombre = r.get("name") or r.get("username") or r.get("sender_name")
            if nombre == u1:
                peticion = r
                break
        assert peticion is not None, "No se encontro la peticion pendiente"
        u1_id = peticion["id"]

        res_rej = requests.post(f"{BASE_URL}/api/friends/{u1_id}/reject", headers=h2)
        assert res_rej.status_code == 200, f"HTTP {res_rej.status_code}: {res_rej.text}"
        ok(f"Peticion rechazada: {res_rej.json().get('message')}")

        # --- Verificar que no son amigos ---
        step(6, "Verificando que no se creó amistad tras el rechazo...")
        panel2 = requests.get(f"{BASE_URL}/api/friends", headers=h2).json()
        no_amigos = not any((f.get("name") or f.get("username")) == u1
                           for f in panel2.get("friends", []))
        assert no_amigos, "Se creo amistad a pesar del rechazo"
        ok("No hay amistad tras rechazo")

        # --- Peticion a usuario inexistente ---
        step(7, "Enviando peticion a usuario que no existe...")
        res_ghost = requests.post(f"{BASE_URL}/api/friends/request",
                                  json={"username": "fantasma_inexistente_xyz"}, headers=h1)
        assert res_ghost.status_code in (400, 404, 422), \
            f"Peticion a fantasma aceptada: HTTP {res_ghost.status_code}"
        ok(f"Peticion a usuario inexistente rechazada: HTTP {res_ghost.status_code}")

        print("\n  ✔ BLOQUE 2 PASADO: Rechazo y edge cases OK")
        return True

    except AssertionError as e:
        print(f"\n  ✘ BLOQUE 2 FALLIDO: {e}")
        return False

    finally:
        print("\n  [Teardown Bloque 2]")
        if t1: delete_user(t1, u1)
        if t2: delete_user(t2, u2)


# ─────────────────────────────────────────────
#  BLOQUE 3: CHAT DIRECTO ENTRE AMIGOS
# ─────────────────────────────────────────────

def run_direct_chat_test():
    print("\n" + "="*60)
    print("  BLOQUE 3: CHAT DIRECTO ENTRE AMIGOS (HTTP)")
    print("="*60)

    u1 = f"chat_a_{uuid.uuid4().hex[:4]}"
    u2 = f"chat_b_{uuid.uuid4().hex[:4]}"
    t1, t2 = None, None

    try:
        step(1, "Creando dos usuarios y haciendolos amigos...")
        t1 = create_and_login(u1)
        t2 = create_and_login(u2)
        h1 = {"Authorization": f"Bearer {t1}"}
        h2 = {"Authorization": f"Bearer {t2}"}

        # Hacer amigos
        requests.post(f"{BASE_URL}/api/friends/request", json={"username": u2}, headers=h1)
        panel = requests.get(f"{BASE_URL}/api/friends", headers=h2).json()
        peticion = next((r for r in panel.get("requests", [])
                        if (r.get("name") or r.get("username") or r.get("sender_name")) == u1), None)
        assert peticion is not None, "No se encontro peticion para hacer amigos"
        requests.post(f"{BASE_URL}/api/friends/{peticion['id']}/accept", headers=h2)
        ok("Usuarios son amigos")

        u2_id = get_user_id(t2)
        u1_id = get_user_id(t1)

        # --- Enviar mensaje ---
        step(2, f"'{u1}' envia mensaje a '{u2}' (POST /api/friends/{u2_id}/chat)...")
        msg_text = "Hola desde el test del frontend movil!"
        res_send = requests.post(f"{BASE_URL}/api/friends/{u2_id}/chat",
                                 json={"message": msg_text}, headers=h1)
        assert res_send.status_code == 200, f"HTTP {res_send.status_code}: {res_send.text}"
        msg_data = res_send.json()
        debug(f"Mensaje enviado: {msg_data}")
        ok("Mensaje enviado correctamente")

        # --- Leer historial ---
        step(3, f"'{u2}' lee el historial de chat (GET /api/friends/{u1_id}/chat)...")
        res_hist = requests.get(f"{BASE_URL}/api/friends/{u1_id}/chat", headers=h2)
        assert res_hist.status_code == 200, f"HTTP {res_hist.status_code}: {res_hist.text}"
        chat = res_hist.json()
        assert isinstance(chat, list), f"Chat no es lista: {type(chat)}"
        assert len(chat) > 0, "El historial de chat esta vacio"
        ultimo = chat[-1]
        assert ultimo.get("message") == msg_text, \
            f"Mensaje incorrecto: esperado='{msg_text}' recibido='{ultimo.get('message')}'"
        debug(f"Ultimo mensaje: {ultimo}")
        ok(f"Historial contiene el mensaje correcto ({len(chat)} mensajes)")

        # --- Responder ---
        step(4, f"'{u2}' responde a '{u1}'...")
        msg_resp = "Recibido! Test funcionando perfectamente."
        res_resp = requests.post(f"{BASE_URL}/api/friends/{u1_id}/chat",
                                 json={"message": msg_resp}, headers=h2)
        assert res_resp.status_code == 200, f"HTTP {res_resp.status_code}: {res_resp.text}"
        ok("Respuesta enviada")

        # --- Verificar ambos mensajes ---
        step(5, "Verificando que ambos mensajes estan en el historial...")
        chat2 = requests.get(f"{BASE_URL}/api/friends/{u2_id}/chat", headers=h1).json()
        mensajes = [m.get("message") for m in chat2]
        assert msg_text in mensajes, f"Primer mensaje no encontrado en historial"
        assert msg_resp in mensajes, f"Respuesta no encontrada en historial"
        ok(f"Ambos mensajes presentes ({len(chat2)} total)")

        # --- Marcar como leido ---
        step(6, f"'{u2}' marca los mensajes como leidos (POST /api/friends/{u1_id}/chat/read)...")
        res_read = requests.post(f"{BASE_URL}/api/friends/{u1_id}/chat/read", headers=h2)
        assert res_read.status_code == 200, f"HTTP {res_read.status_code}: {res_read.text}"
        ok("Mensajes marcados como leidos")

        print("\n  ✔ BLOQUE 3 PASADO: Chat directo OK")
        return True

    except AssertionError as e:
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
    print("  TEST SUITE FRONTEND-MOVIL: SISTEMA SOCIAL")
    print("#"*60)

    results = {
        "Flujo completo de amistad"     : run_friendship_flow_test(),
        "Rechazo y edge cases"          : run_reject_and_edge_cases_test(),
        "Chat directo entre amigos"     : run_direct_chat_test(),
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
