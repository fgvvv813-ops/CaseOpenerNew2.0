package com.caseopener.newtwo

import android.content.Context
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun JSONArray?.socialObjects(): List<JSONObject> =
    if(this==null) emptyList() else (0 until length()).mapNotNull {optJSONObject(it)}

data class GameNotice(
    val id: Long, val kind: String, val title: String, val body: String,
    val createdAt: Long, val read: Boolean
) {
    companion object {
        fun from(j:JSONObject) = GameNotice(
            j.optLong("id"),j.optString("kind"),j.optString("title"),
            j.optString("body"),j.optLong("created_at"),
            j.optBoolean("read",false) || j.optInt("read",0)==1
        )
    }
}

data class TradeItem(
    val id: Long, val name: String, val rarity: String,
    val value: Int, val emoji: String, val imageUrl: String
) {
    companion object {
        fun from(j:JSONObject) = TradeItem(
            j.optLong("id"),j.optString("item_name",j.optString("name")),
            j.optString("rarity"),j.optInt("value"),
            j.optString("emoji","◆"),j.optString("image_url")
        )
    }
}

data class GameTrade(
    val id: String, val senderId: String, val receiverId: String,
    val senderName: String, val receiverName: String,
    val offerItems: List<TradeItem>, val requestItems: List<TradeItem>,
    val offerCoins: Int, val requestCoins: Int,
    val status: String, val createdAt: Long, val expiresAt: Long
) {
    companion object {
        fun from(j:JSONObject) = GameTrade(
            j.optString("id"), j.optString("sender_id"), j.optString("receiver_id"),
            j.optString("sender_name"),j.optString("receiver_name"),
            j.optJSONArray("offer_items").socialObjects().map {TradeItem.from(it)},
            j.optJSONArray("request_items").socialObjects().map {TradeItem.from(it)},
            j.optInt("offer_coins"),j.optInt("request_coins"),j.optString("status"),
            j.optLong("created_at"),j.optLong("expires_at")
        )
    }
}

data class TradePlayer(val id: String,val username: String) {
    companion object {fun from(j:JSONObject)=TradePlayer(j.optString("id"),j.optString("username"))}
}

data class TradeTargetInfo(
    val id: String, val username: String, val balance: Int, val inventory: List<TradeItem>,
    val more: Boolean
) {
    companion object {
        fun from(j:JSONObject) = TradeTargetInfo(
            j.optString("user_id"),j.optString("username"),
            j.optInt("balance"),
            j.optJSONArray("inventory").socialObjects().map{TradeItem.from(it)},
            j.optBoolean("more")
        )
    }
}

internal object DeviceAlerts {
    private const val CHANNEL = "caseopener_events_v1"
    fun ensureChannel(context:Context) {
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL,"События CaseOpener",NotificationManager.IMPORTANCE_DEFAULT).apply {
                description="Трейды, выдача монет и уведомления администратора"
            }
        )
    }

    fun notify(context:Context, notice:GameNotice) {
        if(Build.VERSION.SDK_INT>=33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent=Intent(context,MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open=PendingIntent.getActivity(context,0,intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification=Notification.Builder(context,CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(notice.title.take(100))
            .setContentText(notice.body.take(150))
            .setStyle(Notification.BigTextStyle().bigText(notice.body))
            .setContentIntent(open).setAutoCancel(true)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .build()
        manager.notify((notice.id % Int.MAX_VALUE).toInt(),notification)
    }
}

private fun socialDate(seconds:Long):String =
    SimpleDateFormat("dd.MM HH:mm",Locale.getDefault()).format(Date(seconds*1000L))

@Composable
internal fun NotificationsDialog(client:GameClient,onClose:()->Unit,onTrades:()->Unit) {
    AlertDialog(
        onDismissRequest=onClose,
        containerColor=DarkPanel,
        title={
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🔔 Уведомления",fontWeight=FontWeight.Black,fontSize=21.sp)
                    Text(if(client.unreadNotices>0)
                        "Новых: ${client.unreadNotices}" else "Всё прочитано",
                        color=PaleText,fontSize=12.sp)
                }
                TextButton(onClick=onClose){Text("Закрыть",color=NeonGold)}
            }
        },
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(9.dp)) {
                if(client.notices.isEmpty()) {
                    Text("Здесь будут новости, подарки, изменения баланса и трейды.",
                        color=PaleText,fontSize=13.sp)
                }
                client.notices.forEach {note->
                    Surface(
                        shape=RoundedCornerShape(13.dp),
                        color=if(!note.read)Color(0xFF332044) else Color(0xFF201B31),
                        border=BorderStroke(1.dp,if(!note.read)NeonViolet else Color(0xFF49415D))
                    ){
                        Column(Modifier.fillMaxWidth().padding(11.dp),
                            verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                Text(note.title,color=SoftWhite,fontWeight=FontWeight.Bold,
                                    fontSize=13.sp,modifier=Modifier.weight(1f))
                                Text(socialDate(note.createdAt),color=PaleText,fontSize=10.sp)
                            }
                            Text(note.body,color=PaleText,fontSize=12.sp)
                            if(note.kind=="trade") {
                                TextButton(onClick={onTrades},
                                    contentPadding=PaddingValues(0.dp)) {
                                    Text("Посмотреть трейды",color=NeonGold,fontSize=12.sp)
                                }
                            }
                        }
                    }
                }
                if(client.noticesHasMore) {
                    OutlinedButton(
                        onClick={client.requestOlderNotices()},
                        enabled=client.status=="Онлайн",
                        modifier=Modifier.fillMaxWidth()) {Text("Загрузить предыдущие")}
                }
            }
        },
        confirmButton={
            TextButton(onClick={client.markNoticesRead()},
                enabled=client.unreadNotices>0 && client.status=="Онлайн") {
                Text("Прочитать все",color=NeonGold)
            }
        }
    )
}

