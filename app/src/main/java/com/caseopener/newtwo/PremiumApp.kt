package com.caseopener.newtwo

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var notificationOpenVersion by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent?.getBooleanExtra("open_notifications",false)==true) notificationOpenVersion++
        setContent { PremiumGame(notificationOpenVersion) }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if(intent.getBooleanExtra("open_notifications",false)) notificationOpenVersion++
    }
    override fun onStart() {
        super.onStart()
        AppVisibility.visible=true
    }
    override fun onStop() {
        AppVisibility.visible=false
        super.onStop()
    }
}

@Composable
internal fun PremiumGame(openNotificationsVersion:Int = 0) {
    val context=LocalContext.current
    val client=remember { GameClient(context.applicationContext) }
    val prefs=remember { context.applicationContext.getSharedPreferences("caseopener_ui",Context.MODE_PRIVATE) }
    val scope=rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf("home") }
    var selectedCaseId by rememberSaveable { mutableStateOf("") }
    var showNotes by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var reducedMotion by remember { mutableStateOf(prefs.getBoolean("reduced_motion",false)) }
    val online=client.state!=null && client.token!=null
    LaunchedEffect(openNotificationsVersion,online) {
        if(online && openNotificationsVersion>0) showNotifications=true
    }
    val noticePermission=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Android manages the selected notification permission. */ }
    LaunchedEffect(online) {
        if(online && Build.VERSION.SDK_INT>=33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED &&
            !prefs.getBoolean("notifications_permission_asked",false)
        ) {
            prefs.edit().putBoolean("notifications_permission_asked",true).apply()
            noticePermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(client.catalogVersion,client.state?.revision) {
        DeviceImageCache.reconcile(context.applicationContext,client.cases,client.state)
    }
    DisposableEffect(client) { onDispose { client.close() } }

    MaterialTheme(colorScheme=premiumScheme) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(DarkBg,Color(0xFF0B222B),DarkBg))
            ).statusBarsPadding().navigationBarsPadding().imePadding()
        ) {
            if(client.token==null) {
                PremiumLogin(client)
            } else if(client.state==null) {
                WaitingForAccount(client,
                    retry={client.reconnect()},
                    logout={scope.launch {client.logout(); page="home"}})
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val tablet = maxWidth >= 720.dp
                    val contentPadding = when {
                        maxWidth < 350.dp -> 11.dp
                        maxWidth < 590.dp -> 16.dp
                        else -> 24.dp
                    }
                    Row(Modifier.fillMaxSize()) {
                        if(tablet) {
                            PremiumSideNav(
                                selected=page,
                                unread=client.chatUnread,
                                pending=client.trades.count {
                                    it.receiverId==client.state?.userId && it.status=="pending"
                                },
                                onSelect={page=it}
                            )
                        }
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            Column(
                                Modifier.fillMaxHeight().widthIn(max=1000.dp).fillMaxWidth()
                                    .align(Alignment.TopCenter)
                            ) {
                        PremiumHeader(
                            client=client,padding=contentPadding,
                            onSettings={showSettings=true},
                            onNotifications={showNotifications=true},
                            hasCatalogUpdate=client.unreadNotices>0
                        )
                        if(client.status!="Онлайн") {
                            Text("Соединение с сервером потеряно. Открытие и продажа временно недоступны.",
                                modifier=Modifier.fillMaxWidth().padding(horizontal=contentPadding,vertical=5.dp),
                                textAlign=TextAlign.Center,color=NeonGold,fontSize=11.sp)
                        }
                        client.error?.let {
                            Text(it,modifier=Modifier.fillMaxWidth().padding(horizontal=contentPadding,vertical=4.dp),
                                color=WarningRed,fontSize=12.sp)
                        }
                                Crossfade(
                                    targetState=page,
                                    modifier=Modifier.weight(1f),
                                    animationSpec=if(reducedMotion) snap() else tween(230),
                                    label="Экран"
                                ) { activePage ->
                                    when(activePage) {
                            "home" -> PremiumHome(
                                client=client,
                                modifier=Modifier.fillMaxSize(),
                                padding=contentPadding,
                                onOpenCases={page="cases"},
                                onCase={selectedCaseId=it;page="detail"},
                                onProfile={page="profile"},
                                onNotes={showNotes=true}
                            )
                            "cases" -> PremiumCases(
                                client=client,modifier=Modifier.fillMaxSize(),padding=contentPadding,
                                onCase={selectedCaseId=it;page="detail"}
                            )
                            "detail" -> {
                                val chosen=client.cases.firstOrNull { it.id==selectedCaseId }
                                if(chosen==null) {
                                    Box(Modifier.fillMaxSize().fillMaxWidth(),contentAlignment=Alignment.Center) {
                                        TextButton(onClick={page="cases"}) {
                                            Text("Этот кейс недоступен. Вернуться к кейсам")
                                        }
                                    }
                                } else {
                                    PremiumCaseDetail(
                                        client=client,gameCase=chosen,
                                        modifier=Modifier.fillMaxSize(),padding=contentPadding,
                                        onBack={page="cases"}
                                    )
                                }
                            }
                            "inventory" -> PremiumInventory(
                                client=client,modifier=Modifier.fillMaxSize(),padding=contentPadding
                            )
                            "trades" -> PremiumTrades(
                                client=client,modifier=Modifier.fillMaxSize(),padding=contentPadding
                            )
                            "menu" -> PremiumMenu(
                                client=client,modifier=Modifier.fillMaxSize(),padding=contentPadding,
                                onPage={page=it},
                                onNotes={showNotes=true}
                            )
                            "chat" -> PremiumChat(
                                client=client,
                                modifier=Modifier.fillMaxSize(),
                                padding=contentPadding,
                                onPlayer={
                                    client.requestPlayerProfile(it)
                                    page="player"
                                }
                            )
                            "player" -> PremiumPlayerProfile(
                                client=client,
                                modifier=Modifier.fillMaxSize(),
                                padding=contentPadding,
                                onBack={page="chat"},
                                onTrade={
                                    client.requestTradeTarget(it)
                                    page="trades"
                                }
                            )
                            else -> PremiumProfile(
                                client=client,modifier=Modifier.fillMaxSize(),padding=contentPadding,
                                onLogout={confirmLogout=true},
                                onSettings={showSettings=true},
                                onNotes={showNotes=true}
                            )
                        }
                                }
                                if(!tablet) {
                                    PremiumBottomNav(
                                        selected=page,
                                        unread=client.chatUnread,
                                        pending=client.trades.count {
                                            it.receiverId==client.state?.userId && it.status=="pending"
                                        },
                                        onSelect={page=it}
                                    )
                                }
                            }
                        }
                    }
                }
                client.reward?.let { item ->
                    PremiumRoulette(
                        reward=item,
                        gameCase=client.cases.firstOrNull { it.id==client.rewardCaseId },
                        reducedMotion=reducedMotion,
                        onClose={client.acknowledgeReward()}
                    )
                }
            }
            if(showNotes) {
                UpdateInfo(onClose={showNotes=false})
            }
            if(showNotifications && online) {
                NotificationsDialog(
                    client=client,
                    onClose={showNotifications=false},
                    onTrades={showNotifications=false;page="trades"}
                )
            }
            if(showSettings) {
                AlertDialog(
                    onDismissRequest={showSettings=false},
                    containerColor=DarkPanel,
                    title={Text("⚙ Настройки")},
                    text={
                        Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Уменьшить анимацию",fontWeight=FontWeight.Bold)
                                    Text("Рулетка будет показывать результат быстрее",
                                        color=PaleText,fontSize=12.sp)
                                }
                                Switch(
                                    checked=reducedMotion,
                                    onCheckedChange={
                                        reducedMotion=it
                                        prefs.edit().putBoolean("reduced_motion",it).apply()
                                    }
                                )
                            }
                            HorizontalDivider(color=NeonViolet.copy(alpha=0.18f))
                            val permissionOK = Build.VERSION.SDK_INT < 33 ||
                                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                                    PackageManager.PERMISSION_GRANTED
                            Text(
                                if(permissionOK)"Уведомления Android разрешены"
                                else "Системные уведомления отключены",
                                color=if(permissionOK)NeonViolet else WarningRed,fontSize=12.sp
                            )
                            OutlinedButton(
                                onClick={
                                    val intent=Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)
                                    context.startActivity(intent)
                                },
                                modifier=Modifier.fillMaxWidth()
                            ){ Text("Настроить уведомления") }
                            Text("Версия "+BuildConfig.VERSION_NAME,color=PaleText,fontSize=12.sp)
                        }
                    },
                    confirmButton={TextButton(onClick={showSettings=false}) {Text("Готово")}}
                )
            }
            if(confirmLogout) {
                AlertDialog(
                    onDismissRequest={confirmLogout=false},
                    containerColor=DarkPanel,
                    title={Text("Выйти из аккаунта?")},
                    text={Text("Прогресс останется в твоём онлайн-аккаунте. Для продолжения игры нужно будет войти снова.")},
                    dismissButton={TextButton(onClick={confirmLogout=false}) {Text("Отмена")}},
                    confirmButton={
                        TextButton(onClick={
                            confirmLogout=false
                            scope.launch {client.logout();page="home"}
                        }) {Text("Выйти",color=WarningRed)}
                    }
                )
            }
        }
    }
}

