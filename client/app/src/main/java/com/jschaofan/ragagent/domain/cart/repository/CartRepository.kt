package com.jschaofan.ragagent.domain.cart.repository

import com.jschaofan.ragagent.domain.cart.model.AddCartProduct
import com.jschaofan.ragagent.domain.cart.model.CartSnapshot
import kotlinx.coroutines.flow.StateFlow

interface CartRepository {
    val cart: StateFlow<CartSnapshot>

    suspend fun refresh(): CartMutationResult
    suspend fun add(product: AddCartProduct): CartMutationResult
    suspend fun updateQuantity(itemKey: String, quantity: Int): CartMutationResult
    suspend fun remove(itemKey: String): CartMutationResult
    suspend fun clear(): CartMutationResult
}

sealed interface CartMutationResult {
    data object Success : CartMutationResult
    data class Failure(val message: String) : CartMutationResult
}
