package com.caseopener.newtwo

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val Bg = Color(0xFF0A0713)
private val Panel = Color(0xFF1B1429)
private val Accent = Color(0xFFC177FF)
private val Gold = Color(0xFFFFD079)
private val Muted = Color(0xFFC6BDD2)

private data class CaseInfo(val name: String, val cost: Int, val emoji: String, val subtitle: String)
private data class Prize(val name: String, val rarity: String, val value: Int, val emoji: String, val color: Color)
private data class ReleaseNotes(val added: List<String>, val improved: List<String>)

private val cases = listOf(
    CaseInfo("STREET", 100, "📦", "Стартовая коллекция"),
    CaseInfo("NEON", 250, "💠", "Яркие редкие предметы"),
    CaseInfo("LEGEND", 500, "👑", "Шанс на легенду")
)

// В интерфейсе показываются изменения только версии, установленной на устройстве.
// Для будущей версии добавьте новую запись, а старую не используйте как запасную.
private val releaseNotesByVersion = mapOf(
    "0.0.002" to ReleaseNotes(
        added = listOf(
            "Добавлена кнопка «Что нового?» с описанием изменений.",
            "Номер версии теперь виден в игре."
        ),
        improved = listOf(
            "Игра удобнее на маленьких экранах и планшетах.",
            "Улучшена прокрутка инвентаря и расположение кейсов.",
            "Повышена надёжность сохранения полученных наград."
        )
    ),
    "0.0.003" to ReleaseNotes(
        added = listOf(
            "Новая иконка игры с фиолетовым кейсом.",
            "Кнопки «Сохранить прогресс» и «Восстановить прогресс» в профиле."
        ),
        improved = listOf(
            "Полностью обновлены цвета, карточки кейсов и оформление главного экрана.",
            "Стали удобнее навигация, инвентарь, профиль и окно обновлений.",
            "Улучшено сохранение баланса, коллекции и статистики."
        )
    )
)

private fun rarityColor(name: String): Color = when (name) {
    "Редкий" -> Color(0xFF55A8FF)
    "Эпический" -> Color(0xFFC38BFF)
    "Мифический" -> Color(0xFFFF729E)
    "Легендарный" -> Gold
    else -> Color(0xFFB0BEC5)
}

private fun generatePrize(case: CaseInfo): Prize {
    val rarity = when (Random.nextInt(1000)) {
        in 0..499 -> "Обычный"
        in 500..779 -> "Редкий"
        in 780..929 -> "Эпический"
        in 930..989 -> "Мифический"
        else -> "Легендарный"
    }
    val multiplier = when (rarity) {
        "Обычный" -> 0.30
        "Редкий" -> 0.75
        "Эпический" -> 1.65
        "Мифический" -> 3.5
        else -> 12.0
    }
    val names = when (rarity) {
        "Обычный" -> listOf("Carbon", "Dust", "Urban")
        "Редкий" -> listOf("Pulse", "Glacier", "Electric")
        "Эпический" -> listOf("Inferno", "Nebula", "Phantom")
        "Мифический" -> listOf("Dragon", "Eclipse", "Spectrum")
        else -> listOf("Royal Crown", "Solar Flare", "Eternal")
    }
    val symbol = when (rarity) {
        "Обычный" -> "⚙️"
        "Редкий" -> "🔷"
        "Эпический" -> "🔮"
        "Мифический" -> "🔥"
        else -> "👑"
    }
    val price = (case.cost * multiplier * (0.8 + Random.nextDouble() * 0.4)).toInt().coerceAtLeast(1)
    return Prize("${names.random()} • ${case.name}", rarity, price, symbol, rarityColor(rarity))
}

