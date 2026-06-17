package com.agentapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.data.model.Character
import com.agentapp.ui.theme.Pink
import com.agentapp.ui.theme.PinkDark
import com.agentapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(character: Character, onSave: (Character) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(character.name) }
    var description by remember { mutableStateOf(character.description) }
    var personality by remember { mutableStateOf(character.personality) }
    var scenario by remember { mutableStateOf(character.scenario) }
    var greeting by remember { mutableStateOf(character.greeting) }
    var systemPrompt by remember { mutableStateOf(character.systemPrompt) }
    var worldBookEnabled by remember { mutableStateOf(character.worldBookEnabled) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var avatarUri by remember { mutableStateOf(character.avatarUri) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val dir = java.io.File(context.filesDir, "avatars").also { it.mkdirs() }
                val file = java.io.File(dir, "${character.id}_${System.currentTimeMillis()}.png")
                inputStream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                avatarUri = file.absolutePath
            } catch (_: Exception) { }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Pink,
        unfocusedBorderColor = Color(0xFFE8DDE8),
        cursorColor = Pink
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("♡ ${if (character.name.isEmpty()) "新建角色" else "编辑角色"}", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onCancel) { Text("取消", color = Pink) } },
                actions = {
                    TextButton(onClick = {
                        onSave(character.copy(name = name, description = description, personality = personality, scenario = scenario, greeting = greeting, systemPrompt = systemPrompt, worldBookEnabled = worldBookEnabled, avatarUri = avatarUri))
                    }) { Text("保存", color = PinkDark, fontWeight = FontWeight.Bold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))

            // 可点击的头像
            Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(com.agentapp.ui.theme.AvatarColors[(character.id.hashCode() and Int.MAX_VALUE) % com.agentapp.ui.theme.AvatarColors.size])
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    if (avatarUri.isNotEmpty()) {
                        val bmp = android.graphics.BitmapFactory.decodeFile(avatarUri)
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp).clip(CircleShape)
                            )
                        } else {
                            Text(character.name.firstOrNull()?.toString() ?: "?",
                                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Text(character.name.firstOrNull()?.toString() ?: "?",
                            fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                // 小相机图标
                Box(Modifier.align(androidx.compose.ui.Alignment.BottomEnd).offset(x = (-4).dp, y = 0.dp)) {
                    Box(Modifier.size(28.dp).background(Color.White, CircleShape), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("📷", fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名字") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, shape = RoundedCornerShape(16.dp), colors = fieldColors)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = personality, onValueChange = { personality = it }, label = { Text("性格") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, shape = RoundedCornerShape(16.dp), colors = fieldColors)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = scenario, onValueChange = { scenario = it }, label = { Text("场景") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, shape = RoundedCornerShape(16.dp), colors = fieldColors)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = greeting, onValueChange = { greeting = it }, label = { Text("开场白") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, shape = RoundedCornerShape(16.dp), colors = fieldColors)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = systemPrompt, onValueChange = { systemPrompt = it }, label = { Text("自定义 System Prompt（可选）") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6, shape = RoundedCornerShape(16.dp), colors = fieldColors)

            Spacer(Modifier.height(12.dp))

            // 世界书总开关
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📖 启用世界书", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                androidx.compose.material3.Switch(checked = worldBookEnabled, onCheckedChange = { worldBookEnabled = it })
            }

            Spacer(Modifier.height(24.dp))

            Button(onClick = { onSave(character.copy(name = name, description = description, personality = personality, scenario = scenario, greeting = greeting, systemPrompt = systemPrompt, worldBookEnabled = worldBookEnabled, avatarUri = avatarUri)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkDark)
            ) { Text("✨ 保存角色", fontWeight = FontWeight.Bold, color = Color.White) }

            Spacer(Modifier.height(32.dp))
        }
    }
}
