package cc.kkano.blog.ui.dynamics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.data.model.Dynamic
import cc.kkano.blog.ui.common.UiState
import kotlinx.coroutines.launch

class DynamicsViewModel : ViewModel() {
    private val repository = AppGraph.repository
    private val _dynamics = MutableLiveData<UiState<List<Dynamic>>>(UiState.Idle)
    val dynamics: LiveData<UiState<List<Dynamic>>> = _dynamics

    fun loadDynamics() {
        _dynamics.value = UiState.Loading
        viewModelScope.launch {
            _dynamics.value = runCatching { repository.dynamics(limit = 20) }
                .fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { UiState.Error(it.message ?: "动态加载失败") },
                )
        }
    }
}
