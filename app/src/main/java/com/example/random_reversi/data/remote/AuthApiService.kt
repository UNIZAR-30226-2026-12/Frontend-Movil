package com.example.random_reversi.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// ── Auth Models ──

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class RegisterResponse(
    val id: Int,
    val username: String,
    val email: String,
    val elo: Int,
    val avatar_url: String?,
    val preferred_piece_color: String,
    val preferred_board_color: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val new_password: String
)

data class MessageResponse(
    val message: String
)

// ── User / Profile Models ──

data class UserMeResponse(
    val id: Int,
    val username: String,
    val email: String,
    val elo: Int,
    val avatar_url: String?,
    val preferred_piece_color: String?,
    val preferred_board_color: String?
)

data class CustomizationUpdateRequest(
    val avatar_url: String? = null,
    val preferred_piece_color: String? = null,
    val preferred_board_color: String? = null
)

data class AvatarUploadResponse(
    val avatar_url: String
)

// ── Stats Models ──

data class UserStatsResponse(
    val total_games: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val winrate: Double,
    val peak_elo: Int,
    val win_streak: Int,
    val winrate_black: Double,
    val winrate_white: Double,
    val nemesis: String?,
    val victim: String?
)

data class HeadToHeadResponse(
    val total_matches: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val total_matches_4p: Int?,
    val first_places_4p: Int?,
    val other_places_4p: Int?
)

// ── History Models ──

data class HistoryEntry(
    val opponent_name: String,
    val result: String,
    val score: String,
    val rankChange: String,
    val mode: String,
    val player_color: String?,
    val date: String?
)

data class AddHistoryRequest(
    val opponent_name: String,
    val result: String,
    val score: String,
    val rankChange: String,
    val mode: String,
    val player_color: String? = null
)

// ── Friends Models ──

data class FriendInfo(
    val id: Int,
    val name: String,
    val rr: Int,
    val avatar_url: String?,
    val unread_count: Int?,
    val status: String?
)

data class GameInviteInfo(
    val game_id: Int,
    val creator: String,
    val mode: String
)

data class SocialPanelResponse(
    val friends: List<FriendInfo>,
    val pending_requests: List<FriendInfo>,
    val game_invitations: List<GameInviteInfo>
)

data class FriendRequestBody(
    val username: String
)

// ── Chat Models ──

data class ChatMessage(
    val id: Int,
    val sender_id: Int,
    val receiver_id: Int,
    val message: String,
    val timestamp: String,
    val is_read: Boolean
)

data class SendMessageBody(
    val message: String
)

// ── Lobby / Games Models ──

data class CreateLobbyRequest(
    val mode: String
)

data class CreateLobbyResponse(
    val game_id: Int,
    val creator: String,
    val mode: String
)

data class PublicLobby(
    val game_id: Int,
    val creator: String,
    val creator_elo: Int,
    val mode: String,
    val players: Int,
    val max_players: Int,
    val status: String
)

data class InviteFriendsRequest(
    val mode: String,
    val friend_ids: List<Int>
)

data class InviteFriendsResponse(
    val game_id: Int,
    val invites_sent: Int
)

data class LobbyPlayerInfo(
    val id: Int,
    val username: String,
    val rr: Int,
    val avatar_url: String?,
    val is_ready: Boolean
)

data class LobbyStateResponse(
    val game_id: Int,
    val status: String,
    val mode: String,
    val players: List<LobbyPlayerInfo>
)

// ── Ranking Models ──

data class RankingEntry(
    val id: Int,
    val username: String,
    val elo: Int,
    val avatar_url: String?
)

// ══════════════════════════════════════════
//  API Service Interface
// ══════════════════════════════════════════

interface AuthApiService {

    // ── Auth ──

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>

    // ── User Profile ──

    @GET("api/users/me")
    suspend fun getMe(): Response<UserMeResponse>

    @PUT("api/users/customization")
    suspend fun updateCustomization(
        @Body request: CustomizationUpdateRequest
    ): Response<UserMeResponse>

    @Multipart
    @POST("api/users/avatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part
    ): Response<AvatarUploadResponse>

    // ── Stats ──

    @GET("api/users/me/stats")
    suspend fun getMyStats(): Response<UserStatsResponse>

    @GET("api/users/{user_id}/stats")
    suspend fun getUserStats(@Path("user_id") userId: Int): Response<UserStatsResponse>

    @GET("api/users/{user_id}/h2h")
    suspend fun getHeadToHead(@Path("user_id") userId: Int): Response<HeadToHeadResponse>

    // ── History ──

    @GET("api/users/me/history")
    suspend fun getMyHistory(): Response<List<HistoryEntry>>

    @POST("api/users/me/history")
    suspend fun addHistory(@Body request: AddHistoryRequest): Response<MessageResponse>

    @GET("api/users/{user_id}/history")
    suspend fun getUserHistory(@Path("user_id") userId: Int): Response<List<HistoryEntry>>

    // ── Friends / Social ──

    @GET("api/friends")
    suspend fun getSocialPanel(): Response<SocialPanelResponse>

    @POST("api/friends/request")
    suspend fun sendFriendRequest(@Body request: FriendRequestBody): Response<MessageResponse>

    @POST("api/friends/{user_id}/accept")
    suspend fun acceptFriendRequest(@Path("user_id") userId: Int): Response<MessageResponse>

    @POST("api/friends/{user_id}/reject")
    suspend fun rejectFriendRequest(@Path("user_id") userId: Int): Response<MessageResponse>

    @DELETE("api/friends/{user_id}")
    suspend fun removeFriend(@Path("user_id") userId: Int): Response<MessageResponse>

    // ── Friend Chat ──

    @GET("api/friends/{user_id}/chat")
    suspend fun getChatHistory(@Path("user_id") userId: Int): Response<List<ChatMessage>>

    @POST("api/friends/{user_id}/chat")
    suspend fun sendChatMessage(
        @Path("user_id") userId: Int,
        @Body body: SendMessageBody
    ): Response<ChatMessage>

    @POST("api/friends/{user_id}/chat/read")
    suspend fun markChatRead(@Path("user_id") userId: Int): Response<MessageResponse>

    // ── Lobby / Games ──

    @POST("api/games/create")
    suspend fun createLobby(@Body request: CreateLobbyRequest): Response<CreateLobbyResponse>

    @GET("api/games/public")
    suspend fun getPublicLobbies(): Response<List<PublicLobby>>

    @POST("api/games/join/{game_id}")
    suspend fun joinLobby(@Path("game_id") gameId: Int): Response<MessageResponse>

    @POST("api/games/invite")
    suspend fun inviteFriends(@Body request: InviteFriendsRequest): Response<InviteFriendsResponse>

    @POST("api/games/{game_id}/accept")
    suspend fun acceptGameInvite(@Path("game_id") gameId: Int): Response<MessageResponse>

    @POST("api/games/{game_id}/reject")
    suspend fun rejectGameInvite(@Path("game_id") gameId: Int): Response<MessageResponse>

    @GET("api/games/{game_id}/state")
    suspend fun getLobbyState(@Path("game_id") gameId: Int): Response<LobbyStateResponse>

    @POST("api/games/{game_id}/ready")
    suspend fun setReady(@Path("game_id") gameId: Int): Response<MessageResponse>

    @POST("api/games/{game_id}/leave")
    suspend fun leaveLobby(@Path("game_id") gameId: Int): Response<MessageResponse>

    // ── Ranking ──

    @GET("api/ranking/")
    suspend fun getRanking(): Response<List<RankingEntry>>
}
