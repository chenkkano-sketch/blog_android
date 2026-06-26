package cc.kkano.blog.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class Dynamic(
    val id: Long = 0,
    @SerializedName("user_id")
    val userId: Long = 0,
    val title: String? = "",
    val content: String? = "",
    val images: JsonElement? = null,
    val views: Long? = 0,
    val likes: Long? = 0,
    @SerializedName("comments_count")
    val commentsCount: Long? = 0,
    @SerializedName("created_at")
    val createdAt: String? = "",
    val user: DynamicUser? = null,
    @SerializedName("is_liked")
    val isLiked: Boolean? = false,
    @SerializedName("like_summary")
    val likeSummary: LikeSummary? = null,
) {
    fun imagePaths(): List<String> {
        val value = images ?: return emptyList()
        return when {
            value.isJsonArray -> value.asJsonArray.mapNotNull { element ->
                when {
                    element.isJsonPrimitive -> element.asString
                    element.isJsonObject -> element.asJsonObject["url"]?.asString
                        ?: element.asJsonObject["path"]?.asString
                    else -> null
                }
            }.filter { it.isNotBlank() }
            value.isJsonPrimitive -> value.asString
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            else -> emptyList()
        }
    }
}

data class DynamicUser(
    val id: Long = 0,
    val username: String? = "",
    val nickname: String? = "",
    val avatar: String? = "",
)

data class LikeSummary(
    val count: Long = 0,
    val preview: List<DynamicUser> = emptyList(),
    @SerializedName("admin_liked")
    val adminLiked: Boolean = false,
)
