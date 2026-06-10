package com.jschaofan.ragagent.data.repository

import com.jschaofan.ragagent.domain.cart.model.AddCartProduct
import com.jschaofan.ragagent.domain.cart.model.CartItem
import com.jschaofan.ragagent.domain.cart.model.CartSnapshot
import com.jschaofan.ragagent.domain.cart.repository.CartMutationResult
import com.jschaofan.ragagent.domain.cart.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 当前需求为模拟购物闭环，因此使用进程内购物车；后续可无缝替换为远程实现。
 */
class InMemoryCartRepository : CartRepository {
    private val _cart = MutableStateFlow(CartSnapshot())
    override val cart: StateFlow<CartSnapshot> = _cart.asStateFlow()

    override suspend fun refresh(): CartMutationResult = CartMutationResult.Success

    override suspend fun add(product: AddCartProduct): CartMutationResult {
        if (!product.unitPrice.isFinite() || product.unitPrice < 0) {
            return CartMutationResult.Failure("商品价格无效")
        }
        val current = _cart.value.items.firstOrNull { it.key == product.key }
        val newQuantity = (current?.quantity ?: 0) + 1
        val quantityLimit = product.maxStock ?: MAX_DEFAULT_QUANTITY
        if (newQuantity > quantityLimit) {
            return CartMutationResult.Failure("最多可购买 $quantityLimit 件")
        }

        _cart.update { snapshot ->
            val newItem = CartItem(
                key = product.key,
                productId = product.productId,
                skuId = product.skuId,
                productName = product.productName,
                imageUrl = product.imageUrl,
                unitPrice = product.unitPrice,
                quantity = newQuantity,
                maxStock = product.maxStock,
                specification = product.specification,
            )
            snapshot.copy(
                items = if (current == null) {
                    snapshot.items + newItem
                } else {
                    snapshot.items.map { item ->
                        if (item.key == product.key) newItem else item
                    }
                },
            )
        }
        return CartMutationResult.Success
    }

    override suspend fun updateQuantity(itemKey: String, quantity: Int): CartMutationResult {
        if (quantity <= 0) {
            remove(itemKey)
            return CartMutationResult.Success
        }
        val item = _cart.value.items.firstOrNull { it.key == itemKey }
            ?: return CartMutationResult.Failure("购物车商品不存在")
        val quantityLimit = item.maxStock ?: MAX_DEFAULT_QUANTITY
        if (quantity > quantityLimit) {
            return CartMutationResult.Failure("最多可购买 $quantityLimit 件")
        }
        _cart.update { snapshot ->
            snapshot.copy(
                items = snapshot.items.map { current ->
                    if (current.key == itemKey) current.copy(quantity = quantity) else current
                },
            )
        }
        return CartMutationResult.Success
    }

    override suspend fun remove(itemKey: String): CartMutationResult {
        _cart.update { snapshot ->
            snapshot.copy(items = snapshot.items.filterNot { it.key == itemKey })
        }
        return CartMutationResult.Success
    }

    override suspend fun clear(): CartMutationResult {
        _cart.value = CartSnapshot()
        return CartMutationResult.Success
    }

    private companion object {
        const val MAX_DEFAULT_QUANTITY = 99
    }
}
