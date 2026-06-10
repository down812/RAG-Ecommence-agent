package com.jschaofan.ragagent.ui.product.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jschaofan.ragagent.domain.product.model.ProductDetail
import com.jschaofan.ragagent.ui.product.ProductCard
import com.jschaofan.ragagent.ui.product.model.ProductCardUiModel

@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel,
    onBack: () -> Unit,
    onProductClick: (Long) -> Unit,
    onAddToCart: (ProductDetail) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            Surface(shadowElevation = 1.dp) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack) { Text("返回") }
                    Text("全部商品", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SearchHeader(
                    keyword = state.searchKeyword,
                    onKeywordChanged = viewModel::onSearchKeywordChanged,
                    onSearch = { viewModel.search() },
                    onReset = viewModel::reset,
                )
            }

            when {
                state.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                state.error != null -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = state.error ?: "加载失败",
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = viewModel::reset) { Text("重试") }
                        }
                    }
                }

                state.products.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (state.isSearchResult) "没有找到相关商品" else "暂无商品",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> {
                    item {
                        Text(
                            text = if (state.isSearchResult) "搜索结果" else "全部商品",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }

                    items(state.products, key = ProductDetail::id) { product ->
                        ProductCard(
                            product = product.toCard(),
                            onClick = { onProductClick(product.id) },
                            onAddToCart = { onAddToCart(product) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = viewModel::previousPage,
                                enabled = state.currentPage > 1,
                            ) { Text("上一页") }
                            Text("${state.currentPage} / ${state.totalPages}")
                            TextButton(
                                onClick = viewModel::nextPage,
                                enabled = state.currentPage < state.totalPages,
                            ) { Text("下一页") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索商品") },
                placeholder = { Text("请输入商品名称关键词") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onSearch,
                    modifier = Modifier.weight(1f),
                    enabled = keyword.isNotBlank(),
                ) {
                    Text("查询")
                }
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("重置")
                }
            }
        }
    }
}

private fun ProductDetail.toCard() = ProductCardUiModel(
    id = id,
    name = title,
    imageUrl = mainImageUrl,
    price = basePrice,
    brand = brand,
    category = category,
    description = subCategory,
    tags = emptyList(),
    salesCount = salesCount,
    badge = if (isOnSale) "在售" else "已下架",
    isOnSale = isOnSale,
)