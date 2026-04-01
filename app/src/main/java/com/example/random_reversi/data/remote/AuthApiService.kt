package com.example.random_reversi.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

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

interface AuthApiService {
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
}
