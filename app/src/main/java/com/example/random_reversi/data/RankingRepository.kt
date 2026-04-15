package com.example.random_reversi.data

import com.example.random_reversi.data.remote.ApiClient
import com.example.random_reversi.data.remote.RankingEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RankingRepository {

    suspend fun getRanking(): UserResult<List<RankingEntry>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getRanking()
            if (response.isSuccessful) {
                val rankingList = response.body()?.ranking ?: emptyList()
                UserResult.Success(rankingList)
            } else {
                UserResult.Error("Error al cargar ranking (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }
}
