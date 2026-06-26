package com.jschaofan.ragagent.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jschaofan.ragagent.domain.chat.model.ChatMessage
import com.jschaofan.ragagent.domain.chat.model.ChatStructuredResult
import com.jschaofan.ragagent.domain.chat.model.MessageSender
import com.jschaofan.ragagent.domain.chat.model.MessageStatus
import com.jschaofan.ragagent.ui.chat.model.ChatUiState
import com.jschaofan.ragagent.ui.chat.model.PreparedChatImage
import com.jschaofan.ragagent.ui.chat.model.MessageEvaluationState
import com.jschaofan.ragagent.ui.chat.media.CameraCaptureTarget
import com.jschaofan.ragagent.ui.product.ProductCardList
import com.jschaofan.ragagent.ui.product.model.ProductCardUiModel
import com.jschaofan.ragagent.ui.product.model.toProductCards
import com.jschaofan.ragagent.ui.components.AppCard
import com.jschaofan.ragagent.ui.components.AppCorners
import com.jschaofan.ragagent.ui.components.AppPrimaryButton
import com.jschaofan.ragagent.ui.components.AppSpacing
import com.jschaofan.ragagent.ui.components.AppTone
import com.jschaofan.ragagent.ui.components.EmptyState
import com.jschaofan.ragagent.ui.components.StatusPill
import com.jschaofan.ragagent.ui.theme.RAGGuideAgentTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier,
    onProductClick: (ProductCardUiModel) -> Unit = {},
    onAddToCart: (ProductCardUiModel) -> Unit = {},
    cartItemCount: Int = 0,
    onCartClick: () -> Unit = {},
    onProductsClick: () -> Unit = {},
    onAdminClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var cameraTarget by remember { mutableStateOf<CameraCaptureTarget?>(null) }
    var historyOpen by remember { mutableStateOf(false) }
    val sendAndDismissKeyboard = {
        if (uiState.canSend) {
            viewModel.sendMessage()
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGE_COUNT),
        onResult = viewModel::onImagesSelected,
    )
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        cameraTarget?.let { target ->
            viewModel.onCameraResult(target, success)
        }
        cameraTarget = null
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSessions()
    }

    ChatScreenContent(
        isAdmin = isAdmin,
        uiState = uiState,
        onInputChanged = viewModel::onInputChanged,
        onSendClick = sendAndDismissKeyboard,
        onStopClick = viewModel::stopGenerating,
        onRetryClick = viewModel::retryMessage,
        onEvaluate = viewModel::submitEvaluation,
        onProductClick = onProductClick,
        onAddToCart = onAddToCart,
        cartItemCount = cartItemCount,
        onCartClick = onCartClick,
        onProductsClick = onProductsClick,
        onAdminClick = onAdminClick,
        onLogoutClick = onLogoutClick,
        onBackClick = onBackClick,
        onHistoryClick = {
            historyOpen = true
            viewModel.refreshSessions()
        },
        onGalleryClick = {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onCameraClick = {
            var createdTarget: CameraCaptureTarget? = null
            runCatching {
                viewModel.createCameraTarget().also { target ->
                    createdTarget = target
                    cameraTarget = target
                    cameraLauncher.launch(target.uri)
                }
            }.onFailure {
                cameraTarget = null
                viewModel.onCameraUnavailable(createdTarget)
            }
        },
        onRemoveImage = viewModel::removeImage,
        onPageErrorShown = viewModel::clearPageError,
        modifier = modifier,
    )

    if (historyOpen) {
        ChatHistoryDialog(
            uiState = uiState,
            onDismiss = { historyOpen = false },
            onRefresh = viewModel::refreshSessions,
            onNewSession = {
                viewModel.createNewSession()
                historyOpen = false
            },
            onSessionClick = { sessionId ->
                viewModel.loadSession(sessionId)
                historyOpen = false
            },
            onDeleteSession = viewModel::deleteSession,
        )
    }
}

