package com.example.taskplanner_new_1.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// ── Auth DTOs ────────────────────────────────────────────────────────────────

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String? = null
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class JwtResponse(
    val token: String,
    val type: String = "Bearer",
    val id: Long,
    val username: String,
    val email: String,
    val role: String
)

// ── User profile DTO ─────────────────────────────────────────────────────────

data class UserProfileResponse(
    val id: Long            = -1L,
    val username: String    = "",
    val email: String       = "",
    val fullName: String?   = null,
    val createdAt: String   = "",   // "dd.MM.yyyy HH:mm"
    val taskListsCount: Long = 0L,
    val role: String        = ""
)

data class UpdateProfileRequest(val fullName: String)
data class ChangePasswordRequest(val newPassword: String)

// ── TaskList DTOs ─────────────────────────────────────────────────────────────

/** Body sent when creating or updating a TaskList on the server. */
data class TaskListRequest(
    val name: String,
    val targetDate: String,     // "yyyy-MM-dd"
    val status: String = "ACTIVE"
)

/** Fields we care about from the server's TaskList response. */
data class TaskListResponse(
    val id: Long = -1L,
    val name: String = "",
    val targetDate: String = "",
    val status: String = ""
)

// ── API interface ─────────────────────────────────────────────────────────────

interface ApiService {

    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Void>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<JwtResponse>

    // User profile
    @GET("api/users/me")
    suspend fun getProfile(): Response<UserProfileResponse>

    @PATCH("api/users/me")
    suspend fun updateMyProfile(@Body request: UpdateProfileRequest): Response<Void>

    @PATCH("api/users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Void>

    // TaskLists (maps to Android "Task / task group")
    @GET("api/tasklists")
    suspend fun getTaskLists(): Response<List<TaskListResponse>>

    @POST("api/tasklists")
    suspend fun createTaskList(@Body request: TaskListRequest): Response<TaskListResponse>

    @PUT("api/tasklists/{id}")
    suspend fun updateTaskList(
        @Path("id") id: Long,
        @Body request: TaskListRequest
    ): Response<TaskListResponse>

    @DELETE("api/tasklists/{id}")
    suspend fun deleteTaskList(@Path("id") id: Long): Response<Void>
}
