package com.jschaofan.ragagent.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jschaofan.ragagent.core.network.ApiResult
import com.jschaofan.ragagent.domain.auth.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val identifier: String = "admin1",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onIdentifierChanged(value: String) =
        _uiState.update { it.copy(identifier = value, errorMessage = null) }

    fun onPasswordChanged(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun login(onSuccess: (com.jschaofan.ragagent.domain.auth.model.LoginSession) -> Unit) {
        val state = _uiState.value
        if (state.identifier.isBlank() || state.password.isBlank() || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.login(state.identifier.trim(), state.password)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, password = "") }
                    onSuccess(result.data)
                }
                is ApiResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(repository) as T
    }
}
