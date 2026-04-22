package com.vision.swimsafe.data.remote

import android.content.Context
import android.content.SharedPreferences

object AuthSession {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_INFO = "user_info"

    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var refreshToken: String? = null

    @Volatile
    private var userInfo: UserInfoVo? = null

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        accessToken = prefs?.getString(KEY_ACCESS_TOKEN, null)
        refreshToken = prefs?.getString(KEY_REFRESH_TOKEN, null)
        val userJson = prefs?.getString(KEY_USER_INFO, null)
        userInfo = userJson?.let { parseUserInfo(it) }
    }

    fun isLoggedIn(): Boolean = !accessToken.isNullOrEmpty()

    fun getAccessToken(): String? = accessToken

    fun getRefreshToken(): String? = refreshToken

    fun getUserInfo(): UserInfoVo? = userInfo

    fun updateFromLogin(result: LoginResultVo) {
        accessToken = result.accessToken
        refreshToken = result.refreshToken
        userInfo = result.user
        saveToPrefs(result)
    }

    fun clear() {
        accessToken = null
        refreshToken = null
        userInfo = null
        prefs?.edit()?.clear()?.apply()
    }

    private fun saveToPrefs(result: LoginResultVo) {
        prefs?.edit()?.apply {
            putString(KEY_ACCESS_TOKEN, result.accessToken)
            putString(KEY_REFRESH_TOKEN, result.refreshToken)
            putString(KEY_USER_INFO, serializeUserInfo(result.user))
            apply()
        }
    }

    private fun serializeUserInfo(user: UserInfoVo?): String {
        if (user == null) return "{}"
        return """{"id":${user.id},"username":"${user.username}","displayName":"${user.displayName ?: ""}","roles":${user.roles}}"""
    }

    private fun parseUserInfo(json: String): UserInfoVo? {
        return try {
            val regex = """\{"id":(\d+),"username":"([^"]*)","displayName":"([^"]*)","roles":\[(.*?)\]\}""".toRegex()
            val match = regex.find(json)
            match?.let {
                val (id, username, displayName, roles) = it.destructured
                UserInfoVo(
                    id = id.toLong(),
                    username = username,
                    displayName = displayName.ifEmpty { null },
                    roles = if (roles.isBlank()) emptyList() else roles.split(",").map { it.trim().removeSurrounding("\"") }
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
