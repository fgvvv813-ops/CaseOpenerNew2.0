package com.caseopener.newtwo

import android.content.Context
import android.os.Bundle
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

private val Bg = Color(0xFF0B101A)
private val Panel = Color(0xFF172233)
private val Accent = Color(0xFF66E3C4)
private val Gold = Color(0xFFF6BF55)
private val Muted = Color(0xFFBBC5D3)

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
            "Нумерация версий, начиная с 0.0.001; текущий выпуск — 0.0.002.",
            "Кнопка «Что добавлено в обновлении?» и окно с изменениями текущей версии.",
            "Версия приложения на главном экране и в профиле."
        ),
        improved = listOf(
            "Адаптация интерфейса для узких экранов, планшетов и горизонтального режима.",
            "Двухколоночное расположение кейсов на широких экранах.",
            "Сетка инвентаря на планшетах и прокрутка длинных списков.",
            "Сохранение награды сразу при открытии кейса: поворот экрана не теряет предмет.",
            "Автоматическая публикация APK с номером версии в GitHub Releases."
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

            var tab by rememberSaveable { mutableIntStateOf(0) }
            var selectedCase by remember { mutableStateOf<CaseInfo?>(null) }
            var winningPrize by remember { mutableStateOf<Prize?>(null) }
            var opening by remember { mutableStateOf(false) }
            var showReleaseNotes by rememberSaveable { mutableStateOf(false) }

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
                                inventory = inventory
                            )
                        }
                    }
                }

                if (showReleaseNotes) {
                    WhatsNewDialog(onDismiss = { showReleaseNotes = false })
                }
            }
        }
    }
}

@Composable
private fun Header(balance: Int, compact: Boolean, pad: androidx.compose.ui.unit.Dp) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = pad, vertical = if (compact) 10.dp else 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "CASEOPENER", color = Color.White, fontWeight = FontWeight.Black,
                fontSize = if (compact) 18.sp else 24.sp, maxLines = 1
            )
            Text(
                "NEW 2.0  •  v" + BuildConfig.VERSION_NAME,
                color = Accent, fontSize = if (compact) 10.sp else 12.sp, maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("БАЛАНС", color = Muted, fontSize = 10.sp)
            Text(
                "🪙 $balance", color = Gold,
                fontWeight = FontWeight.Bold, fontSize = if (compact) 17.sp else 21.sp,
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
        Modifier.fillMaxWidth().padding(horizontal = padding),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
    ) {
        listOf("Кейсы", "Инвентарь", "Профиль").forEachIndexed { index, label ->
            Button(
                onClick = { onTabClick(index) },
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == index) Accent else Panel,
                    contentColor = if (selectedTab == index) Bg else Color.White
                )
            ) {
                Text(label, fontSize = if (compact) 11.sp else 13.sp, maxLines = 1)
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
        title = {
            Column {
                Text("Что нового?", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Версия $version", color = Accent, fontSize = 14.sp)
            }
        },
        text = {
            Column(
                Modifier.heightIn(max = 370.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (notes == null) {
                    Text("Для версии $version список изменений пока не опубликован.", color = Muted)
                } else {
                    Text("ДОБАВЛЕНО", fontWeight = FontWeight.Bold, color = Accent)
                    notes.added.forEach { Text("• $it", color = Color.White, fontSize = 14.sp) }
                    Spacer(Modifier.height(7.dp))
                    Text("УЛУЧШЕНО", fontWeight = FontWeight.Bold, color = Gold)
                    notes.improved.forEach { Text("• $it", color = Color.White, fontSize = 14.sp) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Понятно") }
        },
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
    Column {
        Text("ОТКРЫВАЙ. СОБИРАЙ. УЛУЧШАЙ.", fontSize = 14.sp,
            fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(5.dp))
        Text("Коллекционируй предметы и ищи легендарные награды.",
            fontSize = 12.sp, color = Muted)
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF213D45) else Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (compact) 11.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(info.emoji, fontSize = if (compact) 35.sp else 43.sp)
            Spacer(Modifier.width(if (compact) 8.dp else 14.dp))
            Column(Modifier.weight(1f)) {
                Text(info.name, fontSize = if (compact) 16.sp else 19.sp, fontWeight = FontWeight.Black)
                Text(info.subtitle, fontSize = 11.sp, color = Muted, maxLines = 2)
                Spacer(Modifier.height(3.dp))
                Text("${info.cost} 🪙", color = Gold, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(5.dp))
            Button(
                enabled = enabled,
                onClick = onSelect,
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
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (opening) "✨" else prize?.emoji ?: choice.emoji, fontSize = 65.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (opening) "Открываем кейс…" else prize?.name ?: choice.name,
                fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                color = if (opening) Color.White else prize?.color ?: Color.White
            )
            Spacer(Modifier.height(7.dp))
            Text(
                if (opening) "Подбираем награду…" else prize?.let { "${it.rarity} • ${it.value} монет" }
                    ?: "Стоимость: ${choice.cost} монет",
                color = Muted, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !opening && balance >= choice.cost,
                onClick = onOpen, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(if (opening) "ОТКРЫТИЕ…" else "ОТКРЫТЬ ЗА ${choice.cost} 🪙")
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
                    shape = RoundedCornerShape(14.dp)
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
    inventory: List<Prize>
) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = padding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 660.dp)) {
            Text("ПРОФИЛЬ ИГРОКА", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileStatistic("🎯  Открыто кейсов", openings.toString())
                    HorizontalDivider(color = Color(0xFF344154))
                    ProfileStatistic("🎒  Предметов", inventory.size.toString())
                    HorizontalDivider(color = Color(0xFF344154))
                    ProfileStatistic("🪙  Баланс", balance.toString())
                    HorizontalDivider(color = Color(0xFF344154))
                    ProfileStatistic("💎  Стоимость инвентаря", inventory.sumOf { it.value }.toString())
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Версия " + BuildConfig.VERSION_NAME + "  •  Офлайн  •  Виртуальная валюта",
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
