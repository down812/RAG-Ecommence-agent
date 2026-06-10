package com.jschaofan.ragagent.ui.product.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jschaofan.ragagent.data.mapper.toDomain
import com.jschaofan.ragagent.data.remote.api.ProductApi
import com.jschaofan.ragagent.domain.product.model.ProductDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductListUiState(
    val products: List<ProductDetail> = emptyList(),
    val searchKeyword: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isSearchResult: Boolean = false,
    val error: String? = null,
)

class ProductListViewModel(private val api: ProductApi) : ViewModel() {
    private val _state = MutableStateFlow(ProductListUiState())
    val state = _state.asStateFlow()

    init {
        loadAllProducts()
    }

    fun onSearchKeywordChanged(value: String) =
        _state.update { it.copy(searchKeyword = value, error = null) }

    fun loadAllProducts(page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, isSearchResult = false) }
            runCatching {
                val result = api.getProductPage(
                    pageNum = page,
                    pageSize = PAGE_SIZE,
                ).data
                requireNotNull(result) { "服务未返回商品数据" }
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        products = result.records.map { dto -> dto.toDomain() },
                        currentPage = result.current.toInt(),
                        totalPages = result.pages.toInt().coerceAtLeast(1),
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message ?: "商品列表加载失败") }
            }
        }
    }

    fun search(resetPage: Boolean = true) {
        val keyword = _state.value.searchKeyword.trim()
        if (keyword.isBlank()) return

        val page = if (resetPage) 1 else _state.value.currentPage
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, isSearchResult = true) }
            runCatching {
                val result = api.searchProducts(
                    keyword = keyword,
                    pageNum = page,
                    pageSize = PAGE_SIZE,
                ).data
                requireNotNull(result) { "服务未返回商品数据" }
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        products = result.records.map { dto -> dto.toDomain() },
                        currentPage = result.current.toInt(),
                        totalPages = result.pages.toInt().coerceAtLeast(1),
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message ?: "搜索失败") }
            }
        }
    }

    fun reset() {
        _state.update {
            it.copy(
                searchKeyword = "",
                isSearchResult = false,
                error = null,
            )
        }
        loadAllProducts()
    }

    fun nextPage() {
        val state = _state.value
        if (state.currentPage >= state.totalPages) return
        val nextPage = state.currentPage + 1
        _state.update { it.copy(currentPage = nextPage) }
        if (state.isSearchResult) {
            search(resetPage = false)
        } else {
            loadAllProducts(nextPage)
        }
    }

    fun previousPage() {
        val state = _state.value
        if (state.currentPage <= 1) return
        val prevPage = state.currentPage - 1
        _state.update { it.copy(currentPage = prevPage) }
        if (state.isSearchResult) {
            search(resetPage = false)
        } else {
            loadAllProducts(prevPage)
        }
    }

    class Factory(private val api: ProductApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProductListViewModel(api) as T
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}