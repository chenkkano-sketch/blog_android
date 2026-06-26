package cc.kkano.blog.data.token

import android.content.Context
import androidx.core.content.edit

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("kkano_auth", Context.MODE_PRIVATE)

    val token: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()

    val refreshToken: String
        get() = prefs.getString(KEY_REFRESH_TOKEN, "").orEmpty()

    val tokenExpiresAt: Long
        get() = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)

    fun hasToken(): Boolean = token.isNotBlank()

    fun isTokenExpiredOrExpiringSoon(): Boolean {
        val expiresAt = tokenExpiresAt
        if (expiresAt <= 0L) return false
        val tenMinutes = 10 * 60 * 1000L
        return expiresAt - System.currentTimeMillis() <= tenMinutes
    }

    fun saveTokens(token: String, refreshToken: String?, expiresInSeconds: Long?) {
        prefs.edit {
            putString(KEY_TOKEN, token)
            if (!refreshToken.isNullOrBlank()) putString(KEY_REFRESH_TOKEN, refreshToken)
            if (expiresInSeconds != null && expiresInSeconds > 0) {
                putLong(KEY_TOKEN_EXPIRES_AT, System.currentTimeMillis() + expiresInSeconds * 1000L)
            }
        }
    }

    fun clear() {
        prefs.edit {
            remove(KEY_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRES_AT)
            remove(KEY_USER_JSON)
        }
    }

    fun saveUserJson(json: String) {
        prefs.edit { putString(KEY_USER_JSON, json) }
    }

    fun userJson(): String = prefs.getString(KEY_USER_JSON, "").orEmpty()

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_REFRESH_TOKEN = "refreshToken"
        const val KEY_TOKEN_EXPIRES_AT = "tokenExpireTime"
        const val KEY_USER_JSON = "userinfo"
    }
}
