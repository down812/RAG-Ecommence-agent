package com.jschaofan.ragagent.data.repository

import com.jschaofan.ragagent.data.remote.api.PortalApi
import com.jschaofan.ragagent.data.remote.api.ProductApi
import com.jschaofan.ragagent.data.remote.dto.AddToCartDto
import com.jschaofan.ragagent.data.remote.dto.CartDto
import com.jschaofan.ragagent.data.remote.dto.UpdateCartItemDto
import com.jschaofan.ragagent.domain.cart.model.AddCartProduct
import com.jschaofan.ragagent.domain.cart.model.CartItem
import com.jschaofan.ragagent.domain.cart.model.CartSnapshot
import com.jschaofan.ragagent.domain.cart.repository.CartMutationResult
import com.jschaofan.ragagent.domain.cart.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RemoteCartRepository(
    private val api: PortalApi,
    private val productApi: ProductApi,
) : CartRepository {
    private val _cart = MutableStateFlow(CartSnapshot())
    override val cart: StateFlow<CartSnapshot> = _cart

    override suspend fun refresh() = execute { api.getCart().data }

    override suspend fun add(product: AddCartProduct): CartMutationResult {
        // 推荐卡片没有 SKU 时，补查详情并自动选择第一个上架规格。
        val skuId = product.skuId ?: runCatching {
            productApi.getProductDetail(product.productId)
                .data
                ?.skuList
                ?.firstOrNull { it.status == STATUS_ON_SALE }
                ?.id
        }.getOrNull() ?: return CartMutationResult.Failure("该商品暂无可售规格")

        return execute {
            api.addToCart(AddToCartDto(product.productId, skuId, DEFAULT_QUANTITY)).data
        }
    }

    override suspend fun updateQuantity(itemKey: String, quantity: Int): CartMutationResult {
        val id = itemKey.toLongOrNull()
            ?: return CartMutationResult.Failure("购物车项无效")
        if (quantity <= 0) return remove(itemKey)
        return execute { api.updateCart(UpdateCartItemDto(id, quantity)).data }
    }

    override suspend fun remove(itemKey: String): CartMutationResult {
        val id = itemKey.toLongOrNull()
            ?: return CartMutationResult.Failure("购物车项无效")
        return execute { api.removeCartItem(id).data }
    }

    override suspend fun clear(): CartMutationResult = runCatching {
        api.clearCart()
        _cart.value = CartSnapshot()
        CartMutationResult.Success
    }.getOrElse {
        CartMutationResult.Failure(it.message ?: "清空购物车失败")
    }

    private suspend fun execute(block: suspend () -> CartDto?): CartMutationResult = runCatching {
        val data = block()
            ?: return@runCatching CartMutationResult.Failure("服务未返回购物车数据")
        _cart.value = CartSnapshot(
            items = data.cartItems.map { item ->
                CartItem(
                    key = item.cartItemId.toString(),
                    productId = item.productId,
                    skuId = item.skuId,
                    productName = item.productName,
                    imageUrl = item.productImage,
                    unitPrice = item.price,
                    quantity = item.quantity,
                    maxStock = null,
                    specification = item.skuSpec?.takeIf(String::isNotBlank)
                        ?: item.skuAttributes.joinToString("，") { "${it.attrName}：${it.attrValue}" }
                            .takeIf(String::isNotBlank),
                )
            },
        )
        CartMutationResult.Success
    }.getOrElse {
        CartMutationResult.Failure(it.message ?: "购物车操作失败")
    }

    private companion object {
        const val STATUS_ON_SALE = 1
        const val DEFAULT_QUANTITY = 1
    }
}
