package com.jschaofan.ragagent.ui.cart

import com.jschaofan.ragagent.domain.cart.model.CartSnapshot
import com.jschaofan.ragagent.domain.cart.model.CheckoutOrder

data class CartUiState(
    val cart: CartSnapshot = CartSnapshot(),
    val defaultAddress: String = "北京市海淀区示例路 88 号",
    val message: String? = null,
    val completedOrder: CheckoutOrder? = null,
)
