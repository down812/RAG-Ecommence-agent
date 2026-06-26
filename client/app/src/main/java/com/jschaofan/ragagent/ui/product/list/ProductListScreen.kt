package com.jschaofan.ragagent.ui.product.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jschaofan.ragagent.domain.product.model.ProductDetail
import com.jschaofan.ragagent.ui.components.AppBottomBar
import com.jschaofan.ragagent.ui.components.AppBottomNavItem
import com.jschaofan.ragagent.ui.components.AppCard
import com.jschaofan.ragagent.ui.components.AppCorners
import com.jschaofan.ragagent.ui.components.AppPrimaryButton
import com.jschaofan.ragagent.ui.components.AppSearchField
import com.jschaofan.ragagent.ui.components.AppSpacing
import com.jschaofan.ragagent.ui.components.EmptyState
import com.jschaofan.ragagent.ui.components.MetricTile
import com.jschaofan.ragagent.ui.components.StatusPill
import com.jschaofan.ragagent.ui.components.AppTone
import com.jschaofan.ragagent.ui.product.ProductCard
import com.jschaofan.ragagent.ui.product.model.ProductCardUiModel
import com.jschaofan.ragagent.ui.theme.BrandBlue
import com.jschaofan.ragagent.ui.theme.BrandViolet
import com.jschaofan.ragagent.ui.theme.SoftBlue
import com.jschaofan.ragagent.ui.theme.SoftSky

@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel,
    onBack: () -> Unit,
    onProductClick: (Long) -> Unit,
    onAddToCart: (ProductDetail) -> Unit,
    onChatClick: () -> Unit = {},
    onCartClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    cartItemCount: Int = 0,
) {
    val state by viewModel.state.collectAsState()
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomBar(
                items = listOf(
                    AppBottomNavItem("home", "首页", "⌂"),
                    AppBottomNavItem("chat", "AI导购", "AI"),
                    AppBottomNavItem("cart", "购物车", "⌗", cartItemCount),
                    AppBottomNavItem("profile", "我的", "●"),
                ),
                selectedKey = "home",
                onSelected = { key ->
                    when (key) {
                        "chat" -> onChatClick()
                        "cart" -> onCartClick()
                        "profile" -> onProfileClick()
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            item {
                HomeHeader(
                    keyword = state.searchKeyword,
                    onKeywordChanged = viewModel::onSearchKeywordChanged,
                    onSearch = { viewModel.search() },
                    onReset = viewModel::reset,
                )
            }
            item { AiGuideHero(onChatClick = onChatClick) }
            item { QuickNeeds(onNeedClick = viewModel::searchKeyword) }
            item { CategoryStrip(onCategoryClick = viewModel::searchKeyword) }

            when {
                state.isLoading -> item { LoadingBlock() }
                state.error != null -> item {
                    EmptyState(
                        title = "商品加载失败",
                        subtitle = state.error ?: "请稍后重试",
                        icon = "!",
                        action = { AppPrimaryButton("重新加载", onClick = viewModel::reset) },
                    )
                }
                state.products.isEmpty() -> item {
                    EmptyState(
                        title = if (state.isSearchResult) "没有找到相关商品" else "暂无商品",
                        subtitle = "换个关键词试试，或者让 AI 帮你描述需求。",
                        icon = "⌕",
                        action = { AppPrimaryButton("问问 AI", onClick = onChatClick) },
                    )
                }
                else -> {
                    item {
                        SectionHeader(
                            title = if (state.isSearchResult) "搜索结果" else "为你推荐",
                            action = if (state.isSearchResult) "清除" else null,
                            onAction = viewModel::reset,
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
                        PagingBar(
                            currentPage = state.currentPage,
                            totalPages = state.totalPages,
                            onPrevious = viewModel::previousPage,
                            onNext = viewModel::nextPage,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "首页",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            StatusPill(text = "RAG 导购", tone = AppTone.Ai)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            AppSearchField(
                value = keyword,
                onValueChange = onKeywordChanged,
                placeholder = "搜索手机、耳机、电脑...",
                modifier = Modifier.weight(1f),
                trailing = {
                    TextButton(
                        onClick = {
                            if (keyword.isBlank()) onReset() else onSearch()
                        },
                    ) { Text(if (keyword.isBlank()) "全部" else "搜索") }
                },
            )
        }
    }
}

@Composable
private fun AiGuideHero(onChatClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorners.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(SoftSky, SoftBlue, androidx.compose.ui.graphics.Color(0xFFF4F0FF)),
                ),
            )
            .padding(AppSpacing.lg),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AI 智能导购",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "◖AI◗",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandViolet,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "告诉我预算、用途和偏好，我来帮你推荐商品。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppPrimaryButton(text = "开始咨询", onClick = onChatClick, leading = "→")
        }
    }
}

@Composable
private fun QuickNeeds(onNeedClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        items(QUICK_NEEDS) { need ->
            AppCard(
                modifier = Modifier
                    .width(168.dp)
                    .height(54.dp)
                    .clickable { onNeedClick(need.query) },
                contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(need.icon, modifier = Modifier.padding(end = AppSpacing.xs))
                    Text(need.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun CategoryStrip(onCategoryClick: (String) -> Unit) {
    AppCard(contentPadding = PaddingValues(AppSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            CATEGORY_ITEMS.forEach { item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AppCorners.medium)
                        .clickable { onCategoryClick(item.label) }
                        .padding(vertical = AppSpacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(item.icon, style = MaterialTheme.typography.titleLarge)
                    Text(item.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        action?.let { TextButton(onClick = onAction) { Text(it) } }
    }
}

@Composable
private fun LoadingBlock() {
    AppCard(tonal = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            CircularProgressIndicator(modifier = Modifier.padding(AppSpacing.xs))
            Column {
                Text("正在加载商品", style = MaterialTheme.typography.titleSmall)
                Text("从后端商品库获取实时数据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PagingBar(currentPage: Int, totalPages: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious, enabled = currentPage > 1) { Text("‹") }
        MetricTile(label = "当前页", value = "$currentPage / $totalPages", modifier = Modifier.weight(1f), icon = null)
        TextButton(onClick = onNext, enabled = currentPage < totalPages) { Text("›") }
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
    tags = listOfNotNull(subCategory?.takeIf(String::isNotBlank)).take(2),
    salesCount = salesCount,
    badge = if (isOnSale) "上架中" else "已下架",
    isOnSale = isOnSale,
)

private data class QuickNeed(val label: String, val query: String, val icon: String)

private val QUICK_NEEDS = listOf(
    QuickNeed("3000元手机推荐", "3000元手机", "▣"),
    QuickNeed("学生笔记本", "学生笔记本", "◇"),
    QuickNeed("续航耳机", "续航耳机", "◉"),
    QuickNeed("拍照手机", "拍照手机", "◎"),
)

private data class CategoryItem(val label: String, val icon: String)

private val CATEGORY_ITEMS = listOf(
    CategoryItem("手机", "▥"),
    CategoryItem("电脑", "▤"),
    CategoryItem("耳机", "◖"),
    CategoryItem("平板", "▯"),
    CategoryItem("穿戴", "◌"),
)
