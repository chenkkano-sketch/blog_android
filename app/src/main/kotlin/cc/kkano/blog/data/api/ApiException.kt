package cc.kkano.blog.data.api

class ApiException(
    message: String,
    val code: Int? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
