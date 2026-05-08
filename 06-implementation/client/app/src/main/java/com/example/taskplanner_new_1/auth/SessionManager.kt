package com.example.taskplanner_new_1.auth

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME    = "taskplanner_session"
        private const val KEY_TOKEN     = "jwt_token"
        private const val KEY_USER_ID   = "user_id"
        private const val KEY_USERNAME  = "username"
        private const val KEY_EMAIL     = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_ROLE      = "role"
    }

    fun saveSession(token: String, userId: Long, username: String, email: String = "") {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun getToken(): String?    = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): Long      = prefs.getLong(KEY_USER_ID, -1L)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getEmail(): String?    = prefs.getString(KEY_EMAIL, null)

    fun saveFullName(fullName: String) =
        prefs.edit().putString(KEY_FULL_NAME, fullName).apply()
    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)

    fun saveCreatedAt(createdAt: String) =
        prefs.edit().putString(KEY_CREATED_AT, createdAt).apply()
    fun getCreatedAt(): String? = prefs.getString(KEY_CREATED_AT, null)

    fun saveRole(role: String) =
        prefs.edit().putString(KEY_ROLE, role).apply()
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
