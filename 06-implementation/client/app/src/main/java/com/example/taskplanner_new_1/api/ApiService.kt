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

// ── TaskList (Folder) DTOs ────────────────────────────────────────────────────

/** Body sent when creating or updating a TaskList (folder) on the server. */
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

// ── Task DTOs ─────────────────────────────────────────────────────────────────

/** Body sent when creating or updating a Task on the server. */
data class TaskRequest(
    val description: String,            // maps to local Task.title
    val status: String = "PENDING",     // PENDING | COMPLETED | OVERDUE
    val priority: String = "MEDIUM",    // LOW | MEDIUM | HIGH
    val orderIndex: Int = 0
)

/** Fields we care about from the server's Task response. */
data class TaskResponse(
    val id: Long = -1L,
    val description: String = "",
    val status: String = "PENDING",
    val priority: String = "MEDIUM",
    val orderIndex: Int = 0
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

    // TaskLists = Folders on the server
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

    // Tasks = individual tasks inside a folder
    @GET("api/tasks/tasklist/{taskListId}")
    suspend fun getTasksForFolder(@Path("taskListId") taskListId: Long): Response<List<TaskResponse>>

    @POST("api/tasks/tasklist/{taskListId}")
    suspend fun createServerTask(
        @Path("taskListId") taskListId: Long,
        @Body request: TaskRequest
    ): Response<TaskResponse>

    @PUT("api/tasks/{id}")
    suspend fun updateServerTask(
        @Path("id") id: Long,
        @Body request: TaskRequest
    ): Response<TaskResponse>

    @DELETE("api/tasks/{id}")
    suspend fun deleteServerTask(@Path("id") id: Long): Response<Void>
}