@Composable
private fun ChatScreenContent(
    uiState: ChatUiState,
    isAdmin: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    onRetryClick: (String) -> Unit,
    onEvaluate: (String, Int, String?) -> Unit,
    onProductClick: (ProductCardUiModel) -> Unit,
    onAddToCart: (ProductCardUiModel) -> Unit,
    cartItemCount: Int,
    onCartClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAdminClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onPageErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var autoFollowMessages by remember(uiState.sessionId) { mutableStateOf(true) }
    val showScrollToBottom =
        uiState.messages.isNotEmpty() &&
            !uiState.isLoadingSession &&
            listState.canScrollForward

    // Entering or switching a session starts at its newest message.
    LaunchedEffect(uiState.sessionId, uiState.isLoadingSession) {
        if (!uiState.isLoadingSession && uiState.messages.isNotEmpty()) {
            autoFollowMessages = true
            listState.scrollToItem(uiState.messages.lastIndex)
        }
    }

    // Pause immediately while scrolling, then resume only if scrolling ends at the bottom.
    LaunchedEffect(listState.isScrollInProgress) {
        autoFollowMessages = if (listState.isScrollInProgress) {
            false
        } else {
            !listState.canScrollForward
        }
    }

    // Stream updates follow the latest message until the user scrolls upward.
    LaunchedEffect(
        uiState.messages.lastOrNull()?.content,
        uiState.messages.lastOrNull()?.structuredResult,
        uiState.messages.size,
    ) {
        if (autoFollowMessages && uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(uiState.pageError) {
        uiState.pageError?.let { message ->
            snackbarHostState.showSnackbar(message)
            onPageErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatHeader(
                isAdmin = isAdmin,
                cartItemCount = cartItemCount,
                onCartClick = onCartClick,
                onProductsClick = onProductsClick,
                onAdminClick = onAdminClick,
                onLogoutClick = onLogoutClick,
                onBackClick = onBackClick,
                onHistoryClick = onHistoryClick,
                historyEnabled = !uiState.isGenerating && !uiState.isLoadingSession,
            )
        },
        bottomBar = {
            ChatInputBar(
                text = uiState.inputText,
                canSend = uiState.canSend,
                isGenerating = uiState.isGenerating,
                isPreparingImage = uiState.isPreparingImage,
                selectedImages = uiState.selectedImages,
                onTextChanged = onInputChanged,
                onSendClick = onSendClick,
                onStopClick = onStopClick,
                onGalleryClick = onGalleryClick,
                onCameraClick = onCameraClick,
                onRemoveImage = onRemoveImage,
            )
        },
    ) { innerPadding ->
        if (uiState.isLoadingSession) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                        )
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(
                                text = "正在打开会话",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "正在取回之前的消息",
                                modifier = Modifier.padding(top = 2.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else if (uiState.messages.isEmpty()) {
            EmptyChatContent(
                onSuggestionClick = onInputChanged,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 18.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(
                        items = uiState.messages,
                        key = ChatMessage::id,
                    ) { message ->
                        ChatMessageItem(
                            message = message,
                            onRetryClick = onRetryClick,
                            evaluationState = uiState.evaluations[message.id],
                            onEvaluate = onEvaluate,
                            onProductClick = onProductClick,
                            onAddToCart = onAddToCart,
                        )
                    }
                }
                if (showScrollToBottom) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                            .size(46.dp)
                            .clickable {
                                autoFollowMessages = true
                                coroutineScope.launch {
                                    listState.animateScrollToItem(uiState.messages.lastIndex)
                                }
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp,
                        tonalElevation = 2.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "↓",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatHeader(
    isAdmin: Boolean,
    cartItemCount: Int,
    onCartClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAdminClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    historyEnabled: Boolean,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBackClick,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp),
            ) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AppSpacing.xs),
            ) {
                Text(
                    text = "AI导购",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "智能购物助手",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HeaderAction(
                label = "◷",
                onClick = onHistoryClick,
                enabled = historyEnabled,
            )
            HeaderAction(
                label = if (cartItemCount > 0) "车$cartItemCount" else "车",
                onClick = onCartClick,
            )
            Box {
                HeaderAction(label = "更多", onClick = { menuExpanded = true })
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("全部商品") },
                        onClick = {
                            menuExpanded = false
                            onProductsClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("退出登录") },
                        onClick = {
                            menuExpanded = false
                            onLogoutClick()
                        },
                    )
                    if (isAdmin) {
                        DropdownMenuItem(
                            text = { Text("管理中心") },
                            onClick = {
                                menuExpanded = false
                                onAdminClick()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ChatHistoryDialog(
    uiState: ChatUiState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onNewSession: () -> Unit,
    onSessionClick: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
) {
    var pendingDeleteSessionId by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 咨询历史", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppPrimaryButton("新建对话", onClick = onNewSession, modifier = Modifier.fillMaxWidth())
                HorizontalDivider()
                when {
                    uiState.isLoadingSessions -> Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                    uiState.sessions.isEmpty() -> Text(
                        text = "还没有历史会话\n开始提问后，会话会保存在这里",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            items = uiState.sessions.sortedByDescending {
                                it.createdAtEpochMillis ?: 0L
                            },
                            key = { it.sessionId },
                        ) { session ->
                            val isCurrentSession = session.sessionId == uiState.sessionId
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isCurrentSession) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = AppCorners.medium,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable { onSessionClick(session.sessionId) }
                                        .padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, AppCorners.medium),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("AI", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = AppSpacing.sm),
                                    ) {
                                        Text(
                                            text = session.title?.takeIf(String::isNotBlank)
                                                ?: "未命名会话",
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrentSession) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            },
                                        )
                                        Row(
                                            modifier = Modifier.padding(top = 5.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = formatSessionTime(session.createdAtEpochMillis),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (isCurrentSession) {
                                                StatusPill(text = "当前", tone = AppTone.Primary)
                                            }
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            pendingDeleteSessionId = session.sessionId
                                        },
                                        enabled = uiState.deletingSessionId == null,
                                    ) {
                                        if (uiState.deletingSessionId == session.sessionId) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Text(
                                                text = "删除",
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            TextButton(onClick = onRefresh, enabled = !uiState.isLoadingSessions) {
                Text("刷新")
            }
        },
    )

    pendingDeleteSessionId?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSessionId = null },
            title = { Text("删除这段会话？") },
            text = { Text("删除后无法恢复，会话中的消息也会一并移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession(sessionId)
                        pendingDeleteSessionId = null
                    },
                ) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSessionId = null }) {
                    Text("取消")
                }
            },
        )
    }
}

