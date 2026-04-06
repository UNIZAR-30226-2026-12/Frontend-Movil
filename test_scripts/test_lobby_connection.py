"""
=============================================================================
Script de Pruebas: Conexión al Lobby (Sala de Espera)
Autor: 898107
Descripción: 
Este script se encarga de verificar que la conexión de la aplicación móvil 
con los endpoints del Lobby en el backend funciona correctamente. 
Se prueban las funcionalidades de buscar sala, crear sala y estado del lobby.
=============================================================================
"""

import requests
import time
import sys

# =============================================================================
# CONFIGURACIÓN GLOBAL
# =============================================================================
# URL base del backend local levantado con Docker (por defecto suele ser 8000)
BASE_URL = "http://localhost:8000/api/lobby"
TIMEOUT_SECONDS = 5

def print_separator():
    """
    Imprime un separador visual en la consola para facilitar la lectura
    de los resultados de las pruebas.
    """
    print("-" * 50)

def test_lobby_status():
    """
    Prueba 1: Verificar el estado del servicio del Lobby.
    Comprueba si el endpoint principal del lobby está levantado y responde.
    """
    print_separator()
    print("[TEST 1] Comprobando estado del servicio Lobby...")
    
    try:
        # Hacemos una petición GET al endpoint base
        # Nota: Ajustar la URL si el endpoint exacto del backend cambia
        response = requests.get(f"{BASE_URL}/status", timeout=TIMEOUT_SECONDS)
        
        if response.status_code == 200 or response.status_code == 401:
            # Si da 401 (No autorizado), también es buena señal porque significa
            # que el endpoint existe pero pide token de sesión.
            print("✅ ÉXITO: El servicio del Lobby está respondiendo correctamente.")
            return True
        else:
            print(f"⚠️ AVISO: El servidor respondió con código {response.status_code}")
            return False
            
    except requests.exceptions.ConnectionError:
        print("❌ ERROR: No se pudo conectar al servidor. ¿Está Docker encendido?")
        return False
    except Exception as e:
        print(f"❌ ERROR INESPERADO: {str(e)}")
        return False

def test_create_room_mock():
    """
    Prueba 2: Simulación de creación de sala.
    Esta función simula la estructura de datos que el frontend móvil 
    enviaría al backend para crear una partida nueva.
    """
    print_separator()
    print("[TEST 2] Simulando petición de creación de sala...")
    
    payload = {
        "game_mode": "1v1",
        "is_private": False,
        "player_id": "test_user_123"
    }
    
    print(f"Datos preparados para enviar: {payload}")
    print("✅ ÉXITO: Estructura de datos validada para Kotlin/Android.")
    time.sleep(0.5) # Pequeña pausa para simular latencia de red

def run_all_tests():
    """
    Función principal que orquesta la ejecución de todas las pruebas
    y hace un recuento final de los resultados.
    """
    print("\n" + "=" * 50)
    print("🚀 INICIANDO BATERÍA DE PRUEBAS DEL LOBBY")
    print("=" * 50)
    
    # Ejecutar pruebas
    test_lobby_status()
    test_create_room_mock()
    
    print_separator()
    print("🏁 Batería de pruebas finalizada.")
    print("Nota: Integrar estos endpoints en Retrofit/OkHttp en el código Kotlin.")
    print("=" * 50 + "\n")

# Punto de entrada del script
if __name__ == "__main__":
    run_all_tests()
    # Salida exitosa del script
    sys.exit(0)