package com.jschaofan.ragagent.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jschaofan.ragagent.domain.cart.model.AddCartProduct
import com.jschaofan.ragagent.domain.cart.model.CartItem
import com.jschaofan.ragagent.domain.cart.model.CheckoutOrder
import com.jschaofan.ragagent.domain.cart.repository.CartMutationResult
import com.jschaofan.ragagent.domain.cart.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class CartViewModel(private val repository: CartRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CartUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.cart.collectLatest { cart ->
                _uiState.update { it.copy(cart = cart) }
            }
        }
        viewModelScope.launch { handleMutation(repository.refresh()) }
    }

    fun addProduct(product: AddCartProduct) {
        viewModelScope.launch {
            handleMutation(repository.add(product), "已加入购物车")
        }
    }

    fun increase(itemKey: String) = updateBy(itemKey, 1)
    fun decrease(itemKey: String) = updateBy(itemKey, -1)

    private fun updateBy(itemKey: String, delta: Int) {
        val item = _uiState.value.cart.items.firstOrNull { it.key == itemKey } ?: return
        viewModelScope.launch {
            handleMutation(repository.updateQuantity(itemKey, item.quantity + delta))
        }
    }

    fun remove(itemKey: String) {
        viewModelScope.launch {
            handleMutation(repository.remove(itemKey), "商品已删除")
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            handleMutation(repository.clear(), "购物车已清空")
        }
    }

    fun submitOrder() {
        val state = _uiState.value
        if (state.cart.items.isEmpty()) {
            showMessage("购物车还是空的")
            return
        }
        val now = System.currentTimeMillis()
        val order = CheckoutOrder(
            orderNumber = createOrderNumber(now),
            items = state.cart.items,
            totalAmount = state.cart.totalAmount,
            address = state.defaultAddress,
            createdAtEpochMillis = now,
        )
        viewModelScope.launch {
            repository.clear()
            _uiState.update { it.copy(completedOrder = order, message = null) }
        }
    }

    fun clearCompletedOrder() = _uiState.update { it.copy(completedOrder = null) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun handleMutation(result: CartMutationResult, successMessage: String? = null) {
        when (result) {
            CartMutationResult.Success -> successMessage?.let(::showMessage)
            is CartMutationResult.Failure -> showMessage(result.message)
        }
    }

    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }

    private fun createOrderNumber(timestamp: Long): String {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(Date(timestamp))
        return "RAG$date${Random.nextInt(1000, 9999)}"
    }

    class Factory(private val repository: CartRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CartViewModel(repository) as T
    }
}
