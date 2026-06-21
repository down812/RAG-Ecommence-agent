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
import androidx.compose.material3.Card
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("评价概览", fontWeight = FontWeight.Bold)
                    Text("当前结果：好评 $likes · 差评 $dislikes")
                    Row {
                        TextButton(onClick = { viewModel.loadEvaluations(null) }) { Text("全部") }
                        TextButton(onClick = { viewModel.loadEvaluations(1) }) { Text("好评") }
                        TextButton(onClick = { viewModel.loadEvaluations(-1) }) { Text("差评") }
                    }
                }
            }
        }
        items(state.evaluations, key = { it.id ?: "${it.messageId}-${it.rating}" }) { evaluation ->
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(if (evaluation.rating == 1) "有帮助" else "没帮助", fontWeight = FontWeight.Bold)
                    Text("用户：${evaluation.userId ?: "-"}")
                    Text("会话：${evaluation.sessionId}", style = MaterialTheme.typography.bodySmall)
                    Text("消息：${evaluation.messageId}", style = MaterialTheme.typography.bodySmall)
                    evaluation.comment?.takeIf(String::isNotBlank)?.let { Text("反馈：$it") }
                    evaluation.createdAt?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    evaluation.id?.let { id ->
                        TextButton(onClick = { deleting = evaluation }) { Text("删除") }
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
