package cc.kkano.blog.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.data.model.Article
import cc.kkano.blog.ui.common.UiState
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = AppGraph.repository
    private val _articles = MutableLiveData<UiState<List<Article>>>(UiState.Idle)
    val articles: LiveData<UiState<List<Article>>> = _articles

    fun loadArticles() {
        _articles.value = UiState.Loading
        viewModelScope.launch {
            _articles.value = runCatching { repository.articles(limit = 20) }
                .fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { UiState.Error(it.message ?: "文章加载失败") },
                )
        }
    }
}
