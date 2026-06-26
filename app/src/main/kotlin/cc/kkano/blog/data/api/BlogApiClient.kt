package cc.kkano.blog.data.api

import cc.kkano.blog.BuildConfig
import cc.kkano.blog.data.token.TokenStore
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class BlogApiClient(
    private val tokenStore: TokenStore,
    val gson: Gson = Gson(),
) {
    val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/') + "/"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val refreshMutex = Mutex()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun get(path: String, query: Map<String, Any?> = emptyMap()): JsonObject {
        return request("GET", path, query = query)
    }

    suspend fun post(path: String, body: Any? = emptyMap<String, Any?>()): JsonObject {
        return request("POST", path, body = body)
    }

    suspend fun put(path: String, body: Any? = emptyMap<String, Any?>()): JsonObject {
        return request("PUT", path, body = body)
    }

    suspend fun delete(path: String, body: Any? = null): JsonObject {
        return request("DELETE", path, body = body)
    }

    private suspend fun request(
        method: String,
        path: String,
        query: Map<String, Any?> = emptyMap(),
        body: Any? = null,
        retryOnUnauthorized: Boolean = true,
    ): JsonObject = withContext(Dispatchers.IO) {
        val request = buildRequest(method, path, query, body)
        execute(request, method, path, query, body, retryOnUnauthorized)
    }

    private fun buildRequest(
        method: String,
        path: String,
        query: Map<String, Any?>,
        body: Any?,
    ): Request {
        val urlBuilder = baseUrl.toHttpUrl()
            .newBuilder()
            .addPathSegments(path.trimStart('/'))

        query.forEach { (key, value) ->
            if (value != null && value.toString().isNotBlank()) {
                urlBuilder.addQueryParameter(key, value.toString())
            }
        }

        val requestBody = when {
            method == "GET" -> null
            body == null -> ByteArray(0).toRequestBody(jsonMediaType)
            else -> gson.toJson(body).toRequestBody(jsonMediaType)
        }

        val builder = Request.Builder()
            .url(urlBuilder.build())
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "KKanoBlogAndroid/${BuildConfig.VERSION_NAME}")

        val token = tokenStore.token
        if (token.isNotBlank() && normalizedPath(path) != ApiRoutes.LOGIN) {
            builder.header("Authorization", "Bearer $token")
        }

        return when (method) {
            "POST" -> builder.post(requestBody ?: ByteArray(0).toRequestBody(jsonMediaType)).build()
            "PUT" -> builder.put(requestBody ?: ByteArray(0).toRequestBody(jsonMediaType)).build()
            "DELETE" -> if (requestBody == null) builder.delete().build() else builder.delete(requestBody).build()
            else -> builder.get().build()
        }
    }

    private suspend fun execute(
        request: Request,
        method: String,
        path: String,
        query: Map<String, Any?>,
        body: Any?,
        retryOnUnauthorized: Boolean,
    ): JsonObject {
        client.newCall(request).execute().use { response ->
            val raw = response.body.string()
            val root = parseJson(raw)
            val apiCode = root["code"]?.asIntOrNull()

            if ((response.code == 401 || apiCode == 401) && retryOnUnauthorized && !isPublicPath(path)) {
                if (refreshToken()) {
                    val retryRequest = buildRequest(method, path, query, body)
                    return execute(retryRequest, method, path, query, body, retryOnUnauthorized = false)
                }
                tokenStore.clear()
                throw ApiException("登录已过期，请重新登录", 401)
            }

            if (!response.isSuccessful) {
                throw ApiException(root.messageOrDefault("请求失败：HTTP ${response.code}"), response.code)
            }

            if (apiCode != null && apiCode !in SUCCESS_CODES) {
                throw ApiException(root.messageOrDefault("请求失败"), apiCode)
            }

            return root
        }
    }

    private suspend fun refreshToken(): Boolean = refreshMutex.withLock {
        val refreshToken = tokenStore.refreshToken
        if (refreshToken.isBlank()) return@withLock false

        val body = mapOf("refresh_token" to refreshToken)
        val request = buildRequest(
            method = "POST",
            path = ApiRoutes.REFRESH,
            query = emptyMap(),
            body = body,
        )

        return@withLock withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val root = parseJson(response.body.string())
                    val code = root["code"]?.asIntOrNull()
                    if (!response.isSuccessful || code !in SUCCESS_CODES) return@use false

                    val data = root["data"]?.takeIf { it.isJsonObject }?.asJsonObject ?: root
                    val token = data["token"]?.asString.orEmpty()
                    if (token.isBlank()) return@use false
                    val newRefreshToken = data["refreshToken"]?.asString
                        ?: data["refresh_token"]?.asString
                    val expiresIn = data["expiresIn"]?.asLongOrNull()
                        ?: data["expires_in"]?.asLongOrNull()
                    tokenStore.saveTokens(token, newRefreshToken, expiresIn)
                    true
                }
            } catch (_: IOException) {
                false
            }
        }
    }

    private fun parseJson(raw: String): JsonObject {
        if (raw.isBlank()) return JsonObject()
        return runCatching { JsonParser.parseString(raw).asJsonObject }
            .getOrElse { throw ApiException("服务器返回了无法解析的数据", cause = it) }
    }

    private fun isPublicPath(path: String): Boolean {
        val normalized = normalizedPath(path)
        return normalized == ApiRoutes.LOGIN ||
            normalized == ApiRoutes.REFRESH ||
            normalized == "api/register" ||
            normalized == "api/auth/forgotPassword" ||
            normalized == "api/auth/resetPassword"
    }

    private fun normalizedPath(path: String): String {
        return path.trimStart('/').substringBefore('?')
    }

    private fun JsonObject.messageOrDefault(default: String): String {
        return this["message"]?.asString
            ?: this["msg"]?.asString
            ?: default
    }

    private fun com.google.gson.JsonElement?.asIntOrNull(): Int? {
        return runCatching { this?.asInt }.getOrNull()
    }

    private fun com.google.gson.JsonElement?.asLongOrNull(): Long? {
        return runCatching { this?.asLong }.getOrNull()
    }

    private companion object {
        val SUCCESS_CODES = setOf(0, 1, 200, 201)
    }
}
