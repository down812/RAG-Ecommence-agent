package com.jschaofan.ragagent.ui.admin

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jschaofan.ragagent.data.remote.dto.SubaccountDto
import com.jschaofan.ragagent.data.remote.dto.DatasetFileDto
import com.jschaofan.ragagent.ui.components.AppCard
import com.jschaofan.ragagent.ui.components.AppCorners
import com.jschaofan.ragagent.ui.components.AppPrimaryButton
import com.jschaofan.ragagent.ui.components.AppSpacing
import com.jschaofan.ragagent.ui.components.AppTone
import com.jschaofan.ragagent.ui.components.EmptyState
import com.jschaofan.ragagent.ui.components.MetricTile
import com.jschaofan.ragagent.ui.components.PageHeader
import com.jschaofan.ragagent.ui.components.StatusPill

private enum class AdminPage { HOME, USERS, PRODUCTS, DATASETS, EVALUATIONS }

@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    currentUserId: Long?,
    currentUserType: Int,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var page by remember { mutableStateOf(AdminPage.HOME) }
    val goBack = { if (page == AdminPage.HOME) onBack() else page = AdminPage.HOME }
    BackHandler(onBack = goBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PageHeader(page.title(), subtitle = "运营工作台", onBack = goBack) },
    ) { padding ->
        when (page) {
            AdminPage.HOME -> AdminHome(
                state = state,
                modifier = Modifier.padding(padding),
                onUsers = { page = AdminPage.USERS },
                onProducts = { page = AdminPage.PRODUCTS },
                onDatasets = { page = AdminPage.DATASETS },
                onEvaluations = { page = AdminPage.EVALUATIONS },
            )
            AdminPage.USERS -> UserManagementScreen(
                state,
                viewModel,
                currentUserId,
                currentUserType,
                Modifier.padding(padding),
            )
            AdminPage.DATASETS -> DatasetManagementScreen(state, viewModel, Modifier.padding(padding))
            AdminPage.PRODUCTS -> AdminProductScreen(state, viewModel, Modifier.padding(padding))
            AdminPage.EVALUATIONS -> AdminEvaluationScreen(state, viewModel, Modifier.padding(padding))
        }
    }
}

