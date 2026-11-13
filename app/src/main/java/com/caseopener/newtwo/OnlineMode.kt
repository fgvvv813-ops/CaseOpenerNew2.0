package com.caseopener.newtwo

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val OnlinePurple = Color(0xFFC177FF)
private val OnlinePanel = Color(0xFF1B1429)
private val OnlineMuted = Color(0xFFC6BDD2)
private val OnlineGold = Color(0xFFFFD079)
private val OnlineBg = Color(0xFF0A0713)

private data class ServerCase(val id: String, val name: String, val subtitle: String, val cost: Int, val emoji: String)
private data class ServerItem(val id: Long, val name: String, val rarity: String, val value: Int, val emoji: String)
private data class ServerState(val username: String, val balance: Int, val openings: Int, val revision: Int, val inventory: List<ServerItem>)
private data class AuthResult(val token: String, val state: ServerState)
private data class OpenResult(val prize: ServerItem, val state: ServerState)
private class ApiException(val status: Int, message: String): Exception(message)

private object GameService {
    // Temporary HTTP testing endpoint. Switch to HTTPS before public registration.
    private const val BASE = "http://48.102.160.0:40017"

    private suspend fun request(
        method: String,
        path: String,
        token: String? = null,
        payload: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(BASE + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 6500
            connection.readTimeout = 9000
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val result = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
            if (status !in 200..299) {
                val detail = result.opt("detail")
                throw ApiException(status, if (detail is String) detail else "Ошибка сервера: $status")
            }
            result
        } catch (e: ApiException) {
            throw e
        } catch (_: java.net.SocketTimeoutException) {
            throw Exception("Сервер не отвечает. Проверь подключение к интернету.")
        } catch (_: java.net.ConnectException) {
            throw Exception("Не удалось подключиться к серверу.")
        } catch (_: java.net.UnknownHostException) {
            throw Exception("Адрес сервера недоступен.")
        } catch (_: java.io.IOException) {
            throw Exception("Ошибка соединения с сервером.")
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONArray.toServerItems(): List<ServerItem> = (0 until length()).map { i ->
        val j = getJSONObject(i)
        ServerItem(
            j.getLong("id"),
            j.getString("name"),
            j.getString("rarity"),
            j.getInt("value"),
            j.optString("emoji", "✦")
        )
    }
    private fun JSONObject.toState(): ServerState =
        ServerState(getString("username"), getInt("balance"), getInt("openings"),
            optInt("revision"), getJSONArray("inventory").toServerItems())

    suspend fun catalog(): List<ServerCase> {
        val array = request("GET", "/api/v1/catalog").getJSONArray("cases")
        return (0 until array.length()).map { i ->
            val j = array.getJSONObject(i)
            ServerCase(j.getString("id"), j.getString("name"), j.optString("subtitle", ""),
                j.getInt("cost"), j.optString("emoji", "📦"))
        }
    }

    suspend fun auth(register: Boolean, username: String, password: String): AuthResult {
        val route = if (register) "/api/v1/auth/register" else "/api/v1/auth/login"
        val json = request("POST", route, payload = JSONObject()
            .put("username", username).put("password", password))
        return AuthResult(json.getString("token"), json.getJSONObject("state").toState())
    }
    suspend fun state(token: String): ServerState =
        request("GET", "/api/v1/state", token).toState()

    suspend fun open(token: String, caseId: String): OpenResult {
        val json = request("POST", "/api/v1/cases/$caseId/open", token)
        val item = json.getJSONObject("prize")
        return OpenResult(ServerItem(item.getLong("id"), item.getString("name"),
            item.getString("rarity"), item.getInt("value"), item.optString("emoji", "✦")),
            json.getJSONObject("state").toState())
    }
    suspend fun sell(token: String, id: Long): ServerState =
        request("POST", "/api/v1/inventory/$id/sell", token).getJSONObject("state").toState()
    suspend fun logout(token: String) {
        request("POST", "/api/v1/auth/logout", token)
    }
}

// Android Keystore keeps the online session token encrypted on disk.
private class OnlineSession(context: Context) {
    private val prefs = context.getSharedPreferences("caseopener_online_auth", Context.MODE_PRIVATE)
    private val alias = "caseopener_online_token_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return (KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")).run {
            init(
                KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    fun load(): String? {
        return try {
            val saved = prefs.getString("session", null) ?: return null
            val bytes = Base64.decode(saved, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun save(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("session", Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
    }
    fun clear() {
        prefs.edit().remove("session").apply()
    }
}

@Composable
fun OnlineHub(modifier: Modifier = Modifier, tab: Int) {
    val context = LocalContext.current
    val session = remember(context) { OnlineSession(context.applicationContext) }
    var token by remember { mutableStateOf(session.load()) }
    var profile by remember { mutableStateOf<ServerState?>(null) }
    var caseList by remember { mutableStateOf<List<ServerCase>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var latestPrize by remember { mutableStateOf<ServerItem?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try { caseList = GameService.catalog() }
        catch (e: Exception) { message = e.message ?: "Не удалось загрузить кейсы" }
    }
    LaunchedEffect(token) {
        val t = token
        if (t != null) {
            busy = true
            try { profile = GameService.state(t); message = null }
            catch (e: Exception) {
                if (e is ApiException && e.status == 401) {
                    session.clear()
                    token = null
                    profile = null
                } else {
                    message = e.message ?: "Нет связи с сервером"
                }
            } finally { busy = false }
        } else {
            profile = null
        }
    }

    Column(modifier.fillMaxWidth().background(OnlineBg).padding(horizontal = 16.dp)) {
        Text(
            "● ОНЛАЙН-РЕЖИМ",
            color = OnlinePurple, fontWeight = FontWeight.Black, fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        if (message != null) {
            Surface(
                color = Color(0xFF40202D), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(message.orEmpty(), color = Color(0xFFFFC6D3),
                    modifier = Modifier.padding(11.dp), fontSize = 12.sp)
            }
            Spacer(Modifier.height(9.dp))
        }

        if (token == null) {
            OnlineAuth(
                modifier = Modifier.weight(1f),
                busy = busy,
                onSubmit = { register, username, password ->
                    if (!busy) {
                        scope.launch {
                            busy = true
                            message = null
                            try {
                                val result = GameService.auth(register, username.trim(), password)
                                session.save(result.token)
                                profile = result.state
                                token = result.token
                            } catch (e: Exception) {
                                message = e.message ?: "Не удалось войти"
                            } finally { busy = false }
                        }
                    }
                }
            )
        } else if (profile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (busy) CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Подключение к аккаунту…", color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = {
                        session.clear()
                        token = null
                    }) { Text("Вернуться ко входу") }
                }
            }
        } else {
            val current = profile!!
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👤 ${current.username}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("🪙 ${current.balance}", color = OnlineGold, fontWeight = FontWeight.Bold)
            }
            when (tab) {
                0 -> OnlineCases(
                    modifier = Modifier.weight(1f),
                    cases = caseList,
                    selectedId = selected,
                    latest = latestPrize,
                    busy = busy,
                    balance = current.balance,
                    onChoose = { selected = it.id; latestPrize = null },
                    onOpen = { chosen ->
                        val t = token ?: return@OnlineCases
                        if (!busy) scope.launch {
                            busy = true
                            message = null
                            try {
                                val result = GameService.open(t, chosen.id)
                                latestPrize = result.prize
                                profile = result.state
                            } catch (e: Exception) {
                                message = e.message ?: "Не получилось открыть кейс"
                            } finally { busy = false }
                        }
                    }
                )
                1 -> OnlineInventory(
                    modifier = Modifier.weight(1f),
                    items = current.inventory,
                    busy = busy,
                    onSell = { item ->
                        val t = token ?: return@OnlineInventory
                        if (!busy) scope.launch {
                            busy = true
                            message = null
                            try { profile = GameService.sell(t, item.id) }
                            catch (e: Exception) { message = e.message ?: "Не удалось продать предмет" }
                            finally { busy = false }
                        }
                    }
                )
                else -> OnlineProfile(
                    modifier = Modifier.weight(1f),
                    state = current,
                    busy = busy,
                    onRefresh = {
                        val t = token
                        if (t != null && !busy) scope.launch {
                            busy = true
                            message = null
                            try {
                                profile = GameService.state(t)
                                caseList = GameService.catalog()
                            } catch (e: Exception) {
                                message = e.message ?: "Нет связи с сервером"
                            } finally { busy = false }
                        }
                    },
                    onLogout = {
                        val t = token
                        session.clear()
                        token = null
                        profile = null
                        message = null
                        if (t != null) scope.launch { try { GameService.logout(t) } catch (_: Exception) {} }
                    }
                )
            }
        }
    }
}

@Composable
private fun OnlineAuth(
    modifier: Modifier,
    busy: Boolean,
    onSubmit: (register: Boolean, username: String, password: String) -> Unit
) {
    var register by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 510.dp),
            colors = CardDefaults.cardColors(containerColor = OnlinePanel),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("🎮 Аккаунт игрока", color = Color.White, fontSize = 24.sp,
                    fontWeight = FontWeight.Black)
                Text("Прогресс в онлайне сохраняется на сервере и доступен после входа с другого устройства.",
                    fontSize = 13.sp, color = OnlineMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !register, onClick = { register = false },
                        label = { Text("Вход") })
                    FilterChip(selected = register, onClick = { register = true },
                        label = { Text("Регистрация") })
                }
                OutlinedTextField(
                    value = username, onValueChange = { username = it.take(24) },
                    label = { Text("Логин") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it.take(128) },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Button(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    enabled = !busy && username.trim().length in 3..24 && password.length >= 8,
                    onClick = { onSubmit(register, username, password) }
                ) {
                    Text(if (busy) "Подключение…" else if (register) "Создать аккаунт" else "Войти",
                        fontWeight = FontWeight.Bold)
                }
                Text("Для регистрации используй уникальный тестовый пароль. До включения HTTPS соединение с сервером не зашифровано.",
                    fontSize = 11.sp, color = OnlineGold)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun OnlineCases(
    modifier: Modifier,
    cases: List<ServerCase>,
    selectedId: String?,
    latest: ServerItem?,
    busy: Boolean,
    balance: Int,
    onChoose: (ServerCase) -> Unit,
    onOpen: (ServerCase) -> Unit
) {
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)) {
        item { Text("КЕЙСЫ", fontWeight = FontWeight.Black, fontSize = 19.sp, color = Color.White) }
        if (cases.isEmpty()) {
            item { Text("Кейсы загружаются с сервера…", color = OnlineMuted) }
        }
        val selectedCase = cases.firstOrNull { it.id == selectedId }
        if (selectedCase != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = OnlinePanel),
                    shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(latest?.emoji ?: selectedCase.emoji, fontSize = 64.sp)
                        Text(latest?.name ?: selectedCase.name, color = Color.White,
                            fontWeight = FontWeight.Black, fontSize = 20.sp,
                            textAlign = TextAlign.Center)
                        if (latest != null) {
                            Text("${latest.rarity} • ${latest.value} 🪙",
                                color = OnlineGold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            enabled = !busy && balance >= selectedCase.cost,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            onClick = { onOpen(selectedCase) }
                        ) {
                            Text(if (busy) "Открываем…" else "Открыть за ${selectedCase.cost} 🪙")
                        }
                        if (balance < selectedCase.cost) {
                            Text("Недостаточно монет", color = OnlineGold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        items(cases, key = { it.id }) { case ->
            Card(
                colors = CardDefaults.cardColors(containerColor =
                    if (case.id == selectedId) Color(0xFF312044) else OnlinePanel),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(case.emoji, fontSize = 40.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(case.name, fontWeight = FontWeight.Black, color = Color.White)
                        Text(case.subtitle, fontSize = 11.sp, color = OnlineMuted)
                        Text("${case.cost} 🪙", color = OnlineGold)
                    }
                    Button(onClick = { onChoose(case) }, enabled = !busy,
                        contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text(if (case.id == selectedId) "Выбран" else "Выбрать", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineInventory(
    modifier: Modifier,
    items: List<ServerItem>,
    busy: Boolean,
    onSell: (ServerItem) -> Unit
) {
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Text("МОИ ПРЕДМЕТЫ • ${items.size}", fontWeight = FontWeight.Black,
            color = Color.White, fontSize = 18.sp) }
        if (items.isEmpty()) {
            item { Text("Пока нет предметов. Открой первый кейс!", color = OnlineMuted) }
        }
        items(items, key = { it.id }) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = OnlinePanel),
                shape = RoundedCornerShape(14.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(item.rarity, color = OnlinePurple, fontSize = 11.sp)
                    }
                    Button(onClick = { onSell(item) }, enabled = !busy,
                        contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text("+${item.value} 🪙", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineProfile(
    modifier: Modifier,
    state: ServerState,
    busy: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ОНЛАЙН-ПРОФИЛЬ", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Black)
        Card(colors = CardDefaults.cardColors(containerColor = OnlinePanel),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("👤  ${state.username}", color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold)
                Text("🪙  Монеты: ${state.balance}", color = OnlineGold)
                Text("🎯  Открыто кейсов: ${state.openings}", color = Color.White)
                Text("🎒  Предметов: ${state.inventory.size}", color = Color.White)
                Text("☁  Прогресс автоматически сохраняется на сервере.",
                    color = OnlineMuted, fontSize = 12.sp)
            }
        }
        Button(onClick = onRefresh, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (busy) "Синхронизация…" else "↻  Обновить данные")
        }
        OutlinedButton(onClick = onLogout,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Выйти из аккаунта")
        }
        Text("Офлайн-прогресс хранится отдельно и при переходе в онлайн не удаляется.",
            fontSize = 12.sp, color = OnlineMuted)
        Spacer(Modifier.height(14.dp))
    }
}
