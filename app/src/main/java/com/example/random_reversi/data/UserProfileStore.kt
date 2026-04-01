package com.example.random_reversi.data

import com.example.random_reversi.data.remote.UserMeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfileUiState(
    val username: String = "Jugador",
    val avatarUrl: String? = null
)

object UserProfileStore {
    private val _state = MutableStateFlow(UserProfileUiState())
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    fun setFromUser(user: UserMeResponse) {
        _state.value = UserProfileUiState(
            username = user.username.ifBlank { "Jugador" },
            avatarUrl = user.avatar_url
        )
    }

    suspend fun refreshFromBackend() {
        when (val result = UserRepository.getMe()) {
            is UserResult.Success -> setFromUser(result.data)
            is UserResult.Error -> Unit
        }
    }

    fun setAvatar(avatarUrl: String?) {
        _state.value = _state.value.copy(avatarUrl = avatarUrl)
    }

    fun clear() {
        _state.value = UserProfileUiState()
    }
}