@Composable
internal fun PremiumMenu(
    client:GameClient, modifier:Modifier, padding:Dp,
    onPage:(String)->Unit, onNotes:()->Unit
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal=padding,vertical=12.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)) {
        Text("Меню игрока",fontSize=24.sp,fontWeight=FontWeight.Black,color=SoftWhite)
        Text("Всё необходимое в одном месте",fontSize=12.sp,color=PaleText)
        MenuTile("▤","Мой инвентарь","Полученные скины и продажа",NeonViolet){onPage("inventory")}
        MenuTile("⇄","Трейды","Предложения и обмен скинами и монетами",NeonGold){onPage("trades")}
        MenuTile("♙","Мой профиль","Статистика, аккаунт и настройки",NeonBlue){onPage("profile")}
        Spacer(Modifier.height(3.dp))
        OutlinedButton(onClick=onNotes,modifier=Modifier.fillMaxWidth()) {
            Text("Что нового в версии ${BuildConfig.VERSION_NAME}")
        }
    }
}

@Composable
private fun MenuTile(symbol:String,name:String,detail:String,accent:Color,onClick:()->Unit) {
    Surface(
        modifier=Modifier.fillMaxWidth().clickable(onClick=onClick),
        color=Color(0xFF222039),
        shape=RoundedCornerShape(17.dp),
        border=BorderStroke(1.dp,accent.copy(alpha=0.6f))
    ){
        Row(Modifier.padding(horizontal=14.dp,vertical=17.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)){
            Text(symbol,fontSize=30.sp,color=accent)
            Column(Modifier.weight(1f)){
                Text(name,fontWeight=FontWeight.Black,fontSize=16.sp,color=SoftWhite)
                Text(detail,fontSize=11.sp,color=PaleText,maxLines=2)
            }
            Text("›",fontSize=27.sp,color=accent)
        }
    }
}

