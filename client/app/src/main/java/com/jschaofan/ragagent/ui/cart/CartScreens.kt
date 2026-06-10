package com.jschaofan.ragagent.ui.cart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jschaofan.ragagent.domain.cart.model.CartItem
import com.jschaofan.ragagent.domain.cart.model.CheckoutOrder
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CartScreen(
    viewModel: CartViewModel,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var itemToRemove by remember { mutableStateOf<CartItem?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            PageHeader("购物车", onBack) {
                if (state.cart.items.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirmation = true }) { Text("清空") }
                }
            }
        },
        bottomBar = {
            if (state.cart.items.isNotEmpty()) {
                CartBottomBar(
                    totalAmount = state.cart.totalAmount,
                    totalQuantity = state.cart.totalQuantity,
                    onCheckout = onCheckout,
                )
            }
        },
    ) { padding ->
        if (state.cart.items.isEmpty()) {
            EmptyPage(
                title = "购物车还是空的",
                subtitle = "从推荐商品或详情页加入商品",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.cart.items, key = CartItem::key) { item ->
                    CartItemCard(
                        item = item,
                        onIncrease = { viewModel.increase(item.key) },
                        onDecrease = { viewModel.decrease(item.key) },
                        onRemove = { itemToRemove = item },
                    )
                }
            }
        }
    }
    itemToRemove?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToRemove = null },
            title = { Text("删除商品") },
            text = { Text("确定从购物车删除“${item.productName}”吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(item.key)
                    itemToRemove = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { itemToRemove = null }) { Text("取消") } },
        )
    }
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("清空购物车") },
            text = { Text("确定清空购物车中的全部商品吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCart()
                    showClearConfirmation = false
                }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirmation = false }) { Text("取消") } },
        )
    }
}

@Composable
fun CheckoutScreen(
    viewModel: CartViewModel,
    onBack: () -> Unit,
    onOrderSubmitted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    BackHandler(onBack = onBack)

    LaunchedEffect(state.completedOrder) {
        if (state.completedOrder != null) onOrderSubmitted()
    }

    Scaffold(
        topBar = { PageHeader("确认订单", onBack) },
        bottomBar = {
            if (state.cart.items.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("应付金额", style = MaterialTheme.typography.labelMedium)
                            Text(
                                state.cart.totalAmount.toPrice(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Button(onClick = viewModel::submitOrder) {
                            Text("确认下单")
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (state.cart.items.isEmpty()) {
            EmptyPage(
                title = "没有可结算的商品",
                subtitle = "请先返回购物车添加商品",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    InfoCard(
                        title = "收货地址",
                        content = state.defaultAddress,
                    )
                }
                item {
                    Text(
                        "商品清单",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(state.cart.items, key = CartItem::key) { item ->
                    CheckoutItem(item)
                }
            }
        }
    }
}

@Composable
fun OrderSuccessScreen(
    order: CheckoutOrder,
    onDone: () -> Unit,
) {
    BackHandler(onBack = onDone)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "下单成功",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("订单号：${order.orderNumber}")
            Text("共 ${order.items.sumOf(CartItem::quantity)} 件商品")
            Text(
                order.totalAmount.toPrice(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "收货地址：${order.address}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onDone,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text("返回导购")
            }
        }
    }
}

@Composable
private fun PageHeader(
    title: String,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            action?.invoke()
        }
    }
}

@Composable
private fun CartItemCard(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CartImage(item)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    item.productName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                item.specification?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    item.unitPrice.toPrice(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDecrease) { Text("−") }
                    Text("${item.quantity}")
                    TextButton(onClick = onIncrease) { Text("+") }
                    TextButton(onClick = onRemove) { Text("删除") }
                }
            }
        }
    }
}

@Composable
private fun CartImage(item: CartItem) {
    if (item.imageUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("暂无图片", style = MaterialTheme.typography.labelSmall)
        }
    } else {
        SubcomposeAsyncImage(
            model = item.imageUrl,
            contentDescription = item.productName,
            modifier = Modifier.size(86.dp),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun CartBottomBar(
    totalAmount: Double,
    totalQuantity: Int,
    onCheckout: () -> Unit,
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("共 $totalQuantity 件")
                Text(
                    totalAmount.toPrice(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Button(onClick = onCheckout) { Text("去结算") }
        }
    }
}

@Composable
private fun CheckoutItem(item: CartItem) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(item.productName, fontWeight = FontWeight.SemiBold)
            item.specification?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${item.unitPrice.toPrice()} × ${item.quantity}")
                Text(item.subtotal.toPrice(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: String) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            Text(content)
        }
    }
}

@Composable
private fun EmptyPage(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Double.toPrice(): String =
    NumberFormat.getCurrencyInstance(Locale.CHINA).format(this)
