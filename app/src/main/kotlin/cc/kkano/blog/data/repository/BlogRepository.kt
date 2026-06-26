package cc.kkano.blog.data.repository

import cc.kkano.blog.data.api.ApiException
import cc.kkano.blog.data.api.ApiRoutes
import cc.kkano.blog.data.api.BlogApiClient
import cc.kkano.blog.data.model.Article
import cc.kkano.blog.data.model.Dynamic
import cc.kkano.blog.data.model.LoginPayload
import cc.kkano.blog.data.model.User
import cc.kkano.blog.data.token.TokenStore
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class BlogRepository(
    private val api: BlogApiClient,
    private val tokenStore: TokenStore,
) {
    suspend fun articles(page: Int = 1, limit: Int = 20, keyword: String = ""): List<Article> {
        val root = api.get(
            ApiRoutes.ARTICLES,
            mapOf("page" to page, "limit" to limit, "keyword" to keyword),
        )
        return root.readList("list", Article::class.java)
    }

    suspend fun article(id: Long): Article {
        val root = api.get(ApiRoutes.article(id))
        return root.readDataObject(Article::class.java)
    }

    suspend fun dynamics(page: Int = 1, limit: Int = 20): List<Dynamic> {
        val root = api.get(
            ApiRoutes.DYNAMICS,
            mapOf("page" to page, "limit" to limit),
        )
        return root.readList("list", Dynamic::class.java)
    }

    suspend fun login(email: String, password: String): User? {
        val root = api.post(
            ApiRoutes.LOGIN,
            mapOf("userName" to email, "password" to password, "verifyCode" to ""),
        )
        val payload = root.readDataObject(LoginPayload::class.java)
        if (payload.token.isBlank()) {
            throw ApiException(root["message"]?.asString ?: "登录失败")
        }
        tokenStore.saveTokens(
            token = payload.token,
            refreshToken = payload.resolvedRefreshToken(),
            expiresInSeconds = payload.resolvedExpiresIn(),
        )

        val user = payload.user ?: runCatching { userInfo() }.getOrNull()
        if (user != null) {
            tokenStore.saveUserJson(api.gson.toJson(user))
        }
        return user
    }

    suspend fun userInfo(): User {
        val root = api.get(ApiRoutes.USER_INFO)
        val user = root.readDataObject(User::class.java)
        tokenStore.saveUserJson(api.gson.toJson(user))
        return user
    }

    suspend fun genericList(
        endpoint: String,
        page: Int = 1,
        limit: Int = 20,
        keyword: String = "",
    ): List<JsonObject> {
        val query = buildMap<String, Any?> {
            put("page", page)
            put("limit", limit)
            if (keyword.isNotBlank()) put("keyword", keyword)
        }
        val root = api.get(endpoint, query)
        return root.readJsonList()
    }

    suspend fun post(endpoint: String, body: Map<String, Any?>): JsonObject {
        return api.post(endpoint, body)
    }

    suspend fun put(endpoint: String, body: Map<String, Any?>): JsonObject {
        return api.put(endpoint, body)
    }

    fun cachedUser(): User? {
        val json = tokenStore.userJson()
        if (json.isBlank()) return null
        return runCatching { api.gson.fromJson(json, User::class.java) }.getOrNull()
    }

    fun logout() {
        tokenStore.clear()
    }

    fun absoluteUrl(path: String?): String {
        val value = path.orEmpty().trim()
        if (value.isBlank()) return ""
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        return api.baseUrl.trimEnd('/') + "/" + value.trimStart('/')
    }

    fun displayValue(element: JsonElement?): String {
        if (element == null || element.isJsonNull) return ""
        return when {
            element.isJsonPrimitive -> element.asString
            element.isJsonArray -> "${element.asJsonArray.size()} 项"
            element.isJsonObject -> {
                val objectValue = element.asJsonObject
                objectValue["name"]?.asString
                    ?: objectValue["title"]?.asString
                    ?: objectValue["nickname"]?.asString
                    ?: objectValue["username"]?.asString
                    ?: ""
            }
            else -> ""
        }
    }

    private fun <T> JsonObject.readList(key: String, type: Class<T>): List<T> {
        val data = this["data"]
        val listElement = when {
            data == null -> null
            data.isJsonObject -> data.asJsonObject[key]
            data.isJsonArray -> data
            else -> null
        }
        if (listElement == null || !listElement.isJsonArray) return emptyList()
        return listElement.asJsonArray.mapNotNull { element ->
            runCatching { api.gson.fromJson(element, type) }.getOrNull()
        }
    }

    private fun <T> JsonObject.readDataObject(type: Class<T>): T {
        val data: JsonElement = this["data"] ?: this
        if (!data.isJsonObject) throw ApiException("接口返回格式不正确")
        return api.gson.fromJson(data, type)
    }

    private fun JsonObject.readJsonList(): List<JsonObject> {
        val data = this["data"]
        val listElement = when {
            data == null -> null
            data.isJsonObject && data.asJsonObject.has("list") -> data.asJsonObject["list"]
            data.isJsonObject && data.asJsonObject.has("data") -> data.asJsonObject["data"]
            data.isJsonArray -> data
            else -> null
        }
        if (listElement == null && data != null && data.isJsonObject) {
            return listOf(data.asJsonObject)
        }
        if (listElement == null || !listElement.isJsonArray) return emptyList()
        return listElement.asJsonArray.mapNotNull { element ->
            element.takeIf { it.isJsonObject }?.asJsonObject
        }
    }
}
