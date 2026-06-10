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
    val brands: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val keyword: String = "",
    val brand: String? = null,
    val category: String? = null,
    val status: Int? = 1,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ProductListViewModel(private val api: ProductApi) : ViewModel() {
    private val _state = MutableStateFlow(ProductListUiState())
    val state = _state.asStateFlow()

    init {
        loadFilterOptions()
        load()
    }

    fun load(page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val filters = _state.value
            runCatching {
                val result = if (filters.keyword.isNotBlank()) {
                    api.searchProducts(filters.keyword.trim(), page).data
                } else {
                    api.getProductPage(
                        pageNum = page,
                        status = filters.status,
                        category = filters.category,
                        brand = filters.brand,
                    ).data
                }
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

    private fun loadFilterOptions() {
        viewModelScope.launch {
            runCatching { api.getProducts().data.orEmpty() }.onSuccess { products ->
                _state.update {
                    it.copy(
                        brands = products.mapNotNull { dto -> dto.brand?.takeIf(String::isNotBlank) }.distinct().sorted(),
                        categories = products.mapNotNull { dto -> dto.category?.takeIf(String::isNotBlank) }.distinct().sorted(),
                    )
                }
            }
        }
    }

    fun onKeywordChanged(value: String) = _state.update { it.copy(keyword = value) }
    fun selectBrand(value: String?) = _state.update { it.copy(brand = value) }
    fun selectCategory(value: String?) = _state.update { it.copy(category = value) }
    fun selectStatus(value: Int?) = _state.update { it.copy(status = value) }
    fun previousPage() = load((_state.value.currentPage - 1).coerceAtLeast(1))
    fun nextPage() = load((_state.value.currentPage + 1).coerceAtMost(_state.value.totalPages))

    fun reset(isAdmin: Boolean) {
        _state.update { it.copy(keyword = "", brand = null, category = null, status = if (isAdmin) null else 1) }
        load()
    }

    class Factory(private val api: ProductApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProductListViewModel(api) as T
    }
}
