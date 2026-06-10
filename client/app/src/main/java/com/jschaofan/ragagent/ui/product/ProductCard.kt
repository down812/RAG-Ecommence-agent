package com.jschaofan.ragagent.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jschaofan.ragagent.core.network.toEncodedImageUrl
import com.jschaofan.ragagent.ui.product.model.ProductCardUiModel
import com.jschaofan.ragagent.ui.theme.RAGGuideAgentTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductCardList(
    products: List<ProductCardUiModel>,
    onProductClick: (ProductCardUiModel) -> Unit,
    onAddToCart: (ProductCardUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (products.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "为你找到 ${products.size} 件商品",
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 2.dp,
                end = 16.dp,
                bottom = 4.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = products,
                // 后端偶尔可能返回重复商品，索引可避免 LazyRow key 冲突。
                key = { index, product -> "${product.id}-$index" },
            ) { _, product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product) },
                    onAddToCart = { onAddToCart(product) },
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductCardUiModel,
    onClick: () -> Unit,
    onAddToCart: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(244.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column {
            ProductImage(
                imageUrl = product.imageUrl,
                contentDescription = product.name,
            )
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                product.badge?.let { badge ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ProductMetadata(product)
                Text(
                    text = product.price.toPriceText(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                product.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ProductTags(tags = product.tags)
                product.salesCount?.let { salesCount ->
                    Text(
                        text = "已售 ${NumberFormat.getIntegerInstance().format(salesCount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onAddToCart != null && product.isOnSale) {
                    Button(
                        onClick = onAddToCart,
                        enabled = product.price != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (product.price == null) "价格待确认" else "加入购物车")
                    }
                } else if (!product.isOnSale) {
                    Text(
                        text = "已下架",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductImage(
    imageUrl: String?,
    contentDescription: String,
) {
    val imageModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(4f / 3f)
        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))

    if (imageUrl.isNullOrBlank()) {
        ProductImagePlaceholder(
            text = "暂无商品图片",
            modifier = imageModifier,
        )
        return
    }

    SubcomposeAsyncImage(
        model = imageUrl.toEncodedImageUrl(),
        contentDescription = contentDescription,
        modifier = imageModifier,
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        },
        error = {
            ProductImagePlaceholder(
                text = "图片加载失败",
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
private fun ProductImagePlaceholder(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProductMetadata(product: ProductCardUiModel) {
    val metadata = listOfNotNull(
        product.brand?.takeIf(String::isNotBlank),
        product.category?.takeIf(String::isNotBlank),
    ).joinToString(" · ")

    if (metadata.isNotBlank()) {
        Text(
            text = metadata,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProductTags(tags: List<String>) {
    if (tags.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.take(MAX_VISIBLE_TAGS).forEach { tag ->
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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

@Preview(showBackground = true)
@Composable
private fun ProductCardPreview() {
    RAGGuideAgentTheme(dynamicColor = false) {
        ProductCard(
            product = ProductCardUiModel(
                id = 1L,
                name = "轻量透气缓震跑鞋",
                imageUrl = null,
                price = 399.0,
                brand = "示例品牌",
                category = "运动鞋",
                description = "适合日常慢跑，鞋面轻盈透气。",
                tags = listOf("轻量", "缓震"),
                salesCount = 2350,
                badge = "评分 4.8",
            ),
            onClick = {},
            onAddToCart = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

private const val MAX_VISIBLE_TAGS = 2
