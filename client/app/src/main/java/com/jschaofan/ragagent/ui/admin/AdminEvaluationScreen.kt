package com.jschaofan.ragagent.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jschaofan.ragagent.data.remote.dto.EvaluateDto
import com.jschaofan.ragagent.ui.components.AppCard
import com.jschaofan.ragagent.ui.components.AppSpacing
import com.jschaofan.ragagent.ui.components.AppTone
import com.jschaofan.ragagent.ui.components.EmptyState
import com.jschaofan.ragagent.ui.components.MetricTile
import com.jschaofan.ragagent.ui.components.StatusPill

@Composable
internal fun AdminEvaluationScreen(
    state: AdminUiState,
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier,
) {
    var deleting by remember { mutableStateOf<EvaluateDto?>(null) }
    val likes = state.evaluations.count { it.rating == 1 }
    val dislikes = state.evaluations.count { it.rating == -1 }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                MetricTile("评价总数", state.evaluations.size.toString(), Modifier.weight(1f), AppTone.Primary, "★")
                MetricTile("有帮助", likes.toString(), Modifier.weight(1f), AppTone.Success, "✓")
            }
        }
        item {
            AppCard {
                Text("评价筛选", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.padding(top = AppSpacing.xs)) {
                    TextButton(onClick = { viewModel.loadEvaluations(null) }) { Text("全部") }
                    TextButton(onClick = { viewModel.loadEvaluations(1) }) { Text("有帮助") }
                    TextButton(onClick = { viewModel.loadEvaluations(-1) }) { Text("没帮助") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    StatusPill(text = "有帮助 $likes", tone = AppTone.Success)
                    StatusPill(text = "没帮助 $dislikes", tone = AppTone.Danger)
                }
            }
        }
        if (state.evaluations.isEmpty()) {
            item { EmptyState("暂无评价", "用户对 AI 回复的反馈会显示在这里。", icon = "★") }
        }
        items(state.evaluations, key = { it.id ?: "${it.messageId}-${it.rating}" }) { evaluation ->
            AppCard {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    Row {
                        Text("用户：${evaluation.userId ?: "-"}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        StatusPill(if (evaluation.rating == 1) "有帮助" else "没帮助", tone = if (evaluation.rating == 1) AppTone.Success else AppTone.Danger)
                    }
                    Text("会话：${evaluation.sessionId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("消息：${evaluation.messageId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    evaluation.comment?.takeIf(String::isNotBlank)?.let { Text("反馈：$it") }
                    evaluation.createdAt?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    evaluation.id?.let { id ->
                        TextButton(onClick = { deleting = evaluation }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    deleting?.let { evaluation ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除评价") },
            text = { Text("确定删除这条评价记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    evaluation.id?.let(viewModel::deleteEvaluation)
                    deleting = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}
