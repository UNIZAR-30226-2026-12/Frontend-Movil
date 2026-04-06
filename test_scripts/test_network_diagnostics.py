
"""
=============================================================================
HERRAMIENTA DE DIAGNÓSTICO DE RED Y CONECTIVIDAD (PING & LATENCY)
=============================================================================
Desarrollador: 898107
Módulo: Frontend-Movil / Infraestructura de Red
-----------------------------------------------------------------------------
Este script realiza pruebas de latencia y disponibilidad sobre los 
endpoints críticos del backend. Es fundamental para garantizar que la 
aplicación móvil no sufra bloqueos (ANR) por culpa de caídas del servidor.
=============================================================================
"""

import time
import requests
import statistics

# Configuración del servidor local (ajustar si el puerto de Docker es diferente)
BASE_URL = "http://localhost:8000/api"
TIMEOUT_SEC = 5

class NetworkDiagnosticsTool:
    def __init__(self):
        self.endpoints_to_test = [
            "/auth/status",
            "/lobby/status",
            "/game/status",
            "/ranking/top"
        ]
        self.latencies = []
        self.errors = 0

    def print_separator(self, char="-", length=60):
        """Imprime un separador para la consola."""
        print(char * length)

    def test_server_ping(self, packets=5):
        """
        Envía varios paquetes de prueba a la raíz del servidor para
        medir el tiempo de respuesta medio (Ping).
        """
        self.print_separator("=")
        print(f" INICIANDO PRUEBA DE PING: {BASE_URL}")
        self.print_separator("=")
        
        for i in range(1, packets + 1):
            start_time = time.time()
            try:
                # Simulamos la petición (puede dar 404 si no hay endpoint base, 
                # pero nos sirve para medir que el servidor escucha)
                res = requests.get(BASE_URL, timeout=TIMEOUT_SEC)
                end_time = time.time()
                
                latency_ms = (end_time - start_time) * 1000
                self.latencies.append(latency_ms)
                
                status = "OK" if res.status_code in [200, 401, 404] else "ERR"
                print(f"Paquete {i}/{packets} | Estado: {status} | Tiempo: {latency_ms:.2f} ms")
                
            except requests.exceptions.RequestException as e:
                self.errors += 1
                print(f"Paquete {i}/{packets} | FALLO DE CONEXIÓN | Error: {type(e).__name__}")
                
            time.sleep(0.5) # Pausa entre pings

    def test_critical_endpoints(self):
        """
        Verifica que las rutas clave de la API están dadas de alta
        en el backend y responden a peticiones HTTP.
        """
        self.print_separator("=")
        print(" VERIFICANDO RUTAS CRÍTICAS (ENDPOINTS)")
        self.print_separator("=")
        
        for endpoint in self.endpoints_to_test:
            url = f"{BASE_URL}{endpoint}"
            try:
                # Hacemos una petición OPTIONS que es más ligera que GET
                res = requests.options(url, timeout=TIMEOUT_SEC)
                
                if res.status_code < 500:
                    print(f"✅ RUTA ACTIVA: {endpoint} (HTTP {res.status_code})")
                else:
                    print(f"⚠️ ERROR SERVIDOR: {endpoint} (HTTP {res.status_code})")
                    self.errors += 1
            except requests.exceptions.RequestException:
                print(f"❌ INACCESIBLE: {endpoint}")
                self.errors += 1

    def generate_report(self):
        """
        Calcula las estadísticas finales de la conexión y 
        determina la calidad de la red.
        """
        self.print_separator("=")
        print(" INFORME DE SALUD DE LA RED (HEALTH CHECK)")
        self.print_separator("=")
        
        if len(self.latencies) > 0:
            avg_ping = statistics.mean(self.latencies)
            min_ping = min(self.latencies)
            max_ping = max(self.latencies)
            
            print(f" -> Ping Mínimo: {min_ping:.2f} ms")
            print(f" -> Ping Máximo: {max_ping:.2f} ms")
            print(f" -> Ping Medio:  {avg_ping:.2f} ms")
            
            if avg_ping < 50:
                calidad = "EXCELENTE (Local/LAN)"
            elif avg_ping < 150:
                calidad = "BUENA (4G/WIFI)"
            else:
                calidad = "POBRE (Latencia Alta)"
                
            print(f" -> Calidad de Conexión: {calidad}")
        else:
            print(" -> Estadísticas: No se pudieron recopilar datos.")
            
        print(f" -> Paquetes perdidos / Errores: {self.errors}")
        self.print_separator("=")
        print("\n")

if __name__ == "__main__":
    # Instanciamos y ejecutamos la herramienta
    diagnostics = NetworkDiagnosticsTool()
    diagnostics.test_server_ping(packets=5)
    diagnostics.test_critical_endpoints()
    diagnostics.generate_report()