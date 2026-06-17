package com.agentapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.data.model.formatVariableTree
import com.agentapp.data.repository.VariableRepository
import com.agentapp.ui.theme.TextGray

/**
 * 状态面板 Composable —— 将角色变量渲染为美化卡片。
 * 替代酒馆的 WebView HTML 面板，纯 Compose 实现。
 */
@Composable
fun StatusPanel(
    sessionId: String,
    variableRepository: VariableRepository,
    modifier: Modifier = Modifier
) {
    val varsFlow = variableRepository.getFlow(sessionId)
    val vars by varsFlow.collectAsState(initial = kotlinx.serialization.json.buildJsonObject {})

    if (vars.keys.isEmpty()) return

    val flatVars = variableRepository.flattenVariables(vars)
    if (flatVars.isEmpty()) return

    // 按顶级分组
    val groups = LinkedHashMap<String, MutableList<Pair<String, String>>>()
    for ((path, value) in flatVars.entries) {
        val parts = path.split("/").filter { it.isNotEmpty() }
        val group = if (parts.size >= 2) parts[0] else "其他"
        val label = parts.drop(1).joinToString(" · ")
        groups.getOrPut(group) { mutableListOf() }.add(label to value)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E) // 深色背景，类似酒馆风格
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "📊 当前状态",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC4B5FD)
            )
            Spacer(Modifier.height(8.dp))

            for ((group, items) in groups) {
                // 分组标题（只显示中文名）
                val groupLabel = group
                Text(
                    groupLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B7EC8)
                )
                Spacer(Modifier.height(3.dp))

                // 变量行
                val rows = items.chunked(2)
                for (row in rows) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for ((label, value) in row) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    label,
                                    fontSize = 10.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                                Text(
                                    value,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE5E7EB)
                                )
                            }
                        }
                        // 填补奇数行
                        if (row.size < 2) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
