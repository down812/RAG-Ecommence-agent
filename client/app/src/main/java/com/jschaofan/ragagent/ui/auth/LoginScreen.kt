package com.jschaofan.ragagent.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jschaofan.ragagent.ui.components.AppCorners
import com.jschaofan.ragagent.ui.components.AppSpacing

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (com.jschaofan.ragagent.domain.auth.model.LoginSession) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.xxl),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.padding(top = 56.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = AppCorners.medium,
                ) {
                    Text(
                        text = "AI SHOPPING COPILOT",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    text = "懂需求，也懂商品",
                    modifier = Modifier.padding(top = AppSpacing.xl),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = "描述预算、场景或上传图片，\n找到真正适合你的选择。",
                    modifier = Modifier.padding(top = AppSpacing.sm),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, AppCorners.large)
                    .padding(AppSpacing.lg),
            ) {
                OutlinedTextField(
                    value = state.identifier,
                    onValueChange = viewModel::onIdentifierChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("账号") },
                    shape = AppCorners.medium,
                    singleLine = true,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.sm),
                    label = { Text("密码") },
                    shape = AppCorners.medium,
                    singleLine = true,
                    enabled = !state.isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.login(onLoginSuccess) },
                    ),
                )
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = { viewModel.login(onLoginSuccess) },
                    modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.lg).height(54.dp),
                    enabled = state.identifier.isNotBlank() &&
                        state.password.isNotBlank() &&
                        !state.isLoading,
                    shape = AppCorners.medium,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("登录")
                    }
                }
            }
        }
    }
}
