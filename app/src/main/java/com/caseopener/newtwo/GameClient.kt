package com.caseopener.newtwo

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Replace with an HTTPS URL and remove cleartext permission before public release.
const val SERVER_BASE = "http://93.123.84.239:40017"

data class GameSkin(
    val id: String, val name: String, val rarity: String,
    val value: Int, val weight: Int, val emoji: String, val imageUrl: String
) {
    companion object {
        fun from(j: JSONObject) = GameSkin(
            j.optString("id"), j.optString("name"), j.optString("rarity"),
            j.optInt("value"), j.optInt("weight", 1), j.optString("emoji", "✦"),
            j.optString("image_url", "")
        )
    }
}

data class GameCase(
    val id: String, val name: String, val subtitle: String,
    val cost: Int, val emoji: String, val imageUrl: String,
    val skins: List<GameSkin>
) {
    companion object {
        fun from(j: JSONObject): GameCase = GameCase(
            j.getString("id"), j.getString("name"), j.optString("subtitle"),
            j.getInt("cost"), j.optString("emoji","📦"), j.optString("image_url",""),
            j.optJSONArray("skins").asObjects().map { GameSkin.from(it) }
        )
    }
}

data class OwnedSkin(
    val id: Long, val name: String, val rarity: String,
    val value: Int, val emoji: String, val imageUrl: String, val caseId: String
) {
    companion object {
        fun from(j: JSONObject): OwnedSkin = OwnedSkin(
            j.getLong("id"), j.getString("name"), j.getString("rarity"),
            j.getInt("value"), j.optString("emoji", "✦"),
            j.optString("image_url", ""), j.optString("case_id", "")
        )
    }
}

data class PlayerState(
    val username: String, val balance: Int, val openings: Int,
    val revision: Int, val inventory: List<OwnedSkin>
) {
    companion object {
        fun from(j: JSONObject): PlayerState = PlayerState(
            j.getString("username"), j.getInt("balance"),
            j.getInt("openings"), j.optInt("revision"),
            j.optJSONArray("inventory").asObjects().map { OwnedSkin.from(it) }
        )
    }
}

private fun JSONArray?.asObjects(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optJSONObject(it) }
}

fun mediaUrl(path: String): String? {
    if (path.isBlank()) return null
    // Never render arbitrary external URLs from the catalog.
    return if (path.startsWith("/api/v1/media/")) SERVER_BASE + path else null
}

private class SessionStore(context: Context) {
    // Same Android Keystore alias / preference file as 0.0.004: existing accounts stay signed in.
    private val prefs = context.getSharedPreferences("caseopener_online_auth", Context.MODE_PRIVATE)
    private val alias = "caseopener_online_token_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }

    fun read(): String? {
        return try {
            val raw = prefs.getString("session", null) ?: return null
            val data = Base64.decode(raw, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0,12)))
            String(cipher.doFinal(data.copyOfRange(12,data.size)), Charsets.UTF_8)
        } catch (_: Exception) {
            clear()
            null
        }
    }
    fun save(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val data = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("session", Base64.encodeToString(data, Base64.NO_WRAP)).apply()
    }
    fun clear() { prefs.edit().remove("session").apply() }
}

class GameClient(context: Context) {
    private val store = SessionStore(context.applicationContext)
    private val main = Handler(Looper.getMainLooper())
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    var token by mutableStateOf(store.read())
        private set
    var state by mutableStateOf<PlayerState?>(null)
        private set
    var cases by mutableStateOf<List<GameCase>>(emptyList())
        private set
    var catalogVersion by mutableStateOf(0)
        private set
    var status by mutableStateOf("Подключение…")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var opening by mutableStateOf(false)
        private set
    var selling by mutableStateOf(false)
        private set
    var reward by mutableStateOf<OwnedSkin?>(null)
        private set
    var rewardCaseId by mutableStateOf<String?>(null)
        private set

    private var socket: WebSocket? = null
    private var closed = false
    private var tries = 0
    private var connectionGeneration = 0
    private var pendingOpenRequest: String? = null
    private var pendingSellRequest: String? = null

    init {
        if (token != null) connect()
    }

