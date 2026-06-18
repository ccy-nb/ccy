package com.agentapp.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import coil.compose.AsyncImage
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import com.agentapp.ui.components.MarkdownText
import com.agentapp.ui.components.StatusPanel
import com.agentapp.data.repository.VariableRepository
import com.agentapp.ui.theme.ChatBubbleAssistant
import com.agentapp.ui.theme.ChatBubbleUser
import com.agentapp.ui.theme.DarkBubbleAssist
import com.agentapp.ui.theme.DarkBubbleUser
import com.agentapp.ui.theme.Pink
import com.agentapp.ui.theme.TextGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// === 可爱消息气泡 ===

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CuteMessageBubble(
    message: Message,
    isUser: Boolean,
    onLongClick: (() -> Unit)? = null,
    onSpeak: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    onContinue: (() -> Unit)? = null,
    avatarUri: String? = null,        // 角色/用户头像 URI
    characterName: String = "",       // AI 消息旁显示的角色名
    isEditing: Boolean = false,       // 是否处于内联编辑模式
    editText: String = "",            // 编辑中的文本
    onEditTextChange: (String) -> Unit = {},
    onEditSave: () -> Unit = {},
    onEditCancel: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val bubbleColor = if (isUser) {
        if (isDark) DarkBubbleUser else ChatBubbleUser
    } else {
        if (isDark) DarkBubbleAssist else ChatBubbleAssistant
    }
    val timeStr = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }
    val hasSwipes = message.swipes.size > 1

    // 头像尺寸
    val avatarSize = 32.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // AI 消息：左侧头像
        if (!isUser && avatarUri != null) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "$characterName 头像",
                modifier = Modifier
                    .padding(top = 6.dp, end = 6.dp)
                    .size(avatarSize)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        if (!isUser && avatarUri == null) {
            // 无头像时显示首字母
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, end = 6.dp)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(if (isSystemInDarkTheme()) Color(0xFFB5A8D5) else Color(0xFFD4C9F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = characterName.firstOrNull()?.toString() ?: "?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

    Column(
        modifier = Modifier.widthIn(max = 280.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(vertical = 2.dp)
                .then(if (onLongClick != null) Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick) else Modifier),
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (isEditing) {
                    TextField(
                        value = editText,
                        onValueChange = onEditTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Pink,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 10,
                        singleLine = false
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onEditSave, modifier = Modifier.size(60.dp, 28.dp)) {
                            Text("保存", fontSize = 11.sp, color = Pink)
                        }
                        TextButton(onClick = onEditCancel, modifier = Modifier.size(60.dp, 28.dp)) {
                            Text("取消", fontSize = 11.sp, color = TextGray)
                        }
                    }
                } else {
                    if (isUser) {
                        Text(message.content, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface)
                    } else {
                        MarkdownText(text = message.content)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(timeStr, fontSize = 11.sp, color = TextGray)
                }
            }
        }

        // Swipe 版本切换控件（仅 AI 消息且有多版本时显示）
        if (!isUser && hasSwipes && onSwipeLeft != null && onSwipeRight != null) {
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 左箭头
                IconButton(
                    onClick = onSwipeLeft,
                    modifier = Modifier.size(24.dp),
                    enabled = message.currentSwipeId > 0
                ) {
                    Text("◀", fontSize = 12.sp,
                        color = if (message.currentSwipeId > 0) Pink else TextGray)
                }
                // 计数器
                Text(
                    "${message.currentSwipeId + 1}/${message.swipes.size}",
                    fontSize = 11.sp,
                    color = TextGray
                )
                // 右箭头
                IconButton(
                    onClick = onSwipeRight,
                    modifier = Modifier.size(24.dp),
                    enabled = true
                ) {
                    Text("▶", fontSize = 12.sp, color = Pink)
                }
            }
        }

        // 操作按钮（仅 AI 消息显示）
        if (!isUser && (onRegenerate != null || onEdit != null || onDelete != null)) {
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (onContinue != null) {
                    IconButton(onClick = onContinue, modifier = Modifier.size(28.dp)) {
                        Text("↘", fontSize = 14.sp, color = Pink)
                    }
                }
                if (onSpeak != null) {
                    IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                        Text("🔊", fontSize = 13.sp)
                    }
                }
                if (onRegenerate != null) {
                    IconButton(onClick = onRegenerate, modifier = Modifier.size(28.dp)) {
                        Text("🔄", fontSize = 13.sp)
                    }
                }
                if (onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Text("✏️", fontSize = 13.sp)
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Text("🗑️", fontSize = 13.sp)
                    }
                }
            }
        }
    }  // close inner Column
    // 用户消息：右侧头像
    if (isUser && avatarUri != null) {
        AsyncImage(
            model = avatarUri,
            contentDescription = "用户头像",
            modifier = Modifier
                .padding(top = 6.dp, start = 6.dp)
                .size(avatarSize)
                .clip(CircleShape),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
}  // close outer Row
}  // close CuteMessageBubble

// === 打字动画 ===

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dots = listOf(
        infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "dot1"),
        infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(400, delayMillis = 150), RepeatMode.Reverse), label = "dot2"),
        infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(400, delayMillis = 300), RepeatMode.Reverse), label = "dot3"),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSystemInDarkTheme()) DarkBubbleAssist else ChatBubbleAssistant)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                dots.forEach { dot ->
                    Box(
                        Modifier
                            .size(8.dp)
                            .alpha(dot.value)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Pink)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}
