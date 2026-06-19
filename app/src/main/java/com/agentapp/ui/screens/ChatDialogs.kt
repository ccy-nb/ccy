package com.agentapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agentapp.data.model.Message
import com.agentapp.data.model.Role
import com.agentapp.ui.theme.CoralAccent
import com.agentapp.ui.theme.Pink
import com.agentapp.ui.theme.PinkDark
import com.agentapp.ui.theme.TextGray

/** 消息操作弹窗：重新生成 / 编辑 / 删除 */
@Composable
fun MessageActionDialog(
    msgId: String,
    message: Message?,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBranch: (() -> Unit)? = null
) {
    val isAi = message?.role == Role.ASSISTANT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("消息操作 ♡", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (isAi) {
                    TextButton(
                        onClick = { onRegenerate(); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🔄 重新生成", fontWeight = FontWeight.Medium) }
                    if (onBranch != null) {
                        TextButton(
                            onClick = { onBranch(); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("🌿 从此创建分支", fontWeight = FontWeight.Medium) }
                    }
                }
                TextButton(
                    onClick = { onEdit(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✏️ 编辑", fontWeight = FontWeight.Medium) }
                TextButton(
                    onClick = { onDelete(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🗑️ 删除", color = CoralAccent, fontWeight = FontWeight.Medium) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

/** 编辑消息弹窗 —— 支持仅保存或保存后重新生成 */
@Composable
fun EditMessageDialog(
    currentText: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onSaveAndRegenerate: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑消息 ✏️", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = currentText,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 10,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("仅保存", fontWeight = FontWeight.Medium, color = TextGray)
            }
        },
        dismissButton = {
            if (onSaveAndRegenerate != null) {
                TextButton(onClick = onSaveAndRegenerate) {
                    Text("保存并重新生成 ✨", fontWeight = FontWeight.Bold, color = PinkDark)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
