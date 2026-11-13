package com.caseopener.newtwo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PremiumGame() }
    }
}

@Composable
internal fun PremiumGame() {
    val context=LocalContext.current
    val client=remember { GameClient(context.applicationContext) }
    val prefs=remember { context.applicationContext.getSharedPreferences("caseopener_ui",Context.MODE_PRIVATE) }
    val scope=rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf("home") }
    var selectedCaseId by rememberSaveable { mutableStateOf("") }
    var showNotes by remember { mutableStateOf(false) }
    var showCoinInfo by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var reducedMotion by remember { mutableStateOf(prefs.getBoolean("reduced_motion",false)) }
    var seenCatalog by rememberSaveable { mutableIntStateOf(0) }
    var firstCatalog by rememberSaveable { mutableStateOf(true) }
    val online=client.state!=null && client.token!=null

    LaunchedEffect(client.catalogVersion) {
        if(firstCatalog && client.catalogVersion>0) {
            seenCatalog=client.catalogVersion
            firstCatalog=false
        }
    }
    DisposableEffect(client) { onDispose { client.close() } }

    MaterialTheme(colorScheme=premiumScheme) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(DarkBg,Color(0xFF100D26),DarkBg))
            ).statusBarsPadding().navigationBarsPadding()
        ) {
            if(client.token==null) {
                PremiumLogin(client)
            } else if(client.state==null) {
                WaitingForAccount(client,
                    retry={client.reconnect()},
                    logout={scope.launch {client.logout(); page="home"}})
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val contentPadding=if(maxWidth<365.dp) 10.dp else 16.dp
                    val maxContent=1050.dp
                    Column(
                        Modifier.fillMaxSize().widthIn(max=maxContent)
                            .align(Alignment.TopCenter)
                    ) {
                        PremiumHeader(
                            client=client,padding=contentPadding,
                            onSettings={showSettings=true},
                            onNotifications={seenCatalog=client.catalogVersion},
                            hasCatalogUpdate=client.catalogVersion>seenCatalog && seenCatalog>0,
                            onCoins={showCoinInfo=true}
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
                        when(page) {
                            "home" -> PremiumHome(
                                client=client,
                                modifier=Modifier.weight(1f),
                                padding=contentPadding,
                                onOpenCases={page="cases";seenCatalog=client.catalogVersion},
                                onCase={selectedCaseId=it;page="detail"},
                                onProfile={page="profile"},
                                onNotes={showNotes=true}
                            )
                            "cases" -> PremiumCases(
                                client=client,modifier=Modifier.weight(1f),padding=contentPadding,
                                onCase={selectedCaseId=it;page="detail"}
                            )
                            "detail" -> {
                                val chosen=client.cases.firstOrNull { it.id==selectedCaseId }
                                if(chosen==null) {
                                    Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                                        TextButton(onClick={page="cases"}) {
                                            Text("Этот кейс недоступен. Вернуться к кейсам")
                                        }
                                    }
                                } else {
                                    PremiumCaseDetail(
                                        client=client,gameCase=chosen,
                                        modifier=Modifier.weight(1f),padding=contentPadding,
                                        onBack={page="cases"}
                                    )
                                }
                            }
                            "inventory" -> PremiumInventory(
                                client=client,modifier=Modifier.weight(1f),padding=contentPadding
                            )
                            else -> PremiumProfile(
                                client=client,modifier=Modifier.weight(1f),padding=contentPadding,
                                onLogout={confirmLogout=true},
                                onSettings={showSettings=true},
                                onNotes={showNotes=true}
                            )
                        }
                        PremiumBottomNav(
                            selected=page,
                            onSelect={
                                page=it
                                if(it=="cases") seenCatalog=client.catalogVersion
                            }
                        )
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
            if(showCoinInfo) {
                AlertDialog(
                    onDismissRequest={showCoinInfo=false},
                    containerColor=DarkPanel,
                    title={Text("🪙 Игровые монеты")},
                    text={Text("Монеты можно получать за продажу выигранных скинов. Пополнение за реальные деньги сейчас не предусмотрено.")},
                    confirmButton={TextButton(onClick={showCoinInfo=false}){Text("Понятно")}}
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
    hasCatalogUpdate: Boolean,
    onCoins: () -> Unit
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
            IconMini("♧",onNotifications,hasCatalogUpdate)
            Spacer(Modifier.width(7.dp))
            IconMini("⚙",onSettings,false)
        }
        val state=client.state
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF17152A)).padding(9.dp),
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
                    trackColor=Color(0xFF423957)
                )
            }
            Spacer(Modifier.width(9.dp))
            Surface(
                color=Color(0xFF35240F),
                border=BorderStroke(1.dp,NeonGold),
                shape=RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("🪙 "+(state?.balance?:0).toString(),
                        fontSize=13.sp,color=NeonGold,fontWeight=FontWeight.Black,
                        modifier=Modifier.padding(start=9.dp,top=8.dp,bottom=8.dp))
                    TextButton(
                        onClick=onCoins,
                        contentPadding=PaddingValues(horizontal=6.dp),
                        modifier=Modifier.defaultMinSize(minWidth=28.dp,minHeight=32.dp)
                    ) {Text("+",color=NeonGold,fontSize=19.sp)}
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
            color=Color(0xFF24203B),
            shape=RoundedCornerShape(12.dp),
            border=BorderStroke(1.dp,Color(0xFF524674)),
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
private fun PremiumBottomNav(selected:String,onSelect:(String)->Unit) {
    NavigationBar(
        modifier=Modifier.fillMaxWidth().heightIn(min=65.dp),
        containerColor=Color(0xFF141027),
        tonalElevation=5.dp
    ) {
        listOf(
            Triple("home","⌂","Главная"),
            Triple("cases","▣","Кейсы"),
            Triple("inventory","▤","Инвентарь"),
            Triple("profile","♙","Профиль")
        ).forEach { (key,glyph,name)->
            val active=selected==key || (selected=="detail" && key=="cases")
            NavigationBarItem(
                selected=active,
                onClick={onSelect(key)},
                icon={
                    Text(glyph,fontSize=24.sp,
                        color=if(active)NeonGold else PaleText,
                        fontWeight=FontWeight.Black)
                },
                label={
                    Text(name,fontSize=10.sp,maxLines=1,
                        color=if(active)NeonGold else PaleText,
                        fontWeight=if(active)FontWeight.Bold else FontWeight.Normal)
                },
                colors=NavigationBarItemDefaults.colors(
                    indicatorColor=Color(0xFF3A2A26),
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
                Text("• Обновлено подключение к игровому серверу.")
            }
        },
        confirmButton={GoldButton(text="ПОНЯТНО",onClick=onClose)}
    )
}
