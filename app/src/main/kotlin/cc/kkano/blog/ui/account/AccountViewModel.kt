package cc.kkano.blog.ui.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.data.model.User
import cc.kkano.blog.ui.common.UiState
import kotlinx.coroutines.launch

class AccountViewModel : ViewModel() {
    private val repository = AppGraph.repository
    private val tokenStore = AppGraph.tokenStore
    private val _user = MutableLiveData<UiState<User?>>(UiState.Idle)
    val user: LiveData<UiState<User?>> = _user

    fun loadUser() {
        val cached = repository.cachedUser()
        if (cached != null) {
            _user.value = UiState.Success(cached)
        } else if (!tokenStore.hasToken()) {
            _user.value = UiState.Success(null)
            return
        } else {
            _user.value = UiState.Loading
        }

        if (!tokenStore.hasToken()) return

        viewModelScope.launch {
            _user.value = runCatching { repository.userInfo() }
                .fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { UiState.Error(it.message ?: "用户信息加载失败") },
                )
        }
    }

    fun logout() {
        repository.logout()
        _user.value = UiState.Success(null)
    }
}
