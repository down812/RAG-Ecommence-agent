package com.jschaofan.ragagent.ui.product.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jschaofan.ragagent.core.network.ApiResult
import com.jschaofan.ragagent.domain.product.repository.ProductRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val repository: ProductRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadProduct(productId: Long) {
        loadJob?.cancel()
        _uiState.update {
            ProductDetailUiState(
                productId = productId,
                isLoading = true,
            )
        }

        loadJob = viewModelScope.launch {
            when (val result = repository.getProductDetail(productId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        // 只接收当前商品的结果，避免旧请求覆盖用户后来点击的商品。
                        if (state.productId == productId) {
                            state.copy(
                                isLoading = false,
                                product = result.data,
                                errorMessage = null,
                            )
                        } else {
                            state
                        }
                    }
                }

                is ApiResult.Failure -> {
                    _uiState.update { state ->
                        if (state.productId == productId) {
                            state.copy(
                                isLoading = false,
                                product = null,
                                errorMessage = result.message,
                            )
                        } else {
                            state
                        }
                    }
                }
            }
        }
    }

    fun retry() {
        _uiState.value.productId?.let(::loadProduct)
    }

    class Factory(
        private val repository: ProductRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProductDetailViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            return ProductDetailViewModel(repository) as T
        }
    }
}
