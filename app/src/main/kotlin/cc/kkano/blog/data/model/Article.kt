package cc.kkano.blog.data.model

import com.google.gson.annotations.SerializedName

data class Article(
    val id: Long = 0,
    val title: String = "",
    val summary: String? = "",
    @SerializedName("home_img")
    val homeImg: String? = "",
    val cover: String? = "",
    @SerializedName("type_name")
    val typeName: String? = "",
    @SerializedName("category_id")
    val categoryId: Long? = null,
    @SerializedName("create_time")
    val createTime: String? = "",
    @SerializedName("created_at")
    val createdAt: String? = "",
    @SerializedName("view_count")
    val viewCount: Long? = null,
    val count: Long? = null,
    val content: String? = "",
    val tags: List<Tag> = emptyList(),
) {
    fun coverPath(): String = listOf(homeImg, cover).firstOrNull { !it.isNullOrBlank() }.orEmpty()
    fun displayTime(): String = createTime?.takeIf { it.isNotBlank() } ?: createdAt.orEmpty()
    fun views(): Long = count ?: viewCount ?: 0L
}

data class Tag(
    val id: Long = 0,
    val name: String = "",
)
