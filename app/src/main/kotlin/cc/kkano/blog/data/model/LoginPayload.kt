package cc.kkano.blog.data.model

import com.google.gson.annotations.SerializedName

data class LoginPayload(
    val token: String = "",
    val refreshToken: String? = null,
    @SerializedName("refresh_token")
    val refreshTokenSnake: String? = null,
    val expiresIn: Long? = null,
    @SerializedName("expires_in")
    val expiresInSnake: Long? = null,
    val user: User? = null,
) {
    fun resolvedRefreshToken(): String? = refreshToken ?: refreshTokenSnake
    fun resolvedExpiresIn(): Long? = expiresIn ?: expiresInSnake
}
