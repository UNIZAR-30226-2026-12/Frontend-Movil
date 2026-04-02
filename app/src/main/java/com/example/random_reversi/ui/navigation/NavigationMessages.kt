package com.example.random_reversi.ui.navigation

object NavigationMessages {
    private var pendingFriendsToast: String? = null

    fun pushFriendsToast(message: String) {
        pendingFriendsToast = message
    }

    fun consumeFriendsToast(): String? {
        val value = pendingFriendsToast
        pendingFriendsToast = null
        return value
    }
}