private fun readInventory(saved: String): List<Prize> = saved
    .split(";")
    .filter { it.isNotBlank() }
    .mapNotNull { raw ->
        val parts = raw.split("|")
        val price = parts.getOrNull(2)?.toIntOrNull()
        if (parts.size != 3 || price == null) null
        else Prize(parts[0], parts[1], price, "✦", rarityColor(parts[1]))
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("caseopener_save", Context.MODE_PRIVATE)
        setContent {
            var balance by remember { mutableIntStateOf(prefs.getInt("balance", 1500)) }
            var openings by remember { mutableIntStateOf(prefs.getInt("openings", 0)) }
            val inventory = remember {
                mutableStateListOf<Prize>().also { it.addAll(readInventory(prefs.getString("items", "").orEmpty())) }
            }
            fun saveProgress() {
                prefs.edit()
                    .putInt("balance", balance)
                    .putInt("openings", openings)
                    .putString("items", inventory.joinToString(";") { "${it.name}|${it.rarity}|${it.value}" })
                    .apply()
            }

            // Резервное копирование позволяет переносить прогресс при переустановке.
            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    try {
                        val items = JSONArray()
                        inventory.forEach { prize ->
                            items.put(
                                JSONObject().put("name", prize.name).put("rarity", prize.rarity)
                                    .put("value", prize.value)
                            )
                        }
                        val backup = JSONObject()
                            .put("format", 1)
                            .put("balance", balance)
                            .put("openings", openings)
                            .put("items", items)
                        val bytes = backup.toString(2).toByteArray(Charsets.UTF_8)
                        val stream = this@MainActivity.contentResolver.openOutputStream(uri)
                            ?: error("Файл не доступен")
                        stream.use { it.write(bytes) }
                        Toast.makeText(this@MainActivity, "Прогресс сохранён в файл", Toast.LENGTH_LONG).show()
                    } catch (ex: Exception) {
                        Toast.makeText(this@MainActivity, "Не удалось сохранить файл: ${ex.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    try {
                        val stream = this@MainActivity.contentResolver.openInputStream(uri)
                            ?: error("Файл не доступен")
                        val text = stream.bufferedReader().use { it.readText() }
                        require(text.length <= 1_000_000) { "Слишком большой файл" }
                        val backup = JSONObject(text)
                        require(backup.getInt("format") == 1) { "Неподдерживаемый формат" }
                        val newBalance = backup.getInt("balance")
                        val newOpenings = backup.getInt("openings")
                        require(newBalance in 0..1000000000 && newOpenings in 0..1000000000) {
                            "Некорректные данные"
                        }
                        val rawItems = backup.getJSONArray("items")
                        require(rawItems.length() <= 5000) { "Слишком много предметов" }
                        val restored = (0 until rawItems.length()).map { index ->
                            val item = rawItems.getJSONObject(index)
                            val name = item.getString("name")
                            val rarity = item.getString("rarity")
                            val value = item.getInt("value")
                            require(name.length <= 100 && name.isNotBlank() && value in 1..1000000000 &&
                                !name.contains("|") && !name.contains(";") &&
                                !rarity.contains("|") && !rarity.contains(";")) {
                                "Некорректные данные предмета"
                            }
                            Prize(name, rarity, value, "✦", rarityColor(rarity))
                        }
                        balance = newBalance
                        openings = newOpenings
                        inventory.clear()
                        inventory.addAll(restored)
                        saveProgress()
                        Toast.makeText(this@MainActivity, "Прогресс восстановлен", Toast.LENGTH_LONG).show()
                    } catch (ex: Exception) {
                        Toast.makeText(this@MainActivity, "Не удалось восстановить: ${ex.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            var tab by rememberSaveable { mutableIntStateOf(0) }
            var selectedCase by remember { mutableStateOf<CaseInfo?>(null) }
            var winningPrize by remember { mutableStateOf<Prize?>(null) }
            var opening by remember { mutableStateOf(false) }
            var showReleaseNotes by rememberSaveable { mutableStateOf(false) }
            var showImportConfirmation by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(opening) {
                if (opening) {
                    delay(1500)
                    opening = false
                }
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent, secondary = Gold, background = Bg, surface = Panel,
                    onBackground = Color.White, onSurface = Color.White
                )
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()
                ) {
                    val wide = maxWidth >= 720.dp
                    val compact = maxWidth < 370.dp
                    val pad = if (compact) 12.dp else 18.dp

                    Column(
                        Modifier.fillMaxSize().widthIn(max = 1160.dp).align(Alignment.TopCenter)
                    ) {
                        Header(balance = balance, compact = compact, pad = pad)
                        NavigationTabs(
                            selectedTab = tab,
                            compact = compact,
                            padding = pad,
                            onTabClick = { tab = it }
                        )
                        TextButton(
                            onClick = { showReleaseNotes = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                .padding(horizontal = pad),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Что добавлено в обновлении?  •  v" + BuildConfig.VERSION_NAME,
                                fontSize = if (compact) 11.sp else 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        when (tab) {
                            0 -> CaseScreen(
                                modifier = Modifier.weight(1f),
                                wide = wide,
                                compact = compact,
                                padding = pad,
                                balance = balance,
                                chosen = selectedCase,
                                winning = winningPrize,
                                opening = opening,
                                onSelect = {
                                    if (!opening) {
                                        selectedCase = it
                                        winningPrize = null
                                    }
                                },
                                onOpen = {
                                    val choice = selectedCase
                                    if (!opening && choice != null && balance >= choice.cost) {
                                        opening = true
                                        winningPrize = generatePrize(choice)
                                        balance -= choice.cost
                                        inventory.add(0, winningPrize!!)
                                        openings++
                                        // Сохраняем ДО анимации — поворот экрана не потеряет награду.
                                        saveProgress()
                                    }
                                }
                            )
                            1 -> InventoryScreen(
                                modifier = Modifier.weight(1f),
                                padding = pad,
                                wide = wide,
                                inventory = inventory,
                                onSell = { prize ->
                                    if (inventory.remove(prize)) {
                                        balance += prize.value
                                        saveProgress()
                                    }
                                }
                            )
                            else -> ProfileScreen(
                                modifier = Modifier.weight(1f),
                                padding = pad,
                                balance = balance,
                                openings = openings,
                                inventory = inventory,
                                onBackup = { exportLauncher.launch("CaseOpener-progress.json") },
                                onRestore = { showImportConfirmation = true }
                            )
                        }
                    }
                }

                if (showReleaseNotes) {
                    WhatsNewDialog(onDismiss = { showReleaseNotes = false })
                }
                if (showImportConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showImportConfirmation = false },
                        containerColor = Panel,
                        title = { Text("Восстановить прогресс?") },
                        text = {
                            Text("Текущий баланс, инвентарь и статистика будут заменены данными из выбранного файла.")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showImportConfirmation = false
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }) { Text("Выбрать файл") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImportConfirmation = false }) { Text("Отмена") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(balance: Int, compact: Boolean, pad: androidx.compose.ui.unit.Dp) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = pad, vertical = if (compact) 10.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = "Иконка CaseOpener",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(if (compact) 42.dp else 56.dp)
                .clip(RoundedCornerShape(15.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(
                "CASEOPENER", color = Color.White, fontWeight = FontWeight.Black,
                fontSize = if (compact) 17.sp else 24.sp, maxLines = 1
            )
            Text(
                "NEW 2.0  •  v" + BuildConfig.VERSION_NAME,
                color = Accent, fontSize = if (compact) 10.sp else 12.sp, maxLines = 1
            )
        }
        Column(
            Modifier.clip(RoundedCornerShape(15.dp))
                .background(Panel).padding(horizontal = if (compact) 9.dp else 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text("БАЛАНС", color = Muted, fontSize = 9.sp)
            Text(
                "🪙 $balance", color = Gold,
                fontWeight = FontWeight.Bold, fontSize = if (compact) 15.sp else 20.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NavigationTabs(
    selectedTab: Int,
    compact: Boolean,
    padding: androidx.compose.ui.unit.Dp,
    onTabClick: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = padding)
            .clip(RoundedCornerShape(19.dp))
            .background(Panel).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("Кейсы", "Инвентарь", "Профиль").forEachIndexed { index, label ->
            val selected = selectedTab == index
            Button(
                onClick = { onTabClick(index) },
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 7.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) Accent else Color.Transparent,
                    contentColor = if (selected) Bg else Muted
                ),
                elevation = null
            ) {
                Text(label, fontSize = if (compact) 11.sp else 13.sp, maxLines = 1,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun WhatsNewDialog(onDismiss: () -> Unit) {
    val version = BuildConfig.VERSION_NAME
    val notes = releaseNotesByVersion[version]
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("✨ Что нового?", fontWeight = FontWeight.Black, color = Color.White, fontSize = 22.sp)
                Text("ОБНОВЛЕНИЕ  v$version", color = Accent,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (notes == null) {
                    Text("Для версии $version список изменений пока не опубликован.", color = Muted)
                } else {
                    Text("✦ ДОБАВЛЕНО", fontWeight = FontWeight.Bold, color = Accent)
                    notes.added.forEach { Text("• $it", color = Color.White, fontSize = 14.sp) }
                    Spacer(Modifier.height(7.dp))
                    Text("✦ УЛУЧШЕНО", fontWeight = FontWeight.Bold, color = Gold)
                    notes.improved.forEach { Text("• $it", color = Color.White, fontSize = 14.sp) }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Понятно") } },
        containerColor = Panel,
        titleContentColor = Color.White
    )
}

@Composable
private fun CaseScreen(
    modifier: Modifier,
    wide: Boolean,
    compact: Boolean,
    padding: androidx.compose.ui.unit.Dp,
    balance: Int,
    chosen: CaseInfo?,
    winning: Prize?,
    opening: Boolean,
    onSelect: (CaseInfo) -> Unit,
    onOpen: () -> Unit
) {
    if (wide) {
        Row(
            modifier.fillMaxWidth().padding(horizontal = padding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            LazyColumn(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item { SectionIntro() }
                items(cases) { info ->
                    CaseTile(info, selected = info == chosen, compact = false, enabled = !opening, onSelect = { onSelect(info) })
                }
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
            ) {
                if (chosen == null) EmptySelection()
                else OpeningPanel(chosen, winning, opening, balance, onOpen)
            }
        }
    } else {
        LazyColumn(
            modifier.fillMaxWidth().padding(horizontal = padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            item { SectionIntro() }
            if (chosen != null) {
                item { OpeningPanel(chosen, winning, opening, balance, onOpen) }
            }
            items(cases) { info ->
                CaseTile(info, selected = info == chosen, compact = compact, enabled = !opening, onSelect = { onSelect(info) })
            }
        }
    }
}

@Composable
private fun SectionIntro() {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF361653), Color(0xFF21132E))))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ТВОЯ КОЛЛЕКЦИЯ НАЧИНАЕТСЯ ЗДЕСЬ",
                    fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text("Открывай кейсы. Находи редкие предметы. Собирай легенды.",
                    fontSize = 12.sp, color = Muted)
            }
            Spacer(Modifier.width(8.dp))
            Text("✦", fontSize = 38.sp, color = Accent)
        }
    }
}

@Composable
private fun EmptySelection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎁", fontSize = 54.sp)
            Spacer(Modifier.height(12.dp))
            Text("Выбери кейс слева", fontWeight = FontWeight.Bold, color = Color.White)
            Text("Здесь появится открытие и полученная награда",
                fontSize = 13.sp, color = Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CaseTile(
    info: CaseInfo,
    selected: Boolean,
    compact: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .then(if (selected) Modifier.border(1.dp, Accent, RoundedCornerShape(18.dp)) else Modifier),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF2A1A42) else Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (compact) 11.dp else 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(if (compact) 54.dp else 65.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF472268), Color(0xFF1B102E)))),
                contentAlignment = Alignment.Center
            ) {
                Text(info.emoji, fontSize = if (compact) 32.sp else 38.sp)
            }
            Spacer(Modifier.width(if (compact) 9.dp else 14.dp))
            Column(Modifier.weight(1f)) {
                Text(info.name, fontSize = if (compact) 16.sp else 19.sp, fontWeight = FontWeight.Black)
                Text(info.subtitle, fontSize = 11.sp, color = Muted, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text("${info.cost} 🪙", color = Gold, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(5.dp))
            Button(
                enabled = enabled,
                onClick = onSelect,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 12.dp),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(if (selected) "ВЫБРАН" else "ВЫБРАТЬ", fontSize = if (compact) 10.sp else 11.sp)
            }
        }
    }
}

@Composable
private fun OpeningPanel(
    choice: CaseInfo,
    prize: Prize?,
    opening: Boolean,
    balance: Int,
    onOpen: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 140.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF4A216D), Color(0xFF1A102B)))),
                contentAlignment = Alignment.Center
            ) {
                Text(if (opening) "✨" else prize?.emoji ?: choice.emoji, fontSize = 76.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (opening) "Открываем кейс…" else prize?.name ?: choice.name,
                fontSize = 21.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                color = if (opening) Color.White else prize?.color ?: Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (opening) "Удача уже определила награду…" else prize?.let { "${it.rarity} • ${it.value} монет" }
                    ?: "Стоимость: ${choice.cost} монет",
                color = Muted, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Button(
                enabled = !opening && balance >= choice.cost,
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (opening) "ОТКРЫТИЕ…" else "ОТКРЫТЬ ЗА ${choice.cost} 🪙",
                    fontWeight = FontWeight.Bold)
            }
            if (!opening && balance < choice.cost) {
                Spacer(Modifier.height(8.dp))
                Text("Не хватает монет — продай предметы в инвентаре.",
                    color = Gold, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun InventoryScreen(
    modifier: Modifier,
    padding: androidx.compose.ui.unit.Dp,
    wide: Boolean,
    inventory: List<Prize>,
    onSell: (Prize) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = if (wide) 280.dp else 320.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = padding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("ИНВЕНТАРЬ • ${inventory.size} ПРЕДМЕТОВ",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Продавай предметы, чтобы открывать новые кейсы.",
                    fontSize = 12.sp, color = Muted)
            }
        }
        if (inventory.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Здесь пока пусто. Открой свой первый кейс!",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center, color = Muted)
            }
        } else {
            items(inventory.size) { index ->
                val prize = inventory[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(prize.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(prize.name, fontWeight = FontWeight.Bold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            Text(prize.rarity, color = prize.color, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(5.dp))
                        Button(
                            onClick = { onSell(prize) },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.heightIn(min = 42.dp)
                        ) {
                            Text("+${prize.value} 🪙", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    modifier: Modifier,
    padding: androidx.compose.ui.unit.Dp,
    balance: Int,
    openings: Int,
    inventory: List<Prize>,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = padding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 660.dp)) {
            Text("МОЙ ПРОФИЛЬ", fontWeight = FontWeight.Black, fontSize = 21.sp)
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileStatistic("🎯  Открыто кейсов", openings.toString())
                    HorizontalDivider(color = Color(0xFF3A294C))
                    ProfileStatistic("🎒  Предметов", inventory.size.toString())
                    HorizontalDivider(color = Color(0xFF3A294C))
                    ProfileStatistic("🪙  Баланс", balance.toString())
                    HorizontalDivider(color = Color(0xFF3A294C))
                    ProfileStatistic("💎  Стоимость инвентаря", inventory.sumOf { it.value }.toString())
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("СОХРАНЕНИЕ ПРОГРЕССА", fontWeight = FontWeight.Bold, color = Accent)
            Spacer(Modifier.height(5.dp))
            Text("Монеты, предметы и статистика сохраняются автоматически. Можно создать резервную копию для переноса или восстановления.",
                color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onBackup, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("⬇  Сохранить прогресс", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onRestore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("⬆  Восстановить из файла", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(15.dp))
            Text("Версия " + BuildConfig.VERSION_NAME + "  •  Офлайн",
                color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProfileStatistic(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
