package com.jschaofan.ragagent.domain.cart.model

data class CartItem(
    val key: String,
    val productId: Long,
    val skuId: Long?,
    val productName: String,
    val imageUrl: String?,
    val unitPrice: Double,
    val quantity: Int,
    val maxStock: Int?,
    val specification: String?,
) {
    val subtotal: Double
        get() = unitPrice * quantity
}

data class CartSnapshot(
    val items: List<CartItem> = emptyList(),
) {
    val totalQuantity: Int
        get() = items.sumOf(CartItem::quantity)

    val totalAmount: Double
        get() = items.sumOf(CartItem::subtotal)
}

data class CheckoutOrder(
    val orderNumber: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val address: String,
    val createdAtEpochMillis: Long,
)

data class AddCartProduct(
    val productId: Long,
    val skuId: Long? = null,
    val productName: String,
    val imageUrl: String?,
    val unitPrice: Double,
    val maxStock: Int? = null,
    val specification: String? = null,
) {
    val key: String
        get() = "$productId-${skuId ?: DEFAULT_SKU_ID}"

    private companion object {
        const val DEFAULT_SKU_ID = 0L
    }
}
