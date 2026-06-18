package com.agentapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import com.agentapp.ui.theme.AccentBlue
import com.agentapp.ui.theme.AccentOrange
import com.agentapp.ui.theme.AvatarColors
import com.agentapp.ui.theme.DarkBubbleAssist
import com.agentapp.ui.theme.DarkBubbleUser
import com.agentapp.ui.theme.LightBubbleAssist
import com.agentapp.ui.theme.LightBubbleUser

/**
 * SillyTavern 风格的聊天气泡
 *
 * 角色消息：左侧显示头像圆形 + 名字 + 气泡
 * 用户消息：右侧显示气泡（无头像，或小头像）
 */
@Composable
fun ChatBubble(
    message: Message,
    characterName: String,
    characterAvatarUri: String?,
    isLast: Boolean,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onEdit: () -> Unit,
    onContinue: (() -> Unit)?,
    onCreateBranch: (() -> Unit)?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == Role.USER
    val isStreaming = isLast && message.content.isEmpty()

    // 用户消息靠右，角色消息靠左
    val alignment = if (isUser) Arrangement.End else Arrangement.Start
    val bubbleColor = if (isUser) {
        if (MaterialTheme.colorScheme.background == Color(0xFF1A1B26)) DarkBubbleUser
        else LightBubbleUser
    } else {
        if (MaterialTheme.colorScheme.background == Color(0xFF1A1B26)) DarkBubbleAssist
        else LightBubbleAssist
    }
    val bubbleShape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = alignment,
            verticalAlignment = Alignment.Top
        ) {
            // 角色头像（仅角色消息显示）
            if (!isUser) {
                CharacterAvatar(
                    name = characterName,
                    avatarUri = characterAvatarUri,
                    size = 36,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                // 名字栏（仅角色消息）
                if (!isUser) {
                    Text(
                        text = characterName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }

                // 消息气泡
                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .background(bubbleColor)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (isStreaming) {
                        // 流式加载占位
                        Text(
                            text = "……",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    } else {
                        MarkdownText(
                            text = message.content,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 底部操作栏（swipe 指示 + 按钮）
                if (!isUser && message.swipes.isNotEmpty()) {
                    SwipeIndicator(
                        currentIndex = message.currentSwipeId,
                        totalCount = message.swipes.size,
                        onPrev = onSwipeLeft,
                        onNext = onSwipeRight
                    )
                }
            }

            // 用户消息右侧极小头像（可选）
            if (isUser) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "我",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // 最后一条消息的菜单按钮（角色消息）
        if (!isUser && isLast) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (isUser) 0.dp else 52.dp, end = 8.dp, top = 2.dp),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                MessageActions(
                    onEdit = onEdit,
                    onContinue = onContinue,
                    onCreateBranch = onCreateBranch,
                    onDelete = onDelete
                )
            }
        }
    }
}

/** 操作按钮：编辑/继续/分支/删除 */
@Composable
private fun MessageActions(
    onEdit: () -> Unit,
    onContinue: (() -> Unit)?,
    onCreateBranch: (() -> Unit)?,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // 编辑按钮
        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "编辑",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        // 继续生成按钮
        if (onContinue != null) {
            IconButton(onClick = onContinue, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "继续",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 更多菜单
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (onCreateBranch != null) {
                    DropdownMenuItem(
                        text = { Text("🌿 从此创建分支") },
                        onClick = { showMenu = false; onCreateBranch() }
                    )
                }
                DropdownMenuItem(
                    text = { Text("🗑️ 删除") },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

/** Swipe 版本指示器 */
@Composable
private fun SwipeIndicator(
    currentIndex: Int,
    totalCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(20.dp), enabled = currentIndex > 0) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "上一个版本",
                tint = if (currentIndex > 0) AccentOrange
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = "${currentIndex + 1}/${totalCount}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        IconButton(onClick = onNext, modifier = Modifier.size(20.dp), enabled = currentIndex < totalCount - 1) {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "下一个版本",
                tint = if (currentIndex < totalCount - 1) AccentOrange
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 角色头像组件
 */
@Composable
fun CharacterAvatar(
    name: String,
    avatarUri: String?,
    size: Int = 40,
    modifier: Modifier = Modifier
) {
    val colorIndex = name.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) } % AvatarColors.size
    val bgColor = AvatarColors[colorIndex]

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri != null) {
            // TODO: 用 Coil 加载图片
            // AsyncImage(model = avatarUri, ...)
            Text(
                text = name.take(1).uppercase(),
                fontSize = (size * 0.4).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                fontSize = (size * 0.4).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
