package cc.kkano.blog

import android.app.Application
import cc.kkano.blog.data.api.BlogApiClient
import cc.kkano.blog.data.repository.BlogRepository
import cc.kkano.blog.data.token.TokenStore

class BlogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}

object AppGraph {
    lateinit var tokenStore: TokenStore
        private set

    lateinit var apiClient: BlogApiClient
        private set

    lateinit var repository: BlogRepository
        private set

    fun init(application: Application) {
        tokenStore = TokenStore(application)
        apiClient = BlogApiClient(tokenStore)
        repository = BlogRepository(apiClient, tokenStore)
    }
}
