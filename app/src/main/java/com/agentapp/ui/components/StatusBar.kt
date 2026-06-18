package com.agentapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.ui.theme.AccentGreen
import com.agentapp.ui.theme.AccentOrange
import com.agentapp.ui.theme.AccentYellow

/**
 * SillyTavern 风格的底部状态栏
 *
 * 显示：LED 状态灯 + 模型名 + 流式指示 + 停止/最小化按钮
 */
@Composable
fun StreamingStatusBar(
    isStreaming: Boolean,
    modelName: String,
    streamingText: String,
    tokensSoFar: Int,
    onStop: () -> Unit,
    onMinimize: () -> Unit
) {
    if (!isStreaming && streamingText.isEmpty()) return

    val transition = rememberInfiniteTransition(label = "led")
    val pulseAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val ledColor = when {
        isStreaming -> AccentGreen
        streamingText.isNotEmpty() -> AccentYellow
        else -> AccentOrange
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LED 灯 + 模型名
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ledColor.copy(alpha = pulseAlpha))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = modelName.take(20),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (tokensSoFar > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "· ${tokensSoFar}t",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // 停止/关闭按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isStreaming) {
                    Text(
                        text = "生成中…",
                        fontSize = 11.sp,
                        color = AccentGreen,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    // 停止按钮
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AccentOrange.copy(alpha = 0.2f))
                            .clickable { onStop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                    }
                } else {
                    IconButton(onClick = onMinimize, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
