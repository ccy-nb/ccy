package com.agentapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentapp.ui.theme.TextGray
import com.agentapp.viewmodel.SettingsViewModel
import com.agentapp.ui.screens.PresetEditDialog
import com.agentapp.viewmodel.WorldBookViewModel

private val CardShape = RoundedCornerShape(20.dp)

data class SettingsItem(
    val icon: ImageVector,
    val label: String,
    val desc: String
)

private val settingsItems = listOf(
    SettingsItem(Icons.Default.Wifi, "API 连接", "配置模型提供商、API Key、模型参数"),
    SettingsItem(Icons.Default.Person, "我的身份", "设置角色名、描述信息"),
    SettingsItem(Icons.Default.Tune, "预设", "管理采样参数预设"),
    SettingsItem(Icons.Default.Palette, "主题", "切换浅色/深色/跟随系统"),
    SettingsItem(Icons.Default.Book, "世界书", "管理世界观设定"),
    SettingsItem(Icons.Default.Face, "角色管理", "新建/编辑/删除角色"),
    SettingsItem(Icons.Default.Code, "正则脚本", "管理 AI 回复替换规则"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    characterListViewModel: com.agentapp.viewmodel.CharacterListViewModel? = null,
    onNavigateToWorldBook: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var currentPage by remember { mutableStateOf("menu") }

    val pageKeys = listOf("api", "persona", "presets", "theme", "worldbook", "characters", "regex")

    when (currentPage) {
        "menu" -> SettingsMenu(
            onBack = onBack,
            onNavigate = { currentPage = it }
        )
        "api" -> ApiSettingsPage(
            settingsViewModel = settingsViewModel,
            onBack = { currentPage = "menu" }
        )
        "persona" -> PersonaSettingsPage(
            settingsViewModel = settingsViewModel,
            onBack = { currentPage = "menu" }
        )
        "presets" -> PresetSettingsPage(
            settingsViewModel = settingsViewModel,
            onBack = { currentPage = "menu" }
        )
        "theme" -> ThemeSettingsPage(
            settingsViewModel = settingsViewModel,
            onBack = { currentPage = "menu" }
        )
        "worldbook" -> {
            onNavigateToWorldBook()
        }
        "characters" -> {
            if (characterListViewModel != null) {
                CharactersSettingsPage(
                    characterListViewModel = characterListViewModel,
                    onBack = { currentPage = "menu" }
                )
            } else {
                currentPage = "menu"
            }
        }
        "regex" -> RegexSettingsPage(onBack = { currentPage = "menu" })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMenu(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val pageKeys = listOf("api", "persona", "presets", "theme", "worldbook", "characters", "regex")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            settingsItems.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onNavigate(pageKeys[index]) },
                    shape = CardShape,
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(item.icon, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(item.desc, fontSize = 12.sp, color = TextGray)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ========= 各设置子页面占位 =========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiSettingsPage(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val config by settingsViewModel.config.collectAsState()
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }
    val testing by settingsViewModel.testing.collectAsState()
    val testResult by settingsViewModel.testResult.collectAsState()
    val testSuccess by settingsViewModel.testSuccess.collectAsState()
    val availableModels by settingsViewModel.availableModels.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 连接", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            SettingsApiContent(
                config = config,
                testing = testing,
                testResult = testResult,
                testSuccess = testSuccess,
                availableModels = availableModels,
                providerExpanded = providerExpanded,
                modelExpanded = modelExpanded,
                showKey = showKey,
                onProviderExpandedChange = { providerExpanded = it },
                onModelExpandedChange = { modelExpanded = it },
                onShowKeyChange = { showKey = it },
                onUpdateConfig = { settingsViewModel.updateConfig(it) },
                onTestConnection = { settingsViewModel.testConnection() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonaSettingsPage(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val persona by settingsViewModel.persona.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的身份", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            SettingsPersonaContent(persona = persona, onSave = { settingsViewModel.updatePersona { it }; settingsViewModel.save() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetSettingsPage(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val presets by settingsViewModel.presets.collectAsState()
    var presetEditDialog by remember { mutableStateOf<com.agentapp.data.model.Preset?>(null) }

    if (presetEditDialog != null) {
        com.agentapp.ui.screens.PresetEditDialog(
            preset = presetEditDialog!!,
            onSave = { settingsViewModel.savePreset(it); presetEditDialog = null },
            onDismiss = { presetEditDialog = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预设", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { presetEditDialog = com.agentapp.data.model.Preset() }) {
                        Icon(Icons.Default.Add, contentDescription = "新建", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            SettingsPresetContent(presets = presets, onEdit = { presetEditDialog = it }, onDelete = { settingsViewModel.deletePreset(it) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharactersSettingsPage(
    characterListViewModel: com.agentapp.viewmodel.CharacterListViewModel,
    onBack: () -> Unit
) {
    val characters by characterListViewModel.characters.collectAsState()
    var showEdit by remember { mutableStateOf(false) }
    var editingCharacter by remember { mutableStateOf<com.agentapp.data.model.Character?>(null) }
    val exportCtx = androidx.compose.ui.platform.LocalContext.current

    if (showEdit) {
        com.agentapp.ui.screens.CharacterEditScreen(
            character = editingCharacter ?: com.agentapp.data.model.Character(),
            onSave = { characterListViewModel.saveCharacter(it); showEdit = false; editingCharacter = null },
            onCancel = { showEdit = false; editingCharacter = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("角色管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { editingCharacter = null; showEdit = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新建角色")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (characters.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无角色", fontSize = 16.sp, color = TextGray)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { editingCharacter = null; showEdit = true }) {
                        Text("＋ 新建角色", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                items(characters, key = { it.id }) { char ->
                    val initial = char.name.firstOrNull()?.toString() ?: "?"
                    val colorIdx = kotlin.math.abs(char.name.hashCode()) % com.agentapp.ui.theme.AvatarColors.size

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(1.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .background(com.agentapp.ui.theme.AvatarColors[colorIdx], androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(char.name.ifEmpty { "未命名" }, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                if (char.description.isNotBlank()) {
                                    Text(char.description.take(50), fontSize = 12.sp, color = TextGray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                            TextButton(onClick = { editingCharacter = char; showEdit = true }) {
                                Text("编辑", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = {
                                val repo = com.agentapp.data.repository.CharacterRepository(exportCtx.applicationContext)
                                repo.shareCharacter(exportCtx, char)
                            }) {
                                Text("导出", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            TextButton(onClick = { characterListViewModel.deleteCharacter(char.id) }) {
                                Text("删除", fontSize = 13.sp, color = com.agentapp.ui.theme.PinkDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSettingsPage(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("主题", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            SettingsThemeContent(selectedTheme = selectedTheme, onSelect = { settingsViewModel.setSelectedTheme(it); settingsViewModel.save() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegexSettingsPage(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { com.agentapp.data.repository.RegexRepository(ctx.applicationContext) }
    var scripts by remember { mutableStateOf<List<com.agentapp.data.model.RegexScript>>(emptyList()) }
    var editingScript by remember { mutableStateOf<com.agentapp.data.model.RegexScript?>(null) }
    val scope = rememberCoroutineScope()

    // 导出正则脚本
    fun exportRegexScripts() {
        val json = kotlinx.serialization.json.Json { prettyPrint = true }
        val serializer = kotlinx.serialization.builtins.ListSerializer(com.agentapp.data.model.RegexScript.serializer())
        val jsonStr = json.encodeToString(serializer, scripts)
        try {
            val file = java.io.File(ctx.cacheDir, "regex_scripts_${System.currentTimeMillis()}.json")
            file.writeText(jsonStr)
            val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(android.content.Intent.createChooser(intent, "导出正则脚本"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(ctx, "导出失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 导入正则脚本（从 JSON 文本解析）
    fun importRegexScriptsFromJson(text: String) {
        scope.launch {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val imported = json.decodeFromString<List<com.agentapp.data.model.RegexScript>>(text)
                imported.forEach { repo.saveOne(it) { it.id } }
                scripts = repo.list()
                android.widget.Toast.makeText(ctx, "✅ 导入 ${imported.size} 条正则脚本", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // 尝试单个
                try {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val single = json.decodeFromString<com.agentapp.data.model.RegexScript>(text)
                    repo.saveOne(single) { it.id }
                    scripts = repo.list()
                    android.widget.Toast.makeText(ctx, "✅ 导入 1 条正则脚本", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e2: Exception) {
                    android.widget.Toast.makeText(ctx, "❌ 导入失败: ${e2.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 文件选择器：导入正则脚本 JSON
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val inputStream = ctx.contentResolver.openInputStream(uri)
                    val text = inputStream?.bufferedReader()?.readText() ?: ""
                    inputStream?.close()
                    if (text.isNotBlank()) {
                        importRegexScriptsFromJson(text)
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(ctx, "❌ 读取文件失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (editingScript != null) {
        val s = editingScript!!
        var name by remember(s) { mutableStateOf(s.name) }
        var findRegex by remember(s) { mutableStateOf(s.findRegex) }
        var replaceString by remember(s) { mutableStateOf(s.replaceString) }
        AlertDialog(
            onDismissRequest = { editingScript = null },
            title = { Text(if (s.id.isEmpty()) "新建正则" else "编辑正则") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = findRegex, onValueChange = { findRegex = it }, label = { Text("查找 (正则)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = replaceString, onValueChange = { replaceString = it }, label = { Text("替换为") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updated = s.copy(name = name, findRegex = findRegex, replaceString = replaceString)
                    scope.launch { repo.saveOne(updated) { it.id }; scripts = repo.list() }
                    editingScript = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingScript = null }) { Text("取消") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("正则脚本", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    if (scripts.isNotEmpty()) {
                        IconButton(onClick = { exportRegexScripts() }) {
                            Text("📤", fontSize = 16.sp)
                        }
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                        Text("📥", fontSize = 16.sp)
                    }
                    IconButton(onClick = { editingScript = com.agentapp.data.model.RegexScript() }) {
                        Icon(Icons.Default.Add, contentDescription = "新建")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (scripts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无正则脚本", color = TextGray)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                items(scripts, key = { it.id }) { script ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(script.name.ifEmpty { "未命名" }, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(script.findRegex.take(40), fontSize = 11.sp, color = TextGray, maxLines = 1)
                            }
                            TextButton(onClick = { editingScript = script }) { Text("编辑", fontSize = 13.sp) }
                            TextButton(onClick = {
                                scope.launch {
                                    repo.delete(script.id) { it.id }
                                    scripts = repo.list()
                                }
                            }) { Text("删除", fontSize = 13.sp, color = com.agentapp.ui.theme.PinkDark) }
                        }
                    }
                }
            }
        }
    }
}
