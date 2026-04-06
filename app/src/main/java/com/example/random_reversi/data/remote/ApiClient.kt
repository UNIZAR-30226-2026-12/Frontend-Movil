/**
 * *********************************************************************************
 * MÓDULO DE COMUNICACIÓN: API CLIENT CONFIGURATION
 * DESARROLLADOR: 898107
 * *********************************************************************************
 * DETALLES TÉCNICOS DE IMPLEMENTACIÓN:
 * - Framework base: Retrofit 2.9.0 para la gestión de peticiones REST.
 * - Serialización: GsonConverterFactory para la transformación de JSON a objetos Kotlin.
 * - Cliente HTTP: OkHttpClient con interceptores de logging para depuración en desarrollo.
 * * SEGURIDAD Y PROTOCOLOS:
 * 1. Gestión de Timeouts: Se han configurado 30 segundos para lectura y conexión.
 * 2. Base URL: Apunta al entorno local de Docker (10.0.2.2 para el emulador de Android).
 * 3. Interceptores: Capacidad de añadir Tokens JWT en las cabeceras de futuras peticiones.
 * *********************************************************************************
 */


/**
 * Singleton ApiClient
 * * Este objeto garantiza una única instancia de la configuración de red en toda la app.
 * Evita fugas de memoria y asegura que todos los servicios compartan el mismo pool 
 * de conexiones HTTP, optimizando el rendimiento de la batería del dispositivo.
 */

package com.example.random_reversi.data.remote

import com.example.random_reversi.BuildConfig
import com.example.random_reversi.data.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val token = SessionManager.getToken()
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
}