private fun formatSessionTime(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "时间未知"
    return SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(timestamp))
}

@Composable
private fun EmptyChatContent(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.lg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            AppCard(tonal = true, contentPadding = androidx.compose.foundation.layout.PaddingValues(AppSpacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("AI", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.padding(start = AppSpacing.md)) {
                        Text(
                            text = "今天想买点什么？",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "说说预算、用途和偏好，我来帮你挑选商品。",
                            modifier = Modifier.padding(top = AppSpacing.xs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                AppPrimaryButton(
                    text = "输入需求开始咨询",
                    onClick = { onSuggestionClick("想买一部3000元左右的手机，电池续航要好一点，适合学生用") },
                    modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.md),
                )
            }
        }

        item {
            Text("试试这样问", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(QUICK_QUESTIONS) { question ->
                    Surface(
                        modifier = Modifier.clickable { onSuggestionClick(question) },
                        color = MaterialTheme.colorScheme.surface,
                        shape = AppCorners.pill,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Text(
                            text = question,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onRetryClick: (String) -> Unit,
    evaluationState: MessageEvaluationState?,
    onEvaluate: (String, Int, String?) -> Unit,
    onProductClick: (ProductCardUiModel) -> Unit,
    onAddToCart: (ProductCardUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.sender == MessageSender.USER
    if (isUser) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 300.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 8.dp,
                ),
            ) {
                MessageContent(
                    message = message,
                    onProductClick = onProductClick,
                    onAddToCart = onAddToCart,
                )
            }
            MessageStatus(
                message = message,
                onRetryClick = onRetryClick,
            )
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(
                    text = "导购助手",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp, 22.dp, 22.dp, 22.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    MessageContent(
                        message = message,
                        onProductClick = onProductClick,
                        onAddToCart = onAddToCart,
                    )
                }
                MessageStatus(
                    message = message,
                    onRetryClick = onRetryClick,
                )
                if (message.status == MessageStatus.COMPLETED) {
                    MessageEvaluationActions(
                        messageId = message.id,
                        state = evaluationState,
                        onEvaluate = onEvaluate,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageEvaluationActions(
    messageId: String,
    state: MessageEvaluationState?,
    onEvaluate: (String, Int, String?) -> Unit,
) {
    var feedbackOpen by remember(messageId) { mutableStateOf(false) }
    var comment by remember(messageId) { mutableStateOf(state?.comment.orEmpty()) }
    Row(
        modifier = Modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (state?.rating == null) "这条建议有帮助吗？" else "感谢你的反馈",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = { onEvaluate(messageId, RATING_LIKE, null) },
            enabled = state?.isSubmitting != true,
        ) {
            Text(if (state?.rating == RATING_LIKE) "已赞" else "有帮助")
        }
        TextButton(
            onClick = { feedbackOpen = true },
            enabled = state?.isSubmitting != true,
        ) {
            Text(if (state?.rating == RATING_DISLIKE) "已反馈" else "需改进")
        }
        if (state?.isSubmitting == true) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
            )
        }
    }

    if (feedbackOpen) {
        AlertDialog(
            onDismissRequest = { feedbackOpen = false },
            title = { Text("反馈这条回答") },
            text = {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("哪里可以改进（选填）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvaluate(messageId, RATING_DISLIKE, comment)
                        feedbackOpen = false
                    },
                ) { Text("提交反馈") }
            },
            dismissButton = {
                TextButton(onClick = { feedbackOpen = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun MessageContent(
    message: ChatMessage,
    onProductClick: (ProductCardUiModel) -> Unit,
    onAddToCart: (ProductCardUiModel) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (message.imagePaths.isNotEmpty()) {
            SentImageRow(imagePaths = message.imagePaths)
        }
        if (message.status == MessageStatus.STREAMING && message.content.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "正在思考…",
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else if (message.sender == MessageSender.ASSISTANT) {
            MarkdownContent(markdown = message.content.ifBlank { "暂未收到回复" })
        } else {
            Text(
                text = message.content.ifBlank { "暂未收到回复" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val structuredResult = message.structuredResult
        val products = structuredResult?.toProductCards().orEmpty()
        if (products.isNotEmpty()) {
            ProductCardList(
                products = products,
                onProductClick = onProductClick,
                onAddToCart = onAddToCart,
            )
        } else if (structuredResult.isProductResult()) {
            Text(
                text = "暂未找到符合条件的商品",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ChatStructuredResult?.isProductResult(): Boolean =
    this is ChatStructuredResult.Recommendation ||
        this is ChatStructuredResult.SearchResult ||
        this is ChatStructuredResult.ImageSearch

@Composable
private fun MessageStatus(
    message: ChatMessage,
    onRetryClick: (String) -> Unit,
) {
    when (message.status) {
        MessageStatus.FAILED -> Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "这条回复没有完成",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = message.error?.message ?: "服务暂时不可用，请稍后再试",
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (message.error?.retryable == true) {
                    TextButton(onClick = { onRetryClick(message.id) }) {
                        Text("重新生成")
                    }
                }
            }
        }
        MessageStatus.STOPPED -> Surface(
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                text = "生成已停止，可以修改需求后重新发送",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> Unit
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    canSend: Boolean,
    isGenerating: Boolean,
    isPreparingImage: Boolean,
    selectedImages: List<PreparedChatImage>,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onRemoveImage: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            if (isGenerating) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = "导购助手正在整理回复，可随时停止",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (selectedImages.isNotEmpty() || isPreparingImage) {
                ImagePreviewRow(
                    images = selectedImages,
                    isPreparing = isPreparingImage,
                    onRemoveImage = onRemoveImage,
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                TextButton(
                    onClick = onGalleryClick,
                    enabled = !isGenerating && !isPreparingImage &&
                        selectedImages.size < MAX_IMAGE_COUNT,
                ) {
                    Text("▧ 图片")
                }
                TextButton(
                    onClick = onCameraClick,
                    enabled = !isGenerating && !isPreparingImage &&
                        selectedImages.size < MAX_IMAGE_COUNT,
                ) {
                    Text("◉ 拍照")
                }
                if (selectedImages.isNotEmpty()) {
                    Text(
                        text = "${selectedImages.size}/$MAX_IMAGE_COUNT",
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(
                    start = AppSpacing.md,
                    end = AppSpacing.md,
                    top = AppSpacing.xxs,
                    bottom = AppSpacing.md,
                ),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入你的购物需求") },
                    minLines = 1,
                    maxLines = 4,
                    shape = AppCorners.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) onSendClick()
                        },
                    ),
                )
                Spacer(modifier = Modifier.size(10.dp))
                if (isGenerating) {
                    Button(
                        onClick = onStopClick,
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .padding(bottom = 1.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text("停止")
                    }
                } else {
                    Button(
                        onClick = onSendClick,
                        enabled = canSend,
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .padding(bottom = 1.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Text("➤")
                    }
                }
            }
        }
    }
}

@Composable
private fun SentImageRow(imagePaths: List<String>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = imagePaths,
            key = { path -> path },
        ) { path ->
            SubcomposeAsyncImage(
                model = File(path),
                contentDescription = "已发送图片",
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                    ),
            )
        }
    }
}

@Composable
private fun ImagePreviewRow(
    images: List<PreparedChatImage>,
    isPreparing: Boolean,
    onRemoveImage: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = images,
            key = PreparedChatImage::id,
        ) { image ->
            Box {
                SubcomposeAsyncImage(
                    model = image.file,
                    contentDescription = "待发送图片",
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                        ),
                )
                Text(
                    text = "×",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = CircleShape,
                        )
                        .clickable { onRemoveImage(image.id) }
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (isPreparing) {
            item {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "处理中",
                            modifier = Modifier.padding(top = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    RAGGuideAgentTheme(dynamicColor = false) {
        ChatScreenContent(
            isAdmin = true,
            uiState = ChatUiState(
                sessionId = "preview",
                messages = listOf(
                    ChatMessage(
                        id = "user",
                        requestId = "request",
                        sender = MessageSender.USER,
                        content = "推荐一款 500 元以内的轻量跑鞋",
                        status = MessageStatus.COMPLETED,
                        createdAtEpochMillis = 0L,
                    ),
                    ChatMessage(
                        id = "assistant",
                        requestId = "request",
                        sender = MessageSender.ASSISTANT,
                        content = "可以，我会根据预算、重量和使用场景为你筛选。",
                        status = MessageStatus.COMPLETED,
                        createdAtEpochMillis = 0L,
                    ),
                ),
            ),
            onInputChanged = {},
            onSendClick = {},
            onStopClick = {},
            onRetryClick = {},
            onEvaluate = { _, _, _ -> },
            onProductClick = {},
            onAddToCart = {},
            cartItemCount = 0,
            onCartClick = {},
            onProductsClick = {},
            onAdminClick = {},
            onLogoutClick = {},
            onBackClick = {},
            onHistoryClick = {},
            onGalleryClick = {},
            onCameraClick = {},
            onRemoveImage = {},
            onPageErrorShown = {},
        )
    }
}

private const val MAX_IMAGE_COUNT = 3
private const val RATING_LIKE = 1
private const val RATING_DISLIKE = -1
private val QUICK_QUESTIONS = listOf(
    "推荐一款通勤耳机",
    "500元以内跑鞋",
    "敏感肌护肤品",
)
