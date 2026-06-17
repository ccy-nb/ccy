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
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import com.agentapp.ui.components.MarkdownText
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
    onDelete: (() -> Unit)? = null
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

    Column(
        modifier = Modifier.fillMaxWidth(),
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
        // 操作按钮（仅 AI 消息显示）
        if (!isUser && (onRegenerate != null || onEdit != null || onDelete != null)) {
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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
    }
}

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
