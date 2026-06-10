package com.jschaofan.ragagent.ui.product.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
    isAdmin: Boolean,
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
                ProductFilters(state, viewModel, isAdmin)
            }
            if (state.isLoading) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.error != null) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(onClick = viewModel::load) { Text("重新加载") }
                    }
                }
            } else {
                items(state.products, key = ProductDetail::id) { product ->
                    ProductCard(
                        product = product.toCard(),
                        onClick = { onProductClick(product.id) },
                        onAddToCart = { onAddToCart(product) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = viewModel::previousPage, enabled = state.currentPage > 1) { Text("上一页") }
                    Text("${state.currentPage} / ${state.totalPages}")
                    TextButton(onClick = viewModel::nextPage, enabled = state.currentPage < state.totalPages) { Text("下一页") }
                }
            }
        }
    }
}

@Composable
private fun ProductFilters(state: ProductListUiState, viewModel: ProductListViewModel, isAdmin: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.keyword,
            onValueChange = viewModel::onKeywordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("按商品名称搜索") },
            singleLine = true,
        )
        Text("品牌", fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = state.brand == null, onClick = { viewModel.selectBrand(null) }, label = { Text("全部") }) }
            items(state.brands) { brand ->
                FilterChip(selected = state.brand == brand, onClick = { viewModel.selectBrand(brand) }, label = { Text(brand) })
            }
        }
        Text("类别", fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = state.category == null, onClick = { viewModel.selectCategory(null) }, label = { Text("全部") }) }
            items(state.categories) { category ->
                FilterChip(selected = state.category == category, onClick = { viewModel.selectCategory(category) }, label = { Text(category) })
            }
        }
        if (isAdmin) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.status == null, onClick = { viewModel.selectStatus(null) }, label = { Text("全部") })
                FilterChip(selected = state.status == 1, onClick = { viewModel.selectStatus(1) }, label = { Text("上架") })
                FilterChip(selected = state.status == 0, onClick = { viewModel.selectStatus(0) }, label = { Text("下架") })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = viewModel::load) { Text("查询") }
            TextButton(onClick = { viewModel.reset(isAdmin) }) { Text("重置") }
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
)
