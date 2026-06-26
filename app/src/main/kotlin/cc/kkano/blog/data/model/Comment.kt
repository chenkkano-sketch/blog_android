package cc.kkano.blog.data.model

import com.google.gson.annotations.SerializedName

data class Comment(
    val id: Long = 0,
    val nickname: String? = "",
    val content: String? = "",
    val likes: Long? = 0,
    @SerializedName("created_at")
    val createdAt: String? = "",
    val user: DynamicUser? = null,
) {
    fun displayName(): String {
        return nickname?.takeIf { it.isNotBlank() }
            ?: user?.nickname?.takeIf { it.isNotBlank() }
            ?: user?.username?.takeIf { it.isNotBlank() }
            ?: "匿名用户"
    }
}