    suspend fun authenticate(register: Boolean, username: String, password: String) {
        if (busy) return
        busy = true
        error = null
        try {
            val json = withContext(Dispatchers.IO) {
                val body = JSONObject().put("username", username.trim())
                    .put("password", password)
                    .toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val endpoint = if (register) "register" else "login"
                val request = Request.Builder().url("$SERVER_BASE/api/v1/auth/$endpoint")
                    .post(body).build()
                http.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    val parsed = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
                    if (!response.isSuccessful) {
                        throw IllegalStateException(parsed.optString("detail", "Ошибка: ${response.code}"))
                    }
                    parsed
                }
            }
            val newToken = json.getString("token")
            store.save(newToken)
            token = newToken
            state = null
            status = "Подключение…"
            connect()
        } catch (e: Exception) {
            error = when(e) {
                is SocketTimeoutException -> "Сервер не отвечает"
                else -> e.message ?: "Не удалось подключиться к серверу"
            }
        } finally { busy = false }
    }

    fun reconnect() {
        if (token != null) {
            tries = 0
            connect()
        }
    }

    private fun connect() {
        if (closed) return
        val t = token ?: return
        connectionGeneration++
        val generation = connectionGeneration
        socket?.cancel()
        socket = null
        status = "Подключение…"
        val req = Request.Builder()
            .url(SERVER_BASE.replaceFirst("http","ws") + "/api/v1/ws")
            .build()
        socket = http.newWebSocket(req, object: WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONObject().put("type","auth").put("token",t).toString())
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                main.post {
                    if (generation != connectionGeneration || closed) return@post
                    try { process(JSONObject(text)) }
                    catch (_: Exception) { error = "Не удалось обработать данные сервера" }
                }
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                main.post { if (generation == connectionGeneration && !closed) disconnected(code) }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                main.post { if (generation == connectionGeneration && !closed) disconnected(null) }
            }
        })
    }

    private fun disconnected(code: Int?) {
        if (code == 4401) {
            logoutLocal()
            error = "Сессия истекла. Войди снова."
            return
        }
        status = "Нет связи с сервером"
        opening = false
        selling = false
        pendingOpenRequest = null
        pendingSellRequest = null
        tries = (tries+1).coerceAtMost(6)
        val backoff = (1000L * (1 shl tries)).coerceAtMost(30000L)
        val generation = connectionGeneration
        main.postDelayed({
            if (!closed && generation == connectionGeneration && token != null) connect()
        }, backoff)
    }

    private fun process(event: JSONObject) {
        when (event.optString("type")) {
            "init" -> {
                state = PlayerState.from(event.getJSONObject("state"))
                updateCatalog(event.getJSONObject("catalog"))
                status = "Онлайн"
                error = null
                tries = 0
            }
            "catalog_changed" -> updateCatalog(event)
            "case_opened" -> {
                val before = state ?: return
                val revision = event.optInt("revision")
                if (revision > before.revision) {
                    val item = OwnedSkin.from(event.getJSONObject("item"))
                    state = before.copy(balance = event.getInt("balance"),
                        openings = event.getInt("openings"),revision=revision,
                        inventory=listOf(item)+before.inventory)
                }
                if (event.optString("request_id") == pendingOpenRequest) {
                    opening = false
                    pendingOpenRequest = null
                    reward = OwnedSkin.from(event.getJSONObject("item"))
                }
            }
            "item_sold" -> {
                val before = state ?: return
                val revision = event.optInt("revision")
                if (revision > before.revision) {
                    state = before.copy(balance=event.getInt("balance"),revision=revision,
                        inventory=before.inventory.filterNot { it.id == event.getLong("item_id") })
                }
                if (event.optString("request_id")==pendingSellRequest) {
                    selling=false
                    pendingSellRequest=null
                }
            }
            "error" -> {
                error = event.optString("message","Ошибка сервера")
                if (event.optString("request_id")==pendingOpenRequest) {
                    pendingOpenRequest = null
                    opening = false
                }
                if (event.optString("request_id")==pendingSellRequest) {
                    pendingSellRequest = null
                    selling = false
                }
            }
        }
    }

    private fun updateCatalog(json: JSONObject) {
        val version = json.optInt("version")
        if (version < catalogVersion) return
        cases = json.optJSONArray("cases").asObjects().map { GameCase.from(it) }
        catalogVersion=version
    }

    fun openCase(caseId: String): Boolean {
        if (status != "Онлайн" || opening || selling || socket == null) return false
        val gameCase = cases.firstOrNull { it.id == caseId } ?: return false
        if (state == null || state!!.balance < gameCase.cost) return false
        val rid=UUID.randomUUID().toString()
        val payload=JSONObject().put("type","open_case").put("case_id",caseId).put("request_id",rid)
        if (socket?.send(payload.toString()) != true) return false
        pendingOpenRequest=rid
        rewardCaseId=caseId
        reward=null
        opening=true
        error=null
        return true
    }

    fun sellItem(item: OwnedSkin) {
        if (status != "Онлайн" || selling || opening) return
        val rid=UUID.randomUUID().toString()
        val payload=JSONObject().put("type","sell_item").put("item_id",item.id)
            .put("request_id",rid)
        if (socket?.send(payload.toString()) == true) {
            selling=true
            pendingSellRequest=rid
            error=null
        }
    }

    fun acknowledgeReward() { reward=null }

    fun logoutLocal() {
        connectionGeneration++
        socket?.close(1000,"logout")
        socket=null
        store.clear()
        token=null
        state=null
        cases=emptyList()
        status="Нет подключения"
        reward=null
        opening=false
        selling=false
        pendingOpenRequest=null
        pendingSellRequest=null
    }

    suspend fun logout() {
        val t=token
        logoutLocal()
        if (t!=null) {
            withContext(Dispatchers.IO) {
                try {
                    http.newCall(Request.Builder().url("$SERVER_BASE/api/v1/auth/logout")
                        .header("Authorization","Bearer $t")
                        .post("".toRequestBody()).build()).execute().close()
                } catch (_: Exception) {}
            }
        }
    }

    fun close() {
        closed=true
        connectionGeneration++
        socket?.close(1000, "app closed")
        socket=null
        http.dispatcher.executorService.shutdown()
        http.connectionPool.evictAll()
    }
}
