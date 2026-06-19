package com.agentapp.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.ui.theme.AccentGreen
import com.agentapp.ui.theme.CoralAccent
import com.agentapp.ui.theme.IndigoLight
import com.agentapp.ui.theme.TealAccent
import com.agentapp.ui.theme.TextGray

/**
 * 浮动流式生成面板。
 * 显示在 ChatScreen 消息列表上方，浮动在底部输入框之上。
 * 显示生成进度、内容预览、控制按钮。
 */
@Composable
fun StreamingPanel(
    text: String,
    isLoading: Boolean,
    isMinimized: Boolean,
    onToggleMinimize: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // LED 脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "led_pulse")
    val ledAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "led_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoading) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // 顶栏：LED + 标签 + 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LED 状态灯
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(if (isLoading) ledAlpha else 1f)
                        .clip(CircleShape)
                        .background(if (isLoading) IndigoLight else TealAccent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isLoading) "生成中..." else "已完成",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isLoading) CoralAccent else TealAccent
                )
                Spacer(Modifier.weight(1f))

                // 控制按钮组
                if (isLoading) {
                    IconButton(onClick = onStop, modifier = Modifier.size(24.dp)) {
                        Text("■", fontSize = 11.sp, color = CoralAccent)
                    }
                    IconButton(onClick = onToggleMinimize, modifier = Modifier.size(24.dp)) {
                        Text(if (isMinimized) "□" else "−", fontSize = 13.sp, color = TextGray)
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Text("×", fontSize = 14.sp, color = TextGray)
                }
            }

            // 内容区域（最小化时隐藏）
            if (!isMinimized && text.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                MarkdownText(
                    text = text.let { raw ->
                        if (raw.length > 500) raw.take(500) + "\n\n…（点击输入框可查看完整内容）" else raw
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${text.length} 字 · 生成中",
                    fontSize = 10.sp,
                    color = TextGray,
                    modifier = Modifier.align(Alignment.End)
                )
            } else if (!isMinimized) {
                Spacer(Modifier.height(6.dp))
                Text("等待生成…", fontSize = 12.sp, color = TextGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }
    }
}
