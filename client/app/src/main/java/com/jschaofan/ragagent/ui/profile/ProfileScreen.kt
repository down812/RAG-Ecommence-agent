package com.jschaofan.ragagent.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jschaofan.ragagent.ui.components.AppBottomBar
import com.jschaofan.ragagent.ui.components.AppBottomNavItem
import com.jschaofan.ragagent.ui.components.AppCard
import com.jschaofan.ragagent.ui.components.AppCorners
import com.jschaofan.ragagent.ui.components.AppSpacing
import com.jschaofan.ragagent.ui.components.AppTone
import com.jschaofan.ragagent.ui.components.StatusPill

@Composable
fun ProfileScreen(
    identifier: String,
    userId: Long?,
    userType: Int,
    cartItemCount: Int,
    onHomeClick: () -> Unit,
    onChatClick: () -> Unit,
    onCartClick: () -> Unit,
    onAdminClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val isAdmin = userType in setOf(0, 1)
    var confirmLogout by remember { mutableStateOf(false) }
    BackHandler(onBack = onHomeClick)

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
                selectedKey = "profile",
                onSelected = { key ->
                    when (key) {
                        "home" -> onHomeClick()
                        "chat" -> onChatClick()
                        "cart" -> onCartClick()
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        ) {
            item { ProfileTitle() }
            item {
                ProfileHero(
                    identifier = identifier,
                    userId = userId,
                    role = userType.toRoleName(),
                    isAdmin = isAdmin,
                )
            }
            if (isAdmin) {
                item {
                    SectionTitle("管理中心")
                    AdminEntryGrid(onAdminClick = onAdminClick)
                }
            }
            item {
                SectionTitle("常用功能")
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    ProfileAction("AI 咨询历史", "查看并继续你的导购对话", "▣", AppTone.Ai, onChatClick)
                    ProfileAction("我的购物车", "查看已加入的商品", "⌗", AppTone.Primary, onCartClick)
                    ProfileAction("退出登录", "安全退出当前账号", "↪", AppTone.Danger) {
                        confirmLogout = true
                    }
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogoutClick()
                }) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ProfileTitle() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "我的",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        StatusPill(text = "已登录", tone = AppTone.Success)
    }
}

@Composable
private fun ProfileHero(
    identifier: String,
    userId: Long?,
    role: String,
    isAdmin: Boolean,
) {
    AppCard(tonal = true, contentPadding = PaddingValues(AppSpacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("AI", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = identifier.ifBlank { "当前用户" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(text = role, tone = if (isAdmin) AppTone.Ai else AppTone.Primary)
                }
                Text(
                    text = "用户 ID：${userId?.toString() ?: "-"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppCorners.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusPill(text = if (isAdmin) "全部权限" else "普通权限", tone = if (isAdmin) AppTone.Ai else AppTone.Neutral)
                        Text(
                            text = if (isAdmin) "可进入运营管理模块" else "可使用导购与购物车",
                            modifier = Modifier.padding(start = AppSpacing.sm),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = AppSpacing.sm),
    )
}

@Composable
private fun AdminEntryGrid(onAdminClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            AdminEntry("商品管理", "商品信息与库存", "▣", onAdminClick, Modifier.weight(1f))
            AdminEntry("知识库管理", "AI 知识与内容", "▤", onAdminClick, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            AdminEntry("用户管理", "用户与权限", "●", onAdminClick, Modifier.weight(1f))
            AdminEntry("评价管理", "评价与反馈", "★", onAdminClick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AdminEntry(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(AppSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, AppCorners.medium),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.padding(start = AppSpacing.sm)) {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProfileAction(
    title: String,
    subtitle: String,
    icon: String,
    tone: AppTone,
    onClick: () -> Unit,
) {
    AppCard(
        modifier = Modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(AppSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(toneContainer(tone), AppCorners.medium),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, color = toneContent(tone), style = MaterialTheme.typography.titleLarge)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AppSpacing.md),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun toneContainer(tone: AppTone) = when (tone) {
    AppTone.Success -> MaterialTheme.colorScheme.secondaryContainer
    AppTone.Danger -> MaterialTheme.colorScheme.errorContainer
    AppTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
    else -> MaterialTheme.colorScheme.primaryContainer
}

@Composable
private fun toneContent(tone: AppTone) = when (tone) {
    AppTone.Success -> MaterialTheme.colorScheme.secondary
    AppTone.Danger -> MaterialTheme.colorScheme.onErrorContainer
    AppTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.primary
}

private fun Int.toRoleName(): String = when (this) {
    0 -> "超级管理员"
    1 -> "管理员"
    else -> "普通用户"
}