@Composable
internal fun PremiumTrades(client:GameClient,modifier:Modifier,padding:Dp) {
    var query by rememberSaveable{mutableStateOf("")}
    var filter by rememberSaveable{mutableStateOf("in")}
    var mine by remember{mutableStateOf(setOf<Long>())}
    var theirs by remember{mutableStateOf(setOf<Long>())}
    var mineCoins by rememberSaveable{mutableStateOf("")}
    var theirCoins by rememberSaveable{mutableStateOf("")}
    var mineQuery by rememberSaveable{mutableStateOf("")}
    var theirQuery by rememberSaveable{mutableStateOf("")}
    var confirm by remember{mutableStateOf<Pair<String,String>?>(null)}
    val playerId=client.state?.userId.orEmpty()
    LaunchedEffect(Unit){client.loadTrades()}
    LaunchedEffect(client.tradeTarget?.id) {
        mine=emptySet();theirs=emptySet();mineCoins="";theirCoins=""
    }
    val offerValue=mineCoins.toIntOrNull()?:0
    val requestValue=theirCoins.toIntOrNull()?:0
    val validCoins=(mineCoins.isBlank() || mineCoins.toIntOrNull()!=null) &&
        (theirCoins.isBlank() || theirCoins.toIntOrNull()!=null) &&
        offerValue in 0..(client.state?.balance?:0) &&
        requestValue in 0..(client.tradeTarget?.balance?:0)
    val target=client.tradeTarget
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal=padding,vertical=9.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)){
                Text("⇄ Трейды",fontSize=24.sp,fontWeight=FontWeight.Black,color=SoftWhite)
                Text("Безопасный обмен через сервер",fontSize=11.sp,color=PaleText)
            }
            TextButton(onClick={client.loadTrades()}){Text("↻ Обновить",color=NeonViolet)}
        }
        client.tradeError?.let{Text(it,color=WarningRed,fontSize=12.sp)}
        client.tradeFeedback?.let{Text(it,color=NeonGold,fontSize=12.sp)}
        NeonPanel(modifier=Modifier.fillMaxWidth(),accent=NeonViolet){
            Text("Новое предложение",fontWeight=FontWeight.Black,color=SoftWhite,fontSize=17.sp)
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(
                    value=query,onValueChange={query=it.take(24)},
                    modifier=Modifier.weight(1f),label={Text("Ник игрока")},
                    singleLine=true,
                    shape=RoundedCornerShape(12.dp)
                )
                OutlinedButton(
                    onClick={client.searchTradePlayers(query)},
                    enabled=query.trim().length>=2 && client.status=="Онлайн"
                ){Text("Найти")}
            }
            if(client.tradeSearchPlayers.isNotEmpty()) {
                Text("Выбери игрока:",color=PaleText,fontSize=11.sp)
                client.tradeSearchPlayers.take(12).forEach {p->
                    TextButton(
                        onClick={
                            query=p.username
                            client.requestTradeTarget(p.id)
                        },modifier=Modifier.fillMaxWidth()
                    ){Text("♙ ${p.username}    ›",color=NeonBlue)}
                }
            }
            if(client.tradeTargetLoading) {
                CircularProgressIndicator(color=NeonViolet,modifier=Modifier.size(28.dp))
            }
            if(target!=null){
                HorizontalDivider(color=Color(0xFF4E416A))
                Text("Игрок: ${target.username}",color=NeonGold,fontWeight=FontWeight.Black)
                Text("Его баланс: ${target.balance} монет",color=PaleText,fontSize=12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Я отдаю · до 6 скинов",fontWeight=FontWeight.Bold,color=SoftWhite)
                OutlinedTextField(value=mineQuery,onValueChange={mineQuery=it},
                    singleLine=true,modifier=Modifier.fillMaxWidth(),
                    label={Text("Поиск по своему инвентарю")})
                val myItems=client.state?.inventory.orEmpty().filter{
                    it.name.contains(mineQuery,ignoreCase=true)
                }.take(80)
                if(myItems.isEmpty()) Text("Предметы не найдены",color=PaleText,fontSize=11.sp)
                myItems.forEach {item->
                    SkinChoiceRow(item.id,item.name,item.rarity,item.imageUrl,item.emoji,
                        item.value,mine.contains(item.id),mine.size<6 || mine.contains(item.id)){
                        mine=if(mine.contains(item.id))mine-item.id else mine+item.id
                    }
                }
                Text("Я хочу получить · до 6 скинов",fontWeight=FontWeight.Bold,color=SoftWhite)
                OutlinedTextField(value=theirQuery,onValueChange={theirQuery=it},
                    singleLine=true,modifier=Modifier.fillMaxWidth(),
                    label={Text("Поиск в инвентаре игрока")})
                target.inventory.filter{
                    it.name.contains(theirQuery,ignoreCase=true)
                }.take(80).forEach {item->
                    SkinChoiceRow(item.id,item.name,item.rarity,item.imageUrl,item.emoji,
                        item.value,theirs.contains(item.id),theirs.size<6 || theirs.contains(item.id)){
                        theirs=if(theirs.contains(item.id))theirs-item.id else theirs+item.id
                    }
                }
                if(target.more) {
                    Text("Показаны последние 500 предметов игрока.",
                        color=PaleText,fontSize=11.sp)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
                    OutlinedTextField(
                        value=mineCoins,onValueChange={mineCoins=it.filter(Char::isDigit).take(10)},
                        modifier=Modifier.weight(1f),label={Text("Отдам монет")},
                        singleLine=true
                    )
                    OutlinedTextField(
                        value=theirCoins,onValueChange={theirCoins=it.filter(Char::isDigit).take(10)},
                        modifier=Modifier.weight(1f),label={Text("Хочу монет")},
                        singleLine=true
                    )
                }
                Text("Отдаю ${mine.size} скинов и ${offerValue} монет · получаю ${theirs.size} скинов и ${requestValue} монет",
                    color=PaleText,fontSize=11.sp)
                GoldButton(
                    text=if(client.tradeBusy)"Отправка…" else "Отправить предложение",
                    modifier=Modifier.fillMaxWidth(),
                    enabled=validCoins && !client.tradeBusy && client.status=="Онлайн" &&
                        (mine.isNotEmpty()||theirs.isNotEmpty()||offerValue>0||requestValue>0),
                    onClick={
                        if(client.createTrade(target.id,mine.toList(),theirs.toList(),offerValue,requestValue)) {
                            mine=emptySet();theirs=emptySet();mineCoins="";theirCoins=""
                        }
                    }
                )
                Text("Предложение действует 24 часа. При принятии сервер проверит оба баланса и владение скинами.",
                    color=PaleText,fontSize=11.sp)
            }else if(!client.tradeTargetLoading){
                Text("Выбери игрока по нику или открой его профиль в чате.",
                    color=PaleText,fontSize=12.sp)
            }
        }
        Text("Мои предложения",fontWeight=FontWeight.Black,color=SoftWhite,fontSize=18.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(selected=filter=="in",onClick={filter="in"},label={Text("Входящие")})
            FilterChip(selected=filter=="out",onClick={filter="out"},label={Text("Исходящие")})
        }
        val shown=client.trades.filter{
            if(filter=="in")it.receiverId==playerId else it.senderId==playerId
        }
        if(shown.isEmpty()) {
            Text(if(filter=="in")"Входящих трейдов пока нет" else "Ты ещё не отправлял предложения",
                color=PaleText,fontSize=12.sp)
        }
        shown.forEach {t->
            NeonPanel(modifier=Modifier.fillMaxWidth(),
                accent=if(t.status=="pending")NeonGold else NeonViolet) {
                Text(
                    if(t.senderId==playerId)"Кому: ${t.receiverName}" else "От: ${t.senderName}",
                    color=SoftWhite,fontWeight=FontWeight.Bold,fontSize=14.sp
                )
                val status=when {
                    t.status=="pending" && t.expiresAt<=System.currentTimeMillis()/1000 ->"Истёк срок"
                    t.status=="pending"->"Ожидает ответа"
                    t.status=="accepted"->"Обмен завершён"
                    t.status=="rejected"->"Отклонён"
                    t.status=="cancelled"->"Отменён"
                    else->"Истёк срок"
                }
                Text(status+" · "+socialDate(t.createdAt),
                    color=if(t.status=="pending")NeonGold else PaleText,fontSize=11.sp)
                Text("Отправитель: ${t.offerItems.joinToString {it.name}.ifEmpty{"нет скинов"}}" +
                     if(t.offerCoins>0) " + ${t.offerCoins} монет" else "",
                    color=PaleText,fontSize=12.sp)
                Text("Получатель: ${t.requestItems.joinToString {it.name}.ifEmpty{"нет скинов"}}" +
                     if(t.requestCoins>0) " + ${t.requestCoins} монет" else "",
                    color=PaleText,fontSize=12.sp)
                if(t.status=="pending" && t.expiresAt>System.currentTimeMillis()/1000) {
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        if(t.receiverId==playerId){
                            Button(onClick={confirm=t.id to "accept"},enabled=!client.tradeBusy) {
                                Text("Принять")
                            }
                            OutlinedButton(onClick={confirm=t.id to "reject"},enabled=!client.tradeBusy) {
                                Text("Отклонить")
                            }
                        }else{
                            OutlinedButton(onClick={confirm=t.id to "cancel"},enabled=!client.tradeBusy){
                                Text("Отменить")
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
    confirm?.let{(id,decision)->
        AlertDialog(
            onDismissRequest={confirm=null},containerColor=DarkPanel,
            title={Text(when(decision){
                "accept"->"Подтвердить обмен?"
                "reject"->"Отклонить трейд?"
                else->"Отменить предложение?"
            })},
            text={Text(if(decision=="accept")
                "Сервер передаст выбранные скины и монеты обоим игрокам. Это действие нельзя отменить."
                else "Обмен не состоится.")},
            dismissButton={TextButton(onClick={confirm=null}){Text("Назад")}},
            confirmButton={TextButton(onClick={
                client.decideTrade(id,decision);confirm=null
            }){Text("Подтвердить",color=NeonGold)}}
        )
    }
}

@Composable
private fun SkinChoiceRow(
    id:Long,name:String,rarity:String,image:String,emoji:String,value:Int,
    checked:Boolean,enabled:Boolean,onClick:()->Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled=enabled,onClick=onClick).padding(vertical=3.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(7.dp)
    ){
        Checkbox(checked=checked,onCheckedChange={onClick()},enabled=enabled)
        GameArt(image,emoji,Modifier.size(43.dp))
        Column(Modifier.weight(1f)){
            Text(name,color=SoftWhite,fontSize=11.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(rarity,color=PaleText,fontSize=10.sp)
        }
        Text("${value} 🪙",color=NeonGold,fontSize=11.sp)
    }
}
