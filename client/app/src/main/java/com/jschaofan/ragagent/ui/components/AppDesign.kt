package com.jschaofan.ragagent.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jschaofan.ragagent.ui.theme.AppCardBorder
import com.jschaofan.ragagent.ui.theme.BrandBlue
import com.jschaofan.ragagent.ui.theme.BrandSky
import com.jschaofan.ragagent.ui.theme.BrandViolet
import com.jschaofan.ragagent.ui.theme.OnSoftAmber
import com.jschaofan.ragagent.ui.theme.OnSoftGreen
import com.jschaofan.ragagent.ui.theme.OnSoftRose
import com.jschaofan.ragagent.ui.theme.SoftAmber
import com.jschaofan.ragagent.ui.theme.SoftBlue
import com.jschaofan.ragagent.ui.theme.SoftGreen
import com.jschaofan.ragagent.ui.theme.SoftRose
import com.jschaofan.ragagent.ui.theme.SoftSky
import com.jschaofan.ragagent.ui.theme.SoftViolet

object AppSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object AppCorners {
    val xsmall = RoundedCornerShape(8.dp)
    val small = RoundedCornerShape(10.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(22.dp)
    val pill = RoundedCornerShape(999.dp)
}

object AppSizes {
    val iconButton = 44.dp
    val bottomBarHeight = 76.dp
    val buttonHeight = 48.dp
    val cardMinHeight = 72.dp
}

enum class AppTone {
    Primary,
    Success,
    Warning,
    Danger,
    Neutral,
    Ai,
}

@Immutable
data class AppBottomNavItem(
    val key: String,
    val label: String,
    val icon: String,
    val badge: Int? = null,
)

@Composable
fun PageHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(AppSizes.iconButton), contentAlignment = Alignment.CenterStart) {
                onBack?.let { CircularAction(label = "‹", onClick = it) }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Box(modifier = Modifier.width(AppSizes.iconButton), contentAlignment = Alignment.CenterEnd) {
                action?.invoke()
            }
        }
    }
}

@Composable
fun CircularAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = AppSizes.iconButton,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(AppSpacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    val background = if (tonal) {
        Brush.linearGradient(listOf(SoftSky, SoftViolet))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppSizes.cardMinHeight)
            .border(BorderStroke(1.dp, AppCardBorder), AppCorners.medium),
        shape = AppCorners.medium,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: String = "⌕",
    trailing: (@Composable () -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(AppCorners.pill),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Text(
                text = leading,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        trailingIcon = trailing,
        shape = AppCorners.pill,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: String? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = AppSizes.buttonHeight),
        shape = AppCorners.medium,
        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
        contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
    ) {
        leading?.let {
            Text(it, modifier = Modifier.padding(end = AppSpacing.xs))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AppOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: AppTone = AppTone.Primary,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 42.dp),
        shape = AppCorners.small,
        border = BorderStroke(1.dp, toneContent(tone)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = toneContent(tone)),
        contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AppTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: AppTone = AppTone.Primary,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, color = toneContent(tone), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: AppTone = AppTone.Primary,
    containerColor: Color = toneContainer(tone),
    contentColor: Color = toneContent(tone),
) {
    Surface(modifier = modifier, shape = AppCorners.small, color = containerColor) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: AppTone = AppTone.Primary,
    icon: String? = null,
) {
    AppCard(modifier = modifier, contentPadding = PaddingValues(AppSpacing.md)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            icon?.let {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(toneContainer(tone), AppCorners.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(it, color = toneContent(tone), style = MaterialTheme.typography.titleLarge)
                }
            }
            Column {
                Text(value, style = MaterialTheme.typography.headlineSmall, color = toneContent(tone))
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AppSegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, AppCorners.medium)
            .border(BorderStroke(1.dp, AppCardBorder), AppCorners.medium)
            .padding(3.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(AppCorners.small)
                    .background(if (selected) SoftBlue else Color.Transparent)
                    .clickable { onSelected(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun AppBottomBar(
    items: List<AppBottomNavItem>,
    selectedKey: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.key == selectedKey,
                onClick = { onSelected(item.key) },
                icon = {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Text(item.icon, style = MaterialTheme.typography.headlineSmall)
                        item.badge?.takeIf { it > 0 }?.let {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = it.coerceAtMost(99).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError,
                                )
                            }
                        }
                    }
                },
                label = {
                    Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: String = "∅",
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xxl, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(SoftBlue, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = BrandBlue, style = MaterialTheme.typography.headlineSmall)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.let {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            it()
        }
    }
}

@Composable
fun LoadingState(
    text: String,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, tonal = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(BrandViolet, CircleShape),
            )
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun toneContainer(tone: AppTone): Color = when (tone) {
    AppTone.Primary -> SoftBlue
    AppTone.Success -> SoftGreen
    AppTone.Warning -> SoftAmber
    AppTone.Danger -> SoftRose
    AppTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
    AppTone.Ai -> SoftViolet
}

@Composable
private fun toneContent(tone: AppTone): Color = when (tone) {
    AppTone.Primary -> BrandBlue
    AppTone.Success -> OnSoftGreen
    AppTone.Warning -> OnSoftAmber
    AppTone.Danger -> OnSoftRose
    AppTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    AppTone.Ai -> BrandViolet
}

@Composable
fun AppHeroBrush(): Brush = Brush.linearGradient(
    colors = listOf(SoftSky, SoftViolet, SoftBlue),
)

@Composable
fun AppPrimaryBrush(): Brush = Brush.linearGradient(
    colors = listOf(BrandBlue, BrandSky, BrandViolet),
)
