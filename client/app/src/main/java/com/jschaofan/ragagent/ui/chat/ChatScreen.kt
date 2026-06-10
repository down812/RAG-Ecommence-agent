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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jschaofan.ragagent.domain.chat.model.ChatMessage
import com.jschaofan.ragagent.domain.chat.model.ChatStructuredResult
import com.jschaofan.ragagent.domain.chat.model.MessageSender
import com.jschaofan.ragagent.domain.chat.model.MessageStatus
import com.jschaofan.ragagent.ui.chat.model.ChatUiState
import com.jschaofan.ragagent.ui.chat.model.PreparedChatImage
import com.jschaofan.ragagent.ui.chat.media.CameraCaptureTarget
import com.jschaofan.ragagent.ui.product.ProductCardList
import com.jschaofan.ragagent.ui.product.model.ProductCardUiModel
import com.jschaofan.ragagent.ui.product.model.toProductCards
import com.jschaofan.ragagent.ui.theme.RAGGuideAgentTheme
import java.io.File

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
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var cameraTarget by remember { mutableStateOf<CameraCaptureTarget?>(null) }
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

    ChatScreenContent(
        isAdmin = isAdmin,
        uiState = uiState,
        onInputChanged = viewModel::onInputChanged,
        onSendClick = sendAndDismissKeyboard,
        onStopClick = viewModel::stopGenerating,
        onRetryClick = viewModel::retryMessage,
        onProductClick = onProductClick,
        onAddToCart = onAddToCart,
        cartItemCount = cartItemCount,
        onCartClick = onCartClick,
        onProductsClick = onProductsClick,
        onAdminClick = onAdminClick,
        onLogoutClick = onLogoutClick,
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
}

@Composable
private fun ChatScreenContent(
    uiState: ChatUiState,
    isAdmin: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    onRetryClick: (String) -> Unit,
    onProductClick: (ProductCardUiModel) -> Unit,
    onAddToCart: (ProductCardUiModel) -> Unit,
    cartItemCount: Int,
    onCartClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAdminClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onPageErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 消息内容持续增长时滚动到底部，让最新的流式文本始终可见。
    LaunchedEffect(
        uiState.messages.lastOrNull()?.content,
        uiState.messages.lastOrNull()?.structuredResult,
        uiState.messages.size,
    ) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
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
        if (uiState.messages.isEmpty()) {
            EmptyChatContent(
                onSuggestionClick = onInputChanged,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = uiState.messages,
                    key = ChatMessage::id,
                ) { message ->
                    ChatMessageItem(
                        message = message,
                        onRetryClick = onRetryClick,
                        onProductClick = onProductClick,
                        onAddToCart = onAddToCart,
                    )
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
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "AI",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "智能导购助手",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "描述需求，我来帮你挑选商品",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { menuExpanded = true }) {
                    Text(if (cartItemCount > 0) "功能 ($cartItemCount)" else "功能")
                }
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
                    DropdownMenuItem(
                        text = {
                            Text(if (cartItemCount > 0) "购物车 ($cartItemCount)" else "购物车")
                        },
                        onClick = {
                            menuExpanded = false
                            onCartClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatContent(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = "AI",
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "今天想买点什么？",
                modifier = Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "可以告诉我预算、使用场景或偏好的品牌",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            LazyRow(
                modifier = Modifier.padding(top = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(QUICK_QUESTIONS) { question ->
                    Surface(
                        modifier = Modifier.clickable { onSuggestionClick(question) },
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
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
    onProductClick: (ProductCardUiModel) -> Unit,
    onAddToCart: (ProductCardUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.sender == MessageSender.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                color = bubbleColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp,
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
    val statusText = when (message.status) {
        MessageStatus.FAILED -> message.error?.message ?: "回复失败"
        MessageStatus.STOPPED -> "已停止生成"
        else -> null
    }

    statusText?.let { text ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (message.status == MessageStatus.FAILED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (message.status == MessageStatus.FAILED && message.error?.retryable == true) {
                TextButton(onClick = { onRetryClick(message.id) }) {
                    Text("重试")
                }
            }
        }
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
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (selectedImages.isNotEmpty() || isPreparingImage) {
                ImagePreviewRow(
                    images = selectedImages,
                    isPreparing = isPreparingImage,
                    onRemoveImage = onRemoveImage,
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onGalleryClick,
                    enabled = !isGenerating && !isPreparingImage &&
                        selectedImages.size < MAX_IMAGE_COUNT,
                ) {
                    Text("＋  添加图片")
                }
                TextButton(
                    onClick = onCameraClick,
                    enabled = !isGenerating && !isPreparingImage &&
                        selectedImages.size < MAX_IMAGE_COUNT,
                ) {
                    Text("◉  拍照识别")
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
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入你的购物需求") },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) onSendClick()
                        },
                    ),
                )
                Spacer(modifier = Modifier.size(10.dp))
                if (isGenerating) {
                    TextButton(
                        onClick = onStopClick,
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Text("停止")
                    }
                } else {
                    Button(
                        onClick = onSendClick,
                        enabled = canSend,
                        modifier = Modifier.padding(bottom = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Text("发送")
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
                    text = "删除",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { onRemoveImage(image.id) }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
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
            onProductClick = {},
            onAddToCart = {},
            cartItemCount = 0,
            onCartClick = {},
            onProductsClick = {},
            onAdminClick = {},
            onLogoutClick = {},
            onGalleryClick = {},
            onCameraClick = {},
            onRemoveImage = {},
            onPageErrorShown = {},
        )
    }
}

private const val MAX_IMAGE_COUNT = 3
private val QUICK_QUESTIONS = listOf(
    "推荐一款通勤耳机",
    "500元以内跑鞋",
    "敏感肌护肤品",
)