@Composable
private fun AdminHome(
    state: AdminUiState,
    modifier: Modifier,
    onUsers: () -> Unit,
    onProducts: () -> Unit,
    onDatasets: () -> Unit,
    onEvaluations: () -> Unit,
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        item {
            AppCard(tonal = true) {
                Text("管理中心", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "维护商品事实、AI 知识库、用户权限与导购反馈。",
                    modifier = Modifier.padding(top = AppSpacing.xs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                MetricTile("商品", state.products.size.toString(), Modifier.weight(1f), AppTone.Primary, "▣")
                MetricTile("知识库", state.datasets.size.toString(), Modifier.weight(1f), AppTone.Ai, "▤")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                MetricTile("用户", state.users.size.toString(), Modifier.weight(1f), AppTone.Success, "●")
                MetricTile("评价", state.evaluations.size.toString(), Modifier.weight(1f), AppTone.Warning, "★")
            }
        }
        item { ManagementEntry("用户管理", "创建、修改和删除子账号", "●", AppTone.Success, onUsers) }
        item { ManagementEntry("商品管理", "新建、编辑、上下架商品与 SKU", "▣", AppTone.Primary, onProducts) }
        item { ManagementEntry("知识库管理", "管理数据集、启停状态和上传文件", "▤", AppTone.Ai, onDatasets) }
        item { ManagementEntry("评价管理", "查看有帮助/没帮助反馈", "★", AppTone.Warning, onEvaluations) }
    }
}

@Composable
private fun ManagementEntry(title: String, subtitle: String, icon: String, tone: AppTone, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(text = icon, tone = tone)
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.sm)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UserManagementScreen(
    state: AdminUiState,
    viewModel: AdminViewModel,
    currentUserId: Long?,
    currentUserType: Int,
    modifier: Modifier,
) {
    var editingUser by remember { mutableStateOf<SubaccountDto?>(null) }
    var deletingUser by remember { mutableStateOf<SubaccountDto?>(null) }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                MetricTile("用户总数", state.users.size.toString(), Modifier.weight(1f), AppTone.Primary, "●")
                MetricTile("管理员", state.users.count { it.type in setOf(0, 1) }.toString(), Modifier.weight(1f), AppTone.Ai, "♛")
            }
        }
        item {
            AppCard {
                Text("新增用户", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(state.identifier, viewModel::onIdentifierChanged, Modifier.fillMaxWidth().padding(top = AppSpacing.sm), label = { Text("账号") })
                OutlinedTextField(
                    state.password,
                    viewModel::onPasswordChanged,
                    Modifier.fillMaxWidth().padding(top = AppSpacing.xs),
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                RoleSelector(
                    selected = state.userType,
                    options = listOf(0, 1, 2),
                    onSelected = viewModel::onUserTypeChanged,
                )
                AppPrimaryButton("创建用户", viewModel::createUser, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.sm))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("用户列表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(viewModel::loadUsers) { Text("刷新") }
            }
        }
        if (state.users.isEmpty()) {
            item { EmptyState("暂无用户", "创建子账号后会显示在这里。", icon = "●") }
        }
        items(state.users, key = SubaccountDto::subaccountId) { user ->
            AppCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user.identifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        StatusPill(user.type.toRoleName(), tone = if (user.type in setOf(0, 1)) AppTone.Ai else AppTone.Primary)
                    }
                    Text("ID：${user.subaccountId}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(
                            onClick = { editingUser = user },
                            enabled = user.subaccountId != currentUserId && user.type != 0,
                        ) { Text("编辑") }
                        if (currentUserType == 0) {
                            TextButton(
                                onClick = { deletingUser = user },
                                enabled = user.subaccountId != currentUserId,
                            ) { Text("删除", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
    }
    editingUser?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = { editingUser = null },
            onConfirm = { password, type ->
                viewModel.updateUser(user, password, type)
                editingUser = null
            },
        )
    }
    deletingUser?.let { user ->
        AlertDialog(
            onDismissRequest = { deletingUser = null },
            title = { Text("确认删除用户") },
            text = { Text("确定删除账号“${user.identifier}”吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteUser(user.subaccountId)
                    deletingUser = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingUser = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun RoleSelector(selected: Int, options: List<Int>, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("用户类型", style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { expanded = true }) { Text(selected.toRoleName()) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.toRoleName()) },
                    onClick = { expanded = false; onSelected(type) },
                )
            }
        }
    }
}

@Composable
private fun EditUserDialog(
    user: SubaccountDto,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var type by remember(user) { mutableStateOf(user.type.coerceIn(1, 2)) }
    var confirmRoleChange by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑 ${user.identifier}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("新密码") },
                    supportingText = { Text("留空表示不修改密码") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                RoleSelector(type, listOf(1, 2)) { type = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (type != user.type) confirmRoleChange = true else onConfirm(password, type)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
    if (confirmRoleChange) {
        AlertDialog(
            onDismissRequest = { confirmRoleChange = false },
            title = { Text("确认修改权限") },
            text = { Text("确定将该用户改为“${type.toRoleName()}”吗？") },
            confirmButton = { TextButton(onClick = { onConfirm(password, type) }) { Text("确认") } },
            dismissButton = { TextButton(onClick = { confirmRoleChange = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun DatasetManagementScreen(state: AdminUiState, viewModel: AdminViewModel, modifier: Modifier) {
    var filesOpen by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::selectFile)
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                MetricTile("知识库", state.datasets.size.toString(), Modifier.weight(1f), AppTone.Ai, "▤")
                MetricTile("已启用", state.datasets.count { it.disabled == 0 }.toString(), Modifier.weight(1f), AppTone.Success, "✓")
            }
        }
        item {
            AppCard {
                Text("新建知识库", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(state.datasetName, viewModel::onDatasetNameChanged, Modifier.fillMaxWidth().padding(top = AppSpacing.sm), label = { Text("名称") })
                OutlinedTextField(state.datasetDescription, viewModel::onDatasetDescriptionChanged, Modifier.fillMaxWidth().padding(top = AppSpacing.xs), label = { Text("描述") })
                AppPrimaryButton("创建知识库", viewModel::createDataset, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.sm))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("知识库列表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(viewModel::loadDatasets) { Text("刷新") }
            }
        }
        if (state.datasets.isEmpty()) {
            item { EmptyState("暂无知识库", "创建知识库后可上传文档供 AI 检索。", icon = "▤") }
        }
        items(state.datasets, key = DatasetItem::id) { dataset ->
            AppCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dataset.name.ifBlank { "知识库 ${dataset.id}" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        StatusPill(if (dataset.disabled == 0) "已启用" else "已禁用", tone = if (dataset.disabled == 0) AppTone.Success else AppTone.Neutral)
                    }
                    if (dataset.description.isNotBlank()) Text(dataset.description)
                    Text("ID：${dataset.id}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        TextButton(onClick = {
                            viewModel.loadDatasetFiles(dataset.id)
                            filesOpen = true
                        }) { Text("文件") }
                        TextButton(onClick = { viewModel.selectDatasetForUpload(dataset.id); picker.launch(arrayOf("*/*")) }) { Text("选择文件") }
                        TextButton(onClick = viewModel::upload, enabled = state.uploadDatasetId == dataset.id) { Text("上传") }
                        TextButton(onClick = { viewModel.toggleDataset(dataset) }) { Text(if (dataset.disabled == 0) "禁用" else "启用") }
                        TextButton(onClick = { viewModel.deleteDataset(dataset.id) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        if (state.isLoading) item { CircularProgressIndicator() }
        state.selectedFileName?.let { item { Text("已选择：$it") } }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
    }
    if (filesOpen) {
        DatasetFilesDialog(
            files = state.datasetFiles,
            isLoading = state.isLoading,
            onOpen = viewModel::openDatasetFile,
            onDelete = viewModel::deleteDatasetFile,
            onDismiss = { filesOpen = false },
        )
    }
}

@Composable
private fun DatasetFilesDialog(
    files: List<DatasetFileDto>,
    isLoading: Boolean,
    onOpen: (DatasetFileDto) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("数据集文件") },
        text = {
            when {
                isLoading -> CircularProgressIndicator()
                files.isEmpty() -> Text("暂无文件")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(files, key = DatasetFileDto::id) { file ->
                        Card {
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Text(file.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${file.fileType.orEmpty()} · ${formatFileSize(file.fileSize)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row {
                                    TextButton(onClick = { onOpen(file) }) { Text("下载并打开") }
                                    TextButton(onClick = { onDelete(file.id) }) { Text("删除") }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

private fun formatFileSize(bytes: Long?): String = when {
    bytes == null -> "未知大小"
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun AdminPage.title() = when (this) {
    AdminPage.HOME -> "管理中心"
    AdminPage.USERS -> "用户管理"
    AdminPage.DATASETS -> "知识库管理"
    AdminPage.PRODUCTS -> "商品管理"
    AdminPage.EVALUATIONS -> "评价管理"
}

private fun Int.toRoleName() = when (this) {
    0 -> "超级管理员"
    1 -> "普通管理员"
    2 -> "普通用户"
    else -> "未知类型 $this"
}
