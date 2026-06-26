package cc.kkano.blog.data.model

data class User(
    val id: Long = 0,
    val username: String = "",
    val nickname: String? = "",
    val email: String? = "",
    val avatar: String? = "",
    val roles: List<String> = emptyList(),
) {
    fun displayName(): String = nickname?.takeIf { it.isNotBlank() } ?: username
    fun primaryRole(): String = roles.firstOrNull().orEmpty()
}
