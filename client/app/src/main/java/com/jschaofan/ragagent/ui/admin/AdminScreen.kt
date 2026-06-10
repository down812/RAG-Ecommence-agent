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

private enum class AdminPage { HOME, USERS, DATASETS }

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
    Scaffold(topBar = { AdminHeader(page.title(), goBack) }) { padding ->
        when (page) {
            AdminPage.HOME -> AdminHome(
                modifier = Modifier.padding(padding),
                onUsers = { page = AdminPage.USERS },
                onDatasets = { page = AdminPage.DATASETS },
            )
            AdminPage.USERS -> UserManagementScreen(
                state,
                viewModel,
                currentUserId,
                currentUserType,
                Modifier.padding(padding),
            )
            AdminPage.DATASETS -> DatasetManagementScreen(state, viewModel, Modifier.padding(padding))
        }
    }
}

@Composable
private fun AdminHeader(title: String, onBack: () -> Unit) {
    Surface(shadowElevation = 1.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminHome(modifier: Modifier, onUsers: () -> Unit, onDatasets: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ManagementEntry("用户管理", "创建、修改和删除子账号", onUsers)
        ManagementEntry("知识库管理", "管理数据集、启停状态和上传文件", onDatasets)
    }
}

@Composable
private fun ManagementEntry(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("新增用户", fontWeight = FontWeight.Bold)
                    OutlinedTextField(state.identifier, viewModel::onIdentifierChanged, Modifier.fillMaxWidth(), label = { Text("账号") })
                    OutlinedTextField(
                        state.password,
                        viewModel::onPasswordChanged,
                        Modifier.fillMaxWidth(),
                        label = { Text("密码") },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    RoleSelector(
                        selected = state.userType,
                        options = listOf(0, 1, 2),
                        onSelected = viewModel::onUserTypeChanged,
                    )
                    Button(viewModel::createUser, enabled = !state.isLoading) { Text("创建用户") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("用户列表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(viewModel::loadUsers) { Text("刷新") }
            }
        }
        items(state.users, key = SubaccountDto::subaccountId) { user ->
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(user.identifier, fontWeight = FontWeight.SemiBold)
                    Text(user.type.toRoleName(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
                        TextButton(
                            onClick = { editingUser = user },
                            enabled = user.subaccountId != currentUserId && user.type != 0,
                        ) { Text("编辑") }
                        if (currentUserType == 0) {
                            TextButton(
                                onClick = { deletingUser = user },
                                enabled = user.subaccountId != currentUserId,
                            ) { Text("删除") }
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
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::selectFile)
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("新建数据集", fontWeight = FontWeight.Bold)
                    OutlinedTextField(state.datasetName, viewModel::onDatasetNameChanged, Modifier.fillMaxWidth(), label = { Text("名称") })
                    OutlinedTextField(state.datasetDescription, viewModel::onDatasetDescriptionChanged, Modifier.fillMaxWidth(), label = { Text("描述") })
                    Button(viewModel::createDataset, enabled = !state.isLoading) { Text("创建数据集") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("数据集列表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(viewModel::loadDatasets) { Text("刷新") }
            }
        }
        items(state.datasets, key = DatasetItem::id) { dataset ->
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(dataset.name.ifBlank { "数据集 ${dataset.id}" }, fontWeight = FontWeight.SemiBold)
                    if (dataset.description.isNotBlank()) Text(dataset.description)
                    Text(if (dataset.disabled == 0) "已启用" else "已禁用")
                    Row {
                        TextButton(onClick = { viewModel.selectDatasetForUpload(dataset.id); picker.launch(arrayOf("*/*")) }) { Text("选择文件") }
                        TextButton(onClick = viewModel::upload, enabled = state.uploadDatasetId == dataset.id) { Text("上传") }
                        TextButton(onClick = { viewModel.toggleDataset(dataset) }) { Text(if (dataset.disabled == 0) "禁用" else "启用") }
                        TextButton(onClick = { viewModel.deleteDataset(dataset.id) }) { Text("删除") }
                    }
                }
            }
        }
        if (state.isLoading) item { CircularProgressIndicator() }
        state.selectedFileName?.let { item { Text("已选择：$it") } }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
    }
}

private fun AdminPage.title() = when (this) {
    AdminPage.HOME -> "管理中心"
    AdminPage.USERS -> "用户管理"
    AdminPage.DATASETS -> "知识库管理"
}

private fun Int.toRoleName() = when (this) {
    0 -> "超级管理员"
    1 -> "普通管理员"
    2 -> "普通用户"
    else -> "未知类型 $this"
}
