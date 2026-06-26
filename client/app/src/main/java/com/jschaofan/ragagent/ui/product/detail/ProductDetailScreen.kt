package com.jschaofan.ragagent.ui.product.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jschaofan.ragagent.core.network.toEncodedImageUrl
import com.jschaofan.ragagent.domain.product.model.ProductDetail
import com.jschaofan.ragagent.domain.product.model.ProductSku
import com.jschaofan.ragagent.ui.components.AppCorners
import com.jschaofan.ragagent.ui.components.AppSpacing
import com.jschaofan.ragagent.ui.components.PageHeader
import com.jschaofan.ragagent.ui.components.StatusPill
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    cartItemCount: Int,
    onCartClick: () -> Unit,
    onAddToCart: (ProductDetail, ProductSku?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ProductDetailHeader(
                onBack = onBack,
                cartItemCount = cartItemCount,
                onCartClick = onCartClick,
            )
        },
        bottomBar = {
            uiState.product?.let { product ->
                ProductDetailBottomBar(
                    product = product,
                    onCartClick = onCartClick,
                    onAddToCart = onAddToCart,
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> ProductDetailLoading(
                modifier = Modifier.padding(innerPadding),
            )

            uiState.errorMessage != null -> ProductDetailError(
                message = uiState.errorMessage.orEmpty(),
                canRetry = uiState.canRetry,
                onRetry = viewModel::retry,
                modifier = Modifier.padding(innerPadding),
            )

            uiState.product != null -> ProductDetailContent(
                product = requireNotNull(uiState.product),
                onAddToCart = onAddToCart,
                modifier = Modifier.padding(innerPadding),
            )

            else -> ProductDetailError(
                message = "暂时无法显示商品详情",
                canRetry = false,
                onRetry = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ProductDetailHeader(
    onBack: () -> Unit,
    cartItemCount: Int,
    onCartClick: () -> Unit,
) {
    PageHeader(
        title = "商品详情",
        subtitle = "真实商品信息",
        onBack = onBack,
        action = {
            Surface(onClick = onCartClick, shape = AppCorners.small) {
                StatusPill(text = if (cartItemCount > 0) "购物车 $cartItemCount" else "购物车")
            }
        },
    )
}

@Composable
private fun ProductDetailLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = "正在加载商品详情…",
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProductDetailError(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
            if (canRetry) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("重新加载")
                }
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: ProductDetail,
    onAddToCart: (ProductDetail, ProductSku?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp),
    ) {
        item {
            DetailProductImage(
                imageUrl = product.mainImageUrl,
                contentDescription = product.title,
            )
        }
        item {
            ProductSummary(product)
        }
        item {
            ProductStatistics(product)
        }
        item {
            SectionTitle("商品规格")
        }
        if (product.skus.isEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "暂无可选规格",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(
                items = product.skus,
                key = ProductSku::id,
            ) { sku ->
                SkuCard(
                    sku = sku,
                    onAddToCart = { onAddToCart(product, sku) },
                )
            }
        }
    }
}

@Composable
private fun DetailProductImage(
    imageUrl: String?,
    contentDescription: String,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1.08f)

    if (imageUrl.isNullOrBlank()) {
        DetailImagePlaceholder("暂无商品图片", modifier)
        return
    }

    SubcomposeAsyncImage(
        model = imageUrl.toEncodedImageUrl(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        },
        error = {
            DetailImagePlaceholder(
                text = "图片加载失败",
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
private fun DetailImagePlaceholder(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProductSummary(product: ProductDetail) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        shape = AppCorners.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusBadge(isOnSale = product.isOnSale)
            product.brand?.takeIf(String::isNotBlank)?.let { brand ->
                Text(
                    text = brand,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = product.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = product.basePrice.toPriceText(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
        )
        val category = listOfNotNull(
            product.category?.takeIf(String::isNotBlank),
            product.subCategory?.takeIf(String::isNotBlank),
        ).joinToString(" / ")
        if (category.isNotBlank()) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (product.productCode.isNotBlank()) {
            Text(
                text = "商品编码：${product.productCode}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
      }
    }
}

@Composable
private fun StatusBadge(isOnSale: Boolean) {
    Surface(
        color = if (isOnSale) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = if (isOnSale) "在售" else "已下架",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isOnSale) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
        )
    }
}

@Composable
private fun ProductStatistics(product: ProductDetail) {
    HorizontalDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatisticItem("销量", product.salesCount.toCountText())
        StatisticItem("收藏", product.favoriteCount.toCountText())
        StatisticItem("规格", "${product.skus.size}")
    }
    HorizontalDivider()
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SkuCard(
    sku: ProductSku,
    onAddToCart: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs),
        shape = AppCorners.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = sku.price.toPriceText(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    if (
                        sku.originalPrice != null &&
                        sku.price != null &&
                        sku.originalPrice > sku.price
                    ) {
                        Text(
                            text = sku.originalPrice.toPriceText(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }
                Text(
                    text = if (sku.isAvailable) "库存 ${sku.stock}" else "暂不可售",
                    color = if (sku.isAvailable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (sku.attributes.isNotEmpty()) {
                sku.attributes.forEach { attribute ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = attribute.name,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = attribute.value,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else {
                Text(
                    text = "默认规格",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (sku.skuCode.isNotBlank()) {
                Text(
                    text = "SKU：${sku.skuCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (sku.isAvailable) "可加入购物车" else "暂不可售",
                style = MaterialTheme.typography.labelMedium,
                color = if (sku.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ProductDetailBottomBar(
    product: ProductDetail,
    onCartClick: () -> Unit,
    onAddToCart: (ProductDetail, ProductSku?) -> Unit,
) {
    val sku = product.skus.firstOrNull { it.isAvailable && it.price != null }
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = addClick@{
                    if (!product.isOnSale) return@addClick
                    onAddToCart(product, sku)
                },
                enabled = product.isOnSale && (product.skus.isEmpty() || sku != null),
                modifier = Modifier.weight(1f),
                shape = AppCorners.medium,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("加入购物车")
            }
            Button(
                onClick = onCartClick,
                modifier = Modifier.weight(1f),
                shape = AppCorners.medium,
            ) {
                Text("查看购物车")
            }
        }
    }
}

private fun Double?.toPriceText(): String {
    if (this == null) return "价格待确认"
    val formatter = NumberFormat.getCurrencyInstance(Locale.CHINA).apply {
        maximumFractionDigits = if (this@toPriceText % 1.0 == 0.0) 0 else 2
    }
    return formatter.format(this)
}

private fun Int?.toCountText(): String =
    this?.let { NumberFormat.getIntegerInstance().format(it) } ?: "-"
