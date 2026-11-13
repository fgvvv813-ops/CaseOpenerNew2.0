package com.caseopener.newtwo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val Bg = Color(0xFF0B101A)
private val Surface = Color(0xFF172233)
private val Accent = Color(0xFF66E3C4)
private val Gold = Color(0xFFF6BF55)

private data class CaseInfo(val name: String, val cost: Int, val emoji: String, val subtitle: String)
private data class Prize(val name: String, val rarity: String, val value: Int, val emoji: String, val color: Color)

private val cases = listOf(
    CaseInfo("STREET", 100, "📦", "Стартовая коллекция"),
    CaseInfo("NEON", 250, "💠", "Яркие редкие предметы"),
    CaseInfo("LEGEND", 500, "👑", "Шанс на легенду")
)

private fun generatePrize(case: CaseInfo): Prize {
    val roll = Random.nextInt(1000)
    val rarity = when {
        roll < 500 -> "Обычный"
        roll < 780 -> "Редкий"
        roll < 930 -> "Эпический"
        roll < 990 -> "Мифический"
        else -> "Легендарный"
    }
    val multiplier = when (rarity) {
        "Обычный" -> 0.30
        "Редкий" -> 0.75
        "Эпический" -> 1.65
        "Мифический" -> 3.5
        else -> 12.0
    }
    val value = (case.cost * multiplier * (0.8 + Random.nextDouble() * 0.4)).toInt().coerceAtLeast(1)
    val color = when (rarity) {
        "Обычный" -> Color(0xFFB0BEC5)
        "Редкий" -> Color(0xFF55A8FF)
        "Эпический" -> Color(0xFFC38BFF)
        "Мифический" -> Color(0xFFFF729E)
        else -> Gold
    }
    val labels = when (rarity) {
        "Обычный" -> listOf("Carbon", "Dust", "Urban")
        "Редкий" -> listOf("Pulse", "Glacier", "Electric")
        "Эпический" -> listOf("Inferno", "Nebula", "Phantom")
        "Мифический" -> listOf("Dragon", "Eclipse", "Spectrum")
        else -> listOf("Royal Crown", "Solar Flare", "Eternal")
    }
    val emoji = when (rarity) {
        "Обычный" -> "⚙️"
        "Редкий" -> "🔷"
        "Эпический" -> "🔮"
        "Мифический" -> "🔥"
        else -> "👑"
    }
    return Prize("${labels.random()} • ${case.name}", rarity, value, emoji, color)
}
private fun rarityColor(name: String): Color = when (name) {
    "Редкий" -> Color(0xFF55A8FF)
    "Эпический" -> Color(0xFFC38BFF)
    "Мифический" -> Color(0xFFFF729E)
    "Легендарный" -> Gold
    else -> Color(0xFFB0BEC5)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("caseopener_save", Context.MODE_PRIVATE)
        setContent {
            var balance by remember { mutableIntStateOf(prefs.getInt("balance", 1500)) }
            var openings by remember { mutableIntStateOf(prefs.getInt("openings", 0)) }
            val inventory = remember {
                mutableStateListOf<Prize>().also { list ->
                    prefs.getString("items", "")!!.split(";").filter { it.isNotBlank() }.forEach { raw ->
                        val parts = raw.split("|")
                        if (parts.size == 3) {
                            val value = parts[2].toIntOrNull()
                            if (value != null) list.add(Prize(parts[0], parts[1], value, "✦", rarityColor(parts[1])))
                        }
                    }
                }
            }
            fun save() {
                prefs.edit().putInt("balance", balance).putInt("openings", openings)
                    .putString("items", inventory.joinToString(";") { "${it.name}|${it.rarity}|${it.value}" })
                    .apply()
            }
            var tab by remember { mutableIntStateOf(0) }
            var currentCase by remember { mutableStateOf<CaseInfo?>(null) }
            var winningPrize by remember { mutableStateOf<Prize?>(null) }
            var opening by remember { mutableStateOf(false) }
            val colors = darkColorScheme(primary = Accent, secondary = Gold, background = Bg, surface = Surface,
                onBackground = Color.White, onSurface = Color.White)
            LaunchedEffect(opening) {
                if (opening) {
                    delay(1500)
                    val selected = currentCase
                    if (selected != null) {
                        val prize = generatePrize(selected)
                        inventory.add(0, prize)
                        openings++
                        winningPrize = prize
                        save()
                    }
                    opening = false
                }
            }
            MaterialTheme(colorScheme = colors) {
                Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("CASEOPENER", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.White)
                            Text("NEW 2.0  •  ALPHA", fontSize = 11.sp, color = Accent)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("БАЛАНС", fontSize = 10.sp, color = Color.LightGray)
                            Text("🪙 $balance", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Gold)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Кейсы", "Инвентарь", "Профиль").forEachIndexed { index, title ->
                            Button(onClick = { tab = index; winningPrize = null; currentCase = null },
                                modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (tab == index) Accent else Surface,
                                    contentColor = if (tab == index) Bg else Color.White)) {
                                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    when (tab) {
                        0 -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)) {
                            item {
                                Text("ОТКРЫВАЙ. СОБИРАЙ. УЛУЧШАЙ.", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White)
                                Spacer(Modifier.height(6.dp))
                                Text("Коллекционируй предметы и ищи легендарные награды.",
                                    fontSize = 12.sp, color = Color.LightGray)
                            }
                            if (currentCase != null) {
                                item {
                                    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(if (opening) "✨" else winningPrize?.emoji ?: currentCase!!.emoji, fontSize = 68.sp)
                                            Spacer(Modifier.height(8.dp))
                                            Text(if (opening) "Открываем кейс…" else winningPrize?.name ?: currentCase!!.name,
                                                fontSize = 21.sp, fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center, color = winningPrize?.color ?: Color.White)
                                            Spacer(Modifier.height(6.dp))
                                            Text(if (opening) "Ищем твою награду" else winningPrize?.let { "${it.rarity} • ${it.value} монет" }
                                                ?: "Стоимость: ${currentCase!!.cost} монет",
                                                color = Color.LightGray, textAlign = TextAlign.Center)
                                            Spacer(Modifier.height(15.dp))
                                            Button(enabled = !opening && balance >= currentCase!!.cost, onClick = {
                                                balance -= currentCase!!.cost
                                                winningPrize = null
                                                save()
                                                opening = true
                                            }, modifier = Modifier.fillMaxWidth()) {
                                                Text(if (opening) "ОТКРЫТИЕ…" else "ОТКРЫТЬ ЗА ${currentCase!!.cost} 🪙")
                                            }
                                            if (!opening && balance < currentCase!!.cost) {
                                                Text("Недостаточно монет — продай предметы в инвентаре.",
                                                    fontSize = 12.sp, color = Gold, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                            }
                            itemsIndexed(cases) { _, case ->
                                Card(colors = CardDefaults.cardColors(containerColor = Surface),
                                    shape = RoundedCornerShape(18.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(case.emoji, fontSize = 48.sp)
                                        Spacer(Modifier.width(14.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(case.name, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                                            Text(case.subtitle, color = Color.LightGray, fontSize = 11.sp)
                                            Spacer(Modifier.height(5.dp))
                                            Text("${case.cost} 🪙", color = Gold, fontWeight = FontWeight.Bold)
                                        }
                                        Button(onClick = {
                                            currentCase = case; winningPrize = null
                                        }, enabled = !opening, contentPadding = PaddingValues(horizontal = 12.dp)) {
                                            Text("ВЫБРАТЬ", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        1 -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)) {
                            item {
                                Text("ИНВЕНТАРЬ • ${inventory.size} ПРЕДМЕТОВ",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                Text("Продавай предметы, чтобы открывать новые кейсы.",
                                    fontSize = 12.sp, color = Color.LightGray)
                            }
                            if (inventory.isEmpty()) item {
                                Text("Здесь пока пусто. Открой свой первый кейс!",
                                    modifier = Modifier.fillMaxWidth().padding(35.dp),
                                    textAlign = TextAlign.Center, color = Color.LightGray)
                            }
                            itemsIndexed(inventory, key = { index, _ -> index }) { _, prize ->
                                Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(prize.emoji, fontSize = 32.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(prize.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(prize.rarity, color = prize.color, fontSize = 11.sp)
                                        }
                                        Button(onClick = {
                                            if (inventory.remove(prize)) {
                                                balance += prize.value
                                                save()
                                            }
                                        }, contentPadding = PaddingValues(horizontal = 9.dp)) {
                                            Text("+${prize.value} 🪙", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        else -> Column(Modifier.fillMaxSize().padding(20.dp)) {
                            Text("ПРОФИЛЬ ИГРОКА", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                            Spacer(Modifier.height(18.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("🎯 Открыто кейсов: $openings", fontSize = 17.sp)
                                    Text("🎒 Предметов: ${inventory.size}", fontSize = 17.sp)
                                    Text("🪙 Баланс: $balance", fontSize = 17.sp)
                                    Text("💎 Стоимость инвентаря: ${inventory.sumOf { it.value }}", fontSize = 17.sp)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Версия 0.1 • Офлайн • Виртуальная валюта",
                                color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