@Composable
private fun PremiumHeader(
    client: GameClient,
    padding: androidx.compose.ui.unit.Dp,
    onSettings: () -> Unit,
    onNotifications: () -> Unit,
    hasCatalogUpdate: Boolean
) {
    Column(Modifier.fillMaxWidth().padding(horizontal=padding)) {
        Row(
            Modifier.fillMaxWidth().padding(top=7.dp,bottom=5.dp),
            verticalAlignment=Alignment.CenterVertically
        ) {
            Image(painter=painterResource(R.drawable.ic_launcher),contentDescription="CaseOpener",
                modifier=Modifier.size(39.dp).clip(RoundedCornerShape(11.dp)))
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text("CASEOPENER",color=SoftWhite,fontSize=19.sp,
                    fontWeight=FontWeight.Black,maxLines=1)
                Text("N E W   2 . 0",color=NeonGold,fontSize=10.sp,fontWeight=FontWeight.Black)
            }
            IconMini("🔔",onNotifications,hasCatalogUpdate)
            Spacer(Modifier.width(7.dp))
            IconMini("⚙",onSettings,false)
        }
        val state=client.state
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF122732)).padding(9.dp),
            verticalAlignment=Alignment.CenterVertically
        ) {
            Image(painter=painterResource(R.drawable.ic_launcher),contentDescription=null,
                modifier=Modifier.size(43.dp).clip(RoundedCornerShape(15.dp)))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(state?.username.orEmpty(),fontSize=14.sp,
                    fontWeight=FontWeight.Bold,maxLines=1)
                Text("Уровень "+progressLevel(state?.openings?:0),
                    fontSize=11.sp,color=PaleText)
                LinearProgressIndicator(
                    progress={progressFraction(state?.openings?:0)},
                    modifier=Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(8.dp)),
                    color=NeonViolet,
                    trackColor=Color(0xFF2C4A55)
                )
            }
            Spacer(Modifier.width(9.dp))
            Surface(
                color=Color(0xFF3C3224),
                border=BorderStroke(1.dp,NeonGold),
                shape=RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("🪙 "+(state?.balance?:0).toString(),
                        fontSize=13.sp,color=NeonGold,fontWeight=FontWeight.Black,
                        modifier=Modifier.padding(start=9.dp,top=8.dp,bottom=8.dp))

                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun IconMini(symbol:String,onClick:()->Unit,hasDot:Boolean) {
    Box {
        Surface(
            color=Color(0xFF1A303A),
            shape=RoundedCornerShape(12.dp),
            border=BorderStroke(1.dp,Color(0xFF3C6070)),
            modifier=Modifier.size(40.dp).clickable(onClick=onClick)
        ) {
            Box(contentAlignment=Alignment.Center) {
                Text(symbol,fontSize=23.sp,color=PaleText)
            }
        }
        if(hasDot) {
            Box(Modifier.size(9.dp).align(Alignment.TopEnd)
                .clip(RoundedCornerShape(99.dp)).background(NeonGold))
        }
    }
}

@Composable
private fun PremiumSideNav(
    selected:String, unread:Int, pending:Int, onSelect:(String)->Unit
) {
    NavigationRail(
        modifier=Modifier.fillMaxHeight().width(94.dp),
        containerColor=DarkPanel,
        header={
            Spacer(Modifier.height(18.dp))
            Text("CO",fontSize=21.sp,fontWeight=FontWeight.Black,color=NeonGold)
            Spacer(Modifier.height(15.dp))
        }
    ) {
        listOf(
            Triple("home","⌂","Главная"),
            Triple("cases","▣","Кейсы"),
            Triple("chat","✉","Чат"),
            Triple("menu","☰","Меню")
        ).forEach { (key,icon,label) ->
            val active=selected==key ||
                (key=="cases" && selected=="detail") ||
                (key=="chat" && selected=="player") ||
                (key=="menu" && selected in listOf("profile","inventory","trades"))
            NavigationRailItem(
                selected=active,
                onClick={onSelect(key)},
                icon={
                    BadgedBox(badge={
                        val count=if(key=="chat")unread else if(key=="menu")pending else 0
                        if(count>0) Badge {
                            Text(if(count>9)"9+" else count.toString())
                        }
                    }){
                        Text(icon,fontSize=24.sp,
                            color=if(active)NeonGold else PaleText)
                    }
                },
                label={Text(label,fontSize=11.sp)},
                alwaysShowLabel=true,
                colors=NavigationRailItemDefaults.colors(
                    indicatorColor=LighterPanel,
                    selectedIconColor=NeonGold,
                    unselectedIconColor=PaleText
                )
            )
        }
    }
}

@Composable
private fun PremiumBottomNav(selected:String,unread:Int,pending:Int,onSelect:(String)->Unit) {
    NavigationBar(
        modifier=Modifier.fillMaxWidth().heightIn(min=65.dp),
        containerColor=Color(0xFF0F222B),
        tonalElevation=5.dp
    ) {
        listOf(
            Triple("home","⌂","Главная"),
            Triple("cases","▣","Кейсы"),
            Triple("chat","✉","Чат"),
            Triple("menu","☰","Меню")
        ).forEach { (key,glyph,name)->
            val active=selected==key || (selected=="detail" && key=="cases") ||
                (selected=="player" && key=="chat") ||
                (key=="menu" && selected in listOf("inventory","trades","profile"))
            NavigationBarItem(
                selected=active,
                onClick={onSelect(key)},
                icon={
                    BadgedBox(badge={
                        val count=when(key) {
                            "chat"->unread
                            "menu"->pending
                            else->0
                        }
                        if(count>0) {
                            Badge(containerColor=NeonViolet,contentColor=SoftWhite) {
                                Text(if(count>9)"9+" else count.toString(),fontSize=9.sp)
                            }
                        }
                    }) {
                        Text(glyph,fontSize=22.sp,
                            color=if(active)NeonGold else PaleText,
                            fontWeight=FontWeight.Black)
                    }
                },
                label={
                    Text(name,fontSize=10.sp,maxLines=1,
                        color=if(active)NeonGold else PaleText,
                        fontWeight=if(active)FontWeight.Bold else FontWeight.Normal)
                },
                colors=NavigationBarItemDefaults.colors(
                    indicatorColor=Color(0xFF203E46),
                    selectedIconColor=NeonGold,
                    unselectedIconColor=PaleText
                )
            )
        }
    }
}

@Composable
private fun PremiumLogin(client:GameClient) {
    val scope=rememberCoroutineScope()
    var create by rememberSaveable {mutableStateOf(false)}
    var username by rememberSaveable {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    var passwordVisible by remember {mutableStateOf(false)}
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal=18.dp,vertical=18.dp),
        horizontalAlignment=Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.Center) {
            Image(painter=painterResource(R.drawable.ic_launcher),contentDescription=null,
                modifier=Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(9.dp))
            Column {
                Text("CASEOPENER",fontSize=22.sp,fontWeight=FontWeight.Black,color=SoftWhite)
                Text("N E W   2 . 0",fontSize=12.sp,fontWeight=FontWeight.Black,color=NeonGold)
            }
        }
        Spacer(Modifier.height(11.dp))
        Box(Modifier.fillMaxWidth().height(205.dp)) {
            GameArt("", "📦",Modifier.size(132.dp).align(Alignment.CenterStart).offset(x=2.dp,y=(-9).dp),
                caseId="street")
            GameArt("", "👑",Modifier.size(133.dp).align(Alignment.CenterEnd).offset(x=(-2).dp,y=(-9).dp),
                caseId="legend")
            GameArt("", "💠",Modifier.size(147.dp).align(Alignment.BottomCenter).offset(y=(-2).dp),
                caseId="neon")
        }
        Spacer(Modifier.height(2.dp))
        Text("🔒 Требуется авторизация",color=NeonGold,
            fontSize=20.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("Войди в аккаунт или создай новый, чтобы открывать кейсы и получать награды.",
            color=PaleText,fontSize=13.sp,textAlign=TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.widthIn(max=520.dp).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF201936)).padding(4.dp),
                horizontalArrangement=Arrangement.spacedBy(4.dp)
            ) {
                listOf(false,true).forEach { mode->
                    val active=create==mode
                    Surface(
                        color=if(active)Color(0xFF672B85) else Color.Transparent,
                        shape=RoundedCornerShape(13.dp),
                        border=if(active)BorderStroke(1.dp,NeonViolet) else null,
                        modifier=Modifier.weight(1f).clickable {create=mode}
                    ) {
                        Text(if(mode) "Регистрация" else "Вход",
                            color=if(active)SoftWhite else PaleText,
                            fontWeight=FontWeight.Bold,
                            textAlign=TextAlign.Center,
                            modifier=Modifier.padding(13.dp))
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            OutlinedTextField(
                value=username,onValueChange={username=it.take(24)},
                label={Text("Логин")},
                singleLine=true,
                modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(13.dp)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value=password,onValueChange={password=it.take(128)},
                label={Text("Пароль")},
                singleLine=true,
                modifier=Modifier.fillMaxWidth(),
                visualTransformation=if(passwordVisible)VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon={
                    TextButton(onClick={passwordVisible=!passwordVisible}) {
                        Text(if(passwordVisible)"Скрыть" else "Показать",fontSize=11.sp)
                    }
                },
                shape=RoundedCornerShape(13.dp)
            )
            client.error?.let {
                Text(it,color=WarningRed,fontSize=12.sp,modifier=Modifier.padding(top=8.dp))
            }
            Spacer(Modifier.height(13.dp))
            GoldButton(
                text=if(client.busy)"Подключаемся…" else if(create)"СОЗДАТЬ АККАУНТ" else "ВОЙТИ",
                enabled=!client.busy && username.trim().length in 3..24 && password.length>=8,
                modifier=Modifier.fillMaxWidth(),
                onClick={scope.launch {client.authenticate(create,username,password)}}
            )
            Spacer(Modifier.height(14.dp))
            NeonPanel(accent=NeonViolet,modifier=Modifier.fillMaxWidth()) {
                Text("☁  Твой прогресс привязан к аккаунту",fontWeight=FontWeight.Bold,fontSize=13.sp)
                Text("Баланс, инвентарь и история открытий сохраняются автоматически.",
                    color=PaleText,fontSize=12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Сервер пока работает без HTTPS. Используй только тестовый пароль.",
                color=NeonGold,fontSize=11.sp,textAlign=TextAlign.Center,
                modifier=Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(15.dp))
    }
}

@Composable
private fun WaitingForAccount(client:GameClient,retry:()->Unit,logout:()->Unit) {
    Column(
        Modifier.fillMaxSize().padding(26.dp),
        verticalArrangement=Arrangement.Center,
        horizontalAlignment=Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color=NeonViolet)
        Spacer(Modifier.height(19.dp))
        Text("Подключаемся к аккаунту",fontSize=20.sp,fontWeight=FontWeight.Bold)
        Text("Один раз загружаем прогресс, затем получаем изменения автоматически.",
            fontSize=12.sp,color=PaleText,textAlign=TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        client.error?.let {Text(it,color=WarningRed,fontSize=12.sp,textAlign=TextAlign.Center)}
        Spacer(Modifier.height(11.dp))
        OutlinedButton(onClick=retry){Text("Повторить подключение")}
        TextButton(onClick=logout){Text("Выйти из аккаунта")}
    }
}

@Composable
private fun UpdateInfo(onClose:()->Unit) {
    AlertDialog(
        onDismissRequest=onClose,
        containerColor=DarkPanel,
        title={
            Column {
                Text("✨ Что нового?",fontWeight=FontWeight.Black)
                Text("Версия "+BuildConfig.VERSION_NAME,fontSize=12.sp,color=NeonViolet)
            }
        },
        text={
            Column(
                Modifier.heightIn(max=330.dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(8.dp)
            ) {
                Text("УЛУЧШЕНО",color=NeonViolet,fontWeight=FontWeight.Bold)
                Text("• Центр уведомлений о подарках, монетах и новостях.")
                Text("• Трейды: обмен до шести скинов и монет между игроками.")
                Text("• Более удобное меню и мгновенная синхронизация данных.")
                Text("• Изображения кэшируются на телефоне и обновляются при изменениях.")
            }
        },
        confirmButton={GoldButton(text="ПОНЯТНО",onClick=onClose)}
    )
}
