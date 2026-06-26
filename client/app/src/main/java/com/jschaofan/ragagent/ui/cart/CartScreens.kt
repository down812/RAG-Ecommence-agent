package com.jschaofan.ragagent.ui.cart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jschaofan.ragagent.domain.cart.model.CartItem
import com.jschaofan.ragagent.domain.cart.model.CheckoutOrder
import com.jschaofan.ragagent.ui.components.AppCard
import com.jschaofan.ragagent.ui.components.AppCorners
import com.jschaofan.ragagent.ui.components.AppPrimaryButton
import com.jschaofan.ragagent.ui.components.AppSpacing
import com.jschaofan.ragagent.ui.components.AppTone
import com.jschaofan.ragagent.ui.components.EmptyState
import com.jschaofan.ragagent.ui.components.PageHeader
import com.jschaofan.ragagent.ui.components.StatusPill
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            PageHeader(
                title = "购物车",
                onBack = onBack,
                action = {
                    if (state.cart.items.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirmation = true }) { Text("清空") }
                    }
                },
            )
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
            EmptyState(
                title = "购物车还是空的",
                subtitle = "从推荐商品或详情页加入商品。",
                icon = "□",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                item { AiRecommendationNotice() }
                items(state.cart.items, key = CartItem::key) { item ->
                    CartItemCard(
                        item = item,
                        onIncrease = { viewModel.increase(item.key) },
                        onDecrease = { viewModel.decrease(item.key) },
                        onRemove = { itemToRemove = item },
                    )
                }
                item { DiscountNotice() }
            }
        }
    }

    itemToRemove?.let { item ->
        ConfirmDialog(
            title = "删除商品",
            text = "确定从购物车删除“${item.productName}”吗？",
            confirmText = "删除",
            onConfirm = {
                viewModel.remove(item.key)
                itemToRemove = null
            },
            onDismiss = { itemToRemove = null },
        )
    }
    if (showClearConfirmation) {
        ConfirmDialog(
            title = "清空购物车",
            text = "确定清空购物车中的全部商品吗？",
            confirmText = "清空",
            onConfirm = {
                viewModel.clearCart()
                showClearConfirmation = false
            },
            onDismiss = { showClearConfirmation = false },
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PageHeader("确认订单", subtitle = "当前为前端模拟下单流程", onBack = onBack) },
        bottomBar = {
            if (state.cart.items.isNotEmpty()) {
                CheckoutBottomBar(totalAmount = state.cart.totalAmount, onSubmit = viewModel::submitOrder)
            }
        },
    ) { padding ->
        if (state.cart.items.isEmpty()) {
            EmptyState(
                title = "没有可结算的商品",
                subtitle = "请先返回购物车添加商品。",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                item {
                    AppCard {
                        Text("收货地址", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            state.defaultAddress,
                            modifier = Modifier.padding(top = AppSpacing.xs),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Text("商品清单", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(state.cart.items, key = CartItem::key) { item -> CheckoutItem(item) }
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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(AppSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        AppCard(tonal = true, contentPadding = PaddingValues(AppSpacing.xxl)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
                }
                Text("下单成功", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("订单号：${order.orderNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("共 ${order.items.sumOf(CartItem::quantity)} 件商品")
                Text(order.totalAmount.toPrice(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.tertiary)
                AppPrimaryButton("返回导购", onClick = onDone, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AiRecommendationNotice() {
    AppCard(tonal = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✦", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = AppSpacing.md)) {
                Text("部分商品来自 AI 推荐", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("结算前请再次核对价格、规格和数量。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DiscountNotice() {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(text = "说明", tone = AppTone.Ai)
            Text(
                text = "商品价格可能随促销活动变化，请以结算页为准。",
                modifier = Modifier.padding(start = AppSpacing.sm),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    AppCard(contentPadding = PaddingValues(AppSpacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = AppCorners.small,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            CartImage(item)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        item.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRemove, contentPadding = PaddingValues(horizontal = AppSpacing.xs)) {
                        Text("删", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item.specification?.takeIf(String::isNotBlank)?.let {
                    StatusPill(text = it, tone = AppTone.Neutral)
                }
                Text(
                    item.unitPrice.toPrice(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                QuantityStepper(
                    quantity = item.quantity,
                    onDecrease = onDecrease,
                    onIncrease = onIncrease,
                )
            }
        }
    }
}

@Composable
private fun CartImage(item: CartItem) {
    val modifier = Modifier
        .size(112.dp)
        .clip(AppCorners.medium)
        .background(MaterialTheme.colorScheme.surfaceVariant)
    if (item.imageUrl.isNullOrBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无图片", style = MaterialTheme.typography.labelSmall)
        }
    } else {
        SubcomposeAsyncImage(
            model = item.imageUrl,
            contentDescription = item.productName,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun QuantityStepper(quantity: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Surface(
        shape = AppCorners.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDecrease, contentPadding = PaddingValues(horizontal = AppSpacing.sm)) { Text("−") }
            Text("$quantity", modifier = Modifier.padding(horizontal = AppSpacing.md), fontWeight = FontWeight.Bold)
            TextButton(onClick = onIncrease, contentPadding = PaddingValues(horizontal = AppSpacing.sm)) { Text("+") }
        }
    }
}

@Composable
private fun CartBottomBar(totalAmount: Double, totalQuantity: Int, onCheckout: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(AppSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(text = "全选", tone = AppTone.Primary)
            Column(modifier = Modifier.weight(1f)) {
                Text("合计", style = MaterialTheme.typography.labelMedium)
                Text(totalAmount.toPrice(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.tertiary)
                Text("共 $totalQuantity 件", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppPrimaryButton("去结算", onClick = onCheckout)
        }
    }
}

@Composable
private fun CheckoutBottomBar(totalAmount: Double, onSubmit: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(AppSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("应付金额", style = MaterialTheme.typography.labelMedium)
                Text(totalAmount.toPrice(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.tertiary)
            }
            AppPrimaryButton("确认下单", onClick = onSubmit)
        }
    }
}

@Composable
private fun CheckoutItem(item: CartItem) {
    AppCard {
        Text(item.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        item.specification?.let {
            Text(it, modifier = Modifier.padding(top = AppSpacing.xs), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.sm))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("${item.unitPrice.toPrice()} × ${item.quantity}")
            Text(item.subtotal.toPrice(), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun Double.toPrice(): String =
    NumberFormat.getCurrencyInstance(Locale.CHINA).format(this)
